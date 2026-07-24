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
    version = "1.5.0-universal-hbm",
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
            try {
                Class.forName("com.wartec.wartecmod.compat.client.PatriotClient")
                        .getMethod("register")
                        .invoke(null);
                Class.forName("com.wartec.wartecmod.compat.client.AdvancedMissileClient")
                        .getMethod("register")
                        .invoke(null);
                Class.forName("com.wartec.wartecmod.compat.client.MobileArtilleryClient")
                        .getMethod("register")
                        .invoke(null);
                Class.forName("com.wartec.wartecmod.compat.client.RadarNetworkClient")
                        .getMethod("register")
                        .invoke(null);
                Class.forName("com.wartec.wartecmod.compat.client.DroneStrikeClient")
                        .getMethod("register")
                        .invoke(null);
                Class.forName("com.wartec.wartecmod.compat.client.OrbitalStrikeClient")
                        .getMethod("register")
                        .invoke(null);
                Class.forName("com.wartec.wartecmod.compat.client.StrategicAviationClient")
                        .getMethod("register")
                        .invoke(null);
            } catch (Throwable t) {
                t.printStackTrace();
            }
            try {
                Class.forName("com.wartec.wartecmod.compat.client.TacticalAviationClient")
                        .getMethod("register")
                        .invoke(null);
            } catch (Throwable t) {
                t.printStackTrace();
            }
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
