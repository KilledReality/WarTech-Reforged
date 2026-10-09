# Building the Community Edition update / Сборка Community Edition

The source tree preserves the Extended API baseline. The CE script generates
an adapted copy under `build/compat-ce-src`, compiles it, runs unit tests and
reobfuscates the production JAR. Do not run plain `gradlew build` for CE.

Исходное дерево сохраняет API-базу Extended. Скрипт CE создаёт адаптированную
копию в `build/compat-ce-src`, компилирует, выполняет тесты и реобфускацию.
Для CE не запускайте просто `gradlew build`.

## Dependencies / Зависимости

- JDK 8 (not only a JRE / именно JDK).
- Minecraft Forge 1.12.2-14.23.5.2860 (resolved by Gradle).
- HBM NTM Community Edition 1.12.2-2.6.1.0 JAR.
- MixinBooter 10.7 JAR.
- Internet access for the Gradle wrapper and Maven dependencies on first build.

HBM, MixinBooter, Java, downloaded model sources and local test worlds are not
redistributed in this repository. Supply dependency paths yourself.

HBM, MixinBooter, Java, исходные архивы моделей и локальные тестовые миры
не распространяются в этом репозитории. Укажите собственные пути.

From this project directory, in PowerShell / Из этой папки проекта в PowerShell:

```powershell
./tools/build_ntm_ce.ps1 -JavaHome 'C:/path/to/jdk8' -NtmJar 'C:/path/to/NTM-CE-1.12.2-2.6.1.0.jar' -MixinBooterJar 'C:/path/to/mixinbooter-10.7.jar'
```

Output / Результат:
`build/libs/WarTech-Reforged-1.12.2-NTM-CE-1.6.0-experimental.jar`.
JUnit report / Отчёт JUnit: `build/reports/tests/test/index.html`.

The opt-in `-PflightQa` source set is a separate local diagnostic mod, not
production content. Never bundle it or dependency JARs into the release.

Опциональный source set `-PflightQa` — отдельный диагностический мод, не
часть релиза. Не включайте его и JAR зависимостей в публикуемый мод.
