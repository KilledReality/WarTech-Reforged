package com.wartec.wartecmod.port;
import com.wartec.wartecmod.port.integration.FlightTickMetrics;
import org.junit.Test;
import static org.junit.Assert.*;
public class FlightTickMetricsTest {
    @Test public void emptyAndInvalidSamplesAreSafe() {
        FlightTickMetrics m=new FlightTickMetrics();m.record(-1);
        assertEquals(0,m.count());assertEquals(0,m.meanMillis(),0);assertEquals(0,m.p95Millis(),0);
    }
    @Test public void meanAndPercentileIncludeSlowChunkIo() {
        FlightTickMetrics m=new FlightTickMetrics();for(int i=0;i<90;i++) m.record(1_000_000);
        for(int i=0;i<10;i++) m.record(151_000_000);
        assertEquals(16,m.meanMillis(),1e-9);assertEquals(151,m.p95Millis(),0);
    }
    @Test public void onlyLastHundredTicksSurviveAndClearResetsSession() {
        FlightTickMetrics m=new FlightTickMetrics();for(int i=0;i<110;i++) m.record(i*1_000_000L);
        assertEquals(100,m.count());assertEquals(59.5,m.meanMillis(),1e-9);assertEquals(104,m.p95Millis(),0);
        m.clear();assertEquals(0,m.count());assertEquals(0,m.meanMillis(),0);
    }
}
