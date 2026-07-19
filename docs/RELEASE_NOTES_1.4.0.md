# WarTech Reforged 1.4.0

- Minecraft: `1.7.10`
- Forge: `10.13.4.1614`
- Required HBM NTM: `1.0.27 X5751`
- Release file: `WarTech-Reforged-1.4.0-hbm5751.jar`

WarTech Reforged `1.4.0` is the air-defense, reusable aviation, and orbital-strike update. It adds two complete mobile SHORAD systems, turns the MQ-9 into a persistent multi-target combat aircraft, introduces ODIN kinetic bombardment, and fixes remote flight, energy, tracking, rendering, and vehicle behavior across the mod.

## English

### Added

- **9K331 Tor-M1** driveable mobile air-defense system with eight WTI-2 Lance cells, vertical launch, integrated X-band radar, battery slot, fire modes, and a 220-block engagement range.
- **96K6 Pantsir-S2** driveable point-defense system with twelve WTI-1 Falcon cells, integrated radar, and twin 30 mm close-in cannons.
- Loadable Pantsir 30 mm ammunition belts. Cannon bursts consume real ammunition and energy and use visible tracers, sound, smooth aiming, and imperfect hit probability.
- **MQ-9 Reaper** reusable strike UAV with six hardpoints, a 2,400-block mission radius, up to six queued targets, randomized routing, automatic return, and smooth landing.
- MQ-9 payloads: precise powered **AGM-114 Hellfire**, guided **GBU-12 Paveway II**, and less accurate unguided **Mk 82**.
- Loadable MQ-9 flare countermeasures with 25% / 15% / 10% decoy probability against tier 1 / 2 / 3 interceptors. Active flares can also disturb Pantsir gun fire.
- Persistent MQ-9 crash sequence, inventory recovery, collidable wreck, and reusable **WarTech Salvage Wrench** for wreck removal.
- **ODIN Kinetic Bombardment Satellite**, launched through HBM's Soyuz infrastructure and controlled through the satellite map, coordinate terminal, or laser designator.
- Four interceptable tungsten rods per ODIN satellite with a 60-second release interval, Tier-3 radar signature, chunk loading, and a large non-nuclear kinetic impact.

### Changed

- All WarTech missiles, drones, anti-radiation weapons, MQ-9 flights, and ODIN rods keep the chunks required by their active flight loaded. Remote redstone/detonator launches no longer require the player to stand nearby or place a separate chunk loader.
- Tier-3 ballistic contacts now require about 2.5 seconds of track establishment and at least 18 blocks of altitude above local terrain before long-range radar distributes a stable track.
- Mobile air-defense interfaces explicitly identify compatible interceptor ammunition and show radar, fire mode, energy, health, contacts, and weapon state.
- Tor-M1 and Pantsir-S2 received corrected driving direction, driver camera placement, acceleration, steering, obstacle climbing, deployment, and terrain handling.
- Heavy missile hits can destroy air-defense vehicles instead of leaving them effectively immortal.
- MQ-9 ground state is no longer reported as an airborne threat. Weapons are attached at corrected scale and position; dropped bombs remain visible during flight.
- MQ-9 guided and unguided weapon accuracy is separated clearly. Target queue controls can remove the last point or clear all points.
- Radar, command, air-defense, and MQ-9 systems now draw real stored HBM energy while operating.
- An interceptor decoyed by MQ-9 flares causes a short launcher reaction cooldown instead of an immediate three-missile salvo.
- Creative inventory icons and imported item renderers were resized and corrected for legibility.

### Fixed

- Fixed crashes when loading missiles into Pantsir-S2 and several remote-launch paths.
- Fixed Tor-M1 placement orientation and the reversed/embedded driving camera on both new systems.
- Fixed Pantsir cannon tracking being instantaneous, silent, invisible, and effectively guaranteed against tier-1 targets.
- Fixed Geran-2 and other remote weapons failing after leaving player-loaded chunks.
- Fixed MQ-9 vertical ground penetration during landing, disappearing bombs, broken payload textures, and inconsistent hardpoint placement.
- Fixed MQ-9 countermeasures not being visibly released before a missile or cannon engagement.
- Fixed ODIN frequency assignment, Satellite Interface `INFO: NO SERVICE`, map targeting, post-strike cooldown state, and missing status messages after the first release.
- Fixed immediate same-tick Tier-3 interceptor launches at the launch site.

### Upgrade instructions

1. Back up the world.
2. Stop Minecraft or the server.
3. Remove the original WarTech JAR and every older WarTech Reforged JAR from `mods`.
4. Install only `WarTech-Reforged-1.4.0-hbm5751.jar` together with HBM NTM `1.0.27 X5751`.
5. Multiplayer servers and all clients must use the same files.

This remains a replacement for WarTech 1.1.1, not an addon to it. Existing worlds should be backed up before upgrading. Survival recipes and progression remain less tested than Creative-mode systems.

## Русский

