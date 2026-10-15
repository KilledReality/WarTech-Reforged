package com.wartec.wartecmod.port.network;
import io.netty.buffer.*;
import java.lang.reflect.Field;
import org.junit.Test;
import static org.junit.Assert.*;
public class TelTargetMessageTest {
    private Object get(TelTargetMessage message,String name) throws Exception {
        Field f=TelTargetMessage.class.getDeclaredField(name);f.setAccessible(true);return f.get(message);
    }
    private TelTargetMessage roundTrip(TelTargetMessage sent) {
        ByteBuf buffer=Unpooled.buffer();try { sent.toBytes(buffer);TelTargetMessage received=new TelTargetMessage();received.fromBytes(buffer);assertEquals(0,buffer.readableBytes());return received; } finally { buffer.release(); }
    }
    @Test public void manualCoordinatesRetainTheirTubeAndSignedWorldPosition() throws Exception {
        for(int slot=0;slot<2;slot++) {
            TelTargetMessage received=roundTrip(new TelTargetMessage(350,slot,-29999984,255,29999984));
            assertEquals(350,get(received,"entityId"));assertEquals(slot,get(received,"slot"));
            assertEquals(-29999984,get(received,"x"));assertEquals(255,get(received,"y"));assertEquals(29999984,get(received,"z"));assertEquals(false,get(received,"importDesignator"));
        }
    }
    @Test public void importUsesServerDesignatorAndRetainsTubeSelection() throws Exception {
        for(int slot=0;slot<2;slot++) {
            TelTargetMessage received=roundTrip(TelTargetMessage.fromDesignator(1024,slot));
            assertEquals(1024,get(received,"entityId"));assertEquals(slot,get(received,"slot"));assertEquals(true,get(received,"importDesignator"));
        }
    }
}
