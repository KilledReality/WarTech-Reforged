# WarTech Reforged 1.4.0 Development Notes

This document describes the first development build of the 1.4.0 branch. It is
not a stable release and should be tested in a disposable world before release.

## Mobile short-range air defense

### 9K331 Tor-M1

- Eight internal WTI-2 Lance interceptor cells.
- Integrated X-band radar with a 340-block detection range.
- 220-block engagement range and 260-block detection ceiling.
- Vertical interceptor launch.

### 96K6 Pantsir-S2

- Twelve WTI-1 Falcon interceptor cells.
- Integrated X-band radar with a 260-block detection range.
- 100-block engagement range and 190-block detection ceiling.
- Angled interceptor launch and animated search radar.

### Controls

- **RMB while retracted:** enter the driver's seat.
- **W / S / A / D:** drive and steer.
- **Left Shift:** leave the driver's seat.
- **Shift+RMB:** deploy or retract the system.
- **RMB while deployed:** open the combat interface.
- **RMB with an HBM battery:** transfer energy directly to the system.

The combat interface contains the missile cells, an HBM battery slot, radar
display, energy and health indicators, radar toggle, and three fire modes:

- **HOLD:** track targets without launching.
- **AUTO:** normal automatic engagement.
- **EMERGENCY:** reduced launch interval for a dense attack.

The systems register themselves in the existing air-defense network, use the
same target reservation and delayed follow-up rules as stationary launchers,
and preserve ammunition, energy, operating mode, and ownership in NBT.

## Русский

Это первая тестовая сборка ветки 1.4.0. Перед выпуском стабильной версии её
следует проверять в отдельном тестовом мире.

### 9К331 «Тор-М1»

- Восемь внутренних ячеек для противоракет WTI-2 «Копьё».
- Встроенная РЛС X-диапазона с дальностью обнаружения 340 блоков.
- Дальность перехвата 220 блоков, потолок обнаружения 260 блоков.
- Вертикальный старт противоракет.

### 96К6 «Панцирь-С2»

- Двенадцать ячеек для противоракет WTI-1 «Сокол».
- Встроенная РЛС X-диапазона с дальностью обнаружения 260 блоков.
- Дальность перехвата 100 блоков, потолок обнаружения 190 блоков.
- Наклонный старт противоракет и вращающаяся поисковая РЛС.

### Управление

- **ПКМ в походном положении:** занять место водителя.
- **W / S / A / D:** движение и поворот.
- **Левый Shift:** покинуть машину.
- **Shift+ПКМ:** развернуть или свернуть комплекс.
- **ПКМ в боевом положении:** открыть интерфейс управления.
- **ПКМ с аккумулятором HBM:** напрямую передать энергию комплексу.

В интерфейсе находятся ячейки противоракет, слот аккумулятора HBM, экран РЛС,
индикаторы энергии и прочности, выключатель РЛС и три режима огня:

- **HOLD:** сопровождение целей без запуска.
- **AUTO:** обычный автоматический перехват.
- **EMERGENCY:** сокращённый интервал запуска при массированном налёте.

Оба комплекса входят в существующую сеть ПВО, используют общую систему
резервирования целей и задержки повторного перехвата, а также сохраняют
боекомплект, энергию, режим работы и принадлежность в NBT.
