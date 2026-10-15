package com.wartec.wartecmod.port.planning;

import java.util.*;
import net.minecraft.nbt.*;

/** Persistent team command settings. Enemy positions never originate in this store. */
public final class CommandCenterState {
    public enum Role { COMMANDER, OPERATOR, OBSERVER }
    public static final int MAX_FLEET=512, MAX_DISTRICTS=64, MAX_ALERTS=128, MAX_TASKS=128;
    public long revision,configRevision;
    public final LinkedHashMap<String,NBTTagCompound> fleet=new LinkedHashMap<>();
    public final LinkedHashMap<UUID,NBTTagCompound> districts=new LinkedHashMap<>();
    public final LinkedHashMap<UUID,Role> roles=new LinkedHashMap<>();
    public final LinkedHashMap<String,NBTTagCompound> alerts=new LinkedHashMap<>();
    public final LinkedHashMap<UUID,NBTTagCompound> tasks=new LinkedHashMap<>();
    private final Map<String,String> lastState=new LinkedHashMap<>();
    private long configurationVersion,policyCacheVersion=-1;
    private boolean policyCache;
    public boolean hasPolicies(){if(policyCacheVersion!=configurationVersion){policyCache=false;for(NBTTagCompound n:fleet.values())if(n.getInteger("Reserve")>0 || n.getInteger("Priority")>0 || !n.getString("District").isEmpty()){policyCache=true;break;}policyCacheVersion=configurationVersion;}return policyCache;}

