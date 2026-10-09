package com.wartec.wartecmod.port.content;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Late-bound bridge for gameplay code. Registration remains independent of
 * entity implementations while the main port can install one handler.
 */
public final class ContentHooks {
    private static final IntentHandler PASS_HANDLER = context -> EnumActionResult.PASS;
    private static volatile IntentHandler handler = PASS_HANDLER;

    private ContentHooks() {
    }

    public static void setHandler(IntentHandler newHandler) {
        handler = newHandler == null ? PASS_HANDLER : newHandler;
    }

    public static void clearHandler() {
        handler = PASS_HANDLER;
    }

    public static EnumActionResult dispatch(
        IntentProvider provider,
        ItemStack stack,
        World world,
        EntityPlayer player,
        EnumHand hand,
        BlockPos pos,
        EnumFacing facing
    ) {
        if (!stack.isEmpty() && StrategicFeature.isDisabledItem(stack.getItem())) {
            return EnumActionResult.FAIL;
        }
        return handler.handle(new IntentContext(
            provider,
            stack,
            provider.createIntentTag(stack),
            world,
            player,
            hand,
            pos,
            facing
        ));
    }

    public interface IntentHandler {
        EnumActionResult handle(IntentContext context);
    }

    public static final class IntentContext {
        private final IntentProvider provider;
        private final ItemStack stack;
        private final NBTTagCompound intent;
        private final World world;
        private final EntityPlayer player;
        private final EnumHand hand;
        private final BlockPos pos;
        private final EnumFacing facing;

        private IntentContext(
            IntentProvider provider,
            ItemStack stack,
            NBTTagCompound intent,
            World world,
            EntityPlayer player,
            EnumHand hand,
            BlockPos pos,
            EnumFacing facing
        ) {
            this.provider = provider;
            this.stack = stack;
            this.intent = intent;
            this.world = world;
            this.player = player;
            this.hand = hand;
            this.pos = pos;
            this.facing = facing;
        }

        public IntentProvider getProvider() {
            return provider;
        }

        public ItemStack getStack() {
            return stack;
        }

        public NBTTagCompound getIntent() {
            return intent.copy();
        }

        public World getWorld() {
            return world;
        }

        public EntityPlayer getPlayer() {
            return player;
        }

        public EnumHand getHand() {
            return hand;
        }

        public BlockPos getPos() {
            return pos;
        }

        public EnumFacing getFacing() {
            return facing;
        }
    }
}
