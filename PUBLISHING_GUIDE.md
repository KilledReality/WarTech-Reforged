# WarTech Reforged: Publishing Guide

## Files to Publish

### Mod file

- File: `WarTech-Reforged-1.2.1-hbm5751.jar`
- Current local path:
  `C:\Users\kiril\AppData\Roaming\.minecraft\mods\WarTech-Reforged-1.2.1-hbm5751.jar`
- SHA-256:
  `B81F01D5E8832733CECBBCBDB32EDAE007CE8165A5B6440A7025321975D82BB9`
- Size: approximately 26.1 MB.

Do not upload or bundle any of the following:

- `HBM-NTM-.1.0.27_X5751.jar`
- `WarTec-1.1.1.jar`
- old WarTech Reforged JARs
- crash reports
- `.bak` files
- `models_inbox`
- original `.blend`, `.fbx`, `.glb`, `.gltf`, `.usd`, or `.usdz` packages

## GitHub Repository

### 1. Create the empty repository

1. Open https://github.com/new.
2. Repository owner: your GitHub account.
3. Repository name: `WarTech-Reforged`.
4. Description:
   `Restored and expanded missile warfare for HBM NTM on Minecraft 1.7.10.`
5. Visibility: `Public`.
6. Do not add a README, `.gitignore`, or license on GitHub. They already exist
   locally or are intentionally handled separately.
7. Click `Create repository`.

### 2. Publish the local source

Open PowerShell in `C:\Users\kiril\Documents\htm dop` and run:

```powershell
git add .gitignore README.md PUBLISHING_GUIDE.md RELEASE_CHECKLIST.md THIRD_PARTY_NOTICES.md src
git status
git commit -m "Initial WarTech Reforged public release"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/WarTech-Reforged.git
git push -u origin main
```

Replace `YOUR_USERNAME` with the actual GitHub account name. Before the commit,
`git status` should not list HBM, original WarTech, crash reports, build output,
or `models_inbox` as staged files.

If Git asks for author details:

```powershell
git config user.name "YOUR_PUBLIC_NAME"
git config user.email "YOUR_GITHUB_EMAIL"
```

### 3. Configure the repository

In `Settings`:

- Enable `Issues`.
- Disable `Wiki` unless it will actually be maintained.
- Add topics: `minecraft`, `minecraft-mod`, `forge`, `minecraft-1-7-10`, `hbm`,
  `missiles`, `air-defense`, `wartech`.
- Add the repository website later as the CurseForge project URL.

Do not select MIT, GPL, LGPL, or a Creative Commons license for the entire
repository. The repository contains material under several different terms;
`THIRD_PARTY_NOTICES.md` describes the scope that is currently known.

### 4. Create the first GitHub Release

1. Open the repository.
2. Select `Releases` and `Draft a new release`.
3. Create tag: `v1.2.1-hbm5751`.
4. Target: `main`.
5. Release title: `WarTech Reforged 1.2.1 - HBM X5751`.
6. Mark it as `Pre-release`.
7. Attach only `WarTech-Reforged-1.2.1-hbm5751.jar`.
8. Use the following release notes:

```markdown
## WarTech Reforged 1.2.1

Initial public beta of WarTech Reforged for Minecraft 1.7.10.

### Requirements

- Minecraft 1.7.10
- Forge 10.13.4.1614
- HBM's Nuclear Tech Mod 1.0.27 X5751
- Client and server must use matching versions

### Highlights

- Restored WarTech cruise and ballistic missile systems
- Layered Patriot and S-400 air defense
- WTI-1 Falcon, WTI-2 Lance, and WTI-3 Sentinel interceptors
- Mobile and long-range radar network with command vehicle
- Storm Shadow cruise missile
- Geran-2 attack drone and rail catapult
- HEMTT mobile artillery platform with Greg and Henry modules
- Missile route separation, interception effects, and performance fixes

### Installation

Remove original WarTech and all older Reforged builds. Install HBM X5751 and
place this JAR in the `mods` folder. The same files are required on the server
and every connecting client.

### Status

This is a beta build primarily tested in Creative mode. Back up important worlds.

SHA-256:
`B81F01D5E8832733CECBBCBDB32EDAE007CE8165A5B6440A7025321975D82BB9`
```

## CurseForge Project

### 1. Create the project

Open https://authors.curseforge.com/#/projects/create/choose-game and enter:

- Game: `Minecraft`.
- Class: `Mods`.
- Name: `WarTech Reforged`.
- Summary:
  `A restored and expanded HBM NTM missile-warfare addon with cruise missiles, layered air defense, mobile radars, command vehicles, drones, and mobile artillery.`
- Main category: `Technology`.
- Additional categories: `Armor, Tools, and Weapons`, `Server Utility`, and the
  closest available category for military or utility content.
- Environment: `Client and Server`.
- Project status: `Experimental` or the closest available beta-stage option.

Do not put Minecraft, Forge, HBM, or file version numbers in the project name.

### 2. Project icon

- Exact size: `400 x 400` pixels.
- Format: PNG or JPG; avoid WebP.
- Use an actual in-game image or a clean original WarTech Reforged emblem.
- Do not use a real military organization's official emblem or a manufacturer's logo.

