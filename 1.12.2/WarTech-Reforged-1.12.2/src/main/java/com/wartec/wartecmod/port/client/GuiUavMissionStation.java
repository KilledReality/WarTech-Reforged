package com.wartec.wartecmod.port.client;

import com.wartec.wartecmod.port.gameplay.TileEntityUavMissionStation;
import com.wartec.wartecmod.port.gui.ContainerUavMissionStation;
import com.wartec.wartecmod.port.integration.DesignatorCompat;
import com.wartec.wartecmod.port.network.UavMissionEditMessage;
import com.wartec.wartecmod.port.network.UavFleetActionMessage;
import com.wartec.wartecmod.port.network.UavFleetRequestMessage;
import com.wartec.wartecmod.port.network.UavFleetSnapshot;
import com.wartec.wartecmod.port.network.WarTechNetwork;
import com.wartec.wartecmod.port.uav.UavBuild;
import com.wartec.wartecmod.port.uav.UavAirframe;
import com.wartec.wartecmod.port.uav.UavMission;
import com.wartec.wartecmod.port.uav.UavStats;
import com.wartec.wartecmod.port.uav.UavWaypoint;
import com.wartec.wartecmod.port.uav.UavWaypointMode;
import com.wartec.wartecmod.port.uav.UavReconReport;
import com.wartec.wartecmod.port.uav.UavReconReport.ReconContact;
import com.wartec.wartecmod.port.uav.UavReconReport.SurveyCell;
import java.io.IOException;
import java.util.List;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.inventory.Slot;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.input.Keyboard;

/** Coordinate mission editor for assembled modular UAVs. */
public final class GuiUavMissionStation extends GuiContainer {
    private static final int FLEET_ROWS = 4;
    private final TileEntityUavMissionStation tile;
    private GuiTextField xField;
    private GuiTextField yField;
    private GuiTextField zField;
    private UavWaypointMode selectedMode = UavWaypointMode.TRANSIT;
    private String inputError = "";
    private boolean reconView;
    private boolean fleetView;
    private int selectedFleetIndex = -1;
    private int fleetPage;
    private int fleetRefreshTicks;
    private ReconContact selectedContact;
    private BlockPos loadedDesignatorTarget;

    public GuiUavMissionStation(InventoryPlayer inventory,
            TileEntityUavMissionStation tile) {
        super(new ContainerUavMissionStation(inventory, tile));
        this.tile = tile;
        xSize = 304;
        ySize = 272;
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        int fieldY = guiTop + 74;
        xField = coordinateField(0, guiLeft + 17, fieldY,
                Integer.toString((int) mc.player.posX));
        yField = coordinateField(1, guiLeft + 81, fieldY,
                Integer.toString((int) mc.player.posY + 25));
        zField = coordinateField(2, guiLeft + 145, fieldY,
                Integer.toString((int) mc.player.posZ));
        buttonList.add(new WarTechGuiButton(10, guiLeft + 16, guiTop + 105,
                66, 18, com.wartec.wartecmod.port.uav.UavText.ui("TRANSIT")));
        buttonList.add(new WarTechGuiButton(11, guiLeft + 85, guiTop + 105,
                66, 18, com.wartec.wartecmod.port.uav.UavText.ui("OBSERVE")));
        buttonList.add(new WarTechGuiButton(12, guiLeft + 154, guiTop + 105,
                66, 18, com.wartec.wartecmod.port.uav.UavText.ui("STRIKE")));
        buttonList.add(new WarTechGuiButton(13, guiLeft + 223, guiTop + 105,
                66, 18, com.wartec.wartecmod.port.uav.UavText.ui("RETURN")));
        buttonList.add(new WarTechGuiButton(14, guiLeft + 209, guiTop + 71,
                86, 20, com.wartec.wartecmod.port.uav.UavText.ui("PROGRAM / OK")));
        buttonList.add(new WarTechGuiButton(15, guiLeft + 16, guiTop + 137,
                92, 18, com.wartec.wartecmod.port.uav.UavText.ui("REMOVE LAST")));
        buttonList.add(new WarTechGuiButton(16, guiLeft + 112, guiTop + 137,
                88, 18, com.wartec.wartecmod.port.uav.UavText.ui("CLEAR ROUTE")));
        buttonList.add(new WarTechGuiButton(20, guiLeft + 153, guiTop + 3,
                68, 16, com.wartec.wartecmod.port.uav.UavText.ui("RECON")));
        buttonList.add(new WarTechGuiButton(21, guiLeft + 224, guiTop + 3,
                72, 16, com.wartec.wartecmod.port.uav.UavText.ui("FLEET")));
        buttonList.add(new WarTechGuiButton(30, guiLeft + 16, guiTop + 157,
                88, 18, com.wartec.wartecmod.port.uav.UavText.ui("RETURN UAV")));
        buttonList.add(new WarTechGuiButton(31, guiLeft + 108, guiTop + 157,
                88, 18, com.wartec.wartecmod.port.uav.UavText.ui("GET REPORT")));
        buttonList.add(new WarTechGuiButton(32, guiLeft + 214, guiTop + 157,
                34, 18, "<"));
        buttonList.add(new WarTechGuiButton(33, guiLeft + 252, guiTop + 157,
                36, 18, ">"));
        updateTaskButtons();
        if (fleetView && ++fleetRefreshTicks % 40 == 0) requestFleetSnapshot();
        updateViewControls();
    }

