# Natural Ghast redesign v0.1

Current design direction from the 2026-10-06 planning session.

- Non-natural-spawning boss-class special Ghast; no boss bar.
- External armor is visual-only and has no combat stats.
- Air combat AI may be rewritten from scratch.
- Legacy attacks are retired except Domain Expansion; existing voice, sound, particle and animation assets may be reused.
- Normal combat uses a stable player-relative Combat Anchor rather than constant orbiting.
- Temporary maneuvers such as dodge and feint normally return to the same Combat Anchor after braking and recovery.
- Flight emphasizes smooth inertia. Braking is possible but weaker than acceleration, and high speed produces broad turns.
- Tactical intent and physical movement are separated: Brain -> movement intent -> Flight Controller.
- Reusable movement primitives include HOLD, DRIFT, APPROACH, WITHDRAW, STRAFE, RISE, DROP, BRAKE, BURST, CURVE, RETURN and OVERSHOOT.
- Feints are readable deception, not random motion: TELEGRAPH -> COMMIT -> BETRAYAL -> MAIN ACTION -> BRAKE -> RETURN.
- Initial feints: False Approach, False Retreat, left-right/right-left fake, vertical fake, pass-by fake, rare double fake, and abort fake.
- Movement, attack, and attack timing remain independent so future attacks can be combined with any maneuver.
- AI uses observable information, short-term memory, action recency and bounded variation to avoid fixed loops and pure randomness.
- Ordinary HOLD/DRIFT/reposition behavior remains common so not every movement is a feint.
- Domain Expansion switches the entity from air combat to a dedicated ground-combat ruleset, preserving useful legacy landing/run/presentation assets.
- Domain world mechanics should be rebuilt around the reusable Jujutsu Craft concepts already documented in KNEEKURA-TECH-HUB: controller, owner, center, lifecycle, barrier/interior/floor roles, reversible world overlay, restoration, participant state and clash policy.
- Domain must work standalone, with an optional compatibility adapter when Jujutsu Craft is present.
- Domain exterior is a dark shell; interior is a fictional hostile hideout/base with clear running space rather than a maze.

## AI goal

The target is not a perfect machine. The enemy should appear intentional, deceptive, smooth and alive. The player should sometimes correctly read a move, sometimes be baited by it, and should not see a fixed combat loop emerge across repeated fights.

## Reference binary

The recovered latest known 1.0.0 JAR from this planning session is 2,934,797 bytes with SHA-256 `5ad5fb4a3ff9a96095c60f87f5adff7c14cfdd3668954427b3396037f1471931`. It is a different generation from the older same-named JAR recorded in the v0.02 audit.
