# WarTech Reforged Controls Guide

This guide applies to WarTech Reforged `1.5.0` with HBM NTM `1.0.27 X5751` or HBM NTM Space `X5758 H261`. The default Minecraft controls are assumed. If key bindings were changed, use the bound keys instead.

## English

### Control notation

- **RMB**: right mouse button.
- **Shift+RMB**: hold the Minecraft Sneak key and right-click.
- **W / S**: drive forward / reverse.
- **A / D**: steer left / right.
- **Left Shift**: leave a vehicle with the default Minecraft key bindings.
- A vehicle or launcher must normally be right-clicked on its visible body or collision surface.

### Placing equipment

- Hold the equipment item and use **RMB on the top face of a solid block**.
- Large vehicles require enough free space around and above the placement point.
- Place the HEMTT, Ural, radar vehicles, and EW units on reasonably level terrain.
- Do not place two large entities into the same space.

### Target selector

The standard WarTech target selector is used by cruise missiles, ballistic missiles, Storm Shadow, Geran-2, and AGM-88.

1. Hold the target selector.
2. Use **RMB on the target block**. The selected coordinates are stored in the item and shown in its tooltip.
3. Open the launcher with **RMB**.
4. Put the configured selector into the targeting slot.
5. Load a compatible missile and provide HBM energy.
6. Activate the armed launcher with a redstone signal. Use an interface launch control instead when that launcher explicitly provides one.

The selector stores coordinates, not an entity. For AGM-88, select a point close enough to the expected hostile emitter; the missile performs its own emitter search around that point.

### Cruise and ballistic launchers

- **RMB**: open the launcher interface.
- Load only a missile accepted by that launcher type.
- Insert a configured target selector into the targeting slot.
- Insert an HBM battery or supply compatible HBM energy where the interface allows it.
- Apply a redstone signal after the launcher is armed and ready.
- Cruise and ballistic launchers retain their original manual targeting workflow. They do not automatically use the air-defense radar network.

### Storm Shadow

- Storm Shadow uses the normal cruise-missile launcher workflow.
- Configure a target selector, insert it with the missile, provide energy, and trigger the launcher.
- Its guidance and separated group routes are automatic after launch.

### Geran-2 rail catapult

- **RMB** on the catapult: open its launch interface.
- Put a Geran-2 drone in the missile/drone slot.
- Put a configured target selector in the targeting slot.
- Supply at least the required launcher energy with a compatible HBM battery or power source.
- Apply a redstone signal to launch.
- Valid target distance is approximately 20 to 1,000 blocks.
- Climb, cruise, route separation, and terminal descent are calculated automatically.

### MQ-9 Reaper

- **RMB** on the parked MQ-9: open ground control.
- Load up to six compatible unified aviation weapons into the hardpoints, a compatible charged HBM battery into `BAT`, and up to 16 countermeasure packs into `LTC`. The MQ-9 accepts AGM-114, HJ-10, GBU-12, Mk 82, and JDAM; fighter-only heavy weapons are rejected. A newly placed MQ-9 has `0 HE` and cannot launch without being charged.
- Set coordinates on an HBM designator, then use **RMB on the MQ-9 while holding it** to append a strike point. Up to six points are retained.
- Use **Shift+RMB with the designator** to discard the old route and start a new target list with the selected point. `LAST` removes the last queued point; `ALL` clears the complete list.
- Select a weapon and press `LAUNCH MISSION`, or use **Shift+RMB with an empty hand** on the parked MQ-9.
- The MQ-9 attacks queued points in order, consuming one available weapon per point, then returns and lands automatically.
- Maximum mission radius is **2,400 blocks from the recorded takeoff point**. Every queued point must be inside this radius.
- AGM-114 has a 95-block release range; HJ-10 extends the light-missile envelope to 145 blocks. GBU-12 releases at 90 blocks, JDAM glides from 245 blocks, and unguided Mk 82 dynamically calculates a 20-66 block release from current altitude and velocity.
- Flares deploy visibly during the final approach of an interceptor or shortly before a locked Pantsir gun burst. One pack starts a short countermeasure window; decoy probability is 25% against tier 1, 15% against tier 2, and 10% against tier 3. A failed decoy remains visible but does not prevent the hit.
- A lethally damaged airborne MQ-9 enters a smoking descent, explodes and burns on impact, drops its surviving inventory, and leaves a persistent collidable wreck instead of disappearing.
- Hold the reusable **WarTech Salvage Wrench** and use **Shift+RMB** on a landed MQ-9 wreck to dismantle and remove it without another explosion.

### F-16C and Su-27 tactical aviation

