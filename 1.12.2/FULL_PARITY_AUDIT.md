# WarTech Reforged 1.12.2: full parity audit

Audit date: 2026-07-30

## Baseline

- Original: `build/WarTech-Reforged-1.6.0-dev66-iff-save.jar`
- Decompiled original: `1.12.2/reference-dev66`
- Audited port: `1.12.2/WarTech-Reforged-1.12.2`
- Release port:
  `1.12.2/WarTech-Reforged-1.12.2-NTM-Extended-1.6.0-experimental.jar`
- Source baseline: `1.6.0-dev29`
- Target runtime: Minecraft 1.12.2, Forge 14.23.5.2860, NTM Extended 3.0.3

The audit compares active registrations, Java behavior, NBT and network
contracts, GUI containers, client rendering, recipes, sounds and binary
resources. A static comparison cannot prove timing-sensitive behavior inside a
real client or dedicated server, so confirmed differences and mandatory live
checks are listed separately.

## Important facts

1. All 308 PNG, OBJ, MTL and OGG resources from the original JAR are present
   byte-for-byte in dev21. Missing, black or displaced visuals are therefore
   caused by bindings, render transforms, block state, entity state or network
   synchronization, not by absent original files.
2. The active content catalog is complete at the registry-item level:
   77 original items plus 3 explicit 1.12.2 compatibility replacements, and all
   32 blocks.
3. The release candidate automated suite has 38
   catalog/resource/registry/NBT/gameplay-contract tests. It
   does not launch Minecraft and does not exercise flight, impact, GUI actions,
   cameras, animation, sound, multiplayer synchronization, NBT reload or
   dedicated-server behavior.

## Release assessment

The source and archive audits found no newly unaccounted binary asset or
registered content gap after dev29. The port is therefore suitable for a
clearly labelled experimental release and real-game testing. It is not promoted
to stable or declared proven full parity: the mandatory live matrix below still
requires side-by-side client and dedicated-server execution.

Dev29 specifically fixes false manual Geran airbursts by requiring swept block
or physical entity contact and excludes the linked operator from Geran/MQ-9
damage checks.

## Dev21 final static audit status

`1.6.0-dev21` adds the last confirmed compatibility fixes found by the repeated
source, NBT, resource and archive audit:

- launcher metadata and decoration facing are ignored by their 1.12.2 model
  state mappers, matching the available base blockstate variants;
- old aircraft, ordnance and AGM-88 route fields migrate to the current entity
  schema without discarding target, health, payload or flight state;
- old radar, EW, PVO, mobile-artillery, Greg and Henry state migrates to current
  flags, power, fire mode, ammunition and inventory fields;
- relay and strategic-radar tiles migrate old power, enabled and battery data;
- all 308 original binary assets are now checked byte-for-byte against dev66
  by the automated suite.

The clean reobfuscated build passes all 29 tests and contains only Java 8
WarTech classes plus its resources. No further confirmed static mismatch was
found. The live matrix remains the release gate because timing-sensitive
Minecraft behavior cannot be proven by archive or unit tests.

## Dev24 live-regression correction

The live dev23 run proved that the earlier collision diagnosis was incomplete.
The saved command truck had changed server position and yaw slightly, so
physical movement was not the primary blocker. Inspection of Forge 1.12.2's
actual event publisher found the root cause: `ClientTickEvent` and
`InputEvent` are posted on `FMLCommonHandler.bus()`, while the vehicle input
controller had been registered only on `MinecraftForge.EVENT_BUS`.

Dev24 registers continuous vehicle input on the correct FML game-event bus,
registers remote flight on both its required buses, and ports dev66's original
three-tick client vehicle interpolation. The forced suite passes all 33 tests,
Forge reobfuscation succeeds, and the archive contains 286 Java 8 WarTech
classes with no bundled runtime or test classes.

The subsequent dev24 live run still showed no continuous displacement, so
dev25 supersedes the event-bus-only diagnosis.

