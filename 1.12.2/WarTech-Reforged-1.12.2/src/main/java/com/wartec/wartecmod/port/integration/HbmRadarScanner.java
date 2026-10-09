package com.wartec.wartecmod.port.integration;

import api.hbm.entity.IRadarDetectable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Server-safe scanner for NTM Extended's compact IRadarDetectable contract.
 */
public final class HbmRadarScanner {
    public interface TargetFilter {
        boolean accept(Entity entity, IRadarDetectable.RadarTargetType targetType);
    }

    public static final TargetFilter ACCEPT_ALL = new TargetFilter() {
        @Override
        public boolean accept(Entity entity, IRadarDetectable.RadarTargetType targetType) {
            return true;
        }
    };

    private HbmRadarScanner() {
    }

    public static List<RadarContact> scan(
        World world,
        BlockPos origin,
        double horizontalRange,
        double verticalRange,
        int limit,
        String observerTeam,
        boolean excludeFriendlies,
        TargetFilter filter
    ) {
        return scan(world,origin,horizontalRange,verticalRange,limit,observerTeam,excludeFriendlies,filter,2.5);
    }
    public static List<RadarContact> scan(World world,BlockPos origin,double horizontalRange,double verticalRange,
            int limit,String observerTeam,boolean excludeFriendlies,TargetFilter filter,double sensorOffset) {
        if (world == null || origin == null || horizontalRange <= 0.0D || verticalRange < 0.0D || limit <= 0) {
            return Collections.emptyList();
        }

        double centerX = origin.getX() + 0.5D;
        double centerY = origin.getY() + 0.5D;
        double centerZ = origin.getZ() + 0.5D;
        AxisAlignedBB searchBox = new AxisAlignedBB(
            centerX - horizontalRange,
            centerY - verticalRange,
            centerZ - horizontalRange,
            centerX + horizontalRange,
            centerY + verticalRange,
            centerZ + horizontalRange
        );
        double horizontalRangeSquared = horizontalRange * horizontalRange;
        TargetFilter effectiveFilter = filter == null ? ACCEPT_ALL : filter;
        List<RadarContact> contacts = new ArrayList<RadarContact>();

        for (Entity entity : world.loadedEntityList) {
            if (entity == null || entity.isDead || !(entity instanceof IRadarDetectable)) {
                continue;
            }
            if(!entity.getEntityBoundingBox().intersects(searchBox)) continue;
            if(com.wartec.wartecmod.port.network.MissileTrackingService.getThreatTier(entity)==0) continue;

            double deltaX = entity.posX - centerX;
            double deltaZ = entity.posZ - centerZ;
            if (deltaX * deltaX + deltaZ * deltaZ > horizontalRangeSquared) {
                continue;
            }

            IRadarDetectable.RadarTargetType targetType =
                ((IRadarDetectable) entity).getTargetType();
            if (targetType == null || targetType==IRadarDetectable.RadarTargetType.PLAYER
                    || targetType==IRadarDetectable.RadarTargetType.MISSILE_AB || !effectiveFilter.accept(entity, targetType)) {
                continue;
            }

            String targetTeam = OwnerTeamNbt.getEntityTeam(entity);
            if (excludeFriendlies && OwnerTeamNbt.areFriendly(observerTeam, targetTeam)) {
                continue;
            }

            double distanceSquared = entity.getDistanceSq(centerX, centerY, centerZ);
            contacts.add(new RadarContact(entity, targetType, targetTeam, distanceSquared));
        }

        Collections.sort(contacts, RadarContact.ORDER);
        List<RadarContact> visible=new ArrayList<>();
        int checked=0;
        for(RadarContact contact:contacts) {
            if(checked++>=AirDefenseVisibility.MAX_CONTACT_CHECKS || visible.size()>=limit) break;
            if(AirDefenseVisibility.visible(world,new Vec3d(centerX,centerY+sensorOffset,centerZ),contact.entity)) visible.add(contact);
        }
        return Collections.unmodifiableList(visible);
    }

    public static final class RadarContact {
        private static final Comparator<RadarContact> ORDER = new Comparator<RadarContact>() {
            @Override
            public int compare(RadarContact first, RadarContact second) {
                int distanceOrder = Double.compare(first.distanceSquared, second.distanceSquared);
                if (distanceOrder != 0) {
                    return distanceOrder;
                }
                return Integer.compare(first.entity.getEntityId(), second.entity.getEntityId());
            }
        };

        private final Entity entity;
        private final IRadarDetectable.RadarTargetType targetType;
        private final String ownerTeam;
        private final double distanceSquared;

        private RadarContact(
            Entity entity,
            IRadarDetectable.RadarTargetType targetType,
            String ownerTeam,
            double distanceSquared
        ) {
            this.entity = entity;
            this.targetType = targetType;
            this.ownerTeam = ownerTeam;
            this.distanceSquared = distanceSquared;
        }

        public Entity getEntity() {
            return entity;
        }

        public IRadarDetectable.RadarTargetType getTargetType() {
            return targetType;
        }

        public String getOwnerTeam() {
            return ownerTeam;
        }

        public double getDistanceSquared() {
            return distanceSquared;
        }
    }
}
