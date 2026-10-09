package com.wartec.wartecmod.port.network;

import com.wartec.wartecmod.WarTechReforged;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public final class WarTechNetwork {
    public static final SimpleNetworkWrapper CHANNEL =
            NetworkRegistry.INSTANCE.newSimpleChannel(WarTechReforged.MODID);

    private WarTechNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(CruiseVisualEventMessage.Handler.class,CruiseVisualEventMessage.class,20,Side.CLIENT);
        CHANNEL.registerMessage(CruiseLauncherActionMessage.Handler.class,CruiseLauncherActionMessage.class,19,Side.SERVER);
        CHANNEL.registerMessage(CruiseFabricatorActionMessage.Handler.class,
                CruiseFabricatorActionMessage.class, 17, Side.SERVER);
        CHANNEL.registerMessage(CruiseMissionEditMessage.Handler.class,
                CruiseMissionEditMessage.class, 18, Side.SERVER);
        CHANNEL.registerMessage(
                VehicleInputMessage.Handler.class,
                VehicleInputMessage.class,
                0,
                Side.SERVER
        );
        CHANNEL.registerMessage(
                FactionCommandRequestMessage.Handler.class,
                FactionCommandRequestMessage.class,
                1,
                Side.SERVER
        );
        CHANNEL.registerMessage(
                FactionCommandSnapshotMessage.Handler.class,
                FactionCommandSnapshotMessage.class,
                2,
                Side.CLIENT
        );
        CHANNEL.registerMessage(
                RemoteControlInputMessage.Handler.class,
                RemoteControlInputMessage.class,
                3,
                Side.SERVER
        );
        CHANNEL.registerMessage(
                RemoteControlStateMessage.Handler.class,
                RemoteControlStateMessage.class,
                4,
                Side.CLIENT
        );
        CHANNEL.registerMessage(
                RemoteControlTelemetryMessage.Handler.class,
                RemoteControlTelemetryMessage.class,
                5,
                Side.CLIENT
        );
        CHANNEL.registerMessage(
                ArtilleryWhitelistMessage.Handler.class,
                ArtilleryWhitelistMessage.class,
                6,
                Side.SERVER
        );
        CHANNEL.registerMessage(
                LegacyMushroomEffectMessage.Handler.class,
                LegacyMushroomEffectMessage.class,
                7,
                Side.CLIENT
        );
        CHANNEL.registerMessage(
                LegacyKeroseneTrailMessage.Handler.class,
                LegacyKeroseneTrailMessage.class,
                8,
                Side.CLIENT
        );
        CHANNEL.registerMessage(
                VehicleStateMessage.Handler.class,
                VehicleStateMessage.class,
                9,
                Side.CLIENT
        );
        CHANNEL.registerMessage(
                RemoteOperatorVisibilityMessage.Handler.class,
                RemoteOperatorVisibilityMessage.class,
                10,
                Side.CLIENT
        );
        CHANNEL.registerMessage(
                UavMissionEditMessage.Handler.class,
                UavMissionEditMessage.class,
                11,
                Side.SERVER
        );
        CHANNEL.registerMessage(
                UavReconReportMessage.Handler.class,
                UavReconReportMessage.class,
                12,
                Side.CLIENT
        );
        CHANNEL.registerMessage(
                UavFleetRequestMessage.Handler.class,
                UavFleetRequestMessage.class,
                13,
                Side.SERVER
        );
        CHANNEL.registerMessage(
                UavFleetSnapshotMessage.Handler.class,
                UavFleetSnapshotMessage.class,
                14,
                Side.CLIENT
        );
        CHANNEL.registerMessage(
                UavFleetActionMessage.Handler.class,
                UavFleetActionMessage.class,
                15,
                Side.SERVER
        );
        CHANNEL.registerMessage(
                UavFabricatorActionMessage.Handler.class,
                UavFabricatorActionMessage.class,
                16,
                Side.SERVER
        );
    }
}
