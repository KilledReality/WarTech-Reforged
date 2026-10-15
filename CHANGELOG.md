# WarTech Reforged Changelog

## 2.0.0 — 2026-10-15 — experimental CE release

Complete implemented update since public 1.7.0: strategic launchers/silos/ABM,
MLRS and D-30/CAESAR artillery, shared defense, vehicle driving, headquarters,
industrial recipes and multiplayer access/recovery.
[Full RU](docs/modular-warfare/CHANGELOG.ru.md) · [Full EN](docs/modular-warfare/CHANGELOG.en.md).


## Modular Warfare Update — player-facing summary / для игроков

[Русский](docs/modular-warfare/CHANGELOG.ru.md) ·
[English](docs/modular-warfare/CHANGELOG.en.md) ·
[Manual / руководство](docs/modular-warfare/README.md).

These release notes summarize the full current 1.12.2 Community Edition update.
The development entries below are engineering history, not a player release body.
Новые материалы описывают всё обновление CE; журнал отдельных dev-сборок ниже
сохранён для разработки и не предназначен для копирования в описание релиза.

## Local dev79 - 2026-10-09 - Manual Geran catapult launch and camera entry

- Reproduced the user's launch rejection: X/Z-only HBM designator, sufficient
  power and an in-range target, but target elevation in an unloaded chunk. Manual
  launch now uses the shared bounded elevation queue, like autonomous launch.
- Repeated clicks coalesce; changed ammo/designator, distant/dead operator,
  dismantled catapult or invalid IFF cancel safely. Main hand only; specific RU/EN
  errors replace the misleading generic Geran-2 error. No consumption or unwanted
  autonomous flight when pilot handoff fails. Existing range/energy unchanged.
- Keep initial telemetry queued alongside activation; create a fresh camera/course
  on each entry. Reject non-finite network input. Retain dev78 packet-safe mouse.
- 455 unit tests; 90 actual manual-catapult checks, 22 Geran, 30 remote/restart,
  24 defense checks; 281 camera checks, 74 GUI/model frames. Old dev78 reproduces
  X/Z-only failure while explicit XYZ succeeds. Initial fixture home-check ZERO
  bug corrected; the complete rerun passes. Isolated QA, not connected MP play.
- 12-entry bounded delta; 493 Java 8 classes/assets match five final stands.
  907 assets / all 496 PNG/OBJ unchanged; only RU/EN launch messages appended.
  Models/balance, recipes, strategic disablement and 1.7.10 unchanged.
- Installed dev79-manual-geran, 112955179 bytes, SHA-256
  `003B91CFBE661F2C357F273686593EFF0953F3F30EDA70B8F0210FBAD4C172DE`.
  dev78 retained disabled; user saves/config untouched (read-only diagnostics).

## Local dev78 - 2026-10-09 - Network/chunk-safe remote camera input

- Isolated actual per-render player.turn mouse changes from server hidden-player
  relocations. Session-old player anchors previously interpreted position/look
  corrections as mouse input and could pitch the reticle upward during flight.
  No camera-rotation heuristic, duplicate mouse reads or arbitrary movement caps.
- GUI/focus loss/player replacement and duplicate render-END consumption ignored.
  Vanilla sensitivity, inversion and smooth camera processing retained.
- Chase focus uses the same smoothed rig as camera position; telemetry catch-up
  no longer mixes a new focus with an old camera position and spikes camera pitch.
- 451 unit tests, 266 native camera checks: 80 real position/look packets alongside
  chunk integration/unload, 40 telemetry jumps, actual EntityPlayerSP.turn input,
  focus/menu and existing C/F5 transitions. Corrects the dev77 fixture which used
  camera rotation to simulate mouse input and missed the real player-turn path.
- Native 22 Geran, 30 remote/restart, 24 defense cases and 74 UI/model frames pass.
  Exact 493 Java 8 classes/assets match four stands. 8-entry camera-only delta;
  all 909 assets / 496 PNG/OBJ and 245 creative variants unchanged.
- Installed dev78-remote-camera, 112953113 bytes, SHA-256
  `AE705F9CF2A4094F0B329018626EAEFEE587407923F558FEBECA799D38342778`.
  dev77 retained disabled; user worlds/config untouched. Long connected-pilot/MP
  verification remains needed. Gameplay, Geran-5 jet/HE/scale and 1.7.10 unchanged.

## Local dev77 - 2026-10-09 - Geran-5 jet, heavy warhead and remote camera

- Added layered luminous exhaust from the measured upper engine outlet and sparse
  pitch/yaw-correct smoke. No flame on the catapult or a disarmed/crashing drone.
  Reuses the custom cruise plume without changing its existing parameters/assets.
- Geran-5 HE strength doubled to 12 vs unchanged Geran-2 strength 6. Native blast
  test: identical open-air victim 16 blocks away retains 20 HP vs 0 HP respectively.
  Damage is nonlinear and cover-dependent, not exactly twice every victim's HP loss.
- Retained logical 4.2 x 2.11 block dimensions: 25% longer than Geran-2, smaller than
  Neptune. Original mesh/texture and launcher supports unchanged.
- Fixed duplicate event/KeyBinding C and R presses. Smooth remote camera transition
  retains pilot control/flight angles; F5 no longer layers vanilla third person over
  remote optics. Restore the user's prior perspective on remote-control exit.
- 444 unit tests; native 22 Geran, 30 remote/restart, 24 defense cases; 74 GUI/tab/
  model frames and 142 camera checks pass. 496 previous PNG/OBJ, 245 variants retained.
  Exact 492 Java 8 classes/assets match all four stands; bounded 19-entry delta.
- Installed dev77-geran5-effects, 112952089 bytes, SHA-256
  `4D04DD201EDE90CC4ABE54DD440653E3ECA01DCC667D68CAE0DCEEA1E9261D85`.
  dev76 retained disabled. User saves/config untouched. Actual piloting, network MP
  and varied terrain still need play testing. No recipes, 1.7.10 or strategic changes.

## Local dev76 - 2026-10-09 - User Geran-5 model and flight

- Added separate Geran5Drone / geran_5 entity (ID 111) without replacing Geran-2
  or shifting existing profile ordinals. Same catapult, 25000 launch energy,
  20–1000 coordinate launch, 1000 manual link and 1800 horizontal path budget.
