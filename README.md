# WarTech Reforged

DISCORD: https://discord.gg/jZAfCe8Z2w

Documentation: [Release notes 1.5.0](docs/RELEASE_NOTES_1.5.0.md) | [Controls guide](docs/CONTROLS.md) | [Electronic warfare](docs/ELECTRONIC_WARFARE.md)

WarTech Reforged is an unofficial restoration and expansion of the abandoned
WarTech 1.1.1 addon for Minecraft 1.7.10 and HBM's Nuclear Tech Mod.

The project preserves the original WarTech content, repairs its broken systems
for HBM NTM `1.0.27 X5751` and HBM NTM Space `X5758 H261`, and develops it into a larger missile-warfare mod
with layered air defense, mobile radar and command vehicles, attack drones,
new cruise missiles, and mobile artillery platforms.

## Main Features

### Restored WarTech systems

- Original WarTech items and blocks restored to the Creative inventory.
- Repaired cruise- and ballistic-missile launchers, inventories, energy storage,
  interfaces, launch logic, rendering, and impact behavior.
- Explosions occur at the impact point instead of being duplicated at launch.
- Support for the original subsonic, supersonic, hypersonic, ballistic, and
  specialized-warhead content inherited from WarTech.

### New missiles and drones

- **Storm Shadow** cruise missile, implemented as a new long-range guided weapon.
- **Geran-2** attack drone with a dedicated rail catapult.
- Reusable **MQ-9 Reaper** strike UAV with six persistent hardpoints, an HBM
  battery slot, a six-point strike queue, a 2,400-block mission radius,
  randomized routing between targets, and automatic return and landing at its
  recorded launch point.
- MQ-9 payloads include the powered **AGM-114 Hellfire**, guided **GBU-12
  Paveway II**, unguided **Mk 82**, light **HJ-10**, and long-release **JDAM**.
  The UAV calculates the Mk 82 release
  point from its actual altitude and velocity before following its return
  route. AGM-114 uses exact terminal guidance, GBU-12 has a small guided
  dispersion, and Mk 82 retains a wider unguided accuracy envelope.
- Medium-range tactical aviation adds the fast four-hardpoint **F-16C** and the
  heavier six-hardpoint **Su-27**. Both use the same unified ammunition items
  as the MQ-9 while also accepting fighter-only AGM-65, Kh-29, and KAB-500L
  weapons. Missile, bomb, and glide-bomb release ranges remain identical on
  every compatible carrier.
- F-16C and Su-27 can switch to an autonomous radar-cued interceptor role.
  With WT-AAM Skyguard missiles loaded, they scramble from a parked state,
  pursue one reserved hostile contact, fire one missile, and return to base.
- Loadable MQ-9 countermeasure flares can decoy tier-1, tier-2, and tier-3
  interceptors with 25%, 15%, and 10% probability and can disrupt Pantsir gun
  bursts while the flare cloud is active. Flares are visibly deployed during
  the final threat approach, before the interceptor or cannon burst reaches
  the aircraft.
- A destroyed MQ-9 no longer vanishes in mid-air. It enters a smoking descent,
  impacts with fire and an explosion, drops its surviving inventory, and leaves
  a persistent, collidable airframe wreck at the crash site.
- The reusable WarTech salvage wrench removes landed MQ-9 wrecks with
  Shift + right-click, without causing another explosion or duplicating drops.
- New powered vehicles and radar systems spawn empty and draw real HBM HE at
  their operating rate. Installed batteries are drained according to their
  own discharge-rate limit instead of receiving an artificial transfer floor.
- Geran-2 dynamically calculates its climb, cruise, and terminal descent so it
  can clear terrain and approach the selected target.
- Geran-2 range is up to 1,000 blocks. It flies substantially slower than a
  tier-1 cruise missile, deals approximately half of tier-1 missile damage, and
  has a 30% chance to ignite the impact area.
- Group launches use route separation and randomized course corrections, so
  multiple weapons do not follow one perfectly identical and predictable path.

