# Полная ведомость производственных рецептов 2.0.0

**РЕАЛИЗОВАНО В 2.0.0.** Производственная база dev112 + артиллерия dev114. Исходная перепись dev111 + HBM CE 2.6.1.0. Статус проверки и установки — в IMPLEMENTATION.ru.md.

Перепись: 273 item/meta; промежуточных узлов: 54. Числа — игровой баланс dev112.

Символические имена `plan:name` соответствуют зарегистрированным `wartecmod:industrial_name`. Статус `proposed` в схеме означает прямой рецепт; `functional` — получение действием, `reserved` — отключено.
В JSON сохранены точные ID ингредиентов, количества, время/HE, жидкости и статус.
Здесь единицы сырья — предметы HBM/ванили, не реальные технические количества.

## Промежуточные узлы

| Предмет / ID и metadata | Станок / ступень | Выход; время; HE/тик | Состав | Особенности |
|---|---|---|---|---|
| Плата управления I<br>`plan:board_t1` | Паяльная станция HBM / 1 | 1 шт.; 5.0 с; 100 | Микрочип ×4; Конденсатор ×2; Печатная плата ×4; wireFineLead ×4 | Раздельные группы входов паяльной станции; штатная HBM PCB остаётся исходной печатной платой. |
| Военная плата II<br>`plan:board_t2` | Паяльная станция HBM / 2 | 1 шт.; 10.0 с; 500 | Микрочип ×8; Танталовый конденсатор ×2; Печатная плата ×6; ingotAnyHardPlastic ×1; wireFineLead ×6 ; SULFURIC_ACID 500 mB | Раздельные группы входов паяльной станции; штатная HBM PCB остаётся исходной печатной платой. |
| Плата обработки III<br>`plan:board_t3` | Паяльная станция HBM / 3 | 1 шт.; 15.0 с; 2500 | Передовой интегральный микрочип ×4; Микрочип ×8; Танталовый конденсатор ×4; Печатная плата ×8; ingotAnyHardPlastic ×2; wireFineLead ×8 ; SOLVENT 500 mB | Раздельные группы входов паяльной станции; штатная HBM PCB остаётся исходной печатной платой. |
| Плата ПРО/стратегии IV<br>`plan:board_t4` | Паяльная станция HBM / 4 | 1 шт.; 20.0 с; 10000 | Передовой интегральный микрочип ×8; Атомные часы ×1; Танталовый конденсатор ×8; Печатная плата ×12; ingotAnyHardPlastic ×4; wireFineLead ×12 ; PERFLUOROMETHYL 1000 mB | Раздельные группы входов паяльной станции; штатная HBM PCB остаётся исходной печатной платой. |
| Комплект сервоприводов<br>`plan:servo_pack` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | Мотор ×2; Электрический поршень ×1; boltSteel ×4; wireFineCopper ×4 |  |
| Блок питания<br>`plan:power_unit` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | Конденсаторная плата ×2; wireFineCopper ×8; plateSteel ×4; Плата управления I ×1 |  |
| Кабина с органами управления<br>`plan:vehicle_cab` | Сборочная машина HBM / 1 | 1 шт.; 10.0 с; 100 | plateSteel ×16; minecraft:glass_pane@0 ×6; Плата управления I ×1; wireFineCopper ×8; ingotAnyRubber ×8 |  |
| Колёсная ось<br>`plan:wheel_axle` | Сборочная машина HBM / 1 | 1 шт.; 10.0 с; 100 | plateSteel ×8; ingotAnyRubber ×8; boltSteel ×4; Гидравлический поршень ×1 |  |
| Гусеничный модуль<br>`plan:track_unit` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | plateSteel ×32; ingotAnyRubber ×8; Мотор ×4; boltSteel ×8 |  |
| Лёгкое колёсное шасси<br>`plan:chassis_light` | Сборочная машина HBM / 1 | 1 шт.; 10.0 с; 100 | plateSteel ×24; Колёсная ось ×2; Мотор ×2; Гидравлический поршень ×2; boltSteel ×12 |  |
| Среднее колёсное шасси<br>`plan:chassis_medium` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | plateSteel ×48; Колёсная ось ×3; Мотор ×4; Гидравлический поршень ×2; boltSteel ×12 |  |
| Тяжёлое колёсное шасси<br>`plan:chassis_heavy` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | plateDuraSteel ×48; Колёсная ось ×4; Мотор ×4; Гидравлический поршень ×2; boltSteel ×12 |  |
| Гусеничное шасси<br>`plan:chassis_tracked` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | plateDuraSteel ×32; Гусеничный модуль ×2; Гидравлический поршень ×4; boltSteel ×16 |  |
| Прицепная рама<br>`plan:chassis_trailer` | Сборочная машина HBM / 1 | 1 шт.; 10.0 с; 100 | plateSteel ×24; Колёсная ось ×2; boltSteel ×8 |  |
| Радиолокационный модуль 1<br>`plan:radar_t1` | Сборочная машина HBM / 1 (цепочка 2) | 1 шт.; 10.0 с; 100 | Плата управления I ×1; Магнетрон ×2; wireFineCopper ×8; plateAluminum ×8; Комплект сервоприводов ×1 |  |
| Радиолокационный модуль 2<br>`plan:radar_t2` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | Военная плата II ×2; Магнетрон ×4; wireFineCopper ×16; plateAluminum ×16; Комплект сервоприводов ×1 |  |
| Радиолокационный модуль 3<br>`plan:radar_t3` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | Плата обработки III ×3; Магнетрон ×6; wireFineCopper ×24; plateTitanium ×24; Комплект сервоприводов ×1 |  |
| Оптический модуль<br>`plan:optics` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | Поляризованная линза ×2; Интегральная схема ×1; plateAluminum ×2; Комплект сервоприводов ×1 |  |
| Тепловизионный модуль<br>`plan:thermal_optics` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | Оптический модуль ×1; Военная плата II ×1; Конденсаторная плата ×1; plateTitanium ×4 |  |
| Модуль связи 1<br>`plan:link_t1` | Сборочная машина HBM / 1 | 1 шт.; 10.0 с; 100 | Плата управления I ×1; wireFineCopper ×4; Медная катушка ×1; ingotAnyPlastic ×2 |  |
| Модуль связи 2<br>`plan:link_t2` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | Военная плата II ×1; wireFineCopper ×8; Медная катушка ×2; ingotAnyPlastic ×4 |  |
| Модуль связи 3<br>`plan:link_t3` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | Плата обработки III ×1; wireFineCopper ×12; Медная катушка ×3; ingotAnyPlastic ×6 |  |
| Система управления огнём 1<br>`plan:fire_control_t1` | Сборочная машина HBM / 1 | 1 шт.; 10.0 с; 100 | Плата управления I ×2; Guidance System Tier 2 ×1; Модуль связи 1 ×1; plateSteel ×4 |  |
| Система управления огнём 2<br>`plan:fire_control_t2` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | Военная плата II ×2; Guidance System Tier 3 ×1; Модуль связи 2 ×1; plateSteel ×8 |  |
| Система управления огнём 3<br>`plan:fire_control_t3` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | Плата обработки III ×2; Guidance System Tier 4 ×1; Модуль связи 3 ×1; plateSteel ×12 |  |
| Система управления огнём 4<br>`plan:fire_control_t4` | Сборочная машина HBM / 4 | 1 шт.; 80.0 с; 10000 | Плата ПРО/стратегии IV ×2; Guidance System Tier 5 ×1; Модуль связи 3 ×1; plateSteel ×16 |  |
| Малый пакет направляющих<br>`plan:launch_pack_small` | Сборочная машина HBM / 1 (цепочка 2) | 1 шт.; 10.0 с; 100 | ntmpipeSteel ×8; plateSteel ×8; Комплект сервоприводов ×1; Гидравлический поршень ×2; boltSteel ×8 |  |
| Средний пакет направляющих<br>`plan:launch_pack_medium` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | ntmpipeSteel ×16; plateSteel ×16; Комплект сервоприводов ×1; Гидравлический поршень ×2; boltSteel ×8 |  |
| Тяжёлый пакет направляющих<br>`plan:launch_pack_heavy` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | ntmpipeSteel ×24; plateDuraSteel ×24; Комплект сервоприводов ×1; Гидравлический поршень ×2; boltSteel ×8 |  |
| Артиллерийский узел<br>`plan:gun_mount` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | barrelHeavySteel ×4; plateDuraSteel ×16; Комплект сервоприводов ×1; Гидравлический поршень ×2 |  |
| Спаренный 30-мм узел<br>`plan:gun_30mm` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | barrelHeavySteel ×2; Мотор ×1; Военная плата II ×1; plateSteel ×8 |  |
| Корпус лёгкого реактивного снаряда<br>`plan:empty_rocket_light` | Сборочная машина HBM / 1 | 4 шт.; 10.0 с; 100 | plateSteel ×2; ntmpipeSteel ×1; Cruise Fins small ×1 |  |
| Корпус среднего реактивного снаряда<br>`plan:empty_rocket_medium` | Сборочная машина HBM / 2 | 2 шт.; 20.0 с; 500 | plateSteel ×6; plateAluminum ×2; Cruise Fins big ×1 |  |
| Корпус тяжёлого реактивного снаряда<br>`plan:empty_rocket_heavy` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | plateSteel ×8; plateTitanium ×4; Cruise Fins big ×1 |  |
| Ступень стратегической ракеты<br>`plan:icbm_stage` | Сборочная машина HBM / 4 | 1 шт.; 120.0 с; 10000 | plateTitanium ×32; plateDuraSteel ×16; Большой топливный бак ×2; Большой двигатель ×2; Solid fuel rocket booster ×8; Комплект сервоприводов ×1 |  |
| Платформа разведения боевых блоков<br>`plan:reentry_bus` | Сборочная машина HBM / 4 | 1 шт.; 80.0 с; 10000 | plateTitanium ×24; ingotTungsten ×16; Guidance System Tier 6 ×2; Комплект сервоприводов ×4; Блок питания ×1 |  |
| Планер заводского БПЛА<br>`plan:airframe_uav` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | plateAluminum ×24; ingotFiberglass ×16; Комплект сервоприводов ×2; boltSteel ×8 |  |
| Планер тактического самолёта<br>`plan:airframe_fighter` | Сборочная машина HBM / 3 | 1 шт.; 120.0 с; 2000 | plateAluminum ×64; plateTitanium ×32; plateDuraSteel ×16; Комплект сервоприводов ×8; Гидравлический поршень ×6; boltSteel ×32 |  |
| Планер тяжёлого бомбардировщика<br>`plan:airframe_bomber` | Сборочная машина HBM / 4 | 1 шт.; 200.0 с; 10000 | Планер тактического самолёта ×2; plateAluminum ×64; plateTitanium ×32; Комплект сервоприводов ×8; boltSteel ×32 |  |
| Пустой артиллерийский корпус<br>`plan:empty_artillery_shell` | Сборочная машина HBM / 1 | 4 шт.; 10.0 с; 100 | plateSteel ×8; ingotCopper ×2; Cruise Fins small ×1 |  |
| Простой неконтактный взрыватель<br>`plan:fuse_proximity_basic` | Сборочная машина HBM / 1 | 1 шт.; 10.0 с; 100 | Плата управления I ×1; Магнетрон ×1; Конденсатор ×1; plateAluminum ×2 |  |
| Упаковка 100 патронов 30 мм<br>`plan:rounds_30mm_100` | Боеприпасный пресс HBM / 2 | 1 шт.; 0.0 с; 0 | ingotCopper ×16; ingotSteel ×8; Кордит ×8; ingotAnyHighexplosive ×4 | Игровой пакет на 100 патронов, Ammo Press HBM. 6 пакетов дают ровно 600 патронов ленты; масштаб абстрактный. Сталь разделить по 4 на две позиции; время/HE задаёт сам станок. |
| Секция несущей мачты<br>`plan:mast_section` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | plateSteel ×4; boltSteel ×2; wireFineCopper ×2 |  |
| Панель корпуса стратегической РЛС<br>`plan:radar_shell_panel` | Сборочная машина HBM / 3 | 8 шт.; 40.0 с; 2000 | plateSteel ×8; plateAluminum ×8; boltSteel ×4 |  |
| Комплект 64 панелей РЛС<br>`plan:radar_shell_bundle` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | Панель корпуса стратегической РЛС ×64 | Оплаченный материал для 64 служебных блоков корпуса; не отдельный свободно размещаемый блок. |
| Сверхзвуковая двигательная секция<br>`plan:supersonic_core` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | Subsonic Cruise Missile without Warhead ×1; plateTitanium ×12; Усиленные лопасти турбовентилятора ×2 |  |
| Гиперзвуковая двигательная секция<br>`plan:hypersonic_core` | Сборочная машина HBM / 4 | 1 шт.; 80.0 с; 10000 | Сверхзвуковая двигательная секция ×1; plateDuraSteel ×16; ingotTungsten ×12; Комплект сервоприводов ×2 |  |

## Материалы

| Предмет / ID и metadata | Станок / ступень | Выход; время; HE/тик | Состав | Особенности |
|---|---|---|---|---|
| Item Ingot Armor Steel<br>`wartecmod:itemingotarmorsteel@0` | Доменная печь HBM / 2 | 4 шт.; 60.0 с; 0 | ingotSteel ×3; ingotTungsten ×1 | Игровой броневой сплав; штатный нагрев/кокс/воздух домны оплачивается самой машиной. |
| U238 Plate<br>`wartecmod:itemplateu238@0` | Пресс HBM / 2 | 1 шт.; 0.0 с; 0 | ingotU238 ×1 | Штамп PLATE; длительность и топливо задаёт пресс. Сохраняется радиационная опасность. |

## Базовые детали

