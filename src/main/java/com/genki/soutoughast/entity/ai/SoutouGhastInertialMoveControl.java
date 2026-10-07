package com.genki.soutoughast.entity.ai;

import com.genki.soutoughast.entity.SoutouGhast;
import com.genki.soutoughast.entity.ai.flight.FlightController;
import com.genki.soutoughast.entity.ai.flight.FlightVector;
import com.genki.soutoughast.entity.ai.flight.MobilityContext;
import com.genki.soutoughast.entity.ai.flight.MovementPlanner;
import com.genki.soutoughast.entity.ai.flight.MovementPrimitive;
import com.genki.soutoughast.entity.ai.flight.CombatAnchor;
import com.genki.soutoughast.entity.ai.flight.TacticalBrain;
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
    private CombatAnchor.Region combatRegion;
    private TacticalBrain.State tacticalState=TacticalBrain.State.idle();

    public SoutouGhastInertialMoveControl(SoutouGhast ghast) {
        super(ghast);
        this.ghast = ghast;
    }

    public void setIntent(FlightController.Intent intent) { this.intent = intent; }
    public FlightController.Intent getIntent() { return intent; }
    public boolean isClearanceBlocked() { return clearanceBlocked; }
    public MobilityContext.Kind getMobilityContext() { return mobility.current(); }
    public MovementPrimitive getPrimitive() { return primitive; }
    public CombatAnchor.Region getCombatRegion(){return combatRegion;}
    public void setCombatRegion(CombatAnchor.Region region){combatRegion=region;}
    public TacticalBrain.State getTacticalState(){return tacticalState;}
    public void setTacticalState(TacticalBrain.State state){tacticalState=state;}
    public void resetMobility() { mobility.reset(); primitive = MovementPrimitive.HOLD; }
    public void setMovementPlan(MovementPlanner.Plan plan) { intent = plan.intent(); primitive = plan.primitive(); }
    public MobilityContext.Sample sampleMobility() {
        int mask = 0;
        // Six coarse body sweeps; diagonals require both adjacent cardinal rays.
        // Planner additionally checks at most two actual candidate displacements.
        for (int i : new int[]{0,2,4,6,8,9}) {
            Vec3 sweep = to(MobilityContext.direction(i).scale(i < 8 ? 8 : 4));
            if (ghast.level().noCollision(ghast, clearanceBox(ghast.getBoundingBox(), sweep))) mask |= 1 << i;
        }
        for(int i=1;i<8;i+=2)if((mask&(1<<(i-1)))!=0&&(mask&(1<<((i+1)%8)))!=0)mask|=1<<i;
        var sample = new MobilityContext.Sample(mask);
        mobility.update(sample);
        return sample;
    }
    public boolean hasDirectionalClearance(FlightVector displacement) {
        Vec3 sweep=to(displacement.limited(8));
        return ghast.level().noCollision(ghast,clearanceBox(ghast.getBoundingBox(),sweep));
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
