package com.wartec.wartecmod.port.cruise;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.math.Vec3d;

/** Launcher-owned twin guide rails, two saddles and triangulated pedestal braces. */
public final class CruiseLaunchCradle {
    private CruiseLaunchCradle() { }
    public static final class Beam {
        public final Vec3d a,b;public final double width;
        Beam(Vec3d a,Vec3d b,double width) { this.a=a;this.b=b;this.width=width; }
    }
    public static List<Beam> beams(CruiseBuild build) {
        List<Beam> out=new ArrayList<>();double scale=CruiseAirframes.modelScale(build.getAirframe());
        int family=CruiseVisuals.family(build.getAirframe());
        double bottom=new double[]{-.1033673,-.4599,-.44051605,-.21616401}[family]*scale;
        double belly=new double[]{-.08,-.20,-.20,-.15}[family]*scale;
        double railY=bottom-.13,z0=-CruiseVisuals.engineEnd(build)*scale+.16,z1=CruiseVisuals.noseOffset(build)*.65;
        float pitch=build.get(CruiseSlot.LAUNCH)==CruisePartDefinition.LAUNCH_BOOSTER?-65:-12;
        Vec3d origin=CruiseVisuals.launchOrigin(build);
        for(double side:new double[]{-.28,.28}) {
            Vec3d tail=at(origin,new Vec3d(side,railY,z0),pitch),head=at(origin,new Vec3d(side,railY,z1),pitch);
            out.add(new Beam(tail,head,.075));
            for(double z:new double[]{-.55*scale,.65*scale})
                out.add(new Beam(at(origin,new Vec3d(side,railY,z),pitch),at(origin,new Vec3d(side,belly,z),pitch),.07));
            out.add(new Beam(new Vec3d(side,1,0),at(origin,new Vec3d(side,railY,.65*scale),pitch),.09));
            Vec3d foot=new Vec3d(side*2.4,.10,Math.min(-.65,tail.z));
            out.add(new Beam(foot,tail,.09));
            out.add(new Beam(new Vec3d(side*2.4,.10,.65),foot,.10));
            out.add(new Beam(new Vec3d(side*2.4,.10,.65),new Vec3d(side,1,0),.09));
        }
        for(double z:new double[]{z0,-.55*scale,.65*scale,z1})
            out.add(new Beam(at(origin,new Vec3d(-.28,railY,z),pitch),at(origin,new Vec3d(.28,railY,z),pitch),.065));
        out.add(new Beam(new Vec3d(-.72,.10,.65),new Vec3d(.72,.10,.65),.10));return out;
    }
    private static Vec3d at(Vec3d origin,Vec3d local,float pitch) { return origin.add(CruiseVisuals.worldOffset(local,0,pitch)); }
}