| Предмет / ID и metadata | Станок / ступень | Выход; время; HE/тик | Состав | Особенности |
|---|---|---|---|---|
| Engine Inlet Tier 1<br>`wartecmod:itemengineinletsectiontier1@0` | Сборочная машина HBM / 1 (цепочка 2) | 1 шт.; 10.0 с; 100 | plateAluminum ×4; plateSteel ×2; Комплект сервоприводов ×1 |  |
| Turbofan Engine Tier 1<br>`wartecmod:itemturbofanenginetier1@0` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | plateTitanium ×6; plateSteel ×8; Титановая паровая турбина ×2; Усиленные лопасти турбовентилятора ×1; wireFineCopper ×8; Мотор ×1 |  |
| Solid fuel rocket booster<br>`wartecmod:itemsolidbooster@0` | Сборочная машина HBM / 1 | 1 шт.; 10.0 с; 100 | plateSteel ×2; Твердое топливо (Ракетное) ×8; Микрочип ×1 |  |
| Cruise Fins small<br>`wartecmod:itemcruisefinssmall@0` | Сборочная машина HBM / 1 | 4 шт.; 10.0 с; 100 | plateSteel ×2; boltSteel ×2 |  |
| Cruise Fins big<br>`wartecmod:itemcruisefinsbig@0` | Сборочная машина HBM / 1 | 2 шт.; 10.0 с; 100 | plateTitanium ×4; boltSteel ×2 |  |
| Cruise Wings<br>`wartecmod:itemcruisewings@0` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | plateAluminum ×8; plateTitanium ×2; Комплект сервоприводов ×1 |  |
| Subsonic Cruise Missile without Warhead<br>`wartecmod:itemcruisemissilenowarheadtier1@0` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | plateSteel ×12; plateAluminum ×8; Средний топливный бак ×1; Cruise Wings ×1; Engine Inlet Tier 1 ×1; Turbofan Engine Tier 1 ×1 | Двигательная секция без БЧ и стартового ускорителя; не включает готовое наведение. |

## Наведение

| Предмет / ID и metadata | Станок / ступень | Выход; время; HE/тик | Состав | Особенности |
|---|---|---|---|---|
| Guidance System Tier 1<br>`wartecmod:itemguidancesystemtier1@0` | Сборочная машина HBM / 1 | 1 шт.; 10.0 с; 100 | Плата управления I ×1; wireFineCopper ×2; ingotAnyPlastic ×2 | Готовый навигационный узел: плата изготовлена на паяльной станции, корпус и оборудование собраны в HBM Assembly Machine. |
| Guidance System Tier 2<br>`wartecmod:itemguidancesystemtier2@0` | Сборочная машина HBM / 1 | 1 шт.; 10.0 с; 100 | Плата управления I ×2; wireFineCopper ×4; ingotAnyPlastic ×2; hbm:circuit@20 ×1 | Готовый навигационный узел: плата изготовлена на паяльной станции, корпус и оборудование собраны в HBM Assembly Machine. |
| Guidance System Tier 3<br>`wartecmod:itemguidancesystemtier3@0` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | Военная плата II ×1; wireFineCopper ×6; ingotAnyPlastic ×2; hbm:circuit@20 ×1; Модуль связи 1 ×1 | Готовый навигационный узел: плата изготовлена на паяльной станции, корпус и оборудование собраны в HBM Assembly Machine. |
| Guidance System Tier 4<br>`wartecmod:itemguidancesystemtier4@0` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | Военная плата II ×2; wireFineCopper ×8; ingotAnyPlastic ×2; hbm:circuit@20 ×1; Модуль связи 2 ×1 | Готовый навигационный узел: плата изготовлена на паяльной станции, корпус и оборудование собраны в HBM Assembly Machine. |
| Guidance System Tier 5<br>`wartecmod:itemguidancesystemtier5@0` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | Плата обработки III ×1; wireFineCopper ×10; ingotAnyPlastic ×2; hbm:circuit@20 ×1; Модуль связи 3 ×1; Атомные часы ×1 | Готовый навигационный узел: плата изготовлена на паяльной станции, корпус и оборудование собраны в HBM Assembly Machine. |
| Guidance System Tier 6<br>`wartecmod:itemguidancesystemtier6@0` | Сборочная машина HBM / 4 | 1 шт.; 80.0 с; 10000 | Плата обработки III ×2; wireFineCopper ×12; ingotAnyPlastic ×2; hbm:circuit@20 ×1; Модуль связи 3 ×1; Атомные часы ×1 | Готовый навигационный узел: плата изготовлена на паяльной станции, корпус и оборудование собраны в HBM Assembly Machine. |

## Боевые части

| Предмет / ID и metadata | Станок / ступень | Выход; время; HE/тик | Состав | Особенности |
|---|---|---|---|---|
| HE Warhead (CM)<br>`wartecmod:itemwarheadhecm@0` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | Средняя боеголовка ×1; plateSteel ×4 | Опирается на готовые игровые компоненты HBM; штатная ядерная/химическая цепочка не обходится. |
| Cluster Warhead (CM)<br>`wartecmod:itemwarheadcluster@0` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | Средняя кластерная боеголовка ×1; plateSteel ×4 | Опирается на готовые игровые компоненты HBM; штатная ядерная/химическая цепочка не обходится. |
| Bunker Buster Warhead (CM)<br>`wartecmod:itemwarheadbuster@0` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | Средняя бункерная боеголовка ×1; U238 Plate ×4; plateTitanium ×4 | Опирается на готовые игровые компоненты HBM; штатная ядерная/химическая цепочка не обходится. |
| EMP Warhead (CM)<br>`wartecmod:itemwarheademp@0` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | Магнетрон ×4; Блок питания ×1; Военная плата II ×1; plateLead ×4 | Опирается на готовые игровые компоненты HBM; штатная ядерная/химическая цепочка не обходится. |
| Item Warhead TB<br>`wartecmod:itemwarheadtb@0` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | ingotAnyHighexplosive ×8; plateAluminum ×4; Интегральная схема ×1 ; KEROSENE_REFORM 1000 mB | Опирается на готовые игровые компоненты HBM; штатная ядерная/химическая цепочка не обходится. |
| Nuclear Warhead (CM)<br>`wartecmod:itemwarheadnuclearcm@0` | Сборочная машина HBM / 4 | 1 шт.; 80.0 с; 10000 | Ядерная боеголовка ×1; plateTitanium ×8; Плата обработки III ×1 | Опирается на готовые игровые компоненты HBM; штатная ядерная/химическая цепочка не обходится. |
| Thermonuclear Warhead (CM)<br>`wartecmod:itemwarheadhcm@0` | Сборочная машина HBM / 4 | 1 шт.; 80.0 с; 10000 | Термоядерная боеголовка ×1; plateTitanium ×12; Плата ПРО/стратегии IV ×1 | Опирается на готовые игровые компоненты HBM; штатная ядерная/химическая цепочка не обходится. |
| Kinetic Kill Vehicle<br>`wartecmod:itemkkv@0` | Сборочная машина HBM / 4 | 1 шт.; 80.0 с; 10000 | ingotTungsten ×16; plateTitanium ×8; Комплект сервоприводов ×2; Guidance System Tier 6 ×1 | Опирается на готовые игровые компоненты HBM; штатная ядерная/химическая цепочка не обходится. |
| Thermonuclear Warhead (SLBM)<br>`wartecmod:itemhwarhead@0` | Сборочная машина HBM / 4 | 1 шт.; 80.0 с; 10000 | Термоядерная боеголовка ×1; plateTitanium ×16; Плата ПРО/стратегии IV ×1; U238 Plate ×8 | Опирается на готовые игровые компоненты HBM; штатная ядерная/химическая цепочка не обходится. |
| Gas Warhead<br>`wartecmod:itemwarheadgas@0` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | Ядовитый газовый баллон ×4; ingotAnyPlastic ×4; plateSteel ×4 | Опирается на готовые игровые компоненты HBM; штатная ядерная/химическая цепочка не обходится. |
| Neutron Warhead<br>`wartecmod:itemwarheadneutron@0` | Сборочная машина HBM / 4 | 1 шт.; 80.0 с; 10000 | Ядерная боеголовка ×1; plateTitanium ×8; plateLead ×4; Плата ПРО/стратегии IV ×1 | Опирается на готовые игровые компоненты HBM; штатная ядерная/химическая цепочка не обходится. |

## Модули крылатых ракет

| Предмет / ID и metadata | Станок / ступень | Выход; время; HE/тик | Состав | Особенности |
|---|---|---|---|---|
| Корпус реактивной ракеты-дрона<br>`wartecmod:cruisemodule@0` | Сборочная машина HBM / 1 | 1 шт.; 10.0 с; 100 | plateAluminum ×8; ingotFiberglass ×4; plateSteel ×2 |  |
| Корпус классической крылатой ракеты<br>`wartecmod:cruisemodule@1` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | plateAluminum ×16; plateSteel ×12; plateTitanium ×4 |  |
| Тяжёлый малозаметный корпус<br>`wartecmod:cruisemodule@2` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | plateAluminum ×24; plateDuraSteel ×16; plateTitanium ×8 |  |
| Тяжёлый дальнобойный корпус ракеты-дрона<br>`wartecmod:cruisemodule@32` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | plateAluminum ×32; plateDuraSteel ×24; plateTitanium ×16 |  |
| Экономичный маршевый двигатель<br>`wartecmod:cruisemodule@3` | Сборочная машина HBM / 1 | 1 шт.; 10.0 с; 100 | Мотор ×1; plateAluminum ×4; Титановая паровая турбина ×1 |  |
| Универсальный маршевый двигатель<br>`wartecmod:cruisemodule@4` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | Turbofan Engine Tier 1 ×1; plateAluminum ×4 |  |
| Форсированный маршевый двигатель<br>`wartecmod:cruisemodule@5` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | Turbofan Engine Tier 1 ×1; Усиленные лопасти турбовентилятора ×2; plateTitanium ×8 |  |
| Тяжёлый экономичный двигатель<br>`wartecmod:cruisemodule@33` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | Turbofan Engine Tier 1 ×2; Титановая паровая турбина ×2; plateTitanium ×12 |  |
| Малая топливная секция<br>`wartecmod:cruisemodule@6` | Сборочная машина HBM / 1 | 1 шт.; 10.0 с; 100 | Малый топливный бак ×1; ingotAnyPlastic ×2 ; KEROSENE_REFORM 1000 mB | Топливный модуль изготовляется заправленным; энергия модуля оплачена топливом. |
| Стандартная топливная секция<br>`wartecmod:cruisemodule@7` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | Средний топливный бак ×1; ingotAnyPlastic ×4 ; KEROSENE_REFORM 3000 mB | Топливный модуль изготовляется заправленным; энергия модуля оплачена топливом. |
| Увеличенная топливная секция<br>`wartecmod:cruisemodule@8` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | Большой топливный бак ×1; ingotAnyPlastic ×8 ; KEROSENE_REFORM 6000 mB | Топливный модуль изготовляется заправленным; энергия модуля оплачена топливом. |
| Тяжёлая дальнобойная топливная секция<br>`wartecmod:cruisemodule@34` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | Большой топливный бак ×2; ingotAnyHardPlastic ×8 ; KEROSENE_REFORM 12000 mB | Топливный модуль изготовляется заправленным; энергия модуля оплачена топливом. |
| Компактные крылья<br>`wartecmod:cruisemodule@9` | Сборочная машина HBM / 1 | 1 шт.; 10.0 с; 100 | plateAluminum ×4; boltSteel ×2 |  |
| Крылья увеличенной дальности<br>`wartecmod:cruisemodule@10` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | plateAluminum ×8; plateTitanium ×2 |  |
| Складные крылья<br>`wartecmod:cruisemodule@11` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | plateAluminum ×6; plateTitanium ×2; Комплект сервоприводов ×1 |  |
| Тяжёлые складные крылья дальности<br>`wartecmod:cruisemodule@35` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | plateAluminum ×12; plateTitanium ×8; Комплект сервоприводов ×2 |  |
| Координатный навигатор<br>`wartecmod:cruisemodule@12` | Сборочная машина HBM / 1 | 1 шт.; 10.0 с; 100 | Guidance System Tier 1 ×1 |  |
| Маршрутный навигатор<br>`wartecmod:cruisemodule@13` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | Guidance System Tier 3 ×1; ingotAnyHardPlastic ×2 |  |
| Навигатор огибания рельефа<br>`wartecmod:cruisemodule@14` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | Guidance System Tier 5 ×1; Радиолокационный модуль 1 ×1; Оптический модуль ×1 |  |
| Координатный носовой модуль<br>`wartecmod:cruisemodule@15` | Сборочная машина HBM / 1 | 1 шт.; 10.0 с; 100 | ingotAnyPlastic ×1; plateAluminum ×1 |  |
| Оптическая головка наведения<br>`wartecmod:cruisemodule@16` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | Оптический модуль ×1; Плата управления I ×1 |  |
| Тепловая головка наведения<br>`wartecmod:cruisemodule@17` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | Тепловизионный модуль ×1; Военная плата II ×1 |  |
| Радиолокационная головка наведения<br>`wartecmod:cruisemodule@18` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | Радиолокационный модуль 2 ×1; Плата обработки III ×1 |  |
| Фугасная боевая часть<br>`wartecmod:cruisemodule@19` | Сборочная машина HBM / 1 | 1 шт.; 10.0 с; 100 | Малая боеголовка ×1; plateSteel ×2 |  |
| Термобарическая боевая часть<br>`wartecmod:cruisemodule@20` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | Item Warhead TB ×1; plateAluminum ×2 |  |
| Проникающая боевая часть<br>`wartecmod:cruisemodule@21` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | Bunker Buster Warhead (CM) ×1; plateDuraSteel ×4 |  |
| Кассетная боевая часть<br>`wartecmod:cruisemodule@22` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | Средняя кластерная боеголовка ×1; plateSteel ×2 |  |
| ЭМИ-боевая часть<br>`wartecmod:cruisemodule@23` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | EMP Warhead (CM) ×1; Военная плата II ×1 |  |
| Тяжёлая фугасная боевая часть<br>`wartecmod:cruisemodule@36` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | Большая боеголовка ×1; plateDuraSteel ×8 |  |
| Тяжёлая термобарическая боевая часть<br>`wartecmod:cruisemodule@37` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | Item Warhead TB ×3; plateAluminum ×8 |  |
| Осколочно-фугасная боевая часть<br>`wartecmod:cruisemodule@38` | Сборочная машина HBM / 1 | 1 шт.; 10.0 с; 100 | Малая боеголовка ×1; Свинцовые гранулы ×8; plateSteel ×4 |  |
| Направленная противотехническая боевая часть<br>`wartecmod:cruisemodule@39` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | Средняя боеголовка ×1; plateCopper ×4; plateDuraSteel ×4 |  |
| Зажигательная боевая часть<br>`wartecmod:cruisemodule@40` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | Малая зажигательная боеголовка ×1; ingotAnyPlastic ×4 |  |
| Тяжёлая кассетная боевая часть<br>`wartecmod:cruisemodule@41` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | Большая кластерная боеголовка ×1; plateSteel ×8 |  |
| Тяжёлая проникающая боевая часть<br>`wartecmod:cruisemodule@42` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | Большая бункерная боеголовка ×1; U238 Plate ×8; plateDuraSteel ×8 |  |
| Тяжёлая осколочно-фугасная боевая часть<br>`wartecmod:cruisemodule@43` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | Большая боеголовка ×1; Свинцовые гранулы ×24; plateSteel ×8 |  |
| Контактный взрыватель<br>`wartecmod:cruisemodule@24` | Сборочная машина HBM / 1 | 1 шт.; 10.0 с; 100 | Конденсатор ×1; plateSteel ×1 |  |
| Взрыватель с задержкой<br>`wartecmod:cruisemodule@25` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | hbm:circuit@20 ×1; Плата управления I ×1; plateSteel ×1 |  |
| Взрыватель воздушного подрыва<br>`wartecmod:cruisemodule@26` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | Радиолокационный модуль 1 ×1; Военная плата II ×1; plateAluminum ×2 |  |
| Комплект наземного ускорителя<br>`wartecmod:cruisemodule@27` | Сборочная машина HBM / 1 | 1 шт.; 10.0 с; 100 | Solid fuel rocket booster ×1; boltSteel ×2 |  |
| Адаптер авиационного запуска<br>`wartecmod:cruisemodule@28` | Сборочная машина HBM / 1 | 1 шт.; 10.0 с; 100 | plateAluminum ×2; boltSteel ×2 |  |
| Комплект направляющей ракеты-дрона<br>`wartecmod:cruisemodule@29` | Сборочная машина HBM / 1 | 1 шт.; 10.0 с; 100 | plateSteel ×4; ingotAnyRubber ×2 |  |
| Автономный модуль управления<br>`wartecmod:cruisemodule@30` | Сборочная машина HBM / 1 | 1 шт.; 10.0 с; 100 | ingotAnyPlastic ×1; wireFineCopper ×2 |  |
| Командный канал обновления маршрута<br>`wartecmod:cruisemodule@31` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | Модуль связи 2 ×1; ingotAnyPlastic ×2 |  |

