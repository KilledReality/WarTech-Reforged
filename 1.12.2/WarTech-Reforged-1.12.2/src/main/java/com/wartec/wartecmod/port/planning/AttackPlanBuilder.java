package com.wartec.wartecmod.port.planning;

import java.util.*;
import net.minecraft.nbt.*;

/** Builds ordinary editable steps from actual stores; never creates ammunition or dispatches orders. */
public final class AttackPlanBuilder {
    private AttackPlanBuilder() { }
    public static List<NBTTagCompound> weapons(NBTTagCompound asset, AttackOperation operation) {
        Map<String,NBTTagCompound> grouped=new LinkedHashMap<>();
        NBTTagList stores=asset.getTagList("Stores",10);
        for(int i=0;i<stores.tagCount();i++){
            NBTTagCompound store=stores.getCompoundTagAt(i);
            if(!store.getBoolean("Launchable"))continue;
            int available=store.getInteger("Count")-reserved(operation,asset.getString("Key"),store.getInteger("Slot"));
            if(available<=0)continue;
            String key=store.getString("Item")+":"+store.getInteger("Meta")+(store.getString("Signature").isEmpty()?"":":"+store.getString("Signature"));
            NBTTagCompound group=grouped.get(key);
            if(group==null){group=store.copy();group.setString("WeaponKey",key);group.setInteger("Available",0);group.setTag("Choices",new NBTTagList());grouped.put(key,group);}
            group.setInteger("Available",group.getInteger("Available")+available);
            NBTTagCompound choice=store.copy();choice.setInteger("Available",available);group.getTagList("Choices",10).appendTag(choice);
        }
        if(asset.getBoolean("OneWay") && asset.getInteger("Ammo")>reserved(operation,asset.getString("Key"),-1)){
            NBTTagCompound self=new NBTTagCompound();self.setString("WeaponKey","self");self.setString("Name","planning.wizard.warhead");self.setInteger("Available",1);self.setInteger("Slot",-1);
            NBTTagList choices=new NBTTagList();NBTTagCompound choice=self.copy();choices.appendTag(choice);self.setTag("Choices",choices);grouped.put("self",self);
        }
        return new ArrayList<>(grouped.values());
    }
    private static int reserved(AttackOperation operation,String key,int slot){
        int total=0;for(AttackOperation.Step step:operation.steps)if(step.asset.equals(key) && (step.action==AttackOperation.Action.LAUNCH || step.action==AttackOperation.Action.RELEASE) && (step.slot==slot || step.slot<0))total+=step.count;return total;
    }
    public static List<AttackOperation.Step> build(AttackOperation operation,NBTTagCompound asset,String weaponKey,int quantity,int x,int y,int z,long delay,int interval,boolean returnHome){
        if(operation.state!=AttackOperation.State.DRAFT)throw new IllegalArgumentException("draft_only");
        if(Math.abs((long)x)>29999984 || Math.abs((long)z)>29999984 || y<0 || y>255)throw new IllegalArgumentException("error.coordinates");
        if(quantity<1 || quantity>40 || delay<0 || delay>1728000 || interval<1 || interval>72000)throw new IllegalArgumentException("error.limits");
        NBTTagCompound weapon=null;for(NBTTagCompound candidate:weapons(asset,operation))if(weaponKey.equals(candidate.getString("WeaponKey")))weapon=candidate;
        if(weapon==null || quantity>weapon.getInteger("Available"))throw new IllegalArgumentException("error.empty");
        String actions=","+asset.getString("Actions")+",";boolean launch=actions.contains(",LAUNCH,"),release=actions.contains(",RELEASE,");
        if(!launch && !release)throw new IllegalArgumentException("error.unsupported");
        List<AttackOperation.Step> result=new ArrayList<>();AttackOperation.Step previous=null;
        for(AttackOperation.Step step:operation.steps)if(step.asset.equals(asset.getString("Key")))previous=step;
        boolean alreadyTakingOff=false;for(AttackOperation.Step step:operation.steps)if(step.asset.equals(asset.getString("Key")) && step.action==AttackOperation.Action.TAKEOFF)alreadyTakingOff=true;
        if(alreadyTakingOff && previous!=null && previous.action==AttackOperation.Action.RETURN)throw new IllegalArgumentException("wizard.returned");
        List<AttackOperation.Action> orders=new ArrayList<>();
        if(actions.contains(",PREPARE,"))orders.add(AttackOperation.Action.PREPARE);
        if(!asset.getBoolean("Airborne") && !alreadyTakingOff && actions.contains(",TAKEOFF,"))orders.add(AttackOperation.Action.TAKEOFF);
        NBTTagList choices=weapon.getTagList("Choices",10);List<Integer> slots=new ArrayList<>();
        for(int i=0;i<choices.tagCount() && slots.size()<quantity;i++){NBTTagCompound choice=choices.getCompoundTagAt(i);for(int j=0;j<choice.getInteger("Available") && slots.size()<quantity;j++)slots.add(choice.getInteger("Slot"));}
        for(AttackOperation.Action action:orders){AttackOperation.Step step=step(asset,action,x,y,z);step.slot=action==AttackOperation.Action.TAKEOFF?slots.get(0):-1;link(step,previous,result.isEmpty()?delay:0);result.add(step);previous=step;}
        for(int i=0;i<quantity;i++){AttackOperation.Step step=step(asset,launch?AttackOperation.Action.LAUNCH:AttackOperation.Action.RELEASE,x,y,z);step.slot=slots.get(i);step.interval=interval;link(step,previous,result.isEmpty()?delay:i==0?0:interval);result.add(step);previous=step;}
        if(returnHome && release && !asset.getBoolean("OneWay") && actions.contains(",RETURN,")){AttackOperation.Step step=step(asset,AttackOperation.Action.RETURN,x,y,z);link(step,previous,0);result.add(step);}
        if(operation.steps.size()+result.size()>AttackOperation.MAX_STEPS)throw new IllegalArgumentException("error.steps");
        return result;
    }
    private static AttackOperation.Step step(NBTTagCompound asset,AttackOperation.Action action,int x,int y,int z){AttackOperation.Step s=new AttackOperation.Step();s.asset=asset.getString("Key");s.action=action;s.x=x;s.y=y;s.z=z;s.deadline=12000;return s;}
    private static void link(AttackOperation.Step step,AttackOperation.Step previous,long offset){step.after=previous==null?null:previous.id;step.event=AttackOperation.Event.COMPLETE;step.offset=offset;}
}
