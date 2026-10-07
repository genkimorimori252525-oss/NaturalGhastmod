package com.genki.soutoughast.entity.ai;

import com.genki.soutoughast.entity.SoutouGhast;
import com.genki.soutoughast.entity.ai.flight.FlightController;
import com.genki.soutoughast.entity.ai.flight.FlightVector;
import com.genki.soutoughast.entity.ai.flight.MobilityContext;
import com.genki.soutoughast.entity.ai.flight.MovementPlanner;
import com.genki.soutoughast.entity.ai.flight.MovementPrimitive;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;

/** The sole server-side velocity writer for the foundation slice. */
public final class SoutouGhastInertialMoveControl extends MoveControl {
    private final SoutouGhast ghast;
    private final FlightController controller = new FlightController();
    private FlightController.Intent intent = FlightController.Intent.hold();
    private boolean clearanceBlocked;
    private final MobilityContext mobility = new MobilityContext();
    private MovementPrimitive primitive = MovementPrimitive.HOLD;

    public SoutouGhastInertialMoveControl(SoutouGhast ghast) {
        super(ghast);
        this.ghast = ghast;
    }

    public void setIntent(FlightController.Intent intent) { this.intent = intent; }
    public FlightController.Intent getIntent() { return intent; }
    public boolean isClearanceBlocked() { return clearanceBlocked; }
    public MobilityContext.Kind getMobilityContext() { return mobility.current(); }
    public MovementPrimitive getPrimitive() { return primitive; }
    public void resetMobility() { mobility.reset(); primitive = MovementPrimitive.HOLD; }
    public void setMovementPlan(MovementPlanner.Plan plan) { intent = plan.intent(); primitive = plan.primitive(); }
    public MobilityContext.Sample sampleMobility() {
        int mask = 0;
        for (int i = 0; i < 10; i++) {
            Vec3 sweep = to(MobilityContext.direction(i).scale(i < 8 ? 8 : 4));
            if (ghast.level().noCollision(ghast, clearanceBox(ghast.getBoundingBox(), sweep))) mask |= 1 << i;
        }
        var sample = new MobilityContext.Sample(mask);
        mobility.update(sample);
        return sample;
    }

    @Override
    public void tick() {
        if (ghast.level().isClientSide) return;
        FlightVector actual = from(ghast.getDeltaMovement());
        FlightVector next = controller.step(actual, intent);
        // One conservative swept box, bounded by the speed cap's stopping horizon.
        // Include inertia during a turn; checking only the waypoint would miss walls.
        double horizon = Math.min(FlightController.MAX_SPEED / FlightController.BRAKING + 2,
                actual.length() / FlightController.BRAKING + 2);
        Vec3 sweep = to(next.scale(horizon * 0.5 + 1));
        clearanceBlocked = next.length() > 1e-9 && !ghast.level().noCollision(ghast,
                clearanceBox(ghast.getBoundingBox(), sweep));
        if (clearanceBlocked) next = controller.step(actual, FlightController.Intent.brake());
        ghast.setDeltaMovement(to(next));
    }

    public static FlightVector from(Vec3 vector) { return new FlightVector(vector.x, vector.y, vector.z); }
    public static Vec3 to(FlightVector vector) { return new Vec3(vector.x(), vector.y(), vector.z()); }
    // Do not inflate behind the movement: existing floor/wall contact must allow departure.
    static AABB clearanceBox(AABB body, Vec3 displacement) { return body.expandTowards(displacement); }
}