### Layered air defense

- Driveable and deployable **9K331 Tor-M1** and **96K6 Pantsir-S2** mobile
  air-defense systems with integrated radar, batteries, fire modes, and fully
  functional missile inventories.
- Tor-M1 carries eight WTI-2 Lance interceptors. Pantsir-S2 carries twelve
  WTI-1 Falcon interceptors plus a loadable twin 30 mm close-in cannon system
  with visible tracers, sound, finite ammunition, energy use, and imperfect
  hit probability.
- Functional **MIM-104 Patriot** and **S-400** launcher objects with interceptor
  storage, energy use, automatic target selection, and launch effects.
- Three interceptor classes: **WTI-1 Falcon** (100 blocks), **WTI-2 Lance**
  (250 blocks), and **WTI-3 Sentinel** (400 blocks).
- Tier-specific speed, guidance, interception difficulty, and failure behavior.
- Support for intercepting cruise missiles, attack drones, and ballistic targets.
- Target reservation prevents every launcher from wasting missiles on the same
  contact. If the first interceptor fails, other systems react after roughly
  five to seven seconds.
- Failed interceptors leave in the target direction and can deviate, crash,
  explode, and ignite terrain instead of falling vertically onto their launcher.
- Tier-3 ballistic contacts require a short track-establishment delay and a
  sensible altitude above terrain before long-range air defense can react.

### Electronic warfare

- **Synytsia** electronic jammer with L, S, X, and wideband modes and a
  350-block effective radius.
- Jamming degrades radar track quality and detection probability and can create
  false contacts instead of simply switching radars off.
- Passive ESM array detects active radar, jammer, and decoy emissions up to
  900 blocks without emitting a radar signal of its own.
- Deployable radar decoy provides a false target for passive sensors and
  anti-radiation seekers.
- **AGM-88 HARM** searches for emitters within 1,200 blocks of the designated
  point, supports home-on-jam, and remembers the last emitter position after a
  radar shutdown with gradually increasing accuracy error. It follows a lofted
  attack profile and destroys radar, EW, or active command vehicles on a close hit.
- AGM-88 salvo launches receive separate lateral, wave, and loft profiles while
  preserving terminal guidance to the selected emitter.
- New radar, command, and EW equipment stores scoreboard-team ownership for
  basic IFF and friendly-jamming protection.

### Radar and command network

- **Renault TRM mobile radar**: detection range up to 600 blocks and altitude up
  to 500 blocks.
- **S-400 long-range radar**: detection range up to 1,200 blocks, altitude up to
  900 blocks, and support for up to 32 tracked contacts.
- Both radar vehicles have an HBM-style control interface with live power,
  contact, range, ceiling, and operating-state readouts plus an HBM battery slot.
- Confirmed airborne contacts are plotted as synchronized moving blips on the
  radar display; anti-radiation launches are registered immediately.
- Right click a radar with an empty hand to open its interface. Shift-right-click
  toggles the TRM radar or deploys/retracts the S-400 radar.
- **Ural air-defense command post** links radars and launchers into one network.
- The Ural command interface contains a persistent HBM battery slot and live
  network/power telemetry.
- The command interface displays connected radars, detected contacts, available
  launchers, and active interceptions.
- Detection is based on actual outgoing launches and tracked flying threats,
  rather than a simple periodic scan around each launcher.
- An **Air-Raid Siren Relay** converts same-team Ural warning contacts into a
  standard redstone signal for HBM sirens.
- Powered **long-range communication masts** form same-team relay chains with a
  2,400-block range per hop. They distribute radar tracks, command links,
  siren alarms, and fighter cueing between distant sectors while keeping a
  local 3x3-chunk operating area loaded.
- Interceptor patrol range is 6,500 blocks for F-16C and 8,000 blocks for
  Su-27; their ordinary ground-strike ranges are unchanged.
- **ODIN** orbital kinetic bombardment integrates with HBM satellite launchers,
  map, laser, and coordinate controllers. Each satellite carries four rods with
  a 60-second release interval; descending rods are interceptable Tier-3 tracks.

