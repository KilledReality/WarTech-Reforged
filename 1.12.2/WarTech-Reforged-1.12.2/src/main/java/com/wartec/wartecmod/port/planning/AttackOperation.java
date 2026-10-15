package com.wartec.wartecmod.port.planning;

import java.util.*;
import net.minecraft.nbt.*;

/** Bounded, versioned server schedule. All times use the world's persistent tick clock. */
public final class AttackOperation {
    public static final int MAX_STEPS=256;
    public enum Action { PREPARE, LAUNCH, TAKEOFF, RELEASE, OBSERVE, TRANSIT, HOLD, RETURN }
    public enum Failure { WAIT, SKIP, STOP_BRANCH, STOP_ALL }
    public enum State { DRAFT, RUNNING, PAUSED, COMPLETE, PARTIAL, CANCELLED }
    public enum Phase { PENDING, WAITING, ACCEPTED, DONE, FAILED, SKIPPED, UNCERTAIN }
    public enum Event { ACCEPTED, ACTUAL, COMPLETE }
    public enum Condition { NONE, FRESH_INTEL, GROUP_READY, LINK_READY }
    public UUID id=UUID.randomUUID(), author;
    public String scope="",title="",reason="";
    public int revision;
    public long started=-1,pausedAt=-1,clockOffset;
    public double originX,originY,originZ;
    public State state=State.DRAFT;
    public final List<Step> steps=new ArrayList<>();
    public static final class Step {
        public UUID id=UUID.randomUUID();
        public String asset="",group="",reason="";
        public Action action=Action.LAUNCH;
        public Failure failure=Failure.WAIT;
        public Phase phase=Phase.PENDING;
        public Event event=Event.ACTUAL;
        public UUID after;
        public long offset,deadline=1200,accepted=-1,actual=-1,completed=-1,nextAttempt,pausedDelay,acceptedPause;
        public int x,y=64,z,slot=-1,count=1,interval=20,emitted;
        public boolean arrival;
        public Condition condition=Condition.NONE;
        public long intelAge=1200;
        public String conditionGroup="";
        public final Set<UUID> flights=new LinkedHashSet<>();
        public NBTTagList route=new NBTTagList();
        public boolean terminal() { return phase==Phase.DONE || phase==Phase.FAILED || phase==Phase.SKIPPED; }
        public long eventTime(Event requested) { return requested==Event.ACCEPTED?accepted:requested==Event.ACTUAL?actual:completed; }
        public NBTTagCompound write() {
            NBTTagCompound n=new NBTTagCompound();n.setUniqueId("Id",id);n.setString("Asset",asset);n.setString("Group",group);
            n.setString("Action",action.name());n.setString("Failure",failure.name());n.setString("Phase",phase.name());n.setString("Event",event.name());
            if(after!=null)n.setUniqueId("After",after);n.setLong("Offset",offset);n.setLong("Deadline",deadline);
            n.setLong("Accepted",accepted);n.setLong("Actual",actual);n.setLong("Completed",completed);n.setLong("Retry",nextAttempt);n.setLong("PausedDelay",pausedDelay);n.setLong("AcceptedPause",acceptedPause);
            n.setInteger("X",x);n.setInteger("Y",y);n.setInteger("Z",z);n.setInteger("Slot",slot);n.setInteger("Count",count);n.setInteger("Interval",interval);
            n.setBoolean("Arrival",arrival);n.setInteger("Emitted",emitted);n.setString("Reason",reason);n.setTag("Route",route.copy());
            n.setString("Condition",condition.name());n.setLong("IntelAge",intelAge);n.setString("ConditionGroup",conditionGroup);
            NBTTagList fs=new NBTTagList();for(UUID f:flights)fs.appendTag(new NBTTagString(f.toString()));n.setTag("Flights",fs);return n;
        }
        public static Step read(NBTTagCompound n) {
            Step s=new Step();if(n.hasUniqueId("Id"))s.id=n.getUniqueId("Id");s.asset=bounded(n.getString("Asset"),180);s.group=bounded(n.getString("Group"),40);
            s.action=value(Action.class,n.getString("Action"),Action.LAUNCH);s.failure=value(Failure.class,n.getString("Failure"),Failure.WAIT);
            s.phase=value(Phase.class,n.getString("Phase"),Phase.PENDING);s.event=value(Event.class,n.getString("Event"),Event.ACTUAL);
            s.after=n.hasUniqueId("After")?n.getUniqueId("After"):null;s.offset=n.getLong("Offset");s.deadline=n.hasKey("Deadline")?n.getLong("Deadline"):1200;
            s.accepted=n.hasKey("Accepted")?n.getLong("Accepted"):-1;s.actual=n.hasKey("Actual")?n.getLong("Actual"):-1;s.completed=n.hasKey("Completed")?n.getLong("Completed"):-1;s.nextAttempt=n.getLong("Retry");s.pausedDelay=n.getLong("PausedDelay");s.acceptedPause=n.getLong("AcceptedPause");
            s.x=n.getInteger("X");s.y=n.getInteger("Y");s.z=n.getInteger("Z");s.slot=n.hasKey("Slot")?n.getInteger("Slot"):-1;
            s.count=n.hasKey("Count")?n.getInteger("Count"):1;s.interval=n.hasKey("Interval")?n.getInteger("Interval"):20;s.arrival=n.getBoolean("Arrival");
            s.emitted=n.getInteger("Emitted");s.reason=bounded(n.getString("Reason"),240);
            s.condition=value(Condition.class,n.getString("Condition"),Condition.NONE);s.intelAge=n.hasKey("IntelAge")?n.getLong("IntelAge"):1200;s.conditionGroup=bounded(n.getString("ConditionGroup"),40);
            NBTTagList route=n.getTagList("Route",10);for(int i=0;i<Math.min(8,route.tagCount());i++){NBTTagCompound r=route.getCompoundTagAt(i),clean=new NBTTagCompound();for(String key:new String[]{"X","Y","Z","Hold"})clean.setInteger(key,r.getInteger(key));clean.setByte("Mode",r.getByte("Mode"));s.route.appendTag(clean);}
            NBTTagList fs=n.getTagList("Flights",8);for(int i=0;i<Math.min(64,fs.tagCount());i++)try{s.flights.add(UUID.fromString(fs.getStringTagAt(i)));}catch(IllegalArgumentException ignored){}return s;
        }
    }
    public Step step(UUID id) { for(Step s:steps)if(s.id.equals(id))return s;return null; }
    public String validate() {
        if(scope.isEmpty() || author==null)return "access";
        if(steps.isEmpty() || steps.size()>MAX_STEPS)return "steps";
        Set<UUID> ids=new HashSet<>();
        for(Step s:steps) {
            if(!ids.add(s.id))return "duplicate";
            if(s.asset.isEmpty() || Math.abs((long)s.x)>29999984 || Math.abs((long)s.z)>29999984 || s.y<0 || s.y>255)return "coordinates";
            if(s.count<1 || s.count>40 || s.slot< -1 || s.slot>63 || s.interval<1 || s.interval>72000 || s.offset<0 || s.offset>1728000 || s.deadline<1 || s.deadline>1728000)return "limits";
            if(s.intelAge<20 || s.intelAge>72000 || s.condition==Condition.GROUP_READY && s.conditionGroup.trim().isEmpty())return "condition";
            for(int i=0;i<s.route.tagCount();i++){NBTTagCompound p=s.route.getCompoundTagAt(i);if(Math.abs((long)p.getInteger("X"))>29999984 || Math.abs((long)p.getInteger("Z"))>29999984 || p.getInteger("Y")<1 || p.getInteger("Y")>255)return "coordinates";if(p.getByte("Mode")<0 || p.getByte("Mode")>1 || p.getInteger("Hold")<0 || p.getInteger("Hold")>72000)return "limits";}
        }
        for(Step s:steps) {
            Set<UUID> path=new HashSet<>();Step p=s;
            while(p.after!=null) { if(!path.add(p.id))return "cycle";p=step(p.after);if(p==null)return "dependency"; }
        }
        return "";
    }
    public boolean dependsOn(Step s,UUID ancestor) {
        Set<UUID> visited=new HashSet<>();while(s!=null && s.after!=null && visited.add(s.id)){if(s.after.equals(ancestor))return true;s=step(s.after);}return false;
    }
    public NBTTagCompound write() {
        NBTTagCompound n=new NBTTagCompound();n.setUniqueId("Id",id);if(author!=null)n.setUniqueId("Author",author);n.setString("Scope",scope);n.setString("Title",title);
        n.setInteger("Revision",revision);n.setLong("Started",started);n.setLong("PausedAt",pausedAt);n.setLong("ClockOffset",clockOffset);n.setString("State",state.name());n.setString("Reason",reason);
        n.setDouble("OriginX",originX);n.setDouble("OriginY",originY);n.setDouble("OriginZ",originZ);
        NBTTagList list=new NBTTagList();for(Step s:steps)list.appendTag(s.write());n.setTag("Steps",list);return n;
    }
    public static AttackOperation read(NBTTagCompound n) {
        AttackOperation o=new AttackOperation();if(n.hasUniqueId("Id"))o.id=n.getUniqueId("Id");o.author=n.hasUniqueId("Author")?n.getUniqueId("Author"):null;
        o.scope=bounded(n.getString("Scope"),100);o.title=bounded(n.getString("Title"),64);o.reason=bounded(n.getString("Reason"),240);
        o.revision=n.getInteger("Revision");o.started=n.hasKey("Started")?n.getLong("Started"):-1;o.clockOffset=n.getLong("ClockOffset");o.pausedAt=n.hasKey("PausedAt")?n.getLong("PausedAt"):-1;o.state=value(State.class,n.getString("State"),State.DRAFT);
        o.originX=n.getDouble("OriginX");o.originY=n.getDouble("OriginY");o.originZ=n.getDouble("OriginZ");
        NBTTagList rows=n.getTagList("Steps",10);for(int i=0;i<Math.min(MAX_STEPS,rows.tagCount());i++)o.steps.add(Step.read(rows.getCompoundTagAt(i)));return o;
    }
    public void resumeClock(long now){if(pausedAt>=0){long delta=Math.max(0,now-pausedAt);clockOffset+=delta;for(Step step:steps){Step parent=step.after==null?null:step(step.after);if(parent!=null && !step.terminal() && step.phase!=Phase.ACCEPTED && parent.eventTime(step.event)>=0)step.pausedDelay+=Math.max(0,now-Math.max(pausedAt,parent.eventTime(step.event)));if(step.phase==Phase.ACCEPTED)step.acceptedPause+=delta;if(step.nextAttempt>0)step.nextAttempt+=delta;}}pausedAt=-1;}
    public AttackOperation template() {
        AttackOperation c=read(write());c.id=UUID.randomUUID();c.revision=0;c.started=-1;c.pausedAt=-1;c.clockOffset=0;c.state=State.DRAFT;c.reason="";
        Map<UUID,UUID> ids=new HashMap<>();for(Step s:c.steps){UUID old=s.id;s.id=UUID.randomUUID();ids.put(old,s.id);s.phase=Phase.PENDING;s.accepted=s.actual=s.completed=-1;s.emitted=0;s.flights.clear();s.reason="";s.nextAttempt=0;s.pausedDelay=s.acceptedPause=0;}
        for(Step s:c.steps)if(s.after!=null)s.after=ids.get(s.after);return c;
    }
    public static String bounded(String s,int max) { return s.length()>max?s.substring(0,max):s; }
    private static <E extends Enum<E>> E value(Class<E> type,String s,E fallback) { try{return Enum.valueOf(type,s);}catch(IllegalArgumentException e){return fallback;} }
}
