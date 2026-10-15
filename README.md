# WarTech Reforged

## 2.0.0 — Combined Operations (experimental)

Minecraft **1.12.2**, Forge **14.23.5.2860**, HBM NTM **Community Edition 2.6.1.0**,
**MixinBooter 10.7**, Java 8. Use matching 2.0.0 on server and clients.

WarTech Reforged combines configurable cruise missiles and UAVs, aviation,
MLRS and gun artillery, strategic mobile launchers, underground silos, air defense,
ABM, reconnaissance, headquarters planning and industrial HBM production.

WarTech Reforged объединяет конструкторы КР/БПЛА, авиацию, РСЗО, ствольную
артиллерию, стратегические комплексы, шахты, ПВО/ПРО, разведку, штаб и крафт HBM.
Все перечисленные функции реализованы. Экспериментальный статус отражает
пределы проверки: длительная survival/MP-кампания и полная миграция мира ещё не пройдены.

- [Download 2.0.0 / Скачать](https://github.com/KilledReality/WarTech-Reforged/releases/tag/v2.0.0)
- [Руководство RU](docs/modular-warfare/GUIDE.ru.md) · [Manual EN](docs/modular-warfare/GUIDE.en.md)
- [Изменения RU](docs/modular-warfare/CHANGELOG.ru.md) · [Changes EN](docs/modular-warfare/CHANGELOG.en.md)
- [Recipes / Рецепты](docs/recipes/CATALOG.ru.md) · [Validation / Проверки](docs/modular-warfare/VALIDATION.md)
- [Source build](1.12.2/README_CE.md) · [Credits and conditions](THIRD_PARTY_NOTICES.md)
- [Discord](https://discord.gg/c46gwVfnea)

Back up worlds before updating. Do not combine CE with NTM Extended or another
WarTech JAR. Dependencies are external and are not included in the download.
Yars is disabled. Supply tasks do not provide automatic delivery; cabins are simplified.

## Legacy versions

Earlier 1.7.10 and Extended builds remain in [GitHub releases](https://github.com/KilledReality/WarTech-Reforged/releases).
They are separate compatibility lines; 2.0.0 does not update or replace their dependencies.

## Source build / Сборка исходников

From `1.12.2/WarTech-Reforged-1.12.2`, run `tools/build_release_2_0.ps1 -FlightQa`.
Supply a Java 8 JDK with `-JavaHome`, the external CE JAR with `-NtmJar` and
MixinBooter with `-MixinBooterJar` when they are outside the default locations.
Outputs: `build/release-2.0.0/libs/`. The optional QA helper is separate from the mod.
Plain Gradle defaults describe the historical Extended project; use the CE script.

## Distribution

WarTech Reforged is freely available. Donations grant no access, early builds,
advantages or priority. Individual third-party asset conditions remain applicable;
the project does not relicense all assets as MIT. Preserve the notices.
The mod is not endorsed by the original WarTech or HBM teams or real manufacturers.

The historical byte-identity audit needs a maintainer-held dev66 JAR; it is
skipped when absent in a clean checkout. That old binary is not redistributed.
