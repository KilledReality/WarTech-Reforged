package com.wartec.wartecmod.port.network;
import java.io.*;
import java.util.zip.*;
/** Bounded application compression; independent of optional Forge transport compression. */
public final class PlanningSnapshotCodec {
    public static final int RAW_LIMIT=4194304, WIRE_LIMIT=786432;
    private PlanningSnapshotCodec(){}
    public static byte[] pack(byte[] raw)throws IOException{
        if(raw.length<1 || raw.length>RAW_LIMIT)throw new IOException("snapshot raw size");
        ByteArrayOutputStream out=new ByteArrayOutputStream();try(GZIPOutputStream zip=new GZIPOutputStream(out)){zip.write(raw);}
        byte[] packed=out.toByteArray();if(packed.length>WIRE_LIMIT)throw new IOException("snapshot wire size");return packed;
    }
    public static byte[] unpack(byte[] packed,int expected)throws IOException{
        if(expected<1 || expected>RAW_LIMIT || packed.length<1 || packed.length>WIRE_LIMIT)throw new IOException("snapshot bounds");
        ByteArrayOutputStream out=new ByteArrayOutputStream(Math.min(expected,65536));byte[] buffer=new byte[8192];
        try(GZIPInputStream zip=new GZIPInputStream(new ByteArrayInputStream(packed))){int n;while((n=zip.read(buffer))!=-1){if(out.size()+n>expected)throw new IOException("snapshot expansion size");out.write(buffer,0,n);}}
        if(out.size()!=expected)throw new IOException("snapshot truncated");return out.toByteArray();
    }
}
