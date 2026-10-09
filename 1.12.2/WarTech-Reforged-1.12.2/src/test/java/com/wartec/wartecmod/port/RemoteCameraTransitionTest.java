package com.wartec.wartecmod.port;

import com.wartec.wartecmod.port.client.RemoteCameraTransition;
import org.junit.Test;
import static org.junit.Assert.*;

public class RemoteCameraTransitionTest {
    @Test public void firstFrameDoesNotJumpUpward() {
        RemoteCameraTransition t=new RemoteCameraTransition();t.start(25,14);
        assertEquals(25,t.yaw(25),0);assertEquals(14,t.pitch(-30),0);
        float previous=14;
        for(int i=0;i<140;i++) {
            t.advance(.05);float next=t.pitch(-30);
            assertTrue(next<=previous+.0001);assertTrue(Math.abs(next-previous)<.5);
            previous=next;
        }
        assertEquals(-30,t.pitch(-30),.0001);
    }
    @Test public void crossesYawWrapOnShortestArc() {
        RemoteCameraTransition t=new RemoteCameraTransition();t.start(179,0);t.advance(3.5);
        assertEquals(180,t.yaw(-179),.0001);
    }
    @Test public void renderRateIndependentDuration() {
        RemoteCameraTransition a=new RemoteCameraTransition(),b=new RemoteCameraTransition();a.start(0,0);b.start(0,0);
        for(int i=0;i<60;i++) a.advance(.1);
        for(int i=0;i<12;i++) b.advance(.5);
        assertEquals(a.pitch(45),b.pitch(45),.0001);
        a.advance(1);assertFalse(a.active());assertEquals(45,a.pitch(45),0);
    }
    @Test public void rapidReverseStartsAtCurrentlyDisplayedAngle() {
        RemoteCameraTransition t=new RemoteCameraTransition();t.start(0,15);t.advance(2);
        float current=t.pitch(-20);t.start(0,current);assertEquals(current,t.pitch(15),0);
    }
    @Test public void invalidFrameTimeDoesNotCorruptView() {
        RemoteCameraTransition t=new RemoteCameraTransition();t.start(0,10);t.advance(Double.NaN);t.advance(-1);
        assertEquals(10,t.pitch(0),0);t.reset();assertEquals(0,t.pitch(0),0);
    }
}
