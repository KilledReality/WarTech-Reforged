package com.wartec.wartecmod.port.planning;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldSavedData;

public final class PlanningWorldData extends WorldSavedData {
    private static final String NAME="wartech_planning_v1";
    public static final int MAX_ASSETS=2048,MAX_SCOPES=128,MAX_OPERATIONS=32,MAX_RUNNING=8,MAX_WORLD_OPERATIONS=256,MAX_WORLD_RUNNING=32;
    public final LinkedHashMap<String,PlanningAsset> assets=new LinkedHashMap<>();
    public final LinkedHashMap<String,TeamIntelData> intel=new LinkedHashMap<>();
    public final LinkedHashMap<String,DefenseBattleLog> defense=new LinkedHashMap<>();
    public final LinkedHashMap<UUID,AttackOperation> operations=new LinkedHashMap<>();
    public final LinkedHashMap<String,CommandCenterState> commandCenters=new LinkedHashMap<>();
    public PlanningWorldData(){super(NAME);}public PlanningWorldData(String name){super(name);}
    public static PlanningWorldData get(World w){PlanningWorldData d=(PlanningWorldData)w.getPerWorldStorage().getOrLoadData(PlanningWorldData.class,NAME);if(d==null){d=new PlanningWorldData();w.getPerWorldStorage().setData(NAME,d);}return d;}
    private boolean scopeAvailable(String scope){if(intel.containsKey(scope) || defense.containsKey(scope) || commandCenters.containsKey(scope))return true;Set<String> keys=new HashSet<>(intel.keySet());keys.addAll(defense.keySet());keys.addAll(commandCenters.keySet());return keys.size()<MAX_SCOPES;}
    public CommandCenterState command(String scope){if(scope.isEmpty())return null;CommandCenterState c=commandCenters.get(scope);if(c==null && scopeAvailable(scope)){c=new CommandCenterState();commandCenters.put(scope,c);markDirty();}return c;}
    public TeamIntelData intel(String scope){if(scope.isEmpty())return null;TeamIntelData d=intel.get(scope);if(d==null && scopeAvailable(scope)){d=new TeamIntelData();intel.put(scope,d);markDirty();}return d;}
    public DefenseBattleLog defense(String scope){if(scope.isEmpty())return null;DefenseBattleLog d=defense.get(scope);if(d==null && scopeAvailable(scope)){d=new DefenseBattleLog();defense.put(scope,d);markDirty();}return d;}
    public List<AttackOperation> operations(String scope){List<AttackOperation> result=new ArrayList<>();for(AttackOperation o:operations.values())if(PlanningAccessPolicy.same(scope,o.scope))result.add(o);return result;}
    public String saveDraft(AttackOperation incoming,String scope,UUID author,int expected){
        AttackOperation previous=operations.get(incoming.id);
        if(previous!=null && (!PlanningAccessPolicy.same(scope,previous.scope) || previous.revision!=expected || previous.state!=AttackOperation.State.DRAFT))return "revision";
        if(previous==null && (operations(scope).size()>=MAX_OPERATIONS || operations.size()>=MAX_WORLD_OPERATIONS))return "archive_full";
        incoming.scope=scope;incoming.author=author;incoming.state=AttackOperation.State.DRAFT;incoming.started=-1;incoming.clockOffset=0;incoming.pausedAt=-1;
        for(AttackOperation.Step s:incoming.steps){s.phase=AttackOperation.Phase.PENDING;s.accepted=s.actual=s.completed=-1;s.nextAttempt=0;s.pausedDelay=s.acceptedPause=0;s.emitted=0;s.flights.clear();s.reason="";}
        String error=incoming.validate();if(!error.isEmpty() && !(incoming.steps.isEmpty() && "steps".equals(error)))return error;
        incoming.revision=previous==null?1:previous.revision+1;operations.put(incoming.id,incoming);markDirty();return "";
    }
    @Override public void readFromNBT(NBTTagCompound n){
        assets.clear();intel.clear();defense.clear();operations.clear();commandCenters.clear();NBTTagList as=n.getTagList("Assets",10);
        for(int i=0;i<Math.min(MAX_ASSETS,as.tagCount());i++){PlanningAsset a=PlanningAsset.read(as.getCompoundTagAt(i));a.loaded=false;assets.put(a.key,a);}
        NBTTagList scopes=n.getTagList("Scopes",10);for(int i=0;i<Math.min(MAX_SCOPES,scopes.tagCount());i++){NBTTagCompound s=scopes.getCompoundTagAt(i);String key=s.getString("Scope");intel.put(key,TeamIntelData.read(s.getCompoundTag("Intel")));defense.put(key,DefenseBattleLog.read(s.getCompoundTag("Defense")));if(s.hasKey("Command",10))commandCenters.put(key,CommandCenterState.read(s.getCompoundTag("Command")));}
        NBTTagList os=n.getTagList("Operations",10);Map<String,Integer> counts=new HashMap<>();
        for(int i=0;i<Math.min(MAX_WORLD_OPERATIONS,os.tagCount());i++){AttackOperation o=AttackOperation.read(os.getCompoundTagAt(i));int count=counts.getOrDefault(o.scope,0);if(count<MAX_OPERATIONS && (o.validate().isEmpty() || o.state==AttackOperation.State.DRAFT && o.steps.isEmpty())){operations.put(o.id,o);counts.put(o.scope,count+1);}}
    }
    @Override public NBTTagCompound writeToNBT(NBTTagCompound n){
        NBTTagList as=new NBTTagList(),ss=new NBTTagList(),os=new NBTTagList();for(PlanningAsset a:assets.values())as.appendTag(a.write());
        Set<String> keys=new LinkedHashSet<>(intel.keySet());keys.addAll(defense.keySet());keys.addAll(commandCenters.keySet());for(String key:keys){NBTTagCompound s=new NBTTagCompound();s.setString("Scope",key);if(intel.containsKey(key))s.setTag("Intel",intel.get(key).write());if(defense.containsKey(key))s.setTag("Defense",defense.get(key).write());if(commandCenters.containsKey(key))s.setTag("Command",commandCenters.get(key).write());ss.appendTag(s);}
        for(AttackOperation o:operations.values())os.appendTag(o.write());n.setTag("Assets",as);n.setTag("Scopes",ss);n.setTag("Operations",os);return n;
    }
}