## Модули БПЛА

| Предмет / ID и metadata | Станок / ступень | Выход; время; HE/тик | Состав | Особенности |
|---|---|---|---|---|
| Одноразовый планер "летающее крыло"<br>`wartecmod:uavmodule@0` | Сборочная машина HBM / 1 | 1 шт.; 10.0 с; 100 | plateAluminum ×6; ingotFiberglass ×4; boltSteel ×2 |  |
| Разведывательный планер БПЛА<br>`wartecmod:uavmodule@1` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | plateAluminum ×12; ingotFiberglass ×8; Комплект сервоприводов ×2 |  |
| Тактический ударный планер БПЛА<br>`wartecmod:uavmodule@2` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | plateAluminum ×20; ingotFiberglass ×12; plateTitanium ×8; Комплект сервоприводов ×3 |  |
| Экономичный поршневой двигатель<br>`wartecmod:uavmodule@3` | Сборочная машина HBM / 1 | 1 шт.; 10.0 с; 100 | Мотор ×1; plateAluminum ×4 |  |
| Сбалансированный роторный двигатель<br>`wartecmod:uavmodule@4` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | Мотор ×2; Титановая паровая турбина ×1; plateAluminum ×6 |  |
| Тяжёлый турбовинтовой двигатель<br>`wartecmod:uavmodule@5` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | Деш-мотор ×2; Титановая паровая турбина ×2; plateTitanium ×6 |  |
| Компактный топливный модуль<br>`wartecmod:uavmodule@6` | Сборочная машина HBM / 1 | 1 шт.; 10.0 с; 100 | Малый топливный бак ×1; ingotAnyPlastic ×2 ; KEROSENE_REFORM 1000 mB | Первый полный запас энергии оплачивается изготовлением модуля; дальнейшая перезарядка штатная. |
| Топливный бак большой дальности<br>`wartecmod:uavmodule@7` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | Средний топливный бак ×1; ingotAnyPlastic ×4 ; KEROSENE_REFORM 3000 mB | Первый полный запас энергии оплачивается изготовлением модуля; дальнейшая перезарядка штатная. |
| Гибридный энергомодуль<br>`wartecmod:uavmodule@8` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | Малый топливный бак ×1; Литий-ионный аккумулятор (LEGACY) ×1; Военная плата II ×1; wireFineCopper ×8 ; KEROSENE_REFORM 1500 mB | Первый полный запас энергии оплачивается изготовлением модуля; дальнейшая перезарядка штатная. |
| Базовый контроллер полёта<br>`wartecmod:uavmodule@9` | Сборочная машина HBM / 1 | 1 шт.; 10.0 с; 100 | Плата управления I ×1; hbm:circuit@20 ×1 |  |
| Точный автопилот<br>`wartecmod:uavmodule@10` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | Военная плата II ×1; hbm:circuit@20 ×1; Комплект сервоприводов ×1 |  |
| Боевой вычислитель полёта<br>`wartecmod:uavmodule@11` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | Плата обработки III ×1; Атомные часы ×1; Комплект сервоприводов ×1 |  |
| Радиоканал малой дальности<br>`wartecmod:uavmodule@12` | Сборочная машина HBM / 1 | 1 шт.; 10.0 с; 100 | Модуль связи 1 ×1 |  |
| Защищённый тактический канал<br>`wartecmod:uavmodule@13` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | Модуль связи 2 ×1; Военная плата II ×1 |  |
| Спутниковый канал связи<br>`wartecmod:uavmodule@14` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | Модуль связи 3 ×1; Атомные часы ×1 |  |
| Дневная камера<br>`wartecmod:uavmodule@15` | Сборочная машина HBM / 1 | 1 шт.; 10.0 с; 100 | Поляризованная линза ×1; Плата управления I ×1; plateAluminum ×2 |  |
| Оптико-тепловизионная станция<br>`wartecmod:uavmodule@16` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | Тепловизионный модуль ×1; Военная плата II ×1 |  |
| Радар синтезированной апертуры<br>`wartecmod:uavmodule@17` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | Радиолокационный модуль 2 ×1; Плата обработки III ×1 |  |
| Фугасная боевая часть<br>`wartecmod:uavmodule@18` | Сборочная машина HBM / 1 | 1 шт.; 10.0 с; 100 | Малая боеголовка ×1; plateSteel ×2 |  |
| Термобарическая боевая часть<br>`wartecmod:uavmodule@19` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | Item Warhead TB ×1 |  |
| Кумулятивная боевая часть<br>`wartecmod:uavmodule@24` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | Малая боеголовка ×1; plateCopper ×4 |  |
| Тяжелая фугасная боевая часть<br>`wartecmod:uavmodule@25` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | Большая боеголовка ×1; plateSteel ×4 |  |
| Тяжелая термобарическая боевая часть<br>`wartecmod:uavmodule@26` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | Item Warhead TB ×2; plateAluminum ×4 |  |
| Лёгкая двухточечная подвеска<br>`wartecmod:uavmodule@20` | Сборочная машина HBM / 1 (цепочка 2) | 1 шт.; 10.0 с; 100 | plateSteel ×4; Комплект сервоприводов ×1 |  |
| Тяжёлая четырёхточечная подвеска<br>`wartecmod:uavmodule@21` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | plateSteel ×8; plateTitanium ×4; Комплект сервоприводов ×2 |  |
| Одиночный подфюзеляжный ракетный подвес<br>`wartecmod:uavmodule@27` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | plateTitanium ×8; Комплект сервоприводов ×2; Военная плата II ×1 |  |
| Блок тепловых ловушек<br>`wartecmod:uavmodule@22` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | plateSteel ×4; Плата управления I ×1; Кассета ЛТЦ для MQ-9 ×8 |  |
| Компактный комплекс РЭБ<br>`wartecmod:uavmodule@23` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | Магнетрон ×2; Модуль связи 2 ×1; Плата обработки III ×1; plateAluminum ×4 |  |

## Штатные ракеты

| Предмет / ID и metadata | Станок / ступень | Выход; время; HE/тик | Состав | Особенности |
|---|---|---|---|---|
| Subsonic HE Cruise Missile<br>`wartecmod:itemcruisemissilehe@0` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | Subsonic Cruise Missile without Warhead ×1; HE Warhead (CM) ×1; Guidance System Tier 1 ×1; Solid fuel rocket booster ×1 ; KEROSENE_REFORM 3000 mB |  |
| Subsonic Cluster Cruise Missile<br>`wartecmod:itemcruisemissilecluster@0` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | Subsonic Cruise Missile without Warhead ×1; Cluster Warhead (CM) ×1; Guidance System Tier 2 ×1; Solid fuel rocket booster ×1 ; KEROSENE_REFORM 3000 mB |  |
| Subsonic Bunker Buster Cruise Missile<br>`wartecmod:itemcruisemissilebuster@0` | Сборочная машина HBM / 2 (цепочка 3) | 1 шт.; 20.0 с; 500 | Subsonic Cruise Missile without Warhead ×1; Bunker Buster Warhead (CM) ×1; Guidance System Tier 3 ×1; Solid fuel rocket booster ×1 ; KEROSENE_REFORM 3000 mB |  |
| Subsonic EMP Cruise Missile<br>`wartecmod:itemcruisemissileemp@0` | Сборочная машина HBM / 2 (цепочка 3) | 1 шт.; 20.0 с; 500 | Subsonic Cruise Missile without Warhead ×1; EMP Warhead (CM) ×1; Guidance System Tier 3 ×1; Solid fuel rocket booster ×1 ; KEROSENE_REFORM 3000 mB |  |
| Item Cruise Missile TB<br>`wartecmod:itemcruisemissiletb@0` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | Subsonic Cruise Missile without Warhead ×1; Item Warhead TB ×1; Guidance System Tier 2 ×1; Solid fuel rocket booster ×1 ; KEROSENE_REFORM 3000 mB |  |
| Subsonic Nuclear Cruise Missile<br>`wartecmod:itemcruisemissilenuclear@0` | Сборочная машина HBM / 4 | 1 шт.; 80.0 с; 10000 | Subsonic Cruise Missile without Warhead ×1; Nuclear Warhead (CM) ×1; Guidance System Tier 5 ×1; Solid fuel rocket booster ×1 ; KEROSENE_REFORM 3000 mB |  |
| Subsonic Thermonuclear Cruise Missile<br>`wartecmod:itemcruisemissileh@0` | Сборочная машина HBM / 4 | 1 шт.; 80.0 с; 10000 | Subsonic Cruise Missile without Warhead ×1; Thermonuclear Warhead (CM) ×1; Guidance System Tier 6 ×1; Solid fuel rocket booster ×1 ; KEROSENE_REFORM 3000 mB |  |
| Supersonic HE Cruise Missile<br>`wartecmod:itemsupersoniccruisemissilehe@0` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | Сверхзвуковая двигательная секция ×1; HE Warhead (CM) ×1; Guidance System Tier 4 ×1; Solid fuel rocket booster ×1 ; KEROSENE_REFORM 3000 mB | Прямая сборка из незаряженной двигательной секции и выбранной БЧ; готовая чужая ракета не расходуется. Различия функциональные, не краситель/флаг. |
| Supersonic Thermonuclear Cruise Missile<br>`wartecmod:itemsupersoniccruisemissileh@0` | Сборочная машина HBM / 4 | 1 шт.; 80.0 с; 10000 | Сверхзвуковая двигательная секция ×1; Thermonuclear Warhead (CM) ×1; Guidance System Tier 6 ×1; Solid fuel rocket booster ×1 ; KEROSENE_REFORM 3000 mB | Прямая сборка из незаряженной двигательной секции и выбранной БЧ; готовая чужая ракета не расходуется. Различия функциональные, не краситель/флаг. |
| Hypersonic HE Cruise Missile<br>`wartecmod:itemhypersoniccruisemissilehe@0` | Сборочная машина HBM / 4 | 1 шт.; 80.0 с; 10000 | Гиперзвуковая двигательная секция ×1; HE Warhead (CM) ×1; Guidance System Tier 5 ×1; Solid fuel rocket booster ×1 ; KEROSENE_REFORM 3000 mB | Прямая сборка из незаряженной двигательной секции и выбранной БЧ; готовая чужая ракета не расходуется. Различия функциональные, не краситель/флаг. |
| Hypersonic Nuclear Cruise Missile<br>`wartecmod:itemhypersoniccruisemissilenuclear@0` | Сборочная машина HBM / 4 | 1 шт.; 80.0 с; 10000 | Гиперзвуковая двигательная секция ×1; Nuclear Warhead (CM) ×1; Guidance System Tier 6 ×1; Solid fuel rocket booster ×1 ; KEROSENE_REFORM 3000 mB | Прямая сборка из незаряженной двигательной секции и выбранной БЧ; готовая чужая ракета не расходуется. Различия функциональные, не краситель/флаг. |
| Long Range Hypersonic Weapon<br>`wartecmod:itemlrhwmissile@0` | Сборочная машина HBM / 4 | 1 шт.; 80.0 с; 10000 | Гиперзвуковая двигательная секция ×1; HE Warhead (CM) ×1; Guidance System Tier 6 ×1; Solid fuel rocket booster ×1; Ступень стратегической ракеты ×1; Solid fuel rocket booster ×4 ; KEROSENE_REFORM 3000 mB | Прямая сборка из незаряженной двигательной секции и выбранной БЧ; готовая чужая ракета не расходуется. Различия функциональные, не краситель/флаг. |
| R/UGM-109H Tomahawk Block Va<br>`wartecmod:itemtomahawkmissile@0` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | Subsonic Cruise Missile without Warhead ×1; HE Warhead (CM) ×1; Guidance System Tier 3 ×1; Solid fuel rocket booster ×1; Модуль связи 2 ×1 ; KEROSENE_REFORM 3000 mB | Прямая сборка из незаряженной двигательной секции и выбранной БЧ; готовая чужая ракета не расходуется. Различия функциональные, не краситель/флаг. |
| 3M14 Kalibr <br>`wartecmod:itemkalibrmissile@0` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | Subsonic Cruise Missile without Warhead ×1; HE Warhead (CM) ×1; Guidance System Tier 3 ×1; Solid fuel rocket booster ×1; plateSteel ×8 ; KEROSENE_REFORM 3000 mB | Прямая сборка из незаряженной двигательной секции и выбранной БЧ; готовая чужая ракета не расходуется. Различия функциональные, не краситель/флаг. |
| DongHai 10<br>`wartecmod:itemcj10missile@0` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | Subsonic Cruise Missile without Warhead ×1; HE Warhead (CM) ×1; Guidance System Tier 3 ×1; Solid fuel rocket booster ×1; Средний топливный бак ×1 ; KEROSENE_REFORM 6000 mB | Прямая сборка из незаряженной двигательной секции и выбранной БЧ; готовая чужая ракета не расходуется. Различия функциональные, не краситель/флаг. |
| Крылатая ракета Storm Shadow<br>`wartecmod:stormshadow@0` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | Subsonic Cruise Missile without Warhead ×1; Bunker Buster Warhead (CM) ×1; Guidance System Tier 5 ×1; Solid fuel rocket booster ×1; Тепловизионный модуль ×1; plateTitanium ×8 ; KEROSENE_REFORM 3000 mB | Прямая сборка из незаряженной двигательной секции и выбранной БЧ; готовая чужая ракета не расходуется. Различия функциональные, не краситель/флаг. |
| Противорадиолокационная ракета AGM-88 HARM<br>`wartecmod:antiradiationmissile@0` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | Subsonic Cruise Missile without Warhead ×1; HE Warhead (CM) ×1; Guidance System Tier 4 ×1; Solid fuel rocket booster ×1; Радиолокационный модуль 2 ×1 ; KEROSENE_REFORM 3000 mB | Прямая сборка из незаряженной двигательной секции и выбранной БЧ; готовая чужая ракета не расходуется. Различия функциональные, не краситель/флаг. |
| Крылатая ракета большой дальности Х-555<br>`wartecmod:kh555missile@0` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | Subsonic Cruise Missile without Warhead ×1; HE Warhead (CM) ×1; Guidance System Tier 5 ×1; Solid fuel rocket booster ×1; Модуль связи 3 ×1; Большой топливный бак ×1 ; KEROSENE_REFORM 6000 mB | Прямая сборка из незаряженной двигательной секции и выбранной БЧ; готовая чужая ракета не расходуется. Различия функциональные, не краситель/флаг. |
| Ракета FP-5 Flamingo<br>`wartecmod:fp5missile@0` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | Тяжёлый малозаметный корпус ×1; HE Warhead (CM) ×1; Guidance System Tier 5 ×1; Solid fuel rocket booster ×1; Тяжёлый экономичный двигатель ×1; Тяжёлая дальнобойная топливная секция ×1; Крылья увеличенной дальности ×1; HE Warhead (CM) ×2 | Прямая сборка из незаряженной двигательной секции и выбранной БЧ; готовая чужая ракета не расходуется. Различия функциональные, не краситель/флаг. |
| Ракета S8000 Banderol<br>`wartecmod:s8000missile@0` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | Корпус реактивной ракеты-дрона ×1; HE Warhead (CM) ×1; Guidance System Tier 3 ×1; Solid fuel rocket booster ×1; Экономичный маршевый двигатель ×1; Стандартная топливная секция ×1; Складные крылья ×1; Модуль связи 2 ×1 | Прямая сборка из незаряженной двигательной секции и выбранной БЧ; готовая чужая ракета не расходуется. Различия функциональные, не краситель/флаг. |
| Gas Micro Missile<br>`wartecmod:itemmissilemicrogas@0` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | Сборка малой ракеты ×1; Gas Warhead ×1; Guidance System Tier 2 ×1 |  |
| Neutron Micro Missile<br>`wartecmod:itemmissilemicroneutron@0` | Сборочная машина HBM / 4 | 1 шт.; 80.0 с; 10000 | Сборка малой ракеты ×1; Neutron Warhead ×1; Guidance System Tier 2 ×1 |  |