- Maximum 37 blocks/sec vs 23; defense target tier 2 vs 1, existing HP 6/HE retained.
  Autonomous detonator and manual piloting share Geran flight/control lifecycle.
  Separate telemetry/cameras, jet trail, inventory model and RU/EN tooltips.
- Imported supplied GLB preserving 13168 triangles/UV/split normals/albedo pixels;
  runtime length 4.2 blocks. Added separate low rail cradle, not missile overlays.
  PBR shaders are not reproduced by the legacy albedo renderer. User source kept.
- Fixed coordinate-fuse restoration and reacquired ENTITY chunks before returning
  the pilot home, preventing post-manual hovering or unobserved frozen flight.
- 436 tests; native 19 Geran, 30 remote/restart, 24 defense cases; 72 GUI/tab/model
  frames pass. All 493 previous PNG/OBJ retained, 245 creative variants (+1).
  490 Java 8 classes/assets match the four final fixtures; 78-entry bounded delta.
  tools/verify_geran5_dev76.py; docs/GERAN5_QA.md; build/qa/dev76-verification.json.
- Installed dev76-geran5, 112948576 bytes, SHA-256
  `E3BD751EFA4AC5BF7DFE48B5764A3DD5F0E77D6F2E97A8FA6131F8884D8252DE`.
  dev75 retained disabled, saves/config untouched. Actual pilot/network/terrain/PVO
  play testing remains needed. Public asset rights unconfirmed. No recipes/1.7.10.

## Local dev75 - 2026-10-09 - Remote launch, interceptor contacts and blueprints

- Imported radar seeker and saved UAV blueprint icons. Blank UAV/cruise plans share
  blank art; filled plans use their own art. Originals/lossless exports preserved.
- Unloaded UAV launch waits for UUID integration; completed start lease survives
  link consumption. Legacy launchers queue their area and resolve missing target Y.
  Detonator launch does not activate pilot mode. Geran autopilot resume recalculates
  NaN altitude between scans. Safe operational queue detachment on world unload.
- Swept/relative custom cruise body contacts resolve actual hits across tick order;
  proximity chances retained. Missed interceptors keep flying and explode against
  obstacles/incidental targets with sound, particles and damage. Live reservations
  retained; failed-shot reassessment 40–60 ticks, not a delay for different targets.
- 429 unit tests; native 30 remote/restart, 12 interceptor, 24 defense checks pass;
  68 GUI/tab frames. Exact 489 Java 8 classes/assets match all four final fixtures.
  Previous 491 PNG/OBJ and 244 creative variants retained; zero residual resources.
  tools/verify_remote_defense_dev75.py; docs/REMOTE_DEFENSE_DEV75_QA.md.
- Installed dev75-remote-defense: 110464633 bytes, SHA-256
  `1178F424660EAE75CB74B60266E6DA7409E822C73B40FC511CA0B3637EB2D1CC`.
  dev74 kept disabled; user saves/config untouched. Arbitrary-world/connected-MP/full
  S400 combat still need play testing. Recipes/1.7.10 deferred; strategic systems off.

## Local dev74 - 2026-10-09 - User component icons

- Imported 49 user PNGs: 43 cruise components, constructor/tab icon, two launch
  fixtures, blank/saved blueprints and the optional ventral UAV rack. Missing
  seeker_radar.png retains its existing fallback. File mapping/list updated.
- Originals preserved; verified lossless square padding and 256×256 RGBA exports
  under artwork/cruise_parts2-square-v1, with manifest hashes and dev73 model backup.
  Provided translucent backdrops retained, not regenerated or removed.
- Inventory body components use supplied art rather than 3D item rendering;
  assembled missiles/world geometry, all old 442 PNG/OBJ, server code and balance
  remain byte-identical. Saved blueprint predicate retained; rack world unchanged.
- 423 tests pass; 68 native GUI/tab frames, 244 creative entries retained. All
  488 production classes/new resources match the final client fixture. Strict
  tools/verify_icons_dev74.py; build/qa/client-dev74-icons and dev74-verification.json.
- Installed dev74-component-icons: 110361051 bytes, SHA-256
  `7B9A786E80E0111B01F6DA0EAB97C1D4F43ADBDE998B526A54295C4421A2635F`.
  dev73 kept disabled; user saves/config untouched. Recipes deferred.

## Local dev73 - 2026-10-08 - Remote operations, elevation and GUI bounds

- Custom detonator commands queue unloaded launch sites, retaining failed links
  and validating ownership/load identities. X/Z HBM designators resolve terrain Y
  server-side, including unloaded targets; stale GUI/mission imports are cancelled.
- Persistent defense index wakes mobile/static nodes near flights without players.
  Existing saved batteries require one visit after upgrading to enter the index.
  Tile unload no longer forgets nodes or reloads the chunk during network cleanup.
  Interceptors wait for flight chunks; missed attempts retry after 12–20 ticks.
- Bounded GUI columns/titles, IFF subtitle/button separation, populated fleet paging
  and recon rows; 68 real RU/EN GUI/tab fixtures at 1280×800 / scale 2.
- 419 tests; 23 remote/restart and 24 defense regression cases pass. Four cruises
  and two UAVs launched from unloaded saved sites >28k blocks away; distant Y resolved,
  unobserved VLS woke and Pantsir fired. All temporary tickets/queues released.
- Exact 488 production class bytes match all three final test installations;
  442 PNG/OBJ and 244 creative variants retained. See docs/REMOTE_OPERATIONS_QA.md.
- Installed dev73-remote-operations: 108226845 bytes, SHA-256
  `29F514BC1D4A21A27852100902DE17ABC6431200A559B6F812F15952F9585A03`.
  dev72 retained disabled; user worlds/config untouched; recipes/icons deferred.

## Local dev72 - 2026-10-08 - Detonators, defense IFF, damage and maintenance

- Ordinary and multi detonators bind/launch custom missiles and UAVs through Forge
  interactions. Failed launches retain their links; stale load IDs cannot launch a
  replacement missile. No synchronous remote chunk generation. Native HBM multi
  bindings retained. Empty custom NBT no longer falls through to a phantom native launch.
