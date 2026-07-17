# Electronic Warfare Model Candidates

This document records the model candidates selected for the WarTech Reforged
electronic-warfare update. Models are not included in the repository until their
download packages have been inspected and converted.

## Selected Candidates

### Mobile jammer module

**Electronic Warfare System "Synytsia"**

- Creator: Khata / Khatagames
- Source: https://sketchfab.com/3d-models/electronic-warfare-system-synytsia-bd54fe1b2e9747ca887cf4a97577e13a
- License: Creative Commons Attribution (CC BY)
- Downloadable: yes
- Geometry: approximately 10,500 triangles / 5,500 vertices
- Planned use: tactical jammer module mounted on the existing optimized Ural or
  HEMTT chassis.
- Assessment: best available candidate. It is small enough for Minecraft 1.7.10,
  has a clear license, and represents actual electronic-warfare equipment.

### Passive ESM antenna array

**BTS Antennas Asset (Parabolic & Sectoral)**

- Creator: Astroregal
- Source: https://sketchfab.com/3d-models/bts-antennas-asset-parabolic-sectoral-5e7feb9be34f4a128b213b9d6d6069a7
- License: Creative Commons Attribution (CC BY)
- Downloadable: yes
- Geometry: approximately 13,800 triangles / 7,100 vertices for the full set
- Planned use: modular directional receivers mounted on a custom mast above the
  existing Renault or Ural chassis.
- Assessment: the modular pieces can be separated and only the needed antennas
  retained, reducing the final render cost further.

### Radar decoy emitter

**CC0 Antenna**

- Creator: plaggy
- Source: https://sketchfab.com/3d-models/cc0-antenna-6bc0ff4565db46ab8f7d229a5d272c12
- License: CC0 1.0 public-domain dedication (the page also exposes the standard
  Sketchfab attribution label; retain creator credit as a courtesy)
- Downloadable: yes
- Geometry: approximately 446 triangles / 261 vertices
- Planned use: compact false-emitter antenna on an original project crate,
  tripod, battery, and cable assembly.
- Assessment: ideal for a cheap deployable decoy. The complete unit can remain
  well below 2,000 triangles after the original support geometry is added.

### Anti-radiation missile

**AGM-88 Missile**

- Creator: hayfertepur1982
- Source: https://sketchfab.com/3d-models/agm-88-missile-065a61a8b6d349d3aed9c544e04f6bf2
- License: Creative Commons Attribution (CC BY)
- Downloadable: yes
- Geometry: approximately 41,300 triangles / 20,900 vertices
- Planned use: visual base for the WarTech anti-radiation missile.
- Required optimization: inspect the original package, remove internal and
  duplicate geometry, decimate to approximately 4,000-6,000 triangles, bake a
  single 1024px texture, and verify fin orientation before integration.
- Assessment: acceptable only after aggressive optimization. If topology or
  textures are unsuitable, an original low-poly missile will be generated instead.

## Useful Alternatives

### High-Tech Military Radio/Radar

- Creator: Digital 3D Vision
- Source: https://sketchfab.com/3d-models/high-tech-military-radioradar-9b7dfce626a84f4698f55b594ee1ee8b
- License: CC BY
- Geometry: approximately 53,800 triangles
- Possible use: stationary command/ESM prop.
- Reason not selected: heavier and less clearly suited to a mobile system than
  the modular BTS antenna set.

### Antenna 3D game asset

- Creator: ryan007oliver
- Source: https://sketchfab.com/3d-models/antenna-3d-game-asset-522895d624db41699fcee784ee309918
- License: CC BY
- Geometry: approximately 4,900 triangles
- Possible use: secondary passive receiver or visual variation for the decoy.

### Military Vehicle Antenna PBR/SUBD

- Creator: EmrysRyan
- Source: https://sketchfab.com/3d-models/military-vehicle-antenna-pbrsubd-2c6aef0c0c3e47d6990f4325883527bb
- License: CC BY
- Geometry: approximately 12,000 triangles
- Possible use: smaller vehicle communication antenna.

## Rejected Candidates

- Commercial Krasukha-4 models from 3DOcean, CGTrader, TurboSquid, RenderHub,
  and similar marketplaces: not free/open model sources and generally too heavy.
- Sketchfab Krasukha-2 by ikhsanfahrip: no download, approximately 1.9 million
  triangles, unsuitable for runtime use.
- Oerlikon Skyguard radar: approximately 598,500 triangles.
- 1S12 Long Track radar: approximately 789,000 triangles.
- Duga radar: approximately 5.9 million triangles.
- Radar Antenna by Chfirchco: explicitly described as unoptimized and contains
  approximately 484,000 triangles.

## Planned Asset Pipeline

1. Preserve each original download archive and its Sketchfab license metadata
   outside the public repository.
2. Convert only the required meshes to OBJ/MTL.
3. Separate static chassis, rotating antenna, deployable mast, and support parts.
4. Remove invisible, internal, duplicate, and unused geometry.
5. Target at most 20,000 additional triangles for the complete jammer vehicle,
   15,000 for the passive ESM module, 2,000 for the decoy, and 6,000 for the
   anti-radiation missile.
6. Bake materials into one or two 1024px textures per unit.
7. Add frustum culling, render-distance checks, and cached display lists.
8. Record creator, permanent source URL, license, and modification notes in
   `MODEL_CREDITS.txt` and `THIRD_PARTY_NOTICES.md` before release.

## Recommended Visual Composition

- **Jammer:** existing Ural/HEMTT chassis plus the Synytsia module, a custom
  equipment enclosure, cable reels, and deployed supports.
- **Passive ESM:** existing Renault/Ural chassis plus a folding original mast and
  three or four selected sectoral antennas.
- **Decoy:** original low-poly crate and tripod plus the CC0 dish antenna.
- **Anti-radiation missile:** optimized AGM-88-derived exterior with a WarTech
  inventory icon and launcher-compatible orientation.
