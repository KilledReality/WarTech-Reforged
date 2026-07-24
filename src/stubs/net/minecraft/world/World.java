package net.minecraft.world;

import java.util.List;
import java.util.Random;
import net.minecraft.entity.Entity;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.chunk.Chunk;

public class World implements IBlockAccess {
    public boolean field_72995_K;
    public List field_72996_f;
    public List field_73010_i;
    public List field_147482_g;
    public Random field_73012_v;
    public long func_82737_E() { return 0L; }
    public int func_72805_g(int x, int y, int z) { return 0; }
    public int func_72976_f(int x, int z) { return 0; }
    public boolean func_72838_d(Entity entity) { return true; }
    public Entity func_73045_a(int id) { return null; }
    public List func_72839_b(Entity excluded, AxisAlignedBB box) { return null; }
    public List func_72945_a(Entity entity, AxisAlignedBB box) { return null; }
    public TileEntity func_147438_o(int x, int y, int z) { return null; }
    public void func_147455_a(int x, int y, int z, TileEntity tile) {}
    public void func_147475_p(int x, int y, int z) {}
    public Block func_147439_a(int x, int y, int z) { return null; }
    public Chunk func_72964_e(int x, int z) { return null; }
    public boolean func_147437_c(int x, int y, int z) { return true; }
    public boolean func_147465_d(int x, int y, int z, Block block, int metadata, int flags) { return true; }
    public boolean func_147468_f(int x, int y, int z) { return true; }
    public void func_72908_a(double x, double y, double z, String sound, float volume, float pitch) {}
    public void func_72956_a(Entity entity, String sound, float volume, float pitch) {}
    public void func_72869_a(String particle, double x, double y, double z,
            double velocityX, double velocityY, double velocityZ) {}
    public Explosion func_72885_a(Entity source, double x, double y, double z, float strength,
            boolean flaming, boolean damagesTerrain) { return null; }
}
