# Remembered-cover Lob implementation plan

> For agentic workers: use superpowers:executing-plans for this coherent unit; one compact scoped Astra review, one Important/Critical correction pass, no re-review.

**Goal:** Implement v0.6 cover pressure using recent visible observations, without hidden target tracking or hidden Standard fallback.
**Architecture:** A bounded observation helper owns a single frozen snapshot and one attempt per LOS-loss episode. The existing attack adapter has a separate cover admission path; ordinary visible attacks retain their LOS contract, profile cue19/launch30 and moving-muzzle path validation. The sole flight controller and broad retained region remain unchanged.
**Tech stack:** Java17, Minecraft1.20.1/Forge47.4.10, existing pure Java/Node tests and finite private TANK_CORE launcher.
**Spec:** `docs/design/NATURAL-GHAST-REDESIGN-v0.1.md`, Lob cover behavior and preference table; existing committed-profile integrity contracts.

## Ruling and constraints

Compact Astra2026-10-09 ACCEPT: same-target last visible eye/landing only; one admission attempt per LOS-loss episode within10ticks of observation. Require a loaded terrain obstruction toward stored eye and full existing Lob preflight. Expire the entire attempt after40actual game ticks from observation, including launch. Interrupted visible charge restarts the complete cover tell. Apply existing variation/600tick Lob repetition penalty. Freeze endpoint at admission; distinct cue19/launch30, actual-muzzle revalidation; rejection has no hidden Standard fallback. Regained LOS, identity/death, clock/context discontinuity, major/feint ownership, expiry or route failure cancels. Never refresh hidden geometry. Native fixture arrangement is distinct from actual native perception.

No new dependency/network/save format, terrain mutation in product, Player/camera/input/health/velocity override, danmaku/Ground fans, or continuous recording. Explicit fresh private fixture terrain/actors are test-only. Existing original85/raw integrity and accepted swimming JAR checks remain mandatory.

## Review focus

- Hidden target moves: snapshot/endpoint cannot update; observer may record actual hidden position only as declared evidence, never feed it into product.
- Visible charge interruption: full30tick cover tell, separate cue19; never inherit charge age or switch to hidden Standard.
- Clock/identity/context gap: fail closed; expiry includes physical launch, recovery retains ordinary semantics.
- Roof/unloaded/changing geometry: loaded terrain ray and full swept Lob proof reject; no alternate after recipe commitment.
- Reacquisition/repetition/ownership: one attempt per loss episode, existing penalty and unforced variation, no feint/major competition.

## Coherent tasks

- [x] Runtime/behavior RED -> bounded helper/admission/expiry/discontinuity, selector penalty, full-tell/recovery and remembered immutable endpoint regressions GREEN.
- [x] Narrow mapped loaded-ray/adapter integration, preserving ordinary visible behavior; affected pure/mapped/build checks.
- [x] Finite private actual-perception fixture/probe/analyzer tests; one compact scoped Astra source review and one correction pass.
- [ ] One pinned-source finite native trial; honest successful/declined scope, clean owned lifecycle, independent raw/material/original integrity audit. No unchanged probability rerun.
- [ ] English receipt/main design/AF0050/guide, scoped GitHub history, guarded original two-MD mirrors.

Base7c5b169/LAB8b55984. Previous goal turn confirmed existing natural-flow rise but changed no authoritative state: NO_PROGRESS; revalidated clean source and selected this actual deferred implementation gap. No live native handle. Whole goal ACTIVE; this unit cannot prove full-boss release, genuine Player counterplay or readability.

Pre-native: genuine `MISSING_FULL_TELL_REMEMBERED_COVER_LOB` runtime RED,129new pure contracts/all preserved flight suites GREEN; missing analyzer module RED ->23new analyzer+2launcher/55related Node PASS; mapped private probe compilation and20second cached offline Forge build PASS. Private static136block wall is created once in verified fresh AIR before new actors; controller drift produces real Sensing LOS loss, native Cow navigation changes the actual hidden position while product endpoint remains remembered. Actor/wall cleanup is conditional and finite; client rows prove boss identity only. Genuine human hiding, production Goal admission, cue pixels/readability/balance and native failure matrix are not acceptance claims.

One compact scoped Astra source review: no Important/Critical finding. No correction pass or re-review needed. Native feasibility remains pending; the controlled trial cannot establish production Goal selection, Player counterplay, terminal impact or human readability.
