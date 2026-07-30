package com.wartec.wartecmod.compat.client;

import com.wartec.wartecmod.compat.ContainerCommandVehicle;
import com.wartec.wartecmod.compat.FactionCommandNetwork;
import com.wartec.wartecmod.compat.FactionTerritoryData;
import com.wartec.wartecmod.compat.MissileTrackingService;
import com.wartec.wartecmod.compat.MissileTrackingService.FactionContact;
import com.wartec.wartecmod.compat.MissileTrackingService.FactionNode;
import com.wartec.wartecmod.compat.MissileTrackingService.FactionSector;
import com.wartec.wartecmod.compat.MissileTrackingService.FactionSnapshot;
import com.wartec.wartecmod.entity.vehicle.EntityCommandTruck;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;

/** Faction-wide air-defense map hosted by a deployed command vehicle. */
public final class GuiCommandNetwork extends GuiContainer {
    private static final int MAP_X = 10;
    private static final int MAP_Y = 30;
    private static final int MAP_W = 390;
    private static final int MAP_H = 220;
    private static final int[] VIEW_RADII =
            {512, 1024, 2048, 4096, 8192, 16384};
    private final EntityCommandTruck command;
    private int refreshTicks;
    private int zoomIndex = 3;
    private double viewX;
    private double viewZ;
    private int selectedContactId;

    public GuiCommandNetwork(InventoryPlayer inventory,
            EntityCommandTruck command) {
        super(new ContainerCommandVehicle(inventory, command));
        this.command = command;
        field_146999_f = 540;
        field_147000_g = 348;
        viewX = command.field_70165_t;
        viewZ = command.field_70161_v;
    }

    @Override
    public void func_73866_w_() {
        super.func_73866_w_();
        field_146292_n.clear();
        int left = field_147003_i;
        int top = field_147009_r;
        field_146292_n.add(new GuiButton(1,
                left + 410, top + 222, 32, 20, "+"));
        field_146292_n.add(new GuiButton(2,
                left + 446, top + 222, 32, 20, "-"));
        field_146292_n.add(new GuiButton(3,
                left + 482, top + 222, 48, 20, "CTR"));
        requestSnapshot();
    }

    @Override
    public void func_73876_c() {
        if (++refreshTicks >= 20) {
            refreshTicks = 0;
            requestSnapshot();
        }
    }

    @Override
    protected void func_146284_a(GuiButton button) {
        if (button.field_146127_k == 1 && zoomIndex > 0) {
            --zoomIndex;
        } else if (button.field_146127_k == 2
                && zoomIndex < VIEW_RADII.length - 1) {
            ++zoomIndex;
        } else if (button.field_146127_k == 3) {
            viewX = command.field_70165_t;
            viewZ = command.field_70161_v;
        }
    }

    @Override
    protected void func_73864_a(int mouseX, int mouseY, int button) {
        if (button == 0 && insideMap(mouseX, mouseY)) {
            selectNearestContact(mouseX, mouseY);
        }
        super.func_73864_a(mouseX, mouseY, button);
    }

    @Override
    protected void func_146976_a(float partialTicks,
            int mouseX, int mouseY) {
        int left = field_147003_i;
        int top = field_147009_r;
        Gui.func_73734_a(left, top, left + field_146999_f,
                top + field_147000_g, 0xFF737873);
        Gui.func_73734_a(left + 4, top + 4,
                left + 536, top + 258, 0xFF101715);
        Gui.func_73734_a(left + MAP_X, top + MAP_Y,
                left + MAP_X + MAP_W, top + MAP_Y + MAP_H,
                0xFF07100E);
        Gui.func_73734_a(left + 406, top + MAP_Y,
                left + 530, top + MAP_Y + MAP_H, 0xFF17201D);
        Gui.func_73734_a(left + 4, top + 258,
                left + 536, top + 346, 0xFF858A84);
        drawMap(left + MAP_X, top + MAP_Y);
        drawInventorySlots(left, top);
    }

