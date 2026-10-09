package com.wartec.wartecmod.port.client;

import com.wartec.wartecmod.port.uav.UavReconReport;
import com.wartec.wartecmod.port.uav.UavReconReport.ReconContact;
import com.wartec.wartecmod.port.uav.UavReconReport.SurveyCell;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

/** Readable field report shown directly by a landed reconnaissance UAV. */
public final class GuiUavReconReport extends GuiScreen {
    private int rowsPerPage = 6;
    private static final int SIDEBAR_WIDTH = 220;
    private static final int ROW_HEIGHT = 30;
    private final String title;
    private final UavReconReport report;
    private final List<ReconContact> contacts;
    private int left;
    private int top;
    private int panelWidth;
    private int panelHeight;
    private int page;
    private ReconContact selected;
    private Projection projection;

    public GuiUavReconReport(String title, UavReconReport report) {
        this.title = title == null || title.trim().isEmpty()
                ? com.wartec.wartecmod.port.uav.UavText.ui("UAV RECON REPORT")
                : (title.endsWith(" // RECON REPORT") ? title.substring(0,title.length()-16)
                        + com.wartec.wartecmod.port.uav.UavText.ui(" // RECON REPORT") : title);
        this.report = report == null ? new UavReconReport() : report.copy();
        contacts = new ArrayList<>(this.report.getContacts());
        Collections.sort(contacts, new Comparator<ReconContact>() {
            @Override
            public int compare(ReconContact first, ReconContact second) {
                int relation = Integer.compare(relationRank(first),
                        relationRank(second));
                if (relation != 0) return relation;
                int threat = Integer.compare(second.type, first.type);
                if (threat != 0) return threat;
                return Float.compare(second.quality, first.quality);
            }
        });
        if (!contacts.isEmpty()) selected = contacts.get(0);
    }

