# Third-Party Notices and Release Rights

This document records the current origin and license status of material used by
WarTech Reforged. It is not a license for the project as a whole.

## Current publication candidate — October 9, 2026

The Modular Warfare Update candidate targets Minecraft 1.12.2 with **NTM
Community Edition 2.6.1.0** and **MixinBooter 10.7**, not the older Extended
branch described in historical sections. Dependencies remain external.
On October 9, 2026, the project owner confirmed that all permissions required
for public distribution of this update, including the original WarTech material
and supplied models, had already been obtained. This records the owner's
confirmation; it is not an independent legal verification or a blanket license.
Existing asset-specific attribution and license conditions remain applicable.

Custom cruise missiles now render their clean base bodies; historical dev55
accessory descriptions below record earlier derivatives, not a promise of
current visual modularity. Retained derivative resources still require their
applicable attribution. Bibliographic details not present in the workspace are
identified below rather than replaced with guessed authors or license grants.

## Original WarTech 1.1.1

WarTech Reforged contains restored and modified code and assets originating from
WarTech 1.1.1 (Warfare-Technology-Mod / `wartec`). The distributed WarTech 1.1.1
JAR contains no license file and its `mcmod.info` does not grant redistribution
or modification rights. The previously referenced source repository is no longer
publicly available.

**Permission status:** the project owner confirmed permission for modification
and public redistribution on October 9, 2026. The permission correspondence is
held by the owner rather than reproduced here. Original authorship is retained;
the absence of a license inside the old JAR is not represented as a new grant.

## HBM's Nuclear Tech Mod

HBM's Nuclear Tech Mod is an external dependency and is not bundled with WarTech
Reforged. Its official project is licensed under LGPL-3.0-only:

- https://modrinth.com/mod/ntm
- https://www.curseforge.com/minecraft/mc-mods/hbms-nuclear-tech-mod

The stable Minecraft 1.7.10 release targets HBM NTM `1.0.27 X5751` and HBM NTM
Space `X5758 H261`. The experimental Minecraft 1.12.2 port targets NTM Extended
`3.0.3`. These dependencies are external and are not included in either
WarTech Reforged release package.

## Verified Model Attribution

### Custom-cruise decorative modules (dev55)

- Original WarTech Reforged game-only accessory geometry: 28 separate OBJ files
  for optical/thermal/radar nose covers and economy/standard/fast/endurance
  engine shrouds. Added over credited bodies; no author faces or UVs removed.
- Surface patches reuse the existing UJ-32 Motor albedo by SpinoCactus,
  CC BY 4.0, attributed below. No texture artwork was generated/modified in dev55.
- Author body/wing/booster resources and political-insignia neutralizations remain
  unchanged from dev54. This decorative adaptation is not real weapon engineering.

### MIM-104 Patriot

- Creator: Muhamad Mirza Arrafi
- Source: https://sketchfab.com/3d-models/mim-104-patriot-surface-to-air-missile-sam-7a64d0af78514a159877edab1ab2bccb
- License: Creative Commons Attribution (CC BY)
- Changes: converted, scaled, optimized, and integrated into Minecraft.

### Storm Shadow Ukraine

- Creator: Viktor_ (Viktor Zhuravlev)
- Source: https://sketchfab.com/3d-models/storm-shadow-ukraine-d1ad04ce964f46dd9cbac6a64cd19b1b
- License: Creative Commons Attribution (CC BY)
- Changes: converted, scaled, optimized, reoriented, and integrated into Minecraft.
  In the 1.12.2 development resources, national insignia, flag-pattern lettering
  and handwritten political decoration were removed from bounded albedo regions.
  Technical warning stencils and neutral missile names were retained. Geometry
  and UV coordinates of the original mesh are unchanged; the author's attribution
  remains. Separate derived custom-cruise meshes regroup original faces and
  continuously vary outboard wing span/folding without removing faces or UVs.

### R-360 "Neptune" anti-ship cruise missile