## Dev25 live-regression correction

The dev24 world save contains a retracted command truck with zero drive speed
and zero motion after the attempted drive. The remaining control flaw was
server-side input arbitration: a fresh zero from the custom channel could take
priority over Minecraft 1.12.2's native mounted-player input.

Dev25 combines both input sources per axis instead of selecting only one
source. It also registers vehicle input on both relevant event buses,
deduplicates sends to one packet per player tick, retains accepted input across
short packet gaps, and sends authoritative server position/motion/rotation to
the driver every server tick. Client state application is scheduled directly
on the Minecraft thread and does not depend on the input tick handler.

The forced suite passes all 33 tests, Forge reobfuscation succeeds, and the
archive contains 289 Java 8 WarTech classes with no bundled runtime or test
classes. The subsequent complete-client run confirmed continuous vehicle
displacement; dev26 addresses the remaining speed presentation issue.

## Dev26 live-regression correction

The dev25 run confirmed working ground-vehicle input and server displacement,
but authoritative driver positions were still fed into a new three-tick
interpolation window every server tick. Dev26 applies those driver-only state
packets immediately while retaining the original dev66 maximum speeds. It also
moves collision speed retention after the actual move result, preventing stale
1.12.2 collision state from repeatedly throttling clear-terrain motion.

The broken ItemZoom view was caused by overriding the requested item camera
transform whenever any GUI was open. Dev26 preserves the transform supplied by
Forge and the preview mod, but the dev26 live run proved that this was only one
part of the problem.

All powered non-interceptor missile movement families now perform swept block
collision checks between substep endpoints. This closes the high-speed
tunnelling path that allowed Iskander and other missiles to pass the impact
block without invoking their original payload. The detonation audit covers
every concrete payload mapping, including Iskander's original strength-40
`ExplosionLarge` call. Kh-555 also normalizes yaw continuity for correct model
interpolation through the `-180/180` boundary.

## Dev27 live-regression correction

The dev26 live screenshot showed the terrain/item atlas mapped across both the
small inventory icon and ItemZoom's enlarged OBJ. The remaining fault was
OpenGL texture state: a built-in item render could inherit the lightmap texture
unit and texture matrix from the surrounding GUI render.

Dev27 selects `OpenGlHelper.defaultTexUnit`, enables 2D texturing and resets the
texture matrix before rendering any custom WarTech item. It restores the
previous active unit and matrix mode afterward, so ItemZoom, JEI and other GUI
mods keep their own state. No original model or PNG was modified. The forced
suite passes all 36 tests, Forge reobfuscation succeeds, and the archive
contains 289 Java 8 WarTech classes with no bundled runtime or test classes.

The subsequent live run still showed the atlas texture, disproving dev27 as a
complete fix.

## Dev28 live-regression correction

The remaining defect was a cache mismatch created by the measurement render.
The port used `GL_ALL_ATTRIB_BITS`, so `glPopAttrib` restored the real OpenGL
texture binding to the Minecraft atlas without restoring the parallel
`GlStateManager` texture cache. The visible pass asked to bind the same WarTech
PNG, the stale cache suppressed the actual `glBindTexture`, and the OBJ was
drawn with the atlas.

Dev28 restores dev66's exact `24833` attribute mask, which excludes texture
bindings, and makes the bounds-only pass skip all texture-manager calls. The
reobfuscated bytecode contains the original mask and no `GL_ALL` push. The
packaged MK-82 texture is byte-identical to the source. All 36 tests and Forge
reobfuscation pass; the archive contains 289 Java 8 WarTech classes.

The forced suite passes all 36 tests, Forge reobfuscation succeeds, and the
archive contains 289 Java 8 WarTech classes with no bundled runtime or test
classes. These four live regressions require confirmation in the complete
client before release status.

## Dev23 live-regression correction

The live dev22 run initially appeared to indicate a collision problem because
the engine sound and throttle state reacted without visible continuous
movement. Dev23:

