# WarTech Reforged 1.3.0 - HBM X5751

Version: `1.3.0-hbm5751`

Minecraft: `1.7.10`

Forge: `10.13.4.1614`

Required dependency: HBM's Nuclear Tech Mod `1.0.27 X5751`

WarTech Reforged 1.3.0 expands the restored WarTech missile systems into a connected air-defense and electronic-warfare environment. This update adds electronic warfare units, an anti-radiation missile, dedicated radar and command interfaces, improved vehicle handling, and more reliable guided-weapon behavior.

## English

### Added

- Added the **Synytsia mobile electronic jammer** with selectable L, S, X, and wideband modes and a 350-block effective radius.
- Added a **passive ESM array** that detects active radar, jammer, and decoy emissions at up to 900 blocks without transmitting its own radar signal.
- Added a **deployable radar decoy emitter** that creates a false emitter for passive sensors and anti-radiation seekers.
- Added the **AGM-88 HARM anti-radiation missile**. It searches for emitters within 1,200 blocks of the designated point, prioritizes active jammers, supports home-on-jam, and remembers the last known position of an emitter after shutdown.
- Added independent lateral, wave, and loft profiles for AGM-88 salvo launches so missiles no longer stack into one identical flight path.
- Added dedicated HBM-style interfaces for the **Renault TRM mobile radar** and **S-400 long-range radar**, including an HBM battery slot, operating-state controls, power telemetry, range and ceiling data, contact counters, and synchronized moving radar blips.
- Added a dedicated interface for the **Ural air-defense command post**, including a persistent HBM battery slot and live network telemetry for linked radars, launchers, contacts, hostile emitters, and active interceptions.
- Added scoreboard-team ownership and basic IFF behavior for new radar, command, and EW equipment.
- Anti-radiation launches are now registered by the tracking network immediately after launch.

### Changed

- Improved AGM-88 flight with a more natural lofted profile, visible in-flight model, stable interpolation, independent salvo routing, and terminal emitter guidance.
- Active radar, jammer, decoy, and deployed command vehicles can now be destroyed by a close AGM-88 hit instead of behaving as invulnerable utility objects.
- Improved Geran-2 terminal guidance so the drone recalculates its descent and preserves enough distance to reach the designated target.
- Improved Ural and HEMTT vehicle handling with smoother acceleration, braking, speed-dependent steering, terrain stepping, suspension pitch, and corrected driver placement inside the cab.
- Corrected Ural forward/reverse input: `W` moves forward and `S` reverses.
- Radar tracking now uses actual outgoing launches and confirmed flying threats instead of relying on a simple periodic proximity scan.
- New network equipment uses owner/team information to reduce friendly tracking and friendly-jamming conflicts.

### Fixed

- Fixed a crash when placing electronic-warfare units.
- Fixed crashes caused by destroying radar and command vehicles with AGM-88 impacts.
- Fixed resource packaging regressions that could produce missing item names, broken inventory icons, invisible imported models, or white placeholder geometry.
- Fixed radar display desynchronization and missing moving contact markers.
- Fixed driver-seat position and reversed driving direction on the Ural command vehicle.
- Fixed Geran-2 descending too early and landing before the selected target.
- Improved entity interpolation to reduce visible vehicle and missile jitter in multiplayer.

### Existing systems included in this build

- Restored WarTech cruise and ballistic launch systems.
- Storm Shadow cruise missile and Geran-2 attack drone with dedicated rail catapult.
- MIM-104 Patriot and S-400 air-defense launchers.
- WTI-1 Falcon, WTI-2 Lance, and WTI-3 Sentinel interceptors with 100, 250, and 400-block engagement ranges.
- Five-to-seven-second backup-launcher reaction delay after a failed interception.
- Renault TRM and S-400 radar network with the Ural command post.
- Driveable HEMTT mobile artillery platform with mutually exclusive Greg and Henry weapon modules.

### Upgrade instructions

1. Back up the world.
2. Remove the original WarTech 1.1.1 JAR and every older WarTech Reforged JAR from `mods`.
3. Install `WarTech-Reforged-1.3.0-hbm5751.jar` on the server and every client.
4. Keep HBM NTM `1.0.27 X5751` installed.
5. Do not mix different WarTech Reforged versions between server and clients.

### Known limitations

- Compatibility is limited to HBM NTM `1.0.27 X5751`.
- The project is primarily tested in Creative mode; survival recipes and progression may be incomplete.
- Imported models can still be demanding on very old hardware when many large vehicles are placed in one area.
- This remains a restoration and expansion of an old Minecraft 1.7.10 addon. Keep world backups and attach a full crash report and Forge log when reporting a problem.

---

## Русский

### Добавлено

