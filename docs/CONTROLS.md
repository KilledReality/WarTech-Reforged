# WarTech Reforged Controls Guide

This guide applies to the WarTech Reforged `1.4.0` development line with HBM NTM `1.0.27 X5751`. The default Minecraft controls are assumed. If key bindings were changed, use the bound keys instead.

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
- Load up to six AGM-114, GBU-12, or Mk 82 weapons into the hardpoints, a compatible charged HBM battery into `BAT`, and up to 16 countermeasure packs into `LTC`. A newly placed MQ-9 has `0 HE` and cannot launch without being charged.
- Set coordinates on an HBM designator, then use **RMB on the MQ-9 while holding it** to append a strike point. Up to six points are retained.
- Use **Shift+RMB with the designator** to discard the old route and start a new target list with the selected point. `LAST` removes the last queued point; `ALL` clears the complete list.
- Select a weapon and press `LAUNCH MISSION`, or use **Shift+RMB with an empty hand** on the parked MQ-9.
- The MQ-9 attacks queued points in order, consuming one available weapon per point, then returns and lands automatically.
- Maximum mission radius is **2,400 blocks from the recorded takeoff point**. Every queued point must be inside this radius.
- AGM-114 is the most accurate weapon, GBU-12 retains a small guided dispersion, and unguided Mk 82 has the widest impact spread. Its release point is calculated from current altitude and velocity.
- Flares deploy visibly during the final approach of an interceptor or shortly before a locked Pantsir gun burst. One pack starts a short countermeasure window; decoy probability is 25% against tier 1, 15% against tier 2, and 10% against tier 3. A failed decoy remains visible but does not prevent the hit.
- A lethally damaged airborne MQ-9 enters a smoking descent, explodes and burns on impact, drops its surviving inventory, and leaves a persistent collidable wreck instead of disappearing.
- Hold the reusable **WarTech Salvage Wrench** and use **Shift+RMB** on a landed MQ-9 wreck to dismantle and remove it without another explosion.

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

New radars, command vehicles, EW units, and seekers record owner/team information when placed. For multiplayer teams, configure the vanilla scoreboard before placing equipment:

```text
/scoreboard teams add red
/scoreboard teams join red PlayerName
```

Use different team names for opposing sides. Equipment placed before joining a team may need to be picked up and placed again to receive the intended ownership.

### Quick troubleshooting

- **Launcher does not fire:** check missile compatibility, target-selector coordinates, energy, redstone input, and required distance.
- **Air defense does not react:** check interceptor type, energy, engagement range, radar power/state, and whether the target is a confirmed hostile track.
- **Radar interface is empty:** insert a battery, enable/deploy the radar, and verify that a real airborne threat is inside its range and ceiling.
- **Ural interface does not open:** deploy it first with Shift+RMB, then use normal RMB.
- **HEMTT turret interface does not open:** install exactly one module and deploy the platform before normal RMB.
- **AGM-88 ignores a target:** designate near an active hostile emitter and verify scoreboard-team ownership.
- **Geran-2 does not launch:** use a Geran-2 item, a configured selector, enough energy, a 20-1,000 block target, and a redstone signal.

### ODIN kinetic bombardment satellite

1. Launch the **ODIN Kinetic Bombardment Satellite** through the standard HBM satellite deployment system.
2. Open its standard HBM coordinate-control interface and select the impact coordinates.
3. Confirm the coordinate action. The satellite releases one tungsten rod above the selected point.