- restores the original dev66 `1.1F` step height;
- keeps normal collision resolution as the primary movement path;
- applies requested horizontal displacement only when normal movement returns
  exactly zero and an independent block/entity collision check confirms the
  destination is clear;
- corrects GUI item normalization for 1.12's TEISR translation by centering
  fitted item geometry on `(0.5, 0.5, 0.5)`.

The forced suite passed all 32 tests and archive checks. The next live run
showed that these changes did not repair continuous input; dev24 supersedes
this diagnosis.

## Dev22 live-regression correction

The first live dev21 run exposed two issues not observable through archive
parity:

- the initial driver-input age overflowed a signed integer, suppressing the
  standard Minecraft rider-input fallback;
- the 1.12 built-in item renderer applied the original presentation
  transforms without fitting their resulting geometry to a GUI slot.

Dev22 uses overflow-safe input age handling, resolves control packets through
the player's current mount, and automatically measures, centers and fits every
custom OBJ item after its original transform. The forced suite passes all 31
tests and the reobfuscated archive checks pass.

## Dev16 resolution status

The first seven requested findings from the user-facing audit were corrected
in `1.6.0-dev16`:

- all 43 active dev66 entity identities are registered through unique concrete
  wrapper classes, while dev15 generic names remain as migration aliases;
- all 16 original tile identities are registered and new blocks create the
  matching concrete tile wrapper;
- original per-entity tracking ranges and update frequency 1 are restored;
- original entity and relevant tile render-distance overrides are restored;
- launch tube and ballistic launcher GUI IDs are restored to 1 and 2;
- radar entity containers now detect blip, fire-mode and hardpoint changes
  independently of power, state and contact count.

These items remain in the historical audit below as the evidence and rationale
for the fixes. Runtime verification in Minecraft is still required.

## Dev18 resolution status

`1.6.0-dev18` closes the remaining static findings and the three visual/recipe
tails that were still present after dev17:

- decoration blocks once again use horizontal metadata-compatible facing,
  invisible/non-opaque TESR block semantics, zero default hardness/resistance
  and the original default block sound;
- MQ-9 state 6 and Tu-95 state 8 use the exact dev66 wreck attitudes;
- the U-238 assembler recipe once again outputs two plates;
- a Forge entity/block-entity data fixer maps old unnamespaced dev66 IDs to
  the concrete 1.12 registrations, and launcher tiles read/write the old
  `power`, `items`, `openanim`, `open` and `shoot` keys;
- unknown visual profiles are reported and left unrendered instead of silently
  substituting Storm Shadow;
- the dormant fragmentation entity remains registered as original ID 24;
- the broken null strong-antiballistic recipe is no longer redirected to the
  distinct nuclear interceptor;
- the artillery designator accepts both dev18 UUID links and original
  coordinate-only links;
- ASAT satellite removal and neutron contamination call the exact NTM 3.0.3
  APIs directly, with a startup capability gate instead of swallowed
  reflection failures;
- the unresolved `wartecmod:rocket_launch` event remains silent because dev66
  contains no corresponding OGG; no replacement sound was invented;
- reinforced wood and decoration mining/blast values match dev66.

The historical entries remain below as audit evidence. The mandatory live
matrix is still required because static tests cannot drive a Minecraft client,
integrated server or dedicated server.

## P0: confirmed gameplay and protocol differences

### WT-PARITY-001: entity registry is collapsed

The original registers 43 concrete entity types with their own names, numeric
IDs and tracking settings. The port registers six generic types:
`missile`, `aircraft`, `ordnance`, `ground_vehicle`,
`artillery_projectile` and `satellite_missile_nuclear`.

References:

- Port: `port/entity/WarTechEntityRegistration.java:13-30`
- Original: `entity/wartecmodEntities.java:40-65` and the entity registrations
  in `compat/*Content.java`