- **RMB** on a parked aircraft opens the unified aviation mission interface. Load an HBM battery into `BAT`, countermeasures into `LTC`, and weapons into the available hardpoints.
- The **F-16C** is the faster profile with four hardpoints, a 3,000-block mission radius, lower energy consumption, and lower durability.
- The **Su-27** is the heavier profile with six hardpoints, a 3,400-block mission radius, more energy reserve, and greater durability.
- Both fighters accept the complete unified ground-strike catalogue: AGM-114, HJ-10, AGM-65, Kh-29, GBU-12, Mk 82, KAB-500L, and JDAM. The same ammunition items, ranges, guidance, accuracy, blast, and energy costs also apply when a compatible item is used by the MQ-9.
- Missile release ranges are AGM-114 95, HJ-10 145, AGM-65 285, and Kh-29 410 blocks. Bomb release ranges are GBU-12 90, KAB-500L 155, JDAM 245, and a dynamic 20-66 blocks for Mk 82.
- Configure up to six points with the HBM designator exactly as on the MQ-9. `LAST`, `ALL`, weapon selection, `LAUNCH MISSION`, **Shift+RMB launch/return**, automatic routing, energy consumption, countermeasures, crash behavior, and salvage-wrench removal all follow the same controls.
- One loaded weapon is consumed per queued strike point. An F-16 cannot use its two locked slots, and incompatible payloads cannot be inserted through shift-clicking, automation, or the GUI.
- Load **WT-AAM Skyguard** missiles and press the `STRIKE / INTERCEPT` button to arm fighter-interceptor mode. The parked fighter then waits for a hostile track from a powered radar and a deployed, powered command Ural belonging to the same scoreboard team.
- In interceptor mode the network patrol radius is **6,500 blocks for F-16C** and **8,000 blocks for Su-27**. Their normal ground-strike mission radii remain 3,000 and 3,400 blocks.
- After a confirmed track is distributed, the F-16 reacts in about 2.75 seconds and the Su-27 in about 3.25 seconds. It takes off, pursues the moving contact, releases exactly one AAM, and returns automatically. AAMs can engage hostile aircraft, cruise missiles, Kh-555 missiles, Geran drones, and MQ-9 UAVs; ballistic targets remain assigned to ground-based air defense.
- The fighter and target reservation system prevents several aircraft from immediately spending missiles on the same contact. If the assigned target disappears before release, the fighter aborts and returns instead of firing blindly.

### Tu-95 strategic missile carrier and Kh-555

- **RMB** on the parked Tu-95 opens strategic aviation control. Load up to six **Kh-555** missiles, one compatible charged HBM battery into `BAT`, and up to 16 countermeasure packs into `LTC`.
- Set coordinates on an HBM designator and use **RMB on the aircraft while holding it** to append up to six targets. **Shift+RMB with the designator** replaces the route; `LAST` and `ALL` remove queued targets.
- Press `LAUNCH MISSION`, or use **Shift+RMB with an empty hand**. Every target must be 250-8,000 blocks from the recorded airfield.
- With one target, all six loaded missiles form a timed salvo. With several targets, loaded hardpoints are distributed between them. Every Kh-555 uses the Tier 2 cruise-missile flight model and an independently randomized route.
- The Tu-95 climbs to its cruise level and releases from a randomized standoff corridor approximately 1,700-1,900 blocks from a distant target. It does not overfly the target. For a closer target it first flies away on the reciprocal heading and releases from a safe cruise distance.
- Kh-555 also works in the standard WarTech cruise-missile launch tube. Its normal valid range is 250-2,000 blocks.
- The aircraft returns and lands automatically after release. `RETURN TO BASE` or **Shift+RMB** while airborne aborts the remaining mission. A destroyed aircraft crashes physically and its landed wreck can be removed with **Shift+RMB using the WarTech Salvage Wrench**.

### AGM-88 HARM

1. Configure a target selector near the suspected radar, jammer, decoy, or deployed command post.
2. Load the AGM-88 and selector into a compatible cruise-launch system.
3. Supply energy and trigger the launcher.
4. The missile searches for hostile emitters within 1,200 blocks of the designated point.

An active jammer receives high seeker priority. If an emitter switches off after detection, the missile continues toward its last known position with accuracy degrading over time. Scoreboard-team ownership is used for basic IFF.

### Patriot, S-400, and interceptor launchers

- **RMB**: open the launcher interface.
- Load one or more compatible WTI interceptor missiles into the missile cells.
- Provide compatible HBM energy.
- **Do not insert a target selector.** Air-defense launchers acquire confirmed airborne threats automatically through the radar/command network.
- Keep a powered, active radar within network range. A deployed and powered Ural command post improves the connected network view.
- The launcher fires automatically when it has a valid track, a compatible interceptor, enough energy, and the target is within the interceptor's engagement range.

Interceptor engagement ranges:

- **WTI-1 Falcon**: 100 blocks.
- **WTI-2 Lance**: 250 blocks.
- **WTI-3 Sentinel**: 400 blocks.

If the first interceptor misses or fails, another launcher waits roughly five to seven seconds before reacting to the same threat. A failed interceptor still leaves toward the threat, then diverts away and can explode or ignite terrain on impact.

### Tor-M1 and Pantsir-S2 mobile air defense

- Place the vehicle item on solid ground. **RMB while retracted** enters the driver's seat.
- **W / S**: accelerate and reverse. **A / D**: steer. **Left Shift**: dismount.
- **Shift+RMB**: deploy or retract the system without moving it to the player.
- **RMB while deployed**: open the combat interface.
- **RMB with a compatible HBM battery**: transfer energy directly. A battery can also remain in the dedicated GUI slot and is drained during operation.
- Use the GUI radar control to switch the integrated radar on or off. Select `HOLD`, `AUTO`, or `EMERGENCY` as the fire mode.
- `HOLD` tracks contacts without launching. `AUTO` performs normal autonomous engagement. `EMERGENCY` allows a shorter reaction interval and faster follow-up launches.

**9K331 Tor-M1:** load up to eight **WTI-2 Lance** interceptors. Its integrated X-band radar detects to 340 blocks with a 260-block ceiling; missile engagement range is 220 blocks. Tor uses vertical launch and does not accept Pantsir cannon belts.

**96K6 Pantsir-S2:** load up to twelve **WTI-1 Falcon** interceptors and one 30 mm ammunition belt. Its radar detects to 260 blocks with a 190-block ceiling; missile and cannon engagement range is 100 blocks. The twin cannon is a last-ditch layer with finite rounds, visible tracers, sound, energy consumption, and a non-guaranteed hit chance. MQ-9 flare clouds can disturb both its missiles and gun solution.