## Баллистические ракеты

| Предмет / ID и metadata | Станок / ступень | Выход; время; HE/тик | Состав | Особенности |
|---|---|---|---|---|
| 9K723 Iskander-M<br>`wartecmod:itemiskandermissile@0` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | Корпус тяжёлого реактивного снаряда ×2; Solid fuel rocket booster ×4; Guidance System Tier 5 ×1; HE Warhead (CM) ×1; Комплект сервоприводов ×2 |  |
| Ракета FP-7<br>`wartecmod:fp7missile@0` | Сборочная машина HBM / 2 (цепочка 3) | 1 шт.; 20.0 с; 500 | Корпус тяжёлого реактивного снаряда ×1; Solid fuel rocket booster ×2; Guidance System Tier 3 ×1; HE Warhead (CM) ×1; Комплект сервоприводов ×1 |  |
| Submarine launched Ballistic Missile<br>`wartecmod:itemmissileslbm@0` | Сборочная машина HBM / 4 | 1 шт.; 80.0 с; 10000 | Ступень стратегической ракеты ×2; Thermonuclear Warhead (SLBM) ×1; Guidance System Tier 6 ×1; plateTitanium ×16 |  |
| Ракета "Тополь-М"<br>`wartecmod:strategicmissile@0` | Сборочная машина HBM / 4 | 1 шт.; 180.0 с; 10000 | Ступень стратегической ракеты ×3; Thermonuclear Warhead (SLBM) ×1; Guidance System Tier 6 ×1; plateTitanium ×24; Блок питания ×2 |  |
| Ракета "Орешник"<br>`wartecmod:strategicmissile@2` | Сборочная машина HBM / 4 | 1 шт.; 160.0 с; 10000 | Ступень стратегической ракеты ×2; Платформа разведения боевых блоков ×1; Kinetic Kill Vehicle ×7; Guidance System Tier 6 ×1; plateTitanium ×24 | Семь реальных игровых блоков, включая центральный. Ядерная БЧ не используется. |

## Перехватчики

| Предмет / ID и metadata | Станок / ступень | Выход; время; HE/тик | Состав | Особенности |
|---|---|---|---|---|
| WTI-1 "Сокол", перехватчик малой дальности<br>`wartecmod:itemmissileantiairtier1@0` | Сборочная машина HBM / 1 (цепочка 2) | 1 шт.; 10.0 с; 100 | Корпус лёгкого реактивного снаряда ×1; Solid fuel rocket booster ×1; Guidance System Tier 2 ×1; Радиолокационный модуль 1 ×1; Простой неконтактный взрыватель ×1; Свинцовые гранулы ×4 |  |
| WTI-2 "Копьё", перехватчик средней дальности<br>`wartecmod:itemmissileantiairtier2@0` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | Корпус среднего реактивного снаряда ×1; Solid fuel rocket booster ×2; Guidance System Tier 3 ×1; Радиолокационный модуль 2 ×1; Взрыватель воздушного подрыва ×1; Свинцовые гранулы ×8 |  |
| WTI-3 "Страж", перехватчик большой дальности<br>`wartecmod:itemmissileantiairtier3@0` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | Корпус тяжёлого реактивного снаряда ×1; Solid fuel rocket booster ×3; Guidance System Tier 4 ×1; Радиолокационный модуль 3 ×1; Взрыватель воздушного подрыва ×1; Свинцовые гранулы ×12 |  |
| Item Missile Anti Ballistic Nuclear<br>`wartecmod:itemmissileantiballisticnuclear@0` | Сборочная машина HBM / 4 | 1 шт.; 80.0 с; 10000 | Ступень стратегической ракеты ×1; Nuclear Warhead (CM) ×1; Guidance System Tier 6 ×1; Радиолокационный модуль 3 ×1 | Старый ядерный перехватчик. Не превращается в новый Заслон-4. |
| Item Missile ASAT<br>`wartecmod:itemmissileasat@0` | Сборочная машина HBM / 4 | 1 шт.; 80.0 с; 10000 | Ступень стратегической ракеты ×1; Kinetic Kill Vehicle ×1; Guidance System Tier 6 ×1; Модуль связи 3 ×1 |  |
| ПРО "Заслон-4" — шахтный перехватчик<br>`wartecmod:silointerceptor@0` | Сборочная машина HBM / 4 | 1 шт.; 160.0 с; 10000 | Ступень стратегической ракеты ×2; Kinetic Kill Vehicle ×1; Система управления огнём 4 ×2; Радиолокационный модуль 3 ×1; plateTitanium ×24; Комплект сервоприводов ×4 | Новая ПРО IV: кинетический перехват, без ядерной/ударной БЧ. Не нужно расходовать готовый Орешник ради перекраски. |

## Заводские БПЛА

| Предмет / ID и metadata | Станок / ступень | Выход; время; HE/тик | Состав | Особенности |
|---|---|---|---|---|
| Ударный БПЛА Герань-2<br>`wartecmod:gerandrone@0` | Сборочная машина HBM / 1 | 1 шт.; 10.0 с; 100 | Одноразовый планер "летающее крыло" ×1; Экономичный поршневой двигатель ×1; Компактный топливный модуль ×1; Базовый контроллер полёта ×1; Радиоканал малой дальности ×1; Фугасная боевая часть ×1; plateAluminum ×2 |  |
| Реактивный ударный БПЛА Герань-5<br>`wartecmod:geran5drone@0` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | Одноразовый планер "летающее крыло" ×1; Экономичный маршевый двигатель ×1; Компактный топливный модуль ×1; Базовый контроллер полёта ×1; Радиоканал малой дальности ×1; Фугасная боевая часть ×2; plateAluminum ×6 |  |

## Ракеты РСЗО

| Предмет / ID и metadata | Станок / ступень | Выход; время; HE/тик | Состав | Особенности |
|---|---|---|---|---|
| Ракета РСЗО "Град" - фугасная<br>`wartecmod:gradrocket@0` | Сборочная машина HBM / 1 | 4 шт.; 10.0 с; 100 | Корпус лёгкого реактивного снаряда ×4; Твердое топливо (Ракетное) ×16; ingotAnyHighexplosive ×8; Микрочип ×4 | Выход по партии; стак остаётся 16. Фосфорная/кассетная/объёмная/осколочная начинка различается материалами. |
| Ракета РСЗО "Град" - фосфорная<br>`wartecmod:gradrocket@1` | Сборочная машина HBM / 2 | 4 шт.; 20.0 с; 500 | Корпус лёгкого реактивного снаряда ×4; Твердое топливо (Ракетное) ×16; Малая зажигательная боеголовка ×4; Микрочип ×4 | Выход по партии; стак остаётся 16. Фосфорная/кассетная/объёмная/осколочная начинка различается материалами. |
| Ракета РСЗО "Град" - кассетная<br>`wartecmod:gradrocket@2` | Сборочная машина HBM / 2 | 4 шт.; 20.0 с; 500 | Корпус лёгкого реактивного снаряда ×4; Твердое топливо (Ракетное) ×16; Взрывчатые гранулы ×16; Микрочип ×4; Взрыватель воздушного подрыва ×4 | Выход по партии; стак остаётся 16. Фосфорная/кассетная/объёмная/осколочная начинка различается материалами. |
| Ракета РСЗО "Град" - термобарическая<br>`wartecmod:gradrocket@3` | Сборочная машина HBM / 2 | 4 шт.; 20.0 с; 500 | Корпус лёгкого реактивного снаряда ×4; Твердое топливо (Ракетное) ×16; Item Warhead TB ×4; Микрочип ×4 | Выход по партии; стак остаётся 16. Фосфорная/кассетная/объёмная/осколочная начинка различается материалами. |
| Ракета РСЗО "Град" - осколочная<br>`wartecmod:gradrocket@4` | Сборочная машина HBM / 1 | 4 шт.; 10.0 с; 100 | Корпус лёгкого реактивного снаряда ×4; Твердое топливо (Ракетное) ×16; ingotAnyHighexplosive ×4; Свинцовые гранулы ×32; Микрочип ×4 | Выход по партии; стак остаётся 16. Фосфорная/кассетная/объёмная/осколочная начинка различается материалами. |
| Ракета РСЗО "Ураган" - фугасная<br>`wartecmod:uraganrocket@0` | Сборочная машина HBM / 2 | 2 шт.; 20.0 с; 500 | Корпус среднего реактивного снаряда ×2; Твердое топливо (Ракетное) ×16; ingotAnyHighexplosive ×4; Микрочип ×2 | Выход по партии; стак остаётся 16. Фосфорная/кассетная/объёмная/осколочная начинка различается материалами. |
| Ракета РСЗО "Ураган" - фосфорная<br>`wartecmod:uraganrocket@1` | Сборочная машина HBM / 2 | 2 шт.; 20.0 с; 500 | Корпус среднего реактивного снаряда ×2; Твердое топливо (Ракетное) ×16; Малая зажигательная боеголовка ×2; Микрочип ×2 | Выход по партии; стак остаётся 16. Фосфорная/кассетная/объёмная/осколочная начинка различается материалами. |
| Ракета РСЗО "Ураган" - кассетная<br>`wartecmod:uraganrocket@2` | Сборочная машина HBM / 2 | 2 шт.; 20.0 с; 500 | Корпус среднего реактивного снаряда ×2; Твердое топливо (Ракетное) ×16; Взрывчатые гранулы ×8; Микрочип ×2; Взрыватель воздушного подрыва ×2 | Выход по партии; стак остаётся 16. Фосфорная/кассетная/объёмная/осколочная начинка различается материалами. |
| Ракета РСЗО "Ураган" - термобарическая<br>`wartecmod:uraganrocket@3` | Сборочная машина HBM / 2 | 2 шт.; 20.0 с; 500 | Корпус среднего реактивного снаряда ×2; Твердое топливо (Ракетное) ×16; Item Warhead TB ×2; Микрочип ×2 | Выход по партии; стак остаётся 16. Фосфорная/кассетная/объёмная/осколочная начинка различается материалами. |
| Ракета РСЗО "Ураган" - осколочная<br>`wartecmod:uraganrocket@4` | Сборочная машина HBM / 2 | 2 шт.; 20.0 с; 500 | Корпус среднего реактивного снаряда ×2; Твердое топливо (Ракетное) ×16; ingotAnyHighexplosive ×2; Свинцовые гранулы ×16; Микрочип ×2 | Выход по партии; стак остаётся 16. Фосфорная/кассетная/объёмная/осколочная начинка различается материалами. |
| Ракета РСЗО "Смерч" - фугасная<br>`wartecmod:smerchrocket@0` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | Корпус тяжёлого реактивного снаряда ×1; Твердое топливо (Ракетное) ×12; ingotAnyHighexplosive ×2; Микрочип ×1 | Выход по партии; стак остаётся 16. Фосфорная/кассетная/объёмная/осколочная начинка различается материалами. |
| Ракета РСЗО "Смерч" - фосфорная<br>`wartecmod:smerchrocket@1` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | Корпус тяжёлого реактивного снаряда ×1; Твердое топливо (Ракетное) ×12; Малая зажигательная боеголовка ×1; Микрочип ×1 | Выход по партии; стак остаётся 16. Фосфорная/кассетная/объёмная/осколочная начинка различается материалами. |
| Ракета РСЗО "Смерч" - кассетная<br>`wartecmod:smerchrocket@2` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | Корпус тяжёлого реактивного снаряда ×1; Твердое топливо (Ракетное) ×12; Взрывчатые гранулы ×4; Микрочип ×1; Взрыватель воздушного подрыва ×1 | Выход по партии; стак остаётся 16. Фосфорная/кассетная/объёмная/осколочная начинка различается материалами. |
| Ракета РСЗО "Смерч" - термобарическая<br>`wartecmod:smerchrocket@3` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | Корпус тяжёлого реактивного снаряда ×1; Твердое топливо (Ракетное) ×12; Item Warhead TB ×1; Микрочип ×1 | Выход по партии; стак остаётся 16. Фосфорная/кассетная/объёмная/осколочная начинка различается материалами. |
| Ракета РСЗО "Смерч" - осколочная<br>`wartecmod:smerchrocket@4` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | Корпус тяжёлого реактивного снаряда ×1; Твердое топливо (Ракетное) ×12; ingotAnyHighexplosive ×1; Свинцовые гранулы ×8; Микрочип ×1 | Выход по партии; стак остаётся 16. Фосфорная/кассетная/объёмная/осколочная начинка различается материалами. |
| Ракета HIMARS - фугасная<br>`wartecmod:himarsrocket@0` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | Корпус среднего реактивного снаряда ×1; Твердое топливо (Ракетное) ×8; ingotAnyHighexplosive ×2; Микрочип ×1; Guidance System Tier 3 ×1 | Выход по партии; стак остаётся 16. Фосфорная/кассетная/объёмная/осколочная начинка различается материалами. |
| Ракета HIMARS - фосфорная<br>`wartecmod:himarsrocket@1` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | Корпус среднего реактивного снаряда ×1; Твердое топливо (Ракетное) ×8; Малая зажигательная боеголовка ×1; Микрочип ×1; Guidance System Tier 3 ×1 | Выход по партии; стак остаётся 16. Фосфорная/кассетная/объёмная/осколочная начинка различается материалами. |
| Ракета HIMARS - кассетная<br>`wartecmod:himarsrocket@2` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | Корпус среднего реактивного снаряда ×1; Твердое топливо (Ракетное) ×8; Взрывчатые гранулы ×4; Микрочип ×1; Guidance System Tier 3 ×1; Взрыватель воздушного подрыва ×1 | Выход по партии; стак остаётся 16. Фосфорная/кассетная/объёмная/осколочная начинка различается материалами. |
| Ракета HIMARS - термобарическая<br>`wartecmod:himarsrocket@3` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | Корпус среднего реактивного снаряда ×1; Твердое топливо (Ракетное) ×8; Item Warhead TB ×1; Микрочип ×1; Guidance System Tier 3 ×1 | Выход по партии; стак остаётся 16. Фосфорная/кассетная/объёмная/осколочная начинка различается материалами. |
| Ракета HIMARS - осколочная<br>`wartecmod:himarsrocket@4` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | Корпус среднего реактивного снаряда ×1; Твердое топливо (Ракетное) ×8; ingotAnyHighexplosive ×1; Свинцовые гранулы ×8; Микрочип ×1; Guidance System Tier 3 ×1 | Выход по партии; стак остаётся 16. Фосфорная/кассетная/объёмная/осколочная начинка различается материалами. |

