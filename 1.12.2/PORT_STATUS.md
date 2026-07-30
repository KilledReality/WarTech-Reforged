# WarTech Reforged 1.12.2 Port Status

## Release candidate

- Minecraft: 1.12.2
- Forge: 14.23.5.2860
- NTM Extended: 3.0.3
- WarTech build: `1.6.0-experimental`
- JAR: `WarTech-Reforged-1.12.2-NTM-Extended-1.6.0-experimental.jar`
- Source baseline: `1.6.0-dev29`
- Status: experimental compatibility port; live client and dedicated-server
  testing remains required.
- Size: `42,585,811` bytes
- SHA-256:
  `874AC8B1B346CBCF42E4D45D80434BFAEB56D527CB8B4EDF1A22C972EDAEE0E1`
- Automated suite: 38 tests, 0 failures, 0 errors, 0 skipped.

The release contains every dev1-dev29 port repair. Dev29 replaces the
manual Geran heightmap fuse with physical block/entity contact and prevents the
remote operator from damaging the Geran or MQ-9 through the camera-presence
entity. The clean release build, Forge reobfuscation, archive scan and Java 8
bytecode scan all pass.

## dev29 remote-aircraft collision repair

- Manual Geran flight detonates only on a swept block impact or a real entity
  contact; terrain height alone no longer causes an airburst.
- The remote operator is excluded from Geran and MQ-9 damage checks while the
  optical link is active.
- Two regression tests cover the physical-contact fuse and operator
  self-damage guard, bringing the forced suite to 38 tests.

## dev28 definitive GUI texture-cache repair

- The dev27 live run still showed the Minecraft item/block atlas mapped over
  MK-82 and the other OBJ item models.
- The actual cause was the bounds-measurement pass combined with
  `GL_ALL_ATTRIB_BITS`: OpenGL restored the atlas texture after measurement,
  while Minecraft's `GlStateManager` cache still believed that the WarTech PNG
  remained bound. The visible render therefore skipped `glBindTexture`.
- All three shared render entry points now use dev66's exact attribute mask
  `24833`, which deliberately excludes `GL_TEXTURE_BIT`.
- The bounds-only pass also skips `TextureManager` bindings entirely, so it
  cannot mutate Minecraft's texture cache.
- The reobfuscated bytecode contains `glPushAttrib(24833)` and no
  `GL_ALL_ATTRIB_BITS`. The packaged `mk82_bomb.png` is byte-identical to the
  source (`SHA-256 2DD4FA583E03104CE40350FC8576B8F51B992A7C38D94C1E6F3B5E2E548F7F5B`).
- Forced Gradle tests pass: 36 tests, 0 failures, 0 errors. The JAR contains
  289 Java 8 classes and no bundled Minecraft, Forge, NTM or test classes.

## dev27 GUI OBJ texture-state repair

- The dev26 live run disproved its ItemZoom fix: preserving Forge's camera
  transform did not repair the rainbow/atlas texture corruption.
- Custom OBJ item rendering now explicitly selects Minecraft's base texture
  unit before binding the original WarTech PNG. It can no longer bind the PNG
  to the lightmap unit while leaving the item/block atlas on the model.
- The inherited texture matrix is isolated and reset for the WarTech item
  render, then the previous active unit and matrix mode are restored for JEI,
  ItemZoom and the rest of the GUI.
- The original `geran2.png`, `entity_iskander_missile_tex.png` and every other
  model texture remain unchanged; the repair is entirely in the 1.12.2 OpenGL
  compatibility path.
- The subsequent live run disproved this diagnosis as sufficient. Dev28
  supersedes it with the GlStateManager cache repair.
- Forced Gradle tests pass: 36 tests, 0 failures, 0 errors. Forge
  reobfuscation and the final build pass.
- The JAR contains 289 Java 8 classes and no bundled Minecraft, Forge, NTM or
  test classes.

## dev26 vehicle-speed, item-preview and missile-impact repair

- The dev25 live run confirmed that command trucks and the other ground
  vehicles now move. Their original dev66 maximum speeds remain exact:
  command truck `0.36`, mobile artillery `0.38`, Tor `0.42` and Pantsir
  `0.46` blocks per tick.