Both systems must be deployed, powered, radar-enabled, armed, and outside `HOLD` to fire. The interface labels the exact compatible interceptor and reports energy, health, contacts, ammunition, and operating state.

### Renault TRM mobile radar

- **RMB with an empty hand**: open the radar interface.
- **Shift+RMB**: switch the radar on or off.
- **RMB while holding a compatible HBM battery**: transfer energy directly into the radar.
- In the interface, place a compatible HBM battery in the battery slot.
- Use the interface toggle button to enable or disable radar operation.
- The display shows power, state, contacts, range, ceiling, and up to eight synchronized moving blips.

The TRM has a detection range of up to 600 blocks and a ceiling of up to 500 blocks. It must be powered and enabled to create tracks.

### S-400 long-range radar

- **RMB with an empty hand**: open the radar interface.
- **Shift+RMB**: deploy or retract the radar.
- **RMB while holding a compatible HBM battery**: transfer energy directly into the radar.
- In the interface, use the battery slot and the deploy/retract control.
- The S-400 radar supports a range of up to 1,200 blocks, a ceiling of up to 900 blocks, and up to 32 tracked contacts.

The radar only joins the tracking network while deployed, powered, and operational.

### Ural air-defense command post

- **RMB while retracted**: enter the driver's seat.
- **Shift+RMB**: deploy or retract the command post. The vehicle remains at its current position.
- **RMB while deployed**: open the command-network interface.
- **RMB while holding a compatible HBM battery**: transfer energy directly into the vehicle.
- In the interface, place an HBM battery in the persistent battery slot.
- **W / S / A / D**: drive while retracted.
- **Left Shift**: leave the driver's seat.

The command post does not move while deployed. Its interface reports connected radars, launchers, confirmed tracks, hostile emitters, and active interceptions. It must be deployed and powered to participate fully in the network.

### Automatic HBM air-raid siren

- Place an **Air-Raid Siren Relay** within 96 blocks of a deployed, powered command Ural from the same scoreboard team.
- Place an HBM siren directly beside the relay, or connect it with ordinary redstone wiring.
- When the command network has at least one confirmed hostile airborne contact, the relay outputs redstone power level 15. It switches off automatically after the last hostile track is lost.
- A relay ignores command posts from other teams. Place it after joining the intended scoreboard team so it records the correct ownership.

### Long-range communication mast

- Place a **Long-Range Communication Mast** near each remote radar, command Ural, siren sector, launcher group, or fighter airfield that must join the same network.
- The structure is **seven blocks tall** and needs six clear blocks above its base. Break and place again any mast created by an older development build so the full tower is generated.
- Every powered mast forms links up to **2,400 blocks** away. Chains of same-team masts relay confirmed tracks, command links, air-raid alarms, and fighter cueing across a large territory.
- **RMB with a compatible HBM battery** transfers energy. The mast stores up to 500,000 HE and consumes 20 HE per tick while online.
- **RMB with an empty hand** reports power, state, range, and the number of reachable relay nodes. **Shift+RMB** enables or disables it.
- An online mast keeps its local 3x3-chunk sector active. Put the associated radar, Ural, siren relay, or parked fighter in that sector when it must operate without a nearby player.
- A broken, disabled, or empty mast immediately breaks routes that have no alternate path. Masts record the scoreboard team of the player who placed them and do not bridge hostile networks.

### Electronic-warfare units

These controls apply to the Synytsia jammer, passive ESM array, and radar decoy.

- **Shift+RMB**: switch the unit online or offline.
- **RMB while holding a compatible HBM battery**: recharge the unit.
- **RMB with another item or an empty hand on Synytsia**: cycle L, S, X, and wideband modes.
- **RMB with another item or an empty hand on the radar decoy**: cycle its emission band.
- **RMB with another item or an empty hand on passive ESM**: report detected emitter count, range, and operating state in chat.

Synytsia affects radar tracking within roughly 350 blocks. Passive ESM listens for emitters within roughly 900 blocks. An offline or unpowered unit does not provide its active function.

### HEMTT mobile artillery platform

#### Installing a weapon module

- Hold the HBM **Greg artillery turret block** or **Henry rocket turret block**.
- Use **RMB on an empty HEMTT platform** to install it.
- Only one module can be installed on a platform. Greg and Henry are mutually exclusive.

#### Driving and deployment

- **RMB while retracted**: enter the driver's seat.
- **W / S / A / D**: drive and steer.
- **Left Shift**: leave the driver's seat.
- **Shift+RMB**: deploy or retract the stabilizers. A weapon module must be installed first.
- The platform cannot drive while deployed.

#### Turret interface and firing

- **RMB while deployed**: open the full native HBM interface of the mounted Greg or Henry turret.
- Load ammunition and batteries and select the weapon's supported operating mode inside that HBM interface.
- Configure any supported HBM/WarTech designator with target coordinates, then use **RMB on the deployed vehicle while holding that designator** to transfer the target.
- To use the HBM artillery range designator, deploy the platform and use **RMB on the vehicle while holding it**. This links the designator to the mobile turret proxy.
- The vehicle confirms accepted target coordinates in chat.

### Scoreboard teams and IFF

The **WarTech IFF Configurator** in the Support tab is the recommended setup method:

- **RMB** opens the selector. Choose `ALPHA`, `BRAVO`, `CHARLIE`, `DELTA`, or `PERSONAL`.
- The selected team is stored in persistent player data, mirrored to the vanilla scoreboard, and restored after reconnecting, changing dimensions, or dying.
- **Shift+RMB on a WarTech vehicle, radar, EW unit, communication mast, or siren relay** rebinds already placed equipment to the player's current IFF team.
- `!wtteam status`, `!wtteam <name>`, and `!wtteam personal` provide a chat fallback and support custom team names.

