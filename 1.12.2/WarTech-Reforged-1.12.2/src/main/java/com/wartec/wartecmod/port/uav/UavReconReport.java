package com.wartec.wartecmod.port.uav;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

/** Bounded, persistent terrain coverage and contact report. */
public final class UavReconReport {
    public static final String NBT_KEY = "WarTechUavRecon";
    public static final int SCHEMA_VERSION = 2;
    public static final int MAX_CELLS = 768;
    public static final int MAX_CONTACTS = 96;

    private final LinkedHashMap<Long, SurveyCell> cells =
            new LinkedHashMap<>();
    private final LinkedHashMap<String, ReconContact> contacts =
            new LinkedHashMap<>();
    private int dimension;
    private boolean dimensionSet;
    private long updatedAt;

    public int getDimension() { return dimension; }
    public boolean hasDimension() { return dimensionSet; }
    public long getUpdatedAt() { return updatedAt; }
    public int getCellCount() { return cells.size(); }
    public int getContactCount() { return contacts.size(); }
    public List<SurveyCell> getCells() {
        return Collections.unmodifiableList(new ArrayList<>(cells.values()));
    }
    public List<ReconContact> getContacts() {
        return Collections.unmodifiableList(
                new ArrayList<>(contacts.values()));
    }

    public void beginDimension(int value) {
        if (dimensionSet && dimension != value) clear();
        dimension = value;
        dimensionSet = true;
    }

    public void recordCell(int cellX, int cellZ, int height, int color,
            long worldTime) {
        long key = key(cellX, cellZ);
        cells.remove(key);
        cells.put(key, new SurveyCell(cellX, cellZ,
                Math.max(0, Math.min(255, height)), color));
        trimOldest(cells, MAX_CELLS);
        updatedAt = Math.max(updatedAt, worldTime);
    }

    public void recordContact(Entity entity, int type, boolean friendly,
            float quality, long worldTime) {
        recordContact(entity, type, friendly ? ReconContact.FRIENDLY
                : ReconContact.UNKNOWN, quality, worldTime);
    }

    public void recordContact(Entity entity, int type, int relation,
            float quality, long worldTime) {
        if (entity == null) return;
        String key = entity.getUniqueID().toString();
        contacts.remove(key);
        contacts.put(key, new ReconContact(key, entity.getEntityId(),
                readableName(entity), type,
                (int) Math.floor(entity.posX),
                (int) Math.floor(entity.posY),
                (int) Math.floor(entity.posZ), relation,
                Math.max(0.0F, Math.min(1.0F, quality)), worldTime));
        trimOldest(contacts, MAX_CONTACTS);
        updatedAt = Math.max(updatedAt, worldTime);
    }

    public void clear() {
        cells.clear();
        contacts.clear();
        dimensionSet = false;
        updatedAt = 0L;
    }

    public UavReconReport copy() {
        UavReconReport copy = new UavReconReport();
        copy.dimension = dimension;
        copy.dimensionSet = dimensionSet;
        copy.updatedAt = updatedAt;
        copy.cells.putAll(cells);
        copy.contacts.putAll(contacts);
        return copy;
    }

