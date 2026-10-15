package com.wartec.wartecmod.port;
import com.wartec.wartecmod.port.entity.*;
import com.wartec.wartecmod.port.network.*;
import io.netty.buffer.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class DefenseCommandTest {
    @Test public void worstCaseTraverseIsFastBoundedAndDoesNotOvershoot() {
        for(float rate:new float[]{AirDefenseTraverse.PATRIOT_YAW,AirDefenseTraverse.BUK_YAW,AirDefenseTraverse.OSA_YAW,AirDefenseTraverse.TOR_YAW,AirDefenseTraverse.PANTSIR_YAW}) {
            float yaw=0;int ticks=0;
            while(Math.abs(LegacySamGeometry.wrap(179-yaw))>.01 && ticks<100) {
                float next=LegacySamGeometry.approach(yaw,179,rate);
                assertTrue(Math.abs(LegacySamGeometry.wrap(next-yaw))<=rate+.0001);
                assertTrue(Math.abs(LegacySamGeometry.wrap(179-next))<=Math.abs(LegacySamGeometry.wrap(179-yaw)));yaw=next;ticks++;
            }
            assertTrue(ticks<=30);assertEquals(179,yaw,.001);
        }
    }
    @Test public void interpolationCrossesAngleWrapAlongTheShortArc() {
        assertEquals(180,AirDefenseTraverse.interpolate(179,-179,.5F),.001);
        assertEquals(-180,AirDefenseTraverse.interpolate(-179,179,.5F),.001);
        assertEquals(6,AirDefenseTraverse.interpolate(0,AirDefenseTraverse.PATRIOT_YAW,1),.001);
    }
    @Test public void shoradReactsBeforeThreatTraversesItsEngagementRange() {
        for(boolean tor:new boolean[]{true,false}) {
            ShoradAnimation a=new ShoradAnimation();a.deployment=120;a.slot=0;int ticks=0;
            while(!a.tick(true,179,tor?0:50,true,tor) && ticks++<100) { }
            assertTrue(ticks<=32);assertEquals(179,a.yaw,.01);assertEquals(12,a.cap);
            float yaw=a.yaw;a.released(5);
            for(int i=0;i<7;i++) { assertFalse(a.tick(true,-90,20,true,tor));assertEquals(yaw,a.yaw,0); }
        }
    }
    @Test public void northIsUpAndEastIsRightWithEqualBlockScale() {
        assertTrue(CommandMapViewport.pixelY(-100,0,256)<CommandMapViewport.HEIGHT*.5);
        assertTrue(CommandMapViewport.pixelX(100,0,256)>CommandMapViewport.WIDTH*.5);
        assertEquals(CommandMapViewport.pixelX(100,0,256)-160,CommandMapViewport.pixelY(100,0,256)-90,.001);
        for(double x:new double[]{-2000,-1,0,2000}) {
            assertEquals(x,CommandMapViewport.worldX(CommandMapViewport.pixelX(x,-321,128),-321,128),.001);
            assertEquals(x,CommandMapViewport.worldZ(CommandMapViewport.pixelY(x,-321,128),-321,128),.001);
        }
    }
    @Test public void fitShowsNearbyStationsWithoutCompressingThemIntoOnePixel() {
        FactionCommandSnapshot.Node[] nodes={new FactionCommandSnapshot.Node(3,1,1877,4,-32,2,0),new FactionCommandSnapshot.Node(1,2,1837,4,-72,250,0)};
        int radius=CommandMapViewport.RADII[CommandMapViewport.fit(nodes,1867,-42)];assertTrue(radius<=128);
        for(FactionCommandSnapshot.Node n:nodes) {assertTrue(CommandMapViewport.pixelX(n.x,1867,radius)>0);assertTrue(CommandMapViewport.pixelY(n.z,-42,radius)>0);}
    }
    @Test public void wirePreservesNamesStatusesAndRealMapCells() {
        int[] colors=new int[48*27];colors[0]=0xFF127EDD;colors[400]=0xFF62A142;
        FactionCommandSnapshot source=new FactionCommandSnapshot("blue",0,-123,-45,99,new FactionCommandSnapshot.Sector[0],
            new FactionCommandSnapshot.Node[]{new FactionCommandSnapshot.Node(3,987,-124,4,-46,2,0,"modern_buk_m3",250,6,1)},
            new FactionCommandSnapshot.Contact[]{new FactionCommandSnapshot.Contact(44,4,2,-100,200,-40,2,1,.8F,2,true,false)},
            new FactionCommandSnapshot.Terrain(-123,-45,128,colors));
        ByteBuf buffer=Unpooled.buffer();try {
            new FactionCommandSnapshotMessage(source).toBytes(buffer);assertTrue(buffer.readableBytes()<8000);
            FactionCommandSnapshotMessage decoded=new FactionCommandSnapshotMessage();decoded.fromBytes(buffer);FactionCommandSnapshot s=decoded.getSnapshot();
            assertEquals(0,buffer.readableBytes());assertEquals("modern_buk_m3",s.nodes[0].system);assertEquals(6,s.nodes[0].ammo);assertEquals(250,s.nodes[0].range);assertEquals(1,s.nodes[0].status);
            assertArrayEquals(colors,s.terrain.colors);assertEquals(-123,s.terrain.x,0);assertTrue(s.contacts[0].assigned);assertFalse(s.contacts[0].friendly);
        } finally {buffer.release();}
    }
    @Test(expected=IllegalArgumentException.class) public void oversizedCountsAreRejectedInsteadOfMisaligningThePacket() {
        ByteBuf b=Unpooled.buffer();try {b.writeShort(0);b.writeInt(0);b.writeInt(0);b.writeInt(0);b.writeLong(0);b.writeShort(257);new FactionCommandSnapshotMessage().fromBytes(b);} finally {b.release();}
    }
}