Impact: the runtime no longer has the original entity identity contract.
Commands, integrations, spawn data, save compatibility and per-entity network
settings can behave differently even when a profile emulates the same missile.

Required fix: restore one registered 1.12.2 entity identity per active original
entity, or add explicit aliases/data fixers plus exact per-profile network and
serialization contracts.

### WT-PARITY-002: tile entity registry is collapsed

The original has concrete tile registrations for VLS tube and exhaust,
ballistic launcher, Geran launcher, Greg, Henry, Patriot, S-400, relays,
strategic radar and decoration tiles. The port registers only
`wartech_machine` and `wartech_visual`.

References:

- Port: `WarTechReforged.java:56-63`
- Original: `tileentity/TileEntityRegistry.java:16-18`,
  `blocks/wartecmodBlocks.java:105-109` and `compat/*Content.java`

Impact: original tile IDs and concrete NBT contracts are not preserved. This
also makes direct world migration impossible without a data fixer.

### WT-PARITY-003: entity tracking ranges and update frequency are reduced

The port uses ranges 160-512 and gives ground vehicles update frequency 2.
The original uses 1000 for missiles, 1200-1400 for drones and orbital
ordnance, 256-768 for vehicles and 12288 for strategic/tactical aviation, all
with update frequency 1.

References:

- Port: `port/entity/WarTechEntityRegistration.java:15-23`
- Original: `entity/wartecmodEntities.java:40-65`,
  `compat/DroneStrikeContent.java:46-47`,
  `compat/RadarNetworkContent.java:92-97`,
  `compat/StrategicAviationContent.java:47-49`,
  `compat/TacticalAviationContent.java:37-38`

Impact: remote aircraft, long-range missiles and strategic bombs can disappear,
update late or jitter. This is a direct candidate for the reported MQ-9/Geran
camera and model instability.

### WT-PARITY-004: original render-distance overrides are missing

Only the satellite missile has a custom render-distance override in the port.
The original separately extends render range for cruise and ballistic missiles,
MQ-9, Tu-95, strategic bombs and EW units.

References:

- Port: `port/entity/EntitySatelliteMissileNuclear.java:63`
- Original: `EntitySubsonicCruiseMissileBase.java:272`,
  `EntityBallisticMissileBase.java:233`, `EntityMq9Drone.java:1816`,
  `EntityTu95Bomber.java:1881`, `EntityStrategicBomb.java:326`,
  `EntityElectronicWarfareUnit.java:254`

Impact: entities may stop rendering well inside their original visible range,
independently of server tracking.

### WT-PARITY-005: launcher GUI numeric protocol differs

The original reserves GUI ID 1 for the VLS launch tube and ID 2 for the
ballistic launcher. The port uses 0 and 1. IDs 71-77 match the original.

References:

- Port: `port/gui/WarTechGuiHandler.java:13-14`
- Original: `handler/WartecmodGUIHandler.java:39-51,65-77`

Impact: callers using the original IDs can open the wrong GUI or no GUI.

### WT-PARITY-006: radar contacts are not synchronized while moving

`ContainerLegacyEntity.detectAndSendChanges()` sends packed radar blips only
when power, state, contact count, flags or payload changes. If the same contacts
move while their count stays constant, clients receive no new coordinates.
The original compares and synchronizes all 16 packed blips and strategic radar
warmup independently.

References:

- Port: `port/gui/ContainerLegacyEntity.java:91-126`
- Original: `compat/ContainerRadarVehicle.java`

Impact: frozen radar blips, stale target coordinates and non-equivalent
strategic-radar state.

### WT-PARITY-007: explosion implementation is not the original algorithm

The original `ExplosionLargeAdvanced` configures NTM `ExplosionVNT` with
`BlockAllocatorStandard(48)`, a no-drop block processor,
`EntityProcessorCross(7.5).withRangeMod`, a player processor and the standard
VNT effect. The port uses vanilla `newExplosion`/`ExplosionNT` plus a custom
exposure and damage approximation.