- Creator: MrZeuglodon
- Source: https://sketchfab.com/3d-models/r-360-neptune-anti-ship-cruise-missile-a64c109936da417eb7ce4346a801a276
- License: Creative Commons Attribution 4.0 (CC BY 4.0)
- License URL: https://creativecommons.org/licenses/by/4.0/
- Input: user-provided `r-360-neptune-anti-ship-cruise-missile.zip`.
- Changes: selected one of the two duplicate showcase meshes, centred/reoriented
  an OBJ export for Minecraft, and exported the existing booster separately.
  National insignia and manufacturer emblems were removed locally from white
  and orange albedos; technical names and calibration marks remain. No original
  mesh detail was replaced by generated geometry. The editable neutral FBX and
  Blender file use only the neutral albedo materials.
- Status: integrated as the classic body in the dev50 custom-cruise test build.
  Runtime variants resize original wing/engine groups and detach the booster.

### UJ-32 Lastvika UAV — adapted light jet-drone airframe (dev53)

- Creator: **SpinoCactus**. Original model:
  https://sketchfab.com/3d-models/uj-32-lastvika-uav-3f681c071708442c91f54407acdd33aa
- License: Creative Commons Attribution 4.0 (CC BY 4.0):
  https://creativecommons.org/licenses/by/4.0/
- User-supplied archive `uj-32-lastvika-uav.zip`, SHA-256
  `27F50C733833358D347479B37F90FC08B947290E4A61C7D6D76916FE6EC674BA`.
- Changes: removed only author propeller (1,700 triangles) and piston motor
  (1,560 triangles); replaced them with an original decorative recessed game
  nozzle. Retained all 12,128 other author triangles and UVs, uniformly centered
  and scaled for the game. Main wing-span variants and three nozzle variants.
- Two wing roundels were locally inpainted with imagegen; all pixels outside
  two explicit circular masks, original alpha and native 2048 texture sizes
  are preserved. Technical numbers/stencils and author attribution retained.
- This is a fictional game adaptation, not a claim that the original UJ-32
  has a jet engine. This asset's CC BY license does not relicense the whole mod.

### Tupolev Tu-141 Strizh reconnaissance drone

- Dev52 also reuses the existing mesh at 1.45 game scale for a fictional heavy
  long-range missile body. It is not a newly acquired Flamingo model; the same
  attribution and noncommercial/share-alike restrictions apply.

- Creator: æck / aeck2142
- Source: https://sketchfab.com/3d-models/tupolev-tu-141-strizh-reconnaissance-drone-501d8fa00fd64ad69a3d1b08d2347d5c
- License: Creative Commons Attribution-NonCommercial-ShareAlike 4.0
  (CC BY-NC-SA 4.0), https://creativecommons.org/licenses/by-nc-sa/4.0/
- Input: user-provided `tupolev-tu-141-strizh-reconnaissance-drone.zip`.
- Changes: original evaluated mesh exported as centred/reoriented, uniformly
  scaled body/glass OBJ files; 4,750 original triangles and control-surface groups
  retained. Seven national stars removed locally from the original 4096² albedo;
  red technical number 05, weathering and author watermark text retained. Original
  glass texture unchanged. Editable neutral Blend retains the author's controls
  and packs the clean albedo materials; original archive/Blend preserved separately.
- Restriction: adapted mesh/texture remains under CC BY-NC-SA 4.0. Noncommercial
  use, attribution and ShareAlike apply to this material; commercial use requires
  separate creator permission. No additional restrictions are imposed on it.
- Status: integrated as the light body in the dev50 custom-cruise test build.
  Derived meshes regroup original faces and continuously vary wing span/folding,
  retaining all original faces/UVs. These adaptations remain CC BY-NC-SA 4.0.

### Geranium-2

- Creator: Karosio
- Source: https://sketchfab.com/3d-models/geranium-2-8f12f601efe44a73b47e30a7077cf249
- License: Creative Commons Attribution-NonCommercial-ShareAlike
  (CC BY-NC-SA)
- Changes: converted, scaled, optimized, reoriented, and integrated into Minecraft.
- Restriction: commercial use is not permitted without separate creator
  permission. Adapted model material must follow the ShareAlike requirement.

### HEMTT Lowpoly

- Creator: Calviking073
- Source: https://sketchfab.com/3d-models/hemtt-lowpoly-3a7c7f888dfe458a8274a300c8893f17
- License: Creative Commons Attribution (CC BY)
- Changes: converted, optimized, and adapted as a mobile artillery platform.

### Renault TRM Radar Truck

