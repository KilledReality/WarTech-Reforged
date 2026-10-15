package com.wartec.wartecmod.port.client;

import com.wartec.wartecmod.port.entity.LegacySamGeometry;
import com.wartec.wartecmod.port.gameplay.TileEntityWarTechMachine;

final class LegacySamRenderer {
    private LegacySamRenderer() { }
    static void render(boolean patriot,TileEntityWarTechMachine tile,float partial) {
        AnimatedModernModel model=AnimatedModernModel.get(patriot?"patriot_rig":"s400_rig");
        float deployment=tile==null?1:tile.getSamDeployment(partial),yaw=tile==null?0:tile.getSamYaw(partial);
        model.render(LegacySamGeometry.pose(model.scene,patriot,deployment,yaw,tile==null?-1:tile.getSamChannel(),
            tile==null?0:tile.getSamCap(partial)),name->true);
    }
}