References:

- Port: `port/integration/HbmExplosionCompat.java:127-205`
- Original: `entity/logic/ExplosionLargeAdvanced.java:53-83`

Impact: crater shape, block destruction, drops, entity/player damage,
knockback and effect timing differ. This affects HE, thermobaric, buster,
Geran, artillery and other profiles routed through this helper.

### WT-PARITY-008: EMP satellite cloud effect is replaced

The original spawns `EntityNukeCloudSmall` at altitude 600 and then applies
EMP. The port replaces the cloud entity with 48 vanilla `CLOUD` particles.

References:

- Port: `port/satellite/SatelliteEmp.java:35-48`
- Original: `savedata/satellites/SatelliteEmp.java:49-57`

### WT-PARITY-009: aircraft mission-launch sound is absent

MQ-9, tactical aircraft and Tu-95 use
`hbm:weapon.missileTakeOffAlt` in the original, with aircraft-specific volume
and pitch. The port's `launchMission()` does not play it. NTM 1.12.2 omits the
sound, but the exact original OGG exists in the local 1.7.10 HBM JAR and can be
registered under the WarTech namespace.

References:

- Port: `port/entity/EntityWarTechAircraft.java:983-1076`
- Original: `EntityMq9Drone.java:518,1262`,
  `EntityTu95Bomber.java:482,1241`

### WT-PARITY-010: aircraft weapon-release sounds are replaced

The original uses `hbm:weapon.missileTakeOff` for powered payloads and
`random.pop` for unpowered MQ-9 payloads, with exact per-path volume and pitch.
Tu-95 uses its own launch values. The port uses generic firework and egg sounds.

References:

- Port: `port/entity/EntityWarTechAircraft.java:2568-2586`
- Original: `EntityMq9Drone.java:875,923`,
  `EntityTu95Bomber.java:1029`,
  `EntityTacticalAircraft.java:178`

The original `techBoop`, `techBleep`, link, selection and lifecycle sound
matrix is also not fully reproduced.

### WT-PARITY-011: item rendering is globally auto-fitted

The port routes custom items through one TEISR and scales every model to
`0.86 / largestSize`. The original has item-specific `IItemRenderer`
implementations with separate inventory, entity, equipped and first-person
transforms.

References:

- Port: `port/client/LegacyRenderLibrary.java:271-314`
- Original: `compat/client/ItemRender*.java` and original item renderers

Impact: incorrect icon scale and alignment in creative tabs, inventory, world
and hand. This is the confirmed cause class for models that are too small,
too large or outside their cells.

### WT-PARITY-012: decoration blocks lost original block-state behavior

Original decoration blocks are non-opaque, non-normal-rendering tile blocks,
store horizontal placement facing in metadata and rotate the OBJ for metadata
2/3/4/5. `PortBlock` does not preserve this facing property or those block
semantics, and its renderer uses one transform.

References:

- Port: `port/content/PortBlock.java:24-80`,
  `port/client/LegacyRenderLibrary.java:1217-1280`
- Original: `blocks/deco/DecoBlock.java`,
  `render/tileentity/RenderTileEntityDecoBlock.java`

Impact: black/full-cube faces, wrong orientation, collision/occlusion
differences and structure visuals that do not match 1.7.10.

### WT-PARITY-013: destroyed-aircraft render attitudes are missing

The original MQ-9 renderer applies a wreck rotation in state 6 and the Tu-95
renderer applies a wreck rotation in state 8. The corresponding branches are
absent from the generic renderer.

References:

- Port: MQ-9/Tu-95 branches in
  `port/client/LegacyRenderLibrary.java:135-165`
- Original: `compat/client/RenderMq9Drone.java`,
  `compat/client/RenderTu95Bomber.java`

### WT-PARITY-014: U-238 assembler output count is wrong

The original recipe converts three U-238 ingots into two plates. The port uses
the assembler helper's default output count of one.