### Mobile artillery

- Driveable **HEMTT heavy artillery platform**.
- Supports the HBM **Greg** artillery turret and **Henry** rocket turret as
  mutually exclusive weapon modules.
- Travel and deployed modes, stabilizing supports, vehicle collision, and
  reinforced vehicle durability.
- The driveable Ural and HEMTT share smoother heavy-vehicle acceleration,
  braking, speed-dependent steering, obstacle climbing, suspension pitch, and
  corrected driver positions inside their cabs.
- Turret inventory, ammunition, batteries, operating modes, and HBM artillery
  designator integration are available from the mounted weapon interface.

### Presentation and performance

- Detailed imported 3D models for launchers, radar vehicles, missiles, drones,
  and the mobile artillery platform.
- Optimized model rendering and display-list reuse to reduce nearby frame loss.
- Launch smoke, interception sound, explosions, fire effects, and corrected
  missile orientation and flight rendering.

## Requirements

- Minecraft `1.7.10`
- Minecraft Forge `10.13.4.1614`
- HBM's Nuclear Tech Mod `1.0.27 X5751` **or** HBM NTM Space `X5758 H261`
- WarTech Reforged `1.5.0-universal-hbm`

Both the server and every connecting client must use matching mod versions.
WarTech Reforged is a replacement for WarTech 1.1.1: do not install the original
WarTech JAR alongside it.

## Installation

1. Install Minecraft Forge 1.7.10 (`10.13.4.1614`).
2. Install either HBM's Nuclear Tech Mod `1.0.27 X5751` or HBM NTM Space `X5758 H261`.
3. Remove the original WarTech JAR and all older WarTech Reforged builds from
   the `mods` folder.
4. Place `WarTech-Reforged-1.5.0-universal-hbm.jar` in the `mods` folder.
5. For multiplayer, install the same files on the server and every client.

## Current Limitations

- The project has primarily been tested in Creative mode. Survival recipes and
  progression may be incomplete.
- The universal release is tested against HBM NTM X5751 and HBM NTM Space X5758 H261. Other forks or revisions are not guaranteed.
- This is a beta-quality restoration of an old 1.7.10 addon; keep world backups
  and report issues with a full crash report and Forge log.

## Free and Noncommercial Distribution

WarTech Reforged is intended to remain freely available to everyone. A donation
is never required to download or use the mod and does not provide early access,
exclusive builds, gameplay advantages, voting rights, support priority, or any
other mod-related benefit. Platform reward programs are disabled while the mod
contains assets licensed for noncommercial use.

Voluntary donations may support the maintainer's general development time, but
they are not payment for WarTech Reforged or its third-party assets.

## Credits and Legal Notice

WarTech Reforged is based on WarTech 1.1.1 and is not an official continuation.
It is not affiliated with or endorsed by the original WarTech team, the HBM NTM
team, real-world equipment manufacturers, or any military organization.

HBM's Nuclear Tech Mod is a required external dependency and is not bundled.
Imported models remain subject to their respective licenses and attribution
requirements. Full attribution and unresolved release requirements are listed in
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

---

# WarTech Reforged на русском

Документация: [Описание версии 1.5.0](docs/RELEASE_NOTES_1.5.0.md) | [Полное управление](docs/CONTROLS.md) | [Радиоэлектронная борьба](docs/ELECTRONIC_WARFARE.md)

DISCORD: https://discord.gg/jZAfCe8Z2w

WarTech Reforged - неофициальное восстановление и масштабное развитие
заброшенного аддона WarTech 1.1.1 для Minecraft 1.7.10 и HBM's Nuclear Tech Mod.

Проект сохраняет содержимое оригинального WarTech, исправляет его работу с HBM
NTM `1.0.27 X5751` и HBM NTM Space `X5758 H261` и превращает его в более крупный мод о ракетном вооружении,
эшелонированной ПВО, мобильных радарах, командных машинах, ударных БПЛА и
подвижной артиллерии.