Vanilla scoreboard commands remain supported:

```text
/scoreboard teams add red
/scoreboard teams join red PlayerName
```

Use different team names for opposing sides. Equipment placed before joining can now be rebound with the IFF Configurator instead of being dismantled.

Aircraft, UAVs, Geran drones, Kh-555 missiles, mobile/fixed launchers, radars, command vehicles, and their spawned guided weapons pass this team identity through the tracking network. A radar and its launchers ignore tracks carrying the same non-empty team identity; hostile or unowned tracks remain eligible targets. Empty-team equipment remains interoperable for ordinary single-player worlds, but scoreboard teams are required for reliable multiplayer IFF.

### Quick troubleshooting

- **Launcher does not fire:** check missile compatibility, target-selector coordinates, energy, redstone input, and required distance.
- **Air defense does not react:** check interceptor type, energy, engagement range, radar power/state, and whether the target is a confirmed hostile track.
- **Radar interface is empty:** insert a battery, enable/deploy the radar, and verify that a real airborne threat is inside its range and ceiling.
- **Ural interface does not open:** deploy it first with Shift+RMB, then use normal RMB.
- **HEMTT turret interface does not open:** install exactly one module and deploy the platform before normal RMB.
- **AGM-88 ignores a target:** designate near an active hostile emitter and verify scoreboard-team ownership.
- **Geran-2 does not launch:** use a Geran-2 item, a configured selector, enough energy, a 20-1,000 block target, and a redstone signal.

### ODIN kinetic bombardment satellite

1. Put **ODIN** in the right-hand random-frequency slot of the HBM Satellite Linker and remove it after a non-zero frequency/ID appears in its tooltip.
2. Put ODIN in the linker's left slot and the desired HBM controller in the middle slot. Link a **Satellite Interface** to select a strike point on its map, a **satellite laser designator** for direct RMB strikes, or a **Satellite Coordinate Interface (`sat_coord`)** for typed coordinates. Each controller item stores its own frequency and must be linked separately.
3. Launch ODIN through the standard HBM Soyuz satellite deployment system. The Soyuz reads this frequency as the satellite ID.
4. Choose one linked control method:
   - **Satellite Interface:** open it, select the linked ODIN frequency, and click the desired point on the map.
   - **Satellite Coordinate Interface (`sat_coord`):** enter the target coordinates and send the command.
   - **Satellite laser designator:** aim at a block within range and use RMB.
5. Chat confirms an accepted strike and reports rods remaining. During cooldown it reports the remaining seconds; when empty it reports that no rods remain.

Each satellite carries four rods and enforces a 60-second interval between releases. A descending rod is a Tier 3 ballistic radar contact, keeps its flight chunks loaded, and can be intercepted during the warning and terminal-descent phases. The impact is a large non-nuclear kinetic explosion: it creates no radiation or nuclear mushroom cloud.

Tier-3 long-range tracking is intentionally not instantaneous: the contact must exist for about 2.5 seconds and rise at least 18 blocks above local terrain before a stable radar track is distributed. This prevents an interceptor launch in the exact tick of departure while preserving a useful warning window.

---

## Русский

### Обозначения

- **ПКМ**: правая кнопка мыши.
- **Shift+ПКМ**: удерживать клавишу приседания Minecraft и нажать ПКМ.
- **W / S**: движение вперёд / задний ход.
- **A / D**: поворот влево / вправо.
- **Левый Shift**: выход из машины при стандартных настройках Minecraft.
- По машине или пусковой обычно нужно нажимать на видимую модель либо её физическую поверхность.

### Размещение техники

- Возьмите предмет техники и нажмите **ПКМ по верхней грани твёрдого блока**.
- Для крупной техники требуется свободное место вокруг и над точкой установки.
- HEMTT, Урал, РЛС и средства РЭБ лучше размещать на достаточно ровной поверхности.
- Не ставьте две крупные сущности в одно и то же пространство.

### Целеуказатель

Стандартный целеуказатель WarTech применяется для крылатых и баллистических ракет, Storm Shadow, «Герани-2» и AGM-88.

1. Возьмите целеуказатель в руку.
2. Нажмите **ПКМ по блоку цели**. Координаты сохранятся в предмете и появятся в его подсказке.
3. Откройте пусковую установку обычным **ПКМ**.
4. Поместите настроенный целеуказатель в слот целеуказания.
5. Загрузите совместимую ракету и обеспечьте установку энергией HBM.
6. Подайте сигнал красного камня на готовую пусковую. Если конкретная установка имеет отдельную кнопку запуска в интерфейсе, используйте её.

Целеуказатель запоминает координаты, а не сущность. Для AGM-88 укажите точку достаточно близко к предполагаемому вражескому излучателю: ракета самостоятельно ищет источник вокруг этой точки.

### Пусковые крылатых и баллистических ракет

- **ПКМ**: открыть интерфейс пусковой.
- Загружайте только ракету, совместимую с данным типом установки.
- Вставьте настроенный целеуказатель в слот целеуказания.
- Вставьте аккумулятор HBM или подайте совместимую энергию HBM, если интерфейс это поддерживает.
- После готовности установки подайте сигнал красного камня.
- Крылатые и баллистические установки сохраняют исходную ручную схему целеуказания и не используют сеть ПВО автоматически.

### Storm Shadow

- Storm Shadow запускается по стандартной схеме крылатой ракеты.
- Настройте целеуказатель, вставьте его вместе с ракетой, подайте энергию и активируйте установку.
- Наведение и раздельные маршруты группового пуска работают автоматически.

