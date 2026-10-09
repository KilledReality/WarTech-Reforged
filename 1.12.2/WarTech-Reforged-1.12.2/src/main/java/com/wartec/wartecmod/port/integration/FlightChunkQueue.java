package com.wartec.wartecmod.port.integration;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.LongSupplier;
import net.minecraft.util.math.ChunkPos;

/** Server-thread round robin. One load per owner per turn, with a shared time budget. */
public final class FlightChunkQueue<K> {
    public static final int MAX_LOADS = 4, MAX_NEW_CHUNKS = 1;
    public static final long MAX_LOAD_NANOS = 8_000_000L;
    public interface Access<K> {
        boolean valid(K owner);
        boolean loaded(K owner, ChunkPos chunk);
        boolean generated(K owner, ChunkPos chunk);
        void load(K owner, ChunkPos chunk);
    }
    public static final class Result {
        public int loads, generated, pending;
        public long nanos;
    }
    private final LinkedHashMap<K, Set<ChunkPos>> requests = new LinkedHashMap<>();
    private final LongSupplier clock;
    public FlightChunkQueue() { this(System::nanoTime); }
    public FlightChunkQueue(LongSupplier clock) { this.clock = clock; }
    public void request(K owner, Set<ChunkPos> chunks) {
        if (chunks.isEmpty()) requests.remove(owner);
        else requests.put(owner, new LinkedHashSet<>(chunks));
    }
    public void remove(K owner) { requests.remove(owner); }
    public void clear() { requests.clear(); }
    public int owners() { return requests.size(); }
    public int pending() { int total = 0; for (Set<ChunkPos> chunks : requests.values()) total += chunks.size(); return total; }

    public Result drain(Access<K> access) {
        Result result = new Result();
        long start = clock.getAsLong();
        int idleTurns = 0;
        while (!requests.isEmpty() && result.loads < MAX_LOADS
                && clock.getAsLong() - start < MAX_LOAD_NANOS && idleTurns < requests.size()) {
            Iterator<Map.Entry<K, Set<ChunkPos>>> entries = requests.entrySet().iterator();
            Map.Entry<K, Set<ChunkPos>> entry = entries.next();
            K owner = entry.getKey(); Set<ChunkPos> chunks = entry.getValue(); entries.remove();
            if (!access.valid(owner)) { idleTurns = 0; continue; }
            boolean attempted = false;
            for (Iterator<ChunkPos> it = chunks.iterator(); it.hasNext();) {
                if (clock.getAsLong() - start >= MAX_LOAD_NANOS) break;
                ChunkPos chunk = it.next();
                if (access.loaded(owner, chunk)) { it.remove(); continue; }
                boolean existing = access.generated(owner, chunk);
                if (!existing && result.generated >= MAX_NEW_CHUNKS) continue;
                // Check again: disk existence checks also cost time.
                if (clock.getAsLong() - start >= MAX_LOAD_NANOS) break;
                result.loads++; if (!existing) result.generated++;
                access.load(owner, chunk);
                if (access.loaded(owner, chunk)) it.remove();
                attempted = true; break;
            }
            if (!chunks.isEmpty()) requests.put(owner, chunks);
            idleTurns = attempted || chunks.isEmpty() ? 0 : idleTurns + 1;
        }
        result.nanos = Math.max(0, clock.getAsLong() - start); result.pending = pending();
        return result;
    }
}
