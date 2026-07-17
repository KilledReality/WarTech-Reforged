# Electronic Warfare Network

WarTech Reforged models electronic warfare as a server-side extension of the
existing radar and command network. It does not simply disable radar blocks.
Instead, jamming changes contact probability and track quality, while passive
sensors and anti-radiation weapons use a separate emitter picture.

## Equipment

### Synytsia jammer

- Effective radius: 350 blocks.
- Modes: L, S, X and wideband.
- A matching narrow band produces the strongest interference.
- Wideband covers every radar band at reduced strength.
- Interference reduces detection probability, makes tracks decay and creates up
  to six false contacts on heavily jammed radars.
- Multiple hostile jammers combine with diminishing returns and cannot exceed
  94 percent noise.

### Passive ESM array

- Detection radius: 900 blocks.
- Detects active radars, jammers and radar decoys without emitting itself.
- It cannot directly replace an acquisition radar for interceptor guidance.

### Radar decoy emitter

- Emits a radar-like signal on the selected band.
- Appears in passive ESM and anti-radiation seeker logic.
- Intended to draw anti-radiation missiles away from real radars.

### AGM-88 HARM

- Seeker search radius around the designated point: 1,200 blocks.
- Selects active emitters and gives an active jammer a home-on-jam priority.
- Continuously updates the impact point while the emitter remains active.
- If the emitter shuts down, the missile flies toward its last known position.
  After 1.5 seconds of silence, the probable impact error grows gradually to a
  maximum of 96 blocks.
- It uses the normal WarTech cruise-missile launch workflow and can be loaded in
  launchers that accept Kalibr-class missile items.

## Controls

- Place an EW item on the ground with right click.
- `Shift + right click` toggles the selected unit on or off.
- Right click a jammer or decoy to cycle L, S, X and wideband operation.
- Right click passive ESM to read its current emitter count.
- Insert an HBM-compatible battery by right clicking the unit with the battery.

## Ownership and IFF

The placing player's scoreboard team is stored on new radars, command posts and
EW equipment. Units on the same non-empty team do not jam or report one another
as hostile. If the player has no scoreboard team, the player name is used as a
private fallback identity. Old entities saved before this system have no owner
until replaced and therefore remain neutral for compatibility.

## Command Post

The air-defense command interface now reports two additional values:

- `HOSTILE EMITTERS`: active radar, jammer and decoy emissions in the ESM picture.
- `HOSTILE JAMMERS`: hostile jammer sources inside command-network range.

These values are informational. Interceptor launch still requires a real missile
track from a linked active radar.