- Explicit different teams override common owner UUID for IFF; personal unteamed
  fixtures snapshot player UUID. Nearby confirmed hostile launches no longer ignored
  solely as local outbound tracks. Pantsir/Tor friendly then switched-owner firing tested.
- Defense HP 200/240/300/360, proportional saved-health migration, radar/command blast
  armor corrected. Critical damage disables radar, repair restores it; damaged PVO range
  reduced. Own/allied vehicles repair with RMB iron (10%, minimum 20 HP); no wasting
  iron at full HP. Colored HP bars, RU/EN maintenance hints and deploy button.
- AA/ABM/ASAT missiles under defense, control/maintenance tools grouped; stable
  vehicle/launcher/ammunition and component ordering. Registry IDs/assets/recipes retained.
- 417 tests, 24 isolated server defense scenarios, 8 load/launch and 15 payload cases,
  allied blast regression; 42 native GUI/tab fixtures. Exact tested production class
  validation; 483 Java 8 classes, all 442 PNG/OBJ unchanged. See docs/DEFENSE_DETONATOR_QA.md.
- dev72-defense-detonator; dev71 retained disabled, user worlds/config untouched.

## Local dev71 - 2026-10-08 - Ground loading and actual payload damage

- Ground custom launcher accepts compatible missiles by click, Shift-click and
  hopper; extraction/ownership/NBT/load identities retained. Loading never launches.
  RU/EN hints distinguish rail, booster and incompatible air adapters.
- Friendly cruise bomblets survive sibling blast damage/knockback instead of silently
  losing most of a contact cluster. Ordinary shells/enemies/living targets/PVO remain
  vulnerable. No change to count/strength/stats. Full blast cover no longer takes +1 chip.
- 413 passing tests, 8/8 actual server container load/launch paths, all 13 custom
  warheads against ticking villagers plus both airborne cluster cases. Ground cluster
  detonations 7/12 → 12/12 and heavy 2/24 → 24/24 vs unchanged dev70 on identical QA.
- Dedicated-server allied-blast/PVO regression and two normal close impacts (3.24 m
  inbound separation at first blast); no flight resource leaks. 36 native RU/EN GUI
  renders. Strict exact production-byte validation; all 442 OBJ/PNG retained.
- Installed dev71-ground-payload, dev70 retained disabled. See
  docs/GROUND_LAUNCH_PAYLOAD_QA.md for test setup corrections and limitations.

## Local dev70 - 2026-10-08 - Friendly strike safety and responsive interfaces

- Explicitly allied armed strike ordnance no longer receives sympathetic damage,
  knockback, fragment/cluster contact or EMP from friendly strike effects. Delayed
  child provenance persists in NBT. Unrelated/enemy explosions and PVO remain lethal;
  no global timed/radius immunity. Fixed a production-server area-AABB overload crash.
- New shared instrument theme for radar/PVO/launcher and main aircraft/constructor/
  mission panels, bounded labels, RU/EN presentation, consistent buttons, compact power
  bars. Slot IDs/actions preserved; launcher layout changes with its container.
- Cache item bounds by geometry, avoid regex OBJ tokenization, prepare known 3D
  variants during resource loading instead of creative-tab opening. Reload-safe
  display-list invalidation. Delta-only legacy window properties, complete initial state.
- 406 tests and real dedicated-server checks of all 13 warheads plus two adjacent
  normal strikes (second inbound 8.10 blocks from first blast); no leftover flight resources.
  Native Minecraft RU/EN screenshots and ten actual item-tab sets; first draw of custom
  cruise tab reduced from 2389.71 to ~2 ms in the isolated fixture, not an FPS/TPS claim.
- All 442 OBJ/PNG, stats/CEP/defense tiers/flight/mounts/IDs/recipes/queue preserved.
  User icon-catalog edit retained. Exact tested common/server and client class bytes
  verified; QA never shipped. See docs/UI_AND_BLAST_QA.md for scope and limitations.
- Installed dev70-salvo-ui; dev69 preserved disabled, one active WarTech JAR.
  Final native client `client-dev70-depth`, all ten tabs 0.11–1.95 ms first draw;
  original PNG pixel checks confirm every radar inventory cell renders completely.

## Local dev69 - 2026-10-08 - Coordinated friendly strike salvos

- Top custom cruise/UAV controllers allocate distinct friendly near-target transit
  lanes rather than relying solely on UUID route variation. Fixed launch/target/time
  cohort anchor, explicit owner/team, unique maximally spaced reservations; saved NBT.
- Smooth lane handover and fan ingress without early merging; real terminal/release
  gates and fuel/return reserves retained. Short/fuel-constrained legs simplify.
  No holding loops, forced simultaneous arrival, retargeting, omniscience or immunity.
- Search-area transit may coordinate; seeker tracking/search loiter and recon/manual/
  return retain their purpose. Lower tiers keep dev68 behavior. RU/EN details updated.
- Per-world bounded weak directories, cached claims/expiry; no new scans/rays/chunk
  loading. Combat stats, CEP, models, damage, mounts, IDs, recipes and queue unchanged.
- 390 passing tests; three actual server runs verify near-target salvos, tier gating,
  stable distinct real paths, two reusable UAV missile releases/returns, and dev67
  short-target regression (9 releases/11 normal finite strikes/four landings).
  Zero leftover flight resources, all 475 production classes match tested bytes.
- Installed dev69-salvo-coordination; dev68 retained disabled. Failed initial cohort
  diagnostic retained. Live client/ordinary terrain/connected MP still need playtests.

## Local dev68 - 2026-10-08 - Visible module-dependent autonomous flight

- Custom cruise/UAV basic navigation stays direct; advanced controllers use stable,
  smooth individual transit corridors plus bounded loaded-terrain local planning.
  No per-tick random jitter, omniscient threat routing or cooperative swarm claim.
- Advanced recon position-relative survey circles/ellipses respect turn radius;
  short/fuel-constrained legs and terminal/release windows avoid decorative weaving.
  Native Geran behavior, manual/takeoff/landing and dev67 finite strikes preserved.
- Persist route preferences/avoidance memory; phased/cached planning, unchanged
  ray/height/chunk limits. Fuel/energy and physical flight remain real; RU/EN details updated.