    @Override
    public void initGui() {
        panelWidth = Math.min(520, width - 18);
        panelHeight = Math.min(300, height - 18);
        rowsPerPage = Math.max(1, Math.min(6, (panelHeight - 142) / ROW_HEIGHT));
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        projection = Projection.create(report, left + 10, top + 43,
                Math.max(130, panelWidth - SIDEBAR_WIDTH - 20),
                panelHeight - 96);
        buttonList.clear();
        buttonList.add(new WarTechGuiButton(0, left + panelWidth - 69,
                top + panelHeight - 28, 58, 18, com.wartec.wartecmod.port.uav.UavText.ui("CLOSE")));
        buttonList.add(new WarTechGuiButton(1, left + panelWidth - 166,
                top + panelHeight - 28, 28, 18, "<"));
        buttonList.add(new WarTechGuiButton(2, left + panelWidth - 133,
                top + panelHeight - 28, 28, 18, ">"));
        updateButtons();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) {
            mc.displayGuiScreen(null);
        } else if (button.id == 1 && page > 0) {
            --page;
        } else if (button.id == 2
                && (page + 1) * rowsPerPage < contacts.size()) {
            ++page;
        }
        updateButtons();
    }

    private void updateButtons() {
        for (GuiButton button : buttonList) {
            if (button.id == 1) button.enabled = page > 0;
            if (button.id == 2) button.enabled =
                    (page + 1) * rowsPerPage < contacts.size();
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton)
            throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton != 0) return;
        int listLeft = left + panelWidth - SIDEBAR_WIDTH;
        int listTop = top + 100;
        if (mouseX >= listLeft && mouseX < left + panelWidth - 10
                && mouseY >= listTop
                && mouseY < listTop + rowsPerPage * ROW_HEIGHT) {
            int index = page * rowsPerPage
                    + (mouseY - listTop) / ROW_HEIGHT;
            if (index >= 0 && index < contacts.size()) {
                selected = contacts.get(index);
            }
        } else if (projection != null && projection.contains(mouseX, mouseY)) {
            selectNearestContact(mouseX, mouseY);
        }
    }

    private void selectNearestContact(int mouseX, int mouseY) {
        ReconContact nearest = null;
        int nearestDistance = 100;
        for (ReconContact contact : contacts) {
            int dx = projection.worldX(contact.x) - mouseX;
            int dy = projection.worldZ(contact.z) - mouseY;
            int distance = dx * dx + dy * dy;
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = contact;
            }
        }
        if (nearest != null) {
            selected = nearest;
            page = Math.max(0, contacts.indexOf(nearest) / rowsPerPage);
            updateButtons();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        Gui.drawRect(left, top, left + panelWidth, top + panelHeight,
                0xFF707672);
        Gui.drawRect(left + 2, top + 2, left + panelWidth - 2, top + 31,
                0xFF293338);
        Gui.drawRect(left + 10, top + 42,
                left + panelWidth - SIDEBAR_WIDTH - 10,
                top + panelHeight - 52, 0xFF11191C);
        Gui.drawRect(left + panelWidth - SIDEBAR_WIDTH, top + 42,
                left + panelWidth - 10, top + 92, 0xFF172126);
        Gui.drawRect(left + panelWidth - SIDEBAR_WIDTH, top + 99,
                left + panelWidth - 10, top + panelHeight - 39,
                0xFF172126);
        drawMap();
        drawHeader();
        drawSummary();
        drawContactList();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawHeader() {
        String cleanTitle = GuiTheme.clip(fontRenderer,title,panelWidth-110);
        fontRenderer.drawString(cleanTitle, left + 10, top + 11,
                0xE8F1EC);
        String dimension = report.hasDimension()
                ? com.wartec.wartecmod.port.uav.UavText.ui("DIM ") + report.getDimension() : com.wartec.wartecmod.port.uav.UavText.ui("DIM --");
        fontRenderer.drawString(dimension,
                left + panelWidth - 12 - fontRenderer.getStringWidth(dimension),
                top + 11, 0x7FC9A2);
    }

    private void drawSummary() {
        int hostile = 0;
        int friendly = 0;
        int neutral = 0;
        int unknown = 0;
        int aircraft = 0;
        int ground = 0;
        int missiles = 0;
        int personnel = 0;
        for (ReconContact contact : contacts) {
            if (contact.relation == ReconContact.FRIENDLY) ++friendly;
            else if (contact.relation == ReconContact.HOSTILE) ++hostile;
            else if (contact.relation == ReconContact.NEUTRAL) ++neutral;
            else ++unknown;
            if (contact.type == ReconContact.AIRCRAFT) ++aircraft;
            else if (contact.type == ReconContact.GROUND_VEHICLE) ++ground;
            else if (contact.type == ReconContact.MISSILE) ++missiles;
            else ++personnel;
        }
        int x = left + panelWidth - SIDEBAR_WIDTH + 7;
        GuiTheme.text(fontRenderer,com.wartec.wartecmod.port.uav.UavText.ui("SURVEY ") + report.getCellCount()
                        + com.wartec.wartecmod.port.uav.UavText.ui("   TRACKS ") + contacts.size(),
                x, top + 49, SIDEBAR_WIDTH-25,0xAEB9B3);
        GuiTheme.text(fontRenderer,com.wartec.wartecmod.port.uav.UavText.ui("THREATS ") + hostile, x, top + 60,100,
                hostile > 0 ? 0xFF8877 : 0xAEB9B3);
        GuiTheme.text(fontRenderer,com.wartec.wartecmod.port.uav.UavText.ui("FRIENDLY ") + friendly, x + 104, top + 60,90,
                0x66D99A);
        GuiTheme.text(fontRenderer,com.wartec.wartecmod.port.uav.UavText.ui("NEUTRAL ") + neutral, x, top + 71,100,
                0x75C5DA);
        GuiTheme.text(fontRenderer,com.wartec.wartecmod.port.uav.UavText.ui("UNKNOWN ") + unknown, x + 104, top + 71,90,
                0xC3C8C5);
        GuiTheme.text(fontRenderer,com.wartec.wartecmod.port.uav.UavText.ui("AIR ") + aircraft + com.wartec.wartecmod.port.uav.UavText.ui("  GND ") + ground
                        + com.wartec.wartecmod.port.uav.UavText.ui("  MSL ") + missiles + com.wartec.wartecmod.port.uav.UavText.ui("  PERS ") + personnel,
                x, top + 82, SIDEBAR_WIDTH-25,0x8F9B95);
    }

    private void drawContactList() {
        int start = page * rowsPerPage;
        int x = left + panelWidth - SIDEBAR_WIDTH + 7;
        int y = top + 103;
        if (contacts.isEmpty()) {
            GuiTheme.text(fontRenderer,com.wartec.wartecmod.port.uav.UavText.ui("NO CONTACTS RECORDED"), x, y,SIDEBAR_WIDTH-25,
                    0xAEB9B3);
            return;
        }
        for (int row = 0; row < rowsPerPage; ++row) {
            int index = start + row;
            if (index >= contacts.size()) break;
            ReconContact contact = contacts.get(index);
            int rowY = y + row * ROW_HEIGHT;
            if (contact == selected) {
                Gui.drawRect(x - 3, rowY - 2, left + panelWidth - 13,
                        rowY + 27, 0xFF344249);
            }
            int color = relationColor(contact);
            String name = fontRenderer.trimStringToWidth(contact.name, 145);
            fontRenderer.drawString(name, x, rowY, color);
            String quality = Math.round(contact.quality * 100.0F) + "%";
            fontRenderer.drawString(quality,
                    left + panelWidth - 15
                            - fontRenderer.getStringWidth(quality),
                    rowY, 0xD5DDD8);
            GuiTheme.text(fontRenderer,contactName(contact.type) + " // "
                            + relationName(contact.relation),
                    x, rowY + 10, SIDEBAR_WIDTH-25,0xAEB9B3);
            String coordinates = "XYZ " + contact.x + " / " + contact.y
                    + " / " + contact.z;
            GuiTheme.text(fontRenderer,coordinates, x, rowY + 20,SIDEBAR_WIDTH-25,0x89958F);
        }
        if (selected != null) {
            String selectedName = fontRenderer.trimStringToWidth(
                    com.wartec.wartecmod.port.uav.UavText.ui("SELECTED: ") + selected.name,
                    panelWidth - SIDEBAR_WIDTH - 30);
            fontRenderer.drawString(selectedName,
                    left + 12, top + panelHeight - 31,
                    relationColor(selected));
            GuiTheme.text(fontRenderer,contactName(selected.type) + " // "
                            + relationName(selected.relation) + " // XYZ "
                            + selected.x + " / " + selected.y + " / "
                            + selected.z,
                    left + 12, top + panelHeight - 20,panelWidth-SIDEBAR_WIDTH-30,0xE8F1EC);
        }
    }

    private void drawMap() {
        if (projection == null) {
            drawCenteredString(fontRenderer, com.wartec.wartecmod.port.uav.UavText.ui("NO TERRAIN DATA"),
                    left + (panelWidth - 170) / 2,
                    top + panelHeight / 2, 0xFFAA66);
            return;
        }
        for (SurveyCell cell : report.getCells()) {
            int x = projection.x(cell.x);
            int y = projection.y(cell.z);
            int size = Math.max(1, (int) Math.ceil(projection.scale));
            Gui.drawRect(x, y, Math.min(projection.right, x + size),
                    Math.min(projection.bottom, y + size),
                    shadeTerrain(cell.color, cell.height));
        }
        for (ReconContact contact : contacts) {
            int x = projection.worldX(contact.x);
            int y = projection.worldZ(contact.z);
            int color = relationColor(contact);
            int radius = contact == selected ? 4 : 2;
            Gui.drawRect(x - radius, y - 1, x + radius + 1, y + 2, color);
            Gui.drawRect(x - 1, y - radius, x + 2, y + radius + 1, color);
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
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

    private static String relationName(int relation) {
        if (relation == ReconContact.FRIENDLY) return com.wartec.wartecmod.port.uav.UavText.ui("FRIENDLY");
        if (relation == ReconContact.HOSTILE) return com.wartec.wartecmod.port.uav.UavText.ui("HOSTILE");
        if (relation == ReconContact.NEUTRAL) return com.wartec.wartecmod.port.uav.UavText.ui("NEUTRAL");
        return com.wartec.wartecmod.port.uav.UavText.ui("UNKNOWN IFF");
    }

    private static int relationRank(ReconContact contact) {
        if (contact.relation == ReconContact.HOSTILE) return 0;
        if (contact.relation == ReconContact.UNKNOWN) return 1;
        if (contact.relation == ReconContact.FRIENDLY) return 2;
        return 3;
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

    private static final class Projection {
        final int minX;
        final int minZ;
        final float scale;
        final float offsetX;
        final float offsetZ;
        final int left;
        final int top;
        final int right;
        final int bottom;

        Projection(int minX, int minZ, float scale, float offsetX,
                float offsetZ, int left, int top, int right, int bottom) {
            this.minX = minX;
            this.minZ = minZ;
            this.scale = scale;
            this.offsetX = offsetX;
            this.offsetZ = offsetZ;
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }

        static Projection create(UavReconReport report, int left, int top,
                int width, int height) {
            if (report.getCells().isEmpty()) return null;
            int minX = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int minZ = Integer.MAX_VALUE;
            int maxZ = Integer.MIN_VALUE;
            for (SurveyCell cell : report.getCells()) {
                minX = Math.min(minX, cell.x);
                maxX = Math.max(maxX, cell.x);
                minZ = Math.min(minZ, cell.z);
                maxZ = Math.max(maxZ, cell.z);
            }
            int spanX = Math.max(1, maxX - minX + 1);
            int spanZ = Math.max(1, maxZ - minZ + 1);
            float scale = Math.max(1.0F,
                    Math.min(width / (float) spanX, height / (float) spanZ));
            float offsetX = left + (width - spanX * scale) * 0.5F;
            float offsetZ = top + (height - spanZ * scale) * 0.5F;
            return new Projection(minX, minZ, scale, offsetX, offsetZ,
                    left, top, left + width, top + height);
        }

        int x(int cellX) {
            return Math.round(offsetX + (cellX - minX) * scale);
        }

        int y(int cellZ) {
            return Math.round(offsetZ + (cellZ - minZ) * scale);
        }

        int worldX(int blockX) { return x(Math.floorDiv(blockX, 16)); }
        int worldZ(int blockZ) { return y(Math.floorDiv(blockZ, 16)); }
        boolean contains(int x, int y) {
            return x >= left && x < right && y >= top && y < bottom;
        }
    }
}