### Рельсовая катапульта «Герани-2»

- **ПКМ** по катапульте: открыть интерфейс запуска.
- Положите БПЛА «Герань-2» в слот ракеты/дрона.
- Положите настроенный целеуказатель в слот целеуказания.
- Обеспечьте необходимый запас энергии совместимым аккумулятором или источником HBM.
- Подайте сигнал красного камня для запуска.
- Допустимая дальность цели составляет примерно от 20 до 1 000 блоков.
- Набор высоты, маршевый участок, разделение маршрутов и терминальное снижение рассчитываются автоматически.

### MQ-9 Reaper

- **ПКМ** по стоящему MQ-9: открыть наземный пункт управления.
- Загрузите до шести совместимых унифицированных авиационных боеприпасов, заряженный аккумулятор HBM в `BAT` и до 16 кассет ЛТЦ в `LTC`. MQ-9 принимает AGM-114, HJ-10, GBU-12, Mk 82 и JDAM; тяжёлое самолётное вооружение блокируется. Новый MQ-9 появляется с `0 HE` и без зарядки не запускается.
- Настройте координаты на целеуказателе HBM, затем нажмите **ПКМ по MQ-9 с целеуказателем в руке**, чтобы добавить точку удара. Сохраняется до шести точек.
- **Shift+ПКМ с целеуказателем** удаляет старый маршрут и начинает новый список с выбранной точки. `LAST` удаляет последнюю точку, а `ALL` полностью очищает очередь.
- Выберите оружие и нажмите `LAUNCH MISSION` либо используйте **Shift+ПКМ пустой рукой** по стоящему MQ-9.
- MQ-9 атакует точки по порядку, расходуя по одному доступному боеприпасу на точку, затем автоматически возвращается и садится.
- ЛТЦ заметно отстреливаются на конечном сближении ракеты-перехватчика либо незадолго до очереди уже наведённых пушек «Панциря». Одна кассета создаёт короткое окно противодействия; шанс отвода составляет 25% против ракет 1-го тира, 15% против 2-го и 10% против 3-го. Неудачный отстрел виден, но не предотвращает попадание.
- Получивший смертельный урон MQ-9 не исчезает: он падает с дымом и огнём, взрывается при ударе, выбрасывает уцелевшее содержимое и оставляет физический обломок корпуса.
- Возьмите многоразовый **демонтажный ключ WarTech** и нажмите **Shift+ПКМ** по лежащему обломку MQ-9, чтобы разобрать и убрать его без повторного взрыва.
- Максимальный радиус миссии составляет **2 400 блоков от записанной точки взлёта**. Каждая точка очереди должна находиться внутри этого радиуса.
- AGM-114 сбрасывается с 95 блоков, HJ-10 расширяет рубеж лёгких ракет до 145 блоков, GBU-12 сбрасывается с 90 блоков, а JDAM планирует с 245 блоков. Для неуправляемой Mk 82 точка сброса динамически рассчитывается в пределах 20-66 блоков по высоте и скорости.
- ЛТЦ отстреливаются автоматически при приближении противоракеты или очереди Панциря. Одна кассета создаёт короткое окно помех; шанс отвода равен 25% для Т1, 15% для Т2 и 10% для Т3.

### Тактическая авиация F-16C и Су-27

- **ПКМ** по стоящему самолёту открывает унифицированный авиационный интерфейс. Установите аккумулятор HBM в `BAT`, ЛТЦ в `LTC` и боеприпасы в доступные узлы подвески.
- **F-16C** быстрее, имеет четыре узла подвески, радиус миссии 3 000 блоков, меньший расход энергии и меньшую прочность.
- **Су-27** тяжелее, имеет шесть узлов подвески, радиус миссии 3 400 блоков, больший запас энергии и повышенную прочность.
- Оба самолёта принимают полный унифицированный набор: AGM-114, HJ-10, AGM-65, Х-29, GBU-12, Mk 82, КАБ-500Л и JDAM. Для совместимого носителя используются те же предметы, дальности, наведение, точность, мощность и расход энергии, что и у MQ-9.
- Рубежи ракет: AGM-114 95, HJ-10 145, AGM-65 285 и Х-29 410 блоков. Рубежи бомб: GBU-12 90, КАБ-500Л 155, JDAM 245 и динамические 20-66 блоков у Mk 82.
- До шести целей задаются целеуказателем HBM так же, как для MQ-9. Кнопки `LAST`, `ALL`, выбор оружия, `LAUNCH MISSION`, **Shift+ПКМ для запуска/возврата**, автоматические маршруты, расход энергии, ЛТЦ, падение и демонтаж ключом работают одинаково.
- На каждую точку расходуется один боеприпас. Два заблокированных слота F-16 нельзя использовать, а несовместимое оружие нельзя вставить через интерфейс, Shift-клик или автоматизацию.
- Загрузите ракеты **WT-AAM Skyguard** и нажмите кнопку `STRIKE / INTERCEPT`, чтобы включить режим истребителя-перехватчика. Стоящий самолёт будет ждать вражескую трассу от запитанной РЛС и развёрнутого запитанного командного Урала своей scoreboard-команды.
- В режиме перехватчика сетевой радиус дежурства составляет **6 500 блоков для F-16C** и **8 000 блоков для Су-27**. Обычная дальность ударных миссий остаётся прежней: 3 000 и 3 400 блоков.
- После подтверждения трассы F-16 реагирует примерно за 2,75 секунды, Су-27 — примерно за 3,25 секунды. Самолёт взлетает, преследует движущуюся цель, выпускает ровно одну AAM и автоматически возвращается. Перехватываются вражеские самолёты, крылатые ракеты, Х-555, «Герани» и MQ-9; баллистические цели остаются задачей наземного ПВО.
- Резервирование цели не позволяет нескольким истребителям немедленно потратить ракеты на один контакт. Если цель исчезла до пуска, самолёт прекращает вылет и возвращается без выстрела.

