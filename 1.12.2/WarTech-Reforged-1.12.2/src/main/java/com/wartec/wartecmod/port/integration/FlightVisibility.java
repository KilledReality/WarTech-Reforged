package com.wartec.wartecmod.port.integration;

import com.wartec.wartecmod.port.cruise.CruiseNavigation;
import java.util.function.BiPredicate;
import net.minecraft.util.math.Vec3d;

/** Clip a navigation probe at the first unknown chunk, before any block ray trace. */
public final class FlightVisibility {
    private FlightVisibility() { }
    public static Vec3d knownEnd(Vec3d from,Vec3d to,BiPredicate<Integer,Integer> loaded) {
        Vec3d delta=to.subtract(from);double length=Math.sqrt(delta.lengthSquared());
        if(!Double.isFinite(length) || length>512) return null;
        double[] edge={1};boolean[] missing={false};
        boolean full=CruiseNavigation.loadedRay(from,to,(x,z)-> {
            if(loaded.test(x,z)) return true;
            missing[0]=true;
            double tx=entry(from.x,delta.x,x),tz=entry(from.z,delta.z,z);
            edge[0]=Math.max(0,Math.max(tx,tz));return false;
        });
        if(full) return to;
        if(!missing[0] || edge[0]<=0) return null;
        return from.add(delta.scale(Math.max(0,Math.min(1,edge[0])-0.01/Math.max(.01,length))));
    }
    private static double entry(double start,double delta,int chunk) {
        if(Math.abs(delta)<1e-12) return Double.NEGATIVE_INFINITY;
        double a=(chunk*16.0-start)/delta,b=((chunk+1)*16.0-start)/delta;
        return Math.min(a,b);
    }
}
