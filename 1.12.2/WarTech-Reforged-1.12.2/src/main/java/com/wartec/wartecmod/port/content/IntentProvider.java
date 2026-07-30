package com.wartec.wartecmod.port.content;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

/**
 * Entity-free contract between registered content and the gameplay port.
 */
public interface IntentProvider {
    String TAG_ROOT = "WarTechPortIntent";
    String TAG_KIND = "Kind";
    String TAG_ID = "Id";
    String TAG_VARIANT = "Variant";

    IntentKind getIntentKind();

    ResourceLocation getIntentId(ItemStack stack);

    int getIntentVariant(ItemStack stack);

    default NBTTagCompound createIntentTag(ItemStack stack) {
        NBTTagCompound intent = new NBTTagCompound();
        intent.setString(TAG_KIND, getIntentKind().getSerializedName());
        intent.setString(TAG_ID, getIntentId(stack).toString());
        intent.setInteger(TAG_VARIANT, getIntentVariant(stack));
        return intent;
    }

    default ItemStack writeIntent(ItemStack stack) {
        NBTTagCompound root = stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();
        root.setTag(TAG_ROOT, createIntentTag(stack));
        stack.setTagCompound(root);
        return stack;
    }

    enum IntentKind {
        DEPLOYMENT("deployment"),
        MISSILE("missile"),
        ORDNANCE("ordnance"),
        SATELLITE("satellite"),
        TOOL("tool");

        private final String serializedName;

        IntentKind(String serializedName) {
            this.serializedName = serializedName;
        }

        public String getSerializedName() {
            return serializedName;
        }
    }
}
