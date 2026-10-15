package com.wartec.wartecmod.port.content;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
public class SiloDecorBlock extends Block {
    public SiloDecorBlock(boolean light){super(Material.IRON);setRegistryName(PortItem.MOD_ID,light?"silolight":"silodeck");setUnlocalizedName(light?"SiloLight":"SiloDeck");setCreativeTab(WarTechCreativeTabs.GEAR);setHardness(5);setResistance(30);setSoundType(SoundType.METAL);if(light)setLightLevel(.9F);}
}