- Creator: Muhamad Mirza Arrafi
- Source: https://sketchfab.com/3d-models/renault-trm-radar-truck-1ad95e5724624a3bba7d36a686817efc
- License: Creative Commons Attribution (CC BY)
- Changes: converted, optimized, and adapted as a mobile radar vehicle.

### Electronic Warfare System Synytsia

- Creator: Khata / Khatagames
- Source: https://sketchfab.com/3d-models/electronic-warfare-system-synytsia-bd54fe1b2e9747ca887cf4a97577e13a
- License: Creative Commons Attribution (CC BY)
- Changes: converted, reoriented, material-atlased, scaled, and integrated as an
  active electronic jammer.

### BTS Antennas Asset (Parabolic & Sectoral)

- Creator: Astroregal
- Source: https://sketchfab.com/3d-models/bts-antennas-asset-parabolic-sectoral-5e7feb9be34f4a128b213b9d6d6069a7
- License: Creative Commons Attribution (CC BY)
- Changes: converted, combined, material-atlased, scaled, and integrated as a
  passive ESM array.

### CC0 Antenna

- Creator: plaggy
- Source: https://sketchfab.com/3d-models/cc0-antenna-6bc0ff4565db46ab8f7d229a5d272c12
- License: CC0 / Public Domain
- Changes: converted, reoriented, texture-reduced, scaled, and integrated as a
  radar decoy emitter.

### AGM-88 HARM

- Creator: hayfertepur1982
- Source: https://sketchfab.com/3d-models/agm-88-missile-065a61a8b6d349d3aed9c544e04f6bf2
- License: Creative Commons Attribution (CC BY)
- Changes: converted, reoriented, scaled, and integrated as an anti-radiation
  missile.

### 9K331 Tor-M1

- Sketchfab uploader: 42manako
- Model credit stated on the source page: DartShinigami
- Source: https://sketchfab.com/3d-models/9k331-tor-m1-3098265903054ae8b8bdc1536835c3db
- License: Creative Commons Attribution (CC BY)
- Changes: extracted from the downloaded archive, material groups separated,
  textures reduced, scaled, cached, and integrated as a mobile air-defense unit.

### 96K6 Pantsir-S2

- Sketchfab uploader: 42manako
- Source: https://sketchfab.com/3d-models/96k6-pantsir-s2-758055e673b543bd9ec3f504c8d60e8b
- License: Creative Commons Attribution (CC BY)
- Source-page note: the uploader states that the model is not their original model.
- Changes: converted from FBX to OBJ, object groups preserved, texture reduced,
  scaled, cached, and integrated as a mobile air-defense unit.

### MQ-9 Reaper Drone

- Creator: The Aesthetic Modeler (@racingrevved)
- Source: https://sketchfab.com/3d-models/mq-9-reaper-drone-game-ready-military-asset-a02057e7401a4f4ea130cb75cc73d8cb
- License: Creative Commons Attribution (CC BY)
- Changes: converted from Collada, source weapon meshes removed, selected parts
  optimized, textures reduced, and integrated as a reusable strike UAV.

### AGM-114 Hellfire

- Creator: xephoney
- Source: https://sketchfab.com/3d-models/hellfire-missile-208e1de6721e439cbb1666ec9a6517e5
- License: Creative Commons Attribution (CC BY)
- Changes: converted from FBX, scaled, cached, and integrated as MQ-9 ordnance.

### GBU-12 Paveway II

- Creator: NA3dmodel
- Source: https://sketchfab.com/3d-models/guided-bomb-unit-12-gbu-12-707162a980214a6e93735a0ce7c42668
- License: Creative Commons Attribution (CC BY)
- Changes: converted from Collada, geometry reduced, recolored, and integrated
  as MQ-9 precision ordnance.

### Mk 82 Bomb

- Creator: Thingits
- Source: https://sketchfab.com/3d-models/mk-82-bomb-b6bf6eab731d4f13a3c79ac0e5cc0d3f
- License: Creative Commons Attribution (CC BY)
- Changes: converted from glTF, scaled, cached, and integrated as an unguided
  MQ-9 payload.

### Transporter Erector (Rocket Launch Pad and Gantry)

- Creator: 3D Assets
- Source: https://3dassets.dev/assets/rocket-launch-pad-and-gantry-transporter-erector-33b1921f
- License: CC0 1.0 Universal
- Changes: converted from quantized glTF to the legacy OBJ renderer, recolored,
  extended to an eight-axle strategic TEL silhouette, scaled, and split into
  fixed chassis and animated erector groups.