### Стратегический ракетоносец Ту-95 и Х-555

- **ПКМ** по стоящему Ту-95 открывает управление стратегической авиацией. Загрузите до шести ракет **Х-555**, заряженный совместимый аккумулятор HBM в `BAT` и до 16 кассет ЛТЦ в `LTC`.
- Задайте координаты целеуказателем HBM и нажмите **ПКМ по самолёту с целеуказателем в руке**, чтобы добавить до шести целей. **Shift+ПКМ с целеуказателем** заменяет маршрут; `LAST` и `ALL` удаляют точки из очереди.
- Нажмите `LAUNCH MISSION` либо **Shift+ПКМ пустой рукой**. Каждая цель должна находиться в 250-8 000 блоках от записанного аэродрома.
- При одной цели все шесть загруженных ракет образуют последовательный залп. При нескольких целях узлы подвески распределяются между ними. Каждая Х-555 использует модель полёта крылатой ракеты Tier 2 и отдельный случайный маршрут.
- Ту-95 набирает эшелон и выполняет пуск из случайного коридора примерно в 1 700-1 900 блоках от дальней цели, не пролетая над ней. Для близкой цели самолёт сначала уходит от неё обратным курсом и запускает ракету с безопасной дистанции.
- Х-555 также совместима со стандартным контейнером запуска крылатых ракет WarTech. Обычная допустимая дальность составляет 250-2 000 блоков.
- После залпа самолёт автоматически возвращается и садится. `RETURN TO BASE` или **Shift+ПКМ** в полёте отменяет оставшуюся часть задания. Сбитый самолёт физически падает, а лежащий остов удаляется **Shift+ПКМ демонтажным ключом WarTech**.

### AGM-88 HARM

1. Настройте целеуказатель рядом с предполагаемой РЛС, станцией помех, ложным излучателем или развёрнутым командным пунктом.
2. Загрузите AGM-88 и целеуказатель в совместимую систему запуска крылатых ракет.
3. Подайте энергию и активируйте пусковую.
4. Ракета ищет вражеские излучатели в радиусе 1 200 блоков от указанной точки.

Активная станция помех имеет высокий приоритет для головки наведения. Если обнаруженный излучатель выключится, ракета продолжит полёт к его последней известной позиции, постепенно теряя точность. Для базового распознавания своих используется команда scoreboard.

### Пусковые Patriot, С-400 и другие установки перехватчиков

- **ПКМ**: открыть интерфейс пусковой.
- Загрузите одну или несколько совместимых ракет-перехватчиков WTI в ракетные ячейки.
- Обеспечьте установку совместимой энергией HBM.
- **Не вставляйте целеуказатель.** Пусковые ПВО автоматически получают подтверждённые воздушные цели от сети РЛС и командного пункта.
- В радиусе сети должна работать запитанная и включённая РЛС. Развёрнутый и запитанный Урал улучшает общую работу сети.
- Пуск происходит автоматически, когда существует подтверждённая цель, загружен подходящий перехватчик, достаточно энергии и цель находится в его зоне поражения.

Дальность перехватчиков:

- **WTI-1 «Сокол»**: 100 блоков.
- **WTI-2 «Копьё»**: 250 блоков.
- **WTI-3 «Страж»**: 400 блоков.

Если первый перехватчик промахнулся или отказал, другая пусковая реагирует на ту же угрозу примерно через пять-семь секунд. Неудачный перехватчик всё равно уходит в сторону цели, затем отклоняется и при падении может взорваться либо поджечь местность.

### Мобильные комплексы «Тор-М1» и «Панцирь-С2»

- Установите предмет машины на твёрдую поверхность. **ПКМ в походном положении**: занять водительское место.
- **W / S**: движение вперёд и назад. **A / D**: поворот. **Левый Shift**: выйти.
- **Shift+ПКМ**: развернуть или свернуть комплекс без телепортации к игроку.
- **ПКМ в развёрнутом положении**: открыть боевой интерфейс.
- **ПКМ с совместимым аккумулятором HBM**: напрямую передать энергию. Аккумулятор также можно оставить в отдельном слоте интерфейса; во время работы он реально разряжается.
- Кнопка РЛС включает или выключает встроенный радар. Режим огня выбирается между `HOLD`, `AUTO` и `EMERGENCY`.
- `HOLD` только сопровождает цели. `AUTO` ведёт обычный автоматический огонь. `EMERGENCY` уменьшает время реакции и интервал между повторными пусками.

**9К331 «Тор-М1»:** до восьми перехватчиков **WTI-2 «Копьё»**. Встроенная РЛС X-диапазона обнаруживает цели на дальности до 340 блоков и высоте до 260 блоков; дальность пуска составляет 220 блоков. Пуск вертикальный, ленты пушек «Панциря» не принимаются.

**96К6 «Панцирь-С2»:** до двенадцати перехватчиков **WTI-1 «Сокол»** и одна лента 30-мм боеприпасов. РЛС обнаруживает цели на дальности до 260 блоков и высоте до 190 блоков; ракетный и пушечный рубеж составляет 100 блоков. Спаренные пушки являются последним рубежом: расходуют боекомплект и энергию, имеют видимые трассеры и звук, а попадание не гарантировано. Облако ЛТЦ MQ-9 мешает и ракетам, и расчёту пушечного упреждения.

