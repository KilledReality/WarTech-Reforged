# WarTech Reforged: player guide

Edition for **Minecraft 1.12.2 / NTM Community Edition 2.6.1.0**,
Modular Warfare Update; checked against candidate dev79, October 9, 2026.
[Русский](GUIDE.ru.md) · [Release notes](CHANGELOG.en.md) · [Documentation index](README.md)

This manual covers the current experimental CE branch, not the 1.7.10 or
NTM Extended builds. Numbers are Minecraft balance values, not real military
specifications. Part descriptions and configuration-specific interface warnings
take precedence over the upper limits quoted for a class.

## Contents

1. [Installation and getting started](#1-installation-and-getting-started)
2. [Controls and manual piloting](#2-controls-and-manual-piloting)
3. [Teams, ownership and coordinates](#3-teams-ownership-and-coordinates)
4. [Cruise missile constructor](#4-cruise-missile-constructor)
5. [Programming, search and accuracy](#5-programming-search-and-accuracy)
6. [Ground launch and detonators](#6-ground-launch-and-detonators)
7. [Aircraft and air-launched missiles](#7-aircraft-and-air-launched-missiles)
8. [Building and operating UAVs](#8-building-and-operating-uavs)
9. [Reconnaissance reports](#9-reconnaissance-reports)
10. [Geran-2 and jet-powered Geran-5](#10-geran-2-and-jet-powered-geran-5)
11. [Air defense, radar, communications and repairs](#11-air-defense-radar-communications-and-repairs)
12. [Electronic warfare and anti-radiation missiles](#12-electronic-warfare-and-anti-radiation-missiles)
13. [Artillery, legacy missiles and satellites](#13-artillery-legacy-missiles-and-satellites)
14. [Long flights and servers](#14-long-flights-and-servers)
15. [Troubleshooting](#15-troubleshooting)

## 1. Installation and getting started

Use Minecraft 1.12.2, Forge **14.23.5.2860**, HBM NTM **Community Edition
2.6.1.0**, **MixinBooter 10.7** and the CE build of WarTech Reforged. Java 8
is recommended. Do not install NTM Extended, original WarTech or a second
Reforged JAR alongside this build. The server and every client must use matching
versions.

Close the game/server before replacing the JAR, and back up the world. Start
testing in a separate Creative world. New-content recipes and complete survival
progression are unfinished: an item being available does not mean it already
has its final crafting recipe.

Creative tabs separate missiles, parts, blocks, equipment, consumables, air
defense, aviation, support, custom UAVs and custom cruise missiles. Constructor
tabs group bodies/airframes first, followed by equipment categories. A blueprint
is a saved configuration; an assembled vehicle is the actual item. You cannot
load a blueprint as ammunition.

For a first test, choose your IFF team, take a starter missile with the correct
launch method, set one nearby—but not point-blank—target on open terrain and
use a matching launcher. Then move on to custom builds and multi-area search.

## 2. Controls and manual piloting

LMB/RMB mean left/right mouse button. Shift+RMB means Sneak and right-click.
Interact with the visible equipment body using your main hand. Place equipment
by right-clicking the top face of a solid block; leave room for wings, stores,
movement and takeoff.

| Action | Default control |
|---|---|
| Open stationary equipment interface | RMB; held items can change the action |
| Drive a folded ground vehicle | W/S, A/D to steer; Shift to leave |
| Deploy/retract a mobile ground installation | Shift+RMB, when its state permits |
| Enter Remote Pilot | Pilot button; on a custom UAV, also empty-hand Shift+RMB |
| Steer in flight | Mouse; A/D for yaw |
| Increase/decrease throttle | W/S |
| Adjust pitch to climb/descend | Space / left Shift |
| Switch nose/chase camera | C or F5 |
| Hold course and look around | R |
| Cycle hardpoints | Z |
| Fire selected weapon | LMB |
| Deploy flares, if installed | F |
| Leave remote control | X |

In Remote Pilot, **Shift does not exit**, unlike a ground vehicle. Use X:
the operator returns to the connection point, and the craft continues its
autopilot behavior. This is not a self-destruct or instant-return command.
R holds a course; it does not freeze the aircraft in midair.

Weapons, flares and return capability depend on the craft. A Geran has no
selectable store: its warhead activates on impact, not on LMB. Switching the
camera does not change your course. If controls conflict, check WarTech and
other mods' key settings; this manual uses the default layout.

## 3. Teams, ownership and coordinates

### Identification friend or foe

RMB with the IFF Configurator and choose ALPHA, BRAVO, CHARLIE, DELTA or
PERSONAL. This determines the friendly network and equipment relations.
PERSONAL provides personal affiliation. Vanilla scoreboard teams are also
supported; `!wtteam status` reports your status.

New equipment records affiliation when placed. **Changing the player's team
does not automatically transfer previously placed equipment.** Use
Shift+RMB with the configurator on existing equipment to rebind it, then check
its status. For an air-defense test, the missile and defense must actually
belong to opposing sides—not merely have been placed before and after the
same player changed teams.

IFF is not general invulnerability: friendly explosions can still harm people
and buildings. Protecting incoming allied missiles from chain detonation is
not the same as disabling friendly fire.

### Target designators

RMB a target block with a designator to record coordinates; inspect its tooltip.
Put it in the launcher, station or programmer's target slot, then use the import
button where provided.

Older HBM designators containing **X/Z without Y** are supported: the server
resolves surface elevation, including for a distant chunk. Wait for preparation;
you do not need to teleport to the target. When entering coordinates manually,
Y means target elevation, not cruise altitude. Enter an exact Y for a rooftop,
slope or other specific level: automatic surface resolution cannot select an
arbitrary room.

Coordinates belong to a dimension. A program from another dimension is not a
valid mission. Empty/invalid targeting data and hostile ownership are different
rejection causes; read the interface message.

Custom cruise targets accept Y from 0 to 255; airburst targets must not exceed
Y=245, leaving room for detonation above them.

## 4. Cruise missile constructor

### Assembly and blueprints

1. Place the cruise missile constructor and open it with RMB.
2. Select a body and fit compatible parts in the ten categories below.
3. Name the build. Watch mass, range, speed and compatibility messages whenever
   you replace a part.
4. Assemble and take the result from the output slot. Required components are
   consumed; a saved configuration does not create free parts.
5. To repeat a build, use a blank blueprint and save the design. A saved blueprint
   is reusable, but each new missile still requires a component set. A blueprint
   stores the build, not a replacement for its flight mission.

To assemble from a saved blueprint, put it in the plan slot, keep the required
components in your inventory and press assemble: the constructor selects them.
Clear the output slot before saving or assembling another item.

There are **44 parts, including four bodies**. Modularity changes performance
and behavior; appearance uses the clean base-body model. Changing an engine
or warhead does not add external module overlays.

| Category | Options and purpose |
|---|---|
| Body | Light Lastvika; classic Neptune; heavy low-observable Storm Shadow; heavy long-range Strizh |
| Engine | Economy, universal, high-output, heavy long-endurance |
| Fuel | Short, standard, extended, heavy long-range section |
| Wings | Compact, long-range, folding, heavy folding |
| Navigation | Coordinate, route, terrain-following |
| Nose/seeker | Coordinate nose without active search; optical, thermal, radar |
| Warhead | 13 options in the next table |
| Fuze | Contact, delayed, airburst |
| Launch | Ground booster, air-launch adapter, rail launch |
| Link | Autonomous or command-updatable |

### Bodies and range

| Body | Mass limit | Class range ceiling | Starter-build range |
|---|---:|---:|---:|
| Lastvika, light jet-drone missile | 220 | 1,800 blocks | 667 blocks |
| Neptune, classic | 520 | 5,500 blocks | 1,293 blocks |
| Storm Shadow, heavy low-observable | 900 | 7,000 blocks | 1,270 blocks |
| Strizh, heavy long-range | 1,800 | 14,000 blocks | 5,123 blocks |

Mass uses shared gameplay units for comparing builds and carriers. A class
ceiling **does not mean** every missile of that class can fly that far. Fuel
adds weight, powerful engines increase consumption, and heavy warheads cost
speed and travel budget. Use your build's calculated result. Mission validation
keeps a reserve of 12% plus 80 blocks; search and detours also consume range.

### Warheads

| Warhead | Mass | Gameplay role / base parameter |
|---|---:|---|
| HE | 30 | General-purpose strike, strength 7 |
| Heavy HE | 180 | Stronger blast, 14 |
| Thermobaric | 45 | Wider blast effect, 9 |
| Heavy thermobaric | 220 | Strengthened version, 18 |
| Penetrating | 95 | Delayed detonation, 8 |
| Heavy penetrating | 260 | Stronger delayed detonation, 14 |
| Cluster | 65 | 12 falling submunitions, strength 4 each |
| Heavy cluster | 180 | 24 submunitions, strength 5 each |
| EMP | 50 | Electronic disruption within 24 blocks; not an anti-personnel warhead |
| Fragmentation | 60 | Exposed entities, area up to 22 blocks |
| Heavy fragmentation | 150 | Stronger fragmentation, up to 32 blocks |
| Focused anti-vehicle | 85 | Forward-sector effect, up to 14 blocks |
| Incendiary | 40 | Local fire effect, area up to 8 blocks |

Explosion strength **is not a guaranteed destruction radius**. Cover, distance,
burst height and block resistance affect the result. A cluster warhead covers
an area; it does not guarantee killing everything inside a circle. Fragmentation
and directional effects do not pass through every kind of cover either.

Penetrating warheads require delayed fuzes. Thermobaric, focused anti-vehicle
and incendiary warheads do not support airburst. The constructor rejects
incompatible combinations. Not every heavy warhead fits a light body. Legacy
nuclear missiles are a separate system, not one of these 13 modules.

## 5. Programming, search and accuracy

RMB in the air while holding an assembled missile to open its programmer.
Program each missile before loading it onto a launcher or carrier.

### Coordinate strike

Select coordinate mode and enter X/Y/Z, or insert a designator in the slot
and import its data. This mode has **one target**. You do not enter a manual
chain of transit waypoints: navigation chooses the intermediate course.
A designator in the temporary slot is returned when the interface closes.

### Target search

Search mode requires an active optical, thermal or radar seeker. Set up to
eight search areas and a target category: any eligible target, ground vehicles,
aircraft, living entities or hostile mobs. Available categories depend on the
seeker; radar is for vehicles/aircraft, not villagers or mobs.

| Seeker | Useful for | Limitations |
|---|---|---|
| Coordinate nose | A known impact point | No moving-target search |
| Optical | Visible targets with sufficient light | Lighting, weather, line of sight |
| Thermal | Thermal target search, including at night | Flares and contact loss |
| Radar | Supported WarTech vehicles and aircraft | Jamming; not universal detection of every modded entity |

Search areas are **not transit waypoints**. The missile searches for a limited
time, then moves to the next area; an empty area is not an automatic full-warhead
detonation command. Friendly and ineligible targets are excluded. Losing contact
leaves a last-known position, not the ability to see through obstacles.

### Navigation quality and salvos

Coordinate navigation is simple and direct. Route navigation uses more developed
local decisions. Terrain-following navigation considers terrain more effectively;
advanced systems diversify group approaches. This is bounded gameplay autopilot,
not an all-seeing planner for the entire map.

CEP is the radius containing half of coordinate impacts statistically, not a
promise to hit a particular block every time. Error depends on navigation and
distance. **Navigation tier and air-defense target class are different stats.**
Good accuracy does not automatically make a missile stealthy; stealth does not
guarantee accuracy. An appropriate active seeker can help against a small target,
but it may lose contact or select another eligible target.

An intelligent salvo does not mean every missile can be assigned an arbitrary
common area and expected to distribute itself perfectly. Program the intended
targets/areas, allow maneuvering reserve and use suitable navigation systems.

### Updating a mission in flight

Fit a command link, edit the program of a matching example and use the button
to update the nearest matching friendly missile. This selects the nearest
**loaded** owned missile with the same configuration in the same dimension,
within 4,000 blocks. Autonomous links do not accept updates; strong EW or
insufficient remaining fuel can prevent them. This is not global control of
any missile anywhere on the map.

## 6. Ground launch and detonators

Custom missiles use a booster-equipped ground installation or a rail launch
for a suitable jet-drone configuration. The build's launch method must match
the installation; an air-launch adapter cannot substitute for a ground booster.

1. Place the launcher on level terrain, orient it correctly and clear space
   for the missile, support and initial flight segment.
2. Program the missile. Load the assembled item through the interface or by
   RMB on a compatible launcher; hopper loading is also supported.
   **Loading is not launching.**
3. Check state and affiliation. For a direct launch, open the launcher with
   an empty hand and press its launch button.
4. For a remote launch, bind a detonator to the ready installation using
   Shift+RMB. Then RMB the detonator in the air to activate linked weapons.

A normal detonator keeps the last single binding; a multi-link detonator holds
up to 64 compatible links, including supported HBM installations, missile pads
and UAVs. Shift+RMB in the air clears links. Reloading/replacing a missile can
require rebinding: an old link does not authorize a new, unchecked round.
Ownership/IFF, mission and readiness are validated.

You do not need to stand near the remote launcher: for a valid known binding
in the same dimension, the server prepares its chunk and executes the request.
This is queued, not necessarily instant. Repeated clicks do not speed up
loading and should not create extra missiles.

Legacy, non-custom launchers have different requirements: matching ammunition,
a designator and HBM power. Supported legacy launchers use a detonator or
their own redstone logic. **Do not apply this rule to the Geran catapult:
its redstone launch is not implemented.**

## 7. Aircraft and air-launched missiles

### Preparing a sortie

RMB a parked aircraft to open its interface. Load compatible weapons, power
and flares, set the mission and check readiness. Leave a level clear takeoff
run in the nose direction, an unobstructed climb area and room to return.
An aircraft is not a helicopter; fitting it beside a wall does not make that
location a suitable runway.

For conventional strike missions, RMB a designator on the carrier to append
a target; Shift+RMB with the designator replaces the list. The queue supports
up to six targets. Use the launch/return button; legacy aircraft also support
empty-hand Shift+RMB. Use the pilot button for manual flight.

### Custom missiles

Build missiles **with an air-launch adapter**, program them and load the first
eligible weapon slots. The carrier recognizes this loadout and imports missile
objectives unless its mission has been manually overridden. The import-from-
missiles button explicitly refreshes the carrier's mission.

| Carrier | Supported custom loadout | Mission radius / custom-cruise aiming range |
|---|---|---|
| F-16C | Up to 3 Lastvika **or** 2 Neptune **or** 1 Storm Shadow | Strike 3,000 / aiming 1,800 blocks |
| Su-27 | Up to 3 Lastvika **or** 2 Neptune **or** 1 Storm Shadow | Strike 3,400 / aiming 2,200 blocks |
| Tu-95 | Up to 6 compatible missiles, including heavy Strizh | Mission 8,000 / aiming 4,000 blocks |
| MQ-9 | Custom cruise missiles unsupported | Conventional mission radius 2,400 blocks |

F-16C/Su-27 do not mix custom missile classes or combine them with conventional
stores in the same such loadout. These limits are alternatives, not additive.
Tu-95 accepts different compatible bodies.

A missile's own nonempty program takes priority at release. An unprogrammed
missile can inherit a carrier strike coordinate; correct an invalid existing
program rather than expecting it to be silently overwritten. For a searching
missile, the carrier uses the first search area as its approach objective;
it does not turn all search areas into carrier waypoints.

Minimum release distances by body are Lastvika 80, Neptune 120, Storm Shadow
160 and Strizh 220 blocks. Actual remaining missile range and carrier capability
also matter. If takeoff brings the carrier too close, it should reposition for
another release opportunity. A visible obstacle in the initial path blocks an
unsafe release; unknown distant terrain is not automatically treated as a wall.
After separation, the missile uses its own navigation, not just the aircraft's
heading.

### Conventional weapons and interception

MQ-9 has six conventional hardpoints for compatible AGM-114, HJ-10, GBU-12,
Mk 82 and JDAM. F-16C has four and Su-27 six conventional hardpoints for their
supported ammunition. Tu-95 uses Kh-555, FAB-5000, KAB-3000 and compatible
custom cruise missiles. Check slot compatibility, not just similar icons.
Bombs and guided missiles have different release logic; Mk 82 release depends
on altitude and speed.

F-16C and Su-27 support an interception sortie with Skyguard AAM, requiring
a confirmed hostile air target and a friendly deployed, powered network with
an Ural command post. This does not replace ground missile defense against
every ballistic threat. Interception mission radii are 6,500 and 8,000 blocks
respectively.

Return requires a clear approach to the home field. Do not place new blocks or
vehicles on the runway after takeoff; a craft may need another landing approach.
A return command does not automatically replenish spent fuel.

## 8. Building and operating UAVs

### Choosing components

There are **28 parts, including three airframes**, in eight categories:

| Category | Options |
|---|---|
| Airframe | One-way attack, reconnaissance, reusable strike |
| Engine | Economy, balanced, heavy |
| Energy | Compact, long-range, hybrid |
| Flight controller | Basic, precision, combat |
| Link | Short-range, encrypted, satellite |
| Sensor | Day optical, EO-IR, SAR |
| Payload | HE, thermobaric, focused, heavy HE, heavy thermobaric warhead; light two-point, heavy four-point or single cruise-missile rack |
| Defense | Flares or compact EW |

Airframe, controller, sensor and link determine capability, not just the craft's
name. Warheads and weapon racks use **one shared category**; they cannot all
be stacked into one slot. The one-way airframe does not support external racks.

| Airframe | Maximum mass | Class radius ceiling | Starter-build radius |
|---|---:|---:|---:|
| One-way | 170 | 1,800 blocks | 679 blocks |
| Reconnaissance | 380 | 4,500 blocks | 1,923 blocks |
| Strike | 490 | 6,000 blocks | 1,725 blocks |

Use the UAV constructor similarly to the missile constructor: install parts,
check calculated stats/errors, name the build and assemble the item. Blank
blueprints save a configuration; completed blueprints are reusable with new
components. Radius accounts for consumption and return; observation and detours
consume time/energy. Stores penalize mass, thrust, speed and range. A longer
radio link does not replace fuel.

### Programming, placement and launch

1. Put the assembled UAV in the UAV mission-planning station.
2. Add up to eight points with roles: **transit, observe, strike, return**.
   Manual transit points are allowed here, unlike in the cruise programmer.
   You can import a designator.
3. Observation requires a sensor. A one-way craft with a warhead is intended
   for a final strike, not reusable landing after detonation.
4. Place the assembled item on top of a **UAV Launch Point**. Its orientation
   sets the initial heading. Placement creates a ready craft but does not
   automatically launch it.
5. RMB the craft for servicing. The launch button starts its autonomous mission;
   the pilot button enters manual control. You can bind a ready craft to a
   detonator and launch it remotely.

RMB a designator on a ready UAV to set its target. **Shift+RMB with a designator**
sets the target and requests autonomous launch. **Empty-hand Shift+RMB** enters
manual piloting, not autonomous launch. If an X/Z-only target needs elevation
preparation, wait while preserving the interaction conditions.

The fleet station lists **loaded friendly custom UAVs in the current dimension**.
It is not an all-seeing list of every saved craft. To launch a linked craft in
a distant unloaded chunk, use its detonator rather than waiting for it to appear
in a local list.

### One cruise missile under a UAV

The recommended module is the **single cruise-missile rack**. RMB an assembled
missile onto a parked ready craft to load it. It uses the first weapon slot,
without stacking several cruise missiles under one another.

- Reconnaissance accepts light Lastvika; strike accepts Lastvika or an eligible
  Neptune. Storm Shadow and Strizh are unsupported.
- The missile needs an air-launch adapter and compact or compatible folding
  wings. A ground booster does not substitute for the adapter.
- Total loaded mass and sufficient thrust are checked. An empty slot alone
  does not authorize loading.
- Read the specific rejection: rack, body, wings, adapter, mass or thrust.
  Change the component instead of trying to bypass the slot.

UAV aiming range is the **lower** of controller and sensor capability.
Controllers: basic 500 / precision 1,000 / combat 1,800 blocks. Sensors:
none 350 / day 600 / EO-IR 1,000 / SAR 1,800 blocks. This is neither flight
radius nor communication range.

### Return and maintenance

Return reusable craft with energy reserve and a clear landing area. Repair is
available in the service interface of a ready landed craft: one iron ingot
restores 20% of maximum health, at least 10 HP. Compatible HBM batteries can
be applied by interacting with the craft.

Use Shift+RMB with the salvage wrench to recover a parked craft. Its item
preserves state: recovery does not provide a free full repair or refill.
Weapon inventory is returned separately. A wreck is not a fully restored UAV.

## 9. Reconnaissance reports

Fit an appropriate sensor and observation points. Day optics depend on lighting,
EO-IR works under different conditions, and SAR is oriented toward ground
reconnaissance, not universal air-defense tracking of missiles and aircraft.

The report displays surveyed terrain, contacts, coordinates, affiliation,
classification and recognition confidence. **Unknown** does not necessarily
mean hostile; a neutral animal does not become a combat target just because
it appears in a report. These are observations, not a permanent live GPS feed.

Use the report-download action in craft servicing or the available report command
at the fleet station. You receive a separate reconnaissance item. RMB it later
to view the snapshot without reconnecting to the drone. Information can become
outdated, especially for moving targets.

One report is limited to 768 survey cells and 96 contacts: it is tactical
reconnaissance, not an unlimited world scanner. Save separate reports for
different areas.

## 10. Geran-2 and jet-powered Geran-5

Both rounds use the existing Geran catapult. Geran-5 is a separate item, not
a rename or forced replacement of Geran-2.

1. Place the catapult in an open area with a safe launch direction.
2. Load the chosen Geran, a target designator and HBM power. Launch requires
   at least 25,000 energy units.
3. For autonomous launch, bind and activate a detonator.
4. For manual launch, **Shift+RMB the loaded catapult with your empty main
   hand**. For X/Z-only targeting, wait for elevation preparation without
   leaving the catapult or changing its contents.

| Parameter | Geran-2 | Geran-5 |
|---|---:|---:|
| Maximum speed at 20 TPS | 23 blocks/s | 37 blocks/s |
| Horizontal travel budget | 1,800 blocks | 1,800 blocks |
| Coordinate-launch distance | 20–1,000 blocks | 20–1,000 blocks |
| Manual link radius | 1,000 blocks | 1,000 blocks |
| HE warhead-strength parameter | 6 | 12 |
| Air-defense target class | 1 | 2 |

Travel budget includes maneuvers; it does not permit a coordinate launch at
1,800 blocks. Maximum speed is not maintained at every throttle/climb setting.
Twice the strength does not mean exactly twice the damage to every block or
entity. Geran-5 is harder to intercept, not invulnerable.

Manual mode uses the shared mouse/W/S/A/D/Space/Shift/C/F5/R/X controls.
Geran-5 has a jet exhaust effect. After X, the craft continues autonomous
behavior toward the programmed target. Catapult redstone launch is not
implemented: use a detonator for autonomous launch or the interaction above
for manual launch.

## 11. Air defense, radar, communications and repairs

### A working defense battery

Patriot/S-400 require compatible WTI interceptors, HBM power, correct affiliation
and confirmed contacts from an active friendly network. **Do not insert a
target designator into an air-defense launcher.** Radar detection range does
not become interceptor engagement range.

A practical sequence: select IFF → place radar, Ural command post and launchers
→ deploy mobile units → supply power → enable radar → load interceptors
→ permit fire → check for a contact.

| Equipment | Capabilities |
|---|---|
| WTI-1 / WTI-2 / WTI-3 | Baseline ranges 100 / 250 / 400 blocks; compatibility and threat difficulty constrain use |
| Tor-M1 | 8 WTI-2; radar 340 blocks, ceiling 260; engagement to 220 blocks |
| Pantsir-S2 | 12 WTI-1 and a 30 mm belt; radar 260 blocks, ceiling 190; missile/gun engagement to 100 blocks |
| Renault TRM | Mobile radar, detection to 600 blocks, ceiling 500 |
| S-400 long-range radar | Detection to 1,200 blocks, ceiling 900, up to 32 contacts |
| Ural | Deployed powered command post for the friendly network |

**HOLD** prohibits fire. Use an authorizing **AUTO** or **EMERGENCY** mode where
provided. Mobile units use their folded state for driving and deployed state
for full operation. Pantsir consumes finite gun ammunition and energy; flares
and jamming can interfere with its intercept solution.

Target classes 1–3 represent difficulty; speed and low observability matter.
Custom missiles do not all have to be class 1. Even a suitable interceptor can
miss. Target allocation limits unnecessary simultaneous launches; follow-up
attempts depend on the previous result and network state.

A downed target can break up in the air or fall with a reduced impact. A missed
interceptor can also hit terrain and produce visible consequences. Do not place
vulnerable facilities directly under an intercept line assuming every missile
will disappear harmlessly.

### Communications and alarms

A long-range mast needs seven blocks of height, power and matching affiliation.
Neighboring masts link over distances up to 2,400 blocks; chains relay friendly
network data. A mast keeps its own local 3×3-chunk sector active. Power loss
or destruction breaks a route if no alternative exists.

For an HBM siren, place the alarm relay within 96 blocks of a friendly Ural and
connect the adjacent siren. A confirmed air threat produces redstone level 15.
This is a network alarm, not a universal sensor for every foreign entity.

### Health and repair

Check health and energy separately in mobile defense interfaces. Repairs apply
to Tor, Pantsir, radar vehicles and the Ural command post: **RMB your own/friendly unit with an iron ingot
while its interface is closed**. One ingot restores 10% of maximum health,
at least 20 HP; this does not refill ammunition or power. Custom UAV repair
uses a service button and different values; see section 8.

## 12. Electronic warfare and anti-radiation missiles

The Synytsia jammer operates to 350 blocks. Select L/S/X band or wideband:
matching the band is stronger, while wideband is weaker but covers more
possibilities. Jamming degrades communication/radar detection and can create
false contacts; it does not confer complete immunity to weapons.

Shift+RMB toggles an EW unit; RMB a jammer or decoy emitter to cycle bands,
and RMB with a compatible battery to charge it. Passive ESM detects active
emitters to 900 blocks; RMB checks detections. ESM does not replace an ordinary
tracking radar. A decoy emitter can distract anti-radiation missiles.

For AGM-88 HARM, designate an area near an active hostile emitter and use a
compatible legacy cruise launcher. The missile searches for emissions within
1,200 blocks of the designated point, prioritizing active jammers. An emitter
switching off leaves a last-known position with degrading accuracy. This is
not map-wide detection of switched-off radars.

## 13. Artillery, legacy missiles and satellites

### HEMTT

RMB an empty platform with a **Greg** or **Henry** turret block to install one
weapon module. They are mutually exclusive. Drive while folded; Shift+RMB
deploys the platform, and RMB while deployed opens the native HBM turret
interface. Load the matching ammunition and power.

RMB with a designator to transfer coordinates; the standard artillery rangefinder
uses its normal HBM artillery link. Watch heading and obstructions. Henry
227 mm rockets are class-1 air threats; 610 mm rockets are class 2. Greg shells
are available to defense only over a short range, up to 220 blocks. Ordinary
bullets do not become universal radar contacts.

### Legacy missiles

The mod retains conventional cruise missiles with different warheads, Tomahawk,
Kalibr, CJ-10, Storm Shadow, Kh-555, supersonic/hypersonic variants, Iskander,
LRHW and SLBM ballistic missiles, and special ammunition. They do not fit any
constructor slot merely because they are called missiles. Use their matching
launcher, power and designator.

Horizontal travel budgets are 3,500 blocks for conventional cruise missiles,
5,500 for Tomahawk/Kalibr/CJ-10/Storm Shadow and 8,000 for Kh-555; supersonic
missiles have 2,000, hypersonic 1,250. Ballistic classes: Iskander 8,000,
LRHW 10,000, SLBM 14,000. These are travel budgets, not unconditional launch
distances without reserve or launcher validation.

Nuclear effects depend on HBM and world settings and cannot be compared to HE
using a single number. Test hazardous ammunition only in a disposable world.
ASAT and nuclear missile defense are separate special systems, not ordinary
custom cruise modules.

ASAT uses a compatible legacy ballistic launcher: instead of surface coordinates,
it needs an HBM satellite chip linked to the intended satellite ID and at least
75,000 power. It targets that ID, not an arbitrary ground point. Nuclear missile
defense uses special interceptors; putting a nuclear warhead on an ordinary
missile does not turn it into a guided surface-to-air missile.

### ODIN

1. Put ODIN in the right-hand frequency-generation slot of the HBM Satellite
   Linker; remove it after a nonzero ID/frequency appears in its tooltip.
2. Put ODIN in the left slot and the desired controller in the middle slot
   and link them. Each controller must be linked separately.
3. Deploy the satellite using the standard HBM Soyuz system.
4. Use a linked Satellite Interface map, Satellite Coordinate Interface
   (`sat_coord`) for typed coordinates, or a satellite laser designator with
   RMB on a point within its range.
5. Check chat for strike confirmation and remaining rods.

ODIN carries four kinetic rods with a 60-second release interval. Its impact
is nonnuclear, without radiation or a nuclear mushroom cloud. A falling rod
is an interceptable class-3 ballistic target, not guaranteed unavoidable damage.
A conventional missile designator does not replace a linked satellite controller.

## 14. Long flights and servers

Missiles have travel budgets; carriers have mission radii and aiming capabilities;
operators have communication links; radars have detection ranges. Improving one
does not remove the others' restrictions.

Autonomous missiles/UAVs use a small moving chunk window. Linked remote pads
can launch without a nearby player, and known defense nodes wake as threats
approach. You do not need to teleport along every route chunk or stand beside
defenses until impact.

This works **only while the server is running**. A fully stopped world does
not simulate flight. Remote launch operates in the same dimension; it does not
make the detonator an interdimensional controller. Equipment still needs valid
placement, binding, power and ammunition.

Server protection limits allow up to 32 active flights per dimension, including
up to 16 custom missiles and UAVs combined, and up to 25 chunks per flight
window. Insufficient capacity rejects a launch before consuming ammunition.
Preparing essential chunks can pause a flight without spending its fuel; this
does not grant infinite endurance during ordinary flight.

Administrators must allow Forge chunk tickets with a depth of at least 25 chunks
and sufficient ticket capacity. `/wtflight status` provides diagnostics. The
queue bounds new loads, but generating complex terrain and large explosions can
still stall a server. At low TPS, real speed and timing differ from 20-TPS values.
For large battles, prepare terrain in advance and test your own modpack's load.

## 15. Troubleshooting

| Symptom | What to check |
|---|---|
| A part will not fit the constructor | Slot type, body, warhead/fuze compatibility, mass and thrust |
| A missile will not load on aircraft/UAV | Air-launch adapter, allowed body, first slots, legal loadout; UAV wings/mass/thrust |
| Aircraft took off but did not release | Missile programs, IFF, release range/minimum, visible obstructions, remaining fuel and valid mission |
| Detonator does not launch | Saved binding, affiliation, readiness, power, dimension and active-flight limit; wait for preparation |
| Designator shows only X/Z | Import and wait for surface resolution; enter Y manually for a specific elevation |
| Geran manual launch does not start | Empty main hand, Shift+RMB loaded catapult, at least 25,000 energy, target 20–1,000 blocks away; stay during Y preparation |
| Craft is missing from fleet station | It lists loaded friendly custom UAVs in this dimension, not every saved craft |
| Defense sees a target but does not fire | HOLD/deployment, power, ammunition, interceptor compatibility, actual target affiliation and engagement envelope |
| Defense does not regard a missile as hostile | Existing equipment IFF and missile ownership; changing player teams does not rebind installations |
| Impact has little effect | Warhead type, height, cover, distance and HBM settings; EMP is not intended to kill villagers |
| Radius is smaller than expected | Actual loaded build rather than class ceiling; search, maneuvers, observation and return costs |
| It is unclear why a missile missed | CEP/distance, acquisition/contact loss, search category, EW/flares and correct target Y |

For an unexplained repeatable failure, record exact versions, build components,
launch type, coordinates/dimension, both sides' affiliation and error text.
For server cases, include `latest.log` and `/wtflight status`; remove personal
addresses/tokens before sharing logs. Test on a copy of the world.
