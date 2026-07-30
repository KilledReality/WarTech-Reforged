package com.wartec.wartecmod.port.integration;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Keeps the owner-team key identical across placed objects, entities and items.
 */
public final class OwnerTeamNbt {
    public static final String OWNER_TEAM_KEY = "WarTechOwnerTeam";
    private static final int MAX_OWNER_TEAM_LENGTH = 64;

    private OwnerTeamNbt() {
    }

    public static String normalize(String team) {
        if (team == null) {
            return "";
        }

        String trimmed = team.trim();
        if (trimmed.equalsIgnoreCase("personal")
            || trimmed.equalsIgnoreCase("solo")
            || trimmed.equalsIgnoreCase("none")) {
            return "";
        }
        StringBuilder result = new StringBuilder(Math.min(trimmed.length(), MAX_OWNER_TEAM_LENGTH));
        for (int index = 0; index < trimmed.length() && result.length() < MAX_OWNER_TEAM_LENGTH; index++) {
            char value = trimmed.charAt(index);
            if (!Character.isISOControl(value)) {
                result.append(value);
            }
        }
        return result.toString();
    }

    public static void write(NBTTagCompound compound, String team) {
        if (compound == null) {
            return;
        }

        String normalized = normalize(team);
        if (normalized.isEmpty()) {
            compound.removeTag(OWNER_TEAM_KEY);
        } else {
            compound.setString(OWNER_TEAM_KEY, normalized);
        }
    }

    public static String read(NBTTagCompound compound, String... legacyKeys) {
        if (compound == null) {
            return "";
        }

        String team = normalize(compound.getString(OWNER_TEAM_KEY));
        if (!team.isEmpty() || legacyKeys == null) {
            return team;
        }

        for (String legacyKey : legacyKeys) {
            if (legacyKey == null || legacyKey.isEmpty()) {
                continue;
            }
            team = normalize(compound.getString(legacyKey));
            if (!team.isEmpty()) {
                return team;
            }
        }
        return "";
    }

    public static void write(ItemStack stack, String team) {
        if (stack == null || stack.isEmpty()) {
            return;
        }

        NBTTagCompound compound = stack.getTagCompound();
        if (compound == null) {
            compound = new NBTTagCompound();
            stack.setTagCompound(compound);
        }
        write(compound, team);
    }

    public static String read(ItemStack stack, String... legacyKeys) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        return read(stack.getTagCompound(), legacyKeys);
    }

    public static String getEntityTeam(Entity entity) {
        if (entity == null) {
            return "";
        }
        if (entity instanceof EntityPlayer) {
            return PlayerTeamPersistence.getPlayerTeam((EntityPlayer) entity);
        }
        if (entity instanceof ITeamOwned) {
            return normalize(((ITeamOwned) entity).getOwnerTeam());
        }
        return read(entity.getEntityData());
    }

    public static void setEntityTeam(Entity entity, String team) {
        if (entity == null) {
            return;
        }

        String normalized = normalize(team);
        if (entity instanceof EntityPlayer) {
            PlayerTeamPersistence.setPlayerTeam((EntityPlayer) entity, normalized);
        } else if (entity instanceof ITeamOwned) {
            ((ITeamOwned) entity).setOwnerTeam(normalized);
        }
        write(entity.getEntityData(), normalized);
    }

    public static boolean areFriendly(String firstTeam, String secondTeam) {
        String first = normalize(firstTeam);
        String second = normalize(secondTeam);
        return !first.isEmpty() && first.equals(second);
    }

    public static String resolvePlacementTeam(ItemStack stack,
            EntityPlayer player) {
        String stored = read(stack, "OwnerTeam", "RadarTeam");
        return stored.isEmpty()
                ? PlayerTeamPersistence.getPlayerTeam(player) : stored;
    }
}