- 380 passing tests and three isolated dedicated-server runs: six guided route
  strikes, three recon returns and one one-way impact; short-target regression
  9/9 releases and 11 normal detonations; ordinary carriers 14/14 releases/six landings.
  Zero leftover flight resources; exact production class bytes verified on all runs.
- Strict dev67 delta: eight changed entries/two added classes. All 442 PNG/OBJ,
  27 UAV icons, stats/CEP/defense/damage/mounts/scale/IDs/recipes/chunk queue unchanged.
- Installed dev68-autonomous-flight, dev67 retained disabled. Preliminary failed
  route-visibility diagnostic retained; no live client/ordinary-terrain/MP/TPS claim.

## Local dev67 - 2026-10-08 - Re-attack approaches and finite cruise strikes

- Valid short-target sorties no longer cancel merely because takeoff consumed the
  launch window. Shared bounded aircraft/UAV flank/downwind/base/final approach;
  safe aligned release remains immediate. Minimum range/fuel/obstacle checks retained.
- Intended building contact volume is not endlessly evaded; missed/empty terminal
  coordinates lead to a finite forward/downward strike, not repeated orbiting.
  Physical motion/fuses/CEP/search logic retained; no automatic perfect-hit shortcut.
- Launch prediction shares terminal-pass logic; new approach/strike state persists.
- 371 passing tests, real short-target suite: 9 carrier releases plus two direct
  regressions, 11 normal detonations, four landings. Ordinary sortie suite: 14 releases,
  six reusable landings. Tickets/forced chunks/queue clear; exact tested production bytes.
- Strict dev66 delta: 13 changed entries, two new classes; 442 PNG/OBJ, 27 UAV icons,
  mounts/scale/combat stats/CEP/defense/IDs/recipes/chunk queue preserved. QA not shipped.
- Installed dev67-strike-approach, dev66 retained disabled. Failed diagnostics retained.
  Live client, arbitrary terrain, damaged flight and connected multiplayer need testing.

## Local dev66 - 2026-10-07 - Mounted stores and working carrier sorties

- Full geometry/contact/gear audit of 237 native/custom payload station combinations;
  Tu-95 mixed six-station layout, corrected body-skin contacts and tapered UAV supports.
- Shared release/render pitch pivot including model lift; correct F-16 native pitch axis.
- Ground roll and smooth rotation, forward-speed-limited climb, bounded heading turns;
  Tu-95 forward climb reference, custom strike ingress after gaining safe altitude.
- UAV downwind/base/final approach, velocity-consistent heading, touchdown completion
  tolerance and old-save approach migration; legacy Geran nonterminal steering improved.
- Initialize home before launch validation; reject too-close heavy missile sorties
  without energy/cargo loss. Match five-ray known-terrain and obstacle corridor widths.
- 366 tests, 237 full-mesh combinations, 15 offline native previews; dedicated server
  confirms 13 custom missile releases plus native MQ-9 release and all six reusable
  carrier landings. Flight tickets/forced chunks/queue clear at completion.
- Installed dev66; dev65 retained disabled. Assets/scale/balance/CEP/IDs/recipes/chunk
  queue unchanged; QA mod excluded. Live client, normal terrain and MP still need testing.

## Local dev63 - 2026-10-06 - Continuous flight windows and coherent weapon bands

- Bound ENTITY chunk tickets persist/recover; continuous current/swept/ahead windows,
  updates on same-chunk turns, depth 25, 32 flights / 16 modular per dimension.
- At most four explicit missing-chunk loads per world tick, preflight reservation,
  main-flight fuel/age pause, loaded-only terrain/blast access; legacy accelerated
  movement has separately validated bounded sweeps, cluster velocity bounded.
- Finite native missile horizontal budgets and crash on exhaustion, persisted
  travelled distance, no old Kh555 lifetime cut-off, distinct native defense classes.
- UAV fuel/radius ceilings and return reserve; conventional HE/TB/artillery/cluster
  power bands, real bounded EMP radius instead of HBM's fixed long-lived 100-block field.
- Safe carrier drop validated within actual known terrain, rather than rejecting all
  ungenerated distant terrain. Clean models, IDs, modular cruise stats/CEP preserved.
- Updated artwork brief: 49 required PNGs, one optional UAV single-pylon icon.
- Own falling cluster submunitions with actual saved strength and flight tickets;
  the CE cluster helper ignored its strength argument. Bounded HE fallback on quota exhaustion.
- 315 headless tests, package/resource audits. Live Minecraft/MP/TPS testing pending.

## Local dev62 - 2026-10-06 - Correct wing mounts and safe guided air release

- Native swept-wing hardpoints, complete fin-envelope clearance, scaled Strizh
  pylon contact and server F-16 release pitch matching the native renderer.
- Air-release free drop retains attitude; installed navigation starts with thrust.
  Advanced terminal approach retains obstacle avoidance; basic INS stays basic.
- Bounded loaded-only initial-flight prediction before automatic/manual release.
  Nearby forward-side/climb alternatives, held manoeuvre, finite timeout, no backwards
  hooks to passed release points. Blocked stores preserved; manual rejection localized.
- 295 tests, Java-driven native mesh previews. 442 PNG/OBJ, module stats, CEP,
  defense, warheads and IDs unchanged. Live Minecraft/MP validation pending.

## Local dev61 - 2026-10-06 - Carrier strike orders from missile programs

- Immediate ready-state import of loaded missile X/Y/Z into aircraft strike queues
  and a custom UAV strike waypoint; replacing/removing stores refreshes auto orders.
- Manual orders and explicit clearing protected; localized "From missiles" action
  replaces them only when a valid compatible program exists in the same dimension.
- Search missiles provide their first approach area without changing their full program.
- Persisted order provenance, conservative legacy-save migration, no airborne replanning.
- 276 tests, including 17 import/save regressions. Models, balance, CEP, defense,
  warheads and release logic unchanged. Live Minecraft/MP validation pending.

## Local dev60 - 2026-10-06 - Cruise defense tiers and working aircraft carriers

- Defense difficulty combines airframe visibility, terrain navigation and speed;
  classes 1–3 shared with radar and interception. CEP and damage unchanged.