    private GuiTextField coordinateField(int id, int x, int y, String value) {
        GuiTextField field = new GuiTextField(id, fontRenderer, x, y, 55, 16);
        field.setMaxStringLength(9);
        field.setText(value);
        return field;
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        super.onGuiClosed();
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        xField.updateCursorCounter();
        yField.updateCursorCounter();
        zField.updateCursorCounter();
        loadDesignatorCoordinates();
        updateTaskButtons();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (!button.enabled) return;
        if (button.id == 20) {
            reconView = !reconView;
            fleetView = false;
            button.displayString = reconView ? com.wartec.wartecmod.port.uav.UavText.ui("MISSION") : com.wartec.wartecmod.port.uav.UavText.ui("RECON");
            updateViewControls();
            return;
        }
        if (button.id == 21) {
            fleetView = !fleetView;
            reconView = false;
            selectedFleetIndex = -1;
            fleetPage = 0;
            requestFleetSnapshot();
            updateViewControls();
            return;
        }
        if (button.id >= 30 && button.id <= 33) {
            handleFleetButton(button.id);
            return;
        }
        if (button.id >= 10 && button.id <= 13) {
            selectedMode = UavWaypointMode.byIndex(button.id - 10);
            updateTaskButtons();
            return;
        }
        int action;
        if (button.id == 14) {
            action = selectedMode != UavWaypointMode.RETURN
                    && DesignatorCompat.isDesignator(tile.getStackInSlot(
                            TileEntityUavMissionStation.DESIGNATOR_SLOT))
                    ? 3 : 0;
        }
        else if (button.id == 15) action = 1;
        else if (button.id == 16) action = 2;
        else return;
        int x = 0;
        int y = 64;
        int z = 0;
        if(action==3) {
            y=-1; // HBM X/Z is authoritative; server resolves missing elevation.
        } else if (selectedMode != UavWaypointMode.RETURN) {
            try {
                x = Integer.parseInt(xField.getText().trim());
                y = Integer.parseInt(yField.getText().trim());
                z = Integer.parseInt(zField.getText().trim());
            } catch (NumberFormatException error) {
                inputError = com.wartec.wartecmod.port.uav.UavText.ui("Coordinates must be whole numbers.");
                return;
            }
            if (y < 1 || y > 255) {
                inputError = com.wartec.wartecmod.port.uav.UavText.ui("Y must be between 1 and 255.");
                return;
            }
        }
        inputError = "";
        WarTechNetwork.CHANNEL.sendToServer(new UavMissionEditMessage(
                tile.getPos(), action, x, y, z, selectedMode.ordinal()));
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (!reconView && !fleetView
                && (xField.textboxKeyTyped(typedChar, keyCode)
                || yField.textboxKeyTyped(typedChar, keyCode)
                || zField.textboxKeyTyped(typedChar, keyCode))) return;
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton)
            throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (reconView && mouseButton == 0
                && loadContactAt(mouseX - guiLeft, mouseY - guiTop)) {
            return;
        }
        if (fleetView && mouseButton == 0) {
            int localX = mouseX - guiLeft;
            int localY = mouseY - guiTop;
            if (localX >= 10 && localX < 294 && localY >= 29
                    && localY < 29 + FLEET_ROWS * 25) {
                int row = (localY - 29) / 25;
                int absolute = fleetPage * FLEET_ROWS + row;
                if (absolute < UavFleetClient.get().entries.length) {
                    selectedFleetIndex = absolute;
                }
                return;
            }
        }
        if (reconView || fleetView) return;
        xField.mouseClicked(mouseX, mouseY, mouseButton);
        yField.mouseClicked(mouseX, mouseY, mouseButton);
        zField.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (!reconView && !fleetView) {
            xField.drawTextBox();
            yField.drawTextBox();
            zField.drawTextBox();
        }
        renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks,
            int mouseX, int mouseY) {
        GuiTheme.frame(guiLeft,guiTop,xSize,ySize);
        Gui.drawRect(guiLeft + 8, guiTop + 27, guiLeft + 296,
                guiTop + 58, 0xFF172126);
        if (fleetView) {
            Gui.drawRect(guiLeft + 8, guiTop + 27, guiLeft + 296,
                    guiTop + 180, 0xFF101719);
            drawFleetRows();
        } else if (reconView) {
            Gui.drawRect(guiLeft + 42, guiTop + 27, guiLeft + 296,
                    guiTop + 180, 0xFF101719);
            drawReconMap();
        } else {
            Gui.drawRect(guiLeft + 8, guiTop + 130, guiLeft + 296,
                    guiTop + 180, 0xFF172126);
        }
        GuiTheme.panel(guiLeft+65,guiTop+189,174,81);
        for (Slot slot : inventorySlots.inventorySlots) {
            if (fleetView && slot.yPos == 39) continue;
            drawSlot(guiLeft + slot.xPos - 1, guiTop + slot.yPos - 1);
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        label(com.wartec.wartecmod.port.uav.UavText.ui("UAV MISSION PROGRAMMER"), 9, 7, 0xE8F1EC);
        ItemStack stack = tile.getStackInSlot(
                TileEntityUavMissionStation.UAV_SLOT);
        if (fleetView) {
            drawFleetForeground();
            return;
        }
        if (reconView) {
            drawReconForeground(stack);
            return;
        }
        if (stack.isEmpty()) {
            label(com.wartec.wartecmod.port.uav.UavText.ui("Insert assembled UAV"), 66, 35, 0xFF8888);
        } else {
            label(UavBuild.fromStack(stack).getName(),
                    66, 30, 0x70F0B1);
            label(missionProfile(
                    UavBuild.fromStack(stack).calculateStats()),
                    66, 41, 0xBFC8C1);
        }
        label(com.wartec.wartecmod.port.uav.UavText.ui("UAV"), 15, 27, 0xAEB9B3);
        label(com.wartec.wartecmod.port.uav.UavText.ui("TGT"), 39, 27, 0xAEB9B3);
        ItemStack designator = tile.getStackInSlot(
                TileEntityUavMissionStation.DESIGNATOR_SLOT);
        if (!DesignatorCompat.isDesignator(designator)) {
            label(com.wartec.wartecmod.port.uav.UavText.ui("Insert HBM designator or enter coordinates"),
                    66, 51, 0xD5B75E);
        }
        label(com.wartec.wartecmod.port.uav.UavText.ui("1  TARGET COORDINATES"), 13, 62, 0x7FC9A2);
        if (selectedMode == UavWaypointMode.RETURN) {
            label(com.wartec.wartecmod.port.uav.UavText.ui("HOME POSITION USED"), 127, 62,
                    0xD5B75E);
        }
        label("X", 9, 77, 0xD8DFD8);
        label("Y", 73, 77, 0xD8DFD8);
        label("Z", 137, 77, 0xD8DFD8);
        label(com.wartec.wartecmod.port.uav.UavText.ui("2  SELECT TASK"), 13, 96, 0x7FC9A2);
        UavMission mission = tile.getMission();
        label(com.wartec.wartecmod.port.uav.UavText.ui("3  PROGRAMMED ROUTE  ") + mission.size()
                + " / " + UavMission.MAX_WAYPOINTS,
                13, 125, 0x7FC9A2);
        StringBuilder firstRow = new StringBuilder();
        StringBuilder secondRow = new StringBuilder();
        for (int index = 0; index < mission.size(); ++index) {
            UavWaypoint point = mission.get(index);
            StringBuilder route = index < 4 ? firstRow : secondRow;
            if (route.length() > 0) route.append(" > ");
            route.append(index + 1).append(' ')
                    .append(shortMode(point.getMode())).append(' ')
                    .append(point.getX()).append('/')
                    .append(point.getY()).append('/')
                    .append(point.getZ());
        }
        label(fontRenderer.trimStringToWidth(
                firstRow.toString(), 278), 13, 159, 0xD8DFD8);
        label(fontRenderer.trimStringToWidth(
                secondRow.toString(), 278), 13, 170, 0xD8DFD8);
        if (!inputError.isEmpty()) {
            label(inputError, 13, 181, 0xFF7777);
        } else if (!mission.isValidFor(UavBuild.fromStack(stack))) {
            label(com.wartec.wartecmod.port.uav.UavText.ui("ROUTE IS INCOMPATIBLE OR INCOMPLETE"),
                    13, 181, 0xFFAA66);
        } else {
            label(com.wartec.wartecmod.port.uav.UavText.ui("MISSION DATA IS STORED IN THIS UAV"),
                    13, 181, 0x7FC9A2);
        }
    }

    private static String shortMode(UavWaypointMode mode) {
        if (mode == UavWaypointMode.OBSERVE) return com.wartec.wartecmod.port.uav.UavText.ui("OBS");
        if (mode == UavWaypointMode.STRIKE) return com.wartec.wartecmod.port.uav.UavText.ui("ATK");
        if (mode == UavWaypointMode.RETURN) return com.wartec.wartecmod.port.uav.UavText.ui("RTB");
        return com.wartec.wartecmod.port.uav.UavText.ui("NAV");
    }

    private void updateTaskButtons() {
        if (buttonList.isEmpty()) return;
        UavBuild build = UavBuild.fromStack(tile.getStackInSlot(
                TileEntityUavMissionStation.UAV_SLOT));
        if (!UavMission.isModeAllowed(build, selectedMode)) {
            selectedMode = UavWaypointMode.TRANSIT;
        }
        for (GuiButton button : buttonList) {
            if (button.id >= 10 && button.id <= 13) {
                UavWaypointMode mode = UavWaypointMode.byIndex(button.id - 10);
                button.enabled = UavMission.isModeAllowed(build, mode);
                String label = mode == UavWaypointMode.RETURN
                        ? com.wartec.wartecmod.port.uav.UavText.ui("RETURN") : mode.getDisplayName().toUpperCase();
                button.displayString = mode == selectedMode
                        ? ">" + label + "<" : label;
            } else if (button.id == 14) {
                button.enabled = UavMission.isModeAllowed(build, selectedMode);
                button.displayString = com.wartec.wartecmod.port.uav.UavText.ui("PROGRAM / OK");
            } else if (button.id == 15 || button.id == 16) {
                button.enabled = !tile.getMission().isEmpty();
            }
        }
        boolean coordinatesEnabled = build.getAirframe() != null
                && selectedMode != UavWaypointMode.RETURN;
        xField.setEnabled(coordinatesEnabled);
        yField.setEnabled(coordinatesEnabled);
        zField.setEnabled(coordinatesEnabled);
    }

    private static String missionProfile(UavStats stats) {
        if (stats.getAirframe() == null) return com.wartec.wartecmod.port.uav.UavText.ui("NO AIRFRAME DATA");
        if (stats.getBlastStrength() > 0.0F) {
            return stats.getAirframe() == UavAirframe.ONE_WAY
                    ? com.wartec.wartecmod.port.uav.UavText.ui("PROFILE: ONE-WAY ATTACK")
                    : com.wartec.wartecmod.port.uav.UavText.ui("PROFILE: HEAVY KAMIKAZE");
        }
        if (stats.getAirframe() == UavAirframe.RECON) {
            return com.wartec.wartecmod.port.uav.UavText.ui("PROFILE: RECON / SURVEY");
        }
        return com.wartec.wartecmod.port.uav.UavText.ui("PROFILE: REUSABLE STRIKE");
    }

    private void updateViewControls() {
        boolean missionView = !reconView && !fleetView;
        xField.setVisible(missionView);
        yField.setVisible(missionView);
        zField.setVisible(missionView);
        for (GuiButton button : buttonList) {
            if (button.id >= 10 && button.id <= 16) {
                button.visible = missionView;
            } else if (button.id >= 30 && button.id <= 33) {
                button.visible = fleetView;
            } else if (button.id == 20) {
                button.displayString = reconView ? com.wartec.wartecmod.port.uav.UavText.ui("MISSION") : com.wartec.wartecmod.port.uav.UavText.ui("RECON");
            } else if (button.id == 21) {
                button.displayString = fleetView ? com.wartec.wartecmod.port.uav.UavText.ui("MISSION") : com.wartec.wartecmod.port.uav.UavText.ui("FLEET");
            }
        }
        updateFleetButtons();
    }

    private void requestFleetSnapshot() {
        if (mc.player != null) {
            WarTechNetwork.CHANNEL.sendToServer(
                    new UavFleetRequestMessage(tile.getPos()));
        }
    }

    private void handleFleetButton(int id) {
        UavFleetSnapshot snapshot = UavFleetClient.get();
        if (id == 32) {
            fleetPage = Math.max(0, fleetPage - 1);
            selectedFleetIndex = -1;
        } else if (id == 33) {
            int pages = Math.max(1, (snapshot.entries.length + FLEET_ROWS - 1) / FLEET_ROWS);
            fleetPage = Math.min(pages - 1, fleetPage + 1);
            selectedFleetIndex = -1;
        } else if (selectedFleetIndex >= 0
                && selectedFleetIndex < snapshot.entries.length) {
            UavFleetSnapshot.Entry entry = snapshot.entries[selectedFleetIndex];
            WarTechNetwork.CHANNEL.sendToServer(new UavFleetActionMessage(
                    tile.getPos(), entry.entityId,
                    id == 30 ? UavFleetActionMessage.RETURN
                            : UavFleetActionMessage.DOWNLOAD_REPORT));
            requestFleetSnapshot();
        }
        updateFleetButtons();
    }

    private void updateFleetButtons() {
        UavFleetSnapshot snapshot = UavFleetClient.get();
        UavFleetSnapshot.Entry selected = selectedFleetIndex >= 0
                && selectedFleetIndex < snapshot.entries.length
                        ? snapshot.entries[selectedFleetIndex] : null;
        int pages = Math.max(1, (snapshot.entries.length + FLEET_ROWS - 1) / FLEET_ROWS);
        for (GuiButton button : buttonList) {
            if (button.id == 30) button.enabled = selected != null
                    && selected.reusable && !"READY".equals(selected.state);
            else if (button.id == 31) button.enabled = selected != null;
            else if (button.id == 32) button.enabled = fleetPage > 0;
            else if (button.id == 33) button.enabled = fleetPage + 1 < pages;
        }
    }

    private void drawFleetRows() {
        UavFleetSnapshot snapshot = UavFleetClient.get();
        int start = fleetPage * FLEET_ROWS;
        for (int row = 0; row < FLEET_ROWS; ++row) {
            int index = start + row;
            int top = guiTop + 29 + row * 25;
            int color = index == selectedFleetIndex
                    ? 0xFF35505A : (row & 1) == 0 ? 0xFF1B272B : 0xFF202D31;
            Gui.drawRect(guiLeft + 10, top, guiLeft + 294, top + 23, color);
        }
    }

    private void drawFleetForeground() {
        UavFleetSnapshot snapshot = UavFleetClient.get();
        int start = fleetPage * FLEET_ROWS;
        for (int row = 0; row < FLEET_ROWS; ++row) {
            int index = start + row;
            if (index >= snapshot.entries.length) break;
            UavFleetSnapshot.Entry entry = snapshot.entries[index];
            int top = 32 + row * 25;
            label(fontRenderer.trimStringToWidth(
                    entry.name, 126), 14, top, 0x7FE7B1);
            label(com.wartec.wartecmod.port.uav.UavText.ui(entry.state), 146, top, 0xEACB72);
            label(com.wartec.wartecmod.port.uav.UavText.ui("HP ") + entry.healthPercent + com.wartec.wartecmod.port.uav.UavText.ui("%  PWR ")
                    + entry.powerPercent + "%", 211, top, 0xD8DFD8);
            String mission = entry.missionSize == 0 ? com.wartec.wartecmod.port.uav.UavText.ui("NO ROUTE")
                    : com.wartec.wartecmod.port.uav.UavText.ui("STEP ") + (entry.missionIndex + 1) + "/" + entry.missionSize;
            label(entry.x + " / " + entry.y + " / "
                    + entry.z + "   " + mission, 14, top + 11, 0xAEB9B3);
        }
        int pages = Math.max(1, (snapshot.entries.length + FLEET_ROWS - 1) / FLEET_ROWS);
        label(com.wartec.wartecmod.port.uav.UavText.ui("ONLINE ") + snapshot.entries.length
                + com.wartec.wartecmod.port.uav.UavText.ui("   PAGE ") + (fleetPage + 1) + "/" + pages,
                13, 145, 0x7FC9A2);
        updateFleetButtons();
    }

    private void drawReconMap() {
        UavReconReport report = UavReconReport.fromStack(tile.getStackInSlot(
                TileEntityUavMissionStation.UAV_SLOT));
        MapProjection projection = MapProjection.create(report);
        if (projection == null) return;
        for (SurveyCell cell : report.getCells()) {
            int left = guiLeft + projection.x(cell.x);
            int top = guiTop + projection.y(cell.z);
            int size = Math.max(1, (int) Math.ceil(projection.scale));
            Gui.drawRect(left, top, Math.min(guiLeft + 295, left + size),
                    Math.min(guiTop + 179, top + size),
                    shadeTerrain(cell.color, cell.height));
        }
        for (UavWaypoint waypoint : tile.getMission().getWaypoints()) {
            if (waypoint.getMode() != UavWaypointMode.OBSERVE) continue;
            int x = guiLeft + projection.worldX(waypoint.getX());
            int y = guiTop + projection.worldZ(waypoint.getZ());
            Gui.drawRect(x - 2, y - 2, x + 3, y + 3, 0xFF55DDE5);
        }
        for (ReconContact contact : report.getContacts()) {
            int x = guiLeft + projection.worldX(contact.x);
            int y = guiTop + projection.worldZ(contact.z);
            int color = relationColor(contact);
            int radius = selectedContact != null
                    && selectedContact.uuid.equals(contact.uuid) ? 3 : 2;
            Gui.drawRect(x - radius, y - 1, x + radius + 1, y + 2, color);
            Gui.drawRect(x - 1, y - radius, x + 2, y + radius + 1, color);
        }
    }

    private void drawReconForeground(ItemStack stack) {
        UavReconReport report = UavReconReport.fromStack(stack);
        label(com.wartec.wartecmod.port.uav.UavText.ui("DATA"), 12, 31, 0x7FC9A2);
        label(com.wartec.wartecmod.port.uav.UavText.ui("CELLS"), 12, 62, 0xAEB9B3);
        label(Integer.toString(report.getCellCount()),
                12, 72, 0xE8F1EC);
        label(com.wartec.wartecmod.port.uav.UavText.ui("TRACKS"), 12, 88, 0xAEB9B3);
        label(Integer.toString(report.getContactCount()),
                12, 98, report.getContactCount() > 0
                        ? 0xFF8877 : 0xE8F1EC);
        label(com.wartec.wartecmod.port.uav.UavText.ui("DIM ") + (report.hasDimension()
                ? report.getDimension() : "--"), 12, 116, 0xAEB9B3);
        if (report.getCellCount() == 0) {
            label(com.wartec.wartecmod.port.uav.UavText.ui("NO RECON DATA - COMPLETE AN OBSERVE POINT"),
                    56, 80, 0xFFAA66);
        } else {
            label(com.wartec.wartecmod.port.uav.UavText.ui("CYAN: OBSERVE   GREEN: FRIENDLY   RED: CONTACT"),
                    48, 169, 0xC7D0CB);
        }
        if (selectedContact != null) {
            label(selectedContact.name + " // "
                    + contactName(selectedContact.type) + "  "
                    + selectedContact.x + " / "
                    + selectedContact.y + " / " + selectedContact.z,
                    48, 28, relationColor(selectedContact));
        }
    }

    private boolean loadContactAt(int mouseX, int mouseY) {
        UavReconReport report = UavReconReport.fromStack(tile.getStackInSlot(
                TileEntityUavMissionStation.UAV_SLOT));
        MapProjection projection = MapProjection.create(report);
        if (projection == null || mouseX < 42 || mouseX >= 296
                || mouseY < 27 || mouseY >= 180) return false;
        ReconContact nearest = null;
        int nearestSq = 64;
        for (ReconContact contact : report.getContacts()) {
            int dx = projection.worldX(contact.x) - mouseX;
            int dy = projection.worldZ(contact.z) - mouseY;
            int distanceSq = dx * dx + dy * dy;
            if (distanceSq < nearestSq) {
                nearestSq = distanceSq;
                nearest = contact;
            }
        }
        if (nearest == null) return false;
        selectedContact = nearest;
        xField.setText(Integer.toString(nearest.x));
        yField.setText(Integer.toString(nearest.y));
        zField.setText(Integer.toString(nearest.z));
        selectedMode = UavWaypointMode.STRIKE;
        updateTaskButtons();
        reconView = false;
        for (GuiButton button : buttonList) {
            if (button.id == 20) button.displayString = com.wartec.wartecmod.port.uav.UavText.ui("RECON MAP");
        }
        updateViewControls();
        return true;
    }

    private void loadDesignatorCoordinates() {
        if (reconView || fleetView || mc.world == null || mc.player == null) return;
        ItemStack designator = tile.getStackInSlot(
                TileEntityUavMissionStation.DESIGNATOR_SLOT);
        BlockPos target = DesignatorCompat.getTarget(mc.world, mc.player,
                designator);
        if(target==null) {
            net.minecraft.util.math.Vec3d horizontal=DesignatorCompat.getHorizontalTarget(designator);
            if(horizontal!=null) {
                BlockPos column=new BlockPos(horizontal);
                if(!column.equals(loadedDesignatorTarget)) {
                    loadedDesignatorTarget=column;xField.setText(Integer.toString(column.getX()));
                    zField.setText(Integer.toString(column.getZ()));yField.setText("");inputError="";
                }
            }return;
        }
        if (target.equals(loadedDesignatorTarget)) return;
        loadedDesignatorTarget = target;
        xField.setText(Integer.toString(target.getX()));
        yField.setText(Integer.toString(target.getY()));
        zField.setText(Integer.toString(target.getZ()));
        inputError = "";
    }

    private static void drawSlot(int x, int y) {
        GuiTheme.slot(x,y);
    }

    private static int contactColor(int type) {
        if (type == ReconContact.AIRCRAFT) return 0xFFFFA84A;
        if (type == ReconContact.MISSILE) return 0xFFFFE06A;
        if (type == ReconContact.GROUND_VEHICLE) return 0xFFFF655A;
        return 0xFFE89696;
    }

    private static int relationColor(ReconContact contact) {
        if (contact.relation == ReconContact.FRIENDLY) return 0xFF55EE99;
        if (contact.relation == ReconContact.NEUTRAL) return 0xFF75C5DA;
        if (contact.relation == ReconContact.UNKNOWN) return 0xFFC3C8C5;
        return contactColor(contact.type);
    }

    private static String contactName(int type) {
        if (type == ReconContact.AIRCRAFT) return com.wartec.wartecmod.port.uav.UavText.ui("AIRCRAFT");
        if (type == ReconContact.MISSILE) return com.wartec.wartecmod.port.uav.UavText.ui("MISSILE");
        if (type == ReconContact.GROUND_VEHICLE) return com.wartec.wartecmod.port.uav.UavText.ui("GROUND VEHICLE");
        if (type == ReconContact.PLAYER) return com.wartec.wartecmod.port.uav.UavText.ui("PERSON");
        if (type == ReconContact.HOSTILE_MOB) return com.wartec.wartecmod.port.uav.UavText.ui("HOSTILE CREATURE");
        if (type == ReconContact.PASSIVE_MOB) return com.wartec.wartecmod.port.uav.UavText.ui("ANIMAL");
        return com.wartec.wartecmod.port.uav.UavText.ui("CIVILIAN / LIVING");
    }

    private static int shadeTerrain(int color, int height) {
        float shade = 0.72F + Math.min(0.24F, height / 512.0F);
        int red = Math.min(255, Math.round(((color >> 16) & 255) * shade));
        int green = Math.min(255, Math.round(((color >> 8) & 255) * shade));
        int blue = Math.min(255, Math.round((color & 255) * shade));
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }

    private static final class MapProjection {
        private final int minX;
        private final int minZ;
        private final float scale;
        private final float offsetX;
        private final float offsetZ;

        private MapProjection(int minX, int minZ, float scale,
                float offsetX, float offsetZ) {
            this.minX = minX;
            this.minZ = minZ;
            this.scale = scale;
            this.offsetX = offsetX;
            this.offsetZ = offsetZ;
        }

        static MapProjection create(UavReconReport report) {
            List<SurveyCell> cells = report.getCells();
            if (cells.isEmpty()) return null;
            int minX = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int minZ = Integer.MAX_VALUE;
            int maxZ = Integer.MIN_VALUE;
            for (SurveyCell cell : cells) {
                minX = Math.min(minX, cell.x);
                maxX = Math.max(maxX, cell.x);
                minZ = Math.min(minZ, cell.z);
                maxZ = Math.max(maxZ, cell.z);
            }
            int spanX = Math.max(1, maxX - minX + 1);
            int spanZ = Math.max(1, maxZ - minZ + 1);
            float scale = Math.max(1.0F,
                    Math.min(250.0F / spanX, 148.0F / spanZ));
            float offsetX = 43.0F + (252.0F - spanX * scale) * 0.5F;
            float offsetZ = 28.0F + (150.0F - spanZ * scale) * 0.5F;
            return new MapProjection(minX, minZ, scale, offsetX, offsetZ);
        }

        int x(int cellX) {
            return Math.round(offsetX + (cellX - minX) * scale);
        }
        int y(int cellZ) {
            return Math.round(offsetZ + (cellZ - minZ) * scale);
        }
        int worldX(int blockX) { return x(Math.floorDiv(blockX, 16)); }
        int worldZ(int blockZ) { return y(Math.floorDiv(blockZ, 16)); }
    }
    private void label(String text,int x,int y,int color) {
        int max=xSize-x-9;
        if(y==7) max=136;
        else if(fleetView && x==146) max=60;
        else if(!fleetView && !reconView && y==62 && x==13 && selectedMode==UavWaypointMode.RETURN) max=106;
        else if(reconView && x==12) max=27;
        GuiTheme.text(fontRenderer,text,x,y,Math.max(8,max),color);
    }
}
