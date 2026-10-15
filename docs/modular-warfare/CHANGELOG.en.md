# WarTech Reforged 2.0.0 — combined weapons and headquarters

Experimental release for Minecraft 1.12.2 / Forge 14.23.5.2860 /
HBM NTM Community Edition 2.6.1.0 / MixinBooter 10.7. Java 8.
Changes since public CE 1.7.0, including the completed gun-artillery integration.

## Strategic launchers and silos

- Topol-M and Oreshnik: animated mobile launchers, dedicated missiles,
  cold ejection and engine ignition after clearing the canister. Topol carries
  one nuclear reentry vehicle; Oreshnik carries seven conventional vehicles,
  including a central impact.
- Iskander-M: two missiles, two independent targets, designator import,
  physical launch sequences and stronger impact effects.
- Complete underground silo: 17×17 site, 32-block depth, entrance, continuous
  ladder, five levels, galleries, lights and an animated HBM hatch.
  The construction kit pays for the structure; excavation produces no drops.
- Silo launch readiness with smoke, signals and flashing lights while waiting
  for a command. Launch opens the hatch and performs cold ejection.
- Zaslon-4 silo ABM: external radar, dedicated interceptor, hatch and cold
  launch. It targets hostile Topol-M/Oreshnik missiles and their reentry vehicles
  from mobile or silo launches. A physical intercept remains conditional.

## MLRS, gun artillery and additional missiles

- Grad, HIMARS, Uragan and Smerch: animated vehicles, actual launch tubes,
  dedicated rockets and warhead variants. Rockets stack to 16.
  HIMARS supports independent targets for its six tubes.
- D-30 and CAESAR: smooth aiming, recoil and barrel return, flash, smoke,
  sound and an actual projectile at the muzzle. All 12 Greg shell types,
  four 16-round slots and 500 HE per shot. D-30: 80–1200 blocks, 7-second
  reload. CAESAR: 120–2200 blocks, 5-second reload and a drivable chassis.
- Artillery designators bind to a launcher's UUID. One accepted command fires
  one round; repeated aiming/reload clicks do not duplicate expenditure.
- FP-5 Flamingo launches from the Geran platform; FP-7 uses the standalone
  ballistic launcher, not the Iskander vehicle; S8000 Banderol uses compatible
  aircraft/UAVs and the cruise-missile launcher.

## Air defense, radar and vehicles

- Buk-M3 (tier 2), Osa (tier 1) and 9S18 join the shared network with Tor,
  Pantsir, S-400, Patriot, command vehicles and relays.
- Animated S-400/Patriot models and matching missile exits. Both require an
  external radar.
- Smooth tracking, interception preparation and battery coordination.
  Fixed stale occupied channels, stalls after large raids and mixed
  S-400/Patriot ammunition handling.
- Ground-vehicle panels for driving, deployment and operation. Acceleration,
  coasting, braking/reverse, parking brake, hull checks, safe exits, HUD,
  camera and free look. First-person cabins are simplified.

## Headquarters, operations and reconnaissance

- A main computer combines owned defense sites and command vehicles in the
  dimension. Defense, attack and reconnaissance maps retain physical power,
  sensor and communication requirements.
- Named, saved and copied operations; a guided
  vehicle → loaded ammunition → target → timing workflow, XYZ and designators.
- Preparation/takeoff, returns, accepted/emitted/completed dependencies,
  intervals, deadlines, failure policies, fresh-intel/readiness/link conditions.
- Nine workspaces: overview, diagnostics, timing, areas, reconnaissance,
  supply, battle review, equipment and access. Named circular areas, groups,
  ammunition reserves and reports based on actual outcomes.
- Commander/operator/observer roles, edit-version conflicts, pause and recall
  of unissued orders. Uncertain launches are not automatically spent again.
- Intelligence comes from owned/team UAV observations and their history.
  Repeat observation refreshes data; allied IFF grants no foreign map access.

## Industry and multiplayer

- 54 industrial parts and 322 direct recipes: 289 assembly, four soldering,
  12 crafting, 12 decorative packaging and five other machine operations.
  Unpacking, armor pressing and belt reloading are additional operations.
- Boards I–IV use the soldering station, vehicles and complex components the
  Assembly Machine, and remaining operations their appropriate HBM machines.
  Recipes are available in JEI.
- New equipment receives no free power or ammunition. Recovery preserves
  actual state, decoration preserves NBT, and downloaded intel costs paper.
  HBM recipe reloads retain WarTech recipes without duplicates.
- Level-2 operators assign teams. The server checks containers, dismantling,
  IFF, UAV stations, stale windows and network commands. Changing team revokes
  old-side access; observers cannot operate weapons.
- Multipart large plans, bounded headquarters packets and size errors without
  disconnecting. Narrow-terminal text wrapping and constrained columns.

## Retained features and release limits

The cruise constructor (44 parts, four bodies, 13 warheads), UAV constructor
(28 parts, three airframes), aviation, Geran-2/5, EW, passive ESM, HARM,
downloadable reconnaissance reports and missile shoot-down consequences remain.

Yars is disabled. Automatic supply delivery, full detailed cabins, polygonal
areas and a 1.7.10 update are outside this release. Supply means player tasks
and accounting, not autonomous cargo vehicles.

Validation: 620 unit tests on the release candidate; artillery acceptance has
82 server checks with restart and 17 RU/EN frames. Earlier dev113 passed 74
connected MP checks with two TCP clients and 150 ms added RTT, 3510 native checks
and 13 blast checks. These describe separate runs, not one combined campaign.
Complete ore-to-equipment survival, a long MP campaign, full-modpack load and
comprehensive migration of an old user world remain unverified. Back up worlds
and use matching 2.0.0 on all clients and the server.

[Detailed manual](GUIDE.en.md) · [Release validation](VALIDATION.md)