References:

- Port: `port/integration/WarTechRecipeRegistration.java:322`
- Original: `inventory/wartecmodAssemblerRecipes.java:54`

### WT-PARITY-015: original NBT identity is not migration-compatible

Generic entity and tile classes use shared profile-oriented NBT rather than the
original concrete entity/tile schemas. No aliases or Forge data fixer migrate
the original registry IDs and keys.

Impact: full parity for new worlds may be achievable, but direct loading of
1.7.10 WarTech entities and tiles is not currently supported.

## P1: confirmed structural or fallback differences

### WT-PARITY-016: unknown visuals silently render as Storm Shadow

The generic renderer falls back to the Storm Shadow model for an unknown or
malformed visual profile. The original had concrete renderer registration and
could not silently substitute an unrelated missile this way.

Reference: `port/client/LegacyRenderLibrary.java:247,441`

Required fix: enforce complete profile-to-renderer coverage and show a
development error for an unknown profile.

### WT-PARITY-017: one registered original entity is absent

`EntityCruiseMissileFragmentation` is registered as entity ID 24 in the
original but has no port profile/registration. It appears dormant because no
active original item or launcher path constructs it, so this is catalog
structure parity rather than a confirmed missing player-facing weapon.

Original source also contains unregistered Sarmat and GBSD classes. They are
dormant source, not active `dev66` gameplay, and should not be added unless the
scope explicitly includes inaccessible code.

### WT-PARITY-018: anti-ballistic progression is intentionally changed

The original declares but never initializes its strong anti-ballistic item and
contains a broken recipe path. The port maps this progression to the WarTech
nuclear anti-ballistic successor.

This is a sensible bug fix but not literal parity. A final decision is needed:
reproduce the broken original behavior or retain the repair and document it as
an approved deviation.

### WT-PARITY-019: three compatibility replacement items are port-specific

`designator_arty_range`, `ArtilleryAmmo` and `HimarsAmmo` replace APIs/items
that are absent from NTM Extended 1.12.2. Their existence is necessary, but
recipes, tooltips, linking behavior, range selection and ammo consumption must
be tested against the original workflows rather than only against static
constants.

### WT-PARITY-020: reflective NTM integrations can silently degrade

ASAT satellite deletion, contamination and several optional HBM API paths use
reflection and catch broad failures. If an NTM 3.0.3 symbol changes, the action
can silently become a no-op. The ASAT path can reach altitude and disappear
without deleting the target satellite.

Reference: `port/entity/EntityWarTechMissile.java:1336`

Required fix: validate required capabilities at startup, log hard failures and
cover every reflective path against the exact target NTM JAR.

### WT-PARITY-021: original PVO launch sound reference is unresolved

The original requests `wartecmod:rocket_launch`, but no matching OGG exists in
any local original WarTech JAR. This is an inherited original defect, not a
missing copied file. Literal parity means preserving the unresolved/silent
event; functional parity needs an authentic source asset rather than an
invented replacement.

### WT-PARITY-022: generic block material values differ

The shared 1.12.2 `PortBlock` assigns common hardness/resistance values to
decoration blocks, and reinforced wood has an explicit hardness absent from
the original definition. These values affect mining time, blast resistance
and tool behavior.

## Mandatory live parity matrix

The following are not declared fixed merely because their source branches
exist. Every row needs side-by-side 1.7.10/1.12.2 execution with recorded
inputs, output state, screenshots and logs.

### Launchers and target flow

- VLS launch tube, exhaust and ballistic launcher: build, validation,
  inventory, power, GUI ID, launch animation, reload and break/salvage.
- Geran launcher: manual launch, NTM detonator launch, model clearing, repeated
  launch, NBT reload and target queue.
- Target Finder, Strike Caller and artillery range designator: set, replace,
  append, select, consume, clear and reload multiple targets.
- Redstone, GUI, remote and detonator launch paths across loaded and unloaded
  chunks.