- The driver no longer stretches each authoritative server position over
  another three client ticks. The current driver receives the exact server
  displacement immediately, removing the observed slow-motion presentation.
- Collision speed retention is now applied only after a movement attempt is
  actually blocked. A stale 1.12.2 `collidedHorizontally` flag can no longer
  reduce speed repeatedly on clear terrain.
- The item renderer keeps the actual Forge camera transform instead of forcing
  GUI perspective merely because an inventory screen is open. The subsequent
  live run showed that texture-unit corruption still remained; dev27
  supersedes this incomplete ItemZoom diagnosis.
- Cruise, ballistic, glide, anti-radiation, Geran (automatic and remote) and
  air-launched Kh-555 movement now ray-traces every high-speed step. Missiles
  cannot skip through the impact block between endpoints.
- Every concrete dev66 warhead mapping is covered by an automated detonation
  case audit. Iskander still uses its exact original `ExplosionLarge` strength
  `40`; the repaired swept impact path now reliably invokes it.
- Kh-555 yaw continuity is normalized across the `-180/180` boundary so the
  original model remains aligned with its flight vector during interpolation.
- Forced Gradle tests pass: 36 tests, 0 failures, 0 errors. Forge
  reobfuscation and the final build pass.
- The JAR contains 289 Java 8 classes and no bundled Minecraft, Forge, NTM or
  test classes.

## dev25 authoritative vehicle-control repair

- The dev24 live run disproved the event-bus-only diagnosis: the command truck
  still remained stationary.
- Inspection of the saved command truck confirmed zero persisted drive speed
  and motion. A fresh custom packet containing zero input could suppress the
  standard Minecraft rider input for that server tick.
- Server movement now combines the custom controller values with Minecraft
  1.12.2's native mounted-player input and uses the strongest value on each
  axis. A zero value from either route can no longer mask a pressed movement
  key from the other route.
- The vehicle controller is registered on both the FML and Forge event buses,
  with one-packet-per-player-tick deduplication.
- The server sends position, motion and rotation directly to the current
  driver every tick. Applying that state is scheduled on Minecraft's client
  thread and no longer depends on another client tick event.
- A short 20-tick packet-gap tolerance prevents a busy 86-mod runtime from
  cancelling throttle between accepted input packets.
- Forced Gradle tests pass: 33 tests, 0 failures, 0 errors. Forge
  reobfuscation and the final build pass.
- The JAR contains 289 Java 8 classes and no bundled Minecraft, Forge, NTM or
  test classes.

## dev24 live vehicle-input repair

- Corrected the root cause of the stationary vehicles. Forge 1.12.2 posts
  `ClientTickEvent` and `InputEvent` on `FMLCommonHandler.bus()`, but the
  vehicle controller had been registered only on `MinecraftForge.EVENT_BUS`.
  It therefore never sent the intended continuous client-to-server input.
- The vehicle input controller is now registered on the FML game-event bus.
  Remapped WASD input is transmitted every client tick while the player is in
  the driver's seat.
- The remote-flight controller is registered on both required event buses:
  FML for tick/input events and Forge for render events.
- Restored dev66's three-tick client position/yaw interpolation for command
  trucks, Tor/Pantsir and mobile artillery.
- Forced Gradle tests pass: 33 tests, 0 failures, 0 errors. Forge
  reobfuscation and the final build pass.
- The JAR contains 286 Java 8 classes and no bundled Minecraft, Forge, NTM or
  test classes.

## dev23 vehicle movement and GUI item centering

- Restored the original dev66 ground-vehicle step height of `1.1F`.
- Ground vehicles still use Minecraft's normal collision movement first. If
  that path incorrectly produces exactly zero horizontal displacement on
  otherwise clear terrain, a collision-checked fallback now applies the
  requested motion. The fallback checks both blocks and unrelated entities and
  cannot move a vehicle through walls.
- Fixed the 1.12 TEISR coordinate mismatch. Custom item models are now centered
  around the GUI renderer's actual `(0.5, 0.5, 0.5)` model-space center after
  their original dev66 transforms, so their fitted geometry stays inside the
  intended slot.
- Forced Gradle tests pass: 32 tests, 0 failures, 0 errors. Forge
  reobfuscation and the final build pass.
- The JAR contains 286 Java 8 classes and no bundled Minecraft, Forge, NTM or
  test classes.