## Авиационные боеприпасы

| Предмет / ID и metadata | Станок / ступень | Выход; время; HE/тик | Состав | Особенности |
|---|---|---|---|---|
| Управляемая ракета AGM-114 Hellfire<br>`wartecmod:mq9payload@0` | Сборочная машина HBM / 1 (цепочка 2) | 1 шт.; 10.0 с; 100 | Корпус лёгкого реактивного снаряда ×1; Малая боеголовка ×1; Оптический модуль ×1; Solid fuel rocket booster ×1 |  |
| Управляемая авиабомба GBU-12 Paveway II<br>`wartecmod:mq9payload@1` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | Средняя боеголовка ×1; Оптический модуль ×1; Cruise Fins big ×1; Guidance System Tier 2 ×1 |  |
| Фугасная авиабомба Mk 82<br>`wartecmod:mq9payload@2` | Сборочная машина HBM / 1 | 1 шт.; 10.0 с; 100 | Средняя боеголовка ×1; Cruise Fins small ×1 |  |
| Лёгкая управляемая ракета HJ-10<br>`wartecmod:mq9payload@3` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | Корпус лёгкого реактивного снаряда ×1; Малая боеголовка ×1; Оптический модуль ×1; Guidance System Tier 2 ×1; Solid fuel rocket booster ×1 |  |
| Тактическая ракета AGM-65 Maverick<br>`wartecmod:mq9payload@4` | Сборочная машина HBM / 2 (цепочка 3) | 1 шт.; 20.0 с; 500 | Корпус среднего реактивного снаряда ×1; Средняя боеголовка ×1; Тепловизионный модуль ×1; Solid fuel rocket booster ×1 |  |
| Тяжёлая ударная ракета Х-29<br>`wartecmod:mq9payload@5` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | Корпус тяжёлого реактивного снаряда ×1; Большая боеголовка ×1; Оптический модуль ×1; Solid fuel rocket booster ×2 |  |
| Управляемая авиабомба КАБ-500Л<br>`wartecmod:mq9payload@6` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | Средняя боеголовка ×1; Оптический модуль ×1; Cruise Fins big ×1; Guidance System Tier 3 ×1 |  |
| Планирующая авиабомба JDAM<br>`wartecmod:mq9payload@7` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | Средняя боеголовка ×1; Guidance System Tier 4 ×1; Модуль связи 2 ×1; Cruise Fins big ×1 |  |
| Ракета воздух-воздух WT-AAM Skyguard<br>`wartecmod:mq9payload@8` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | Корпус среднего реактивного снаряда ×1; Радиолокационный модуль 2 ×1; Guidance System Tier 4 ×1; Solid fuel rocket booster ×2; Взрыватель воздушного подрыва ×1 |  |
| Кассета ЛТЦ для MQ-9<br>`wartecmod:mq9flares@0` | Сборочная машина HBM / 1 | 8 шт.; 10.0 с; 100 | plateAluminum ×2; dustAluminum ×2; Твердое топливо (Ракетное) ×1 | 8 отдельных ловушек; изготовление не восстанавливает бесплатно уже использованные ловушки. |
| ФАБ-5000, тяжёлая неуправляемая авиабомба<br>`wartecmod:strategicbomb@0` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | Большая боеголовка ×4; plateSteel ×24; Cruise Fins big ×1 |  |
| КАБ-3000, тяжёлая управляемая авиабомба<br>`wartecmod:strategicbomb@1` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | Большая боеголовка ×3; plateSteel ×20; Оптический модуль ×1; Guidance System Tier 4 ×1; Комплект сервоприводов ×2 |  |

## Артиллерийские боеприпасы

| Предмет / ID и metadata | Станок / ступень | Выход; время; HE/тик | Состав | Особенности |
|---|---|---|---|---|
| 16-дюймовый артиллерийский снаряд<br>`wartecmod:artilleryammo@0` | Сборочная машина HBM / 1 | 1 шт.; 10.0 с; 100 | Пустой артиллерийский корпус ×1; Кордит ×2; ingotAnyHighexplosive ×2; Свинцовые гранулы ×2 |  |
| 16-дюймовый артиллерийский снаряд (Прямо из Факторио)<br>`wartecmod:artilleryammo@1` | Сборочная машина HBM / 1 | 1 шт.; 10.0 с; 100 | Пустой артиллерийский корпус ×1; Кордит ×2; ingotAnyHighexplosive ×3; Свинцовые гранулы ×4 |  |
| Фугасный 16-дюймовый артиллерийский снаряд<br>`wartecmod:artilleryammo@2` | Сборочная машина HBM / 1 | 1 шт.; 10.0 с; 100 | Пустой артиллерийский корпус ×1; Кордит ×2; ingotAnyHighexplosive ×4; Свинцовые гранулы ×2 |  |
| Мини-ядерный 16-дюймовый артиллерийский снаряд<br>`wartecmod:artilleryammo@3` | Сборочная машина HBM / 4 | 1 шт.; 80.0 с; 10000 | 16-дюймовый артиллерийский снаряд ×1; plateSteel ×2 |  |
| Ядерный 16-дюймовый артиллерийский снаряд<br>`wartecmod:artilleryammo@4` | Сборочная машина HBM / 4 | 1 шт.; 80.0 с; 10000 | 16-дюймовый артиллерийский снаряд ×1; plateSteel ×2 |  |
| Фосфорный 16-дюймовый артиллерийский снаряд<br>`wartecmod:artilleryammo@5` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | Пустой артиллерийский корпус ×1; Кордит ×2; Малая зажигательная боеголовка ×1 |  |
| Мини-ядерный 16-дюймовый мульти-снаряд<br>`wartecmod:artilleryammo@6` | Сборочная машина HBM / 4 | 1 шт.; 80.0 с; 10000 | 16-дюймовый артиллерийский снаряд ×1; plateSteel ×2 | Количество разделяемых элементов оплачено входами. |
| Фосфорный 16-дюймовый мульти-снаряд<br>`wartecmod:artilleryammo@7` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | Пустой артиллерийский корпус ×1; Кордит ×2; Малая зажигательная боеголовка ×10 | Количество разделяемых элементов оплачено входами. |
| 16-дюймовый артиллерийский снаряд для экспресс-доставки<br>`wartecmod:artilleryammo@8` | Сборочная машина HBM / 1 | 1 шт.; 10.0 с; 100 | Пустой артиллерийский корпус ×1; Кордит ×2; ingotAnyPlastic ×1; minecraft:chest@0 ×1 | Пустой грузовой снаряд; загрузка настоящего груза отдельно, содержимое NBT нельзя выдумать. |
| Хлорный 16-дюймовый артиллерийский снаряд<br>`wartecmod:artilleryammo@9` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | 16-дюймовый артиллерийский снаряд ×1; plateSteel ×2 | Используется готовый специальный снаряд HBM соответствующего типа; без обхода химии. |
| Фосгеновый 16-дюймовый артиллерийский снаряд<br>`wartecmod:artilleryammo@10` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | 16-дюймовый артиллерийский снаряд ×1; plateSteel ×2 | Используется готовый специальный снаряд HBM соответствующего типа; без обхода химии. |
| 16-дюймовый артиллерийский снаряд с ипритом<br>`wartecmod:artilleryammo@11` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | 16-дюймовый артиллерийский снаряд ×1; plateSteel ×2 | Используется готовый специальный снаряд HBM соответствующего типа; без обхода химии. |
| 227-мм управляемый артиллерийский ракетный снаряд<br>`wartecmod:himarsammo@0` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | hbm:ammo_himars@0 ×1; plateSteel ×2 | Старый контейнер Henry: сохраняет шесть/одну ракету HBM. Не путать с одиночной новой ракетой HIMARS; из частично использованного пакета нельзя получать полный. |
| 610-мм управляемый артиллерийский ракетный снаряд<br>`wartecmod:himarsammo@1` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | hbm:ammo_himars@1 ×1; plateSteel ×2 | Старый контейнер Henry: сохраняет шесть/одну ракету HBM. Не путать с одиночной новой ракетой HIMARS; из частично использованного пакета нельзя получать полный. |
| 227-мм управляемый артиллерийский ракетный снаряд (Фугасный)<br>`wartecmod:himarsammo@2` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | hbm:ammo_himars@2 ×1; plateSteel ×2 | Старый контейнер Henry: сохраняет шесть/одну ракету HBM. Не путать с одиночной новой ракетой HIMARS; из частично использованного пакета нельзя получать полный. |
| 227-мм управляемый артиллерийский ракетный снаряд (БФ)<br>`wartecmod:himarsammo@3` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | hbm:ammo_himars@3 ×1; plateSteel ×2 | Старый контейнер Henry: сохраняет шесть/одну ракету HBM. Не путать с одиночной новой ракетой HIMARS; из частично использованного пакета нельзя получать полный. |
| 227-мм управляемый артиллерийский ракетный снаряд (Термобарический)<br>`wartecmod:himarsammo@4` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | hbm:ammo_himars@4 ×1; plateSteel ×2 | Старый контейнер Henry: сохраняет шесть/одну ракету HBM. Не путать с одиночной новой ракетой HIMARS; из частично использованного пакета нельзя получать полный. |
| 610-мм управляемый артиллерийский ракетный снаряд (Термобарический)<br>`wartecmod:himarsammo@5` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | hbm:ammo_himars@5 ×1; plateSteel ×2 | Старый контейнер Henry: сохраняет шесть/одну ракету HBM. Не путать с одиночной новой ракетой HIMARS; из частично использованного пакета нельзя получать полный. |
| 227-мм управляемый артиллерийский ракетный снаряд (Мини-ядерный)<br>`wartecmod:himarsammo@6` | Сборочная машина HBM / 4 | 1 шт.; 80.0 с; 10000 | hbm:ammo_himars@6 ×1; plateSteel ×2 | Старый контейнер Henry: сохраняет шесть/одну ракету HBM. Не путать с одиночной новой ракетой HIMARS; из частично использованного пакета нельзя получать полный. |
| 227-мм управляемый артиллерийский ракетный снаряд (Лава)<br>`wartecmod:himarsammo@7` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | hbm:ammo_himars@7 ×1; plateSteel ×2 | Старый контейнер Henry: сохраняет шесть/одну ракету HBM. Не путать с одиночной новой ракетой HIMARS; из частично использованного пакета нельзя получать полный. |
| 30-мм боекомплект ЗРПК "Панцирь-С2"<br>`wartecmod:pantsir30mmbelt@0` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | Упаковка 100 патронов 30 мм ×6; plateSteel ×4; ingotAnyRubber ×2 | Лента 600/600. Пополнение старой ленты — отдельная NBT-aware операция за фактические недостающие патроны. |

## Техника