    public NBTTagCompound writeToNbt() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("Schema", SCHEMA_VERSION);
        tag.setInteger("Dimension", dimension);
        tag.setBoolean("DimensionSet", dimensionSet);
        tag.setLong("Updated", updatedAt);
        NBTTagList cellList = new NBTTagList();
        for (SurveyCell cell : cells.values()) {
            cellList.appendTag(cell.writeToNbt());
        }
        tag.setTag("Cells", cellList);
        NBTTagList contactList = new NBTTagList();
        for (ReconContact contact : contacts.values()) {
            contactList.appendTag(contact.writeToNbt());
        }
        tag.setTag("Contacts", contactList);
        return tag;
    }

    public static UavReconReport readFromNbt(NBTTagCompound tag) {
        UavReconReport report = new UavReconReport();
        if (tag == null) return report;
        report.dimension = tag.getInteger("Dimension");
        report.dimensionSet = tag.getBoolean("DimensionSet");
        report.updatedAt = tag.getLong("Updated");
        NBTTagList cellList = tag.getTagList("Cells",
                Constants.NBT.TAG_COMPOUND);
        for (int index = 0; index < cellList.tagCount()
                && index < MAX_CELLS; ++index) {
            SurveyCell cell = SurveyCell.readFromNbt(
                    cellList.getCompoundTagAt(index));
            report.cells.put(key(cell.x, cell.z), cell);
        }
        NBTTagList contactList = tag.getTagList("Contacts",
                Constants.NBT.TAG_COMPOUND);
        for (int index = 0; index < contactList.tagCount()
                && index < MAX_CONTACTS; ++index) {
            ReconContact contact = ReconContact.readFromNbt(
                    contactList.getCompoundTagAt(index));
            report.contacts.put(contact.uuid, contact);
        }
        return report;
    }

    public void writeToStack(ItemStack stack) {
        NBTTagCompound root = stack.hasTagCompound()
                ? stack.getTagCompound() : new NBTTagCompound();
        root.setTag(NBT_KEY, writeToNbt());
        stack.setTagCompound(root);
    }

    public static UavReconReport fromStack(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTagCompound()) {
            return new UavReconReport();
        }
        return readFromNbt(stack.getTagCompound().getCompoundTag(NBT_KEY));
    }

    private static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }

    private static String readableName(Entity entity) {
        if (entity == null) return "Unknown contact";
        if (entity instanceof com.wartec.wartecmod.port.entity.EntityCustomUav) {
            return ((com.wartec.wartecmod.port.entity.EntityCustomUav) entity)
                    .getBuild().getName();
        }
        if (entity instanceof com.wartec.wartecmod.port.entity.EntityWarTechBase) {
            String name = ((com.wartec.wartecmod.port.entity.EntityWarTechBase)
                    entity).getProfile().name().replace('_', ' ');
            return titleCase(name);
        }
        String name = entity.getName();
        if (name == null || name.trim().isEmpty()) {
            name = entity.getClass().getSimpleName();
        }
        return name == null || name.trim().isEmpty()
                ? "Unknown contact" : name.trim();
    }

    private static String titleCase(String value) {
        StringBuilder result = new StringBuilder();
        boolean upper = true;
        for (int index = 0; index < value.length(); ++index) {
            char character = value.charAt(index);
            if (character == ' ') {
                upper = true;
                result.append(character);
            } else {
                result.append(upper ? Character.toUpperCase(character)
                        : Character.toLowerCase(character));
                upper = false;
            }
        }
        return result.toString();
    }

    private static <K, V> void trimOldest(LinkedHashMap<K, V> values,
            int maximum) {
        while (values.size() > maximum) {
            Iterator<Map.Entry<K, V>> iterator = values.entrySet().iterator();
            if (!iterator.hasNext()) return;
            iterator.next();
            iterator.remove();
        }
    }

    public static final class SurveyCell {
        public final int x;
        public final int z;
        public final int height;
        public final int color;

        SurveyCell(int x, int z, int height, int color) {
            this.x = x;
            this.z = z;
            this.height = height;
            this.color = color;
        }

        NBTTagCompound writeToNbt() {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("X", x);
            tag.setInteger("Z", z);
            tag.setByte("Y", (byte) height);
            tag.setInteger("Color", color);
            return tag;
        }

        static SurveyCell readFromNbt(NBTTagCompound tag) {
            return new SurveyCell(tag.getInteger("X"), tag.getInteger("Z"),
                    tag.getByte("Y") & 255, tag.getInteger("Color"));
        }
    }

    public static final class ReconContact {
        public static final int LIVING = 1;
        public static final int GROUND_VEHICLE = 2;
        public static final int AIRCRAFT = 3;
        public static final int MISSILE = 4;
        public static final int PLAYER = 5;
        public static final int HOSTILE_MOB = 6;
        public static final int PASSIVE_MOB = 7;

        public static final int UNKNOWN = 0;
        public static final int FRIENDLY = 1;
        public static final int HOSTILE = 2;
        public static final int NEUTRAL = 3;

        public final String uuid;
        public final int entityId;
        public final String name;
        public final int type;
        public final int x;
        public final int y;
        public final int z;
        public final boolean friendly;
        public final int relation;
        public final float quality;
        public final long lastSeen;

        ReconContact(String uuid, int entityId, String name, int type,
                int x, int y, int z, int relation, float quality,
                long lastSeen) {
            this.uuid = uuid == null ? "" : uuid;
            this.entityId = entityId;
            this.name = name == null || name.trim().isEmpty()
                    ? "Unknown contact" : name.trim();
            this.type = type;
            this.x = x;
            this.y = y;
            this.z = z;
            this.relation = Math.max(UNKNOWN, Math.min(NEUTRAL, relation));
            this.friendly = this.relation == FRIENDLY;
            this.quality = quality;
            this.lastSeen = lastSeen;
        }

        public boolean isThreat() { return relation == HOSTILE; }
        public boolean isNeutral() { return relation == NEUTRAL; }

        NBTTagCompound writeToNbt() {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("UUID", uuid);
            tag.setInteger("Entity", entityId);
            tag.setString("Name", name);
            tag.setByte("Type", (byte) type);
            tag.setInteger("X", x);
            tag.setInteger("Y", y);
            tag.setInteger("Z", z);
            tag.setBoolean("Friendly", friendly);
            tag.setByte("Relation", (byte) relation);
            tag.setFloat("Quality", quality);
            tag.setLong("Seen", lastSeen);
            return tag;
        }

        static ReconContact readFromNbt(NBTTagCompound tag) {
            int relation = tag.hasKey("Relation", Constants.NBT.TAG_BYTE)
                    ? tag.getByte("Relation") & 255
                    : tag.getBoolean("Friendly") ? FRIENDLY : UNKNOWN;
            return new ReconContact(tag.getString("UUID"),
                    tag.getInteger("Entity"), tag.getString("Name"),
                    tag.getByte("Type") & 255,
                    tag.getInteger("X"), tag.getInteger("Y"),
                    tag.getInteger("Z"), relation,
                    tag.getFloat("Quality"), tag.getLong("Seen"));
        }
    }
}
