package com.wartec.wartecmod.port;

import com.wartec.wartecmod.WarTechReforged;
import java.util.*;
import net.minecraftforge.fml.relauncher.Side;
import org.junit.Test;
import static org.junit.Assert.*;

public class MultiplayerProtocolTest {
    @Test public void matchingClientAndServerAreAccepted() {
        WarTechReforged mod=new WarTechReforged();
        for(Side side:Side.values())assertTrue(mod.compatibleNetwork(Collections.singletonMap("wartecmod",WarTechReforged.VERSION),side));
    }
    @Test public void oldBuildWithSameHistoricalReleaseNumberIsRejected() {
        assertFalse(new WarTechReforged().compatibleNetwork(Collections.singletonMap("wartecmod","1.6.0-experimental"),Side.CLIENT));
    }
    @Test public void missingModCannotJoinDedicatedServer() {
        assertFalse(new WarTechReforged().compatibleNetwork(Collections.emptyMap(),Side.CLIENT));
    }
    @Test public void differentDevelopmentProtocolIsRejectedInBothDirections() {
        for(Side side:Side.values())assertFalse(new WarTechReforged().compatibleNetwork(Collections.singletonMap("wartecmod","1.6.0-mp-dev114"),side));
    }
}
