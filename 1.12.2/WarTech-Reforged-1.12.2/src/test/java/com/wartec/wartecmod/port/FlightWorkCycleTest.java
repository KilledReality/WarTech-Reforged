package com.wartec.wartecmod.port;
import com.wartec.wartecmod.port.integration.FlightWorkCycle;
import org.junit.Test;
import static org.junit.Assert.*;
public class FlightWorkCycleTest {
    @Test public void scanFrequencyIsUnchangedForAllEntityPhases() {
        for(int id=-100;id<100;id++) for(int period:new int[]{5,10,20,80}) {
            int count=0;for(int tick=0;tick<800;tick++) if(FlightWorkCycle.due(tick,id,period)) count++;
            assertEquals(800/period,count);
        }
    }
    @Test public void sixteenMissilesNoLongerScanOnTheSameTick() {
        for(int tick=0;tick<10;tick++) {
            int count=0;for(int id=0;id<16;id++) if(FlightWorkCycle.due(tick,id,5)) count++;
            assertTrue(count>=3 && count<=4);
        }
    }
    @Test public void thermalDecoyChecksStillCoincideWithSeekerScan() {
        for(int id=0;id<100;id++) for(int tick=0;tick<100;tick++)
            if(FlightWorkCycle.due(tick,id,20)) assertTrue(FlightWorkCycle.due(tick,id,5));
    }
    @Test public void negativeAndOverflowIdsDoNotBreakScheduling() {
        assertTrue(FlightWorkCycle.due(Integer.MAX_VALUE,Integer.MAX_VALUE,2));
        assertTrue(FlightWorkCycle.due(Integer.MIN_VALUE,Integer.MIN_VALUE,2));
        assertFalse(FlightWorkCycle.due(0,0,0));
    }
}