- Добавлена **мобильная станция помех «Синица»** с диапазонами L, S, X и широкополосным режимом. Эффективный радиус составляет 350 блоков.
- Добавлена **пассивная станция радиотехнической разведки ESM**, обнаруживающая работающие РЛС, станции помех и ложные излучатели на расстоянии до 900 блоков без собственного радиоизлучения.
- Добавлен **развёртываемый ложный радиолокационный излучатель**, создающий ложную цель для пассивных датчиков и противорадиолокационных ракет.
- Добавлена **противорадиолокационная ракета AGM-88 HARM**. Она ищет источники излучения в радиусе 1 200 блоков от указанной точки, отдаёт приоритет активным помехам, поддерживает наведение на постановщик помех и запоминает последнее положение выключенного излучателя.
- Для групповых пусков AGM-88 добавлены отдельные боковые, волновые и высотные профили, поэтому ракеты больше не летят одной наложенной траекторией.
- Добавлены отдельные интерфейсы в стиле HBM для **мобильной РЛС Renault TRM** и **дальней РЛС С-400**: слот аккумулятора HBM, управление режимом работы, данные об энергии, дальности и потолке, счётчики целей и синхронизированные движущиеся отметки.
- Добавлен отдельный интерфейс **командного пункта ПВО на базе Урала** с постоянным слотом аккумулятора HBM и телеметрией подключённых РЛС, пусковых установок, целей, вражеских излучателей и активных перехватов.
- Для новых РЛС, командных машин и средств РЭБ добавлена принадлежность к команде scoreboard и базовая система «свой-чужой».
- Пуск противорадиолокационной ракеты теперь немедленно появляется в системе сопровождения.

### Изменено

- Полёт AGM-88 стал естественнее: добавлены высотный профиль, видимая модель в полёте, стабильная интерполяция, раздельные маршруты залпа и терминальное наведение на излучатель.
- Работающие РЛС, станции помех, ложные излучатели и развёрнутые командные машины теперь уничтожаются близким попаданием AGM-88 и больше не являются бессмертными служебными объектами.
- Улучшено терминальное наведение «Герани-2»: БПЛА динамически пересчитывает снижение и сохраняет достаточную дистанцию для достижения указанной цели.
- Улучшено управление Уралом и HEMTT: плавнее разгон и торможение, поворот зависит от скорости, добавлены преодоление перепадов рельефа и наклон подвески, водитель находится внутри кабины.
- Исправлено направление управления Уралом: `W` ведёт машину вперёд, `S` включает задний ход.
- Система РЛС теперь отслеживает реальные исходящие пуски и подтверждённые летящие угрозы, а не использует упрощённое периодическое сканирование вокруг пусковой.
- Новое сетевое оборудование учитывает владельца и команду, уменьшая количество конфликтов дружественного сопровождения и помех.

### Исправлено

- Исправлен краш при размещении средств радиоэлектронной борьбы.
- Исправлены краши при уничтожении РЛС и командных машин ракетой AGM-88.
- Исправлены ошибки упаковки ресурсов, из-за которых могли пропадать названия предметов, ломаться иконки, исчезать импортированные модели или появляться белая геометрия-заглушка.
- Исправлена рассинхронизация интерфейса РЛС и отсутствие движущихся отметок целей.
- Исправлены положение водителя и перепутанное направление движения командного Урала.
- Исправлено слишком раннее снижение «Герани-2», из-за которого БПЛА падал до цели.
- Улучшена интерполяция сущностей, чтобы уменьшить дёрганье машин и ракет в сетевой игре.

### Уже существующие системы в этой сборке

- Восстановленные пусковые системы крылатых и баллистических ракет WarTech.
- Крылатая ракета Storm Shadow и ударный БПЛА «Герань-2» с отдельной рельсовой катапультой.
- Пусковые установки ПВО MIM-104 Patriot и С-400.
- Перехватчики WTI-1 «Сокол», WTI-2 «Копьё» и WTI-3 «Страж» с дальностями 100, 250 и 400 блоков.
- Задержка реакции резервной пусковой на пять-семь секунд после неудачного перехвата.
- Сеть РЛС Renault TRM и С-400 с командным пунктом на базе Урала.
- Управляемая мобильная артиллерийская платформа HEMTT со взаимоисключающими модулями Greg и Henry.

### Обновление со старой версии

1. Сделайте резервную копию мира.
2. Удалите оригинальный JAR WarTech 1.1.1 и все старые JAR WarTech Reforged из папки `mods`.
3. Установите `WarTech-Reforged-1.3.0-hbm5751.jar` на сервер и на каждый клиент.
4. Оставьте установленным HBM NTM `1.0.27 X5751`.
5. Не используйте разные версии WarTech Reforged на сервере и клиентах.

### Известные ограничения

- Поддерживается только HBM NTM `1.0.27 X5751`.
- Проект в основном тестируется в творческом режиме; рецепты и развитие в выживании могут быть неполными.
- Большое количество тяжёлых импортированных моделей в одном месте может нагружать старые компьютеры.
- Это восстановление и расширение старого аддона для Minecraft 1.7.10. Сохраняйте резервные копии миров, а к сообщениям об ошибках прикладывайте полный crash report и Forge log.