- Launcher-owned twin rails, saddles and braced pedestal; clean missile meshes retained.
- Su-27/F-16: 3 light OR 2 Neptune OR 1 Storm Shadow, enforced on insertion.
  Tu-95: six of any body, including Strizh. Shared render/release positions and fixed orientation.
- Auto cruise strike selection, inherited carrier X/Y/Z for unprogrammed stores,
  preserved user programs and sequential release of multiple missiles.
- Removed artificial close-target withdrawal loops; minimum safe release distances retained.
- Separated localized carrier GUI rows and bounded button labels; server slots relocated consistently.
- Offline geometry preview and headless regression tests; live client/MP validation pending.

## Local dev59 - 2026-10-06 - Clean missile bodies and compact cruise carriage

- Disabled visible modular alterations: four fixed native bodies, original paint,
  fixed wings/nozzles; no added boosters, cylinder overlays or detached booster meshes.
  Lastvika keeps its propeller removed and standard decorative jet nozzle.
- Gameplay modules, mass/range/speed, guidance/search/CEP and warheads retained.
- OBJ-measured native tail socket position shared by launcher render and server spawn;
  no extra launch beam or levitating cylinder, original plume/dust/sound retained.
- Narrow aerodynamic ventral pylon with small ejector shoes, closer stores and no
  service stilts. Light-store ground lift removed, Neptune clearance reduced to .35;
  persisted parked heights migrate without teleporting airborne carriers.
- Terminal speed no longer shrinks with distance; full speed when aligned, bounded
  heading-error correction. Swept actual contact and selected fuse logic retained.
- 242 tests; all existing 441 PNG/OBJ unchanged, one new pylon OBJ. Live Minecraft
  validation remains pending. Strategic systems disabled; 1.7.10 untouched.

## Local dev58 - 2026-10-06 - Cruise screenshot fixes and dedicated UAV rack

- Four bodies first; grouped cruise/UAV parts with stable old metadata.
- Safe X/Z-only HBM designator height resolution/manual Y, no distant chunk generation.
- Class-specific mass/fuel-dependent range balance; altitude/turn-aware terminal descent
  and approach speed limiting, preserving probabilistic CEP and air-defense tables.
- Native airframe components instead of alien overlay housings; family-specific boosters,
  geometry-based launch height/cradle and explicit lighting/color/normal state.
- Dedicated single ventral cruise rack, recipe and 3D item icon; two carrier presets;
  numeric overload and specific adapter/body/wing/thrust rejection messages.
- RU/EN UAV modules, constructor, mission/control/report panels and HUD.
- 234 tests; 441 PNG/OBJ and existing 27 user icons preserved. No 1.7.10 changes;
  strategic systems remain disabled. Live Minecraft/MP/GPU QA is still pending.

## Local dev57 - 2026-10-06 - Missile crash outcomes and custom UAV cruise carriage

- Persistent falling shoot-down states for custom and legacy cruise missiles;
  probabilistic reduced/full impact, dud kinetic impact and delayed airburst.
- One real ventral custom cruise store on reusable UAVs, alternative to wing stores;
  full item/build/program sync, air-adapter/geometry/mass/thrust checks, landed loading.
- Loaded UAV speed/turn/range/energy penalties; release restores dry envelope, not HP.
  Recon light rack gets one node and 380 takeoff mass; strike retains 490.
- Carrier aiming range and full missile route budget checked independently;
  body-specific minimum standoff, automatic withdrawal/ingress for UAV/aircraft.
- 220 tests; native models, 441 PNG/OBJ and 27 user UAV icons preserved.
  Strategic content remains disabled; 1.7.10 untouched; live Minecraft/MP QA pending.

## Local dev56 - 2026-10-06 - Cruise air-defense and probabilistic accuracy

- Separate navigation tiers and speed/load-dependent custom target classes;
  explicitly recognise modular cruise/UAV in aircraft and network tracking.
- Persistent per-flight CEP dispersion, better brains/confirmed seekers improve
  precision; range and heavy airframes increase game aim error. RU/EN GUI/tooltips.
- Loaded-only segmented sight checks, bounded radar candidates, no refreshed
  hidden contacts, friendly/dead/crashing/landed target revalidation.
- Probabilistic custom-target interceptions (no 100% guarantee), swept proximity
  checks, obstacle rejection, falling damaged cruise instead of disappearing.
  AAM also validates visibility and uses probabilistic custom interception.
- Retargeted strike coordinates update; search areas are not known strike goals
  before a confirmed seeker target. Existing payload/stats/assets untouched.
- Automated tests and resource audits; live Minecraft/MP validation pending.

## Local dev55 - 2026-10-06 - Cruise visual modularity and launch effects

- Added 28 original decorative seeker/engine accessory meshes over the four
  detailed author airframes; retained all existing OBJ/PNG resources byte-for-byte.
  Fuel changes radial profile, folding wings interpolate continuously, previews,
  pylons and loaded launchers share the same configuration.
- Correctly located layered luminous exhaust, bounded continuous smoke trails,
  ground dust, air-drop delayed ignition and cosmetic falling booster separation.
- Distinct bounded impact accents by payload family, additional short launch
  sounds and crash smoke. Existing damage/flight computers/seekers/stats unchanged.
- Fixed-size client-only event packet ID 20, persisted non-replaying milestones,
  world-reset/culling/particle-setting and shared particle/event/debris limits.
- Updated presentation clearances and nose contact length. 1.7.10, public releases,
  UAV artwork and disabled strategic content untouched. Live Minecraft/MP QA pending.

## Local dev54 - 2026-10-06 - Cruise navigation and seeker development

- Persistent local avoidance side, waypoint hysteresis, two-leg checks and
  bounded terrain probes for upgraded flight computers; basic flight stays direct.
- Five programmed target categories, server-validated seeker compatibility,
  bilingual controls/tooltips and schema-2/legacy mission migration to schema 3.
- Confirmed target acquisition, last-seen memory, reacquisition, bounded movement
  prediction, sensor sectors, optical weather penalties and radar terrain occlusion.
- Corrected tracking toward captured targets and airburst using the actual strike
  point; speed/turn-aware loitering, meaningful search durations and fuel reserve.
- Models, component IDs/build NBT/balance and UAV icons unchanged. Strategic
  content remains disabled; live Minecraft/multiplayer testing still pending.

