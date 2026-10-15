# 2.0.0 — validation scope / пределы проверки

**Implemented does not mean tested under every configuration.**
Релиз экспериментальный: описаны готовые функции и реальные пределы проверки.

| Приёмка / Acceptance | Результат / Result | Область / Scope |
|---|---|---|
| Release candidate | 620 unit tests, no failures/errors | Current CE code, data and geometry |
| dev114 artillery | 78 server + 4 real restart checks | Physical shots/impacts, one expenditure, permissions, pending command persistence |
| dev114 visuals | 17 RU/EN frames, 800×600 | Models, aim/recoil/elevation, inventory and controls |
| dev113 multiplayer | 74 connected MP checks | Dedicated server, two TCP clients, survival without OP, 150 ms added RTT, roles, shared stocks, 256-stage plan, reconnect and restart |
| dev113 native | 3510 native + 13 blast checks | Production, driving, plans/HQ, defense/ABM, raids, remote commands, silo and effects |
| dev112 industry | Historical 8255 native checks | Native machine recipes, HBM reload, 528/LBSM, NBT and material accounting |

The 2.0.0 package is compared against accepted dev114 gameplay classes and data.
Allowed release changes are the consistent mod/network version, accurate CE log
text, documentation and removal of two unused local source-path manifests.
All remaining gameplay classes and assets must match byte-for-byte.

Проверки разных checkpoint не складываются в один длительный прогон. Итоговая
упаковка проверяет соответствие принятой реализации; длительная survival/MP-кампания,
полная миграция старого мира и нагрузка всего пользовательского модпака не пройдены.
Стенды с защищёнными батареями не доказывают выживаемость под реальным уроном.
Перехват ограничен физикой, радаром, каналами, энергией и боезапасом.

No complete ore-to-equipment playthrough, long multiplayer campaign,
full-user-modpack benchmark or comprehensive old-world migration is claimed.
Protected QA batteries do not establish survival under real damage.
Static frames do not establish subjective audio quality.