    @Override
    protected void func_146979_b(int mouseX, int mouseY) {
        FactionSnapshot snapshot = FactionCommandClient.getSnapshot();
        int green = 0x63F09A;
        int red = 0xFF6868;
        int white = 0xD8E7DF;
        field_146289_q.func_78276_b("WARTECH // FACTION AIR DEFENSE",
                12, 11, white);

        int radars = countNodes(snapshot,
                MissileTrackingService.FACTION_NODE_RADAR)
                + countNodes(snapshot,
                MissileTrackingService.FACTION_NODE_STRATEGIC_RADAR);
        int launchers = countNodes(snapshot,
                MissileTrackingService.FACTION_NODE_LAUNCHER);
        int relays = countNodes(snapshot,
                MissileTrackingService.FACTION_NODE_RELAY);
        String networkStatus;
        int networkColor;
        if (!command.isNetworkActive()) {
            networkStatus = "NETWORK OFFLINE";
            networkColor = red;
        } else if (radars == 0) {
            networkStatus = "NO RADAR LINK";
            networkColor = red;
        } else if (launchers == 0) {
            networkStatus = "RADAR ONLY - NO PVO";
            networkColor = 0xFFB45D;
        } else {
            networkStatus = "NETWORK ONLINE";
            networkColor = green;
        }
        int statusWidth = field_146289_q.func_78256_a(networkStatus);
        field_146289_q.func_78276_b(networkStatus,
                526 - statusWidth, 11, networkColor);
        int x = 414;
        drawPair(x, 38, "IFF", snapshot.team.length() == 0
                ? command.getOwnerTeam() : snapshot.team, white);
        drawPair(x, 51, "DIM", Integer.toString(snapshot.dimension), white);
        drawPair(x, 64, "SECTORS",
                Integer.toString(snapshot.sectors.length), 0x66DDB0);
        drawPair(x, 77, "RADARS", Integer.toString(radars),
                radars > 0 ? 0x5FE6EF : red);
        drawPair(x, 90, "PVO NODES",
                Integer.toString(launchers),
                launchers > 0 ? 0x8BE36C : red);
        drawPair(x, 103, "RELAYS", Integer.toString(relays), 0xE4D568);
        drawPair(x, 116, "TRACKS",
                Integer.toString(snapshot.contacts.length),
                snapshot.contacts.length > 0 ? 0xFF756B : white);
        int hostiles = countHostileContacts(snapshot);
        drawPair(x, 129, "HOSTILE", Integer.toString(hostiles),
                hostiles > 0 ? red : green);

        FactionContact selected = selected(snapshot);
        field_146289_q.func_78276_b("SELECTED TRACK", x, 151, 0xA9B8B0);
        if (selected == null) {
            field_146289_q.func_78276_b("NONE", x, 164, 0x6F7E77);
        } else {
            field_146289_q.func_78276_b(
                    (selected.friendly ? "FRIEND " : "HOSTILE ")
                    + contactCode(selected.type) + " #" + selected.entityId,
                    x, 164, selected.friendly ? green
                    : selected.assigned ? 0xFFD36A : 0xFF756B);
            field_146289_q.func_78276_b("POS "
                    + (int) selected.x + " / " + (int) selected.z,
                    x, 177, white);
            field_146289_q.func_78276_b("ALT " + (int) selected.y
                    + "  SPD " + speed(selected), x, 190, white);
            field_146289_q.func_78276_b("HDG " + heading(selected)
                    + "  SRC " + selected.sourceCount, x, 203, white);
            field_146289_q.func_78276_b("TRACK "
                    + Math.round(selected.quality * 100.0F) + "%",
                    x, 216, selected.quality >= 0.55F
                    ? green : 0xFFB45D);
        }
        field_146289_q.func_78276_b("INVENTORY", 12, 260, white);
        field_146289_q.func_78276_b("BATTERY", 486, 260, white);
        drawMapReadout(snapshot);
        drawLegend();
    }