- Iskander: takeoff effects, climb, terminal descent and impact coordinates at
  short/long range, varying terrain height and chunk boundaries.
- Greg and Henry: original animation phases, FIFO queue, consecutive targets,
  ammunition consumption, smoke/trails, save/reload and structure damage.

### Missile and bomb families

- Cruise: HE, cluster, buster, EMP, thermobaric, nuclear and H.
- Supersonic: HE and H; hypersonic: HE and nuclear; LRHW and SLBM.
- Micro gas, neutron, anti-air tiers 1/2/3 and nuclear anti-ballistic.
- Tomahawk, Kalibr, CJ-10, Iskander, ASAT, Storm Shadow, Geran,
  anti-radiation and Kh-555.
- Strategic and tactical bombs including FAB-5000: exact hardpoint transform,
  drop trajectory, damage, terrain destruction, smoke, mushroom and sound.
- For every payload: direct hit, ground hit, entity hit, water, protected
  blocks, friendly target, interception and maximum-range timeout.

### Aircraft and remote control

- MQ-9, F-16, Su-27, Tu-95 and Geran mouse yaw/pitch, throttle, takeoff,
  landing, collision, destruction and despawn.
- Nose/chase cameras at low and high speed, weapon release, target selection,
  packet delay, other players nearby and long tracking distance.
- All hardpoints: item compatibility, visual attachment, selected slot,
  release order and post-release model removal.
- Empty, one-target and multi-target GUI states; no automatic phantom target.
- Remote link recovery after GUI close, death, dimension change, disconnect
  and world reload.

### Ground systems and PVO

- Radar truck, S-400 radar, command truck, TOR, Pantsir, all EW variants,
  Greg and Henry: WASD, reverse, steering, collisions, mounting and dismounting.
- Deploy/retract on shift-right-click, rotating radar parts and all original
  animation timing.
- Correct per-vehicle GUI, slots, shift-click rules, power, radar state,
  targeting, team and NBT persistence.
- TOR/Pantsir/S-400/Patriot tier, ammunition, range, health and power tables.
- PVO interception: launch, trail, smoke, sound, hit, miss, abort, malfunction,
  ground crash, debris, explosion and fire.
- Radar/IFF/EW/decoy/anti-radiation behavior with multiple teams and players.

### Structures, satellites and persistence

- Communication mast and strategic radar complete structure placement,
  orientation, validation, break cascade and salvage.
- Nuclear, EMP and kinetic satellites through NTM's satellite GUI and save
  data, including ASAT removal and ODIN integration.
- Save/reload every entity, tile, inventory, queue, deployment/animation state,
  team and target list.
- Dedicated server with two clients, cross-dimension use, chunk tickets,
  long-flight continuation and ticket cleanup.
- JEI/assembler/press/shredder/smelting recipes, hazards, stack sizes,
  creative tabs and all translated tooltips.

## Fix order

1. Restore exact tracking ranges, update frequencies and render-distance
   overrides.
2. Repair radar/contact synchronization and launcher GUI protocol.
3. Replace global item auto-fit with the original per-item render transforms;
   restore decoration block facing and non-opaque semantics.
4. Restore the original aircraft sound matrix and EMP/PVO effects.
5. Port `ExplosionLargeAdvanced` onto the NTM 1.12.2 VNT API and verify every
   payload's damage/destruction.
6. Correct recipes and remove silent renderer/integration fallbacks.
7. Decide the registry/NBT migration strategy, then run the complete live
   parity matrix and fix failures in subsystem order.

## Definition of full parity

The port is not full parity until:

- every confirmed item above is fixed or explicitly approved as a documented
  compatibility deviation;
- every live-matrix scenario passes side-by-side against `dev66`;
- client, integrated server and dedicated server all pass;
- no original active asset, registry entry, GUI action, recipe, effect, sound,
  animation, state transition or save/network contract remains unaccounted for.