| Предмет / ID и metadata | Станок / ступень | Выход; время; HE/тик | Состав | Особенности |
|---|---|---|---|---|
| Тяжёлая платформа HEMTT<br>`wartecmod:mobileartillery@0` | Сборочная машина HBM / 1 (цепочка 2) | 1 шт.; 40.0 с; 100 | Среднее колёсное шасси ×1; Кабина с органами управления ×1; Блок питания ×1; Модуль связи 1 ×1 | Без припасов и бесплатного заряда. Если native размещение сейчас даёт запас энергии, нужно согласовать нулевой заводской заряд или включить его стоимость отдельно. |
| Артиллерийская установка Greg на HEMTT<br>`wartecmod:mobileartillery@1` | Сборочная машина HBM / 2 | 1 шт.; 60.0 с; 500 | Тяжёлая платформа HEMTT ×1; Артиллерийский узел ×1; Система управления огнём 1 ×1 | Без припасов и бесплатного заряда. Если native размещение сейчас даёт запас энергии, нужно согласовать нулевой заводской заряд или включить его стоимость отдельно. |
| Ракетная установка Henry на HEMTT<br>`wartecmod:mobileartillery@2` | Сборочная машина HBM / 2 | 1 шт.; 60.0 с; 500 | Тяжёлая платформа HEMTT ×1; Средний пакет направляющих ×1; Система управления огнём 2 ×1 | Без припасов и бесплатного заряда. Если native размещение сейчас даёт запас энергии, нужно согласовать нулевой заводской заряд или включить его стоимость отдельно. |
| Град<br>`wartecmod:grad@0` | Сборочная машина HBM / 1 (цепочка 2) | 1 шт.; 40.0 с; 100 | Среднее колёсное шасси ×1; Кабина с органами управления ×1; Блок питания ×1; Малый пакет направляющих ×1; Система управления огнём 1 ×1 | Без припасов и бесплатного заряда. Если native размещение сейчас даёт запас энергии, нужно согласовать нулевой заводской заряд или включить его стоимость отдельно. |
| Ураган<br>`wartecmod:uragan@0` | Сборочная машина HBM / 2 (цепочка 3) | 1 шт.; 60.0 с; 500 | Тяжёлое колёсное шасси ×1; Кабина с органами управления ×1; Блок питания ×1; Средний пакет направляющих ×1; Система управления огнём 2 ×1 | Без припасов и бесплатного заряда. Если native размещение сейчас даёт запас энергии, нужно согласовать нулевой заводской заряд или включить его стоимость отдельно. |
| Смерч<br>`wartecmod:smerch@0` | Сборочная машина HBM / 3 | 1 шт.; 100.0 с; 2000 | Тяжёлое колёсное шасси ×1; Кабина с органами управления ×1; Блок питания ×2; Тяжёлый пакет направляющих ×1; Система управления огнём 3 ×1 | Без припасов и бесплатного заряда. Если native размещение сейчас даёт запас энергии, нужно согласовать нулевой заводской заряд или включить его стоимость отдельно. |
| HIMARS<br>`wartecmod:himars@0` | Сборочная машина HBM / 3 | 1 шт.; 100.0 с; 2000 | Среднее колёсное шасси ×1; Кабина с органами управления ×1; Блок питания ×1; Средний пакет направляющих ×1; Система управления огнём 3 ×1; Модуль связи 2 ×1 | Без припасов и бесплатного заряда. Если native размещение сейчас даёт запас энергии, нужно согласовать нулевой заводской заряд или включить его стоимость отдельно. |
| Оса<br>`wartecmod:osa@0` | Сборочная машина HBM / 1 (цепочка 2) | 1 шт.; 40.0 с; 100 | Среднее колёсное шасси ×1; Кабина с органами управления ×1; Блок питания ×1; Малый пакет направляющих ×1; Радиолокационный модуль 1 ×1; Система управления огнём 1 ×1 | Без припасов и бесплатного заряда. Если native размещение сейчас даёт запас энергии, нужно согласовать нулевой заводской заряд или включить его стоимость отдельно. |
| Бук-М3<br>`wartecmod:bukm3@0` | Сборочная машина HBM / 2 | 1 шт.; 60.0 с; 500 | Гусеничное шасси ×1; Кабина с органами управления ×1; Блок питания ×2; Средний пакет направляющих ×1; Радиолокационный модуль 2 ×1; Система управления огнём 2 ×1 | Без припасов и бесплатного заряда. Если native размещение сейчас даёт запас энергии, нужно согласовать нулевой заводской заряд или включить его стоимость отдельно. |
| Мобильный ЗРК 9К331 Тор-М1<br>`wartecmod:mobileairdefensesystem@0` | Сборочная машина HBM / 2 | 1 шт.; 60.0 с; 500 | Гусеничное шасси ×1; Кабина с органами управления ×1; Блок питания ×2; Средний пакет направляющих ×1; Радиолокационный модуль 2 ×1; Система управления огнём 2 ×1 | Без припасов и бесплатного заряда. Если native размещение сейчас даёт запас энергии, нужно согласовать нулевой заводской заряд или включить его стоимость отдельно. |
| ЗРПК 96К6 Панцирь-С2<br>`wartecmod:mobileairdefensesystem@1` | Сборочная машина HBM / 2 (цепочка 3) | 1 шт.; 60.0 с; 500 | Тяжёлое колёсное шасси ×1; Кабина с органами управления ×1; Блок питания ×2; Средний пакет направляющих ×1; Радиолокационный модуль 2 ×1; Система управления огнём 2 ×1; Спаренный 30-мм узел ×1 | Без припасов и бесплатного заряда. Если native размещение сейчас даёт запас энергии, нужно согласовать нулевой заводской заряд или включить его стоимость отдельно. |
| Радар 9С18<br>`wartecmod:radar9s18@0` | Сборочная машина HBM / 2 | 1 шт.; 60.0 с; 500 | Гусеничное шасси ×1; Кабина с органами управления ×1; Блок питания ×2; Радиолокационный модуль 2 ×2; Модуль связи 2 ×1; Комплект сервоприводов ×2 | Без припасов и бесплатного заряда. Если native размещение сейчас даёт запас энергии, нужно согласовать нулевой заводской заряд или включить его стоимость отдельно. |
| Мобильная РЛС Renault TRM<br>`wartecmod:mobileradartruck@0` | Сборочная машина HBM / 1 (цепочка 2) | 1 шт.; 40.0 с; 100 | Среднее колёсное шасси ×1; Кабина с органами управления ×1; Блок питания ×1; Радиолокационный модуль 1 ×1; Модуль связи 1 ×1 | Без припасов и бесплатного заряда. Если native размещение сейчас даёт запас энергии, нужно согласовать нулевой заводской заряд или включить его стоимость отдельно. |
| РЛС дальнего обнаружения С-400<br>`wartecmod:s400longrangeradar@0` | Сборочная машина HBM / 3 | 1 шт.; 100.0 с; 2000 | Тяжёлое колёсное шасси ×1; Кабина с органами управления ×1; Блок питания ×3; Радиолокационный модуль 3 ×2; Модуль связи 3 ×1; Комплект сервоприводов ×3 | Без припасов и бесплатного заряда. Если native размещение сейчас даёт запас энергии, нужно согласовать нулевой заводской заряд или включить его стоимость отдельно. |
| Командный пункт ПВО на базе Урала<br>`wartecmod:airdefensecommandtruck@0` | Сборочная машина HBM / 3 | 1 шт.; 100.0 с; 2000 | Среднее колёсное шасси ×1; Кабина с органами управления ×1; Блок питания ×2; Терминал планирования ×1; Модуль связи 3 ×2 | Без припасов и бесплатного заряда. Если native размещение сейчас даёт запас энергии, нужно согласовать нулевой заводской заряд или включить его стоимость отдельно. |
| Мобильная станция РЭБ Синица<br>`wartecmod:electronicwarfareunit@0` | Сборочная машина HBM / 3 | 1 шт.; 100.0 с; 2000 | Лёгкое колёсное шасси ×1; Кабина с органами управления ×1; Блок питания ×1; Магнетрон ×4; Плата обработки III ×1; Модуль связи 2 ×1 | Без припасов и бесплатного заряда. Если native размещение сейчас даёт запас энергии, нужно согласовать нулевой заводской заряд или включить его стоимость отдельно. |
| Пассивная станция радиотехнической разведки<br>`wartecmod:electronicwarfareunit@1` | Сборочная машина HBM / 2 | 1 шт.; 60.0 с; 500 | Лёгкое колёсное шасси ×1; Кабина с органами управления ×1; Блок питания ×1; Радиолокационный модуль 2 ×1; Военная плата II ×1; Модуль связи 2 ×1 | Без припасов и бесплатного заряда. Если native размещение сейчас даёт запас энергии, нужно согласовать нулевой заводской заряд или включить его стоимость отдельно. |
| Развёртываемый ложный излучатель РЛС<br>`wartecmod:electronicwarfareunit@2` | Сборочная машина HBM / 2 | 1 шт.; 60.0 с; 500 | Лёгкое колёсное шасси ×1; Кабина с органами управления ×1; Блок питания ×1; Магнетрон ×2; Военная плата II ×1; Модуль связи 1 ×1 | Без припасов и бесплатного заряда. Если native размещение сейчас даёт запас энергии, нужно согласовать нулевой заводской заряд или включить его стоимость отдельно. |
| Мобильная пусковая "Искандер-М"<br>`wartecmod:iskandertel@0` | Сборочная машина HBM / 3 | 1 шт.; 100.0 с; 2000 | Тяжёлое колёсное шасси ×1; Кабина с органами управления ×1; Блок питания ×2; Тяжёлый пакет направляющих ×1; Система управления огнём 3 ×1; Гидравлический поршень ×4 | Без припасов и бесплатного заряда. Если native размещение сейчас даёт запас энергии, нужно согласовать нулевой заводской заряд или включить его стоимость отдельно. |
| ПГРК "Тополь-М"<br>`wartecmod:topolmtel@0` | Сборочная машина HBM / 4 | 1 шт.; 160.0 с; 10000 | Тяжёлое колёсное шасси ×2; Кабина с органами управления ×1; Блок питания ×3; Тяжёлый пакет направляющих ×1; Система управления огнём 4 ×1; Гидравлический поршень ×8 | Без припасов и бесплатного заряда. Если native размещение сейчас даёт запас энергии, нужно согласовать нулевой заводской заряд или включить его стоимость отдельно. |
| ПГРК "Орешник"<br>`wartecmod:oreshniktel@0` | Сборочная машина HBM / 4 | 1 шт.; 160.0 с; 10000 | Тяжёлое колёсное шасси ×2; Кабина с органами управления ×1; Блок питания ×3; Тяжёлый пакет направляющих ×1; Система управления огнём 4 ×1; Модуль связи 3 ×1; Гидравлический поршень ×6 | Без припасов и бесплатного заряда. Если native размещение сейчас даёт запас энергии, нужно согласовать нулевой заводской заряд или включить его стоимость отдельно. |
| Ударный БПЛА MQ-9 Reaper<br>`wartecmod:mq9reaperdrone@0` | Сборочная машина HBM / 2 (цепочка 3) | 1 шт.; 60.0 с; 500 | Планер заводского БПЛА ×1; Тяжёлый турбовинтовой двигатель ×1; Топливный бак большой дальности ×1; Точный автопилот ×1; Защищённый тактический канал ×1; Оптико-тепловизионная станция ×1; Тяжёлая четырёхточечная подвеска ×1 | Без припасов и бесплатного заряда. Если native размещение сейчас даёт запас энергии, нужно согласовать нулевой заводской заряд или включить его стоимость отдельно. |
| Тактический ударный самолёт F-16C<br>`wartecmod:tacticalaircraft@0` | Сборочная машина HBM / 3 | 1 шт.; 100.0 с; 2000 | Планер тактического самолёта ×1; Turbofan Engine Tier 1 ×1; Система управления огнём 3 ×1; Радиолокационный модуль 2 ×1; Модуль связи 2 ×1; Большой топливный бак ×2 | Без припасов и бесплатного заряда. Если native размещение сейчас даёт запас энергии, нужно согласовать нулевой заводской заряд или включить его стоимость отдельно. |
| Тактический ударный самолёт Су-27<br>`wartecmod:su27tacticalaircraft@0` | Сборочная машина HBM / 3 | 1 шт.; 100.0 с; 2000 | Планер тактического самолёта ×1; Turbofan Engine Tier 1 ×2; Система управления огнём 3 ×1; Радиолокационный модуль 2 ×1; Модуль связи 2 ×1; Большой топливный бак ×3 | Без припасов и бесплатного заряда. Если native размещение сейчас даёт запас энергии, нужно согласовать нулевой заводской заряд или включить его стоимость отдельно. |
| Стратегический ракетоносец Ту-95<br>`wartecmod:tu95strategicbomber@0` | Сборочная машина HBM / 4 | 1 шт.; 160.0 с; 10000 | Планер тяжёлого бомбардировщика ×1; Turbofan Engine Tier 1 ×4; Система управления огнём 3 ×1; Радиолокационный модуль 2 ×1; Модуль связи 3 ×1; Большой топливный бак ×6 | Без припасов и бесплатного заряда. Если native размещение сейчас даёт запас энергии, нужно согласовать нулевой заводской заряд или включить его стоимость отдельно. |

## Блоки и управление