Оба комплекса стреляют только в развёрнутом состоянии, при наличии энергии, включённой РЛС, подходящего боекомплекта и режиме не `HOLD`. Интерфейс прямо показывает совместимую ракету, энергию, прочность, цели, боекомплект и состояние комплекса.

### Мобильная РЛС Renault TRM

- **ПКМ пустой рукой**: открыть интерфейс РЛС.
- **Shift+ПКМ**: включить или выключить РЛС.
- **ПКМ с совместимым аккумулятором HBM в руке**: напрямую передать энергию РЛС.
- В интерфейсе можно поместить аккумулятор HBM в отдельный слот.
- Кнопка в интерфейсе включает и выключает работу РЛС.
- Экран показывает энергию, состояние, количество целей, дальность, потолок и до восьми синхронизированных движущихся отметок.

Дальность TRM составляет до 600 блоков, потолок обнаружения — до 500 блоков. Для создания трасс РЛС должна быть запитана и включена.

### Дальняя РЛС С-400

- **ПКМ пустой рукой**: открыть интерфейс РЛС.
- **Shift+ПКМ**: развернуть или свернуть РЛС.
- **ПКМ с совместимым аккумулятором HBM в руке**: напрямую передать энергию РЛС.
- В интерфейсе используйте слот аккумулятора и кнопку развёртывания/сворачивания.
- Дальность РЛС С-400 составляет до 1 200 блоков, потолок — до 900 блоков, одновременно сопровождается до 32 целей.

РЛС подключается к сети сопровождения только в развёрнутом, запитанном и рабочем состоянии.

### Командный пункт ПВО на базе Урала

- **ПКМ в походном положении**: сесть на водительское место.
- **Shift+ПКМ**: развернуть или свернуть командный пункт. Машина остаётся на текущем месте.
- **ПКМ в развёрнутом положении**: открыть интерфейс командной сети.
- **ПКМ с совместимым аккумулятором HBM в руке**: напрямую передать энергию машине.
- В интерфейсе установите аккумулятор HBM в постоянный слот.
- **W / S / A / D**: управление машиной в походном положении.
- **Левый Shift**: покинуть водительское место.

Развёрнутый командный пункт не двигается. Интерфейс показывает подключённые РЛС и пусковые, подтверждённые цели, вражеские излучатели и активные перехваты. Для полноценной работы в сети машина должна быть развёрнута и запитана.

### Автоматическая воздушная тревога через сирену HBM

- Поставьте **реле сирены воздушной тревоги** не дальше 96 блоков от развёрнутого и запитанного командного Урала той же scoreboard-команды.
- Поставьте сирену HBM вплотную к реле либо соедините её обычной цепью красного камня.
- Пока командная сеть сопровождает хотя бы одну подтверждённую вражескую воздушную цель, реле выдаёт сигнал красного камня силой 15. После потери последней трассы сигнал отключается автоматически.
- Реле игнорирует командные пункты других команд. Устанавливайте его после вступления в нужную scoreboard-команду.

### Мачта дальней связи

- Устанавливайте **мачту дальней связи** рядом с удалённой РЛС, командным Уралом, сектором сирен, группой пусковых или аэродромом истребителей, который должен войти в общую сеть.
- Конструкция имеет высоту **семь блоков** и требует шесть свободных блоков над основанием. Мачту из старой тестовой сборки нужно сломать и установить заново, чтобы появилась полная конструкция.
- Каждая запитанная мачта связывается с узлами на расстоянии до **2 400 блоков**. Цепочка мачт одной scoreboard-команды передаёт подтверждённые трассы, командные связи, воздушную тревогу и целеуказание истребителям по большой территории.
- **ПКМ с совместимым аккумулятором HBM** передаёт энергию. Мачта хранит до 500 000 HE и расходует 20 HE за тик во включённом состоянии.
- **ПКМ пустой рукой** показывает заряд, состояние, радиус и число доступных ретрансляторов. **Shift+ПКМ** включает или выключает мачту.
- Включённая мачта удерживает загруженным локальный сектор 3x3 чанка. Размещайте обслуживаемую РЛС, КШМ, реле сирены или дежурный самолёт в этом секторе, если они должны работать без игрока поблизости.
- Разрушенная, выключенная или разряженная мачта немедленно обрывает маршрут, если нет запасного пути. Мачта запоминает scoreboard-команду установившего её игрока и не соединяет вражеские сети.

### Средства радиоэлектронной борьбы

Управление относится к станции «Синица», пассивной ESM и ложному радиолокационному излучателю.

- **Shift+ПКМ**: включить или выключить устройство.
- **ПКМ с совместимым аккумулятором HBM в руке**: зарядить устройство.
- **ПКМ пустой рукой или другим предметом по «Синице»**: переключить диапазон L, S, X или широкополосный режим.
- **ПКМ пустой рукой или другим предметом по ложному излучателю**: переключить диапазон излучения.
- **ПКМ пустой рукой или другим предметом по пассивной ESM**: вывести в чат количество обнаруженных излучателей, дальность и состояние.

«Синица» ухудшает радиолокационное сопровождение примерно в радиусе 350 блоков. Пассивная ESM обнаруживает излучатели примерно до 900 блоков. Выключенное или обесточенное устройство не выполняет активную функцию.

### Мобильная артиллерийская платформа HEMTT

#### Установка боевого модуля

- Возьмите блок артиллерийской турели HBM **Greg** или ракетной турели **Henry**.
- Нажмите **ПКМ по пустой платформе HEMTT**, чтобы установить модуль.
- На одной платформе может находиться только один модуль. Greg и Henry взаимоисключающие.

#### Движение и развёртывание

