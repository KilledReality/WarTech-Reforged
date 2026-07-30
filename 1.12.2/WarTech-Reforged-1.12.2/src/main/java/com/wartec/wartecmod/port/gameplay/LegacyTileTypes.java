package com.wartec.wartecmod.port.gameplay;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.GameRegistry;

public final class LegacyTileTypes {
    private static final String MODID = "wartecmod";

    private static final List<TileRegistration> LEGACY_REGISTRATIONS =
            Collections.unmodifiableList(Arrays.asList(
        machine("wartecmodLaunchTube", VlsLaunchTube.class),
        machine("wartecmodVlsExhaust", VlsExhaust.class),
        machine("wartecmodBallisticMissileLauncher", BallisticLauncher.class),
        visual("tileDecoBlockCruiseMissile", DecoCruiseMissile.class),
        visual("tileDecoBlockFlagUS", DecoFlagUs.class),
        visual("tileDecoBlockFlagSU", DecoFlagSu.class),
        visual("tileDecoBlockFlagEU", DecoFlagEu.class),
        visual("tileDecoBlockFlagCH", DecoFlagCh.class),
        machine("wartecGeranLauncher", GeranLauncher.class),
        machine("wartecMobileGreg", MobileGreg.class),
        machine("wartecMobileHenry", MobileHenry.class),
        machine("wartecPatriotLauncher", PatriotLauncher.class),
        machine("wartecS400Launcher", S400Launcher.class),
        machine("wartecAirRaidSirenRelay", AirRaidRelay.class),
        machine("wartecLongRangeCommunicationMast", CommunicationRelay.class),
        machine("wartecStrategicEarlyWarningRadar", StrategicRadar.class)
    ));

    private LegacyTileTypes() {
    }

    public static void registerAll() {
        GameRegistry.registerTileEntity(TileEntityWarTechMachine.class,
                new ResourceLocation(MODID, "wartech_machine"));
        GameRegistry.registerTileEntity(TileEntityWarTechVisual.class,
                new ResourceLocation(MODID, "wartech_visual"));
        for (TileRegistration registration : LEGACY_REGISTRATIONS) {
            GameRegistry.registerTileEntity(registration.getTileClass(),
                    new ResourceLocation(MODID, registration.getRegistryPath()));
        }
    }

    public static List<TileRegistration> getLegacyRegistrations() {
        return LEGACY_REGISTRATIONS;
    }

    public static TileEntityWarTechMachine createMachine(String legacyBlockName) {
        String name = normalize(legacyBlockName);
        if (name.equals("launchtube")) return new VlsLaunchTube();
        if (name.equals("vlsexhaust")) return new VlsExhaust();
        if (name.equals("ballisticmissilelauncher")) return new BallisticLauncher();
        if (name.equals("geranlauncher")) return new GeranLauncher();
        if (name.equals("patriotlauncher")) return new PatriotLauncher();
        if (name.equals("s400launcher")) return new S400Launcher();
        if (name.equals("airraidsirenrelay")) return new AirRaidRelay();
        if (name.equals("longrangecommunicationmast")) return new CommunicationRelay();
        if (name.equals("strategicearlywarningradar")) return new StrategicRadar();
        return new TileEntityWarTechMachine();
    }

    public static TileEntityWarTechVisual createVisual(String legacyBlockName) {
        String name = normalize(legacyBlockName);
        if (name.equals("decoblockflagus")) return new DecoFlagUs();
        if (name.equals("decoblockflagsu")) return new DecoFlagSu();
        if (name.equals("decoblockflageu")) return new DecoFlagEu();
        if (name.equals("decoblockflagch")) return new DecoFlagCh();
        if (name.startsWith("decoblockcruisemissile")
                || name.startsWith("decoblocksupersonic")
                || name.startsWith("decoblockhypersonic")
                || name.startsWith("decoblocksatellite")) {
            return new DecoCruiseMissile();
        }
        return new TileEntityWarTechVisual();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private static TileRegistration machine(String legacyName,
            Class<? extends TileEntityWarTechMachine> tileClass) {
        return new TileRegistration(legacyName, tileClass);
    }

    private static TileRegistration visual(String legacyName,
            Class<? extends TileEntityWarTechVisual> tileClass) {
        return new TileRegistration(legacyName, tileClass);
    }

    public static final class TileRegistration {
        private final String legacyName;
        private final String registryPath;
        private final Class<? extends TileEntity> tileClass;

        private TileRegistration(String legacyName,
                Class<? extends TileEntity> tileClass) {
            this.legacyName = legacyName;
            this.registryPath = legacyName.toLowerCase(Locale.ROOT);
            this.tileClass = tileClass;
        }

        public String getLegacyName() { return legacyName; }
        public String getRegistryPath() { return registryPath; }
        public Class<? extends TileEntity> getTileClass() { return tileClass; }
    }

    private abstract static class LongRangeMachine extends TileEntityWarTechMachine {
        @Override public double getMaxRenderDistanceSquared() { return 65536.0D; }
    }

    public static final class VlsLaunchTube extends LongRangeMachine {
    }
    public static final class VlsExhaust
            extends com.wartec.wartecmod.tileentity.vls.TileEntityVlsExhaust {
    }
    public static final class BallisticLauncher extends LongRangeMachine {
    }
    public static final class GeranLauncher extends TileEntityWarTechMachine {
        @Override public double getMaxRenderDistanceSquared() { return 1600.0D; }
    }
    public static final class MobileGreg extends TileEntityWarTechMachine {
    }
    public static final class MobileHenry extends TileEntityWarTechMachine {
    }
    public static final class PatriotLauncher extends TileEntityWarTechMachine {
    }
    public static final class S400Launcher extends TileEntityWarTechMachine {
    }
    public static final class AirRaidRelay extends TileEntityWarTechMachine {
    }
    public static final class CommunicationRelay extends TileEntityWarTechMachine {
    }
    public static final class StrategicRadar extends TileEntityWarTechMachine {
        @Override public double getMaxRenderDistanceSquared() { return 262144.0D; }
    }

    private abstract static class LongRangeVisual extends TileEntityWarTechVisual {
        @Override public double getMaxRenderDistanceSquared() { return 65536.0D; }
    }

    public static final class DecoCruiseMissile extends LongRangeVisual {
    }
    public static final class DecoFlagUs extends LongRangeVisual {
    }
    public static final class DecoFlagSu extends LongRangeVisual {
    }
    public static final class DecoFlagEu extends LongRangeVisual {
    }
    public static final class DecoFlagCh extends LongRangeVisual {
    }
}
