# WarTech Reforged Release Checklist

## 1. Rights Gate

- [ ] Obtain written permission to modify and redistribute WarTech 1.1.1.
- [ ] If the author cannot be reached, open a CurseForge support ticket before
      submission and request written moderation guidance for an abandoned,
      unlicensed, noncommercial preservation fork.
- [ ] Verify the exact S-400 radar source and license.
- [ ] Verify the exact Ural model source and license.
- [ ] Add the permanent S-400 launcher source URL.
- [ ] Keep CurseForge/Modrinth rewards disabled while CC BY-NC and
      CC BY-NC-SA models remain in the mod.
- [ ] Keep every download and feature free. Donations must grant no early access,
      exclusive files, gameplay benefits, priority, or other consideration.
- [ ] Put `THIRD_PARTY_NOTICES.md` inside the release JAR.
- [ ] Choose a project license only after all inherited-code rights are clear.

## 2. Clean Release Build

- [ ] Use version `1.2.1-hbm5751` or increment it if code/assets change.
- [ ] Build one clean JAR named `WarTech-Reforged-<version>.jar`.
- [ ] Verify that no original WarTech JAR, HBM JAR, `.bak`, crash report, model
      source archive, or private development file is bundled.
- [ ] Verify `mcmod.info`, displayed version, dependencies, and Creative tabs.
- [ ] Test a new single-player world and a dedicated server with matching clients.
- [ ] Test Patriot, S-400, all interceptor tiers, ballistic interception, Geran,
      Storm Shadow, both radar types, command post, Greg, and Henry.
- [ ] Test placing and removing every large 3D object while watching memory and FPS.
- [ ] Calculate SHA-256 for the final JAR and record it in release notes.

## 3. GitHub

- [ ] Create repository `WarTech-Reforged` after the rights gate is complete.
- [ ] Publish source, build instructions, `README.md`, this checklist,
      `THIRD_PARTY_NOTICES.md`, and a changelog.
- [ ] Do not commit Minecraft, Forge, HBM, or third-party binary dependencies.
- [ ] Tag the release as `v1.2.1-hbm5751`.
- [ ] Create a draft GitHub Release, attach only the clean JAR, add its SHA-256,
      dependency requirements, known limitations, and upgrade instructions.
- [ ] Enable GitHub Issues and provide templates for crashes and gameplay bugs.

## 4. CurseForge

- [ ] Create a Minecraft mod project named **WarTech Reforged**.
- [ ] Use the English section of `README.md` as the primary description; add the
      Russian section below it.
- [ ] Suggested English summary:

      `A restored and expanded HBM NTM missile-warfare addon with cruise missiles, layered air defense, mobile radars, command vehicles, drones, and mobile artillery.`

- [ ] Select Minecraft `1.7.10`, Forge, client and server support.
- [ ] Add HBM's Nuclear Tech Mod as a required dependency and state that the exact
      supported build is `1.0.27 X5751`.
- [ ] Upload the first public file as **Beta**, not Release.
- [ ] Upload only `WarTech-Reforged-1.2.1-hbm5751.jar`.
- [ ] State clearly that the original WarTech JAR must not be installed alongside it.
- [ ] Link the GitHub source repository and issue tracker.
- [ ] Disable project rewards/monetization while noncommercial model licenses remain.
- [ ] Add a square project icon and genuine in-game screenshots showing missiles,
      air defense, radar/command network, Geran catapult, and mobile artillery.
- [ ] Credit and link the original WarTech project/author after permission is obtained.
- [ ] Wait for moderation approval before announcing the download publicly.

## 5. Optional Modrinth Mirror

- [ ] Publish only after the same rights gate is complete.
- [ ] Use the same dependency, compatibility, attribution, and monetization terms.
- [ ] Keep one canonical changelog and matching SHA-256 across all platforms.

## 6. Release Announcement

- [ ] State that this is an unofficial restoration/fork based on WarTech 1.1.1.
- [ ] State the exact Minecraft, Forge, and HBM versions.
- [ ] Mark the build as beta and recommend world backups.
- [ ] Link the download page, source, issue tracker, and third-party notices.
- [ ] Never bundle or re-upload HBM NTM with the mod.
