package com.wartec.wartecmod;

import com.wartec.wartecmod.port.content.ContentHooks;
import com.wartec.wartecmod.port.entity.WarTechEntityRegistration;
import com.wartec.wartecmod.port.gameplay.GameplayIntentHandler;
import com.wartec.wartecmod.port.gameplay.LegacyTileTypes;
import com.wartec.wartecmod.port.gameplay.CommandWarTechTeam;
import com.wartec.wartecmod.port.gui.WarTechGuiHandler;
import com.wartec.wartecmod.port.integration.MissileChunkLoader;
import com.wartec.wartecmod.port.integration.HbmCeWorldMigration;
import com.wartec.wartecmod.port.integration.LegacyNbtDataFixer;
import com.wartec.wartecmod.port.integration.NtmCompatibilityVerifier;
import com.wartec.wartecmod.port.integration.WarTechRecipeRegistration;
import com.wartec.wartecmod.port.network.WarTechNetwork;
import com.wartec.wartecmod.port.proxy.CommonProxy;
import com.wartec.wartecmod.port.satellite.WarTechSatelliteRegistration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import org.apache.logging.log4j.Logger;

@Mod(
    modid = WarTechReforged.MODID,
    name = WarTechReforged.NAME,
    version = WarTechReforged.VERSION,
    acceptedMinecraftVersions = "[1.12.2]",
    dependencies = "required-after:hbm"
)
public final class WarTechReforged {
    public static final String MODID = "wartecmod";
    public static final String NAME = "WarTech Reforged";
    public static final String VERSION = "1.6.0-experimental";

    @Instance(MODID)
    public static WarTechReforged instance;

    @SidedProxy(
        clientSide = "com.wartec.wartecmod.port.proxy.ClientProxy",
        serverSide = "com.wartec.wartecmod.port.proxy.CommonProxy"
    )
    public static CommonProxy proxy;

    public static Logger logger;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        NtmCompatibilityVerifier.verifyRequiredApis();
        LegacyNbtDataFixer.register();
        WarTechNetwork.register();
        HbmCeWorldMigration.register();
        MissileChunkLoader.register();
        WarTechEntityRegistration.registerAll(this, 1);
        LegacyTileTypes.registerAll();
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new WarTechGuiHandler());
        ContentHooks.setHandler(new GameplayIntentHandler());
        proxy.preInit();
        logger.info("Loading WarTech Reforged {} for NTM Extended", VERSION);
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        logger.info("WarTech Reforged 1.12.2 compatibility layer initialized");
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        WarTechRecipeRegistration.registerRuntimeIntegration();
        WarTechSatelliteRegistration.register();
        logger.info("Registered the dev66 crafting, NTM machine and hazard progression");
    }

    @EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandWarTechTeam());
        event.registerServerCommand(new com.wartec.wartecmod.port.gameplay.CommandWarTechFlight());
    }
}
