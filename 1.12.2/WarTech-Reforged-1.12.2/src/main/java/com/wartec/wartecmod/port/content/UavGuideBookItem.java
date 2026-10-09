package com.wartec.wartecmod.port.content;

import com.wartec.wartecmod.WarTechReforged;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.stats.StatList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.client.util.ITooltipFlag;

/** A signed in-game manual with separate English and Russian editions. */
public final class UavGuideBookItem extends VariantItem {
    private static final int GUIDE_SCHEMA = 5;

    private static final String[] ENGLISH_PAGES = {
        "CUSTOM UAV GUIDE\n\nThis manual covers the UAV Constructor, modules, design rules, launch and recovery.",
        "1. REQUIRED ITEMS\n\nUAV Constructor\nUAV Launch Point\nBlank UAV Blueprint\nOne module for every required slot.",
        "2. WORKFLOW\n\nPlace the Constructor. Insert modules into their labeled slots. Press BLUEPRINT, then ASSEMBLE UAV. Invalid designs list their exact faults.",
        "3. AIRFRAMES\n\nOne-way: 170 kg limit.\nRecon: 275 kg limit.\nStrike: 490 kg limit and up to four weapon hardpoints.",
        "4. ENGINES\n\nEconomy saves fuel. Balanced suits general missions. Heavy carries large payloads but consumes more energy. Insufficient thrust invalidates a design.",
        "5. ENERGY & RANGE\n\nCompact is light. Long-range has maximum endurance. Hybrid balances mass and capacity. The displayed MAX FLIGHT RANGE includes engine draw and total mass.",
        "6. FLIGHT CONTROL\n\nBasic is economical. Precision gives smoother accurate flight. Combat gives the strongest turning authority for strike and terrain-following missions.",
        "7. DATA LINKS\n\nShort: 850 blocks.\nEncrypted: 2,600 blocks.\nSatellite: 8,000 blocks.\nManual control ends outside the displayed REMOTE LINK range.",
        "8. SENSORS\n\nDay camera is light. EO/IR supports all-weather observation. SAR is heavy and cannot fit the smallest one-way airframe.",
        "9. PAYLOAD MODES\n\nAny installed warhead makes the UAV kamikaze. A weapon rack keeps it reusable. Only the Strike airframe accepts external racks.",
        "10. WARHEADS\n\nHE: general blast.\nThermobaric: structures and area targets.\nShaped charge: compact penetrator for hardened targets.",
        "11. HEAVY WARHEADS\n\nHeavy HE: 14 blast.\nHeavy thermobaric: 18 blast.\nTheir mass demands a heavy engine and carefully chosen modules.",
        "12. DEFENSE\n\nFlares provide eight infrared decoys. The EW suite reduces hostile jamming. The one-way airframe cannot carry defensive modules.",
        "13. LIGHT KAMIKAZE\n\nOne-way frame + economy engine + compact fuel + basic control + short link + day camera + HE warhead.",
        "14. HEAVY KAMIKAZE\n\nStrike frame + heavy engine + long-range fuel + combat control + satellite link + EO/IR + heavy thermobaric warhead + EW.",
        "15. RECON BUILD\n\nRecon frame + balanced engine + long-range fuel + precision control + satellite link + EO/IR + flares. Leave payload empty.",
        "16. REUSABLE STRIKE\n\nStrike frame + heavy engine + long-range fuel + combat control + encrypted link + EO/IR + heavy rack + flares.",
        "17. DEPLOYMENT\n\nPlace a Launch Point with clear space above it. Use the assembled UAV on its top. Right-click for weapons, battery, LTC, launch and data. Shift + right-click opens Remote Pilot.",
        "18. FIELD RULES\n\nKamikaze UAVs crash after link loss. Reusable UAVs return home. Contact detonates a warhead once. Use the GUI values as the final authority.",
        "19. MISSION PROGRAMMER\n\nInsert the UAV in UAV and an HBM or WarTech designator in TGT. Select a task and press PROGRAM / OK. Manual X/Y/Z routes remain available. A one-way route must finish with STRIKE.",
        "20. OBSERVE & MAP\n\nOBSERVE surveys terrain and contacts. DOWNLOAD DATA or FLEET > GET REPORT puts a permanent report sheet in your inventory. Right-click it anytime to reopen the full map.",
        "21. FLEET & SERVICE\n\nFLEET lists friendly UAV telemetry and can recall reusable aircraft or open reports. Recovered UAVs keep damage and power. Recharge with an HBM battery; REPAIR consumes iron while landed."
    };

