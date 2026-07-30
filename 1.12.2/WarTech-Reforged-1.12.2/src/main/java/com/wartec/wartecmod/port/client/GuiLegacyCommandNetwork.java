package com.wartec.wartecmod.port.client;

import com.wartec.wartecmod.port.entity.EntityWarTechBase;
import com.wartec.wartecmod.port.gui.ContainerLegacyEntity;
import com.wartec.wartecmod.port.network.FactionCommandRequestMessage;
import com.wartec.wartecmod.port.network.FactionCommandSnapshot;
import com.wartec.wartecmod.port.network.WarTechNetwork;
import java.io.IOException;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;

public final class GuiLegacyCommandNetwork extends GuiContainer {
    private static final int MAP_X = 10;
    private static final int MAP_Y = 30;
    private static final int MAP_W = 390;
    private static final int MAP_H = 220;
    private static final int[] VIEW_RADII =
            {512, 1024, 2048, 4096, 8192, 16384};
    private final EntityWarTechBase command;
    private int refreshTicks;
    private int zoomIndex = 3;
    private double viewX;
    private double viewZ;
    private int selectedContactId;

    public GuiLegacyCommandNetwork(InventoryPlayer inventory,
            EntityWarTechBase command) {
        super(new ContainerLegacyEntity(inventory, command,
                ContainerLegacyEntity.Layout.COMMAND));
        this.command = command;
        xSize = 540;
        ySize = 348;
        viewX = command.posX;
        viewZ = command.posZ;
    }