| Предмет / ID и metadata | Станок / ступень | Выход; время; HE/тик | Состав | Особенности |
|---|---|---|---|---|
| Vertical Launch System<br>`wartecmod:launchtube@0` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | ntmpipeSteel ×12; plateSteel ×12; Система управления огнём 1 ×1; Блок питания ×1; Комплект сервоприводов ×1 |  |
| VLS Exhaust<br>`wartecmod:vlsexhaust@0` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | plateSteel ×24; plateDuraSteel ×8; Средний пакет направляющих ×1; Система управления огнём 2 ×1; Блок питания ×1 |  |
| Ballistic Missile Launcher<br>`wartecmod:ballisticmissilelauncher@0` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | plateDuraSteel ×24; Тяжёлый пакет направляющих ×1; Система управления огнём 3 ×1; Блок питания ×2; Гидравлический поршень ×4 |  |
| Рельсовая катапульта для БПЛА Герань<br>`wartecmod:geranlauncher@0` | Сборочная машина HBM / 1 | 1 шт.; 10.0 с; 100 | plateSteel ×12; plateAluminum ×8; Модуль связи 1 ×1; Плата управления I ×1; Мотор ×1 |  |
| Пусковая опора крылатой ракеты<br>`wartecmod:cruiselaunchpoint@0` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | plateSteel ×12; Система управления огнём 1 ×1; Комплект сервоприводов ×1; Блок питания ×1 |  |
| Направляющая ракеты-дрона<br>`wartecmod:cruisedronerail@0` | Сборочная машина HBM / 1 | 1 шт.; 10.0 с; 100 | plateSteel ×8; ingotAnyRubber ×4; Модуль связи 1 ×1; Мотор ×1 |  |
| Пусковая точка БПЛА<br>`wartecmod:uavlaunchpoint@0` | Сборочная машина HBM / 1 | 1 шт.; 10.0 с; 100 | plateSteel ×8; Модуль связи 1 ×1; Плата управления I ×1; Мотор ×1 |  |
| ЗРК MIM-104 Patriot<br>`wartecmod:patriotlauncher@0` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | Прицепная рама ×1; Тяжёлый пакет направляющих ×1; Система управления огнём 3 ×1; Блок питания ×2; Модуль связи 3 ×1; plateDuraSteel ×16 | Пусковая без ракет; внешняя РЛС требуется как прежде. |
| ЗРК С-400 Триумф<br>`wartecmod:s400launcher@0` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | Прицепная рама ×1; Тяжёлый пакет направляющих ×1; Система управления огнём 3 ×1; Блок питания ×2; Модуль связи 3 ×1; plateTitanium ×16 | Пусковая без ракет; внешняя РЛС требуется как прежде. |
| Реле сирены воздушной тревоги<br>`wartecmod:airraidsirenrelay@0` | Сборочная машина HBM / 1 | 1 шт.; 10.0 с; 100 | plateSteel ×6; Сирена ×1; Модуль связи 1 ×1; Плата управления I ×1 |  |
| Long Range Communication Mast<br>`wartecmod:longrangecommunicationmast@0` | Сборочная машина HBM / 2 (цепочка 3) | 1 шт.; 20.0 с; 500 | plateSteel ×8; Секция несущей мачты ×6; wireFineCopper ×16; Модуль связи 3 ×1; Блок питания ×2; Военная плата II ×1 |  |
| Strategic Early Warning Radar<br>`wartecmod:strategicearlywarningradar@0` | Сборочная машина HBM / 4 | 1 шт.; 80.0 с; 10000 | plateDuraSteel ×64; plateAluminum ×64; Радиолокационный модуль 3 ×4; Блок питания ×4; Модуль связи 3 ×1; Плата ПРО/стратегии IV ×1; Комплект сервоприводов ×4; Комплект 64 панелей РЛС ×57; Панель корпуса стратегической РЛС ×24 |  |
| Конструктор БПЛА<br>`wartecmod:uavfabricator@0` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | plateSteel ×24; plateAluminum ×8; Мотор ×4; Комплект сервоприводов ×2; Военная плата II ×1; Блок питания ×1 |  |
| Конструктор крылатых ракет<br>`wartecmod:cruisefabricator@0` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | plateDuraSteel ×24; plateTitanium ×8; Мотор ×4; Комплект сервоприводов ×4; Плата обработки III ×1; Блок питания ×2 |  |
| Терминал планирования<br>`wartecmod:planningterminal@0` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | plateSteel ×12; Экран радара ×2; Плата обработки III ×2; Модуль связи 3 ×1; Передовая микросхема ×4; Блок питания ×1 |  |
| Программатор задач БПЛА<br>`wartecmod:uavmissionstation@0` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | plateSteel ×8; Экран радара ×1; Военная плата II ×1; Модуль связи 2 ×1; Блок питания ×1 |  |
| Шахтный стартовый блок<br>`wartecmod:silostart@0` | Сборочная машина HBM / 4 | 1 шт.; 80.0 с; 10000 | plateDuraSteel ×32; plateTitanium ×16; Гидравлический поршень ×4; Система управления огнём 4 ×1; Блок питания ×2 |  |
| Шахтный настил<br>`wartecmod:silodeck@0` | Сборочная машина HBM / 1 | 4 шт.; 10.0 с; 100 | plateSteel ×4; boltSteel ×2 |  |
| Шахтный светильник<br>`wartecmod:silolight@0` | Сборочная машина HBM / 1 | 4 шт.; 10.0 с; 100 | plateSteel ×2; minecraft:glowstone_dust@0 ×2; minecraft:glass_pane@0 ×2; Микрочип ×1 |  |
| Ракетная шахта — строительный модуль<br>`wartecmod:missilesilo@0` | Сборочная машина HBM / 4 | 1 шт.; 80.0 с; 10000 | Полный строительный комплект шахты ×1; plateSteel ×8; Плата ПРО/стратегии IV ×1 | Предмет включает полностью оплаченный стройкомплект. Первый монтаж расходует одноразовый маркер комплекта; обновление существующей шахты повторно не списывает базовые блоки и не даёт новый комплект. Ракеты покупаются отдельно. |

## Инструменты

| Предмет / ID и metadata | Станок / ступень | Выход; время; HE/тик | Состав | Особенности |
|---|---|---|---|---|
| Target selector<br>`wartecmod:itemtargetfinder@0` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | Оптический модуль ×1; Модуль связи 1 ×1; Плата управления I ×1; plateAluminum ×2 |  |
| Артиллерийский целеуказатель<br>`wartecmod:designator_arty_range@0` | Сборочная машина HBM / 2 | 1 шт.; 20.0 с; 500 | Оптический модуль ×1; Модуль связи 2 ×1; Военная плата II ×1; plateAluminum ×4 |  |
| VLS-demonstrating-tool<br>`wartecmod:itemmissilestrikecaller@0` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | Модуль связи 3 ×1; Плата обработки III ×1; Атомные часы ×1; plateAluminum ×4 |  |
| IFF-конфигуратор WarTech<br>`wartecmod:wartechiffconfigurator@0` | Сборочная машина HBM / 1 | 1 шт.; 10.0 с; 100 | Модуль связи 1 ×1; Плата управления I ×1; ingotAnyPlastic ×2 |  |
| Демонтажный ключ WarTech<br>`wartecmod:wartecsalvagewrench@0` | Сборочная машина HBM / 1 | 1 шт.; 10.0 с; 100 | ingotSteel ×4; ingotCopper ×2 |  |

## Спутники

| Предмет / ID и metadata | Станок / ступень | Выход; время; HE/тик | Состав | Особенности |
|---|---|---|---|---|
| H.A.D.E.S - Satellite based Thermonuclear Weapon System<br>`wartecmod:sat_nuclear@0` | Сборочная машина HBM / 4 | 1 шт.; 80.0 с; 10000 | plateTitanium ×32; plateAluminum ×32; Thermonuclear Warhead (SLBM) ×1; Модуль связи 3 ×1; Guidance System Tier 6 ×1; Блок питания ×4; Комплект сервоприводов ×4 | Спутник изготовлен, но ещё не выведен на орбиту. Выведение отдельно оплачивается штатным носителем/процедурой. |
| sat emp<br>`wartecmod:sat_emp@0` | Сборочная машина HBM / 4 | 1 шт.; 80.0 с; 10000 | plateTitanium ×24; plateAluminum ×24; EMP Warhead (CM) ×4; Модуль связи 3 ×1; Guidance System Tier 5 ×1; Блок питания ×4 |  |
| Спутник кинетической бомбардировки "ОДИН"<br>`wartecmod:kineticbombardmentsatellite@0` | Сборочная машина HBM / 4 | 1 шт.; 80.0 с; 10000 | plateTitanium ×32; plateAluminum ×32; blockTungsten ×16; Модуль связи 3 ×1; Guidance System Tier 6 ×1; Блок питания ×4; Комплект сервоприводов ×4 | Боекомплект ODIN должен оплачиваться материалами; нельзя пополнять восстановлением предмета/NBT. |

## Документация

| Предмет / ID и metadata | Станок / ступень | Выход; время; HE/тик | Состав | Особенности |
|---|---|---|---|---|
| Чертёж крылатой ракеты<br>`wartecmod:cruiseblueprint@0` | Верстак / 1 | 1 шт.; 0.0 с; 0 | minecraft:paper@0 ×2; minecraft:dye@4 ×1 | Пустой синий чертёж; сохранение конструкции не создаёт модули. |
| Чертёж БПЛА<br>`wartecmod:uavblueprint@0` | Верстак / 1 | 1 шт.; 0.0 с; 0 | minecraft:paper@0 ×2; minecraft:dye@2 ×1 | Пустой зелёный чертёж; отдельный состав исключает конфликт с чертежом КР. |
| Руководство по сборке БПЛА (English)<br>`wartecmod:uavguidebook@0` | Верстак / 1 | 1 шт.; 0.0 с; 0 | minecraft:book@0 ×1; Чертёж БПЛА ×1; minecraft:dye@4 ×1 | Язык меняет только издание; переводы можно конвертировать 1:1. |
| Руководство по сборке БПЛА (Русский)<br>`wartecmod:uavguidebook@1` | Верстак / 1 | 1 шт.; 0.0 с; 0 | minecraft:book@0 ×1; Чертёж БПЛА ×1; minecraft:dye@1 ×1 | Язык меняет только издание; переводы можно конвертировать 1:1. |

## Расходники

| Предмет / ID и metadata | Станок / ступень | Выход; время; HE/тик | Состав | Особенности |
|---|---|---|---|---|
| Raw Minced Meat<br>`wartecmod:itemmincedmeatraw@0` | Дробилка HBM / 1 | 9 шт.; 0.0 с; 0 | minecraft:beef@0 ×1 | Сохраняется нынешний выход; питание/время самой дробилки штатные. |
| Fried Minced Meat<br>`wartecmod:itemmincedmeatcooked@0` | Печь / 1 | 1 шт.; 10.0 с; 0 | Raw Minced Meat ×1 |  |

## Декор и материалы

| Предмет / ID и metadata | Станок / ступень | Выход; время; HE/тик | Состав | Особенности |
|---|---|---|---|---|
| Block of Armor Steel<br>`wartecmod:blockarmorsteel@0` | Верстак / 2 | 1 шт.; 0.0 с; 0 | Item Ingot Armor Steel ×9 |  |
| Block Reinforced Wood<br>`wartecmod:blockreinforcedwood@0` | Верстак / 1 | 4 шт.; 0.0 с; 0 | minecraft:planks@32767 ×4; plateSteel ×2; boltSteel ×2 |  |
| US Flag	<br>`wartecmod:decoblockflagus@0` | Верстак / 1 | 9 шт.; 0.0 с; 0 | minecraft:wool@11 ×1; minecraft:wool@14 ×3; minecraft:wool@0 ×5 | Только оформление; не технологический ингредиент оружия. |
| Soviet Union Flag<br>`wartecmod:decoblockflagsu@0` | Верстак / 1 | 9 шт.; 0.0 с; 0 | minecraft:wool@4 ×1; minecraft:wool@14 ×8 | Только оформление; не технологический ингредиент оружия. |
| European Union Flag<br>`wartecmod:decoblockflageu@0` | Верстак / 1 | 9 шт.; 0.0 с; 0 | minecraft:wool@4 ×8; minecraft:wool@11 ×1 | Только оформление; не технологический ингредиент оружия. |
| Arab League Flag<br>`wartecmod:decoblockflagal@0` | Верстак / 1 | 9 шт.; 0.0 с; 0 | minecraft:wool@13 ×8; minecraft:wool@0 ×1 | Только оформление; не технологический ингредиент оружия. |
| China Flag<br>`wartecmod:decoblockflagch@0` | Верстак / 1 | 9 шт.; 0.0 с; 0 | minecraft:wool@4 ×2; minecraft:wool@14 ×7 | Только оформление; не технологический ингредиент оружия. |
| Israel Flag<br>`wartecmod:decoblockflagisr@0` | Верстак / 1 | 9 шт.; 0.0 с; 0 | minecraft:wool@11 ×7; minecraft:wool@0 ×2 | Только оформление; не технологический ингредиент оружия. |
| Subsonic HE Cruise Missile (Deco block)<br>`wartecmod:decoblockcruisemissile@0` | Упаковка 1:1 с NBT / 1 (цепочка 2) | 1 шт.; 0.0 с; 0 | Subsonic HE Cruise Missile ×1 | Упаковка 1:1; обратная операция возвращает тот же предмет/NBT. Только неповреждённый неиспользованный предмет; без восстановления расхода/заряда. |
| Subsonic Cluster Cruise Missile (Deco block)<br>`wartecmod:decoblockcruisemissilecluster@0` | Упаковка 1:1 с NBT / 1 (цепочка 2) | 1 шт.; 0.0 с; 0 | Subsonic Cluster Cruise Missile ×1 | Упаковка 1:1; обратная операция возвращает тот же предмет/NBT. Только неповреждённый неиспользованный предмет; без восстановления расхода/заряда. |
| Subsonic Bunker Buster Cruise Missile (Deco block)<br>`wartecmod:decoblockcruisemissilebuster@0` | Упаковка 1:1 с NBT / 1 (цепочка 3) | 1 шт.; 0.0 с; 0 | Subsonic Bunker Buster Cruise Missile ×1 | Упаковка 1:1; обратная операция возвращает тот же предмет/NBT. Только неповреждённый неиспользованный предмет; без восстановления расхода/заряда. |
| Subsonic EMP Cruise Missile (Deco block)<br>`wartecmod:decoblockcruisemissileemp@0` | Упаковка 1:1 с NBT / 1 (цепочка 3) | 1 шт.; 0.0 с; 0 | Subsonic EMP Cruise Missile ×1 | Упаковка 1:1; обратная операция возвращает тот же предмет/NBT. Только неповреждённый неиспользованный предмет; без восстановления расхода/заряда. |
| Subsonic Thermobaric Cruise Missile (Deco block)<br>`wartecmod:decoblockcruisemissilefae@0` | Упаковка 1:1 с NBT / 1 (цепочка 2) | 1 шт.; 0.0 с; 0 | Item Cruise Missile TB ×1 | Упаковка 1:1; обратная операция возвращает тот же предмет/NBT. Только неповреждённый неиспользованный предмет; без восстановления расхода/заряда. |
| Subsonic Nuclear Cruise Missile (Deco block)<br>`wartecmod:decoblockcruisemissilenuclear@0` | Упаковка 1:1 с NBT / 1 (цепочка 4) | 1 шт.; 0.0 с; 0 | Subsonic Nuclear Cruise Missile ×1 | Упаковка 1:1; обратная операция возвращает тот же предмет/NBT. Только неповреждённый неиспользованный предмет; без восстановления расхода/заряда. |
| Subsonic Thermonuclear Cruise Missile (Deco block)<br>`wartecmod:decoblockcruisemissileh@0` | Упаковка 1:1 с NBT / 1 (цепочка 4) | 1 шт.; 0.0 с; 0 | Subsonic Thermonuclear Cruise Missile ×1 | Упаковка 1:1; обратная операция возвращает тот же предмет/NBT. Только неповреждённый неиспользованный предмет; без восстановления расхода/заряда. |
| Supersonic HE Cruise Missile (Deco block)<br>`wartecmod:decoblocksupersoniccruisemissile@0` | Упаковка 1:1 с NBT / 1 (цепочка 3) | 1 шт.; 0.0 с; 0 | Supersonic HE Cruise Missile ×1 | Упаковка 1:1; обратная операция возвращает тот же предмет/NBT. Только неповреждённый неиспользованный предмет; без восстановления расхода/заряда. |
| Supersonic Thermonuclear Cruise Missile (Deco block)<br>`wartecmod:decoblocksupersoniccruisemissileh@0` | Упаковка 1:1 с NBT / 1 (цепочка 4) | 1 шт.; 0.0 с; 0 | Supersonic Thermonuclear Cruise Missile ×1 | Упаковка 1:1; обратная операция возвращает тот же предмет/NBT. Только неповреждённый неиспользованный предмет; без восстановления расхода/заряда. |
| Hypersonic HE Cruise Missile (Deco block)<br>`wartecmod:decoblockhypersoniccruisemissile@0` | Упаковка 1:1 с NBT / 1 (цепочка 4) | 1 шт.; 0.0 с; 0 | Hypersonic HE Cruise Missile ×1 | Упаковка 1:1; обратная операция возвращает тот же предмет/NBT. Только неповреждённый неиспользованный предмет; без восстановления расхода/заряда. |
| Hypersonic Nuclear Cruise Missile (Deco block)<br>`wartecmod:decoblockhypersoniccruisemissilenuclear@0` | Упаковка 1:1 с NBT / 1 (цепочка 4) | 1 шт.; 0.0 с; 0 | Hypersonic Nuclear Cruise Missile ×1 | Упаковка 1:1; обратная операция возвращает тот же предмет/NBT. Только неповреждённый неиспользованный предмет; без восстановления расхода/заряда. |
|  H.A.D.E.S - Satellite based Thermonuclear Weapon System (Deco block)<br>`wartecmod:decoblocksatellitenuclear@0` | Упаковка 1:1 с NBT / 1 (цепочка 4) | 1 шт.; 0.0 с; 0 | H.A.D.E.S - Satellite based Thermonuclear Weapon System ×1 | Упаковка 1:1; обратная операция возвращает тот же предмет/NBT. Только неповреждённый неиспользованный предмет; без восстановления расхода/заряда. |

