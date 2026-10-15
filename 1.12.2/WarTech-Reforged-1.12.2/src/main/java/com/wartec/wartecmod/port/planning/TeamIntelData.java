package com.wartec.wartecmod.port.planning;

import java.util.*;
import net.minecraft.nbt.*;

/** Immutable-at-observation snapshots. No lookup of the present hostile world in read(). */
public final class TeamIntelData {
    public static final int MAX_CELLS=4096,MAX_CONTACTS=256;
    public final LinkedHashMap<Long,NBTTagCompound> cells=new LinkedHashMap<>();
    public final LinkedHashMap<String,NBTTagCompound> contacts=new LinkedHashMap<>();
    public void cell(int x,int z,int height,int color,long tick,String sensor,UUID source) {
        long key=((long)x<<32)|(z&0xffffffffL);NBTTagCompound n=new NBTTagCompound();
        n.setInteger("X",x);n.setInteger("Z",z);n.setInteger("Height",height);n.setInteger("Color",color);n.setLong("Seen",tick);n.setString("Sensor",sensor);n.setUniqueId("Source",source);
        cells.remove(key);cells.put(key,n);trim(cells,MAX_CELLS);
    }
    public void contact(String key,String name,String type,double x,double y,double z,float quality,long tick,String sensor,UUID source) {
        NBTTagCompound n=new NBTTagCompound();n.setString("Key",key);n.setString("Name",name);n.setString("Kind",type);
        n.setDouble("X",x);n.setDouble("Y",y);n.setDouble("Z",z);n.setFloat("Quality",quality);n.setLong("Seen",tick);
        n.setString("Sensor",sensor);n.setUniqueId("Source",source);n.setBoolean("Confirmed",true);
        int patchX=Math.floorDiv((int)Math.floor(x),16)-1,patchZ=Math.floorDiv((int)Math.floor(z),16)-1;int[] patch=new int[16],heights=new int[16],ages=new int[16];Arrays.fill(ages,-1);
        for(int row=0;row<4;row++)for(int col=0;col<4;col++){NBTTagCompound c=cells.get(((long)(patchX+col)<<32)|((patchZ+row)&0xffffffffL));if(c!=null && c.getLong("Seen")<=tick){int at=row*4+col;patch[at]=c.getInteger("Color");heights[at]=c.getInteger("Height");ages[at]=(int)Math.min(Integer.MAX_VALUE,Math.max(0,tick-c.getLong("Seen")));}}
        n.setInteger("PatchX",patchX);n.setInteger("PatchZ",patchZ);n.setIntArray("Patch",patch);n.setIntArray("PatchHeights",heights);n.setIntArray("PatchAges",ages);
        NBTTagCompound previous=contacts.get(key);if(previous!=null){NBTTagList history=previous.getTagList("History",10).copy();if(tick-previous.getLong("Seen")>=200 || !previous.getBoolean("Confirmed") || Math.hypot(x-previous.getDouble("X"),z-previous.getDouble("Z"))>=8){NBTTagCompound old=previous.copy();old.removeTag("History");if(history.tagCount()>=12)history.removeTag(0);history.appendTag(old);}n.setTag("History",history);}
        contacts.remove(key);contacts.put(key,n);trim(contacts,MAX_CONTACTS);
    }
    /** Only actually surveyed, visible cells may mark an old ground contact unconfirmed. */
    public void resurvey(int cellX,int cellZ,Set<String> present,long tick) {
        for(NBTTagCompound c:contacts.values())if(Math.floorDiv((int)Math.floor(c.getDouble("X")),16)==cellX
                && Math.floorDiv((int)Math.floor(c.getDouble("Z")),16)==cellZ && !present.contains(c.getString("Key"))
                && !"aircraft".equals(c.getString("Kind")) && !"missile".equals(c.getString("Kind"))) {
            rememberAbsence(c,tick);
        }
    }
    public void markUnconfirmed(String key,long tick){NBTTagCompound c=contacts.get(key);if(c!=null && tick>=Math.max(c.getLong("Seen"),c.getLong("Checked")))rememberAbsence(c,tick);}
    private static void rememberAbsence(NBTTagCompound c,long tick){if(c.getBoolean("Confirmed")){NBTTagList history=c.getTagList("History",10);NBTTagCompound old=c.copy();old.removeTag("History");if(history.tagCount()>=12)history.removeTag(0);history.appendTag(old);c.setTag("History",history);}c.setBoolean("Confirmed",false);c.setLong("Checked",tick);}
    public NBTTagCompound write() {
        NBTTagCompound n=new NBTTagCompound();NBTTagList cs=new NBTTagList(),ts=new NBTTagList();
        for(NBTTagCompound c:cells.values())cs.appendTag(c.copy());for(NBTTagCompound c:contacts.values()){NBTTagCompound row=c.copy();for(String key:new String[]{"History","Patch","PatchHeights","PatchAges"})row.removeTag(key);ts.appendTag(row);}n.setTag("Cells",cs);n.setTag("Contacts",ts);return n;
    }
    public NBTTagCompound viewport(double x,double z,int radius){
        NBTTagCompound n=new NBTTagCompound();NBTTagList cs=new NBTTagList(),ts=new NBTTagList();int[] terrain=new int[48*27];
        for(int row=0;row<27;row++)for(int col=0;col<48;col++){
            int wx=(int)Math.floor(x+((col+.5)/48-.5)*2*radius),wz=(int)Math.floor(z+((row+.5)/27-.5)*2*radius*com.wartec.wartecmod.port.network.CommandMapViewport.HEIGHT/com.wartec.wartecmod.port.network.CommandMapViewport.WIDTH);
            NBTTagCompound c=cells.get(((long)Math.floorDiv(wx,16)<<32)|(Math.floorDiv(wz,16)&0xffffffffL));if(c!=null)terrain[row*48+col]=c.getInteger("Color");
        }
        List<NBTTagCompound> newest=new ArrayList<>(cells.values());Collections.reverse(newest);
        for(NBTTagCompound c:newest)if(cs.tagCount()<256 && Math.abs(c.getInteger("X")*16-x)<=radius+16 && Math.abs(c.getInteger("Z")*16-z)<=radius+16)cs.appendTag(c.copy());
        for(NBTTagCompound c:contacts.values()){NBTTagCompound row=c.copy();for(String key:new String[]{"History","Patch","PatchHeights","PatchAges"})row.removeTag(key);ts.appendTag(row);}n.setTag("Cells",cs);n.setTag("Contacts",ts);n.setIntArray("Terrain",terrain);return n;
    }
    public static TeamIntelData read(NBTTagCompound n) {
        TeamIntelData data=new TeamIntelData();NBTTagList cs=n.getTagList("Cells",10),ts=n.getTagList("Contacts",10);
        for(int i=0;i<Math.min(MAX_CELLS,cs.tagCount());i++){NBTTagCompound c=cs.getCompoundTagAt(i).copy();data.cells.put(((long)c.getInteger("X")<<32)|(c.getInteger("Z")&0xffffffffL),c);}
        for(int i=0;i<Math.min(MAX_CONTACTS,ts.tagCount());i++){NBTTagCompound c=ts.getCompoundTagAt(i).copy();data.contacts.put(c.getString("Key"),c);}return data;
    }
    private static void trim(Map<?,?> map,int max) { while(map.size()>max){Iterator<?> it=map.keySet().iterator();it.next();it.remove();} }
}