### Tu-95 BEAR

- Creator: meminbiyikli
- Source: https://sketchfab.com/3d-models/tu-95-bear-8b535275901f421086bbd9fe7626878e
- License: Creative Commons Attribution 4.0 (CC BY 4.0)
- Changes: converted from glTF, geometry reduced, textures baked into a reduced
  atlas, original high-poly propellers replaced by lightweight animated runtime
  blades, scaled, cached, and integrated as a reusable strategic missile carrier.

### Russian X-555 air-launched cruise missile

- Creator: Dmitriy Mitroshin / LtxxwSibeRia
- Source: https://sketchfab.com/3d-models/russian-x-555-air-launched-cruise-missile-204c992ab27c4c1cad6a60b7c20b8c01
- License: Creative Commons Attribution-NonCommercial 4.0 (CC BY-NC 4.0)
- Changes: converted from glTF, geometry reduced, textures baked into a reduced
  atlas, scaled, cached, and integrated as a Tier 2 cruise missile.

## Additional Attribution Records

Public-distribution permissions for the entries in this section were confirmed
by the project owner on October 9, 2026. Some exact source-page/creator details
are not recorded in this workspace; do not mistake that absence for a claim
that the project created those imported models or owns unrestricted rights.

### Tactical aviation asset set

- Local packages: `f16-c-falcon.zip`, `sukhoi-su-27pu-ussr.zip`, and
  `missile-bomb-collection-fighter-jets-free.zip`.
- Used assets: F-16C, Su-27PU, AGM-114, GBU-12, HJ-10, AGM-65, Kh-29,
  KAB-500L, and JDAM.
- Distribution permission: confirmed by the project owner. Exact creator names,
  permanent source URLs and individual terms are held in the owner's records
  and have not been independently transcribed into this document.

### S-400 Triumf SAM system

- Creator: 42manako
- License shown on the source page: Creative Commons Attribution-NonCommercial
  (CC BY-NC)
- The exact permanent source URL is not recorded here; the recorded creator
  and NC conditions are preserved.
- Restriction: commercial use, including platform reward programs, should remain
  disabled unless the creator grants separate permission.

### S-400 long-range radar

- Local source archive: `s400-trioumf-radar-bonus-free.zip`
- Distribution permission: confirmed by the project owner. Exact creator,
  permanent source URL and license details are not transcribed here.

### Ural command vehicle

- Possible source: "Ural 4320" by Brout / davidbroutian under CC BY:
  https://sketchfab.com/3d-models/ural-4320-f953c51a5dbc4a15949f4dcc0905c4e8
- Distribution permission: confirmed by the project owner. The possible source
  above remains a possible match, not independently verified attribution.

### Geran catapult

- Original WarTech Reforged project asset.
- Original project geometry; no third-party license is granted by this entry.

### User-supplied Geran-5 v2 (local dev76)

- Supplied input: `geran5_minecraft_v2.glb`,
  explicitly supplied by the user for integration into this local mod.
- Source SHA-256: `DFFCB12460630EE7AC7DD7AF720883A6639CF87B461A3A294B6E88BC64879E34`.
- Runtime conversion retains 13,168 triangles, UVs and split normals; axis/scale
  adaptation only. Embedded albedo is exported without repainting. Legacy OpenGL
  uses the albedo, not the source PBR metallic/roughness shaders.
- A separate original WarTech rail-slide cradle supports the unchanged model.
- Public redistribution permission: explicitly confirmed by the project owner
  on October 9, 2026. External creator/source/license details were not independently
  established in this workspace; no invented attribution or blanket license is added.

## Monetization Warning

The current asset set includes CC BY-NC and CC BY-NC-SA material. CurseForge or
Modrinth rewards, advertising revenue tied to distribution, paid access, donor
perks, exclusive builds, and other commercial use should be disabled unless all
noncommercial assets are replaced or their creators grant explicit commercial
permission.

The project policy is that every build and feature remains freely available and
voluntary donations grant no access, advantage, early build, priority, or other
consideration. This supports the project's noncommercial intent, but it is not a
legal determination that every donation arrangement satisfies a Creative Commons
NonCommercial license. Creative Commons states that this assessment depends on
the purpose and circumstances of the specific use and the applicable jurisdiction.