## Стройкомплект шахты

| Предмет / ID и metadata | Станок / ступень | Выход; время; HE/тик | Состав | Особенности |
|---|---|---|---|---|
| Стройпакет шахты 1<br>`plan:silo_material_package_1` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | Железобетон ×64; Железобетон ×64; Железобетон ×64; Железобетон ×64; Железобетон ×64; Железобетон ×64; Железобетон ×64; Железобетон ×64; Железобетон ×64; Железобетон ×64 | Запечатанные оплаченные блоки; пакет расходуется только в заводском комплекте. Сегменты люка/верхние половины дверей не оплачиваются второй раз. |
| Стройпакет шахты 2<br>`plan:silo_material_package_2` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | Железобетон ×64; Железобетон ×64; Железобетон ×64; Железобетон ×64; Железобетон ×64; Железобетон ×64; Железобетон ×64; Железобетон ×64; Железобетон ×64; Железобетон ×64 | Запечатанные оплаченные блоки; пакет расходуется только в заводском комплекте. Сегменты люка/верхние половины дверей не оплачиваются второй раз. |
| Стройпакет шахты 3<br>`plan:silo_material_package_3` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | Железобетон ×64; Железобетон ×64; Железобетон ×64; Железобетон ×64; Железобетон ×49; Бетон ×64; Бетон ×64; Бетон ×64; Бетон ×64; Бетон ×64 | Запечатанные оплаченные блоки; пакет расходуется только в заводском комплекте. Сегменты люка/верхние половины дверей не оплачиваются второй раз. |
| Стройпакет шахты 4<br>`plan:silo_material_package_4` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | Бетон ×64; Бетон ×64; Бетон ×64; Бетон ×64; Бетон ×50; Провод из красной меди ×40; hbm:silo_hatch_drillgon@0 ×1; minecraft:glass@0 ×15; minecraft:iron_bars@0 ×64; minecraft:iron_bars@0 ×16 | Запечатанные оплаченные блоки; пакет расходуется только в заводском комплекте. Сегменты люка/верхние половины дверей не оплачиваются второй раз. |
| Стройпакет шахты 5<br>`plan:silo_material_package_5` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | minecraft:iron_block@0 ×5; minecraft:iron_door@0 ×6; minecraft:ladder@0 ×30; minecraft:lever@0 ×12; minecraft:sign@0 ×5; Шахтный настил ×64; Шахтный настил ×64; Шахтный настил ×64; Шахтный настил ×64; Шахтный настил ×64 | Запечатанные оплаченные блоки; пакет расходуется только в заводском комплекте. Сегменты люка/верхние половины дверей не оплачиваются второй раз. |
| Стройпакет шахты 6<br>`plan:silo_material_package_6` | Сборочная машина HBM / 3 | 1 шт.; 40.0 с; 2000 | Шахтный настил ×64; Шахтный настил ×64; Шахтный настил ×39; Шахтный светильник ×32 | Запечатанные оплаченные блоки; пакет расходуется только в заводском комплекте. Сегменты люка/верхние половины дверей не оплачиваются второй раз. |
| Полный строительный комплект шахты<br>`plan:silo_build_kit` | Сборочная машина HBM / 4 | 1 шт.; 120.0 с; 10000 | Стройпакет шахты 1 ×1; Стройпакет шахты 2 ×1; Стройпакет шахты 3 ×1; Стройпакет шахты 4 ×1; Стройпакет шахты 5 ×1; Стройпакет шахты 6 ×1; Шахтный стартовый блок ×1; Система управления огнём 4 ×1; Модуль связи 3 ×1; Блок питания ×2 |  |

## Совместимость HBM

| Предмет / ID и metadata | Станок / ступень | Выход; время; HE/тик | Состав | Особенности |
|---|---|---|---|---|
| hbm:silo_hatch_drillgon@0<br>`hbm:silo_hatch_drillgon@0` | Сборочная машина HBM / 3 | 1 шт.; 60.0 с; 2000 | hbm:silo_hatch_large@0 ×1; Комплект сервоприводов ×4; plateDuraSteel ×16; Гидравлический поршень ×4; Плата обработки III ×1 | Адаптация штатного большого люка в анимированный Drillgon для шахты. В CE 2.6.1.0 этот вариант зарегистрирован, но отдельного штатного рецепта нет; существующие рецепты HBM не заменяются. |

## Особые пути получения

| Предмет / ID и metadata | Станок / ступень | Выход; время; HE/тик | Состав | Особенности |
|---|---|---|---|---|
| Сборная крылатая ракета<br>`wartecmod:assembledcruise@0` | Конструктор КР / 1 | 1 шт.; 0.0 с; 0 |  | Любая допустимая конструкция из десяти оплаченных модулей + пустой/записанный чертёж. Все восемь стартовых примеров достижимы; чертёж не материализует детали. |
| Собранный БПЛА<br>`wartecmod:assembleduav@0` | Конструктор БПЛА / 1 | 1 шт.; 0.0 с; 0 |  | Любая допустимая конструкция из оплаченных обязательных/дополнительных модулей + чертёж. Все пять стартовых примеров достижимы; боеприпасы подвесок отдельно. |
| Отчёт разведки БПЛА<br>`wartecmod:uavreconreport@0` | Настоящая разведка / 1 | 1 шт.; 0.0 с; 0 |  | Изготовить бумагу, провести настоящий разведполёт, скачать отчёт. Предлагается цена 1 бумага за новое скачивание; содержимое карты создаётся только серверными наблюдениями. |
| Конструкция мачты связи<br>`wartecmod:longrangecommunicationmastsegment@0` | Часть структуры / 1 | 1 шт.; 0.0 с; 0 |  | Автоматическая деталь оплаченной мачты; нельзя самостоятельно размещать неполную служебную структуру. |
| Конструкция стратегической РЛС<br>`wartecmod:strategicradarstructure@0` | Часть структуры / 1 | 1 шт.; 0.0 с; 0 |  | 3672 панели корпуса оплачены основанием: 57 комплектов по 64 и ещё 24 панели. Фундамент 33×33 игрок строит отдельно; служебный блок сам не создаёт материалы. |
| Mobile Turret Proxy<br>`wartecmod:mobileturretproxy@0` | Часть структуры / 1 | 1 шт.; 0.0 с; 0 |  | Техническая коллизия/прокси настоящей установки. Получается созданием установки, не самостоятельный предмет производства. |
| ПГРК РС-24 "Ярс"<br>`wartecmod:yarstel@0` | Отключено: резерв / 4 | 1 шт.; 0.0 с; 0 |  | Ярс отключён ранее по решению проекта. Проект будущего рецепта: тяжёлое шасси ×2, кабина, питание ×3, пакет heavy, СУО IV, гидравлика ×8. Не регистрировать и не включать без отдельной команды. |
| Ракета РС-24 "Ярс"<br>`wartecmod:strategicmissile@1` | Отключено: резерв / 4 | 1 шт.; 0.0 с; 0 |  | Будущий рецепт: ступень МБР ×3, платформа разведения ×1, готовая термоядерная БЧ WarTech ×3, наведение VI ×2. Пока исключён из активной прогрессии. |

## Все стартовые NBT-примеры конструкторов

| Пример | Оплаченные модули для конструктора |
|---|---|
| cruise_body_light_ground | Корпус реактивной ракеты-дрона; Экономичный маршевый двигатель; Малая топливная секция; Складные крылья; Навигатор огибания рельефа; Координатный носовой модуль; Фугасная боевая часть; Контактный взрыватель; Комплект направляющей ракеты-дрона; Автономный модуль управления |
| cruise_body_light_air | Корпус реактивной ракеты-дрона; Экономичный маршевый двигатель; Малая топливная секция; Складные крылья; Навигатор огибания рельефа; Координатный носовой модуль; Фугасная боевая часть; Контактный взрыватель; Адаптер авиационного запуска; Автономный модуль управления |
| cruise_body_classic_ground | Корпус классической крылатой ракеты; Универсальный маршевый двигатель; Стандартная топливная секция; Складные крылья; Навигатор огибания рельефа; Координатный носовой модуль; Фугасная боевая часть; Контактный взрыватель; Комплект наземного ускорителя; Автономный модуль управления |
| cruise_body_classic_air | Корпус классической крылатой ракеты; Универсальный маршевый двигатель; Стандартная топливная секция; Складные крылья; Навигатор огибания рельефа; Координатный носовой модуль; Фугасная боевая часть; Контактный взрыватель; Адаптер авиационного запуска; Автономный модуль управления |
| cruise_body_heavy_ground | Тяжёлый малозаметный корпус; Универсальный маршевый двигатель; Стандартная топливная секция; Складные крылья; Навигатор огибания рельефа; Координатный носовой модуль; Фугасная боевая часть; Контактный взрыватель; Комплект наземного ускорителя; Автономный модуль управления |
| cruise_body_heavy_air | Тяжёлый малозаметный корпус; Универсальный маршевый двигатель; Стандартная топливная секция; Складные крылья; Навигатор огибания рельефа; Координатный носовой модуль; Фугасная боевая часть; Контактный взрыватель; Адаптер авиационного запуска; Автономный модуль управления |
| cruise_body_long_range_ground | Тяжёлый дальнобойный корпус ракеты-дрона; Тяжёлый экономичный двигатель; Тяжёлая дальнобойная топливная секция; Тяжёлые складные крылья дальности; Координатный навигатор; Координатный носовой модуль; Тяжёлая фугасная боевая часть; Контактный взрыватель; Комплект наземного ускорителя; Автономный модуль управления |
| cruise_body_long_range_air | Тяжёлый дальнобойный корпус ракеты-дрона; Тяжёлый экономичный двигатель; Тяжёлая дальнобойная топливная секция; Тяжёлые складные крылья дальности; Координатный навигатор; Координатный носовой модуль; Тяжёлая фугасная боевая часть; Контактный взрыватель; Адаптер авиационного запуска; Автономный модуль управления |
| uav_one_way | Одноразовый планер "летающее крыло"; Экономичный поршневой двигатель; Компактный топливный модуль; Базовый контроллер полёта; Радиоканал малой дальности; Дневная камера; Фугасная боевая часть |
| uav_recon | Разведывательный планер БПЛА; Сбалансированный роторный двигатель; Топливный бак большой дальности; Точный автопилот; Спутниковый канал связи; Оптико-тепловизионная станция; Блок тепловых ловушек |
| uav_recon_carrier | Разведывательный планер БПЛА; Сбалансированный роторный двигатель; Компактный топливный модуль; Точный автопилот; Спутниковый канал связи; Оптико-тепловизионная станция; Одиночный подфюзеляжный ракетный подвес; Блок тепловых ловушек |
| uav_strike | Тактический ударный планер БПЛА; Тяжёлый турбовинтовой двигатель; Топливный бак большой дальности; Боевой вычислитель полёта; Защищённый тактический канал; Оптико-тепловизионная станция; Тяжёлая четырёхточечная подвеска; Блок тепловых ловушек |
| uav_strike_carrier | Тактический ударный планер БПЛА; Тяжёлый турбовинтовой двигатель; Топливный бак большой дальности; Боевой вычислитель полёта; Защищённый тактический канал; Оптико-тепловизионная станция; Одиночный подфюзеляжный ракетный подвес; Блок тепловых ловушек |

## Ствольная артиллерия 2.0.0

| Предмет | Станок | Время / HE за тик | Состав |
|---|---|---|---|
| Д-30 `wartecmod:d30@0` | Assembly Machine | 30 с / 500 | Прицепная рама ×1; артиллерийский узел ×1; СУО I ×1; блок питания ×1; стальная пластина ×16 |
| CAESAR `wartecmod:caesar@0` | Assembly Machine | 50 с / 500 | Среднее шасси ×1; кабина ×1; артиллерийский узел ×1; СУО II ×1; блок питания ×1; сервоприводы ×2; стальная пластина ×24 |

Итого: 324 действующих пользовательских позиции, 54 детали, 322 прямых рецепта,
из них 289 Assembly Machine. 330 строк полной ведомости включают также
служебные/отключённые пути и рецепт люка HBM. Старые 328 строк сохранены.
