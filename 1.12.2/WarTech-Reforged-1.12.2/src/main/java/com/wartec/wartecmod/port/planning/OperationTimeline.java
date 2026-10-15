package com.wartec.wartecmod.port.planning;

import java.util.*;

/** Preview only: actual acceptance/emission timestamps always take precedence. */
public final class OperationTimeline {
    private OperationTimeline(){}
    public static long start(AttackOperation operation,AttackOperation.Step step,Map<UUID,Long> duration,Map<UUID,Long> flight){return start(operation,step,duration,flight,new HashSet<>());}
    private static long start(AttackOperation o,AttackOperation.Step s,Map<UUID,Long> duration,Map<UUID,Long> flight,Set<UUID> path){
        if(!path.add(s.id))return 0;
        if(o.started>=0 && s.accepted>=0)return Math.max(0,s.accepted-o.started);
        AttackOperation.Step parent=s.after==null?null:o.step(s.after);long base=o.clockOffset;
        if(parent!=null){long actual=parent.eventTime(s.event);base=actual>=0 && o.started>=0?actual-o.started:start(o,parent,duration,flight,path)+(s.event==AttackOperation.Event.ACCEPTED?0:duration.getOrDefault(parent.id,20L));}
        return Math.max(0,base+s.offset+s.pausedDelay-(s.arrival?flight.getOrDefault(s.id,0L):0));
    }
    public static long duration(AttackOperation.Step s){
        if(s.action==AttackOperation.Action.PREPARE)return 120;
        if(s.action==AttackOperation.Action.TAKEOFF)return 100;
        if(s.action==AttackOperation.Action.OBSERVE)return 400;
        if(s.action==AttackOperation.Action.RETURN || s.action==AttackOperation.Action.TRANSIT)return 200;
        return Math.max(20,(long)Math.max(0,s.count-1)*s.interval+20);
    }
    public static Set<UUID> conflicts(AttackOperation o,Map<UUID,Long> duration,Map<UUID,Long> flight){
        Set<UUID> out=new HashSet<>();for(int i=0;i<o.steps.size();i++)for(int j=i+1;j<o.steps.size();j++){AttackOperation.Step a=o.steps.get(i),b=o.steps.get(j);if(!a.asset.equals(b.asset) || o.dependsOn(a,b.id) || o.dependsOn(b,a.id))continue;long ax=start(o,a,duration,flight),bx=start(o,b,duration,flight);if(ax<bx+duration.getOrDefault(b.id,duration(b)) && bx<ax+duration.getOrDefault(a.id,duration(a))){out.add(a.id);out.add(b.id);}}return out;
    }
}
