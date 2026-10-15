package com.wartec.wartecmod.port.network;

import java.util.*;

/** One bounded transaction per player/window. Used only on the server thread. */
public final class PlanningRequestAssembler {
    public static final int CHUNK=24576, MAX_BYTES=196608, MAX_PARTS=8;
    private java.lang.ref.WeakReference<Object> session=new java.lang.ref.WeakReference<>(null);
    private UUID pending;
    private long expires;
    private byte[] buffer;
    private BitSet received=new BitSet();
    private int parts,total;
    private final Set<UUID> completed=new LinkedHashSet<>();
    public static boolean valid(int index,int parts,int total,int length){
        return total>0 && total<=MAX_BYTES && parts==(total+CHUNK-1)/CHUNK && parts<=MAX_PARTS
            && index>=0 && index<parts && length==Math.min(CHUNK,total-index*CHUNK);
    }
    public byte[] accept(Object session,UUID id,int index,int parts,int total,byte[] bytes,long now){
        if(session==null || id==null || bytes==null || !valid(index,parts,total,bytes.length))return null;
        if(this.session.get()!=session){this.session=new java.lang.ref.WeakReference<>(session);completed.clear();clear();}
        if(completed.contains(id))return null;
        if(buffer!=null && now>=expires)clear();
        if(buffer==null){pending=id;this.parts=parts;this.total=total;buffer=new byte[total];expires=now+100;}
        if(!pending.equals(id) || this.parts!=parts || this.total!=total || received.get(index))return null;
        System.arraycopy(bytes,0,buffer,index*CHUNK,bytes.length);received.set(index);
        if(received.cardinality()!=parts)return null;
        byte[] result=buffer;completed.add(id);if(completed.size()>16)completed.remove(completed.iterator().next());clear();return result;
    }
    private void clear(){buffer=null;pending=null;received.clear();}
}
