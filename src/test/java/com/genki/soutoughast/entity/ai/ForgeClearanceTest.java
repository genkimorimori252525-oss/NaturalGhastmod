package com.genki.soutoughast.entity.ai;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Run against the mapped Forge classpath; no world creation or Minecraft launch. */
public final class ForgeClearanceTest {
    public static void main(String[] args) {
        AABB body = new AABB(0, 1, 0, 4, 5, 4);
        AABB floor = new AABB(-1, 0, -1, 5, 1, 5);
        AABB wall = new AABB(-1, 1, 0, 0, 5, 4);
        check(!SoutouGhastInertialMoveControl.clearanceBox(body, new Vec3(0, 2, 0)).intersects(floor), "safe takeoff cannot include the floor");
        check(!SoutouGhastInertialMoveControl.clearanceBox(body, new Vec3(2, 0, 0)).intersects(wall), "safe departure cannot include the wall behind");
        check(SoutouGhastInertialMoveControl.clearanceBox(body, new Vec3(-2, 0, 0)).intersects(wall), "flight into a wall remains blocked");
        // Cardinal context rays do not certify an off-axis body's actual path.
        AABB diagonalBlock=new AABB(6,1,6,7,2,7);
        check(!SoutouGhastInertialMoveControl.clearanceBox(body,new Vec3(0,0,8)).intersects(diagonalBlock),"cardinal ray misses diagonal obstacle");
        var planner=new com.genki.soutoughast.entity.ai.flight.MovementPlanner();
        var anchor=new com.genki.soutoughast.entity.ai.flight.CombatAnchor();
        anchor.evaluate(com.genki.soutoughast.entity.ai.flight.FlightVector.ZERO,
                com.genki.soutoughast.entity.ai.flight.FlightVector.ZERO,new com.genki.soutoughast.entity.ai.flight.FlightVector(0,6,28));
        var boss=new com.genki.soutoughast.entity.ai.flight.FlightVector(-24,6,3);
        var plan=planner.step(anchor,
                com.genki.soutoughast.entity.ai.flight.FlightVector.ZERO,boss,
                new com.genki.soutoughast.entity.ai.flight.FlightVector(.35,0,.94).normalized(),
                com.genki.soutoughast.entity.ai.flight.MobilityContext.Kind.OPEN_AIR,
                new com.genki.soutoughast.entity.ai.flight.MobilityContext.Sample(1023),.9,
                displacement->!SoutouGhastInertialMoveControl.clearanceBox(body,SoutouGhastInertialMoveControl.to(displacement.limited(8))).intersects(diagonalBlock));
        check(plan.intent().mode()!=com.genki.soutoughast.entity.ai.flight.FlightController.Mode.MOVE,"actual diagonal obstacle rejects otherwise clear region return");
        System.out.println("PASS: 5 mapped Forge swept-AABB regressions");
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
