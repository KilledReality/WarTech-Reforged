package com.wartec.wartecmod.compat;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import net.minecraftforge.common.MinecraftForge;

@Mod(
    modid = "wartecfix",
    name = "WarTech Reforged Compatibility",
    version = "1.6.0",
    dependencies = "required-after:hbm;after:wartecmod"
)
public final class WarTecBootstrap {
    @Mod.Instance("wartecfix")
    public static WarTecBootstrap instance;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        try {
            Class<?> items = Class.forName("com.wartec.wartecmod.items.wartecmodItems");
            Class<?> blocks = Class.forName("com.wartec.wartecmod.blocks.wartecmodBlocks");

            boolean registeredItems = false;
            boolean registeredBlocks = false;

            if (get(items, "itemTomahawkMissile") == null) {
                Method itemsMethod = items.getMethod("Items");
                itemsMethod.invoke(null);
                registeredItems = true;
            }

            if (get(blocks, "LaunchTube") == null) {
                Method blocksMethod = blocks.getMethod("Blocks");
                blocksMethod.invoke(null);
                registeredBlocks = true;
            }

            CreativeTabFix.apply();
            LegacyIconFix.apply();
            RemoteControlNetwork.register();
            FactionCommandNetwork.register();
            PatriotContent.register();
            AdvancedMissileContent.register();
            MobileArtilleryContent.register();
            RadarNetworkContent.register();
            DroneStrikeContent.register();
            OrbitalStrikeContent.register();
            StrategicAviationContent.register();
            MissileChunkLoader.register();
            MinecraftForge.EVENT_BUS.register(new SalvageWrenchCompat());
            MinecraftForge.EVENT_BUS.register(new TeamPersistenceHandler());
            writeMarker(event, "loaded, registeredItems=" + registeredItems + ", registeredBlocks=" + registeredBlocks);
        } catch (Throwable t) {
            writeMarker(event, "failed: " + t);
            t.printStackTrace();
        }
        try {
            TacticalAviationContent.register();
            writeMarker(event, "loaded, tacticalAircraft=true");
        } catch (Throwable t) {
            writeMarker(event, "failed tactical aircraft: " + t);
            t.printStackTrace();
        }
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        NetworkRegistry.INSTANCE.registerGuiHandler(instance, new RadarGuiHandler());
        if (event.getSide().isClient()) {
            registerClient("com.wartec.wartecmod.compat.client.PatriotClient");
            registerClient("com.wartec.wartecmod.compat.client.AdvancedMissileClient");
            registerClient("com.wartec.wartecmod.compat.client.MobileArtilleryClient");
            registerClient("com.wartec.wartecmod.compat.client.RadarNetworkClient");
            registerClient("com.wartec.wartecmod.compat.client.DroneStrikeClient");
            registerClient("com.wartec.wartecmod.compat.client.OrbitalStrikeClient");
            registerClient("com.wartec.wartecmod.compat.client.StrategicAviationClient");
            registerClient("com.wartec.wartecmod.compat.client.TacticalAviationClient");
        }
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        OrbitalStrikeContent.registerSatelliteType();
        CreativeTabFix.reorderTabs();
    }

    private static Object get(Class<?> owner, String name) throws Exception {
        Field field = owner.getField(name);
        return field.get(null);
    }

    private static void registerClient(String className) {
        try {
            Class.forName(className).getMethod("register").invoke(null);
        } catch (Throwable failure) {
            System.err.println("[WarTech] Client registrar failed: " + className);
            failure.printStackTrace();
        }
    }

    private static void writeMarker(FMLPreInitializationEvent event, String text) {
        try {
            File file = new File(event.getModConfigurationDirectory(), "wartecfix-load-marker.txt");
            FileWriter writer = new FileWriter(file, false);
            try {
                writer.write(text);
                writer.write(System.getProperty("line.separator"));
            } finally {
                writer.close();
            }
        } catch (Throwable ignored) {
        }
    }
}
