# WarTech Reforged 1.5.0

- Minecraft: `1.7.10`
- Forge: `10.13.4.1614`
- Required HBM: NTM `1.0.27 X5751` or NTM Space `X5758 H261`
- Release file: `WarTech-Reforged-1.5.0-universal-hbm.jar`

WarTech Reforged `1.5.0` is the aviation, networked air-defense, and universal-HBM update. It adds strategic and tactical combat aviation, radar-cued fighter interception, persistent IFF teams, long-range communications, new heavy aviation weapons, and one JAR for both supported HBM branches.

## English

### Added

- **Tu-95 strategic bomber** with six hardpoints, batteries, countermeasures, multi-point missions, long-range standoff release, automatic return, landing, crash behavior, and salvage-wrench recovery.
- **Kh-555** Tier-2 long-range cruise missile. It can be launched by the Tu-95 or a standard WarTech cruise launcher and uses separated salvo routes.
- Tu-95-exclusive **FAB-5000** unguided heavy bomb and **KAB-3000** guided heavy bomb. FAB delivers the larger concentrated blast; KAB trades some yield for range and terminal guidance.
- **F-16C** tactical aircraft with four hardpoints and a 3,000-block strike radius.
- **Su-27** tactical aircraft with six hardpoints, greater durability, and a 3,400-block strike radius.
- Unified aviation ammunition shared where appropriate with MQ-9: AGM-114, HJ-10, GBU-12, Mk 82, and JDAM, plus fighter-only AGM-65 Maverick, Kh-29, and KAB-500L.
- **WT-AAM Skyguard** and autonomous fighter-interceptor mode. Parked fighters can receive hostile tracks from the radar network, scramble, reserve a target, fire one AAM, and return.
- Persistent scoreboard-team **IFF** with an in-game team selector. Friendly missiles, aircraft, and drones can cross their own air-defense network without being engaged.
- Powered **long-range communication mast** with a 2,400-block link radius per hop, a status interface, battery storage, local chunk loading, and relay support for tracks, sirens, command links, and fighter cueing.
- Automatic HBM siren integration through the **Air-Raid Siren Relay**.
- Three organized WarTech creative tabs for air defense, aviation, and support equipment.

### Changed

- One universal compatibility layer now supports both HBM NTM `X5751` and HBM NTM Space `X5758 H261`.
- Guided aviation weapons continuously steer in flight. Missiles are faster and more precise than glide or unguided bombs while remaining valid Tier-1 air-defense targets.
- F-16C, Su-27, Tu-95, MQ-9, Geran-2, cruise missiles, ballistic missiles, and active guided payloads maintain the chunks required for remote missions.
- Tactical aircraft now use ground roll, progressive takeoff, stable cruise attitude, approach, flare, and rollout instead of instant vertical transitions.
- Tu-95 releases heavy weapons without hovering or snapping in place. Kh-555 salvos and group missile launches use smooth route separation without visible zigzagging.
- Communication relays propagate warnings and clear them after the final hostile track expires, including remote siren sectors.
- Radar and command vehicles have reinforced blast resistance: nearby shock waves no longer destroy them trivially, while direct serious hits remain lethal.
- ODIN kinetic impact yield was increased while remaining non-nuclear.
- The salvage wrench can remove aircraft, wrecks, launchers, radars, command vehicles, communication equipment, and other WarTech deployables.

### Fixed

- Fixed remote missiles freezing in the air after crossing unloaded chunks.
- Fixed Geran-2 failing to leave its catapult remotely and grounded aircraft remaining marked as airborne threats.
- Fixed team ownership being lost after reconnecting or reopening a world.
- Fixed sirens sometimes continuing after a remote network threat disappeared.
- Fixed crashes and API linkage failures between classic HBM and HBM Space.
- Fixed Long Range Hypersonic Weapon launch crashes, stalled ballistic missiles, HARM flying backwards, and excessive cruise-missile zigzagging.
- Fixed gas micro-missile payloads producing no effect and reduced neutron micro-missile impact stalls.
- Fixed strategic and tactical weapon orientation, attachment, release accuracy, terminal guidance, and close-range airburst behavior.
- Fixed S-400 radar and Ural command vehicle damage handling.
- Fixed communication-mast removal crashes and several VLS, launcher, radar, GUI, and Creative inventory regressions.

### Upgrade

1. Back up the world.
2. Stop Minecraft or the server.
3. Remove the original WarTech JAR and every older WarTech Reforged JAR from `mods`.
4. Install exactly one supported HBM build and `WarTech-Reforged-1.5.0-universal-hbm.jar`.
5. Use the same HBM and WarTech Reforged files on the server and all clients.

WarTech Reforged replaces WarTech 1.1.1; it is not an addon that should be installed beside the original JAR.

## Русский

WarTech Reforged `1.5.0` — обновление авиации, сетевой ПВО и универсальной совместимости с HBM. Оно добавляет стратегическую и оперативно-тактическую авиацию, автоматический вылет истребителей по данным РЛС, постоянную систему «свой-чужой», дальнюю связь, новые тяжёлые авиабоеприпасы и единый JAR для двух поддерживаемых веток HBM.

