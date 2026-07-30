/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  api.hbm.entity.IRadarDetectable
 *  api.hbm.entity.IRadarDetectable$RadarTargetType
 *  api.hbm.entity.IRadarDetectableNT
 *  net.minecraft.entity.Entity
 *  net.minecraft.util.Vec3
 *  net.minecraft.world.World
 */
package com.wartec.wartecmod.port.network;

import api.hbm.entity.IRadarDetectable;

import com.wartec.wartecmod.port.integration.ElectronicWarfareService;

import com.wartec.wartecmod.port.integration.ITeamOwned;
import com.wartec.wartecmod.port.integration.MissileChunkLoader;
import com.wartec.wartecmod.port.integration.NetworkTeamHelper;

import com.wartec.wartecmod.port.entity.EntityWarTechAircraft;
import com.wartec.wartecmod.port.entity.EntityWarTechArtilleryProjectile;
import com.wartec.wartecmod.port.entity.EntityWarTechBase;
import com.wartec.wartecmod.port.entity.EntityWarTechMissile;
import com.wartec.wartecmod.port.entity.LegacyMissileSpecification.FlightFamily;
import com.wartec.wartecmod.port.entity.WarTechEntityProfile;
import com.wartec.wartecmod.port.entity.WarTechEntityType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public final class MissileTrackingService {
    private static final double PROTECTED_RADIUS = 192.0;
    private static final double CLOSE_THREAT_RADIUS = 110.0;
    private static final double MAX_CPA_TIME = 320.0;
    private static final long TRACK_TIMEOUT = 40L;
    private static final long RESERVATION_TIME = 80L;
    private static final long RADAR_TIMEOUT = 30L;
    private static final long COMMAND_TIMEOUT = 40L;
    private static final long LAUNCHER_TIMEOUT = 40L;
    private static final long COMMUNICATION_RELAY_TIMEOUT = 40L;
    private static final double RADAR_NETWORK_RANGE = 800.0;
    private static final double COMMAND_RADAR_LINK_RANGE = 1400.0;
    private static final double COMMAND_LAUNCHER_LINK_RANGE = 900.0;
    public static final double COMMUNICATION_RELAY_RANGE = 2400.0;
    private static final int MAX_COMMUNICATION_RELAYS = 64;
    private static final int TIER_3_TRACK_ESTABLISHMENT_TICKS = 50;
    private static final double TIER_3_MIN_RADAR_ALTITUDE = 18.0;
    public static final int FACTION_NODE_RADAR = 1;
    public static final int FACTION_NODE_STRATEGIC_RADAR = 2;
    public static final int FACTION_NODE_LAUNCHER = 3;
    public static final int FACTION_NODE_COMMAND = 4;
    public static final int FACTION_NODE_RELAY = 5;
    public static final int FACTION_CONTACT_UNKNOWN = 0;
    public static final int FACTION_CONTACT_MISSILE = 1;
    public static final int FACTION_CONTACT_AIRCRAFT = 2;
    public static final int FACTION_CONTACT_HEAVY_AIRCRAFT = 3;
    public static final int FACTION_CONTACT_BALLISTIC = 4;
    public static final int FACTION_CONTACT_DRONE = 5;
    public static final int FACTION_CONTACT_ARTILLERY_ROCKET = 6;
    private static final int MAX_FACTION_NODES = 192;
    private static final int MAX_FACTION_CONTACTS = 128;
    private static final int MAX_FACTION_SECTORS = 256;
    private static final int HBM_ARTILLERY_NONE = 0;
    private static final int HBM_ARTILLERY_SHELL = 1;
    private static final int HBM_ARTILLERY_ROCKET = 2;
    private static final double ARTILLERY_SHELL_INTERCEPT_RANGE = 220.0;
    private static final double ARTILLERY_IFF_INFERENCE_RANGE = 192.0;
    private static final double HEAVY_HENRY_TIER_ONE_RANGE = 70.0;
    private static final Map<World, WorldTracks> WORLDS = new WeakHashMap<World, WorldTracks>();
    private static final Map<Class<?>, CoordinateFields> COORDINATE_FIELDS = new WeakHashMap();
    private static final Map<Class<?>, Integer> HBM_ARTILLERY_TYPES = new WeakHashMap();
    private static final Map<Class<?>, RocketTypeAccess> HBM_ROCKET_TYPE_ACCESS = new WeakHashMap();

    private MissileTrackingService() {
    }

    public static void registerLaunch(Entity entity, double d, double d2, double d3, int n, int n2) {
        MissileTrackingService.registerLaunch(entity, d, d2, d3, n, n2, "");
    }

    public static void registerLaunch(Entity entity, double d, double d2, double d3, int n, int n2, String string) {
        if (entity == null || entity.world == null || entity.world.isRemote) {
            return;
        }
        World world = entity.world;
        WorldTracks worldTracks = MissileTrackingService.getWorldTracks(world);
        Track track = MissileTrackingService.getOrCreateTrack(worldTracks, entity, world.getTotalWorldTime());
        track.originX = d;
        track.originY = d2;
        track.originZ = d3;
        track.targetX = n;
        track.targetZ = n2;
        track.originKnown = true;
        track.targetKnown = true;
        track.explicitLaunch = true;
        String string2 = MissileTrackingService.normalizeTeam(string);
        if (string2.length() == 0) {
            string2 = NetworkTeamHelper.getEntityTeam(entity);
        }
        if (string2.length() == 0) {
            string2 = MissileTrackingService.findNetworkTeamNear(worldTracks, d, d2, d3, world.getTotalWorldTime());
        }
        track.team = string2;
        if (entity instanceof ITeamOwned && string2.length() > 0) {
            ((ITeamOwned)entity).setOwnerTeam(string2);
        }
        MissileChunkLoader.track(entity);
    }

    public static Entity findThreat(World world, double d, double d2, double d3, int n, double d4, long l) {
        return MissileTrackingService.findThreat(world, d, d2, d3, n, d4, l, "");
    }

    public static Entity findThreat(World world, double d, double d2, double d3, int n, double d4, long l, String string) {
        return MissileTrackingService.findThreat(world, d, d2, d3, n, d4, l, false, string);
    }

    public static Entity findCloseThreat(World world, double d, double d2, double d3, int n, double d4, long l) {
        return MissileTrackingService.findPointDefenseThreat(world, d, d2, d3, d4, "");
    }

    public static Entity findCloseThreat(World world, double d, double d2, double d3, int n, double d4, long l, String string) {
        return MissileTrackingService.findPointDefenseThreat(world, d, d2, d3, d4, string);
    }

    public static Entity findPointDefenseThreat(World world, double d, double d2, double d3, double d4) {
        return MissileTrackingService.findPointDefenseThreat(world, d, d2, d3, d4, "");
    }

    public static Entity findPointDefenseThreat(World world, double d, double d2, double d3, double d4, String string) {
        if (world == null || world.isRemote) {
            return null;
        }
        WorldTracks worldTracks = MissileTrackingService.getWorldTracks(world);
        long l = world.getTotalWorldTime();
        MissileTrackingService.refresh(world, worldTracks, l);
        String string2 = MissileTrackingService.normalizeTeam(string);
        if (string2.length() == 0) {
            MissileTrackingService.expireNetworkNodes(worldTracks, l);
            CommandStation commandStation =
                    MissileTrackingService.findLinkedCommand(worldTracks, d, d2, d3, l);
            if (commandStation != null) {
                string2 = commandStation.team;
            }
        }
        Entity selected = null;
        double d5 = Double.MAX_VALUE;
        double d6 = d4 * d4;
        for (Object e : world.loadedEntityList) {
            double d7;
            double d8;
            double d9;
            double d10;
            if (!(e instanceof Entity)) continue;
            Entity entity = (Entity)e;
            int n = MissileTrackingService.getTargetTier(entity);
            Track track = worldTracks.tracks.get(entity.getEntityId());
            if (n == 0 || entity.isDead || NetworkTeamHelper.isFriendly(string2, entity) || MissileTrackingService.isFriendlyTrack(string2, track) || (d10 = (d9 = entity.posX - d) * d9 + (d8 = entity.posY - d2) * d8 + (d7 = entity.posZ - d3) * d7) > d6) continue;
            double d11 = d9 * entity.motionX + d8 * entity.motionY + d7 * entity.motionZ;
            double d12 = d10 + (double)n * d6 * 0.12;
            if (d11 < 0.0) {
                d12 *= 0.45;
            }
            if (MissileTrackingService.isDroneTarget(entity)) {
                d12 *= 0.55;
            }
            if (!(d12 < d5)) continue;
            d5 = d12;
            selected = entity;
        }
        return selected;
    }

    public static Entity findAirInterceptTarget(World world, double d, double d2, double d3, double d4, String string, long l) {
        if (world == null || world.isRemote) {
            return null;
        }
        WorldTracks worldTracks = MissileTrackingService.getWorldTracks(world);
        long l2 = world.getTotalWorldTime();
        MissileTrackingService.refresh(world, worldTracks, l2);
        MissileTrackingService.expireReservations(world, worldTracks, l2);
        MissileTrackingService.expireRadars(worldTracks, l2);
        MissileTrackingService.expireNetworkNodes(worldTracks, l2);
        CommandStation commandStation = MissileTrackingService.findLinkedCommand(worldTracks, d, d2, d3, l2, string);
        if (commandStation == null) {
            return null;
        }
        String string2 = MissileTrackingService.normalizeTeam(string);
        if (string2.length() == 0) {
            string2 = commandStation.team;
        }
        Entity entity = null;
        double d5 = Double.MAX_VALUE;
        double d6 = d4 * d4;
        for (Track track : worldTracks.tracks.values()) {
            double d7;
            double d8;
            double d9;
            double d10;
            Entity entity2 = track.entity;
            int n = MissileTrackingService.getTargetTier(entity2);
            if (n == 0 || entity2.isDead || MissileTrackingService.isFriendlyTrack(string2, track) || !MissileTrackingService.isAirInterceptable(entity2) || !MissileTrackingService.hasCommandRadarContact(track, worldTracks, commandStation, l2)) continue;
            Integer n2 = track.entityId;
            Long l3 = worldTracks.blockedUntil.get(n2);
            Reservation reservation = worldTracks.reservations.get(n2);
            if (l3 != null && l3 > l2 || reservation != null && reservation.expiresAt >= l2 && reservation.ownerKey != l || (d10 = (d9 = entity2.posX - d) * d9 + (d8 = entity2.posY - d2) * d8 + (d7 = entity2.posZ - d3) * d7) > d6) continue;
            double d11 = d9 * entity2.motionX + d7 * entity2.motionZ;
            double d12 = d10 + (double)n * d6 * 0.06;
            if (d11 < 0.0) {
                d12 *= 0.62;
            }
            if (!(d12 < d5)) continue;
            d5 = d12;
            entity = entity2;
        }
        return entity;
    }

    private static boolean isAirInterceptable(Entity entity) {
        if (entity == null || MissileTrackingService.isBallisticTarget(entity)) {
            return false;
        }
        if (entity instanceof EntityWarTechAircraft) {
            return isFlyingAircraft((EntityWarTechAircraft) entity);
        }
        String string = entity.getClass().getName();
        return string.endsWith(".EntityGeran") || string.contains("CruiseMissile") || string.endsWith(".EntityKh555");
    }

    private static boolean isFlyingAircraft(EntityWarTechAircraft aircraft) {
        return aircraft != null && aircraft.getLegacyState() != 0
                && aircraft.getLegacyState() != 6;
    }

    private static Entity findThreat(World world, double d, double d2, double d3, int n, double d4, long l, boolean bl, String string) {
        if (world == null || world.isRemote) {
            return null;
        }
        WorldTracks worldTracks = MissileTrackingService.getWorldTracks(world);
        long l2 = world.getTotalWorldTime();
        MissileTrackingService.refresh(world, worldTracks, l2);
        MissileTrackingService.expireReservations(world, worldTracks, l2);
        MissileTrackingService.expireNetworkNodes(worldTracks, l2);
        MissileTrackingService.updateLauncher(worldTracks, l, d, d2, d3, n, string, l2);
        Entity entity = null;
        double d5 = Double.MAX_VALUE;
        double d6 = d4 * d4;
        MissileTrackingService.expireRadars(worldTracks, l2);
        CommandStation commandStation = MissileTrackingService.findLinkedCommand(worldTracks, d, d2, d3, l2, string);
        String string2 = MissileTrackingService.normalizeTeam(string);
        if (string2.length() == 0 && commandStation != null) {
            string2 = commandStation.team;
        }
        for (Track track : worldTracks.tracks.values()) {
            boolean bl2;
            Entity entity2 = track.entity;
            int n2 = MissileTrackingService.getTargetTier(entity2);
            if (n2 == 0 || entity2.isDead || MissileTrackingService.isFriendlyTrack(string2, track) || !MissileTrackingService.canInterceptorEngage(entity2, n, d4)) continue;
            Integer n3 = track.entityId;
            Long l3 = worldTracks.blockedUntil.get(n3);
            if (!bl && l3 != null && l3 > l2) continue;
            Reservation reservation = worldTracks.reservations.get(n3);
            if (!bl && reservation != null && reservation.expiresAt >= l2 && reservation.ownerKey != l) continue;
            double d7 = entity2.posX - d;
            double d8 = entity2.posY - d2;
            double d9 = entity2.posZ - d3;
            double d10 = d7 * d7 + d8 * d8 + d9 * d9;
            double d11 = MissileTrackingService.isBallisticTarget(entity2) ? d7 * d7 + d9 * d9 : d10;
            double d12 = d6;
            if (n == 1 && MissileTrackingService.isHbmHeavyArtilleryRocket(entity2)) {
                double d13 = Math.min(d4, 70.0);
                d12 = d13 * d13;
            }
            if (d11 > d12) {
                MissileTrackingService.clearThreatState(track, l);
                continue;
            }
            boolean bl3 = bl2 = commandStation != null ? MissileTrackingService.hasCommandRadarContact(track, worldTracks, commandStation, l2) : MissileTrackingService.hasLinkedRadarContact(track, worldTracks, d, d2, d3, l2, string2);
            if (!bl2 && d11 > 12100.0) {
                MissileTrackingService.clearThreatState(track, l);
                continue;
            }
            Threat threat = MissileTrackingService.evaluateThreat(track, d, d3, l2);
            if (!threat.threatening) {
                MissileTrackingService.clearThreatState(track, l);
                continue;
            }
            if (!MissileTrackingService.confirmThreat(track, l, l2, threat.immediate)) continue;
            double d14 = threat.timeToClosest * 120.0 + threat.closestDistance * 4.0 + Math.sqrt(d10);
            int n4 = n2 - n;
            if (n4 == 1) {
                d14 *= 2.5;
            } else if (n4 >= 2) {
                d14 *= 6.0;
            }
            if (!(d14 < d5)) continue;
            d5 = d14;
            entity = entity2;
        }
        return entity;
    }

    public static void updateLauncherPresence(World world, double d, double d2, double d3, int n, long l) {
        MissileTrackingService.updateLauncherPresence(world, d, d2, d3, n, l, "");
    }

    public static void updateLauncherPresence(World world, double d, double d2, double d3, int n, long l, String string) {
        if (world == null || world.isRemote) {
            return;
        }
        WorldTracks worldTracks = MissileTrackingService.getWorldTracks(world);
        long l2 = world.getTotalWorldTime();
        MissileTrackingService.expireNetworkNodes(worldTracks, l2);
        MissileTrackingService.updateLauncher(worldTracks, l, d, d2, d3, n, string, l2);
    }

    public static int updateRadarSweep(World world, int n, double d, double d2, double d3, double d4, double d5) {
        return MissileTrackingService.updateRadarSweep(world, n, d, d2, d3, d4, d5, Integer.MAX_VALUE, "", 2);
    }

    public static int updateRadarSweep(World world, int n, double d, double d2, double d3, double d4, double d5, int n2) {
        return MissileTrackingService.updateRadarSweep(world, n, d, d2, d3, d4, d5, n2, "", 2);
    }

    public static int updateRadarSweep(World world, int n, double d, double d2, double d3, double d4, double d5, int n2, String string, int n3) {
        return MissileTrackingService.updateRadarSweep(world, n, d, d2, d3, d4, d5, n2, string, n3, false);
    }

    public static int updateStrategicRadarSweep(World world, int n, double d, double d2, double d3, double d4, double d5, int n2, String string, int n3) {
        return MissileTrackingService.updateRadarSweep(world, n, d, d2, d3, d4, d5, n2, string, n3, true);
    }

    private static int updateRadarSweep(World world, int n, double d, double d2, double d3, double d4, double d5, int n2, String string, int n3, boolean bl) {
        if (world == null || world.isRemote || n <= 0) {
            return 0;
        }
        WorldTracks worldTracks = MissileTrackingService.getWorldTracks(world);
        long l = world.getTotalWorldTime();
        MissileTrackingService.refresh(world, worldTracks, l);
        RadarStation radarStation = worldTracks.radars.get(n);
        if (radarStation == null) {
            radarStation = new RadarStation(n);
            worldTracks.radars.put(n, radarStation);
        }
        radarStation.x = d;
        radarStation.y = d2;
        radarStation.z = d3;
        radarStation.range = d4;
        radarStation.ceiling = d5;
        radarStation.team = string == null ? "" : string;
        radarStation.frequencyBand = n3;
        radarStation.lastUpdate = l;
        ElectronicWarfareService.updateEmitter(world, n, d, d2, d3, 0, n3, radarStation.team);
        ElectronicWarfareService.JammingResult jammingResult = ElectronicWarfareService.getJamming(world, d, d2, d3, n3, radarStation.team);
        radarStation.jamming = jammingResult.noise;
        int n4 = 0;
        double d6 = d4 * d4;
        Integer n5 = n;
        for (Track track : worldTracks.tracks.values()) {
            boolean bl2;
            Entity entity = track.entity;
            if (entity == null || entity.isDead || MissileTrackingService.getTargetTier(entity) == 0 || bl && !MissileTrackingService.isStrategicRadarTarget(entity)) continue;
            double d7 = entity.posX - d;
            double d8 = entity.posY - d2;
            double d9 = entity.posZ - d3;
            boolean bl3 = d7 * d7 + d9 * d9 <= d6 && d8 >= -64.0 && d8 <= d5;
            Float f = track.radarQuality.get(n5);
            double d10 = f == null ? 0.0 : (double)f.floatValue();
            boolean bl4 = bl2 = bl3 && (jammingResult.noise < 0.05 || world.rand.nextDouble() >= jammingResult.noise * 0.78);
            d10 = bl2 ? Math.min(1.0, d10 + 0.22 + (1.0 - jammingResult.noise) * 0.36) : Math.max(0.0, d10 - (bl3 ? 0.16 : 0.35));
            track.radarQuality.put(n5, Float.valueOf((float)d10));
            if (n4 < n2 && d10 >= 0.34) {
                track.radarSeen.put(n5, l);
                ++n4;
                continue;
            }
            if (!(d10 < 0.18)) continue;
            track.radarSeen.remove(n5);
        }
        MissileTrackingService.expireRadars(worldTracks, l);
        return Math.min(n2, n4 + jammingResult.falseContacts);
    }

    public static boolean isStrategicRadarTarget(Entity entity) {
        if (entity == null || entity.isDead) {
            return false;
        }
        if (MissileTrackingService.isHbmArtilleryShell(entity)) {
            return false;
        }
        if (MissileTrackingService.isHbmArtilleryRocket(entity)) {
            return true;
        }
        if (entity instanceof EntityWarTechAircraft) {
            EntityWarTechAircraft aircraft = (EntityWarTechAircraft) entity;
            return aircraft.getProfile() != WarTechEntityProfile.MQ_9_REAPER
                    && isFlyingAircraft(aircraft);
        }
        String string = entity.getClass().getName();
        String string2 = string.toLowerCase(Locale.ROOT);
        if (string.endsWith(".EntityGeran") || string2.contains("drone") || string2.contains(".uav") || string2.contains("quadcopter")) {
            return false;
        }
        return MissileTrackingService.getTargetTier(entity) > 0;
    }

    public static void removeRadar(World world, int n) {
        if (world == null || world.isRemote || n <= 0) {
            return;
        }
        WorldTracks worldTracks = MissileTrackingService.getWorldTracks(world);
        Integer n2 = n;
        worldTracks.radars.remove(n2);
        ElectronicWarfareService.removeNode(world, n);
        for (Track track : worldTracks.tracks.values()) {
            track.radarSeen.remove(n2);
            track.radarQuality.remove(n2);
        }
    }

    public static int[] getRadarBlips(World world, int n, double d, double d2, int n2) {
        if (world == null || world.isRemote || n <= 0 || n2 <= 0) {
            return new int[0];
        }
        WorldTracks worldTracks = MissileTrackingService.getWorldTracks(world);
        long l = world.getTotalWorldTime();
        MissileTrackingService.refresh(world, worldTracks, l);
        int n3 = Math.min(16, n2);
        int[] nArray = new int[n3];
        double[] dArray = new double[n3];
        int n4 = 0;
        Integer n5 = n;
        RadarStation radarStation = worldTracks.radars.get(n5);
        String string = radarStation == null ? "" : radarStation.team;
        for (Track track : worldTracks.tracks.values()) {
            int n6;
            Entity entity = track.entity;
            Long l2 = track.radarSeen.get(n5);
            if (entity == null || entity.isDead || l2 == null || l - l2 > 40L || MissileTrackingService.getTargetTier(entity) == 0 || MissileTrackingService.isFriendlyTrack(string, track)) continue;
            double d3 = entity.posX - d;
            double d4 = entity.posZ - d2;
            double d5 = d3 * d3 + d4 * d4;
            int n7 = MissileTrackingService.clampSignedShort((int)Math.round(d3));
            int n8 = MissileTrackingService.clampSignedShort((int)Math.round(d4));
            int n9 = (n7 & 0xFFFF) << 16 | n8 & 0xFFFF;
            for (n6 = n4; n6 > 0 && dArray[n6 - 1] > d5; --n6) {
                if (n6 >= n3) continue;
                dArray[n6] = dArray[n6 - 1];
                nArray[n6] = nArray[n6 - 1];
            }
            if (n6 >= n3) continue;
            dArray[n6] = d5;
            nArray[n6] = n9;
            if (n4 >= n3) continue;
            ++n4;
        }
        int[] result = new int[n4];
        System.arraycopy(nArray, 0, result, 0, n4);
        return result;
    }

    private static int clampSignedShort(int n) {
        return Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, n));
    }

    public static FactionSnapshot getFactionSnapshot(World world, String string, double d, double d2) {
        String string2 = MissileTrackingService.normalizeTeam(string);
        if (world == null || world.isRemote || string2.length() == 0) {
            return FactionSnapshot.EMPTY;
        }
        WorldTracks worldTracks = MissileTrackingService.getWorldTracks(world);
        long l = world.getTotalWorldTime();
        MissileTrackingService.refresh(world, worldTracks, l);
        MissileTrackingService.expireRadars(worldTracks, l);
        MissileTrackingService.expireNetworkNodes(worldTracks, l);
        MissileTrackingService.expireReservations(world, worldTracks, l);
        FactionTerritoryData.Sector[] sectorArray = FactionTerritoryData.getSectors(world, string2, 256);
        FactionSector[] factionSectorArray = new FactionSector[sectorArray.length];
        for (int i = 0; i < sectorArray.length; ++i) {
            factionSectorArray[i] = new FactionSector(sectorArray[i].x, sectorArray[i].z);
        }
        ArrayList<FactionNode> arrayList2 = new ArrayList<FactionNode>();
        HashSet<Integer> hashSet = new HashSet<Integer>();
        for (RadarStation arrayList3 : worldTracks.radars.values()) {
            if (arrayList2.size() >= 192) break;
            if (l - arrayList3.lastUpdate > 30L || !NetworkTeamHelper.areFriendly(string2, arrayList3.team) || !FactionTerritoryData.isOwnedBy(world, string2, arrayList3.x, arrayList3.z)) continue;
            hashSet.add(arrayList3.entityId);
            int n = arrayList3.range >= 3000.0 ? 2 : 1;
            arrayList2.add(new FactionNode(n, arrayList3.entityId, arrayList3.x, arrayList3.y, arrayList3.z, (int)Math.round(arrayList3.range), arrayList3.frequencyBand));
        }
        for (LauncherStation launcherStation : worldTracks.launchers.values()) {
            if (arrayList2.size() >= 192) break;
            if (l - launcherStation.lastUpdate > 40L || !NetworkTeamHelper.areFriendly(string2, launcherStation.team) || !FactionTerritoryData.isOwnedBy(world, string2, launcherStation.x, launcherStation.z)) continue;
            arrayList2.add(new FactionNode(3, launcherStation.ownerKey, launcherStation.x, launcherStation.y, launcherStation.z, launcherStation.tier, 0));
        }
        for (CommandStation commandStation : worldTracks.commands.values()) {
            if (arrayList2.size() >= 192) break;
            if (l - commandStation.lastUpdate > 40L || !NetworkTeamHelper.areFriendly(string2, commandStation.team) || !FactionTerritoryData.isOwnedBy(world, string2, commandStation.x, commandStation.z)) continue;
            arrayList2.add(new FactionNode(4, commandStation.entityId, commandStation.x, commandStation.y, commandStation.z, 0, 0));
        }
        for (CommunicationRelay communicationRelay : worldTracks.communicationRelays.values()) {
            if (arrayList2.size() >= 192) break;
            if (l - communicationRelay.lastUpdate > 40L || !NetworkTeamHelper.areFriendly(string2, communicationRelay.team) || !FactionTerritoryData.isOwnedBy(world, string2, communicationRelay.x, communicationRelay.z)) continue;
            arrayList2.add(new FactionNode(5, communicationRelay.key, communicationRelay.x, communicationRelay.y, communicationRelay.z, 0, 0));
        }
        ArrayList<FactionContact> hostileContacts = new ArrayList<FactionContact>();
        ArrayList<FactionContact> friendlyContacts = new ArrayList<FactionContact>();
        for (Track track : worldTracks.tracks.values()) {
            if (track.entity == null || track.entity.isDead || l - track.lastSeen > 40L) continue;
            float f = 0.0f;
            int n = 0;
            for (Map.Entry<Integer, Long> radarEntry : track.radarSeen.entrySet()) {
                if (!hashSet.contains(radarEntry.getKey()) || l - radarEntry.getValue() > 30L) continue;
                ++n;
                Float f2 = track.radarQuality.get(radarEntry.getKey());
                if (f2 == null || !(f2.floatValue() > f)) continue;
                f = f2.floatValue();
            }
            if (n == 0) continue;
            boolean bl = MissileTrackingService.isFriendlyTrack(string2, track);
            FactionContact contact = new FactionContact(track.entityId,
                    MissileTrackingService.classifyFactionContact(track.entity),
                    MissileTrackingService.getTargetTier(track.entity),
                    track.lastX, track.lastY, track.lastZ,
                    track.velocityX, track.velocityZ, f, n,
                    worldTracks.reservations.containsKey(track.entityId), bl);
            if (bl) {
                friendlyContacts.add(contact);
                continue;
            }
            if (hostileContacts.size() >= 128) continue;
            hostileContacts.add(contact);
        }
        for (FactionContact factionContact : friendlyContacts) {
            if (hostileContacts.size() >= 128) break;
            hostileContacts.add(factionContact);
        }
        return new FactionSnapshot(string2,
                world.provider == null ? 0 : world.provider.getDimension(),
                d, d2, l, factionSectorArray,
                arrayList2.toArray(new FactionNode[arrayList2.size()]),
                hostileContacts.toArray(new FactionContact[hostileContacts.size()]));
    }

    private static int classifyFactionContact(Entity entity) {
        if (MissileTrackingService.isHbmArtilleryRocket(entity)) {
            return 6;
        }
        if (MissileTrackingService.isDroneTarget(entity)) {
            return 5;
        }
        if (entity instanceof EntityWarTechAircraft) {
            WarTechEntityProfile profile = ((EntityWarTechAircraft) entity).getProfile();
            return profile == WarTechEntityProfile.TU_95 ? 3
                    : profile == WarTechEntityProfile.MQ_9_REAPER ? 5 : 2;
        }
        if (MissileTrackingService.isBallisticTarget(entity)) {
            return 4;
        }
        return MissileTrackingService.getTargetTier(entity) > 0 ? 1 : 0;
    }

    public static CommandSnapshot updateCommandPost(World world, int n, double d, double d2, double d3) {
        return MissileTrackingService.updateCommandPost(world, n, d, d2, d3, "");
    }

    public static CommandSnapshot updateCommandPost(World world, int n, double d, double d2, double d3, String string) {
        if (world == null || world.isRemote || n <= 0) {
            return CommandSnapshot.EMPTY;
        }
        WorldTracks worldTracks = MissileTrackingService.getWorldTracks(world);
        long l = world.getTotalWorldTime();
        MissileTrackingService.refresh(world, worldTracks, l);
        MissileTrackingService.expireRadars(worldTracks, l);
        MissileTrackingService.expireNetworkNodes(worldTracks, l);
        CommandStation commandStation = worldTracks.commands.get(n);
        if (commandStation == null) {
            commandStation = new CommandStation(n);
            worldTracks.commands.put(n, commandStation);
        }
        commandStation.x = d;
        commandStation.y = d2;
        commandStation.z = d3;
        commandStation.team = string == null ? "" : string;
        commandStation.lastUpdate = l;
        int n2 = 0;
        for (RadarStation object : worldTracks.radars.values()) {
            if (!MissileTrackingService.isRadarLinkedToCommand(object, commandStation, worldTracks, l)) continue;
            ++n2;
        }
        int n3 = 0;
        for (LauncherStation launcherStation : worldTracks.launchers.values()) {
            if (l - launcherStation.lastUpdate > 40L || !NetworkTeamHelper.canShareNetwork(launcherStation.team, commandStation.team) || !MissileTrackingService.endpointsConnected(worldTracks, launcherStation.x, launcherStation.y, launcherStation.z, commandStation.x, commandStation.y, commandStation.z, commandStation.team, l, 900.0)) continue;
            ++n3;
        }
        int contacts = 0;
        for (Track track : worldTracks.tracks.values()) {
            if (MissileTrackingService.isFriendlyTrack(commandStation.team, track) || !MissileTrackingService.hasCommandRadarContact(track, worldTracks, commandStation, l)) continue;
            ++contacts;
        }
        int n4 = ElectronicWarfareService.updatePassiveSweep(world, d, d2, d3, 900.0, commandStation.team);
        int n5 = ElectronicWarfareService.countJammers(world, d, d2, d3, 1400.0, commandStation.team);
        return new CommandSnapshot(n2, n3, contacts, worldTracks.reservations.size(), n4, n5);
    }

    public static void removeCommandPost(World world, int n) {
        if (world == null || world.isRemote || n <= 0) {
            return;
        }
        MissileTrackingService.getWorldTracks((World)world).commands.remove(n);
    }

    public static long communicationRelayKey(int n, int n2, int n3) {
        return ((long)n & 0x3FFFFFFL) << 38 | ((long)n3 & 0x3FFFFFFL) << 12 | (long)n2 & 0xFFFL;
    }

    public static void updateCommunicationRelay(World world, long l, double d, double d2, double d3, String string) {
        if (world == null || world.isRemote) {
            return;
        }
        WorldTracks worldTracks = MissileTrackingService.getWorldTracks(world);
        long l2 = world.getTotalWorldTime();
        Long l3 = l;
        CommunicationRelay communicationRelay = worldTracks.communicationRelays.get(l3);
        if (communicationRelay == null) {
            communicationRelay = new CommunicationRelay(l);
            worldTracks.communicationRelays.put(l3, communicationRelay);
        }
        communicationRelay.x = d;
        communicationRelay.y = d2;
        communicationRelay.z = d3;
        communicationRelay.team = MissileTrackingService.normalizeTeam(string);
        communicationRelay.lastUpdate = l2;
        MissileTrackingService.expireNetworkNodes(worldTracks, l2);
    }

    public static void removeCommunicationRelay(World world, long l) {
        if (world == null || world.isRemote) {
            return;
        }
        MissileTrackingService.getWorldTracks((World)world).communicationRelays.remove(l);
    }

    public static boolean hasNetworkAlarm(World world, double d, double d2, double d3, String string) {
        if (world == null || world.isRemote) {
            return false;
        }
        WorldTracks worldTracks = MissileTrackingService.getWorldTracks(world);
        long l = world.getTotalWorldTime();
        MissileTrackingService.refresh(world, worldTracks, l);
        MissileTrackingService.expireRadars(worldTracks, l);
        MissileTrackingService.expireNetworkNodes(worldTracks, l);
        for (CommandStation commandStation : worldTracks.commands.values()) {
            if (l - commandStation.lastUpdate > 40L || !NetworkTeamHelper.canShareNetwork(string, commandStation.team) || !MissileTrackingService.endpointsConnected(worldTracks, d, d2, d3, commandStation.x, commandStation.y, commandStation.z, string, l, 96.0)) continue;
            for (Track track : worldTracks.tracks.values()) {
                if (track.entity == null || track.entity.isDead || l - track.lastSeen > 40L || MissileTrackingService.getTargetTier(track.entity) <= 0 || MissileTrackingService.isFriendlyTrack(commandStation.team, track) || !MissileTrackingService.hasCommandRadarContact(track, worldTracks, commandStation, l)) continue;
                return true;
            }
        }
        return false;
    }

    public static int countLinkedCommunicationRelays(World world, double d, double d2, double d3, String string) {
        if (world == null || world.isRemote) {
            return 0;
        }
        WorldTracks worldTracks = MissileTrackingService.getWorldTracks(world);
        long l = world.getTotalWorldTime();
        MissileTrackingService.expireNetworkNodes(worldTracks, l);
        return MissileTrackingService.collectReachableRelays(worldTracks, d, d2, d3, string, l).size();
    }

    private static void updateLauncher(WorldTracks worldTracks, long l, double d, double d2, double d3, int n, String string, long l2) {
        Long l3 = l;
        LauncherStation launcherStation = worldTracks.launchers.get(l3);
        if (launcherStation == null) {
            launcherStation = new LauncherStation(l);
            worldTracks.launchers.put(l3, launcherStation);
        }
        launcherStation.x = d;
        launcherStation.y = d2;
        launcherStation.z = d3;
        launcherStation.tier = n;
        launcherStation.team = MissileTrackingService.normalizeTeam(string);
        launcherStation.lastUpdate = l2;
    }

    private static CommandStation findLinkedCommand(WorldTracks worldTracks, double d, double d2, double d3, long l) {
        return MissileTrackingService.findLinkedCommand(worldTracks, d, d2, d3, l, "");
    }

    private static CommandStation findLinkedCommand(WorldTracks worldTracks, double d, double d2, double d3, long l, String string) {
        CommandStation commandStation = null;
        double d4 = Double.MAX_VALUE;
        for (CommandStation commandStation2 : worldTracks.commands.values()) {
            double d5;
            if (l - commandStation2.lastUpdate > 40L || MissileTrackingService.normalizeTeam(string).length() > 0 && !NetworkTeamHelper.canShareNetwork(string, commandStation2.team) || !((d5 = MissileTrackingService.distanceSquared(d, d2, d3, commandStation2.x, commandStation2.y, commandStation2.z)) <= d4) || !MissileTrackingService.endpointsConnected(worldTracks, d, d2, d3, commandStation2.x, commandStation2.y, commandStation2.z, string, l, 900.0)) continue;
            d4 = d5;
            commandStation = commandStation2;
        }
        return commandStation;
    }

    private static boolean hasCommandRadarContact(Track track, WorldTracks worldTracks, CommandStation commandStation, long l) {
        for (Map.Entry<Integer, Long> entry : track.radarSeen.entrySet()) {
            RadarStation radarStation;
            if (l - entry.getValue() > 30L || (radarStation = worldTracks.radars.get(entry.getKey())) == null || !MissileTrackingService.isRadarLinkedToCommand(radarStation, commandStation, worldTracks, l)) continue;
            return true;
        }
        return false;
    }

    private static boolean isRadarLinkedToCommand(RadarStation radarStation, CommandStation commandStation, WorldTracks worldTracks, long l) {
        return l - radarStation.lastUpdate <= 30L && NetworkTeamHelper.canShareNetwork(radarStation.team, commandStation.team) && MissileTrackingService.endpointsConnected(worldTracks, radarStation.x, radarStation.y, radarStation.z, commandStation.x, commandStation.y, commandStation.z, commandStation.team, l, 1400.0);
    }

    private static boolean endpointsConnected(WorldTracks worldTracks, double d, double d2, double d3, double d4, double d5, double d6, String string, long l, double d7) {
        if (MissileTrackingService.distanceSquared(d, d2, d3, d4, d5, d6) <= d7 * d7) {
            return true;
        }
        List<CommunicationRelay> list = MissileTrackingService.collectReachableRelays(worldTracks, d, d2, d3, string, l);
        double d8 = 5760000.0;
        for (CommunicationRelay communicationRelay : list) {
            if (!NetworkTeamHelper.canShareNetwork(string, communicationRelay.team) || !(MissileTrackingService.distanceSquared(communicationRelay.x, communicationRelay.y, communicationRelay.z, d4, d5, d6) <= d8)) continue;
            return true;
        }
        return false;
    }

    private static List<CommunicationRelay> collectReachableRelays(WorldTracks worldTracks, double d, double d2, double d3, String string, long l) {
        ArrayList<CommunicationRelay> arrayList = new ArrayList<CommunicationRelay>();
        HashSet<Long> hashSet = new HashSet<Long>();
        double d4 = 5760000.0;
        for (CommunicationRelay communicationRelay : worldTracks.communicationRelays.values()) {
            if (l - communicationRelay.lastUpdate > 40L || !NetworkTeamHelper.canShareNetwork(string, communicationRelay.team) || !(MissileTrackingService.distanceSquared(d, d2, d3, communicationRelay.x, communicationRelay.y, communicationRelay.z) <= d4)) continue;
            arrayList.add(communicationRelay);
            hashSet.add(communicationRelay.key);
        }
        block1: for (int i = 0; i < arrayList.size() && arrayList.size() < 64; ++i) {
            CommunicationRelay communicationRelay;
            communicationRelay = (CommunicationRelay)arrayList.get(i);
            for (CommunicationRelay communicationRelay2 : worldTracks.communicationRelays.values()) {
                Long l2 = communicationRelay2.key;
                if (hashSet.contains(l2) || l - communicationRelay2.lastUpdate > 40L || !NetworkTeamHelper.canShareNetwork(string, communicationRelay2.team) || !NetworkTeamHelper.canShareNetwork(communicationRelay.team, communicationRelay2.team) || !(MissileTrackingService.distanceSquared(communicationRelay.x, communicationRelay.y, communicationRelay.z, communicationRelay2.x, communicationRelay2.y, communicationRelay2.z) <= d4)) continue;
                arrayList.add(communicationRelay2);
                hashSet.add(l2);
                if (arrayList.size() < 64) continue;
                continue block1;
            }
        }
        return arrayList;
    }

    private static double distanceSquared(double d, double d2, double d3, double d4, double d5, double d6) {
        double d7 = d - d4;
        double d8 = d2 - d5;
        double d9 = d3 - d6;
        return d7 * d7 + d8 * d8 + d9 * d9;
    }

    private static String normalizeTeam(String string) {
        return string == null ? "" : string;
    }

    public static String findNetworkTeamNear(World world, double d, double d2, double d3) {
        if (world == null || world.isRemote) {
            return "";
        }
        WorldTracks worldTracks = MissileTrackingService.getWorldTracks(world);
        long l = world.getTotalWorldTime();
        MissileTrackingService.expireNetworkNodes(worldTracks, l);
        return MissileTrackingService.findNetworkTeamNear(worldTracks, d, d2, d3, l);
    }

    private static boolean isFriendlyTrack(String string, Track track) {
        return track != null && NetworkTeamHelper.areFriendly(MissileTrackingService.normalizeTeam(string), MissileTrackingService.normalizeTeam(track.team));
    }

    private static String findNetworkTeamNear(WorldTracks worldTracks, double d, double d2, double d3, long l) {
        double d4;
        String string = "";
        double d5 = 810000.0;
        for (CommandStation object : worldTracks.commands.values()) {
            if (l - object.lastUpdate > 40L || object.team.length() == 0 || !((d4 = MissileTrackingService.distanceSquared(d, d2, d3, object.x, object.y, object.z)) <= d5)) continue;
            d5 = d4;
            string = object.team;
        }
        if (string.length() > 0) {
            return string;
        }
        d5 = 640000.0;
        for (RadarStation radarStation : worldTracks.radars.values()) {
            if (l - radarStation.lastUpdate > 30L || radarStation.team.length() == 0 || !((d4 = MissileTrackingService.distanceSquared(d, d2, d3, radarStation.x, radarStation.y, radarStation.z)) <= d5)) continue;
            d5 = d4;
            string = radarStation.team;
        }
        if (string.length() > 0) {
            return string;
        }
        d5 = 5760000.0;
        for (CommunicationRelay communicationRelay : worldTracks.communicationRelays.values()) {
            if (l - communicationRelay.lastUpdate > 40L || communicationRelay.team.length() == 0 || !((d4 = MissileTrackingService.distanceSquared(d, d2, d3, communicationRelay.x, communicationRelay.y, communicationRelay.z)) <= d5)) continue;
            d5 = d4;
            string = communicationRelay.team;
        }
        return string;
    }

    private static boolean hasLinkedRadarContact(Track track, WorldTracks worldTracks, double d, double d2, double d3, long l, String string) {
        double d4 = 640000.0;
        for (Map.Entry<Integer, Long> entry : track.radarSeen.entrySet()) {
            double d5;
            double d6;
            double d7;
            RadarStation radarStation;
            if (l - entry.getValue() > 30L || (radarStation = worldTracks.radars.get(entry.getKey())) == null || l - radarStation.lastUpdate > 30L || !((d7 = radarStation.x - d) * d7 + (d6 = radarStation.y - d2) * d6 + (d5 = radarStation.z - d3) * d5 <= d4) && !MissileTrackingService.endpointsConnected(worldTracks, d, d2, d3, radarStation.x, radarStation.y, radarStation.z, string, l, 800.0)) continue;
            return true;
        }
        return false;
    }

    private static void expireRadars(WorldTracks worldTracks, long l) {
        Iterator<Map.Entry<Integer, RadarStation>> iterator = worldTracks.radars.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, RadarStation> entry = iterator.next();
            if (l - entry.getValue().lastUpdate <= 30L) continue;
            Integer n = entry.getKey();
            iterator.remove();
            for (Track track : worldTracks.tracks.values()) {
                track.radarSeen.remove(n);
                track.radarQuality.remove(n);
            }
        }
    }

    private static void expireNetworkNodes(WorldTracks worldTracks, long l) {
        Iterator<Map.Entry<Integer, CommandStation>> iterator = worldTracks.commands.entrySet().iterator();
        while (iterator.hasNext()) {
            if (l - iterator.next().getValue().lastUpdate <= 40L) continue;
            iterator.remove();
        }
        Iterator<Map.Entry<Long, LauncherStation>> iterator2 = worldTracks.launchers.entrySet().iterator();
        while (iterator2.hasNext()) {
            if (l - iterator2.next().getValue().lastUpdate <= 40L) continue;
            iterator2.remove();
        }
        Iterator<Map.Entry<Long, CommunicationRelay>> iterator3 = worldTracks.communicationRelays.entrySet().iterator();
        while (iterator3.hasNext()) {
            if (l - iterator3.next().getValue().lastUpdate <= 40L) continue;
            iterator3.remove();
        }
    }

    public static boolean isBallisticTarget(Entity entity) {
        if (entity == null) {
            return false;
        }
        if (MissileTrackingService.isHbmArtilleryTarget(entity)) {
            return true;
        }
        if (entity instanceof EntityWarTechMissile) {
            FlightFamily family = ((EntityWarTechMissile) entity)
                    .getMissileSpecification().getFlightFamily();
            return family == FlightFamily.BALLISTIC
                    || family == FlightFamily.GLIDE
                    || family == FlightFamily.ASAT;
        }
        for (Class<?> clazz = entity.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
            String string = clazz.getName();
            if (!"com.wartec.wartecmod.entity.missile.EntityBallisticMissileBase".equals(string) && !"com.wartec.wartecmod.entity.missile.EntityGlideWeaponBase".equals(string) && !"com.wartec.wartecmod.entity.missile.EntityKineticRod".equals(string)) continue;
            return true;
        }
        return false;
    }

    public static int getThreatTier(Entity entity) {
        return MissileTrackingService.getTargetTier(entity);
    }

    public static boolean isHbmArtilleryTarget(Entity entity) {
        return MissileTrackingService.getHbmArtilleryType(entity) != 0;
    }

    public static boolean isHbmArtilleryShell(Entity entity) {
        return MissileTrackingService.getHbmArtilleryType(entity) == 1;
    }

    public static boolean isHbmArtilleryRocket(Entity entity) {
        return MissileTrackingService.getHbmArtilleryType(entity) == 2;
    }

    public static boolean isHbmHeavyArtilleryRocket(Entity entity) {
        if (!MissileTrackingService.isHbmArtilleryRocket(entity)) {
            return false;
        }
        if (entity instanceof EntityWarTechArtilleryProjectile) {
            int type = ((EntityWarTechArtilleryProjectile) entity).getAmmoType();
            return type == 1 || type == 5;
        }
        RocketTypeAccess rocketTypeAccess = MissileTrackingService.getRocketTypeAccess(entity.getClass());
        if (rocketTypeAccess != null) {
            try {
                Object object = rocketTypeAccess.getType.invoke(entity, new Object[0]);
                if (object != null) {
                    return rocketTypeAccess.modelType.getInt(object) == 1;
                }
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        return false;
    }

    public static boolean canInterceptorEngage(Entity entity, int n, double d) {
        if (MissileTrackingService.getTargetTier(entity) == 0 || n < 1 || n > 3) {
            return false;
        }
        return !MissileTrackingService.isHbmArtilleryShell(entity) || n <= 2 && d <= 220.0;
    }

    public static void assignProjectileTeam(Entity entity, String string) {
        if (entity == null || entity.world == null || entity.world.isRemote
                || !MissileTrackingService.isHbmArtilleryTarget(entity)) {
            return;
        }
        String string2 = MissileTrackingService.normalizeTeam(string);
        if (string2.length() == 0) {
            return;
        }
        if (entity instanceof EntityWarTechBase) {
            ((EntityWarTechBase) entity).setOwnerTeam(string2);
        }
        World world = entity.world;
        Track track = MissileTrackingService.getOrCreateTrack(MissileTrackingService.getWorldTracks(world), entity, world.getTotalWorldTime());
        track.team = string2;
    }

    public static void assignNewestArtilleryProjectile(World world, double d, double d2, double d3, String string, boolean bl) {
        String string2 = MissileTrackingService.normalizeTeam(string);
        if (world == null || world.isRemote || string2.length() == 0 || world.loadedEntityList == null) {
            return;
        }
        Entity entity = null;
        double d4 = 256.0;
        for (int i = world.loadedEntityList.size() - 1; i >= 0; --i) {
            double d5;
            Object e = world.loadedEntityList.get(i);
            if (!(e instanceof Entity)) continue;
            Entity entity2 = (Entity)e;
            if (entity2.isDead || entity2.ticksExisted > 1 || bl != MissileTrackingService.isHbmArtilleryRocket(entity2) || !bl && !MissileTrackingService.isHbmArtilleryShell(entity2) || !((d5 = MissileTrackingService.distanceSquared(d, d2, d3, entity2.posX, entity2.posY, entity2.posZ)) <= d4)) continue;
            d4 = d5;
            entity = entity2;
        }
        if (entity != null) {
            MissileTrackingService.assignProjectileTeam(entity, string2);
        }
    }

    public static boolean isDroneTarget(Entity entity) {
        if (entity == null) {
            return false;
        }
        if (entity instanceof EntityWarTechAircraft) {
            EntityWarTechAircraft aircraft = (EntityWarTechAircraft) entity;
            return aircraft.getProfile() == WarTechEntityProfile.MQ_9_REAPER
                    && isFlyingAircraft(aircraft);
        }
        String string = entity.getClass().getName();
        return string.endsWith(".EntityGeran");
    }

    public static boolean tryReserve(World world, int n, long l) {
        if (world == null || n <= 0) {
            return false;
        }
        WorldTracks worldTracks = MissileTrackingService.getWorldTracks(world);
        long l2 = world.getTotalWorldTime();
        MissileTrackingService.expireReservations(world, worldTracks, l2);
        Integer n2 = n;
        Long l3 = worldTracks.blockedUntil.get(n2);
        if (l3 != null && l3 > l2) {
            return false;
        }
        Reservation reservation = worldTracks.reservations.get(n2);
        if (reservation != null && reservation.expiresAt >= l2) {
            return false;
        }
        worldTracks.reservations.put(n2, new Reservation(l, 0, l2 + 80L));
        return true;
    }

    public static void confirmReservation(World world, int n, long l, int n2) {
        if (world == null || n <= 0) {
            return;
        }
        WorldTracks worldTracks = MissileTrackingService.getWorldTracks(world);
        Reservation reservation = worldTracks.reservations.get(n);
        if (reservation != null && reservation.ownerKey == l) {
            reservation.interceptorId = n2;
            reservation.expiresAt = world.getTotalWorldTime() + 620L;
        }
    }

    public static void releaseReservation(World world, int n, int n2) {
        if (world == null || n <= 0) {
            return;
        }
        WorldTracks worldTracks = MissileTrackingService.getWorldTracks(world);
        Integer n3 = n;
        Reservation reservation = worldTracks.reservations.get(n3);
        if (reservation == null || n2 == 0 || reservation.interceptorId == 0 || reservation.interceptorId == n2) {
            worldTracks.reservations.remove(n3);
        }
    }

    public static void releaseReservation(World world, int n, long l) {
        if (world == null || n <= 0) {
            return;
        }
        WorldTracks worldTracks = MissileTrackingService.getWorldTracks(world);
        Integer n2 = n;
        Reservation reservation = worldTracks.reservations.get(n2);
        if (reservation != null && reservation.ownerKey == l && reservation.interceptorId == 0) {
            worldTracks.reservations.remove(n2);
        }
    }

    public static void deferTarget(World world, int n) {
        if (world == null || n <= 0 || world.isRemote) {
            return;
        }
        WorldTracks worldTracks = MissileTrackingService.getWorldTracks(world);
        MissileTrackingService.deferTarget(world, worldTracks, n);
    }

    private static void deferTarget(World world, WorldTracks worldTracks, Integer n) {
        long l = world.getTotalWorldTime();
        long l2 = 100L + (long)world.rand.nextInt(41);
        Long l3 = worldTracks.blockedUntil.get(n);
        long l4 = l + l2;
        if (l3 == null || l3 < l4) {
            worldTracks.blockedUntil.put(n, l4);
        }
        worldTracks.reservations.remove(n);
        System.out.println("[WarTec PVO] Retry for target " + n + " delayed by " + l2 + " ticks");
    }

    private static Threat evaluateThreat(Track track, double d, double d2, long l) {
        boolean bl;
        double d3;
        double d4;
        Entity entity = track.entity;
        double d5 = entity.posX - d;
        double d6 = entity.posZ - d2;
        double d7 = Math.sqrt(d5 * d5 + d6 * d6);
        double d8 = 36864.0;
        if (track.targetKnown && (d4 = (double)track.targetX - d) * d4 + (d3 = (double)track.targetZ - d2) * d3 <= d8) {
            return new Threat(true, true, 0.0, 0.0);
        }
        d4 = track.velocityX;
        d3 = track.velocityZ;
        double d9 = d4 * d4 + d3 * d3;
        if (d9 < 1.0E-4) {
            return Threat.NONE;
        }
        double d10 = d5 * d4 + d6 * d3;
        boolean bl2 = bl = d10 >= 0.0;
        if (MissileTrackingService.isLocalOutbound(track, d, d2, bl, l)) {
            return Threat.NONE;
        }
        double d11 = -(d5 * d4 + d6 * d3) / d9;
        if (d11 < 0.0) {
            d11 = 0.0;
        }
        if (d11 > 320.0) {
            return Threat.NONE;
        }
        double d12 = d5 + d4 * d11;
        double d13 = d6 + d3 * d11;
        double d14 = Math.sqrt(d12 * d12 + d13 * d13);
        boolean bl3 = d7 <= 110.0 && !bl;
        boolean bl4 = bl3 || !bl && d14 <= 192.0;
        boolean bl5 = bl3 || d11 <= 20.0;
        return bl4 ? new Threat(true, bl5, d11, d14) : Threat.NONE;
    }

    private static boolean isLocalOutbound(Track track, double d, double d2, boolean bl, long l) {
        if (!track.originKnown || !track.targetKnown) {
            return false;
        }
        double d3 = track.originX - d;
        double d4 = track.originZ - d2;
        if (d3 * d3 + d4 * d4 > 36864.0) {
            return false;
        }
        double d5 = (double)track.targetX - d;
        double d6 = (double)track.targetZ - d2;
        if (d5 * d5 + d6 * d6 <= 36864.0) {
            return false;
        }
        double d7 = (double)track.targetX - track.originX;
        double d8 = (double)track.targetZ - track.originZ;
        double d9 = track.velocityX * d7 + track.velocityZ * d8;
        long l2 = l - track.firstSeen;
        return l2 <= 60L || bl && d9 > 0.0;
    }

    private static boolean confirmThreat(Track track, long l, long l2, boolean bl) {
        Long l3 = l;
        ThreatState threatState = track.threatStates.get(l3);
        if (threatState == null || l2 - threatState.lastSeen > 30L) {
            threatState = new ThreatState();
            track.threatStates.put(l3, threatState);
        }
        threatState.lastSeen = l2;
        ++threatState.confirmations;
        return bl || track.targetKnown || threatState.confirmations >= 2;
    }

    private static void clearThreatState(Track track, long l) {
        track.threatStates.remove(l);
    }

    private static void refresh(World world, WorldTracks worldTracks, long l) {
        if (worldTracks.lastRefresh >= 0L && l - worldTracks.lastRefresh < 5L) {
            return;
        }
        worldTracks.lastRefresh = l;
        HashSet<Integer> hashSet = new HashSet<Integer>();
        List<Entity> list = world.loadedEntityList;
        for (Entity entity : list) {
            if (MissileTrackingService.getTargetTier(entity) == 0) continue;
            int n = entity.getEntityId();
            hashSet.add(n);
            Track track = MissileTrackingService.getOrCreateTrack(worldTracks, entity, l);
            MissileTrackingService.updateTrackFromEntity(track, entity, l);
        }
        Iterator<Map.Entry<Integer, Track>> iterator =
                worldTracks.tracks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Track> entry = iterator.next();
            Track track = entry.getValue();
            if (hashSet.contains(entry.getKey()) && !track.entity.isDead
                    || l - track.lastSeen <= 40L) continue;
            iterator.remove();
            worldTracks.reservations.remove(entry.getKey());
            worldTracks.blockedUntil.remove(entry.getKey());
        }
    }

    private static Track getOrCreateTrack(WorldTracks worldTracks, Entity entity, long l) {
        String string;
        Integer n = entity.getEntityId();
        Track track = worldTracks.tracks.get(n);
        if (track == null || track.entity != entity) {
            track = new Track(entity, l);
            MissileTrackingService.readCoordinates(track, entity);
            worldTracks.tracks.put(n, track);
        }
        if ((string = NetworkTeamHelper.getEntityTeam(entity)).length() > 0) {
            track.team = string;
        } else if (track.team.length() == 0 && MissileTrackingService.isHbmArtilleryTarget(entity) && entity.ticksExisted <= 8) {
            track.team = MissileTrackingService.findArtilleryLaunchTeamNear(worldTracks, entity.posX, entity.posY, entity.posZ, l);
        }
        return track;
    }

    private static void updateTrackFromEntity(Track track, Entity entity, long l) {
        long l2 = l - track.lastSeen;
        if (l2 > 0L) {
            double d = (entity.posX - track.lastX) / (double)l2;
            double d2 = (entity.posZ - track.lastZ) / (double)l2;
            if (track.samples == 0) {
                track.velocityX = d;
                track.velocityZ = d2;
            } else {
                track.velocityX = track.velocityX * 0.65 + d * 0.35;
                track.velocityZ = track.velocityZ * 0.65 + d2 * 0.35;
            }
            ++track.samples;
        } else if (track.samples == 0) {
            track.velocityX = entity.motionX;
            track.velocityZ = entity.motionZ;
        }
        track.entity = entity;
        track.lastX = entity.posX;
        track.lastY = entity.posY;
        track.lastZ = entity.posZ;
        track.lastSeen = l;
        String string = NetworkTeamHelper.getEntityTeam(entity);
        if (string.length() > 0) {
            track.team = string;
        }
        if (!track.originKnown || !track.targetKnown) {
            MissileTrackingService.readCoordinates(track, entity);
        }
    }

    private static void readCoordinates(Track track, Entity entity) {
        CoordinateFields coordinateFields = MissileTrackingService.getCoordinateFields(entity.getClass());
        try {
            Object object;
            if (!track.originKnown && coordinateFields.startX != null && coordinateFields.startZ != null) {
                track.originX = (double)coordinateFields.startX.getInt(entity) + 0.5;
                track.originY = entity.posY;
                track.originZ = (double)coordinateFields.startZ.getInt(entity) + 0.5;
                track.originKnown = true;
            }
            if (!track.targetKnown && coordinateFields.targetX != null && coordinateFields.targetZ != null) {
                track.targetX = coordinateFields.targetX.getInt(entity);
                track.targetZ = coordinateFields.targetZ.getInt(entity);
                track.targetKnown = true;
            }
            if (!track.targetKnown && coordinateFields.targetVector != null
                    && (object = coordinateFields.targetVector.get(entity)) instanceof Vec3d) {
                Vec3d vec3 = (Vec3d)object;
                track.targetX = (int)Math.floor(vec3.x);
                track.targetZ = (int)Math.floor(vec3.z);
                track.targetKnown = true;
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static CoordinateFields getCoordinateFields(Class<?> clazz) {
        Map<Class<?>, CoordinateFields> map = COORDINATE_FIELDS;
        synchronized (map) {
            CoordinateFields coordinateFields = COORDINATE_FIELDS.get(clazz);
            if (coordinateFields != null) {
                return coordinateFields;
            }
            CoordinateFields coordinateFields2 = new CoordinateFields();
            for (Class<?> clazz2 = clazz; clazz2 != null; clazz2 = clazz2.getSuperclass()) {
                if (coordinateFields2.startX == null) {
                    coordinateFields2.startX = MissileTrackingService.findField(clazz2, "startX");
                }
                if (coordinateFields2.startZ == null) {
                    coordinateFields2.startZ = MissileTrackingService.findField(clazz2, "startZ");
                }
                if (coordinateFields2.targetX == null) {
                    coordinateFields2.targetX = MissileTrackingService.findField(clazz2, "targetX");
                }
                if (coordinateFields2.targetZ == null) {
                    coordinateFields2.targetZ = MissileTrackingService.findField(clazz2, "targetZ");
                }
                if (coordinateFields2.targetVector != null) continue;
                coordinateFields2.targetVector = MissileTrackingService.findField(clazz2, "lastTargetPos");
            }
            COORDINATE_FIELDS.put(clazz, coordinateFields2);
            return coordinateFields2;
        }
    }

    private static Field findField(Class<?> clazz, String string) {
        try {
            Field field = clazz.getDeclaredField(string);
            field.setAccessible(true);
            return field;
        }
        catch (Throwable throwable) {
            return null;
        }
    }

    private static int getTargetTier(Entity entity) {
        int n;
        String string;
        if (entity == null || entity.isDead) {
            return 0;
        }
        if (entity instanceof EntityWarTechAircraft
                && !isFlyingAircraft((EntityWarTechAircraft) entity)) {
            return 0;
        }
        if (MissileTrackingService.isHbmArtilleryTarget(entity)) {
            return MissileTrackingService.isHbmHeavyArtilleryRocket(entity) ? 2 : 1;
        }
        if (entity instanceof EntityWarTechMissile) {
            FlightFamily family = ((EntityWarTechMissile) entity)
                    .getMissileSpecification().getFlightFamily();
            if (family == FlightFamily.HYPERSONIC) {
                return MissileTrackingService.applyRadarActivationEnvelope(entity, 3);
            }
            if (family == FlightFamily.SUPERSONIC || family == FlightFamily.KH555) {
                return 2;
            }
            if (family == FlightFamily.SUBSONIC || family == FlightFamily.GERAN
                    || family == FlightFamily.ANTI_RADIATION) {
                return 1;
            }
        }
        for (Class<?> clazz = entity.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
            string = clazz.getName();
            if ("com.wartec.wartecmod.entity.missile.EntityHypersonicCruiseMissileBase".equals(string)) {
                return MissileTrackingService.applyRadarActivationEnvelope(entity, 3);
            }
            if ("com.wartec.wartecmod.entity.missile.EntitySupersonicCruiseMissileBase".equals(string)) {
                return 2;
            }
            if (!"com.wartec.wartecmod.entity.missile.EntitySubsonicCruiseMissileBase".equals(string)) continue;
            return 1;
        }
        if (entity instanceof IRadarDetectable) {
            IRadarDetectable.RadarTargetType targetType =
                    ((IRadarDetectable)entity).getTargetType();
            if (targetType == null
                    || targetType == IRadarDetectable.RadarTargetType.MISSILE_AB
                    || targetType == IRadarDetectable.RadarTargetType.PLAYER) {
                return 0;
            }
            n = targetType.ordinal();
        } else {
            return 0;
        }
        if (n < 0 || n > 9) {
            return 0;
        }
        int n2 = n <= 1 ? 1 : (n == 2 ? 2 : 3);
        return MissileTrackingService.applyRadarActivationEnvelope(entity, n2);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static int getHbmArtilleryType(Entity entity) {
        if (entity == null) {
            return 0;
        }
        if (entity instanceof EntityWarTechArtilleryProjectile) {
            return ((EntityWarTechArtilleryProjectile) entity)
                    .getProjectileKind()
                    == EntityWarTechArtilleryProjectile.KIND_HENRY ? 2 : 1;
        }
        Class<?> clazz = entity.getClass();
        Map<Class<?>, Integer> map = HBM_ARTILLERY_TYPES;
        synchronized (map) {
            Integer n = HBM_ARTILLERY_TYPES.get(clazz);
            if (n != null) {
                return n;
            }
            int n2 = 0;
            for (Class<?> clazz2 = clazz; clazz2 != null; clazz2 = clazz2.getSuperclass()) {
                String string = clazz2.getName();
                if ("com.hbm.entity.projectile.EntityArtilleryShell".equals(string)) {
                    n2 = 1;
                    break;
                }
                if (!"com.hbm.entity.projectile.EntityArtilleryRocket".equals(string)) continue;
                n2 = 2;
                break;
            }
            HBM_ARTILLERY_TYPES.put(clazz, n2);
            return n2;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static RocketTypeAccess getRocketTypeAccess(Class<?> clazz) {
        Map<Class<?>, RocketTypeAccess> map = HBM_ROCKET_TYPE_ACCESS;
        synchronized (map) {
            if (HBM_ROCKET_TYPE_ACCESS.containsKey(clazz)) {
                return HBM_ROCKET_TYPE_ACCESS.get(clazz);
            }
            RocketTypeAccess rocketTypeAccess = null;
            try {
                Method method = clazz.getMethod("getType", new Class[0]);
                Field field = method.getReturnType().getField("modelType");
                method.setAccessible(true);
                field.setAccessible(true);
                rocketTypeAccess = new RocketTypeAccess(method, field);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            HBM_ROCKET_TYPE_ACCESS.put(clazz, rocketTypeAccess);
            return rocketTypeAccess;
        }
    }

    private static String findArtilleryLaunchTeamNear(WorldTracks worldTracks, double d, double d2, double d3, long l) {
        double d4;
        String string = "";
        double d5 = 36864.0;
        for (CommandStation object : worldTracks.commands.values()) {
            if (l - object.lastUpdate > 40L || object.team.length() == 0 || !((d4 = MissileTrackingService.distanceSquared(d, d2, d3, object.x, object.y, object.z)) <= d5)) continue;
            d5 = d4;
            string = object.team;
        }
        for (RadarStation radarStation : worldTracks.radars.values()) {
            if (l - radarStation.lastUpdate > 30L || radarStation.team.length() == 0 || !((d4 = MissileTrackingService.distanceSquared(d, d2, d3, radarStation.x, radarStation.y, radarStation.z)) <= d5)) continue;
            d5 = d4;
            string = radarStation.team;
        }
        for (LauncherStation launcherStation : worldTracks.launchers.values()) {
            if (l - launcherStation.lastUpdate > 40L || launcherStation.team.length() == 0 || !((d4 = MissileTrackingService.distanceSquared(d, d2, d3, launcherStation.x, launcherStation.y, launcherStation.z)) <= d5)) continue;
            d5 = d4;
            string = launcherStation.team;
        }
        for (CommunicationRelay communicationRelay : worldTracks.communicationRelays.values()) {
            if (l - communicationRelay.lastUpdate > 40L || communicationRelay.team.length() == 0 || !((d4 = MissileTrackingService.distanceSquared(d, d2, d3, communicationRelay.x, communicationRelay.y, communicationRelay.z)) <= d5)) continue;
            d5 = d4;
            string = communicationRelay.team;
        }
        return string;
    }

    public static boolean holdReservation(World world, int n, long l) {
        if (world == null || n <= 0) {
            return false;
        }
        WorldTracks worldTracks = MissileTrackingService.getWorldTracks(world);
        Reservation reservation = worldTracks.reservations.get(n);
        if (reservation == null || reservation.ownerKey != l || reservation.interceptorId != 0) {
            return false;
        }
        reservation.expiresAt = world.getTotalWorldTime() + 160L;
        return true;
    }

    private static int applyRadarActivationEnvelope(Entity entity, int n) {
        int n2;
        if (n < 3 || entity.world == null) {
            return n;
        }
        if (entity.ticksExisted < 50) {
            return 0;
        }
        int n3 = (int)Math.floor(entity.posX);
        int n4 = entity.world.getHeight(n3, n2 = (int)Math.floor(entity.posZ));
        return entity.posY - (double)n4 >= 18.0 ? n : 0;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static WorldTracks getWorldTracks(World world) {
        Map<World, WorldTracks> map = WORLDS;
        synchronized (map) {
            WorldTracks worldTracks = WORLDS.get(world);
            if (worldTracks == null) {
                worldTracks = new WorldTracks();
                WORLDS.put(world, worldTracks);
            }
            return worldTracks;
        }
    }

    private static void expireReservations(World world, WorldTracks worldTracks, long l) {
        Iterator<Map.Entry<Integer, Reservation>> iterator = worldTracks.reservations.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Reservation> entry = iterator.next();
            Reservation reservation = entry.getValue();
            Track track = worldTracks.tracks.get(entry.getKey());
            boolean bl2 = track == null || track.entity.isDead;
            Entity entity = reservation.interceptorId == 0
                    ? null : world.getEntityByID(reservation.interceptorId);
            boolean bl3 = reservation.interceptorId != 0
                    && (entity == null || entity.isDead);
            boolean expired = reservation.expiresAt < l;
            if (!expired && !bl2 && !bl3) continue;
            iterator.remove();
            if (bl2 || !expired && !bl3) continue;
            long l2 = 100L + (long)world.rand.nextInt(41);
            worldTracks.blockedUntil.put(entry.getKey(), l + l2);
            System.out.println("[WarTec PVO] Lost interceptor; retry for target "
                    + entry.getKey() + " delayed by " + l2 + " ticks");
        }
        Iterator<Map.Entry<Integer, Long>> blockedIterator =
                worldTracks.blockedUntil.entrySet().iterator();
        while (blockedIterator.hasNext()) {
            Map.Entry<Integer, Long> entry = blockedIterator.next();
            Track track = worldTracks.tracks.get(entry.getKey());
            if (entry.getValue() > l && track != null && !track.entity.isDead) continue;
            blockedIterator.remove();
        }
    }

    private static final class WorldTracks {
        final Map<Integer, Track> tracks = new HashMap<Integer, Track>();
        final Map<Integer, Reservation> reservations = new HashMap<Integer, Reservation>();
        final Map<Integer, Long> blockedUntil = new HashMap<Integer, Long>();
        final Map<Integer, RadarStation> radars = new HashMap<Integer, RadarStation>();
        final Map<Integer, CommandStation> commands = new HashMap<Integer, CommandStation>();
        final Map<Long, LauncherStation> launchers = new HashMap<Long, LauncherStation>();
        final Map<Long, CommunicationRelay> communicationRelays = new HashMap<Long, CommunicationRelay>();
        long lastRefresh = -1L;

        private WorldTracks() {
        }
    }

    private static final class Track {
        Entity entity;
        final int entityId;
        final long firstSeen;
        long lastSeen;
        double lastX;
        double lastY;
        double lastZ;
        double velocityX;
        double velocityZ;
        double originX;
        double originY;
        double originZ;
        int targetX;
        int targetZ;
        int samples;
        boolean originKnown;
        boolean targetKnown;
        boolean explicitLaunch;
        String team = "";
        final Map<Long, ThreatState> threatStates = new HashMap<Long, ThreatState>();
        final Map<Integer, Long> radarSeen = new HashMap<Integer, Long>();
        final Map<Integer, Float> radarQuality = new HashMap<Integer, Float>();

        Track(Entity entity, long l) {
            this.entity = entity;
            this.entityId = entity.getEntityId();
            this.firstSeen = l;
            this.lastSeen = l;
            this.lastX = entity.posX;
            this.lastY = entity.posY;
            this.lastZ = entity.posZ;
            this.velocityX = entity.motionX;
            this.velocityZ = entity.motionZ;
        }
    }

    private static final class CommandStation {
        final int entityId;
        double x;
        double y;
        double z;
        String team = "";
        long lastUpdate;

        CommandStation(int n) {
            this.entityId = n;
        }
    }

    private static final class Reservation {
        final long ownerKey;
        int interceptorId;
        long expiresAt;

        Reservation(long l, int n, long l2) {
            this.ownerKey = l;
            this.interceptorId = n;
            this.expiresAt = l2;
        }
    }

    private static final class Threat {
        static final Threat NONE = new Threat(false, false, 0.0, Double.MAX_VALUE);
        final boolean threatening;
        final boolean immediate;
        final double timeToClosest;
        final double closestDistance;

        Threat(boolean bl, boolean bl2, double d, double d2) {
            this.threatening = bl;
            this.immediate = bl2;
            this.timeToClosest = d;
            this.closestDistance = d2;
        }
    }

    private static final class RadarStation {
        final int entityId;
        double x;
        double y;
        double z;
        double range;
        double ceiling;
        String team = "";
        int frequencyBand;
        double jamming;
        long lastUpdate;

        RadarStation(int n) {
            this.entityId = n;
        }
    }

    public static final class FactionSnapshot {
        public static final FactionSnapshot EMPTY = new FactionSnapshot("", 0, 0.0, 0.0, 0L, new FactionSector[0], new FactionNode[0], new FactionContact[0]);
        public final String team;
        public final int dimension;
        public final double centerX;
        public final double centerZ;
        public final long generatedAt;
        public final FactionSector[] sectors;
        public final FactionNode[] nodes;
        public final FactionContact[] contacts;

        public FactionSnapshot(String string, int n, double d, double d2, long l, FactionSector[] factionSectorArray, FactionNode[] factionNodeArray, FactionContact[] factionContactArray) {
            this.team = string == null ? "" : string;
            this.dimension = n;
            this.centerX = d;
            this.centerZ = d2;
            this.generatedAt = l;
            this.sectors = factionSectorArray == null ? new FactionSector[]{} : factionSectorArray;
            this.nodes = factionNodeArray == null ? new FactionNode[]{} : factionNodeArray;
            this.contacts = factionContactArray == null ? new FactionContact[]{} : factionContactArray;
        }
    }

    public static final class FactionSector {
        public final int x;
        public final int z;

        public FactionSector(int n, int n2) {
            this.x = n;
            this.z = n2;
        }
    }

    public static final class FactionNode {
        public final int type;
        public final long id;
        public final double x;
        public final double y;
        public final double z;
        public final int value;
        public final int band;

        public FactionNode(int n, long l, double d, double d2, double d3, int n2, int n3) {
            this.type = n;
            this.id = l;
            this.x = d;
            this.y = d2;
            this.z = d3;
            this.value = n2;
            this.band = n3;
        }
    }

    private static final class LauncherStation {
        final long ownerKey;
        double x;
        double y;
        double z;
        int tier;
        String team = "";
        long lastUpdate;

        LauncherStation(long l) {
            this.ownerKey = l;
        }
    }

    private static final class CommunicationRelay {
        final long key;
        double x;
        double y;
        double z;
        String team = "";
        long lastUpdate;

        CommunicationRelay(long l) {
            this.key = l;
        }
    }

    public static final class FactionContact {
        public final int entityId;
        public final int type;
        public final int tier;
        public final double x;
        public final double y;
        public final double z;
        public final double velocityX;
        public final double velocityZ;
        public final float quality;
        public final int sourceCount;
        public final boolean assigned;
        public final boolean friendly;

        public FactionContact(int n, int n2, int n3, double d, double d2, double d3, double d4, double d5, float f, int n4, boolean bl) {
            this(n, n2, n3, d, d2, d3, d4, d5, f, n4, bl, false);
        }

        public FactionContact(int n, int n2, int n3, double d, double d2, double d3, double d4, double d5, float f, int n4, boolean bl, boolean bl2) {
            this.entityId = n;
            this.type = n2;
            this.tier = n3;
            this.x = d;
            this.y = d2;
            this.z = d3;
            this.velocityX = d4;
            this.velocityZ = d5;
            this.quality = f;
            this.sourceCount = n4;
            this.assigned = bl;
            this.friendly = bl2;
        }
    }

    public static final class CommandSnapshot {
        static final CommandSnapshot EMPTY = new CommandSnapshot(0, 0, 0, 0, 0, 0);
        public final int linkedRadars;
        public final int linkedLaunchers;
        public final int contacts;
        public final int assignedTargets;
        public final int hostileEmitters;
        public final int activeJammers;

        CommandSnapshot(int n, int n2, int n3, int n4, int n5, int n6) {
            this.linkedRadars = n;
            this.linkedLaunchers = n2;
            this.contacts = n3;
            this.assignedTargets = n4;
            this.hostileEmitters = n5;
            this.activeJammers = n6;
        }
    }

    private static final class RocketTypeAccess {
        final Method getType;
        final Field modelType;

        RocketTypeAccess(Method method, Field field) {
            this.getType = method;
            this.modelType = field;
        }
    }

    private static final class ThreatState {
        int confirmations;
        long lastSeen;

        private ThreatState() {
        }
    }

    private static final class CoordinateFields {
        Field startX;
        Field startZ;
        Field targetX;
        Field targetZ;
        Field targetVector;

        private CoordinateFields() {
        }
    }
}
