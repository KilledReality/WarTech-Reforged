package com.wartec.wartecmod.port.cruise;

import com.wartec.wartecmod.port.content.CruisePartItem;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public final class CruiseBuild {
    public static final String TAG = "CruiseBuild";
    private final EnumMap<CruiseSlot,CruisePartDefinition> parts = new EnumMap<>(CruiseSlot.class);
    private String name="Custom Cruise Missile";
    private boolean malformed;
    public CruisePartDefinition get(CruiseSlot slot) { return parts.get(slot); }
    public CruisePartDefinition getAirframe() { return get(CruiseSlot.BODY); }
    public void set(CruiseSlot slot, CruisePartDefinition part) {
        if(part==null) parts.remove(slot);
        else if(part.getSlot()!=slot) throw new IllegalArgumentException("Wrong component slot");
        else parts.put(slot,part);
    }
    public String getName() { return name; }
    public void setName(String value) {
        if(value==null) return;
        value=value.replaceAll("[\\p{Cntrl}§]", "").trim();
        name=value.isEmpty()?"Custom Cruise Missile":value.substring(0,Math.min(32,value.length()));
    }
    public int checksum() {
        int hash = 0x811c9dc5;
        for (CruiseSlot slot : CruiseSlot.values()) {
            String id = get(slot) == null ? "-" : get(slot).getId();
            for (int i = 0; i < id.length(); i++) hash = (hash ^ id.charAt(i)) * 0x01000193;
            hash = (hash ^ 0) * 0x01000193;
        }
        return hash;
    }
    public CruiseStats calculateStats() {
        List<String> errors=new ArrayList<>();
        if(malformed) errors.add("corrupt_build");
        double mass=0;
        for(CruiseSlot slot:CruiseSlot.values()) {
            CruisePartDefinition part=get(slot);
            if(part==null) errors.add("missing_"+slot.name().toLowerCase(java.util.Locale.ROOT));
            else mass+=part.getMass();
        }
        CruisePartDefinition body=get(CruiseSlot.BODY),engine=get(CruiseSlot.ENGINE),fuel=get(CruiseSlot.FUEL),wings=get(CruiseSlot.WINGS);
        double maxMass=body==null?0:body.getPrimary();
        if(body!=null && mass>maxMass) errors.add("overweight");
        if(body==CruisePartDefinition.BODY_LIGHT) {
            if(fuel==CruisePartDefinition.FUEL_EXTENDED || engine==CruisePartDefinition.ENGINE_FAST
                    || engine==CruisePartDefinition.ENGINE_LONG_RANGE || fuel==CruisePartDefinition.FUEL_LONG_RANGE
                    || wings==CruisePartDefinition.WINGS_HEAVY_FOLDING
                    || CruiseWarheads.needsDelay(get(CruiseSlot.WARHEAD))) errors.add("incompatible_light_body");
        }
        if(body!=null && body!=CruisePartDefinition.BODY_LONG_RANGE
                && (engine==CruisePartDefinition.ENGINE_LONG_RANGE || fuel==CruisePartDefinition.FUEL_LONG_RANGE
                    || wings==CruisePartDefinition.WINGS_HEAVY_FOLDING)) errors.add("heavy_hardware_requires_long_range_body");
        if(get(CruiseSlot.LAUNCH)==CruisePartDefinition.LAUNCH_RAIL && body!=CruisePartDefinition.BODY_LIGHT) errors.add("rail_requires_light_body");
        if(get(CruiseSlot.LAUNCH)==CruisePartDefinition.LAUNCH_BOOSTER && !CruiseAirframes.folding(wings)) errors.add("booster_requires_folding_wings");
        if(CruiseWarheads.needsDelay(get(CruiseSlot.WARHEAD)) && get(CruiseSlot.FUSE)!=CruisePartDefinition.FUSE_DELAY) errors.add("penetrator_requires_delayed_fuse");
        if(get(CruiseSlot.FUSE)==CruisePartDefinition.FUSE_AIRBURST
                && !CruiseWarheads.supportsAirburst(get(CruiseSlot.WARHEAD))) errors.add("incompatible_airburst");
        if(engine!=null && mass>engine.getPrimary()*2.4) errors.add("insufficient_thrust");
        double loading=engine==null?1:engine.getPrimary()/Math.max(1,mass+engine.getPrimary()*0.35);
        double speed=body==null || engine==null?0:body.getSecondary()*(0.75+0.4*engine.getSecondary())*Math.sqrt(loading);
        // An unfinished wing slot is neutral for a live estimate, not zero range.
        // Game-distance budget, not raw tank capacity. A light airframe cannot
        // turn a cheap economy engine into a strategic 20,000-block missile.
        double range=fuel==null || engine==null?0:fuel.getPrimary()*0.18*(wings==null?1:wings.getPrimary())/(engine.getSecondary()*(1+mass/700.0));
        double turn=wings==null?0:3.0*wings.getSecondary()/Math.max(1,mass/220.0);
        double ceiling=CruiseAirframes.maximumRange(body);
        // A smooth ceiling preserves the effect of mass, engine and fuel even
        // near the class limit, unlike a flat clamp masking all upgrades.
        double effective=ceiling<=0?0:ceiling*range/(ceiling+range);
        return new CruiseStats(mass,maxMass,speed,effective,turn,errors);
    }
    public NBTTagCompound write() {
        NBTTagCompound tag=new NBTTagCompound(); tag.setInteger("Schema",1); tag.setString("Name",name);
        for(CruiseSlot slot:CruiseSlot.values()) if(get(slot)!=null) tag.setString(slot.name(),get(slot).getId());
        if(malformed) tag.setBoolean("Corrupt",true);
        return tag;
    }
    public static CruiseBuild read(NBTTagCompound tag) {
        CruiseBuild result=new CruiseBuild(); result.setName(tag.getString("Name"));
        result.malformed=tag.getBoolean("Corrupt") || tag.getInteger("Schema")!=1;
        for(CruiseSlot slot:CruiseSlot.values()) if(tag.hasKey(slot.name())) {
            CruisePartDefinition part=CruisePartDefinition.byId(tag.getString(slot.name()));
            if(part==null || part.getSlot()!=slot) result.malformed=true;
            else result.set(slot,part);
        }
        return result;
    }
    public void writeToStack(ItemStack stack) {
        if(stack.isEmpty()) return;
        if(!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        stack.getTagCompound().setTag(TAG,write());
    }
    public static CruiseBuild fromStack(ItemStack stack) {
        return !stack.isEmpty() && stack.hasTagCompound() && stack.getTagCompound().hasKey(TAG,10)
                ?read(stack.getTagCompound().getCompoundTag(TAG)):new CruiseBuild();
    }
    public static CruiseBuild fromInventory(IInventory inventory) {
        CruiseBuild result=new CruiseBuild();
        for(CruiseSlot slot:CruiseSlot.values()) {
            ItemStack stack=inventory.getStackInSlot(slot.ordinal());
            if(!stack.isEmpty() && stack.getItem() instanceof CruisePartItem) {
                CruisePartDefinition part=((CruisePartItem)stack.getItem()).getDefinition(stack);
                if(part!=null && part.getSlot()==slot) result.set(slot,part);
                else result.malformed=true;
            } else if(!stack.isEmpty()) result.malformed=true;
        }
        return result;
    }
    public static CruiseBuild starter(CruisePartDefinition body) {
        CruiseBuild build=new CruiseBuild();
        build.set(CruiseSlot.BODY,body);
        build.set(CruiseSlot.ENGINE,body==CruisePartDefinition.BODY_LIGHT?CruisePartDefinition.ENGINE_ECONOMY:CruisePartDefinition.ENGINE_STANDARD);
        build.set(CruiseSlot.FUEL,body==CruisePartDefinition.BODY_LIGHT?CruisePartDefinition.FUEL_SHORT:CruisePartDefinition.FUEL_STANDARD);
        build.set(CruiseSlot.WINGS,CruisePartDefinition.WINGS_FOLDING);
        build.set(CruiseSlot.NAVIGATION,CruisePartDefinition.NAV_TERRAIN);
        build.set(CruiseSlot.SEEKER,CruisePartDefinition.SEEKER_NONE);
        build.set(CruiseSlot.WARHEAD,CruisePartDefinition.WARHEAD_HE);
        build.set(CruiseSlot.FUSE,CruisePartDefinition.FUSE_CONTACT);
        build.set(CruiseSlot.LAUNCH,body==CruisePartDefinition.BODY_LIGHT?CruisePartDefinition.LAUNCH_RAIL:CruisePartDefinition.LAUNCH_BOOSTER);
        build.set(CruiseSlot.LINK,CruisePartDefinition.LINK_AUTONOMOUS);
        if(body==CruisePartDefinition.BODY_LONG_RANGE) {
            build.set(CruiseSlot.ENGINE,CruisePartDefinition.ENGINE_LONG_RANGE);
            build.set(CruiseSlot.FUEL,CruisePartDefinition.FUEL_LONG_RANGE);
            build.set(CruiseSlot.WINGS,CruisePartDefinition.WINGS_HEAVY_FOLDING);
            build.set(CruiseSlot.NAVIGATION,CruisePartDefinition.NAV_COORDINATE);
            build.set(CruiseSlot.WARHEAD,CruisePartDefinition.WARHEAD_HEAVY_HE);
        }
        build.setName(body.getEnglishName());
        return build;
    }
}
