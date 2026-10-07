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
        System.out.println("PASS: 3 mapped Forge swept-AABB regressions");
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
