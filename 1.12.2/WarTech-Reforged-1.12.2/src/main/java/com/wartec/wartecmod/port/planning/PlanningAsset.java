package com.wartec.wartecmod.port.planning;

import java.util.*;
import net.minecraft.nbt.*;

/** Own-unit telemetry, never a reference to an enemy. */
public final class PlanningAsset {
    public String key="",scope="",name="",kind="",state="",actions="",readyReason="";
    public int dimension,ammo,range,tier;
    public double x,y,z;
    public long seen,power,launchPowerMinimum,launchPowerCost;
    public float health,capacity;
    public boolean loaded,defense,attack,network,airborne,oneWay;
    public NBTTagList stores=new NBTTagList();
    public boolean allows(AttackOperation.Action action) { return Arrays.asList(actions.split(",")).contains(action.name()); }
    public NBTTagCompound write() {
        NBTTagCompound n=new NBTTagCompound();n.setString("Key",key);n.setString("Scope",scope);n.setString("Name",name);n.setString("Kind",kind);
        n.setString("ReadyReason",readyReason);n.setString("State",state);n.setString("Actions",actions);n.setInteger("Dimension",dimension);n.setInteger("Ammo",ammo);n.setInteger("Range",range);n.setInteger("Tier",tier);
        n.setDouble("X",x);n.setDouble("Y",y);n.setDouble("Z",z);n.setLong("Seen",seen);n.setLong("Power",power);n.setFloat("Health",health);n.setFloat("Capacity",capacity);
        n.setBoolean("Loaded",loaded);n.setBoolean("Defense",defense);n.setBoolean("Attack",attack);n.setBoolean("Network",network);n.setBoolean("Airborne",airborne);n.setBoolean("OneWay",oneWay);n.setTag("Stores",stores.copy());n.setLong("LaunchPowerMinimum",launchPowerMinimum);n.setLong("LaunchPowerCost",launchPowerCost);return n;
    }
    public static PlanningAsset read(NBTTagCompound n) {
        PlanningAsset a=new PlanningAsset();a.oneWay=n.getBoolean("OneWay");a.key=n.getString("Key");a.scope=n.getString("Scope");a.name=n.getString("Name");a.kind=n.getString("Kind");a.readyReason=n.getString("ReadyReason");a.state=n.getString("State");a.actions=n.getString("Actions");
        a.dimension=n.getInteger("Dimension");a.ammo=n.getInteger("Ammo");a.range=n.getInteger("Range");a.tier=n.getInteger("Tier");a.x=n.getDouble("X");a.y=n.getDouble("Y");a.z=n.getDouble("Z");
        a.seen=n.getLong("Seen");a.power=n.getLong("Power");a.health=n.getFloat("Health");a.capacity=n.getFloat("Capacity");a.loaded=n.getBoolean("Loaded");a.defense=n.getBoolean("Defense");a.attack=n.getBoolean("Attack");a.network=n.getBoolean("Network");a.airborne=n.getBoolean("Airborne");a.stores=n.getTagList("Stores",10).copy();a.launchPowerMinimum=n.getLong("LaunchPowerMinimum");a.launchPowerCost=n.getLong("LaunchPowerCost");return a;
    }
}