Each satellite carries four rods and enforces a 60-second interval between releases. A descending rod is a Tier 3 ballistic radar contact, keeps its flight chunks loaded, and can be intercepted during the warning and terminal-descent phases. The impact is a large non-nuclear kinetic explosion: it creates no radiation or nuclear mushroom cloud.

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
- Загрузите до шести AGM-114, GBU-12 или Mk 82 в узлы подвески, заряженный совместимый аккумулятор HBM в `BAT` и до 16 кассет ЛТЦ в `LTC`. Новый MQ-9 появляется с `0 HE` и без зарядки не запускается.
- Настройте координаты на целеуказателе HBM, затем нажмите **ПКМ по MQ-9 с целеуказателем в руке**, чтобы добавить точку удара. Сохраняется до шести точек.
- **Shift+ПКМ с целеуказателем** удаляет старый маршрут и начинает новый список с выбранной точки. `LAST` удаляет последнюю точку, а `ALL` полностью очищает очередь.
- Выберите оружие и нажмите `LAUNCH MISSION` либо используйте **Shift+ПКМ пустой рукой** по стоящему MQ-9.
- MQ-9 атакует точки по порядку, расходуя по одному доступному боеприпасу на точку, затем автоматически возвращается и садится.
- ЛТЦ заметно отстреливаются на конечном сближении ракеты-перехватчика либо незадолго до очереди уже наведённых пушек «Панциря». Одна кассета создаёт короткое окно противодействия; шанс отвода составляет 25% против ракет 1-го тира, 15% против 2-го и 10% против 3-го. Неудачный отстрел виден, но не предотвращает попадание.
- Получивший смертельный урон MQ-9 не исчезает: он падает с дымом и огнём, взрывается при ударе, выбрасывает уцелевшее содержимое и оставляет физический обломок корпуса.
- Возьмите многоразовый **демонтажный ключ WarTech** и нажмите **Shift+ПКМ** по лежащему обломку MQ-9, чтобы разобрать и убрать его без повторного взрыва.
- Максимальный радиус миссии составляет **2 400 блоков от записанной точки взлёта**. Каждая точка очереди должна находиться внутри этого радиуса.
- AGM-114 имеет максимальную точность, GBU-12 сохраняет небольшую погрешность управляемого боеприпаса, а неуправляемая Mk 82 обладает наибольшим разбросом. Момент её сброса рассчитывается по текущей высоте и скорости.
- ЛТЦ отстреливаются автоматически при приближении противоракеты или очереди Панциря. Одна кассета создаёт короткое окно помех; шанс отвода равен 25% для Т1, 15% для Т2 и 10% для Т3.

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

Новые РЛС, командные машины, средства РЭБ и головки наведения сохраняют владельца и команду при установке. В сетевой игре создайте команды до размещения техники:

```text
/scoreboard teams add red
/scoreboard teams join red PlayerName
```

Для противоборствующих сторон используйте разные названия команд. Технику, установленную до вступления в команду, может потребоваться подобрать и поставить заново.

### Быстрая диагностика

- **Пусковая не запускается:** проверьте совместимость ракеты, координаты целеуказателя, энергию, сигнал красного камня и допустимую дальность.
- **ПВО не реагирует:** проверьте тип перехватчика, энергию, дальность поражения, питание и состояние РЛС, а также наличие подтверждённой вражеской трассы.
- **Интерфейс РЛС пуст:** установите аккумулятор, включите/разверните РЛС и убедитесь, что реальная воздушная угроза находится в пределах дальности и потолка.
- **Не открывается интерфейс Урала:** сначала разверните его через Shift+ПКМ, затем нажмите обычный ПКМ.
- **Не открывается интерфейс турели HEMTT:** установите ровно один модуль и разверните платформу перед обычным ПКМ.
- **AGM-88 игнорирует цель:** укажите точку рядом с активным вражеским излучателем и проверьте принадлежность к scoreboard-команде.
- **«Герань-2» не запускается:** нужны предмет «Герань-2», настроенный целеуказатель, достаточная энергия, цель на расстоянии 20-1 000 блоков и сигнал красного камня.

### Спутник кинетической бомбардировки «ОДИН»

1. Запустите **спутник кинетической бомбардировки «ОДИН»** через штатную систему запуска спутников HBM.
2. Откройте его штатный координатный интерфейс HBM и выберите координаты удара.
3. Подтвердите координатное действие. Спутник сбросит один вольфрамовый стержень над выбранной точкой.

Один спутник несёт четыре стержня; между сбросами действует интервал 60 секунд. Падающий стержень отображается на РЛС как баллистическая цель Tier 3, самостоятельно прогружает чанки полёта и может быть перехвачен во время предупреждения и терминального снижения. Удар представляет собой мощный неядерный кинетический взрыв без радиации и ядерного гриба.