    public Role role(UUID player){return roles.getOrDefault(player,Role.OBSERVER);}
    public boolean mayOrder(UUID player){return role(player)!=Role.OBSERVER;}
    public String setRole(UUID actor,UUID target,Role role){
        if(actor==null || target==null || role(actor)!=Role.COMMANDER)return "access";
        if(!roles.containsKey(target) && roles.size()>=64)return "limits";
        long commanders=roles.values().stream().filter(r->r==Role.COMMANDER).count();
        if(roles.get(target)==Role.COMMANDER && role!=Role.COMMANDER && commanders<=1)return "last_commander";
        roles.put(target,role);revision++;configRevision++;return "";
    }
    public String configure(String key,NBTTagCompound input){
        if(key.isEmpty() || key.length()>180 || !fleet.containsKey(key) && fleet.size()>=MAX_FLEET)return "limits";
        int reserve=input.getInteger("Reserve"),tier=input.hasKey("ReserveTier")?input.getInteger("ReserveTier"):3;
        int priority=input.getInteger("Priority");
        if(reserve<0 || reserve>256 || tier<1 || tier>4 || priority<0 || priority>2)return "limits";
        String district=input.getString("District");
        if(!district.isEmpty())try{if(!districts.containsKey(UUID.fromString(district)))return "district";}catch(IllegalArgumentException e){return "district";}
        NBTTagCompound n=new NBTTagCompound();n.setString("Key",key);
        n.setString("Title",clean(input.getString("Title"),64));n.setString("Group",clean(input.getString("Group"),40));
        n.setString("District",district);n.setInteger("Reserve",reserve);n.setInteger("ReserveTier",tier);n.setInteger("Priority",priority);
        fleet.put(key,n);revision++;configRevision++;configurationVersion++;return "";
    }
    public String district(NBTTagCompound input){
        UUID id=input.hasUniqueId("Id")?input.getUniqueId("Id"):UUID.randomUUID();
        double x=input.getDouble("X"),z=input.getDouble("Z");int radius=input.getInteger("Radius");
        if(!Double.isFinite(x) || !Double.isFinite(z) || Math.abs(x)>29999984 || Math.abs(z)>29999984)return "coordinates";
        if(radius<32 || radius>4096 || !districts.containsKey(id) && districts.size()>=MAX_DISTRICTS)return "limits";
        NBTTagCompound n=new NBTTagCompound();n.setUniqueId("Id",id);n.setString("Title",clean(input.getString("Title"),64));
        n.setDouble("X",x);n.setDouble("Z",z);n.setInteger("Radius",radius);districts.put(id,n);revision++;configRevision++;return "";
    }
    public void removeFleet(String key){if(fleet.remove(key)!=null){revision++;configRevision++;configurationVersion++;}}
    public void removeDistrict(UUID id){districts.remove(id);for(NBTTagCompound n:fleet.values())if(id.toString().equals(n.getString("District")))n.setString("District","");revision++;configRevision++;configurationVersion++;}
    public void notify(String key,String kind,String asset,long tick){notify(key,kind,asset,tick,"","");}
    public void notify(String key,String kind,String asset,long tick,String reason,String title){
        NBTTagCompound previous=alerts.get(key);
        if(previous!=null && !previous.getBoolean("Resolved") && kind.equals(previous.getString("Kind")) && asset.equals(previous.getString("Asset")) && reason.equals(previous.getString("Reason"))){previous.setLong("LastSeen",tick);return;}
        NBTTagCompound n=new NBTTagCompound();n.setString("Key",key);n.setString("Kind",kind);n.setString("Asset",asset);n.setString("Reason",reason);n.setString("Title",title);n.setLong("Tick",tick);n.setLong("LastSeen",tick);
        alerts.remove(key);alerts.put(key,n);trim(alerts,MAX_ALERTS);revision++;
    }
    public void resolveNotice(String key,long now){NBTTagCompound n=alerts.get(key);if(n!=null && !n.getBoolean("Resolved")){n.setBoolean("Resolved",true);n.setLong("ResolvedAt",now);revision++;}}
    public void acknowledge(String key){NBTTagCompound n=alerts.get(key);if(n!=null){n.setBoolean("Acknowledged",true);revision++;}}
    public void observe(PlanningAsset a,long now){
        String warning="";
        if(!a.loaded)warning="unloaded";
        else if(a.capacity>0 && a.health<a.capacity*.5)warning="repair";
        else if((a.attack || "pvo".equals(a.kind)) && a.ammo==0 && !a.allows(AttackOperation.Action.OBSERVE))warning="empty";
        else if(CommandCenterService.launchEnergy(a)>0 && a.power<CommandCenterService.launchEnergy(a))warning="power";
        else if("pvo".equals(a.kind) && !a.network)warning="network";
        else if((a.attack || "pvo".equals(a.kind)) && a.ammo>0 && a.ammo<=2)warning="low_ammo";
        String old=lastState.put(a.key,warning);if(warning.isEmpty())resolveNotice("asset:"+a.key,now);if(!warning.isEmpty() && !warning.equals(old))notify("asset:"+a.key,warning,a.key,now);
        trim(lastState,PlanningWorldData.MAX_ASSETS);
    }
    public UUID task(String asset,String kind,String group,long now){
        for(Map.Entry<UUID,NBTTagCompound> e:tasks.entrySet())if(asset.equals(e.getValue().getString("Asset")) && kind.equals(e.getValue().getString("Kind")) && !e.getValue().getBoolean("Done"))return e.getKey();
        if(tasks.size()>=MAX_TASKS){UUID old=null;for(Map.Entry<UUID,NBTTagCompound> e:tasks.entrySet())if(e.getValue().getBoolean("Done")){old=e.getKey();break;}if(old==null)return null;tasks.remove(old);}
        UUID id=UUID.randomUUID();NBTTagCompound n=new NBTTagCompound();n.setUniqueId("Id",id);n.setString("Asset",asset);n.setString("Kind",kind);n.setString("Group",clean(group,40));n.setLong("Tick",now);tasks.put(id,n);revision++;return id;
    }
    public boolean completeTask(UUID id){NBTTagCompound n=tasks.get(id);if(n==null)return false;n.setBoolean("Done",true);revision++;return true;}
    public NBTTagCompound write(){
        NBTTagCompound n=new NBTTagCompound();n.setLong("Revision",revision);n.setLong("ConfigRevision",configRevision);n.setTag("Fleet",list(fleet.values()));n.setTag("Districts",list(districts.values()));n.setTag("Alerts",list(alerts.values()));n.setTag("Tasks",list(tasks.values()));
        NBTTagList rs=new NBTTagList();for(Map.Entry<UUID,Role> r:roles.entrySet()){NBTTagCompound row=new NBTTagCompound();row.setUniqueId("Player",r.getKey());row.setString("Role",r.getValue().name());rs.appendTag(row);}n.setTag("Roles",rs);NBTTagList states=new NBTTagList();for(Map.Entry<String,String> s:lastState.entrySet()){NBTTagCompound row=new NBTTagCompound();row.setString("Key",s.getKey());row.setString("State",s.getValue());states.appendTag(row);}n.setTag("States",states);return n;
    }
    public static CommandCenterState read(NBTTagCompound n){
        CommandCenterState s=new CommandCenterState();NBTTagList f=n.getTagList("Fleet",10);
        // Districts are validated first so saved unit assignments resolve correctly.
        NBTTagList ds=n.getTagList("Districts",10);for(int i=0;i<Math.min(MAX_DISTRICTS,ds.tagCount());i++)s.district(ds.getCompoundTagAt(i));
        for(int i=0;i<Math.min(MAX_FLEET,f.tagCount());i++){NBTTagCompound row=f.getCompoundTagAt(i);s.configure(row.getString("Key"),row);}
        NBTTagList rs=n.getTagList("Roles",10);for(int i=0;i<Math.min(64,rs.tagCount());i++){NBTTagCompound row=rs.getCompoundTagAt(i);if(row.hasUniqueId("Player"))try{s.roles.put(row.getUniqueId("Player"),Role.valueOf(row.getString("Role")));}catch(IllegalArgumentException ignored){}}
        if(!s.roles.isEmpty() && !s.roles.containsValue(Role.COMMANDER))s.roles.clear();
        NBTTagList as=n.getTagList("Alerts",10);for(int i=0;i<Math.min(MAX_ALERTS,as.tagCount());i++){NBTTagCompound row=as.getCompoundTagAt(i).copy();s.alerts.put(clean(row.getString("Key"),220),row);}
        NBTTagList ts=n.getTagList("Tasks",10);for(int i=0;i<Math.min(MAX_TASKS,ts.tagCount());i++){NBTTagCompound row=ts.getCompoundTagAt(i).copy();if(row.hasUniqueId("Id"))s.tasks.put(row.getUniqueId("Id"),row);}
        NBTTagList states=n.getTagList("States",10);for(int i=0;i<Math.min(PlanningWorldData.MAX_ASSETS,states.tagCount());i++){NBTTagCompound row=states.getCompoundTagAt(i);s.lastState.put(row.getString("Key"),row.getString("State"));}s.revision=n.getLong("Revision");s.configRevision=n.hasKey("ConfigRevision")?n.getLong("ConfigRevision"):s.revision;return s;
    }
    public static NBTTagList list(Collection<NBTTagCompound> values){NBTTagList list=new NBTTagList();for(NBTTagCompound n:values)list.appendTag(n.copy());return list;}
    private static String clean(String value,int max){return AttackOperation.bounded(value.replaceAll("[\\p{Cntrl}]",""),max).trim();}
    private static void trim(Map<?,?> map,int max){while(map.size()>max){Iterator<?> i=map.keySet().iterator();i.next();i.remove();}}
}
