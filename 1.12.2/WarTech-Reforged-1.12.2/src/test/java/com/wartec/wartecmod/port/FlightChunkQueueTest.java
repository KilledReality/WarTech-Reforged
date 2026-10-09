package com.wartec.wartecmod.port;

import com.wartec.wartecmod.port.integration.FlightChunkQueue;
import com.wartec.wartecmod.port.integration.FlightChunkWindow;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.util.math.ChunkPos;
import org.junit.Test;
import static org.junit.Assert.*;

public class FlightChunkQueueTest {
    private static Set<ChunkPos> chunks(int start,int count) {
        Set<ChunkPos> out=new LinkedHashSet<>();for(int i=0;i<count;i++) out.add(new ChunkPos(start+i,0));return out;
    }
    private static class Access implements FlightChunkQueue.Access<Integer> {
        final Set<ChunkPos> loaded=new HashSet<>();final List<Integer> turns=new ArrayList<>();
        final AtomicLong clock;boolean existing=true;long cost;int checks;
        Access(AtomicLong clock) { this.clock=clock; }
        public boolean valid(Integer owner) { return owner>=0; }
        public boolean loaded(Integer owner,ChunkPos chunk) { return loaded.contains(chunk); }
        public boolean generated(Integer owner,ChunkPos chunk) { checks++;return existing; }
        public void load(Integer owner,ChunkPos chunk) { turns.add(owner);loaded.add(chunk);clock.addAndGet(cost); }
    }
    @Test public void globallyFourLoadsNotFourPerEntityOrDimension() {
        AtomicLong clock=new AtomicLong();FlightChunkQueue<Integer> q=new FlightChunkQueue<>(clock::get);
        Access a=new Access(clock);for(int i=0;i<32;i++) q.request(i,chunks(i*100,10));
        assertEquals(4,q.drain(a).loads);assertEquals(Arrays.asList(0,1,2,3),a.turns);
    }
    @Test public void repeatedEntityRequestsCannotJumpTheQueue() {
        AtomicLong clock=new AtomicLong();FlightChunkQueue<Integer> q=new FlightChunkQueue<>(clock::get);Access a=new Access(clock);
        for(int tick=0;tick<8;tick++) {
            for(int i=0;i<32;i++) q.request(i,chunks(i*100,10));
            q.drain(a);
        }
        assertEquals(32,a.turns.size());for(int i=0;i<32;i++) assertEquals(i,a.turns.get(i).intValue());
    }
    @Test public void atMostOneUnknownTerrainLoadPerServerTick() {
        AtomicLong clock=new AtomicLong();FlightChunkQueue<Integer> q=new FlightChunkQueue<>(clock::get);Access a=new Access(clock);a.existing=false;
        for(int i=0;i<16;i++) q.request(i,chunks(i*100,4));
        for(int i=0;i<16;i++) { FlightChunkQueue.Result r=q.drain(a);assertEquals(1,r.loads);assertEquals(1,r.generated); }
        assertEquals(16,new HashSet<>(a.turns).size());
    }
    @Test public void slowChunkStopsFurtherIoRatherThanMultiplyingTheSpike() {
        AtomicLong clock=new AtomicLong();FlightChunkQueue<Integer> q=new FlightChunkQueue<>(clock::get);Access a=new Access(clock);a.cost=150_000_000;
        q.request(1,chunks(0,9));FlightChunkQueue.Result r=q.drain(a);
        assertEquals(1,r.loads);assertEquals(150_000_000,r.nanos);assertEquals(8,r.pending);
    }
    @Test public void budgetStopsBeforeAnotherDiskRead() {
        AtomicLong clock=new AtomicLong();FlightChunkQueue<Integer> q=new FlightChunkQueue<>(clock::get);Access a=new Access(clock);a.cost=4_000_000;
        q.request(1,chunks(0,9));assertEquals(2,q.drain(a).loads);
    }
    @Test public void alreadyLoadedOrSharedChunksDoNotSpendIoBudget() {
        AtomicLong clock=new AtomicLong();FlightChunkQueue<Integer> q=new FlightChunkQueue<>(clock::get);Access a=new Access(clock);
        for(int i=0;i<16;i++) q.request(i,chunks(0,1));
        FlightChunkQueue.Result r=q.drain(a);assertEquals(1,r.loads);assertEquals(0,q.owners());assertEquals(0,q.pending());
    }
    @Test public void removalCancellationAndWorldUnloadLeaveNoQueuedReferences() {
        FlightChunkQueue<Integer> q=new FlightChunkQueue<>(()->0);q.request(1,chunks(0,9));q.request(2,chunks(100,9));
        q.remove(1);assertEquals(9,q.pending());q.request(2,Collections.emptySet());assertEquals(0,q.owners());
        q.request(3,chunks(0,9));q.clear();assertEquals(0,q.pending());
    }
    @Test public void invalidDeadOwnerCannotLoadTerrain() {
        AtomicLong clock=new AtomicLong();FlightChunkQueue<Integer> q=new FlightChunkQueue<>(clock::get);Access a=new Access(clock);
        q.request(-1,chunks(0,25));assertEquals(0,q.drain(a).loads);assertEquals(0,q.owners());assertEquals(0,a.checks);
    }
    @Test public void turnReplacesOldCorridorAndDoesNotAccumulateTheRoute() {
        FlightChunkQueue<Integer> q=new FlightChunkQueue<>(()->0);
        for(int tick=0;tick<10000;tick++) {
            q.request(1,FlightChunkWindow.at(tick*3,-tick*2,tick%2==0?2:-2,1));
            assertTrue(q.pending()<=25);assertEquals(1,q.owners());
        }
    }
    @Test public void sixteenFlightsFinishFourteenThousandBlocksWithBoundedWindows() {
        AtomicLong clock=new AtomicLong();FlightChunkQueue<Integer> q=new FlightChunkQueue<>(clock::get);
        Access a=new Access(clock);a.existing=false;a.cost=1000;
        double[] x=new double[16];int finished=0,tick=0;int[] advanced=new int[16];
        for(;tick<100000 && finished<16;tick++) {
            Set<ChunkPos> retained=new HashSet<>();finished=0;
            for(int i=0;i<16;i++) {
                if(Math.abs(x[i])>=14000) { finished++;q.remove(i);continue; }
                double vx=i%2==0?2:-2;
                Set<ChunkPos> window=FlightChunkWindow.at(x[i],i*96,vx,0);retained.addAll(window);
                Set<ChunkPos> missing=new LinkedHashSet<>(window);missing.removeAll(a.loaded);q.request(i,missing);
                if(missing.isEmpty()) { x[i]+=vx;advanced[i]++; }
            }
            a.loaded.retainAll(retained);FlightChunkQueue.Result r=q.drain(a);
            assertTrue(r.loads<=4);assertTrue(r.generated<=1);assertTrue(q.pending()<=16*25);assertTrue(a.loaded.size()<=16*25);
        }
        assertEquals(16,finished);assertEquals(0,q.pending());
        for(int n:advanced) assertEquals(7000,n);
        System.out.println("DEV65 synthetic 16 x 14000 blocks: "+tick+" scheduler ticks, "+a.turns.size()+" bounded loads; not Minecraft TPS");
    }
}
