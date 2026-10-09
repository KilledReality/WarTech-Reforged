package com.wartec.wartecmod.port.cruise;

import java.util.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.util.math.Vec3d;

/** User coordinates are strike/search goals, never hand-authored transit points. */
public final class CruiseMission {
    public enum Mode { COORDINATE, SEARCH }
    private final List<Vec3d> goals=new ArrayList<>();
    private Mode mode=Mode.COORDINATE;
    private CruiseTargetCategory category=CruiseTargetCategory.ANY;
    private boolean malformed;
    private int dimension;
    public Mode getMode() { return mode; }
    public CruiseTargetCategory getCategory() { return category; }
    public boolean setCategory(CruiseTargetCategory value,CruiseBuild build) {
        if(value==null || !value.supports(build.get(CruiseSlot.SEEKER))) return false;
        category=value;return true;
    }
    public boolean setMode(Mode value,CruiseBuild build) {
        if(value==null || value==Mode.SEARCH && (!hasSeeker(build) || !category.supports(build.get(CruiseSlot.SEEKER)))) return false;
        mode=value;
        if(mode==Mode.COORDINATE && goals.size()>1) { Vec3d first=goals.get(0);goals.clear();goals.add(first); }
        return true;
    }
    public static boolean hasSeeker(CruiseBuild build) {
        return build.get(CruiseSlot.SEEKER)!=null && build.get(CruiseSlot.SEEKER)!=CruisePartDefinition.SEEKER_NONE;
    }
    public void clear() { goals.clear();malformed=false; }
    public void removeLast() { if(!goals.isEmpty()) goals.remove(goals.size()-1); }
    public boolean setTarget(Vec3d point,int dim) {
        if(!validPoint(point)) return false;
        clear();dimension=dim;goals.add(point);return true;
    }
    public boolean append(Vec3d point,int dim) {
        if(goals.size()>=(mode==Mode.COORDINATE?1:8) || !validPoint(point) || !goals.isEmpty() && dimension!=dim) return false;
        dimension=dim;goals.add(point);return true;
    }
    private static boolean validPoint(Vec3d p) {
        return p!=null && Double.isFinite(p.x) && Double.isFinite(p.y) && Double.isFinite(p.z)
            && Math.abs(p.x)<=29999984 && Math.abs(p.z)<=29999984 && p.y>=0 && p.y<=255;
    }
    public List<Vec3d> getTargets() { return Collections.unmodifiableList(goals); }
    /** Compatibility API: entries now describe target/search areas, not transit. */
    public List<Vec3d> getRoute() { return getTargets(); }
    public int getDimension() { return dimension; }
    public boolean isValidFor(CruiseBuild build,int dim) {
        if(malformed || goals.isEmpty() || dimension!=dim || build.get(CruiseSlot.NAVIGATION)==null) return false;
        if(mode==Mode.SEARCH && (!hasSeeker(build) || !category.supports(build.get(CruiseSlot.SEEKER))) || mode==Mode.COORDINATE && goals.size()!=1 || goals.size()>8) return false;
        if(build.get(CruiseSlot.FUSE)==CruisePartDefinition.FUSE_AIRBURST)
            for(Vec3d p:goals) if(p.y>245) return false;
        return true;
    }
    public double routeLength(Vec3d start) {
        double distance=0;for(Vec3d point:goals) { distance+=start.distanceTo(point);start=point; }return distance;
    }
    public NBTTagCompound write() {
        NBTTagCompound tag=new NBTTagCompound();tag.setInteger("Schema",3);tag.setString("Mode",mode.name());tag.setString("Category",category.name());
        tag.setInteger("Dimension",dimension);tag.setBoolean("Corrupt",malformed);
        NBTTagList points=new NBTTagList();
        for(Vec3d p:goals) { NBTTagCompound n=new NBTTagCompound();n.setDouble("X",p.x);n.setDouble("Y",p.y);n.setDouble("Z",p.z);points.appendTag(n); }
        tag.setTag("Targets",points);return tag;
    }
    public static CruiseMission read(NBTTagCompound tag) {
        CruiseMission result=new CruiseMission();result.dimension=tag.getInteger("Dimension");result.malformed=tag.getBoolean("Corrupt");
        boolean legacy=!tag.hasKey("Schema");
        if(!legacy) {
            if(tag.getInteger("Schema")!=2 && tag.getInteger("Schema")!=3) result.malformed=true;
            try { result.mode=Mode.valueOf(tag.getString("Mode")); } catch(IllegalArgumentException ex) { result.malformed=true; }
            if(tag.getInteger("Schema")==3) try { result.category=CruiseTargetCategory.valueOf(tag.getString("Category")); } catch(IllegalArgumentException ex) { result.malformed=true; }
        }
        NBTTagList points=tag.getTagList(legacy?"Route":"Targets",10);
        if(points.tagCount()>8 || !legacy && result.mode==Mode.COORDINATE && points.tagCount()>1) result.malformed=true;
        // dev50 transit entries are retired. Preserve its final strike goal only.
        for(int i=legacy?Math.max(0,points.tagCount()-1):0;i<Math.min(8,points.tagCount());i++) {
            NBTTagCompound p=points.getCompoundTagAt(i);
            if(!p.hasKey("X",99)||!p.hasKey("Y",99)||!p.hasKey("Z",99)
                    || !result.append(new Vec3d(p.getDouble("X"),p.getDouble("Y"),p.getDouble("Z")),result.dimension)) result.malformed=true;
        }
        return result;
    }
    public void writeToStack(ItemStack stack) {
        if(stack.isEmpty()) return;
        if(!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        stack.getTagCompound().setTag("CruiseMission",write());
    }
    public static CruiseMission fromStack(ItemStack stack) {
        return !stack.isEmpty() && stack.hasTagCompound()?read(stack.getTagCompound().getCompoundTag("CruiseMission")):new CruiseMission();
    }
}