    private void drawMap(int x, int y) {
        FactionSnapshot snapshot = FactionCommandClient.getSnapshot();
        int radius = VIEW_RADII[zoomIndex];
        drawGrid(x, y, radius);
        for (FactionSector sector : snapshot.sectors) {
            int x0 = worldToMapX(
                    sector.x * FactionTerritoryData.SECTOR_SIZE);
            int x1 = worldToMapX(
                    (sector.x + 1) * FactionTerritoryData.SECTOR_SIZE);
            int y0 = worldToMapY(
                    (sector.z + 1) * FactionTerritoryData.SECTOR_SIZE);
            int y1 = worldToMapY(
                    sector.z * FactionTerritoryData.SECTOR_SIZE);
            int left = Math.max(x, Math.min(x0, x1));
            int right = Math.min(x + MAP_W, Math.max(x0, x1));
            int top = Math.max(y, Math.min(y0, y1));
            int bottom = Math.min(y + MAP_H, Math.max(y0, y1));
            if (left < right && top < bottom) {
                Gui.func_73734_a(left, top, right, bottom, 0x28357A5B);
                Gui.func_73734_a(left, top, right, top + 1, 0x885BD39A);
                Gui.func_73734_a(left, bottom - 1, right, bottom, 0x885BD39A);
                Gui.func_73734_a(left, top, left + 1, bottom, 0x885BD39A);
                Gui.func_73734_a(right - 1, top, right, bottom, 0x885BD39A);
            }
        }
        for (FactionNode node : snapshot.nodes) {
            int px = worldToMapX(node.x);
            int py = worldToMapY(node.z);
            if (!insideAbsoluteMap(px, py)) continue;
            int color = nodeColor(node.type);
            int size = node.type == MissileTrackingService
                    .FACTION_NODE_STRATEGIC_RADAR ? 4 : 3;
            Gui.func_73734_a(px - size, py - size,
                    px + size + 1, py + size + 1, 0xFF101715);
            Gui.func_73734_a(px - size + 1, py - size + 1,
                    px + size, py + size, color);
            field_146289_q.func_78276_b(nodeSymbol(node.type),
                    px - 2, py - 4, 0xFF07100E);
        }
        for (FactionContact contact : snapshot.contacts) {
            int px = worldToMapX(contact.x);
            int py = worldToMapY(contact.z);
            if (!insideAbsoluteMap(px, py)) continue;
            int color = contact.friendly ? 0xFF63F09A
                    : contact.assigned ? 0xFFFFD25E
                    : contactColor(contact.type);
            Gui.func_73734_a(px - 1, py - 4, px + 2, py + 5, color);
            Gui.func_73734_a(px - 4, py - 1, px + 5, py + 2, color);
            if (snapshot.contacts.length <= 32
                    || contact.entityId == selectedContactId) {
                field_146289_q.func_78276_b(contactCode(contact.type),
                        px + 6, py - 4, color);
            }
            double length = Math.sqrt(contact.velocityX * contact.velocityX
                    + contact.velocityZ * contact.velocityZ);
            if (length > 0.001D) {
                int vx = (int) Math.round(contact.velocityX / length * 10.0D);
                int vz = (int) Math.round(contact.velocityZ / length * 10.0D);
                drawCourse(px, py, vx, -vz, color);
            }
            if (contact.entityId == selectedContactId) {
                Gui.func_73734_a(px - 6, py - 6, px + 7, py - 5, 0xFFFFFFFF);
                Gui.func_73734_a(px - 6, py + 6, px + 7, py + 7, 0xFFFFFFFF);
                Gui.func_73734_a(px - 6, py - 6, px - 5, py + 7, 0xFFFFFFFF);
                Gui.func_73734_a(px + 6, py - 6, px + 7, py + 7, 0xFFFFFFFF);
            }
        }
        int centerX = worldToMapX(command.field_70165_t);
        int centerY = worldToMapY(command.field_70161_v);
        if (insideAbsoluteMap(centerX, centerY)) {
            Gui.func_73734_a(centerX - 4, centerY - 1,
                    centerX + 5, centerY + 2, 0xFFFFFFFF);
            Gui.func_73734_a(centerX - 1, centerY - 4,
                    centerX + 2, centerY + 5, 0xFFFFFFFF);
        }
    }

    private void drawGrid(int x, int y, int radius) {
        int startX = (int) Math.floor((viewX - radius)
                / FactionTerritoryData.SECTOR_SIZE)
                * FactionTerritoryData.SECTOR_SIZE;
        int endX = (int) Math.ceil((viewX + radius)
                / FactionTerritoryData.SECTOR_SIZE)
                * FactionTerritoryData.SECTOR_SIZE;
        for (int worldX = startX; worldX <= endX;
                worldX += FactionTerritoryData.SECTOR_SIZE) {
            int px = worldToMapX(worldX);
            if (px >= x && px <= x + MAP_W) {
                Gui.func_73734_a(px, y, px + 1, y + MAP_H, 0x44243832);
            }
        }
        int startZ = (int) Math.floor((viewZ - radius)
                / FactionTerritoryData.SECTOR_SIZE)
                * FactionTerritoryData.SECTOR_SIZE;
        int endZ = (int) Math.ceil((viewZ + radius)
                / FactionTerritoryData.SECTOR_SIZE)
                * FactionTerritoryData.SECTOR_SIZE;
        for (int worldZ = startZ; worldZ <= endZ;
                worldZ += FactionTerritoryData.SECTOR_SIZE) {
            int py = worldToMapY(worldZ);
            if (py >= y && py <= y + MAP_H) {
                Gui.func_73734_a(x, py, x + MAP_W, py + 1, 0x44243832);
            }
        }
    }