### 3. Description

Copy the English part of `README.md` first. Add a horizontal separator, then copy
the Russian part. English must remain above Russian.

At the very top, add this compatibility warning:

```markdown
> **Requires Minecraft 1.7.10, Forge 10.13.4.1614, and HBM's Nuclear Tech Mod
> 1.0.27 X5751. Other HBM builds are not currently supported. Do not install the
> original WarTech 1.1.1 alongside WarTech Reforged.**
```

At the bottom, add the optional donation section:

```markdown
## Support development

WarTech Reforged is always available free of charge. Donations are completely
optional and do not unlock downloads, early builds, features, gameplay benefits,
or support priority.

[Support development](YOUR_DONATION_URL)
```

Do not place an external JAR download link in the CurseForge description. GitHub
may be linked as `Source` and `Issues`, but the CurseForge download must remain in
the CurseForge `Files` section.

### 4. Project license

Choose `Custom License`. Use this text:

```text
WarTech Reforged is distributed free of charge for non-commercial use.
No payment or donation is required to download, use, or update the mod.

This notice applies only to original WarTech Reforged contributions. Material
derived from WarTech 1.1.1 and third-party models, textures, and other assets
remain subject to the rights and licenses of their respective creators, as
documented in THIRD_PARTY_NOTICES.md.

The complete WarTech Reforged JAR may not be sold, paywalled, or used to provide
paid access, exclusive paid builds, or donor-only functionality. Unmodified links
to the official CurseForge or GitHub release pages may be shared freely.

The software is provided without warranty. The project is unofficial and is not
affiliated with or endorsed by the original WarTech team, the HBM NTM team,
equipment manufacturers, or military organizations.
```

Set the third-party distribution toggle to `On` if launchers and third-party mod
managers should be able to access the CurseForge file through the API. Set it to
`Off` only if downloads should be limited to CurseForge's own website and app.

Keep the CurseForge Rewards Program disabled for this project.

### 5. Links

After GitHub is published, configure:

- Source: `https://github.com/YOUR_USERNAME/WarTech-Reforged`
- Issues: `https://github.com/YOUR_USERNAME/WarTech-Reforged/issues`

Do not use the removed original WarTech repository as the project source link.
It can be mentioned historically in the credits if desired.

### 6. Required dependency

In `Related Projects` add:

- Project: `Hbm's Nuclear Tech Mod`
- Relation: `Required Dependency`
- CurseForge Project ID: `235439`
- Official page:
  https://www.curseforge.com/minecraft/mc-mods/hbms-nuclear-tech-mod

The exact supported HBM build is available from the official HBM GitHub release:

https://github.com/HbmMods/Hbm-s-Nuclear-Tech-GIT/releases/tag/1.0.27_X5751

Do not upload or bundle the HBM JAR with WarTech Reforged.

### 7. Upload the first file

Open the project dashboard, select `Files`, and upload:

- File: `WarTech-Reforged-1.2.1-hbm5751.jar`
- Display name: `WarTech Reforged 1.2.1 for HBM X5751`
- Release type: `Beta`
- Game version: `Minecraft 1.7.10`
- Mod loader: `Forge`
- Java: leave unspecified unless the form requires it; Minecraft 1.7.10 normally
  runs on Java 8.
- Required project: `Hbm's Nuclear Tech Mod`.

Use this changelog:

```markdown
Initial public beta.

- Restored original WarTech missile and launcher functionality
- Added layered automated air defense and three interceptor tiers
- Added Patriot and S-400 launchers
- Added mobile and long-range radar vehicles and command network
- Added Storm Shadow and Geran-2 with a dedicated catapult
- Added mobile HEMTT artillery with Greg and Henry modules
- Added route separation, improved guidance, impact effects, and performance fixes

Requires HBM NTM 1.0.27 X5751 exactly. Remove original WarTech before installing.
```

Leave automatic publication enabled so the file becomes visible after moderation.

### 8. Gallery

Upload at least five real in-game screenshots:

1. Patriot and S-400 launchers.
2. An interceptor engaging a missile.
3. Renault and S-400 radars with the command interface.
4. Storm Shadow and Geran-2 with its catapult.
5. HEMTT with Greg and Henry shown in separate screenshots.

Use short English titles and descriptions. Do not use edited images that show
features unavailable in the uploaded build.

## Optional Modrinth Mirror

Publish on Modrinth only after the GitHub and CurseForge pages are working. Use
the same JAR, version, dependency warning, attribution, and SHA-256. Keep Modrinth
monetization disabled and link back to the same GitHub Issues page.

## After Publication

1. Download the public file from CurseForge, not from the local build folder.
2. Verify its SHA-256 and launch it in a clean test instance.
3. Verify that CurseForge shows HBM as a required dependency.
4. Verify that the English description appears before Russian.
5. Pin a GitHub issue template requesting `latest.log`, crash report, HBM version,
   Forge version, client/server context, and reproduction steps.
6. Never replace an existing release JAR silently. Increment the version and
   publish a new file for every public code or asset change.
