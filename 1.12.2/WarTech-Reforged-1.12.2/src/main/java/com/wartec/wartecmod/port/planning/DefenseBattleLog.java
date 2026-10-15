package com.wartec.wartecmod.port.planning;

import java.util.*;
import net.minecraft.nbt.*;

/** Admission and confirmed outcomes are separate. A lost track is never a kill. */
public final class DefenseBattleLog {
    public static final int LIMIT=2048;
    public final LinkedHashMap<UUID,NBTTagCompound> shots=new LinkedHashMap<>();
    public final LinkedHashMap<UUID,NBTTagCompound> targets=new LinkedHashMap<>();
    public void track(UUID target,long tick){if(target!=null && !targets.containsKey(target)){NBTTagCompound t=new NBTTagCompound();t.setUniqueId("Id",target);t.setLong("Tick",tick);t.setString("Outcome","unknown");targets.put(target,t);trim(targets);}}
    public void shot(UUID id,String battery,String name,UUID target,long tick) {
        if(shots.containsKey(id))return;NBTTagCompound n=new NBTTagCompound();n.setUniqueId("Id",id);n.setString("Battery",battery);n.setString("Name",name);n.setLong("Tick",tick);n.setString("Outcome","flying");
        if(target!=null)n.setUniqueId("Target",target);shots.put(id,n);trim(shots);
        if(target!=null && !targets.containsKey(target)){NBTTagCompound t=new NBTTagCompound();t.setUniqueId("Id",target);t.setLong("Tick",tick);t.setString("Outcome","unknown");targets.put(target,t);trim(targets);}
    }
    public void outcome(UUID shot,UUID target,boolean intercepted,long tick) {
        NBTTagCompound s=shots.get(shot);if(s!=null && "flying".equals(s.getString("Outcome"))){s.setString("Outcome",intercepted?"intercepted":"failed");s.setLong("End",tick);}
        if(intercepted && target!=null){targetOutcome(target,"intercepted",tick);NBTTagCompound t=targets.get(target);if(t!=null && s!=null && !t.hasKey("WinningBattery"))t.setString("WinningBattery",s.getString("Battery"));}
    }
    public void targetOutcome(UUID target,String outcome,long tick) {
        NBTTagCompound t=targets.get(target);if(t==null)return;
        if("unknown".equals(t.getString("Outcome"))){t.setString("Outcome",outcome);t.setLong("End",tick);}
    }
    public NBTTagCompound summary(String battery) {
        NBTTagCompound n=new NBTTagCompound();int launched=0,successful=0,killed=0,through=0,unknown=0,bursts=0,rounds=0,failed=0,flying=0,assisted=0;
        for(NBTTagCompound s:shots.values())if(battery.isEmpty() || battery.equals(s.getString("Battery"))){if(s.hasKey("Rounds")){bursts++;rounds+=s.getInteger("Rounds");}else{launched++;if("intercepted".equals(s.getString("Outcome")))successful++;if("failed".equals(s.getString("Outcome")))failed++;if("flying".equals(s.getString("Outcome")))flying++;}}
        Set<UUID> selected=new HashSet<>();if(!battery.isEmpty())for(NBTTagCompound s:shots.values())if(battery.equals(s.getString("Battery")) && s.hasUniqueId("Target"))selected.add(s.getUniqueId("Target"));
        for(Map.Entry<UUID,NBTTagCompound> row:targets.entrySet())if(battery.isEmpty() || selected.contains(row.getKey())) {
            String o=row.getValue().getString("Outcome");if("intercepted".equals(o)){boolean own=battery.isEmpty() || battery.equals(row.getValue().getString("WinningBattery"));if(!row.getValue().hasKey("WinningBattery")){String winner="";long end=Long.MAX_VALUE;for(NBTTagCompound shot:shots.values())if("intercepted".equals(shot.getString("Outcome")) && shot.hasUniqueId("Target") && row.getKey().equals(shot.getUniqueId("Target")) && shot.getLong("End")<end){winner=shot.getString("Battery");end=shot.getLong("End");}own=battery.isEmpty() || battery.equals(winner);}if(own)killed++;else assisted++;}else if("breakthrough".equals(o))through++;else unknown++;
        }
        n.setInteger("Assisted",assisted);n.setInteger("Engaged",selected.size());n.setInteger("Failed",failed);n.setInteger("Flying",flying);n.setInteger("Identified",battery.isEmpty()?targets.size():selected.size());n.setInteger("GunBursts",bursts);n.setInteger("GunRounds",rounds);n.setInteger("Launched",launched);n.setInteger("SuccessfulShots",successful);n.setInteger("Intercepted",killed);n.setInteger("Breakthrough",through);n.setInteger("Unknown",unknown);
        n.setDouble("Rate",killed+through+assisted==0?-1:100D*killed/(killed+through+assisted));n.setDouble("Efficiency",launched==0?-1:100D*successful/launched);return n;
    }
    public List<NBTTagCompound> events(){List<NBTTagCompound> rows=new ArrayList<>();for(NBTTagCompound t:targets.values()){NBTTagCompound identified=t.copy();identified.setString("Outcome","identified");identified.setString("Kind","target");rows.add(identified);if(!"unknown".equals(t.getString("Outcome"))){NBTTagCompound end=t.copy();end.setLong("Tick",t.getLong("End"));end.setString("Kind","target");rows.add(end);}}
        for(NBTTagCompound s:shots.values()){NBTTagCompound row=s.copy();row.setString("Kind","shot");if(row.hasKey("End"))row.setLong("Tick",row.getLong("End"));if(row.hasUniqueId("Target")){NBTTagCompound t=targets.get(row.getUniqueId("Target"));if(t!=null){row.setString("TargetName",t.getString("Name"));row.setInteger("Tier",t.getInteger("Tier"));}}rows.add(row);}rows.sort(java.util.Comparator.comparingLong(n->n.getLong("Tick")));return rows;}
    public NBTTagCompound write() { NBTTagCompound n=new NBTTagCompound();NBTTagList ss=new NBTTagList(),tt=new NBTTagList();for(NBTTagCompound s:shots.values())ss.appendTag(s.copy());for(NBTTagCompound t:targets.values())tt.appendTag(t.copy());n.setTag("Shots",ss);n.setTag("Targets",tt);return n; }
    public static DefenseBattleLog read(NBTTagCompound n) { DefenseBattleLog d=new DefenseBattleLog();load(d.shots,n.getTagList("Shots",10));load(d.targets,n.getTagList("Targets",10));return d; }
    public static final int TRACE_LIMIT=128;
    private static void compact(NBTTagList trace){int remove=1;double best=Double.POSITIVE_INFINITY;for(int i=1;i<trace.tagCount()-1;i++){NBTTagCompound a=trace.getCompoundTagAt(i-1),b=trace.getCompoundTagAt(i),c=trace.getCompoundTagAt(i+1);double ratio=(b.getLong("Tick")-a.getLong("Tick"))/(double)Math.max(1,c.getLong("Tick")-a.getLong("Tick")),error=0;for(String axis:new String[]{"X","Y","Z"}){double predicted=a.getDouble(axis)+(c.getDouble(axis)-a.getDouble(axis))*ratio;error+=Math.pow(b.getDouble(axis)-predicted,2);}error=Math.sqrt(error)+Math.max(0,c.getLong("Tick")-a.getLong("Tick"))*.0001;if(error<best){best=error;remove=i;}}trace.removeTag(remove);}
    public static void sample(NBTTagCompound record,double x,double y,double z,long tick){
        NBTTagList trace=record.getTagList("Trace",10);if(trace.tagCount()>0 && !record.hasKey("End") && tick-trace.getCompoundTagAt(trace.tagCount()-1).getLong("Tick")<20)return;
        NBTTagCompound point=new NBTTagCompound();point.setDouble("X",x);point.setDouble("Y",y);point.setDouble("Z",z);point.setLong("Tick",tick);
        if(trace.tagCount()>=TRACE_LIMIT)compact(trace);trace.appendTag(point);record.setTag("Trace",trace);
    }
    public NBTTagCompound replay(UUID target){
        NBTTagCompound result=new NBTTagCompound(),t=targets.get(target);if(t==null)return result;result.setTag("Target",t.copy());
        NBTTagList events=new NBTTagList(),flights=new NBTTagList();NBTTagCompound seen=t.copy();seen.removeTag("Trace");seen.setString("Outcome","identified");events.appendTag(seen);
        for(NBTTagCompound shot:shots.values())if(shot.hasUniqueId("Target") && target.equals(shot.getUniqueId("Target"))){flights.appendTag(shot.copy());NBTTagCompound begin=shot.copy();begin.removeTag("Trace");begin.setString("Outcome","launched");events.appendTag(begin);
            if(shot.hasKey("End")){NBTTagCompound end=shot.copy();end.removeTag("Trace");end.setLong("Tick",shot.getLong("End"));events.appendTag(end);}}
        if(!"unknown".equals(t.getString("Outcome"))){NBTTagCompound end=t.copy();end.removeTag("Trace");end.setLong("Tick",t.getLong("End"));events.appendTag(end);}
        List<NBTTagCompound> sorted=new ArrayList<>();for(int i=0;i<events.tagCount();i++)sorted.add(events.getCompoundTagAt(i));sorted.sort(Comparator.comparingLong(n->n.getLong("Tick")));
        result.setTag("Events",CommandCenterState.list(sorted));result.setTag("Flights",flights);return result;
    }
    public NBTTagList episodes(){
        List<NBTTagCompound> sorted=new ArrayList<>(targets.values());sorted.sort(Comparator.comparingLong(n->n.getLong("Tick")));List<NBTTagCompound> result=new ArrayList<>();NBTTagCompound current=null;long last=-1;
        for(NBTTagCompound target:sorted){long tick=target.getLong("Tick");if(current==null || tick-last>400){current=new NBTTagCompound();current.setUniqueId("Id",target.getUniqueId("Id"));current.setString("Name","command.episode");current.setLong("Tick",tick);current.setLong("LastDetection",tick);result.add(current);}current.setLong("LastDetection",tick);current.setLong("End",Math.max(current.getLong("End"),Math.max(tick,target.getLong("End"))));current.setInteger("Count",current.getInteger("Count")+1);String outcome=target.getString("Outcome");if("intercepted".equals(outcome))current.setInteger("Intercepted",current.getInteger("Intercepted")+1);if("breakthrough".equals(outcome))current.setInteger("Breakthrough",current.getInteger("Breakthrough")+1);last=tick;}
        return CommandCenterState.list(result.subList(Math.max(0,result.size()-64),result.size()));
    }
    public NBTTagCompound replayEpisode(UUID id){
        NBTTagCompound episode=null;NBTTagList episodes=episodes();for(int i=0;i<episodes.tagCount();i++)if(id.equals(episodes.getCompoundTagAt(i).getUniqueId("Id")))episode=episodes.getCompoundTagAt(i);if(episode==null)return new NBTTagCompound();
        NBTTagCompound result=new NBTTagCompound();result.setTag("Target",episode.copy());NBTTagList contacts=new NBTTagList(),flights=new NBTTagList();List<NBTTagCompound> events=new ArrayList<>();
        for(Map.Entry<UUID,NBTTagCompound> e:targets.entrySet())if(e.getValue().getLong("Tick")>=episode.getLong("Tick") && e.getValue().getLong("Tick")<=episode.getLong("LastDetection") && contacts.tagCount()<128){NBTTagCompound one=replay(e.getKey());contacts.appendTag(e.getValue().copy());NBTTagList fs=one.getTagList("Flights",10),es=one.getTagList("Events",10);for(int i=0;i<fs.tagCount() && flights.tagCount()<256;i++)flights.appendTag(fs.getCompoundTagAt(i));for(int i=0;i<es.tagCount() && events.size()<1024;i++)events.add(es.getCompoundTagAt(i));}
        events.sort(Comparator.comparingLong(n->n.getLong("Tick")));limitDisplay(contacts);limitDisplay(flights);result.setTag("Targets",contacts);result.setTag("Flights",flights);result.setTag("Events",CommandCenterState.list(events));return result;
    }
    private static void limitDisplay(NBTTagList records){for(int i=0;i<records.tagCount();i++){NBTTagList trace=records.getCompoundTagAt(i).getTagList("Trace",10);while(trace.tagCount()>64)compact(trace);}}
    private static void load(Map<UUID,NBTTagCompound> map,NBTTagList list){for(int i=0;i<Math.min(LIMIT,list.tagCount());i++){NBTTagCompound n=list.getCompoundTagAt(i);if(n.hasUniqueId("Id"))map.put(n.getUniqueId("Id"),n.copy());}}
    private static void trim(Map<?,?> m){while(m.size()>LIMIT){Iterator<?> i=m.keySet().iterator();i.next();i.remove();}}
}