    private void drawCourse(int x, int y, int dx, int dy, int color) {
        int steps = Math.max(Math.abs(dx), Math.abs(dy));
        if (steps <= 0) return;
        for (int step = 1; step <= steps; ++step) {
            int px = x + dx * step / steps;
            int py = y + dy * step / steps;
            if (insideAbsoluteMap(px, py)) {
                Gui.func_73734_a(px, py, px + 1, py + 1, color);
            }
        }
    }

    private void drawLegend() {
        field_146289_q.func_78276_b("C HQ", 14, 240, 0xFFFFFF);
        field_146289_q.func_78276_b("R RADAR", 44, 240, 0x5FE6EF);
        field_146289_q.func_78276_b("S STRAT", 92, 240, 0x70A8FF);
        field_146289_q.func_78276_b("L PVO", 140, 240, 0x8BE36C);
        field_146289_q.func_78276_b("T LINK", 176, 240, 0xE4D568);
        field_146289_q.func_78276_b("RK HENRY", 218, 240, 0xFF55D8);
        field_146289_q.func_78276_b("F FRIEND", 278, 240, 0x63F09A);
    }

    private void drawMapReadout(FactionSnapshot snapshot) {
        int white = 0xA9B8B0;
        String center = "CTR " + (int) viewX + " / " + (int) viewZ;
        field_146289_q.func_78276_b(center,
                MAP_X + 4, MAP_Y + 4, white);
        field_146289_q.func_78276_b("N",
                MAP_X + MAP_W / 2 - 2, MAP_Y + 4, 0xD8E7DF);
        String range = "+/- " + VIEW_RADII[zoomIndex];
        int width = field_146289_q.func_78256_a(range);
        field_146289_q.func_78276_b(range,
                MAP_X + MAP_W - width - 4, MAP_Y + 4, white);
    }

    private void drawInventorySlots(int left, int top) {
        drawSlot(left + 510, top + 270);
        for (int row = 0; row < 3; ++row) {
            for (int column = 0; column < 9; ++column) {
                drawSlot(left + 12 + column * 18,
                        top + 270 + row * 18);
            }
        }
        for (int column = 0; column < 9; ++column) {
            drawSlot(left + 12 + column * 18, top + 328);
        }
    }

    private void drawSlot(int x, int y) {
        Gui.func_73734_a(x, y, x + 18, y + 18, 0xFF343834);
        Gui.func_73734_a(x + 1, y + 1,
                x + 17, y + 17, 0xFF9DA29A);
    }

    private void drawPair(int x, int y, String label,
            String value, int color) {
        field_146289_q.func_78276_b(label, x, y, 0x9EACA5);
        int width = field_146289_q.func_78256_a(value);
        field_146289_q.func_78276_b(value,
                526 - width, y, color);
    }

    private void selectNearestContact(int mouseX, int mouseY) {
        FactionSnapshot snapshot = FactionCommandClient.getSnapshot();
        int bestId = 0;
        int bestDistance = 81;
        for (FactionContact contact : snapshot.contacts) {
            int dx = worldToMapX(contact.x) - mouseX;
            int dy = worldToMapY(contact.z) - mouseY;
            int distance = dx * dx + dy * dy;
            if (distance < bestDistance) {
                bestDistance = distance;
                bestId = contact.entityId;
            }
        }
        selectedContactId = bestId;
    }

    private FactionContact selected(FactionSnapshot snapshot) {
        for (FactionContact contact : snapshot.contacts) {
            if (contact.entityId == selectedContactId) return contact;
        }
        return null;
    }

    private int worldToMapX(double worldX) {
        double normalized = (worldX - viewX) / VIEW_RADII[zoomIndex];
        return field_147003_i + MAP_X + MAP_W / 2
                + (int) Math.round(normalized * MAP_W / 2.0D);
    }

