package com.wartec.wartecmod.port.entity;

/** One pose for the launcher and detached rocket, including its original scale and skin. */
public final class TelLaunchGeometry {
    private TelLaunchGeometry() { }
    public static int missileNode(AnimatedAssetScene scene,StrategicSystemProfile system,int slot) {
        String prefix=system.isTactical()?(slot==1?"Missile_R":"Missile_L"):"Missile";
        for(int i=0;i<scene.nodes.length;i++) if(scene.nodes[i].name.startsWith(prefix)) return i;
        throw new IllegalArgumentException("No rocket in "+system.getId());
    }
    public static AnimatedAssetScene.Pose pose(AnimatedAssetScene scene,StrategicSystemProfile system,float deployment,float launch,int slot) {
        AnimatedAssetScene.Pose pose=scene.pose();
        pose.apply(system.isTactical()?"Prepare":"Raise",deployment*(system.isTactical()?5.25F:5F),n->true);
        if(launch>0) {
            String prefix=system.isTactical()?(slot==1?"Missile_R":"Missile_L"):"Missile";
            pose.apply(system.isTactical()?(slot==1?"Launch_R":"Launch_L"):"Launch",
                (system.isTactical()?0:5)+Math.max(0,launch-1)/20F,
                n->n.name.startsWith(prefix)||!system.isTactical()&&"Hatch_Pivot".equals(n.name));
            int rocket=missileNode(scene,system,slot);
            pose.translation[rocket][2]=scene.nodes[rocket].translation[2]+(float)system.launchTravel(launch);
        }
        return pose;
    }
}
