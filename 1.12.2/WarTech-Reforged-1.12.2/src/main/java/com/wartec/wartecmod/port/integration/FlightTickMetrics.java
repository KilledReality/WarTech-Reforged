package com.wartec.wartecmod.port.integration;

import java.util.Arrays;

/** Rolling active-tick span, including END chunk I/O excluded by vanilla's timer. */
public final class FlightTickMetrics {
    private final long[] samples=new long[100];
    private int next,count;
    public void record(long nanos) {
        if(nanos<0) return;
        samples[next]=nanos;next=(next+1)%samples.length;count=Math.min(samples.length,count+1);
    }
    public int count() { return count; }
    public double meanMillis() {
        double total=0;for(int i=0;i<count;i++) total+=samples[i];return count==0?0:total/count/1e6;
    }
    public double p95Millis() {
        if(count==0) return 0;
        long[] sorted=Arrays.copyOf(samples,count);Arrays.sort(sorted);
        return sorted[(int)Math.ceil(count*.95)-1]/1e6;
    }
    public void clear() { next=count=0;Arrays.fill(samples,0); }
}