    private int worldToMapY(double worldZ) {
        double normalized = (worldZ - viewZ) / VIEW_RADII[zoomIndex];
        return field_147009_r + MAP_Y + MAP_H / 2
                - (int) Math.round(normalized * MAP_H / 2.0D);
    }

    private boolean insideMap(int mouseX, int mouseY) {
        return insideAbsoluteMap(mouseX, mouseY);
    }

    private boolean insideAbsoluteMap(int x, int y) {
        int left = field_147003_i + MAP_X;
        int top = field_147009_r + MAP_Y;
        return x >= left && x <= left + MAP_W
                && y >= top && y <= top + MAP_H;
    }

    private void requestSnapshot() {
        if (command != null && !command.field_70128_L) {
            FactionCommandNetwork.requestSnapshot(command.func_145782_y());
        }
    }

    private static int countNodes(FactionSnapshot snapshot, int type) {
        int count = 0;
        for (FactionNode node : snapshot.nodes) {
            if (node.type == type) ++count;
        }
        return count;
    }

    private static int countHostileContacts(FactionSnapshot snapshot) {
        int count = 0;
        for (FactionContact contact : snapshot.contacts) {
            if (!contact.friendly) ++count;
        }
        return count;
    }

    private static int nodeColor(int type) {
        if (type == MissileTrackingService.FACTION_NODE_LAUNCHER) {
            return 0xFF8BE36C;
        }
        if (type == MissileTrackingService.FACTION_NODE_COMMAND) {
            return 0xFFFFFFFF;
        }
        if (type == MissileTrackingService.FACTION_NODE_RELAY) {
            return 0xFFE4D568;
        }
        if (type == MissileTrackingService.FACTION_NODE_STRATEGIC_RADAR) {
            return 0xFF70A8FF;
        }
        return 0xFF5FE6EF;
    }

    private static String nodeSymbol(int type) {
        if (type == MissileTrackingService.FACTION_NODE_LAUNCHER) return "L";
        if (type == MissileTrackingService.FACTION_NODE_COMMAND) return "C";
        if (type == MissileTrackingService.FACTION_NODE_RELAY) return "T";
        if (type == MissileTrackingService.FACTION_NODE_STRATEGIC_RADAR) {
            return "S";
        }
        return "R";
    }

    private static int contactColor(int type) {
        if (type == MissileTrackingService.FACTION_CONTACT_ARTILLERY_ROCKET) {
            return 0xFFFF55D8;
        }
        if (type == MissileTrackingService.FACTION_CONTACT_BALLISTIC) {
            return 0xFFFF4A4A;
        }
        if (type == MissileTrackingService.FACTION_CONTACT_AIRCRAFT
                || type == MissileTrackingService
                .FACTION_CONTACT_HEAVY_AIRCRAFT) {
            return 0xFFFF8A66;
        }
        if (type == MissileTrackingService.FACTION_CONTACT_DRONE) {
            return 0xFFFFB45D;
        }
        return 0xFFFF6868;
    }

    private static String contactCode(int type) {
        if (type == MissileTrackingService.FACTION_CONTACT_ARTILLERY_ROCKET) {
            return "RK";
        }
        if (type == MissileTrackingService.FACTION_CONTACT_BALLISTIC) {
            return "BM";
        }
        if (type == MissileTrackingService.FACTION_CONTACT_AIRCRAFT) {
            return "AC";
        }
        if (type == MissileTrackingService.FACTION_CONTACT_HEAVY_AIRCRAFT) {
            return "HV";
        }
        if (type == MissileTrackingService.FACTION_CONTACT_DRONE) {
            return "DR";
        }
        if (type == MissileTrackingService.FACTION_CONTACT_MISSILE) {
            return "MS";
        }
        return "?";
    }

    private static int speed(FactionContact contact) {
        return (int) Math.round(Math.sqrt(
                contact.velocityX * contact.velocityX
                + contact.velocityZ * contact.velocityZ) * 20.0D);
    }

    private static int heading(FactionContact contact) {
        double heading = Math.toDegrees(Math.atan2(
                -contact.velocityX, contact.velocityZ));
        if (heading < 0.0D) heading += 360.0D;
        return (int) Math.round(heading) % 360;
    }
}