    private static final String[] RUSSIAN_PAGES = {
        "РУКОВОДСТВО БПЛА\n\nЗдесь описаны конструктор, модули, правила сборки, запуск и возврат пользовательских БПЛА.",
        "1. ЧТО НУЖНО\n\nКонструктор БПЛА\nПусковая точка\nПустой чертеж\nПо одному модулю для каждого обязательного слота.",
        "2. ПОРЯДОК СБОРКИ\n\nПоставьте конструктор. Разложите модули по подписанным слотам. Нажмите ЧЕРТЕЖ, затем СБОРКА. Все ошибки выводятся в интерфейсе.",
        "3. ПЛАНЕРЫ\n\nОдноразовый: до 170 кг.\nРазведчик: до 275 кг.\nУдарный: до 490 кг и до четырех точек подвески.",
        "4. ДВИГАТЕЛИ\n\nЭкономичный бережет топливо. Сбалансированный универсален. Тяжелый несет крупную нагрузку, но расходует больше энергии. Недостаток тяги блокирует сборку.",
        "5. ЭНЕРГИЯ И ДАЛЬНОСТЬ\n\nКомпактный модуль легкий. Дальний бак дает максимум времени. Гибрид сохраняет баланс. Дальность в интерфейсе учитывает расход и массу.",
        "6. УПРАВЛЕНИЕ\n\nБазовый контроллер дешевый. Точный дает плавный полет. Боевой обеспечивает максимальную маневренность для удара и полета у земли.",
        "7. КАНАЛЫ СВЯЗИ\n\nКороткий: 850 блоков.\nЗащищенный: 2600.\nСпутниковый: 8000.\nРучное управление доступно в пределах REMOTE LINK.",
        "8. СЕНСОРЫ\n\nДневная камера легкая. EO/IR работает в любую погоду. РЛС SAR тяжелая и не ставится в малый одноразовый планер.",
        "9. ТИП НАГРУЗКИ\n\nЛюбая боевая часть делает БПЛА камикадзе. Подвеска сохраняет многоразовый режим. Внешние подвески ставятся только на ударный планер.",
        "10. БОЕВЫЕ ЧАСТИ\n\nФугасная: общий урон.\nТермобарическая: здания и площадь.\nКумулятивная: компактный пробивной заряд для укрепленных целей.",
        "11. ТЯЖЕЛЫЕ БЧ\n\nТяжелая фугасная: взрыв 14.\nТяжелая термобарическая: взрыв 18.\nДля них нужны тяжелый двигатель и точный расчет массы.",
        "12. ЗАЩИТА\n\nЛовушки дают восемь ИК-приманок. Комплекс РЭБ снижает влияние помех. Одноразовый планер не несет защитные модули.",
        "13. ЛЕГКИЙ КАМИКАДЗЕ\n\nОдноразовый планер + экономичный двигатель + компактное топливо + базовый контроллер + короткая связь + камера + фугасная БЧ.",
        "14. ТЯЖЕЛЫЙ КАМИКАДЗЕ\n\nУдарный планер + тяжелый двигатель + дальний бак + боевой контроллер + спутниковая связь + EO/IR + тяжелая термобарическая БЧ + РЭБ.",
        "15. РАЗВЕДЧИК\n\nРазведпланер + сбалансированный двигатель + дальний бак + точный контроллер + спутниковая связь + EO/IR + ловушки. Без нагрузки.",
        "16. УДАРНЫЙ БПЛА\n\nУдарный планер + тяжелый двигатель + дальний бак + боевой контроллер + защищенная связь + EO/IR + тяжелая подвеска + ловушки.",
        "17. ЗАПУСК\n\nПоставьте пусковую точку со свободным местом сверху. Обычный ПКМ открывает вооружение, батарею, ЛТЦ, запуск и данные. Shift + ПКМ открывает ручное управление.",
        "18. ПРАВИЛА\n\nКамикадзе при потере связи падает. Многоразовый БПЛА возвращается домой. БЧ срабатывает при контакте один раз. Итоговые цифры смотрите в интерфейсе.",
        "19. ПРОГРАММАТОР\n\nВставьте БПЛА в UAV, а целеуказатель HBM или WarTech в TGT. Выберите задачу и нажмите PROGRAM / OK. Ручные маршруты X/Y/Z также доступны.",
        "20. OBSERVE И КАРТА\n\nOBSERVE обследует местность и цели. DOWNLOAD DATA или FLEET > GET REPORT кладет постоянный отчет в инвентарь. Откройте его ПКМ в любое время.",
        "21. ГРУППА И СЕРВИС\n\nFLEET показывает союзные БПЛА и позволяет вернуть многоразовый аппарат или открыть отчет. После демонтажа сохраняются урон и заряд. Батарея HBM заряжает, REPAIR расходует железо на земле."
    };

    public UavGuideBookItem(String legacyName) {
        super(legacyName, WarTechCreativeTabs.CUSTOM_UAV, 1, "en", "ru");
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world,
            EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        ensureBookData(stack);
        if (world.isRemote) {
            WarTechReforged.proxy.openUavGuide(player, stack);
        } else {
            player.addStat(StatList.getObjectUseStats(this));
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world,
            List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(TextFormatting.GRAY + (getVariant(stack) == 0
                ? "English edition - 21 pages"
                : "Русское издание - 21 страница"));
    }

    private void ensureBookData(ItemStack stack) {
        NBTTagCompound tag = stack.hasTagCompound()
                ? stack.getTagCompound() : new NBTTagCompound();
        if (tag.getInteger("WarTechGuideSchema") == GUIDE_SCHEMA
                && tag.hasKey("pages", 9)) {
            return;
        }
        boolean russian = getVariant(stack) == 1;
        tag.setString("title", russian
                ? "Конструктор БПЛА" : "Custom UAV Assembly Guide");
        tag.setString("author", "WarTech Engineering Bureau");
        tag.setBoolean("resolved", true);
        tag.setInteger("generation", 0);
        tag.setInteger("WarTechGuideSchema", GUIDE_SCHEMA);
        NBTTagList pages = new NBTTagList();
        for (String page : russian ? RUSSIAN_PAGES : ENGLISH_PAGES) {
            ITextComponent text = new TextComponentString(page);
            pages.appendTag(new NBTTagString(
                    ITextComponent.Serializer.componentToJson(text)));
        }
        tag.setTag("pages", pages);
        stack.setTagCompound(tag);
    }
}
