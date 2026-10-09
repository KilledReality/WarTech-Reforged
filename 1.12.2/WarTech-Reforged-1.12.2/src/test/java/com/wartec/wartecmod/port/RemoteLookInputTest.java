package com.wartec.wartecmod.port;

import com.wartec.wartecmod.port.client.RemoteLookInput;
import org.junit.Test;
import static org.junit.Assert.*;

public class RemoteLookInputTest {
    @Test public void networkCorrectionBeforeFrameIsNotInput() {
        RemoteLookInput input=new RemoteLookInput();Object player=new Object();
        for(int i=0;i<1000;i++) {
            float networkPitch=i%2==0?-65:35,networkYaw=i*47%360;
            input.begin(player,networkYaw,networkPitch,true);
            assertTrue(input.consume(player,networkYaw,networkPitch));
            assertEquals(0,input.pitchDelta(),0);assertEquals(0,input.yawDelta(),0);
        }
    }
    @Test public void realMouseAfterCorrectionIsPreserved() {
        RemoteLookInput input=new RemoteLookInput();Object player=new Object();
        input.begin(player,155,-55,true);assertTrue(input.consume(player,158,-53));
        assertEquals(3,input.yawDelta(),0);assertEquals(2,input.pitchDelta(),0);
    }
    @Test public void frameCannotBeConsumedTwice() {
        RemoteLookInput input=new RemoteLookInput();Object player=new Object();
        input.begin(player,0,0,true);assertTrue(input.consume(player,2,-3));
        assertFalse(input.consume(player,2,-3));assertEquals(0,input.pitchDelta(),0);
    }
    @Test public void respawnedPlayerIsNotMouseMovement() {
        RemoteLookInput input=new RemoteLookInput();input.begin(new Object(),0,0,true);
        assertFalse(input.consume(new Object(),40,-80));
    }
    @Test public void menusAndLostFocusAreNotInput() {
        RemoteLookInput input=new RemoteLookInput();Object player=new Object();
        input.begin(player,0,0,false);assertFalse(input.consume(player,80,-80));
    }
    @Test public void angleWrapPreservesSmallMovement() {
        RemoteLookInput input=new RemoteLookInput();Object player=new Object();
        input.begin(player,179,0,true);assertTrue(input.consume(player,-179,0));
        assertEquals(2,input.yawDelta(),0);
    }
    @Test public void noInputWithoutMatchingStartAndNoInvalidAngles() {
        RemoteLookInput input=new RemoteLookInput();Object player=new Object();
        assertFalse(input.consume(player,0,-50));input.begin(player,0,0,true);
        assertFalse(input.consume(player,Float.NaN,0));input.begin(player,0,0,true);input.reset();
        assertFalse(input.consume(player,0,-50));input.begin(player,0,Float.POSITIVE_INFINITY,true);
        assertFalse(input.consume(player,0,-50));
    }
}
