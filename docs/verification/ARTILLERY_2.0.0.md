# D-30 / CAESAR: crafting and terminal verification — 2.0.0

15 October 2026. Additional targeted acceptance of the **published 2.0.0 JAR**.
No gameplay fix was necessary; the release binary and tag remain unchanged.
SHA-256: `5BD888F13C329115805BC184C58DE6AB4D9FCC4EA2938A01ED037959CB25C6DE`.

## Русский

Обе установки крафтятся на **сборочной машине HBM**, не на верстаке.
Д-30: 30 секунд, 500 HE/тик, всего 300 000 HE. CAESAR: 50 секунд,
500 HE/тик, всего 500 000 HE. Материалы — в [каталоге рецептов](../recipes/CATALOG.ru.md).
Новая установка поставляется без энергии и боеприпасов; каждый выстрел стоит
один совместимый снаряд Greg и 500 HE.

Повторный прогон производства: **2609 проверок**, без ошибок.
Дополнительный сквозной сценарий: **76 проверок + 11 после настоящего рестарта**:

- Штатная обработка зарегистрированного рецепта HBM: точный предмет,
  расход материалов/энергии, отсутствие бесплатного результата без питания.
- Размещение изготовленного предмета обычным действием предмета в survival.
- Настоящая сессия серверного контейнера терминала; свои Д-30/CAESAR
  присутствуют на карте атаки с правильной дальностью и остатком снарядов.
- Мастер выбирает конкретный боеприпас из загруженного слота. Целеуказатель
  не считается боеприпасом; план резервирует реальный остаток.
- Сохранение и запуск операции через PlanningService: подготовка,
  два отдельных выстрела, ожидание перезарядки при слишком коротком интервале.
- Физические снаряды нужного типа, попадания, один расход на выпуск,
  правильные отметки фактического выпуска и завершения этапов.
- Отмена принятой команды до выпуска сохраняет снаряд и энергию.
- Перезапуск того же dedicated-мира во время наведения: сохраняются
  установка/операция/этап, выполняется ровно один ожидающий выстрел.
- Проверены минимальная/максимальная дальность и отказ чужому игроку.

Как применять через терминал: загрузите снаряды и питание, оставьте орудие
на твёрдой земле без водителя. **Атака → Добавить удар: мастер → установка →
боеприпас → количество → цель → время → сохранить → проверить → начать**.
Мастер добавляет подготовку и отдельный этап на каждый выстрел.
Физическая перезарядка имеет приоритет над слишком коротким интервалом плана.
Уже выпущенный снаряд отмена не возвращает.

## English

Both guns are manufactured in the **HBM Assembly Machine**, rather than a crafting
table. D-30: 30 seconds at 500 HE/tick, 300,000 HE total. CAESAR: 50 seconds at
500 HE/tick, 500,000 HE total. Ingredients are listed in the recipe catalog.
Factory guns contain no power or ammunition. Each shot costs one compatible
Greg shell and 500 HE.

The repeated production suite passed **2,609 checks**. A dedicated workflow
passed **76 checks plus 11 after a real server restart**: paid native HBM
processing, placement of the manufactured survival item, actual server terminal
session/snapshot, wizard payload/slot selection and reservation, save/start,
automatic preparation, two sequential physical shots and impacts, reload gating,
exact expenditure, cancellation before release, access/range checks, and recovery
of a pending accepted order across a restart without duplicate expenditure.

Usage: load shells and power, park on solid ground and leave the driver's seat.
**Attack → guided attack → gun → ammunition → quantity → target → time → save →
validate → start**. The wizard adds preparation and one step per shot. Native
reload takes priority over an unrealistically short requested interval.
Cancellation does not recall an already fired shell.

## Evidence and scope

[Machine-readable acceptance](ARTILLERY_2.0.0.json) contains individual check names,
the exact release SHA, recipe costs and hashes of the source reports.
The opt-in `TubePlanningQa` helper uses an isolated Forge world and server-side
fake players, native HBM processing, the shared terminal endpoint/wizard builder,
actual projectiles and two dedicated-server starts. It is separate from the mod.
The published 2.0.0 connected test with two real TCP clients remains separate.
This targeted test does not establish long survival progression or every modpack.
User saves/settings and the installed dev114 JAR were not used or changed.

[RU manual](../modular-warfare/GUIDE.ru.md) · [EN manual](../modular-warfare/GUIDE.en.md)
