package com.wartec.wartecmod.port.uav;

import com.wartec.wartecmod.port.content.UavPartItem;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public final class UavBuild {
    public static final int SCHEMA_VERSION = 1;
    public static final String NBT_KEY = "WarTechUavBuild";
    private final EnumMap<UavSlot, String> parts =
            new EnumMap<>(UavSlot.class);
    private String name = "Custom UAV";

    public UavBuild() {
    }

    public UavBuild setName(String value) {
        String clean = value == null ? "" : value.trim();
        name = clean.isEmpty() ? "Custom UAV"
                : clean.substring(0, Math.min(32, clean.length()));
        return this;
    }

    public String getName() { return name; }

    public UavBuild set(UavSlot slot, UavPartDefinition part) {
        if (slot == null) return this;
        if (part == null) parts.remove(slot);
        else if (part.getSlot() == slot) parts.put(slot, part.getId());
        return this;
    }

    public UavPartDefinition get(UavSlot slot) {
        return UavPartDefinition.byId(parts.get(slot));
    }

    public UavAirframe getAirframe() {
        UavPartDefinition frame = get(UavSlot.AIRFRAME);
        return frame == null ? null : frame.getAirframe();
    }

    public UavStats calculateStats() {
        List<String> errors = new ArrayList<>();
        UavAirframe frame = getAirframe();
        if (frame == null) errors.add("missing_airframe");
        require(UavSlot.PROPULSION, errors);
        require(UavSlot.ENERGY, errors);
        require(UavSlot.FLIGHT_CONTROL, errors);
        require(UavSlot.DATA_LINK, errors);
        if (frame == UavAirframe.RECON && get(UavSlot.SENSOR) == null) {
            errors.add("missing_sensor");
        }
        if (frame != null && frame.getMinimumPayloads() > 0
                && get(UavSlot.PAYLOAD) == null) {
            errors.add("missing_payload");
        }

        double moduleMass = 0.0D;
        for (Map.Entry<UavSlot, String> entry : parts.entrySet()) {
            UavPartDefinition part = UavPartDefinition.byId(entry.getValue());
            if (part == null || part.getSlot() != entry.getKey()) {
                errors.add("unknown_part");
                continue;
            }
            // The airframe item advertises its real structural mass, while
            // baseMass below accounts for it exactly once in flight stats.
            if (entry.getKey() != UavSlot.AIRFRAME) {
                moduleMass += part.getMass();
            }
            if (frame != null && !part.isCompatible(frame)) {
                errors.add("incompatible_" + part.getId());
            }
        }

        double baseMass = frame == null ? 0.0D : frame.getBaseMass();
        double maximumMass = frame == null ? 1.0D : frame.getMaximumMass();
        double totalMass = baseMass + moduleMass;
        if (frame != null && totalMass > maximumMass) errors.add("overweight");

        UavPartDefinition engine = get(UavSlot.PROPULSION);
        UavPartDefinition energy = get(UavSlot.ENERGY);
        UavPartDefinition controller = get(UavSlot.FLIGHT_CONTROL);
        UavPartDefinition link = get(UavSlot.DATA_LINK);
        UavPartDefinition sensor = get(UavSlot.SENSOR);
        UavPartDefinition payload = get(UavSlot.PAYLOAD);
        UavPartDefinition defense = get(UavSlot.DEFENSE);

        double thrust = engine == null ? 0.0D : engine.getPrimary();
        double consumption = engine == null ? 1.0D : engine.getSecondary();
        double minimumSpecificThrust = frame == UavAirframe.ONE_WAY
                ? 0.18D : frame == UavAirframe.RECON ? 0.10D : 0.09D;
        double specificThrust = thrust / Math.max(1.0D, totalMass);
        if (frame != null && specificThrust < minimumSpecificThrust) {
            errors.add("insufficient_thrust");
        }
        double massRatio = frame == null ? 1.0D
                : clamp(frame.getBaseMass() / Math.max(frame.getBaseMass(), totalMass),
                        0.45D, 1.0D);
        double thrustRatio = frame == null ? 0.0D
                : clamp(specificThrust / 0.22D, 0.30D, 2.15D);
        double speed = frame == null ? 0.0D
                : frame.getBaseSpeed() * Math.pow(thrustRatio, 0.72D)
                        * (0.58D + massRatio * 0.42D);
        double controlQuality = controller == null ? 0.5D : controller.getPrimary();
        double turnRate = frame == null ? 0.0D
                : frame.getBaseTurnRate() * controlQuality * (0.55D + massRatio * 0.45D);
        double energyValue = energy == null ? 0.0D : energy.getPrimary();
        int energyPerTick = Math.max(1, (int) Math.ceil(consumption * 3.0D));
        double rawEnergy = energyValue * 6.0D;
        double ceiling = com.wartec.wartecmod.port.integration.WeaponBalance.uavRadius(frame)
                * 2.0D / Math.max(.01D, speed) * energyPerTick;
        int energyCapacity = (int)Math.round(ceiling <= 0 ? 0 : ceiling * rawEnergy / (ceiling + rawEnergy));
        int enduranceTicks = energyCapacity / energyPerTick;
        // Mission radius reserves return flight and maneuvering; telemetry range remains independent.
        int range = (int) Math.floor(enduranceTicks * speed * 0.42D);
        int linkRange = link == null ? 0 : (int) Math.round(link.getPrimary());
        float blast = payload == null ? 0.0F : (float) payload.getPrimary();
        int hardpoints = payload == null || frame == null ? 0
                : Math.min(frame.getMaximumHardpoints(), payload.getHardpoints());
        int flares = defense == UavPartDefinition.DEFENSE_FLARES
                ? (int) defense.getPrimary() : 0;
        double sensorQuality = sensor == null ? 0.0D : sensor.getPrimary();
        float health = frame == null ? 1.0F
                : (float) (frame.getBaseHealth() * (0.82D + massRatio * 0.18D));

        return new UavStats(frame, totalMass, maximumMass, speed, turnRate,
                Math.max(0, range), linkRange, energyCapacity, energyPerTick,
                health, blast, hardpoints, flares, sensorQuality, errors);
    }

    private void require(UavSlot slot, List<String> errors) {
        if (get(slot) == null) {
            errors.add("missing_" + slot.name().toLowerCase(Locale.ROOT));
        }
    }

    public NBTTagCompound writeToNbt() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("Schema", SCHEMA_VERSION);
        tag.setString("Name", name);
        NBTTagCompound partTag = new NBTTagCompound();
        for (UavSlot slot : UavSlot.values()) {
            String id = parts.get(slot);
            if (id != null) partTag.setString(slot.name(), id);
        }
        tag.setTag("Parts", partTag);
        tag.setInteger("Checksum", checksum());
        return tag;
    }

    public static UavBuild readFromNbt(NBTTagCompound tag) {
        UavBuild build = new UavBuild();
        if (tag == null) return build;
        build.setName(tag.getString("Name"));
        NBTTagCompound partTag = tag.getCompoundTag("Parts");
        for (UavSlot slot : UavSlot.values()) {
            UavPartDefinition part = UavPartDefinition.byId(
                    partTag.getString(slot.name()));
            if (part != null && part.getSlot() == slot) build.set(slot, part);
        }
        return build;
    }

    public static UavBuild fromStack(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTagCompound()) return new UavBuild();
        return readFromNbt(stack.getTagCompound().getCompoundTag(NBT_KEY));
    }

    public void writeToStack(ItemStack stack) {
        NBTTagCompound root = stack.hasTagCompound()
                ? stack.getTagCompound() : new NBTTagCompound();
        root.setTag(NBT_KEY, writeToNbt());
        stack.setTagCompound(root);
    }

    public static UavBuild fromInventory(IInventory inventory) {
        UavBuild build = new UavBuild();
        for (UavSlot slot : UavSlot.values()) {
            ItemStack stack = inventory.getStackInSlot(slot.ordinal());
            if (!stack.isEmpty() && stack.getItem() instanceof UavPartItem) {
                UavPartDefinition part = ((UavPartItem) stack.getItem())
                        .getDefinition(stack);
                if (part.getSlot() == slot) build.set(slot, part);
            }
        }
        return build;
    }

    public int checksum() {
        int result = 17;
        for (UavSlot slot : UavSlot.values()) {
            String id = parts.get(slot);
            result = 31 * result + (id == null ? 0 : id.hashCode());
        }
        return result;
    }

    public static UavBuild starter(UavAirframe frame) {
        UavBuild build = new UavBuild();
        if (frame == UavAirframe.ONE_WAY) {
            return build.setName("Wasp OW-1")
                    .set(UavSlot.AIRFRAME, UavPartDefinition.FRAME_ONE_WAY)
                    .set(UavSlot.PROPULSION, UavPartDefinition.ENGINE_ECONOMY)
                    .set(UavSlot.ENERGY, UavPartDefinition.FUEL_COMPACT)
                    .set(UavSlot.FLIGHT_CONTROL, UavPartDefinition.CONTROL_BASIC)
                    .set(UavSlot.DATA_LINK, UavPartDefinition.LINK_SHORT)
                    .set(UavSlot.SENSOR, UavPartDefinition.SENSOR_DAY)
                    .set(UavSlot.PAYLOAD, UavPartDefinition.WARHEAD_HE);
        }
        if (frame == UavAirframe.RECON) {
            return build.setName("Falcon R-1")
                    .set(UavSlot.AIRFRAME, UavPartDefinition.FRAME_RECON)
                    .set(UavSlot.PROPULSION, UavPartDefinition.ENGINE_BALANCED)
                    .set(UavSlot.ENERGY, UavPartDefinition.FUEL_LONG_RANGE)
                    .set(UavSlot.FLIGHT_CONTROL, UavPartDefinition.CONTROL_PRECISION)
                    .set(UavSlot.DATA_LINK, UavPartDefinition.LINK_SATELLITE)
                    .set(UavSlot.SENSOR, UavPartDefinition.SENSOR_EO_IR)
                    .set(UavSlot.DEFENSE, UavPartDefinition.DEFENSE_FLARES);
        }
        return build.setName("Kite S-1")
                .set(UavSlot.AIRFRAME, UavPartDefinition.FRAME_STRIKE)
                .set(UavSlot.PROPULSION, UavPartDefinition.ENGINE_HEAVY)
                .set(UavSlot.ENERGY, UavPartDefinition.FUEL_LONG_RANGE)
                .set(UavSlot.FLIGHT_CONTROL, UavPartDefinition.CONTROL_COMBAT)
                .set(UavSlot.DATA_LINK, UavPartDefinition.LINK_ENCRYPTED)
                .set(UavSlot.SENSOR, UavPartDefinition.SENSOR_EO_IR)
                .set(UavSlot.PAYLOAD, UavPartDefinition.RACK_HEAVY)
                .set(UavSlot.DEFENSE, UavPartDefinition.DEFENSE_FLARES);
    }
    /** Ready-to-configure examples: no hidden mass exemptions. */
    public static UavBuild cruiseCarrier(UavAirframe frame) {
        if(frame==UavAirframe.ONE_WAY) throw new IllegalArgumentException("Reusable carrier required");
        UavBuild build=starter(frame).set(UavSlot.PAYLOAD,UavPartDefinition.RACK_CRUISE);
        if(frame==UavAirframe.RECON) build.set(UavSlot.ENERGY,UavPartDefinition.FUEL_COMPACT);
        return build;
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