## dev22 vehicle input and GUI item rendering

- Fixed an integer overflow in the age of the last custom driver-input packet.
  A vehicle with no accepted packet now immediately falls back to Minecraft's
  standard rider input instead of retaining zero throttle indefinitely.
- Server packets resolve the player's actual ridden WarTech entity first and
  validate both sides of the passenger relationship.
- Client input combines the configured key bindings with Minecraft's current
  movement input, preserving remapped controls.
- Custom OBJ items are measured after their exact dev66 presentation
  transforms, centered and normalized to 82% of one 1.12 GUI slot. Container,
  creative-tab, JEI and hotbar rendering can no longer spill into neighboring
  cells.
- Forced Gradle tests pass: 31 tests, 0 failures, 0 errors. Forge
  reobfuscation and the final build pass.
- The JAR contains 286 Java 8 classes and no bundled Minecraft, Forge, NTM or
  test classes.

## dev21 final static parity recovery

- Added 1.12.2 state mappers for launcher metadata and decoration facing so
  Forge no longer requests nonexistent model variants.
- Completed legacy NBT migration for aircraft, ordnance, AGM-88 route origins,
  radar/EW/PVO controls, mobile artillery, Greg/Henry ammunition, relay and
  strategic-radar tiles.
- Added a binary resource parity test. All 308 original PNG, OBJ, MTL and OGG
  assets are present under normalized 1.12.2 resource paths and are
  byte-identical to dev66.
- Clean Gradle `test build` and Forge reobfuscation pass. All 29 automated
  parity tests pass.
- The packaged JAR contains 286 Java 8 class files, 103 OBJ, 225 PNG, 16 MTL
  and 7 OGG resources. It contains no bundled Minecraft, Forge, NTM or test
  classes.
- Static source, registry, NBT, archive and resource audits found no remaining
  confirmed mismatch. Live Minecraft verification remains mandatory for
  timing, rendering, controls, networking, effects and save migration.

## dev15 Iskander flight and air-defence interception parity

- Ground-launched missiles now initialize their dev66 flight state before
  spawning, using the exact integer designator coordinates rather than
  rebuilding the route from synchronized float fields on the first tick.
- Restored the ballistic-base `soyuz` exhaust on every internal flight step,
  including the original count, width randomization and substep offsets.
- Restored the complete dev66 interceptor result effects: successful hits use
  the huge blast, eight large explosions, smoke, optional flame and the
  original high-volume explosion sound; misses use fizz, three small
  explosions and smoke.
- Restored the distinct abort, ground-impact and malfunction detonations with
  their original blast sizes, terrain/fire flags, sounds and particle counts.
- Restored stationary and mobile interceptor launch smoke, initial TOR/Pantsir
  velocities, and the original tier malfunction rates of 15%, 10% and 5%.
- The legacy code references `wartecmod:rocket_launch`, but no matching sound
  asset exists in dev66 or any other local WarTech JAR. No replacement audio
  was invented; all packaged legacy audio remains byte-identical.
- Clean Gradle `test build` and Forge reobfuscation pass; 9 tests pass.
- Packaged JAR contains 103 OBJ, 225 PNG and 6 OGG resources, with no bundled
  `com.hbm` classes.
- dev15 is the only active WarTech 1.12.2 JAR in `.minecraft/mods`; its
  installed SHA-256 matches the source artifact.
- Live verification is required for Iskander impact coordinates and all four
  interceptor outcomes.

## dev14 Greg/Henry, cruise sound and ballistic launch fixes

- Restored the dev66 FIFO target queue for Greg and Henry. They now retain
  multiple designator targets and remove only the target successfully fired.
- Restored the original Greg barrel recoil and Henry crane/tube movement,
  including the dev66 interpolation rates and model-part transforms.
- Restored Henry's original NTM `exKerosene` projectile trail and the legacy
  Greg muzzle burst. Phosphorus and thermobaric shell effects now use the
  original fire, potion, shrapnel, haze, mushroom and terrain constants.
- Registered the WarTech sound files as Forge `SoundEvent` objects. The cruise
  launcher now plays the byte-identical dev66 `CruiseMissileTakeoff.ogg`
  instead of falling back to Minecraft's firework sound.
