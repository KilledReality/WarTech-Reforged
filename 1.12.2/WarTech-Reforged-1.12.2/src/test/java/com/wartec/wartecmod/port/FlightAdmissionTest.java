package com.wartec.wartecmod.port;
import com.wartec.wartecmod.port.integration.FlightAdmission;
import org.junit.Test;
import static org.junit.Assert.*;
public class FlightAdmissionTest {
    @Test public void saturatedAttackPoolLeavesDefenseCapacity() {
        for(int defense=0;defense<24;defense++) {
            assertTrue(FlightAdmission.accepts(24+defense,24,true));
            assertFalse(FlightAdmission.accepts(24+defense,24,false));
        }
        assertFalse(FlightAdmission.accepts(48,24,true));
    }
    @Test public void defenseSaturationDoesNotConsumeAttackAllocation() {
        for(int attacks=0;attacks<24;attacks++) {
            assertTrue(FlightAdmission.accepts(24+attacks,attacks,false));
            assertFalse(FlightAdmission.accepts(24+attacks,attacks,true));
        }
    }
    @Test public void mixedFlightOrderCannotExceedEitherPool() {
        for(int seed=0;seed<100;seed++) {
            java.util.Random random=new java.util.Random(seed);int total=0,attacks=0;
            for(int i=0;i<200;i++) {
                boolean defense=random.nextBoolean();
                if(FlightAdmission.accepts(total,attacks,defense)){total++;if(!defense)attacks++;}
                assertTrue(total<=48);assertTrue(attacks<=24);assertTrue(total-attacks<=24);
            }
            assertEquals(48,total);assertEquals(24,attacks);
        }
    }
    @Test public void releasedFlightRestoresOnlyItsPool() {
        assertTrue(FlightAdmission.accepts(47,24,true));assertFalse(FlightAdmission.accepts(47,24,false));
        assertTrue(FlightAdmission.accepts(47,23,false));assertFalse(FlightAdmission.accepts(47,23,true));
    }
    @Test public void corruptAccountingCannotAdmitFlight() {
        assertFalse(FlightAdmission.accepts(-1,0,true));assertFalse(FlightAdmission.accepts(1,-1,false));
        assertFalse(FlightAdmission.accepts(1,2,true));assertFalse(FlightAdmission.accepts(49,24,false));
    }
}
