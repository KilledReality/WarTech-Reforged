package com.wartec.wartecmod.port.client;

import com.wartec.wartecmod.WarTechReforged;
import com.wartec.wartecmod.port.content.HimarsAmmoItem;
import com.wartec.wartecmod.port.content.LegacyDecorationBlock;
import com.wartec.wartecmod.port.content.LegacyLauncherBlock;
import com.wartec.wartecmod.port.content.PortItem;
import com.wartec.wartecmod.port.content.VariantItem;
import com.wartec.wartecmod.port.content.UavPartItem;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMap;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = WarTechReforged.MODID, value = Side.CLIENT)
public final class ClientModelEvents {
    private static final Set<ModelResourceLocation> CUSTOM_MODELS =
            new HashSet<>();

    private ClientModelEvents() {
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        for (Block block : ForgeRegistries.BLOCKS.getValuesCollection()) {
            ResourceLocation name = block.getRegistryName();
            if (name == null
                    || !WarTechReforged.MODID.equals(name.getResourceDomain())) {
                continue;
            }
            if (block instanceof LegacyLauncherBlock) {
                ModelLoader.setCustomStateMapper(block, new StateMap.Builder()
                        .ignore(LegacyLauncherBlock.META).build());
            } else if (block instanceof LegacyDecorationBlock) {
                ModelLoader.setCustomStateMapper(block, new StateMap.Builder()
                        .ignore(LegacyDecorationBlock.FACING).build());
            }
        }
        for (Item item : ForgeRegistries.ITEMS.getValuesCollection()) {
            ResourceLocation name = item.getRegistryName();
            if (name != null && WarTechReforged.MODID.equals(name.getResourceDomain())) {
                if(item==com.wartec.wartecmod.port.content.WarTechContent.CRUISE_BLUEPRINT)
                    item.addPropertyOverride(new ResourceLocation(WarTechReforged.MODID,"saved"),(stack,world,entity)->com.wartec.wartecmod.port.cruise.CruiseBuild.fromStack(stack).getAirframe()==null?0:1);
                if(item==com.wartec.wartecmod.port.content.WarTechContent.UAV_BLUEPRINT)
                    item.addPropertyOverride(new ResourceLocation(WarTechReforged.MODID,"saved"),(stack,world,entity)->com.wartec.wartecmod.port.uav.UavBuild.fromStack(stack).getAirframe()==null?0:1);
                boolean custom =
                        LegacyRenderLibrary.isCustomItem(name.getResourcePath());
                if (custom) {
                    item.setTileEntityItemStackRenderer(WarTechItemStackRenderer.INSTANCE);
                }
                if (item instanceof com.wartec.wartecmod.port.content.CruisePartItem) {
                    for(com.wartec.wartecmod.port.cruise.CruisePartDefinition part:com.wartec.wartecmod.port.cruise.CruisePartDefinition.values()) {
                        ModelLoader.setCustomModelResourceLocation(item,part.ordinal(),
                            new ModelResourceLocation(new ResourceLocation(WarTechReforged.MODID,"cruisemodule_"+part.getId()),"inventory"));
                    }
                } else if (item instanceof UavPartItem) {
                    UavPartItem partItem = (UavPartItem) item;
                    for (int metadata = 0;
                            metadata < partItem.getVariantCount(); ++metadata) {
                        ModelLoader.setCustomModelResourceLocation(item, metadata,
                                new ModelResourceLocation(
                                        new ResourceLocation(WarTechReforged.MODID,
                                                "uavmodule_" + PortItem.safePath(
                                                        partItem.getVariantName(metadata))),
                                        "inventory"));
                    }
                } else if (item instanceof HimarsAmmoItem) {
                    for (int metadata = 0;
                            metadata < ((HimarsAmmoItem) item).getVariantCount();
                            metadata++) {
                        ModelLoader.setCustomModelResourceLocation(item, metadata,
                                track(new ModelResourceLocation(
                                        name, "inventory"), custom));
                    }
                } else if (item instanceof VariantItem
                    && ((VariantItem) item).getVariantCount() > 1) {
                    VariantItem variantItem = (VariantItem) item;
                    for (int metadata = 0;
                         metadata < variantItem.getVariantCount();
                         metadata++) {
                        String modelPath = name.getResourcePath()
                            + "_"
                            + PortItem.safePath(variantItem.getVariantName(metadata));
                        ModelLoader.setCustomModelResourceLocation(
                            item,
                            metadata,
                            track(new ModelResourceLocation(
                                new ResourceLocation(WarTechReforged.MODID, modelPath),
                                "inventory"
                            ), custom)
                        );
                    }
                } else {
                    ModelLoader.setCustomModelResourceLocation(
                        item,
                        0,
                        track(new ModelResourceLocation(name, "inventory"),
                                custom)
                    );
                }
            }
        }
    }

    @SubscribeEvent
    public static void bakeModels(ModelBakeEvent event) {
        LegacyRenderLibrary.reloadPreviews();
        for (ModelResourceLocation location : CUSTOM_MODELS) {
            IBakedModel model = event.getModelRegistry().getObject(location);
            if (model != null && !(model instanceof PerspectiveAwareItemModel)) {
                event.getModelRegistry().putObject(location,
                        new PerspectiveAwareItemModel(model));
            }
        }
        LegacyRenderLibrary.warmItemPreviews();
    }

    private static ModelResourceLocation track(ModelResourceLocation location,
            boolean custom) {
        if (custom) {
            CUSTOM_MODELS.add(location);
        }
        return location;
    }
}