- Replaced the inert ballistic launcher block with an `IBomb` implementation,
  allowing the NTM 1.12.2 detonator to invoke its existing loaded-missile launch
  path. Ballistic missiles retain the original HBM `weapon.missileTakeOff`
  sound.
- Clean Gradle `test build` and Forge reobfuscation pass; 9 tests pass.
- Packaged JAR contains 103 OBJ, 225 PNG and 6 OGG resources, with no bundled
  `com.hbm` classes.
- dev14 is the only active WarTech 1.12.2 JAR in `.minecraft/mods`; its
  installed SHA-256 matches the source artifact.
- Live verification is required for sequential Greg/Henry firing and each
  ballistic payload through the NTM detonator.

## dev13 effects, warheads, Geran and camera fixes

- Restored the dev66 VNT blast contract for all HE and thermobaric missiles:
  block destruction, no-drop processing, the original range multiplier,
  exposure-based entity damage and knockback now use the 1.7.10 constants.
- Restored the original `bombDet3`/`explosion_medium` sound split and copied
  the exact 1.7.10 `explosion_medium.ogg` asset.
- Replaced NTM Extended's broken opaque RBMK mushroom rendering with the
  dev66 additive-blend state while retaining the byte-identical HBM atlas.
- Restored the original `soyuz` exhaust effect and cruise-launch smoke/sound.
- Fixed stale client inventory entries when an update NBT omits an emptied
  slot. The Geran model now disappears from its catapult after launch.
- Restored the dev66 telemetry smoothing path. Nose-camera rendering suppresses
  the controlled external model, preventing MQ-9 geometry from clipping across
  the optical view.
- FAB-5000 and KAB-3000 again use the exact plain damage source, fire flag,
  terrain flag, radii and direct-damage constants from dev66.
- Clean `test build` and Forge reobfuscation passed; 9 tests pass.
- Packaged JAR uses Java class version 52, contains 103 OBJ and 225 PNG
  resources, and contains zero bundled `com.hbm` classes.
- Live verification is required for terrain damage, the additive mushroom,
  Geran clearing and the Reaper nose camera.

## dev12 detonator, target and remote-camera fixes

- Restored the original NTM detonator contract: every WarTech launcher block
  implements `com.hbm.interfaces.IBomb`, and detonation resolves any selected
  multiblock part back to the launcher core before firing.
- NTM detonator launches Geran-2 autonomously, while the launcher's manual
  remote action still transfers control to the player.
- New MQ-9, F-16C, Su-27 and Tu-95 entities no longer inherit the player's
  previously saved Target Finder coordinates. Their target queues start empty,
  matching dev66.
- Remote nose and chase cameras now use the exact interpolated render pose of
  the tracked aircraft or Geran. The render loop no longer rewrites the client
  entity position, removing the feedback loop that caused weapon-action jerks.
- Clean `test build` and Forge reobfuscation passed with 9 tests.
- Packaged JAR uses Java class version 52 and contains zero bundled `com.hbm`
  classes.
- dev12 is the only active WarTech 1.12.2 JAR in `.minecraft/mods`; its
  installed SHA-256 matches the source artifact.
- Live verification remains required for both NTM detonator launch paths,
  empty target queues on newly placed aircraft, and Reaper camera feel.

## dev11 VLS, aircraft target and remote-camera fixes

- Restored dev66 remote VLS lookup behavior: Strike Caller loads the launcher's
  surrounding 3x3 chunks before resolving the saved multiblock position.
- Redstone activation now resolves power across every block of the 1.12.2 VLS
  multiblock instead of depending on the update reaching its core.
- Geran launch immediately synchronizes the emptied drone slot, so the stowed
  model disappears from the launcher after a successful launch.
- MQ-9, F-16C, Su-27 and Tu-95 GUIs again show the actual dev66 target index
  and queue size instead of the hard-coded `1/1`.
- Restored dev66 three-tick aircraft interpolation and aligned the remotely
  controlled aircraft/Geran render transform with the smoothed camera frame.
- Clean `test build` and Forge reobfuscation passed with 8 tests.
- Packaged JAR uses Java class version 52 and contains zero bundled `com.hbm`
  classes.

## dev10 flight, artillery and VLS fixes

