package com.wartec.wartecmod.port;

import com.wartec.wartecmod.port.content.WarTechContent;
import com.wartec.wartecmod.port.integration.*;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.util.math.*;
import org.junit.*;
import static org.junit.Assert.*;

public class RemoteOperationsTest {
    @BeforeClass public static void bootstrap() { Bootstrap.register(); }
    @Test public void allThreeHorizontalFormatsRemainValidWithoutY() {
        for(String[] keys:new String[][]{{"xCoord","zCoord"},{"x","z"},{"targetX","targetZ"}}) {
            ItemStack item=new ItemStack(WarTechContent.ITEM_TARGET_FINDER);NBTTagCompound n=new NBTTagCompound();
            n.setDouble(keys[0],-769.5);n.setDouble(keys[1],738);item.setTagCompound(n);
            assertEquals(new Vec3d(-769.5,0,738),DesignatorCompat.getHorizontalTarget(item));
            assertNull(DesignatorCompat.getSavedTarget(item));
            n.setDouble(keys[0],Double.NaN);assertNull(DesignatorCompat.getHorizontalTarget(item));
        }
    }
    @Test public void defenseIndexPersistsUnloadedPositionsAndRejectsMalformedIds() {
        NBTTagCompound n=new NBTTagCompound();NBTTagList nodes=new NBTTagList();
        for(String id:new String[]{"00000000-0000-0000-0000-000000000073","not-a-uuid"}) {
            NBTTagCompound node=new NBTTagCompound();node.setString("Id",id);node.setBoolean("Entity",true);
            node.setLong("Pos",new BlockPos(-50000,4,60000).toLong());node.setInteger("Range",220);nodes.appendTag(node);
        }n.setTag("Nodes",nodes);OperationalChunks.Index index=new OperationalChunks.Index();index.readFromNBT(n);
        NBTTagCompound saved=index.writeToNBT(new NBTTagCompound());assertEquals(1,saved.getTagList("Nodes",10).tagCount());
        assertEquals(new BlockPos(-50000,4,60000).toLong(),saved.getTagList("Nodes",10).getCompoundTagAt(0).getLong("Pos"));
        OperationalChunks.Index copy=new OperationalChunks.Index();copy.readFromNBT(saved);assertEquals(saved,copy.writeToNBT(new NBTTagCompound()));
    }
}