## Local dev53 - 2026-10-06 - Light jet-drone model replacement

- Replaced only the light cruise airframe with user-supplied UJ-32 Lastvika,
  CC BY 4.0 by SpinoCactus. Removed the propeller/piston motor by material,
  retained all other 12,128 author triangles and UVs; added a recessed game nozzle.
- Native 2048 textures, localized two-roundel retouch, wing variants and
  three engine-nozzle shapes. Corrected nose/exhaust offsets and pylon clearance.
- Heavy Tu-141, existing component IDs/NBT/balance and all 27 UAV icons unchanged.
  Strategic features still disabled. No live Minecraft/multiplayer validation yet.

## Local dev52 - 2026-10-06 - Heavy endurance cruise class and payload catalog

- Appended a fourth heavy long-range jet-missile body, endurance engine, large
  fuel section and heavy folding wings. Reuses the credited detailed Tu-141
  mesh at 1.45 game scale, not a purported Flamingo model. The original light
  configuration and all old IDs/NBT/checksums remain compatible.
- Expanded cruise payloads from five to thirteen: heavy HE/thermobaric/cluster/
  penetrator, standard/heavy fragmentation, focused anti-vehicle and incendiary.
  Distinct server effects, fuse rules, real mass/range/speed/turn penalties,
  loaded-only area/fire probes and bounded target/fire counts.
- Added eight starter missiles/four saved starter plans in total. New heavy
  airframe can launch from a booster fixture or Tu-95, not fighter/Reaper rails.
  Preserved 4-bit aircraft payload categories using synced NBT for the new body.
- Launcher/model/collision offsets account for larger size; measured booster
  tails now clear the ground at both scales. No automatic launch on loading.
- RU/EN descriptions and recipes for all 44 modules; art brief now 49 PNGs
  (12 additional masters). Existing user UAV icons and neutral atlases retained.
- Automated checks and Blender resource preview do not replace live Minecraft/
  multiplayer testing. Strategic Topol-M/Yars/Oreshnik remain disabled.

## Local dev51 - 2026-10-06 - Cruise UI, autonomous routing and commanded launch

- Replaced manual transit programs with one coordinate strike goal or up to eight
  seeker-only search areas. Dev50 missions migrate to their final strike goal.
- Basic computers guide directly; higher tiers choose local obstacle avoidance,
  cruise height and loaded-terrain samples with a bounded rolling planner.
- Ground loading stores a persistent missile without spawning flight; launch is
  a separate fixture button or owned HBM multi-detonator command. Added synced
  loaded-missile rendering, retrieval and per-load identity against stale links.
- Added a real inventory designator slot and return-on-close; held missiles are
  locked against swaps. Fully localized cruise UI/module descriptions and added
  live range/speed/mass changes plus prospective module-install comparisons.
- Made actual speed/acceleration depend continuously on mass/engine, repaired
  incomplete-build range estimates, compacted GUI layouts and paged search areas.
- Art specification: `docs/CRUISE_COMPONENT_ICONS.md`, 37 requested PNG masters.
  No new raster art has been substituted. UAV icons/models and strategic-disabled
  state remain preserved. Live Minecraft/MP verification remains required.

## Local dev50 - 2026-10-06 - Minecraft 1.12.2 NTM CE custom cruise

- Added the ten-slot cruise constructor, 32 stable module variants, reusable
  blueprints, six creative starter builds and held-item route programming.
- Added rail/booster ground fixtures and air adapters for F-16C, Su-27 and Tu-95,
  server-owned fuel-limited flight, seekers, fuses, five HBM payload behaviours,
  owner-checked command-link updates and shared bounded UAV/cruise chunk tickets.
- Integrated neutral Strizh/Neptune/Storm Shadow assets, continuous wing-span
  variants, folded deployment stages, item/constructor previews and synced pylons.
  Original faces/UVs and all 27 user UAV component icons remain preserved.
- Strategic Topol-M/Yars/Oreshnik additions remain disabled. 1.7.10 and public
  release artifacts are unchanged. Automated/Blender verification is complete;
  live Minecraft and multiplayer validation of this checkpoint is still required.
- Controls and known visual limits: `docs/CUSTOM_CRUISE_MISSILES.md`.

## Unreleased - Minecraft 1.7.10 Custom UAV backport

- Backported the complete dev44 modular Custom UAV system from Minecraft
  1.12.2 to Forge 1.7.10: all three airframes, all 27 stable module metadata
  values, NBT blueprints, Constructor, Mission Programmer, Launch Point,
  Fleet, Recon Report, Remote Pilot, missions, recovery and wreck lifecycle.
- Preserved server-authoritative networking, IFF, HBM/WarTech designators,
  HBM energy/explosions, radar/air-defense/EW/flares, aviation hardpoints,
  chain detonator groups and bounded two-chunk flight tickets.
- Ported the approved OBJ geometry, TB2 strike model, skins, block geometry,
  item previews, bilingual guide books, localizations and survival recipes.
- Added a reproducible Java 8 backport build and contract smoke test. Production
  Forge tests pass with HBM NTM X5751 and NTM Space X5758 H261, including two
  simultaneous clients, entity spawn/render sync, save/restart and loading a
  world created by the pre-UAV stable 1.7.10 JAR.

## 1.6.0 - 2026-07-30

WarTech Reforged 1.6.0 contains two separate builds:

- **Stable:** Minecraft `1.7.10`, Forge `10.13.4.1614`, HBM NTM
  `1.0.27 X5751` or HBM NTM Space `X5758 H261`.
- **Experimental port:** Minecraft `1.12.2`, Forge `14.23.5.2860`,
  NTM Extended `3.0.3`.

The builds are not interchangeable. Never install both JARs in the same
`mods` directory.

### Added

- Blank UAV blueprints are now exposed in the UAV creative tab. Saved plans
  accept a custom 32-character model name and can automatically pull one exact
  matching component set from the player's inventory for repeat production.
- Custom UAV fleet control in the Mission Programmer, with live friendly-UAV
  telemetry, route progress, reusable-aircraft recall and recon report access.
- Persistent custom-UAV operational condition and landed repair servicing.
- Experimental 1.12.2 modular UAV constructor with three airframes, 24 parts,
  persistent reusable blueprints, calculated flight characteristics,
  server-side design validation, autonomous missions, Remote Pilot, modular
  payloads, flares/EW and bilingual documentation.
