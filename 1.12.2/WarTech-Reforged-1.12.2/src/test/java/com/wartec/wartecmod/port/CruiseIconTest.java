package com.wartec.wartecmod.port;

import com.google.gson.*;
import com.wartec.wartecmod.port.cruise.CruisePartDefinition;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import javax.imageio.ImageIO;
import org.junit.Test;
import static org.junit.Assert.*;

/** Inventory-only artwork; no changes to flight or world model bindings. */
public class CruiseIconTest {
    private static final Path ROOT=Paths.get("src/main/resources/assets/wartecmod");
    private JsonObject model(String name) throws Exception {
        return new JsonParser().parse(new String(Files.readAllBytes(ROOT.resolve("models/item/"+name+".json")),StandardCharsets.UTF_8)).getAsJsonObject();
    }
    private void icon(String name,String texture) throws Exception {
        JsonObject m=model(name);assertEquals("item/generated",m.get("parent").getAsString());
        assertEquals("wartecmod:items/"+texture,m.getAsJsonObject("textures").get("layer0").getAsString());
        BufferedImage image=ImageIO.read(ROOT.resolve("textures/items/"+texture+".png").toFile());
        assertNotNull(image);assertEquals(256,image.getWidth());assertEquals(256,image.getHeight());assertTrue(image.getColorModel().hasAlpha());
        int visible=0;
        for(int y=0;y<256;y++) for(int x=0;x<256;x++) {
            int alpha=image.getRGB(x,y)>>>24;
            if(alpha>=20) { visible++;assertTrue("Artwork touches icon edge: "+texture,x>=8 && y>=8 && x<248 && y<248); }
        }
        assertTrue("Empty artwork: "+texture,visible>100);
    }
    @Test public void suppliedComponentsUseTheirOwnPng() throws Exception {
        int count=0;
        for(CruisePartDefinition part:CruisePartDefinition.values()) {
            if(part==CruisePartDefinition.SEEKER_RADAR) continue;
            icon("cruisemodule_"+part.getId(),"cruise_parts/"+part.getId());count++;
        }assertEquals(43,count);
    }
    @Test public void serviceItemsAndBlueprintPredicateUseNewArt() throws Exception {
        icon("cruisefabricator","cruise_parts/cruise_constructor");
        icon("cruiselaunchpoint","cruise_parts/cruise_launch_point");
        icon("cruisedronerail","cruise_parts/cruise_drone_rail");
        icon("cruiseblueprint","cruise_parts/cruise_blueprint_blank");
        icon("cruiseblueprint_saved","cruise_parts/cruise_blueprint_saved");
        JsonObject override=model("cruiseblueprint").getAsJsonArray("overrides").get(0).getAsJsonObject();
        assertEquals(1,override.getAsJsonObject("predicate").get("wartecmod:saved").getAsInt());
        assertEquals("wartecmod:item/cruiseblueprint_saved",override.get("model").getAsString());
    }
    @Test public void optionalRackHasInventoryArt() throws Exception { icon("uavmodule_rack_cruise","uav_parts/rack_cruise"); }
    @Test public void missingRadarIconKeepsResolvableFallback() throws Exception {
        JsonObject m=model("cruisemodule_seeker_radar");assertEquals("item/generated",m.get("parent").getAsString());
        String texture=m.getAsJsonObject("textures").get("layer0").getAsString();assertTrue(texture.startsWith("wartecmod:"));
        assertTrue(Files.isRegularFile(ROOT.resolve("textures/"+texture.substring("wartecmod:".length())+".png")));
    }
}
