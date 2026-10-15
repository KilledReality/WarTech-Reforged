package com.wartec.wartecmod.port.planning;

import com.wartec.wartecmod.port.integration.OwnerTeamNbt;
import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldSavedData;

/** A team's IFF whitelist, shared across dimensions. It never grants ownership or archive access. */
public final class DefenseRelations extends WorldSavedData {
    private static final String NAME="wartech_defense_relations";
    private final Map<String,Set<String>> friends=new LinkedHashMap<>();
    public DefenseRelations(){super(NAME);} public DefenseRelations(String name){super(name);}
    public static DefenseRelations get(World world){
        World root=world.getMinecraftServer()==null?world:world.getMinecraftServer().getWorld(0);
        if(root==null)root=world;
        DefenseRelations data=(DefenseRelations)root.getMapStorage().getOrLoadData(DefenseRelations.class,NAME);
        if(data==null){data=new DefenseRelations();root.getMapStorage().setData(NAME,data);}return data;
    }
    public boolean friendly(String team,String other){team=OwnerTeamNbt.normalize(team);other=OwnerTeamNbt.normalize(other);return !team.isEmpty() && !other.isEmpty() && (team.equals(other) || friends.getOrDefault(team,Collections.emptySet()).contains(other));}
    public String change(String team,String other,boolean add){
        team=OwnerTeamNbt.normalize(team);other=OwnerTeamNbt.normalize(other);
        if(team.isEmpty() || other.isEmpty() || team.equals(other))return "team";
        Set<String> list=friends.get(team);if(list==null){if(!add)return "";if(friends.size()>=128)return "archive_full";list=new LinkedHashSet<>();friends.put(team,list);}
        if(add){if(list.size()>=32 && !list.contains(other))return "archive_full";list.add(other);}else list.remove(other);markDirty();return "";
    }
    public NBTTagList list(String team){NBTTagList n=new NBTTagList();for(String s:friends.getOrDefault(OwnerTeamNbt.normalize(team),Collections.emptySet()))n.appendTag(new NBTTagString(s));return n;}
    @Override public void readFromNBT(NBTTagCompound n){friends.clear();NBTTagList rows=n.getTagList("Teams",10);for(int i=0;i<Math.min(128,rows.tagCount());i++){NBTTagCompound row=rows.getCompoundTagAt(i);String team=OwnerTeamNbt.normalize(row.getString("Team"));NBTTagList list=row.getTagList("Friends",8);for(int j=0;j<Math.min(32,list.tagCount());j++)change(team,list.getStringTagAt(j),true);}}
    @Override public NBTTagCompound writeToNBT(NBTTagCompound n){NBTTagList rows=new NBTTagList();for(String team:friends.keySet()){NBTTagCompound row=new NBTTagCompound();row.setString("Team",team);row.setTag("Friends",list(team));rows.appendTag(row);}n.setTag("Teams",rows);return n;}
}
