package com.wartec.wartecmod.port;
import com.wartec.wartecmod.port.network.PlanningSnapshotCodec;
import java.io.IOException;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class PlanningSnapshotCodecTest {
    @Test public void repeatedTelemetryLargerThanVanillaPayloadRoundTrips()throws Exception{byte[] raw=new byte[1200000];for(int i=0;i<raw.length;i++)raw[i]=(byte)(i%251);byte[] packed=PlanningSnapshotCodec.pack(raw);assertTrue(packed.length<32767);assertArrayEquals(raw,PlanningSnapshotCodec.unpack(packed,raw.length));}
    @Test(expected=IOException.class) public void decompressionCannotExceedDeclaredBudget()throws Exception{byte[] packed=PlanningSnapshotCodec.pack(new byte[1200000]);PlanningSnapshotCodec.unpack(packed,100);}
    @Test(expected=IOException.class) public void incompleteDataIsRejected()throws Exception{PlanningSnapshotCodec.unpack(PlanningSnapshotCodec.pack(new byte[100]),101);}
    @Test(expected=IOException.class) public void oversizedIncompressibleDataIsRejected()throws Exception{byte[] raw=new byte[900000];new Random(123).nextBytes(raw);PlanningSnapshotCodec.pack(raw);}
}