    @Override
    public void initGui() {
        super.initGui();
        buttonList.clear();
        buttonList.add(new GuiButton(1, guiLeft + 410, guiTop + 222, 32, 20, "+"));
        buttonList.add(new GuiButton(2, guiLeft + 446, guiTop + 222, 32, 20, "-"));
        buttonList.add(new GuiButton(3, guiLeft + 482, guiTop + 222, 48, 20, "CTR"));
        requestSnapshot();
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (++refreshTicks >= 20) {
            refreshTicks = 0;
            requestSnapshot();
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 1 && zoomIndex > 0) {
            --zoomIndex;
        } else if (button.id == 2 && zoomIndex < VIEW_RADII.length - 1) {
            ++zoomIndex;
        } else if (button.id == 3) {
            viewX = command.posX;
            viewZ = command.posZ;
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton)
            throws IOException {
        if (mouseButton == 0 && insideAbsoluteMap(mouseX, mouseY)) {
            selectNearestContact(mouseX, mouseY);
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks,
            int mouseX, int mouseY) {
        Gui.drawRect(guiLeft, guiTop, guiLeft + xSize, guiTop + ySize, 0xFF737873);
        Gui.drawRect(guiLeft + 4, guiTop + 4, guiLeft + 536, guiTop + 258,
                0xFF101715);
        Gui.drawRect(guiLeft + MAP_X, guiTop + MAP_Y,
                guiLeft + MAP_X + MAP_W, guiTop + MAP_Y + MAP_H, 0xFF07100E);
        Gui.drawRect(guiLeft + 406, guiTop + MAP_Y,
                guiLeft + 530, guiTop + MAP_Y + MAP_H, 0xFF17201D);
        Gui.drawRect(guiLeft + 4, guiTop + 258,
                guiLeft + 536, guiTop + 346, 0xFF858A84);
        drawMap();
        drawInventorySlots();
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        FactionCommandSnapshot snapshot = FactionCommandClient.getSnapshot();
        int green = 0x63F09A;
        int red = 0xFF6868;
        int white = 0xD8E7DF;
        fontRenderer.drawString("WARTECH // FACTION AIR DEFENSE", 12, 11, white);
        int radarCount = countNodes(snapshot, 1) + countNodes(snapshot, 2);
        int pvoCount = countNodes(snapshot, 3);
        int relayCount = countNodes(snapshot, 5);
        String status;
        int statusColor;
        if (!command.isLegacyOperational()) {
            status = "NETWORK OFFLINE";
            statusColor = red;
        } else if (radarCount == 0) {
            status = "NO RADAR LINK";
            statusColor = red;
        } else if (pvoCount == 0) {
            status = "RADAR ONLY - NO PVO";
            statusColor = 0xFFB35D;
        } else {
            status = "NETWORK ONLINE";
            statusColor = green;
        }
        fontRenderer.drawString(status, 526 - fontRenderer.getStringWidth(status),
                11, statusColor);
        int x = 414;
        drawPair(x, 38, "IFF", snapshot.team.isEmpty()
                ? command.getOwnerTeam() : snapshot.team, white);
        drawPair(x, 51, "DIM", Integer.toString(snapshot.dimension), white);
        drawPair(x, 64, "SECTORS", Integer.toString(snapshot.sectors.length),
                0x66DDB0);
        drawPair(x, 77, "RADARS", Integer.toString(radarCount),
                radarCount > 0 ? 0x5FE6EF : red);
        drawPair(x, 90, "PVO NODES", Integer.toString(pvoCount),
                pvoCount > 0 ? 0x8BE36C : red);
        drawPair(x, 103, "RELAYS", Integer.toString(relayCount), 0xE4D568);
        drawPair(x, 116, "TRACKS", Integer.toString(snapshot.contacts.length),
                snapshot.contacts.length > 0 ? 0xFF756B : white);
        int hostileCount = countHostileContacts(snapshot);
        drawPair(x, 129, "HOSTILE", Integer.toString(hostileCount),
                hostileCount > 0 ? red : green);
        FactionCommandSnapshot.Contact selected = selected(snapshot);
        fontRenderer.drawString("SELECTED TRACK", x, 151, 0xA9B8B0);
        if (selected == null) {
            fontRenderer.drawString("NONE", x, 164, 0x6F7E77);
        } else {
            fontRenderer.drawString((selected.friendly ? "FRIEND " : "HOSTILE ")
                    + contactCode(selected.type) + " #" + selected.entityId,
                    x, 164, selected.friendly ? green
                            : selected.assigned ? 0xFFB72A : 0xFF756B);
            fontRenderer.drawString("POS " + (int) selected.x + " / "
                    + (int) selected.z, x, 177, white);
            fontRenderer.drawString("ALT " + (int) selected.y + "  SPD "
                    + speed(selected), x, 190, white);
            fontRenderer.drawString("HDG " + heading(selected) + "  SRC "
                    + selected.sourceCount, x, 203, white);
            fontRenderer.drawString("TRACK "
                    + Math.round(selected.quality * 100.0F) + "%",
                    x, 216, selected.quality >= 0.55F ? green : 0xFFB35D);
        }
        fontRenderer.drawString("INVENTORY", 12, 260, white);
        fontRenderer.drawString("BATTERY", 486, 260, white);
        drawMapReadout();
        drawLegend();
    }

    private void drawMap() {
        FactionCommandSnapshot snapshot = FactionCommandClient.getSnapshot();
        drawGrid();
        int mapLeft = guiLeft + MAP_X;
        int mapTop = guiTop + MAP_Y;
        for (FactionCommandSnapshot.Sector sector : snapshot.sectors) {
            int x1 = worldToMapX(sector.x * 512.0D);
            int x2 = worldToMapX((sector.x + 1) * 512.0D);
            int y1 = worldToMapY((sector.z + 1) * 512.0D);
            int y2 = worldToMapY(sector.z * 512.0D);
            int left = Math.max(mapLeft, Math.min(x1, x2));
            int right = Math.min(mapLeft + MAP_W, Math.max(x1, x2));
            int top = Math.max(mapTop, Math.min(y1, y2));
            int bottom = Math.min(mapTop + MAP_H, Math.max(y1, y2));
            if (left >= right || top >= bottom) continue;
            Gui.drawRect(left, top, right, bottom, 0x283B655B);
            Gui.drawRect(left, top, right, top + 1, 0x8866DDBA);
            Gui.drawRect(left, bottom - 1, right, bottom, 0x8866DDBA);
            Gui.drawRect(left, top, left + 1, bottom, 0x8866DDBA);
            Gui.drawRect(right - 1, top, right, bottom, 0x8866DDBA);
        }
        for (FactionCommandSnapshot.Node node : snapshot.nodes) {
            int x = worldToMapX(node.x);
            int y = worldToMapY(node.z);
            if (!insideAbsoluteMap(x, y)) continue;
            int half = node.type == 2 ? 4 : 3;
            Gui.drawRect(x - half, y - half, x + half + 1, y + half + 1,
                    0xFF101715);
            Gui.drawRect(x - half + 1, y - half + 1, x + half, y + half,
                    nodeColor(node.type));
            fontRenderer.drawString(nodeSymbol(node.type), x - 2, y - 4,
                    0xFF07100E);
        }
        for (FactionCommandSnapshot.Contact contact : snapshot.contacts) {
            int x = worldToMapX(contact.x);
            int y = worldToMapY(contact.z);
            if (!insideAbsoluteMap(x, y)) continue;
            int color = contact.friendly ? 0xFF63F09A
                    : contact.assigned ? 0xFFFFD25A : contactColor(contact.type);
            Gui.drawRect(x - 1, y - 4, x + 2, y + 5, color);
            Gui.drawRect(x - 4, y - 1, x + 5, y + 2, color);
            if (snapshot.contacts.length <= 32
                    || contact.entityId == selectedContactId) {
                fontRenderer.drawString(contactCode(contact.type), x + 6, y - 4,
                        color);
            }
            double velocity = Math.sqrt(contact.velocityX * contact.velocityX
                    + contact.velocityZ * contact.velocityZ);
            if (velocity > 0.001D) {
                int dx = (int) Math.round(contact.velocityX / velocity * 10.0D);
                int dy = (int) Math.round(contact.velocityZ / velocity * 10.0D);
                drawCourse(x, y, dx, -dy, color);
            }
            if (contact.entityId == selectedContactId) {
                Gui.drawRect(x - 6, y - 6, x + 7, y - 5, 0xFFFFFFFF);
                Gui.drawRect(x - 6, y + 6, x + 7, y + 7, 0xFFFFFFFF);
                Gui.drawRect(x - 6, y - 6, x - 5, y + 7, 0xFFFFFFFF);
                Gui.drawRect(x + 6, y - 6, x + 7, y + 7, 0xFFFFFFFF);
            }
        }
        int commandX = worldToMapX(command.posX);
        int commandY = worldToMapY(command.posZ);
        if (insideAbsoluteMap(commandX, commandY)) {
            Gui.drawRect(commandX - 4, commandY - 1,
                    commandX + 5, commandY + 2, 0xFFFFFFFF);
            Gui.drawRect(commandX - 1, commandY - 4,
                    commandX + 2, commandY + 5, 0xFFFFFFFF);
        }
    }

    private void drawGrid() {
        int radius = VIEW_RADII[zoomIndex];
        int firstX = (int) Math.floor((viewX - radius) / 512.0D) * 512;
        int lastX = (int) Math.ceil((viewX + radius) / 512.0D) * 512;
        for (int worldX = firstX; worldX <= lastX; worldX += 512) {
            int x = worldToMapX(worldX);
            if (x >= guiLeft + MAP_X && x <= guiLeft + MAP_X + MAP_W) {
                Gui.drawRect(x, guiTop + MAP_Y, x + 1,
                        guiTop + MAP_Y + MAP_H, 0x442A5542);
            }
        }
        int firstZ = (int) Math.floor((viewZ - radius) / 512.0D) * 512;
        int lastZ = (int) Math.ceil((viewZ + radius) / 512.0D) * 512;
        for (int worldZ = firstZ; worldZ <= lastZ; worldZ += 512) {
            int y = worldToMapY(worldZ);
            if (y >= guiTop + MAP_Y && y <= guiTop + MAP_Y + MAP_H) {
                Gui.drawRect(guiLeft + MAP_X, y,
                        guiLeft + MAP_X + MAP_W, y + 1, 0x442A5542);
            }
        }
    }

    private void drawCourse(int x, int y, int dx, int dy, int color) {
        int length = Math.max(Math.abs(dx), Math.abs(dy));
        if (length <= 0) return;
        for (int index = 1; index <= length; ++index) {
            int px = x + dx * index / length;
            int py = y + dy * index / length;
            if (insideAbsoluteMap(px, py)) {
                Gui.drawRect(px, py, px + 1, py + 1, color);
            }
        }
    }

    private void drawLegend() {
        fontRenderer.drawString("C HQ", 14, 240, 0xFFFFFF);
        fontRenderer.drawString("R RADAR", 44, 240, 0x5FE6EF);
        fontRenderer.drawString("S STRAT", 92, 240, 0x70A8FF);
        fontRenderer.drawString("L PVO", 140, 240, 0x8BE36C);
        fontRenderer.drawString("T LINK", 176, 240, 0xE4D568);
        fontRenderer.drawString("RK HENRY", 218, 240, 0xFF9A58);
        fontRenderer.drawString("F FRIEND", 278, 240, 0x63F09A);
    }

    private void drawMapReadout() {
        fontRenderer.drawString("CTR " + (int) viewX + " / " + (int) viewZ,
                14, 34, 0xA9B8B0);
        fontRenderer.drawString("N", 203, 34, 0xD8E7DF);
        String scale = "+/- " + VIEW_RADII[zoomIndex];
        fontRenderer.drawString(scale, 396 - fontRenderer.getStringWidth(scale),
                34, 0xA9B8B0);
    }

    private void drawInventorySlots() {
        drawSlot(guiLeft + 510, guiTop + 270);
        for (int row = 0; row < 3; ++row) {
            for (int column = 0; column < 9; ++column) {
                drawSlot(guiLeft + 12 + column * 18,
                        guiTop + 270 + row * 18);
            }
        }
        for (int column = 0; column < 9; ++column) {
            drawSlot(guiLeft + 12 + column * 18, guiTop + 328);
        }
    }

    private static void drawSlot(int x, int y) {
        Gui.drawRect(x, y, x + 18, y + 18, 0xFF343434);
        Gui.drawRect(x + 1, y + 1, x + 17, y + 17, 0xFF9D9D9A);
    }

    private void drawPair(int x, int y, String label, String value, int color) {
        fontRenderer.drawString(label, x, y, 0x9EB1A5);
        fontRenderer.drawString(value, 526 - fontRenderer.getStringWidth(value),
                y, color);
    }

    private void selectNearestContact(int mouseX, int mouseY) {
        FactionCommandSnapshot snapshot = FactionCommandClient.getSnapshot();
        int selected = 0;
        int bestDistance = 81;
        for (FactionCommandSnapshot.Contact contact : snapshot.contacts) {
            int dx = worldToMapX(contact.x) - mouseX;
            int dy = worldToMapY(contact.z) - mouseY;
            int distance = dx * dx + dy * dy;
            if (distance < bestDistance) {
                bestDistance = distance;
                selected = contact.entityId;
            }
        }
        selectedContactId = selected;
    }

    private FactionCommandSnapshot.Contact selected(
            FactionCommandSnapshot snapshot) {
        for (FactionCommandSnapshot.Contact contact : snapshot.contacts) {
            if (contact.entityId == selectedContactId) return contact;
        }
        return null;
    }

    private int worldToMapX(double worldX) {
        double normalized = (worldX - viewX) / VIEW_RADII[zoomIndex];
        return guiLeft + MAP_X + MAP_W / 2
                + (int) Math.round(normalized * MAP_W / 2.0D);
    }

    private int worldToMapY(double worldZ) {
        double normalized = (worldZ - viewZ) / VIEW_RADII[zoomIndex];
        return guiTop + MAP_Y + MAP_H / 2
                - (int) Math.round(normalized * MAP_H / 2.0D);
    }

    private boolean insideAbsoluteMap(int x, int y) {
        return x >= guiLeft + MAP_X && x <= guiLeft + MAP_X + MAP_W
                && y >= guiTop + MAP_Y && y <= guiTop + MAP_Y + MAP_H;
    }

    private void requestSnapshot() {
        if (command != null && !command.isDead) {
            WarTechNetwork.CHANNEL.sendToServer(
                    new FactionCommandRequestMessage(command.getEntityId()));
        }
    }

    private static int countNodes(FactionCommandSnapshot snapshot, int type) {
        int count = 0;
        for (FactionCommandSnapshot.Node node : snapshot.nodes) {
            if (node.type == type) ++count;
        }
        return count;
    }

    private static int countHostileContacts(FactionCommandSnapshot snapshot) {
        int count = 0;
        for (FactionCommandSnapshot.Contact contact : snapshot.contacts) {
            if (!contact.friendly) ++count;
        }
        return count;
    }

    private static int nodeColor(int type) {
        if (type == 3) return 0xFF8BE36C;
        if (type == 4) return 0xFFFFFFFF;
        if (type == 5) return 0xFFE4D568;
        if (type == 2) return 0xFF70A8FF;
        return 0xFF5FE6EF;
    }

    private static String nodeSymbol(int type) {
        if (type == 3) return "L";
        if (type == 4) return "C";
        if (type == 5) return "T";
        if (type == 2) return "S";
        return "R";
    }

    private static int contactColor(int type) {
        if (type == 6) return 0xFFFF55D8;
        if (type == 4) return 0xFFFF4A4A;
        if (type == 2 || type == 3) return 0xFFFF8A66;
        if (type == 5) return 0xFFFFB45D;
        return 0xFFFF6868;
    }

    private static String contactCode(int type) {
        if (type == 6) return "RK";
        if (type == 4) return "BM";
        if (type == 2) return "AC";
        if (type == 3) return "HV";
        if (type == 5) return "DR";
        if (type == 1) return "MS";
        return "?";
    }

    private static int speed(FactionCommandSnapshot.Contact contact) {
        return (int) Math.round(Math.sqrt(
                contact.velocityX * contact.velocityX
                + contact.velocityZ * contact.velocityZ) * 20.0D);
    }

    private static int heading(FactionCommandSnapshot.Contact contact) {
        double heading = Math.toDegrees(
                Math.atan2(-contact.velocityX, contact.velocityZ));
        if (heading < 0.0D) heading += 360.0D;
        return (int) Math.round(heading) % 360;
    }
}