## Основные возможности

### Восстановленный WarTech

- Предметы и блоки оригинального WarTech снова отображаются в творческом меню.
- Исправлены пусковые установки крылатых и баллистических ракет, интерфейсы,
  инвентари, питание, запуск, отрисовка и обработка попаданий.
- Взрыв происходит только в месте попадания, без дублирующего взрыва на старте.
- Сохранены исходные дозвуковые, сверхзвуковые, гиперзвуковые и баллистические
  ракеты, а также специализированные боевые части WarTech.

### Новые ракеты и беспилотники

- Добавлена крылатая ракета **Storm Shadow**.
- Добавлен ударный БПЛА **Герань-2** с отдельной рельсовой катапультой.
- Многоразовый ударный БПЛА **MQ-9 Reaper** с шестью узлами подвески,
  аккумулятором HBM, очередью до шести целей, радиусом миссии 2 400 блоков,
  вариативными маршрутами, автоматическим возвращением и посадкой.
- Вооружение MQ-9 включает управляемые **AGM-114 Hellfire** и **GBU-12
  Paveway II**, а также неуправляемую **Mk 82** с большей погрешностью.
- Загружаемые ЛТЦ MQ-9 имеют шанс 25%, 15% и 10% отвлечь перехватчик Tier 1,
  Tier 2 и Tier 3 соответственно и могут мешать пушкам «Панциря».
- Уничтоженный MQ-9 падает с дымом и огнём, оставляет уцелевший инвентарь и
  физический обломок, который убирается демонтажным ключом WarTech.
- Герань динамически рассчитывает набор высоты, маршевый участок и снижение к
  цели, учитывая оставшееся расстояние.
- Дальность Герани - до 1 000 блоков. Она заметно медленнее крылатой ракеты
  первого тира, наносит около 50% ее урона и с вероятностью 30% вызывает пожар.
- При групповом запуске ракеты и БПЛА разводят маршруты и случайно корректируют
  курс, поэтому не летят одной идеально совпадающей и предсказуемой колонной.

### Эшелонированная система ПВО

- Управляемые и разворачиваемые комплексы **9К331 «Тор-М1»** и
  **96К6 «Панцирь-С2»** со встроенными РЛС, аккумуляторами, режимами огня и
  полноценными ракетными ячейками.
- «Тор-М1» несёт восемь WTI-2 «Копьё». «Панцирь-С2» несёт двенадцать WTI-1
  «Сокол» и спаренные 30-мм пушки ближнего рубежа с видимыми трассерами,
  звуком, расходом боекомплекта и энергии, а также ненулевым шансом промаха.
- Полноценные установки **MIM-104 Patriot** и **С-400** с боекомплектом,
  энергопотреблением, автоматическим выбором цели и эффектами запуска.
- Три класса противоракет: **WTI-1 «Сокол»** (100 блоков),
  **WTI-2 «Копье»** (250 блоков) и **WTI-3 «Страж»** (400 блоков).
- Скорость, наведение, сложность перехвата и вероятность отказа зависят от тира.
- Система работает против крылатых ракет, ударных БПЛА и баллистических целей.
- Резервирование целей не позволяет всем установкам одновременно тратить
  боекомплект на одну угрозу. Если первая противоракета не справилась, другие
  комплексы реагируют примерно через 5-7 секунд.
- При неудаче противоракета продолжает полет в сторону угрозы, уходит с курса и
  может упасть со взрывом и возгоранием, а не возвращается на свою установку.

### Радиолокационная и командная сеть

- **Мобильная РЛС Renault TRM**: дальность обнаружения до 600 блоков, высота до
  500 блоков.
- **Дальняя РЛС С-400**: дальность до 1 200 блоков, высота до 900 блоков и до
  32 сопровождаемых контактов.
- **Командный пункт ПВО на базе Ural** объединяет РЛС и пусковые установки.
- В интерфейсе отображаются подключенные радары, обнаруженные цели, готовые
  пусковые установки и уже выполняемые перехваты.