### Добавлено

- **Стратегический ракетоносец Ту-95** с шестью узлами подвески, аккумулятором, ЛТЦ, очередью целей, дальним пуском, автоматическим возвращением, посадкой, аварией и демонтажем ключом.
- Крылатая ракета большой дальности **Х-555** класса Tier 2. Она запускается с Ту-95 и из стандартной пусковой WarTech; ракеты залпа расходятся по отдельным маршрутам.
- Уникальные для Ту-95 **ФАБ-5000** и управляемая **КАБ-3000**. ФАБ имеет более мощный сосредоточенный взрыв, КАБ — меньшую мощность, но большую дальность и точное терминальное наведение.
- Тактический **F-16C** с четырьмя узлами подвески и радиусом ударной миссии 3 000 блоков.
- Тактический **Су-27** с шестью узлами подвески, большей прочностью и радиусом 3 400 блоков.
- Унифицированные с MQ-9 боеприпасы: AGM-114, HJ-10, GBU-12, Mk 82 и JDAM, а также доступные только самолётам AGM-65 Maverick, Х-29 и КАБ-500Л.
- Ракета **WT-AAM Skyguard** и автоматический режим истребителя-перехватчика. Самолёт получает вражескую трассу от сети РЛС, взлетает, резервирует цель, выпускает одну ракету и возвращается.
- Постоянная система **«свой-чужой»** на scoreboard-командах с удобным игровым выбором команды. Свои самолёты, БПЛА и ракеты свободно проходят над своей ПВО.
- Энергозависимая **мачта дальней связи** с радиусом 2 400 блоков на один узел, интерфейсом, аккумулятором, локальной прогрузкой чанков и ретрансляцией трасс, тревоги, команд и вызова истребителей.
- Автоматическое включение сирен HBM через **реле воздушной тревоги**.
- Три упорядоченные вкладки WarTech: ПВО, авиация и вспомогательные системы.

### Изменено

- Один универсальный слой совместимости поддерживает HBM NTM `X5751` и HBM NTM Space `X5758 H261`.
- Управляемые авиабоеприпасы постоянно корректируют курс. Ракеты быстрее и точнее планирующих и неуправляемых бомб, но остаются целями для ракетной ПВО Tier 1.
- F-16C, Су-27, Ту-95, MQ-9, «Герань-2», крылатые и баллистические ракеты и активные управляемые боеприпасы самостоятельно удерживают нужные чанки.
- Тактические самолёты получили разбег, плавный взлёт, устойчивый полёт, заход, выравнивание и пробег вместо мгновенных вертикальных переходов.
- Ту-95 сбрасывает тяжёлые боеприпасы без зависания и рывков. Залпы Х-555 и групповые пуски расходятся плавно, без заметных зигзагов.
- Дальняя сеть правильно передаёт и снимает тревогу после исчезновения последней вражеской трассы, включая удалённые сирены.
- РЛС и командные машины усилены против далёкой ударной волны, но серьёзное прямое попадание всё ещё способно их уничтожить.
- Усилено неядерное кинетическое поражение спутника «ОДИН».
- Демонтажным ключом снимаются самолёты, обломки, пусковые, РЛС, командные машины, средства связи и другая размещаемая техника WarTech.

### Исправлено

- Ракеты больше не зависают после выхода из прогруженных игроком чанков.
- Исправлены удалённый старт «Герани-2» и ошибочное отображение стоящих самолётов как воздушных угроз.
- Командная принадлежность больше не сбрасывается после перезахода в мир.
- Удалённые сирены выключаются после исчезновения угрозы.
- Исправлены краши и несовместимые вызовы API между обычным HBM и HBM Space.
- Исправлены краш Long Range Hypersonic Weapon, зависание баллистических ракет, полёт HARM задом наперёд и заметные зигзаги крылатых ракет.
- Газовая микроракета снова создаёт эффект; попадание нейтронной микроракеты вызывает значительно меньше зависаний.
- Исправлены ориентация, крепление, точность, терминальное наведение и близкие воздушные подрывы авиационного вооружения.
- Исправлена обработка урона дальней РЛС С-400 и командного Урала.
- Исправлены краш при демонтаже мачты и ряд регрессий VLS, пусковых, РЛС, интерфейсов и творческого меню.

### Обновление

1. Сделайте резервную копию мира.
2. Полностью остановите игру или сервер.
3. Удалите оригинальный WarTech и все старые сборки WarTech Reforged из `mods`.
4. Установите одну поддерживаемую версию HBM и `WarTech-Reforged-1.5.0-universal-hbm.jar`.
5. На сервере и всех клиентах должны совпадать версии HBM и WarTech Reforged.

WarTech Reforged заменяет WarTech 1.1.1 и не должен устанавливаться одновременно с оригинальным JAR.
