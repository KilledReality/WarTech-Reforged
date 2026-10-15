package com.wartec.wartecmod.port;
import com.wartec.wartecmod.port.network.PlanningRequestAssembler;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class PlanningRequestAssemblerTest {
    @Test public void outOfOrderFragmentsReconstructExactlyOnce(){PlanningRequestAssembler a=new PlanningRequestAssembler();Object session=new Object();UUID id=UUID.randomUUID();byte[] first=new byte[24576],last={3,4,5};Arrays.fill(first,(byte)9);assertNull(a.accept(session,id,1,2,24579,last,0));assertNull(a.accept(session,id,1,2,24579,last,1));byte[] result=a.accept(session,id,0,2,24579,first,2);assertEquals(24579,result.length);assertEquals(9,result[0]);assertEquals(5,result[24578]);assertNull(a.accept(session,id,0,2,24579,first,3));assertNull(a.accept(session,id,1,2,24579,last,3));}
    @Test public void budgetsAndPartLengthsAreEnforced(){assertFalse(PlanningRequestAssembler.valid(0,9,196609,24576));assertFalse(PlanningRequestAssembler.valid(-1,1,1,1));assertFalse(PlanningRequestAssembler.valid(0,2,1,1));assertFalse(PlanningRequestAssembler.valid(0,1,1,0));assertTrue(PlanningRequestAssembler.valid(7,8,196608,24576));}
    @Test public void expiryDiscardsPreviousFragments(){PlanningRequestAssembler a=new PlanningRequestAssembler();Object s=new Object();UUID id=UUID.randomUUID();assertNull(a.accept(s,id,0,2,24577,new byte[24576],0));assertNull(a.accept(s,id,1,2,24577,new byte[1],101));assertNotNull(a.accept(s,id,0,2,24577,new byte[24576],102));}
    @Test public void newContainerCannotCompleteOldContainerRequest(){PlanningRequestAssembler a=new PlanningRequestAssembler();UUID id=UUID.randomUUID();assertNull(a.accept(new Object(),id,0,2,24577,new byte[24576],0));assertNull(a.accept(new Object(),id,1,2,24577,new byte[1],1));}
    @Test public void interleavedTransactionCannotReplacePendingMemory(){PlanningRequestAssembler a=new PlanningRequestAssembler();Object s=new Object();UUID id=UUID.randomUUID();assertNull(a.accept(s,id,0,2,24577,new byte[24576],0));assertNull(a.accept(s,UUID.randomUUID(),0,2,24577,new byte[24576],1));assertNotNull(a.accept(s,id,1,2,24577,new byte[1],2));}
}
