package com.wartec.wartecmod.port.client;

import net.minecraft.client.renderer.block.model.ItemCameraTransforms;

final class LegacyItemRenderContext {
    private static final ThreadLocal<ItemCameraTransforms.TransformType> TYPE =
            new ThreadLocal<>();

    private LegacyItemRenderContext() {
    }

    static void set(ItemCameraTransforms.TransformType type) {
        TYPE.set(type);
    }

    static ItemCameraTransforms.TransformType consume() {
        ItemCameraTransforms.TransformType type = TYPE.get();
        TYPE.remove();
        return type == null
                ? ItemCameraTransforms.TransformType.GUI : type;
    }
}