- Restored the dev66 mouse-control fallback to player rotation. Geran-2, MQ-9,
  F-16C, Su-27 and Tu-95 now receive mouse yaw/pitch on 1.12.2 even when the
  camera entity itself does not rotate.
- Added the removed HBM long-range artillery remote with its exact 1.7.10
  texture, deployed-artillery linking, persistent UUID/dimension binding and
  500-block target selection.
- Artillery designator targets select the original manual fire modes for Greg
  and Henry and are consumed as one-shot guidance targets.
- VLS and Geran launchers now charge from their battery slot, restoring the
  energy path required for launch.
- Strike Caller and Target Finder now intercept first-use on interactive
  launcher blocks, matching the 1.7.10 item interaction order.
- Loaded cruise missiles use their folded/stowed dev66 model sections instead
  of flight geometry, including folded Kalibr fins.
- Clean `test build` and Forge reobfuscation passed.
- Packaged JAR contains 103 OBJ and 225 PNG resources, Java class version 52,
  and zero bundled `com.hbm` classes.

## dev9 parity fixes

- Fixed the 1.12.2 driving regression: fresh `VehicleInputMessage` input now
  takes priority over the zeroed server-side vanilla rider fields.
- Restored exact dev66 vehicle/aircraft dimensions, health, destruction
  behavior, collision bounds, radar target tiers, and old-save health
  migration.
- Restored exact Pantsir/Tor behavior: Pantsir uses tier 1, Tor uses tier 2;
  launch cooldowns and launch decisions run every tick, radar sweeps keep their
  original cadence, damaged engagement range is reduced, and S-400 keeps its
  32-contact limit.
- Restored original L/S/X radar bands, S-400 scan height, and the 1,000,000 HE
  EW capacity.
- Restored exact jammer/ESM/decoy interaction behavior. Passive ESM no longer
  cycles a nonexistent operating band.
- Restored dev66 missile debris and rare-drop tables for every missile family,
  including original guidance-system and warhead drops.
- Restored dynamic aircraft radar classification and the loaded ballistic
  launcher payload render/synchronization.
- Clean `test build` and Forge reobfuscation passed with 8 tests.
- `build.gradle`, `mcmod.info`, and the compiled `@Mod` annotation all report
  `1.6.0-dev9`.
- Packaged JAR contains 103 OBJ and 224 PNG resources, Java class version 52,
  and zero bundled `com.hbm` classes.

## dev6 legacy-interface restoration

- Removed the rejected universal `GuiWarTechControl`,
  `ContainerWarTechControl`, and `WarTechControlPacket`.
- Restored dev66 GUI routing IDs 71-77 and per-system containers.
- Restored the original dev66 sizes, coordinates, colors, slot layouts, labels,
  and button action IDs for:
  - mobile radar and S-400 radar;
  - faction command vehicle map;
  - TOR-M1 and Pantsir-S2;
  - MQ-9, F-16C, and Su-27;
  - Tu-95;
  - communication mast;
  - strategic early-warning radar;
  - launch tube, VLS exhaust, and ballistic launcher;
  - handheld IFF selector.
- Bundled the original 1.7.10 HBM `gui_radar_nt.png` and retained the original
  WarTech `gui_launch_tube.png`.
- GUI buttons again use vanilla container action packets
  (`sendEnchantPacket` / `Container.enchantItem`) as in dev66.
- Added persistent entity/tile inventories, HE battery discharge, property
  synchronization, shift-click routing, and NBT persistence.
- Existing dev5 launcher tiles are migrated to the functional machine tile on
  first activation, so saved test worlds do not require replacing the blocks.
- Restored server-side radar contacts, IFF filtering, mobile-air-defense
  ammunition and HE consumption, moving-target interceptor guidance, aircraft
  mission launch/weapon release/return, deployed/travel vehicle states,
  redstone launcher activation, IFF selection, and direct EW/artillery
  interactions.

## Verification

- Gradle `clean test build`: passed.
- Forge reobfuscation: passed.
- Rejected universal GUI/control classes in packaged JAR: zero.
- Original radar and launch-tube GUI resources are present.
- Source artifact and installed `.minecraft/mods` copy have identical SHA-256.
- dev6 is the only active WarTech JAR in `.minecraft/mods`; dev1-dev5 are
  disabled.
- Real-client visual and interaction verification is pending the next game run.
