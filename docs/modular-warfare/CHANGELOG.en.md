# WarTech Reforged — Modular Warfare Update

For Minecraft 1.12.2 / HBM NTM Community Edition. Experimental branch.

This update centres on building your own cruise missile and integrating it
with reconnaissance, aircraft, UAVs and air defense. A successful strike now
depends on the weapon's configuration, flight program, carrier and opposing
defense—not just the ammunition you select.

## Build your own cruise missiles

- A new constructor with **44 parts, including four bodies**, ten equipment
  categories, reusable blueprints and starter configurations.
- Four distinct classes: the light Lastvika jet-drone missile, versatile
  Neptune, heavy low-observable Storm Shadow and a heavy long-range missile
  using the Strizh model.
- Engines, fuel, wings, navigation and payload determine actual mass, speed,
  range and handling. A heavier warhead is a trade-off, not a free upgrade.
- **13 warheads**: standard and heavy HE, thermobaric, penetrating, cluster
  and fragmentation options, plus EMP, focused anti-vehicle and incendiary
  payloads. Contact, delayed and airburst fuzes require compatible warheads.

## Navigation, target search and coordinated attacks

- Coordinate strikes and target search are separate modes. A conventional
  program specifies one impact point; an active seeker can search several
  designated areas for a suitable target.
- Optical, thermal and radar seekers have different detection conditions,
  target capabilities and countermeasure vulnerabilities.
- Navigation quality affects local obstacle avoidance and accuracy. Advanced
  systems diversify flight corridors and approach directions during group
  attacks; basic configurations fly much more directly.
- Suitable missiles with a command link can receive an updated mission in
  flight. Better navigation does not remove fuel limits, search constraints
  or the possibility of missing.

## Ground launch and carrier integration

- Ground launchers distinguish loading from launch authorization. Fire a
  prepared missile using the launcher button or a linked detonator.
- F-16C and Su-27 carry up to three light missiles, two Neptunes or one Storm
  Shadow. Tu-95 accepts up to six compatible custom missiles, including the
  heavy class.
- Modular reconnaissance and strike UAVs can carry one suitable missile,
  subject to rack type, mass, thrust and dimensional restrictions.
- Carriers can obtain strike objectives from the programs stored in their
  missiles and seek a valid release position. A target that is too close or
  a visible obstacle calls for another approach, not a point-blank release.

## Expanded UAV operations and Geran-5

- The UAV constructor combines 28 parts, including three airframes, supporting
  one-way attack craft, reusable scouts and weapon carriers.
- Reconnaissance can be downloaded into a separate report item and read
  later, independently of the drone's current flight.
- Geran-5 is a new jet-powered round for the existing Geran catapult: faster,
  harder to intercept and with twice Geran-2's warhead-strength parameter,
  while retaining a comparable travel budget. Both autonomous launch and
  manual piloting are supported.

## A connected combat system

- Range, power and carrier restrictions have been rebalanced. Weapon range,
  mission radius, communication and detection are separate considerations.
- Interception difficulty varies with the target's configuration. Air defense
  respects team affiliation and coordinates follow-up attempts; a hit is
  still not guaranteed.
- Downed missiles can break up in the air or fall with consequences. Allied
  strikes should not prematurely detonate nearby incoming friendly missiles
  and UAVs; this does not disable all friendly fire.
- Defense interfaces show equipment health, and damaged equipment can be
  repaired using iron.
- Long autonomous flights, remote launches and defense without a nearby
  player use bounded server-side chunk loading rather than keeping an
  entire route permanently active.
- Refreshed interfaces, localization, icons, Creative-tab organization,
  vehicle sizes and weapon mounting. Custom missiles retain clean base-body
  models without external module overlays.

## Before updating

Requires Forge 14.23.5.2860, NTM Community Edition 2.6.1.0 and MixinBooter 10.7.
This build is not for NTM Extended or Minecraft 1.7.10. Back up your world and
use matching versions on the server and every client. New-content recipes and
full survival progression are not finalized. The new Topol-M, Yars and
Oreshnik systems remain disabled.

Practical instructions: [full player guide](GUIDE.en.md).