WarTech Reforged `1.4.0` — крупное обновление мобильной ПВО, многоразовой ударной авиации и орбитального оружия. Оно добавляет два полноценных самоходных комплекса, превращает MQ-9 в многоцелевой возвращаемый БПЛА, вводит кинетический спутник «ОДИН» и исправляет прогрузку чанков, энергию, сопровождение целей, модели и движение техники.

### Добавлено

- Управляемый комплекс **9К331 «Тор-М1»** с восемью ячейками WTI-2 «Копьё», вертикальным пуском, встроенной РЛС X-диапазона, аккумулятором, режимами огня и дальностью перехвата 220 блоков.
- Управляемый комплекс **96К6 «Панцирь-С2»** с двенадцатью WTI-1 «Сокол», встроенной РЛС и спаренными 30-мм пушками ближнего рубежа.
- Загружаемые ленты 30-мм боеприпасов. Очереди пушек реально расходуют боекомплект и энергию, имеют трассеры, звук, плавное наведение и ненулевой шанс промаха.
- Многоразовый ударный БПЛА **MQ-9 Reaper** с шестью узлами подвески, радиусом миссии 2 400 блоков, очередью до шести целей, вариативными маршрутами, автоматическим возвращением и плавной посадкой.
- Вооружение MQ-9: точная управляемая **AGM-114 Hellfire**, управляемая **GBU-12 Paveway II** и менее точная неуправляемая **Mk 82**.
- Загружаемые ЛТЦ MQ-9 с шансом отвода 25% / 15% / 10% против перехватчиков Tier 1 / 2 / 3. Активное облако ЛТЦ также мешает пушкам «Панциря».
- Полноценное падение уничтоженного MQ-9, возврат уцелевшего содержимого, физический обломок и многоразовый **демонтажный ключ WarTech**.
- Спутник кинетической бомбардировки **«ОДИН»**, запускаемый через Soyuz HBM и управляемый с карты, координатного терминала или лазерного целеуказателя.
- Четыре перехватываемых вольфрамовых стержня на спутник, интервал 60 секунд, сигнатура Tier 3, прогрузка чанков и мощный неядерный кинетический удар.

### Изменено

- Все ракеты, дроны, противорадиолокационное оружие, MQ-9 и стержни «ОДИНА» самостоятельно удерживают нужные для активного полёта чанки. Дистанционный пуск красным камнем или детонатором больше не требует игрока рядом либо отдельного прогрузчика.
- Устойчивая трасса баллистической цели Tier 3 появляется примерно через 2,5 секунды и только после подъёма минимум на 18 блоков над локальным рельефом.
- Интерфейсы мобильной ПВО прямо указывают совместимую ракету и показывают РЛС, режим огня, энергию, прочность, цели и состояние оружия.
- Исправлены направление движения, положение водителя, разгон, руление, подъём на блоки, развёртывание и работа на рельефе у «Тора» и «Панциря».
- Серьёзное попадание ракеты теперь может уничтожить машину ПВО.
- Стоящий на земле MQ-9 больше не считается воздушной угрозой. Масштаб и крепление вооружения исправлены, сброшенные бомбы видны в полёте.
- Точность управляемых и неуправляемых боеприпасов MQ-9 разделена; добавлены кнопки удаления последней и всех целей.
- РЛС, командные машины, комплексы ПВО и MQ-9 реально расходуют энергию HBM.
- После отвода ракеты ЛТЦ пусковая выдерживает короткую паузу, а не выпускает сразу несколько перехватчиков.
- Исправлен размер и отображение новых иконок и импортированных предметов в инвентаре.

### Исправлено

- Устранён краш при загрузке ракеты в «Панцирь-С2» и в нескольких сценариях удалённого запуска.
- Исправлена ориентация «Тора» при установке и перевёрнутое управление/камера внутри корпуса обеих машин.
- Пушки «Панциря» больше не наводятся мгновенно, не стреляют бесшумно и невидимо и не гарантируют уничтожение любой цели Tier 1.
- «Герань-2» и другие удалённые боеприпасы больше не останавливаются после выхода из чанков игрока.
- Исправлены вертикальное зарывание MQ-9 при посадке, исчезающие бомбы, текстуры вооружения и крепление на пилонах.
- ЛТЦ теперь видимо отстреливаются перед возможным попаданием ракеты или пушечной очереди.
- Исправлены выдача частоты «ОДИНУ», `INFO: NO SERVICE`, управление с карты, состояние после первого удара и пропавшие уведомления о боезапасе и перезарядке.
- Убраны мгновенные пуски противоракет по Tier 3 прямо в момент старта.

### Обновление

1. Сделайте резервную копию мира.
2. Полностью остановите игру или сервер.
3. Удалите оригинальный WarTech и все старые JAR WarTech Reforged из `mods`.
4. Установите только `WarTech-Reforged-1.4.0-hbm5751.jar` вместе с HBM NTM `1.0.27 X5751`.
5. На сервере и всех клиентах должны стоять одинаковые файлы.

Это замена WarTech 1.1.1, а не аддон к нему. Перед обновлением существующего мира обязательна резервная копия. Рецепты и выживание по-прежнему протестированы слабее, чем системы творческого режима.