- Система отслеживает реальные исходящие запуски и летящие угрозы, а не просто
  периодически ищет сущности вокруг каждой установки.
- Спутник кинетической бомбардировки **«ОДИН»** запускается штатной системой
  Soyuz HBM и принимает цели с карты, координатного терминала или лазерного
  целеуказателя. На борту четыре перехватываемых стержня с интервалом 60 секунд.
- Баллистическая цель Tier 3 получает устойчивую трассу после короткой задержки
  и подъёма над рельефом, поэтому ПВО не запускается в тот же тик, что и цель.

### Мобильная артиллерия

- Управляемая тяжелая платформа **HEMTT**.
- На машину устанавливается один из двух несовместимых одновременно модулей:
  артиллерийская турель HBM **Greg** или ракетная турель **Henry**.
- Есть походный и боевой режимы, выдвижные опоры, коллизия и повышенная прочность.
- Через интерфейс установленной турели доступны боеприпасы, аккумуляторы,
  режимы работы и взаимодействие с артиллерийским целеуказателем HBM.

### Модели, эффекты и производительность

- Детальные 3D-модели пусковых установок, радарных машин, ракет, БПЛА и
  артиллерийской платформы.
- Оптимизированная отрисовка и повторное использование display list для снижения
  просадок FPS рядом с крупными моделями.
- Дым при запуске, звук и взрыв перехвата, возгорание, а также исправленная
  ориентация и отрисовка ракет в полете.

## Требования

- Minecraft `1.7.10`
- Minecraft Forge `10.13.4.1614`
- HBM's Nuclear Tech Mod `1.0.27 X5751` **или** HBM NTM Space `X5758 H261`
- WarTech Reforged `1.5.0-universal-hbm`

На сервере и всех клиентах должны стоять одинаковые версии. WarTech Reforged
заменяет WarTech 1.1.1: оригинальный JAR WarTech устанавливать одновременно с
Reforged нельзя.

## Установка

1. Установите Minecraft Forge 1.7.10 (`10.13.4.1614`).
2. Установите HBM's Nuclear Tech Mod `1.0.27 X5751` или HBM NTM Space `X5758 H261`.
3. Удалите из `mods` оригинальный WarTech и старые сборки WarTech Reforged.
4. Поместите `WarTech-Reforged-1.5.0-universal-hbm.jar` в папку `mods`.
5. Для мультиплеера установите одинаковый набор файлов на сервер и клиенты.

## Текущие ограничения

- Основное тестирование проводилось в творческом режиме. Рецепты и развитие в
  выживании могут быть реализованы не полностью.
- Универсальная сборка протестирована с HBM NTM X5751 и HBM NTM Space X5758 H261. Работа с другими форками и ревизиями не гарантируется.
- Это бета-версия восстановления старого аддона для 1.7.10. Делайте резервные
  копии миров, а к сообщениям об ошибках прикладывайте crash report и Forge log.

## Бесплатное некоммерческое распространение

WarTech Reforged всегда должен оставаться бесплатно доступным для всех. Донат не
требуется для скачивания или использования мода и не дает раннего доступа,
эксклюзивных сборок, игровых преимуществ, права влиять на разработку, приоритета
поддержки или других связанных с модом привилегий. Пока в проекте используются
модели с некоммерческими лицензиями, программы вознаграждений площадок отключены.

Добровольные пожертвования могут поддерживать общее время работы автора, но не
являются оплатой за WarTech Reforged или использованные сторонние материалы.

## Авторы и правовая информация

WarTech Reforged основан на WarTech 1.1.1 и не является официальным продолжением.
Проект не связан с авторами WarTech, командой HBM NTM, реальными производителями
техники или военными организациями и не одобрен ими.

HBM's Nuclear Tech Mod является внешней обязательной зависимостью и не входит в
архив WarTech Reforged. Все импортированные модели используются на условиях их
собственных лицензий. Полная атрибуция и незакрытые требования перед релизом
перечислены в [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
