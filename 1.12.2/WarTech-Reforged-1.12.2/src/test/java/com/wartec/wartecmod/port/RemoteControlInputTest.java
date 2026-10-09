package com.wartec.wartecmod.port;

import com.wartec.wartecmod.port.network.RemoteControlInputMessage;
import io.netty.buffer.Unpooled;
import org.junit.Test;
import static org.junit.Assert.*;

public class RemoteControlInputTest {
    @Test public void finiteControlsAccepted() {
        assertTrue(new RemoteControlInputMessage(7,179,-35,-179,32,1,0).hasFiniteControls());
    }
    @Test public void invalidControlsRejectedInEveryField() {
        for(float bad:new float[]{Float.NaN,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY}) {
            assertFalse(new RemoteControlInputMessage(7,bad,0,0,0,1,0).hasFiniteControls());
            assertFalse(new RemoteControlInputMessage(7,0,bad,0,0,1,0).hasFiniteControls());
            assertFalse(new RemoteControlInputMessage(7,0,0,bad,0,1,0).hasFiniteControls());
            assertFalse(new RemoteControlInputMessage(7,0,0,0,bad,1,0).hasFiniteControls());
            assertFalse(new RemoteControlInputMessage(7,0,0,0,0,bad,0).hasFiniteControls());
        }
    }
    @Test public void wireRoundTripRetainsValidInput() {
        io.netty.buffer.ByteBuf wire=Unpooled.buffer();
        try {
            new RemoteControlInputMessage(7,-45,-18,-45,-18,.72F,0x42).toBytes(wire);
            RemoteControlInputMessage restored=new RemoteControlInputMessage();restored.fromBytes(wire);
            assertTrue(restored.hasFiniteControls());assertEquals(0,wire.readableBytes());
        } finally { wire.release(); }
    }
    @Test public void wireRoundTripCannotHideNan() {
        io.netty.buffer.ByteBuf wire=Unpooled.buffer();
        try {
            new RemoteControlInputMessage(7,Float.NaN,0,0,0,1,0).toBytes(wire);
            RemoteControlInputMessage restored=new RemoteControlInputMessage();restored.fromBytes(wire);
            assertFalse(restored.hasFiniteControls());
        } finally { wire.release(); }
    }
}
