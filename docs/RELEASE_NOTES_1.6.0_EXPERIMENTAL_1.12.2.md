# WarTech Reforged 1.6.0 Experimental for Minecraft 1.12.2

This is an **experimental compatibility port**, not the stable WarTech Reforged
release.

## Exact Runtime

- Minecraft `1.12.2`
- Forge `14.23.5.2860`
- NTM Extended `3.0.3`
- WarTech Reforged `1.6.0-experimental`
- File: `WarTech-Reforged-1.12.2-NTM-Extended-1.6.0-experimental.jar`

Do not install the Minecraft 1.7.10 WarTech JAR beside this build. NTM Extended
is required and is not bundled.

## Port Scope

- Ports the complete 1.6 content catalog, original binary assets, legacy GUI
  layouts, entities, launchers, vehicles, aircraft, radar and command network,
  IFF, recipes, effects, sounds, target queues, and save migration layer to the
  Minecraft 1.12.2 Forge/NTM APIs.
- Includes the latest vehicle-input, missile-impact, Kh-555 orientation, item
  texture-cache, Remote Pilot, camera, and manual Geran collision repairs.
- The release build is checked by the Gradle test suite, Forge reobfuscation,
  archive inspection, Java 8 bytecode validation, and dependency-class scans.

## Experimental Warning

Automated checks do not reproduce a complete modpack, an integrated client, or
a dedicated multiplayer server. Crashes and behavioral differences may remain,
including broken or displaced textures, GUI state errors, camera or flight
jitter, entity collision and physics problems, missile or explosion
differences, desynchronization, dedicated-server faults, and save-migration
bugs.

Back up every world before installation. Test on a copy first. Use only the
exact dependency versions listed above, and do not use this build on an
irreplaceable world. A useful bug report should include the crash report,
`latest.log`, screenshots or video, and exact reproduction steps.

---

# WarTech Reforged 1.6.0 Experimental для Minecraft 1.12.2

Это **экспериментальный порт совместимости**, а не стабильная версия WarTech
Reforged.

## Точное окружение

- Minecraft `1.12.2`
- Forge `14.23.5.2860`
- NTM Extended `3.0.3`
- WarTech Reforged `1.6.0-experimental`
- Файл: `WarTech-Reforged-1.12.2-NTM-Extended-1.6.0-experimental.jar`

Не устанавливайте рядом JAR WarTech для Minecraft 1.7.10. NTM Extended
обязателен и не входит в комплект.

## Состав порта

- Перенесены полный каталог 1.6, оригинальные бинарные ресурсы, старые
  интерфейсы, сущности, пусковые, техника, авиация, сеть РЛС и командования,
  IFF, рецепты, эффекты, звуки, очереди целей и миграция сохранений на API
  Forge/NTM для Minecraft 1.12.2.
- Включены последние исправления управления техникой, попаданий ракет,
  ориентации Х-555, кэша текстур предметов, Remote Pilot, камер и физических
  столкновений «Герани» в ручном режиме.
- Релиз проходит Gradle-тесты, Forge reobfuscation, проверку архива, байткода
  Java 8 и отсутствие упакованных классов зависимостей.

## Предупреждение

Автоматические проверки не заменяют полный тест в реальном модпаке, обычном
клиенте и на dedicated server. Возможны краши и отличия поведения: сломанные
или съехавшие текстуры, ошибки состояния интерфейсов, рывки камеры и полёта,
проблемы физики и столкновений, отличия ракет и взрывов, рассинхронизация,
ошибки выделенного сервера и миграции сохранений. Короче, это experimental:
баги и прочие неприятные сюрпризы ещё возможны.

Перед установкой сделайте резервную копию каждого мира и сначала тестируйте на
его копии. Используйте только указанные версии зависимостей и не запускайте
сборку на незаменимом мире. К полезному баг-репорту приложите crash report,
`latest.log`, скриншоты или видео и точные шаги воспроизведения.
