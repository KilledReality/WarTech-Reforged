package com.wartec.wartecmod.port.gui;

/** Short-valued Forge window properties. Initial full state, then per-field deltas. */
public final class WindowPropertySync {
    private final int[] last;
    private boolean initialized;
    public WindowPropertySync(int count) { last = new int[count]; }
    public void send(int[] values, java.util.function.BiConsumer<Integer, Integer> sink, boolean full) {
        if (values.length != last.length) throw new IllegalArgumentException("property count");
        for (int i=0; i<last.length; i++)
            if (full || !initialized || last[i] != (values[i] & 65535)) sink.accept(i, values[i] & 65535);
    }
    public void remember(int[] values) {
        if (values.length != last.length) throw new IllegalArgumentException("property count");
        for (int i=0; i<last.length; i++) last[i] = values[i] & 65535;
        initialized = true;
    }
}