- Complete Minecraft 1.12.2 compatibility port of the WarTech Reforged 1.6
  content catalog, mechanics, registries, recipes, entities, tile entities,
  GUIs, networking, save data, models, textures and sounds.
- Driveable command, radar, electronic-warfare, air-defense and artillery
  vehicles with deployment states, persistent inventories, HBM energy and IFF.
- Patriot, S-400, Tor-M1 and Pantsir-S2 air-defense systems with layered radar
  tracking, target reservation and tiered interceptor behavior.
- Strategic early-warning radar, long-range communication relays, air-raid
  siren integration and faction command networking.
- Geran-2, MQ-9 Reaper, F-16C, Su-27, Tu-95, Kh-555, aircraft payloads,
  countermeasures, wrecks and Remote Pilot controls.
- HBM-compatible artillery target designator and restored Greg/Henry FIFO
  target queues.
- Legacy NBT migration for vehicles, aircraft, missiles, launchers, radars,
  EW equipment, artillery, ownership and inventories.
- Automated registry, resource, NBT, vehicle-input, missile-impact and archive
  integrity tests for the 1.12.2 port.

### Changed

- Custom UAV terminal guidance, ballistic bomb release and landing have been
  retuned for tighter target accuracy and a stable aligned glideslope. TB2
  pylons, bombs and rear propellers now use the actual model bounds.
- Autonomous custom UAVs now use bounded two-chunk flight corridors, terrain
  look-ahead and obstruction-aware landing with automatic go-around behavior.
- Ground vehicles retain the original maximum speeds and heavy-vehicle
  handling from the 1.7.10 implementation.
- Aircraft and drone remote controls use synchronized mouse flight, smoothed
  telemetry and nose/chase camera modes.
- Cruise, ballistic, anti-radiation and air-launched weapons use swept impact
  checks so fast missiles cannot skip through their target.
- Pantsir-S2 uses Tier-1 interceptors; Tor-M1 uses Tier-2 interceptors, matching
  the original behavior.
- Item previews use the original model transforms and textures without leaking
  the Minecraft item atlas into OBJ rendering.

### Fixed

- Restored functional movement for all ground vehicles, including remapped
  controls, server-authoritative input and correct client synchronization.
- Restored the original per-machine GUI layouts, slots, controls and status
  synchronization instead of a shared replacement interface.
- Restored original models, binary textures, item previews, GUI resources,
  animation, missile orientation and loaded-payload rendering.
- Restored cruise and ballistic launcher activation by hand, redstone,
  detonator and remote strike caller.
- Fixed Geran-2 catapult clearing, automatic and manual launch paths, remote
  collision fuse and operator self-damage.
- Fixed MQ-9 and aircraft target queues, weapon release, hardpoint rendering,
  flight, camera clipping and weapon-action jitter.
- Removed Geran-2 heightmap and broad-overlap airbursts during manual or resumed
  remote flight; designated coordinates no longer detonate it without contact.
- Stabilized MQ-9 and Geran-2 Remote Pilot camera prediction and aligned the
  chase-camera model with the same smoothed network pose.
- Restored staged forward chunk prefetch for Geran-2 and remotely piloted
  aircraft, and smoothed camera recovery after long chunk-build frames.
- Corrected MQ-9 mounted weapon heading while preserving the verified F-16,
  Su-27 and Tu-95 hardpoint transforms.
- Fixed Iskander trajectory, impact detection, warhead detonation and launch
  exhaust.
- Fixed Kh-555 stowed and in-flight orientation, route continuity and impact.
- Restored original missile, bomb, artillery and interceptor damage mappings,
  terrain destruction, fire, smoke, mushroom effects and sounds.
- Restored complete interceptor success, failure, malfunction and ground-impact
  effects.
- Restored Greg/Henry animation, target memory, sequential fire and projectile
  effects.
- Restored communication mast multiblock deployment, strategic radar structure
  and animated radar assemblies.
- Fixed NTM Extended compatibility crashes, dedicated-server class separation
  and Java 8 packaging.

### 1.12.2 Experimental Warning

The Minecraft 1.12.2 build is an **experimental compatibility port**. Automated
checks pass, but they cannot reproduce every modpack, client renderer,
dedicated server, multiplayer timing condition or migrated world.

Crashes and behavioral differences may still exist, including rendering,
camera, flight, collision, networking, weapon-effect and save-migration bugs.
Back up every world, test on a copy first and report problems with a crash
report, `latest.log`, screenshots or video and exact reproduction steps.

### Verification

- Minecraft 1.7.10 compatibility checks passed for HBM NTM X5751 and HBM NTM
  Space X5758 H261.
- Minecraft 1.12.2: 41 automated tests passed with zero failures.
- Forge reobfuscation, Java 8 bytecode validation, archive inspection and
  dependency-class scans passed.
- All 308 original PNG, OBJ, MTL and OGG assets used by the port are present
  under normalized 1.12.2 paths and byte-identical to the 1.7.10 source.

### Upgrade

1. Back up the world.
2. Stop Minecraft or the dedicated server.
3. Remove the original WarTech JAR and all older WarTech Reforged JARs.
4. Install exactly one build matching the Minecraft and dependency versions.
5. Use identical mod files on the server and every client.

---

# История изменений WarTech Reforged

## Не выпущено - backport Custom UAV на Minecraft 1.7.10

- Полностью перенесена модульная система Custom UAV dev44 с Minecraft 1.12.2
  на Forge 1.7.10: три планера, все 27 стабильных metadata модулей, NBT-чертежи,
  Конструктор, Программатор миссий, Пусковая точка, Fleet, Recon Report,
  Remote Pilot, задания, возврат, ремонт, падение и обломки.
- Сохранены серверная авторитетность, IFF, целеуказатели HBM/WarTech, энергия и
  взрывы HBM, РЛС/ПВО/РЭБ/ЛТЦ, авиационные подвесы, цепной мультидетонатор и
  ограниченный двухчанковый коридор полета.
- Перенесены утвержденные OBJ-модели, ударный TB2, текстуры, геометрия блоков,
  предпросмотр предметов, двуязычные книги, локализации и survival-рецепты.