- **ПКМ в походном положении**: сесть на водительское место.
- **W / S / A / D**: ехать и поворачивать.
- **Левый Shift**: покинуть водительское место.
- **Shift+ПКМ**: выставить или убрать опоры. Сначала должен быть установлен боевой модуль.
- В развёрнутом положении платформа не может двигаться.

#### Интерфейс турели и стрельба

- **ПКМ в развёрнутом положении**: открыть полный штатный интерфейс установленной турели HBM Greg или Henry.
- В интерфейсе HBM загружайте боеприпасы и аккумуляторы и выбирайте поддерживаемый режим работы.
- Настройте любой совместимый целеуказатель HBM/WarTech, затем нажмите **ПКМ по развёрнутой машине с этим целеуказателем в руке**, чтобы передать координаты цели.
- Для артиллерийского дальномера-целеуказателя HBM сначала разверните платформу, затем нажмите **ПКМ по машине с дальномером в руке**. Предмет привяжется к мобильной турели.
- Машина подтверждает принятые координаты цели сообщением в чате.

### Команды scoreboard и система «свой-чужой»

Рекомендуемый способ настройки — **IFF-конфигуратор WarTech** во вкладке поддержки:

- **ПКМ** открывает меню выбора `ALPHA`, `BRAVO`, `CHARLIE`, `DELTA` или `PERSONAL`.
- Команда сохраняется в постоянных данных игрока, дублируется в vanilla scoreboard и восстанавливается после перезахода, смены измерения или смерти.
- **Shift+ПКМ по машине WarTech, РЛС, РЭБ, мачте связи или реле сирены** перепривязывает уже установленный объект к текущей IFF-команде игрока.
- `!wtteam status`, `!wtteam <название>` и `!wtteam personal` остаются текстовым вариантом и позволяют использовать произвольные названия.

Обычные команды vanilla scoreboard также поддерживаются:

```text
/scoreboard teams add red
/scoreboard teams join red PlayerName
```

Для противоборствующих сторон используйте разные названия команд. Ранее установленную технику теперь можно перепривязать IFF-конфигуратором без демонтажа.

Самолёты, БПЛА, «Герани», Х-555, мобильные и стационарные пусковые, РЛС, командные машины и создаваемое ими управляемое оружие передают эту принадлежность по сети сопровождения. РЛС и пусковые игнорируют трассы с той же непустой командой; чужие цели и цели без владельца остаются допустимыми. В одиночной игре техника без команды продолжает взаимодействовать, но для надёжного сетевого «свой-чужой» scoreboard-команды обязательны.

### Быстрая диагностика

- **Пусковая не запускается:** проверьте совместимость ракеты, координаты целеуказателя, энергию, сигнал красного камня и допустимую дальность.
- **ПВО не реагирует:** проверьте тип перехватчика, энергию, дальность поражения, питание и состояние РЛС, а также наличие подтверждённой вражеской трассы.
- **Интерфейс РЛС пуст:** установите аккумулятор, включите/разверните РЛС и убедитесь, что реальная воздушная угроза находится в пределах дальности и потолка.
- **Не открывается интерфейс Урала:** сначала разверните его через Shift+ПКМ, затем нажмите обычный ПКМ.
- **Не открывается интерфейс турели HEMTT:** установите ровно один модуль и разверните платформу перед обычным ПКМ.
- **AGM-88 игнорирует цель:** укажите точку рядом с активным вражеским излучателем и проверьте принадлежность к scoreboard-команде.
- **«Герань-2» не запускается:** нужны предмет «Герань-2», настроенный целеуказатель, достаточная энергия, цель на расстоянии 20-1 000 блоков и сигнал красного камня.

### Спутник кинетической бомбардировки «ОДИН»

1. Поместите **«ОДИН»** в правый слот генерации случайной частоты спутникового связующего HBM и заберите его после появления ненулевой частоты/ID в подсказке предмета.
2. Поместите «ОДИН» в левый слот связующего, а нужный контроллер HBM — в средний. Привяжите **спутниковый интерфейс** для выбора точки удара на карте, **спутниковый лазерный целеуказатель** для прямого удара по ПКМ или **координатный интерфейс спутника (`sat_coord`)** для ручного ввода координат. Каждый предмет-контроллер хранит собственную частоту и привязывается отдельно.
3. Запустите «ОДИН» через штатную ракету-носитель Soyuz HBM. Soyuz использует записанную частоту как ID спутника.
4. Используйте один из отдельно привязанных способов управления:
   - **Спутниковый интерфейс:** откройте его, выберите частоту «ОДИНА» и нажмите на нужную точку карты.
   - **Координатный интерфейс (`sat_coord`):** введите координаты цели и отправьте команду.
   - **Спутниковый лазерный целеуказатель:** наведитесь на блок в пределах дальности и нажмите ПКМ.
5. Чат подтверждает принятый удар и сообщает остаток стержней. Во время перезарядки он показывает оставшиеся секунды, а после исчерпания боезапаса сообщает об отсутствии стержней.

Один спутник несёт четыре стержня; между сбросами действует интервал 60 секунд. Падающий стержень отображается на РЛС как баллистическая цель Tier 3, самостоятельно прогружает чанки полёта и может быть перехвачен во время предупреждения и терминального снижения. Удар представляет собой мощный неядерный кинетический взрыв без радиации и ядерного гриба.

Дальняя РЛС получает устойчивую трассу Tier 3 не мгновенно: цель должна существовать около 2,5 секунды и подняться минимум на 18 блоков над локальным рельефом. Поэтому противоракета не стартует в ту же игровую секунду, но окно для перехвата сохраняется.