- Добавлены воспроизводимая Java 8-сборка и contract smoke. Production Forge
  проверен с HBM NTM X5751 и NTM Space X5758 H261, двумя одновременными
  клиентами, spawn/render-синхронизацией, save/restart и старым миром 1.7.10.

## 1.6.0 - 2026-07-30

WarTech Reforged 1.6.0 выпускается в двух отдельных вариантах:

- **Стабильная версия:** Minecraft `1.7.10`, Forge `10.13.4.1614`, HBM NTM
  `1.0.27 X5751` или HBM NTM Space `X5758 H261`.
- **Экспериментальный порт:** Minecraft `1.12.2`, Forge `14.23.5.2860`,
  NTM Extended `3.0.3`.

Эти JAR не взаимозаменяемы. Никогда не устанавливайте обе сборки в одну папку
`mods`.

### Добавлено

- Полный порт каталога содержимого и механик WarTech Reforged 1.6 на Minecraft
  1.12.2: регистрации, рецепты, сущности, Tile Entity, интерфейсы, сеть,
  сохранения, модели, текстуры и звуки.
- Управляемые командные, радиолокационные, артиллерийские машины, комплексы РЭБ
  и ПВО с развёртыванием, постоянным инвентарём, энергией HBM и IFF.
- Patriot, С-400, «Тор-М1» и «Панцирь-С2» с эшелонированным обнаружением,
  резервированием целей и противоракетами разных тиров.
- Стратегическая РЛС, дальняя связь, реле воздушной тревоги и фракционная
  командная сеть.
- «Герань-2», MQ-9 Reaper, F-16C, Су-27, Ту-95, Х-555, авиационное вооружение,
  ЛТЦ, обломки и управление Remote Pilot.
- Совместимый с HBM артиллерийский целеуказатель и оригинальные очереди целей
  Greg/Henry.
- Миграция старого NBT техники, авиации, ракет, пусковых, РЛС, РЭБ, артиллерии,
  владельцев и инвентарей.
- Автоматические проверки реестров, ресурсов, NBT, управления техникой,
  попаданий ракет и содержимого JAR для порта 1.12.2.

### Изменено

- Наземная техника использует исходные максимальные скорости и тяжёлую динамику
  версии 1.7.10.
- Дистанционно управляемая авиация получила управление мышью, сглаженную
  телеметрию и носовую/внешнюю камеры.
- Крылатые, баллистические, противорадиолокационные и авиационные ракеты
  проверяют всю пройденную траекторию и не пролетают сквозь цель между тиками.
- «Панцирь-С2» использует ракеты Tier 1, «Тор-М1» - Tier 2, как в оригинале.
- Предпросмотр предметов использует оригинальные трансформации и текстуры без
  наложения цветного атласа Minecraft на OBJ-модели.

### Исправлено

- Восстановлена езда всей наземной техники: переназначенные клавиши,
  серверная обработка ввода и корректная синхронизация клиента.
- Возвращены отдельные оригинальные интерфейсы, слоты, кнопки и состояния всех
  систем вместо универсального заменителя.
- Возвращены оригинальные модели, бинарные текстуры, предпросмотр предметов,
  GUI-ресурсы, анимации, ориентация ракет и отображение боеприпасов.
- Восстановлен запуск крылатых и баллистических ракет рукой, редстоуном,
  детонатором и дистанционным пультом.
- Исправлены очистка катапульты «Герани-2», автоматический и ручной пуск,
  контактный взрыватель и урон оператору.
- Исправлены очереди целей, сброс вооружения, подвесы, полёт, камера и рывки
  MQ-9 и самолётов.
- Исправлены траектория, попадание, взрыв боевой части и стартовые эффекты
  «Искандера».
- Исправлены положение Х-555 в контейнере и полёте, маршрут и попадание.
- Возвращены оригинальные параметры урона ракет, бомб, артиллерии и
  противоракет, разрушение местности, огонь, дым, грибовидные эффекты и звуки.
- Возвращены эффекты успешного перехвата, промаха, отказа и падения
  противоракеты.
- Восстановлены анимации Greg/Henry, запоминание нескольких целей,
  последовательный огонь и эффекты снарядов.
- Восстановлены структура мачты дальней связи, стратегическая РЛС и вращение
  радарных установок.
- Исправлены краши совместимости с NTM Extended, разделение клиентского кода на
  dedicated server и упаковка под Java 8.

### Предупреждение для 1.12.2

Сборка для Minecraft 1.12.2 - **экспериментальный порт совместимости**.
Автоматические проверки проходят, но не заменяют тестирование в каждом
модпаке, на клиенте, dedicated server, в мультиплеере и на старых сохранениях.

Возможны краши и отличия поведения, включая ошибки текстур, камеры, полёта,
коллизий, сети, эффектов оружия и миграции сохранений. Сделайте резервную копию
мира и сначала тестируйте на его копии. К баг-репорту приложите crash report,
`latest.log`, скриншоты или видео и точные шаги воспроизведения.

### Проверка

- Проверки Minecraft 1.7.10 пройдены для HBM NTM X5751 и HBM NTM Space
  X5758 H261.
- Minecraft 1.12.2: пройдено 38 автоматических тестов без ошибок.
- Пройдены Forge reobfuscation, проверка байткода Java 8, содержимого архива и
  отсутствия упакованных классов зависимостей.
- Все 308 исходных PNG, OBJ, MTL и OGG, используемых портом, присутствуют в
  нормализованных путях 1.12.2 и побайтово совпадают с версией 1.7.10.

### Обновление

1. Сделайте резервную копию мира.
2. Остановите Minecraft или dedicated server.
3. Удалите оригинальный WarTech и все старые JAR WarTech Reforged.
4. Установите ровно одну сборку под нужные версии Minecraft и зависимостей.
5. На сервере и всех клиентах должны использоваться одинаковые файлы модов.

## Previous Releases / Предыдущие версии

- [1.5.1](docs/RELEASE_NOTES_1.5.1.md)
- [1.5.0](docs/RELEASE_NOTES_1.5.0.md)
- [1.4.0](docs/RELEASE_NOTES_1.4.0.md)
- [1.3.0](docs/RELEASE_NOTES_1.3.0.md)
