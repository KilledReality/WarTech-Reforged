package com.wartec.wartecmod.port;

import com.wartec.wartecmod.port.client.ItemPreviewKey;
import com.wartec.wartecmod.port.gui.WindowPropertySync;
import com.wartec.wartecmod.port.integration.StrikeBlastSafety;
import java.util.*;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.Test;
import static org.junit.Assert.*;

public class InterfacePerformanceTest {
    @Test public void decimalLabelsKeepPrecisionEvenWithoutLoadedLanguageResources() throws Exception {
        java.lang.reflect.Method method=com.wartec.wartecmod.port.client.GuiUavFabricator.class.getDeclaredMethod("decimalTemplate",String.class,int.class);
        method.setAccessible(true);
        String speed=(String)method.invoke(null,"TOP SPEED: %.1f blocks/sec",1);
        String thrust=(String)method.invoke(null,"THRUST/MASS: %.2f",2);
        assertTrue(String.format(Locale.US,speed,25.9602489).contains("26.0"));
        assertTrue(String.format(Locale.US,thrust,.2533333333).contains("0.25"));
    }
    @Test public void unownedOrEnemyWeaponsAreNeverImplicitlyAllied() {
        assertFalse(StrikeBlastSafety.allied(null,"",null,""));
        assertFalse(StrikeBlastSafety.allied(new UUID(1,2),"red",new UUID(3,4),"blue"));
        assertFalse(StrikeBlastSafety.allied(null,"",null,"blue"));
    }
    @Test public void explicitOwnerAndNonemptyTeamAreRecognized() {
        UUID owner=new UUID(1,2);
        assertTrue(StrikeBlastSafety.allied(owner,"",owner,""));
        assertTrue(StrikeBlastSafety.allied(owner,"blue",new UUID(3,4),"blue"));
    }
    @Test public void initialStateIncludesZeroValuesThenIdleSendsNothing() {
        WindowPropertySync sync=new WindowPropertySync(46);int[] values=new int[46];
        List<Integer> ids=new ArrayList<>();sync.send(values,(id,value)->ids.add(id),true);
        assertEquals(46,ids.size());sync.remember(values);ids.clear();
        for(int i=0;i<200;i++) sync.send(values,(id,value)->ids.add(id),false);
        assertTrue(ids.isEmpty());
    }
    @Test public void changingPowerDoesNotResendThirtyTwoRadarWords() {
        WindowPropertySync sync=new WindowPropertySync(46);int[] values=new int[46];sync.remember(values);
        values[0]=65535;List<Integer> ids=new ArrayList<>();sync.send(values,(id,value)->ids.add(id),false);
        assertEquals(Collections.singletonList(0),ids);
    }
    @Test public void signedShortAliasesDoNotCauseRepeatedPackets() {
        WindowPropertySync sync=new WindowPropertySync(1);sync.remember(new int[]{-1});
        sync.send(new int[]{65535},(id,value)->fail("Same wire value"),false);
    }
    @Test public void highPowerWordsAndPackedNegativeBlipsRoundTrip() {
        int power=123456789,blip=(-500<<16)|(-735&65535);
        int[] sent={power&65535,power>>>16,blip&65535,blip>>>16};
        WindowPropertySync sync=new WindowPropertySync(4);int[] received=new int[4];
        sync.send(sent,(id,value)->received[id]=(short)(int)value,true);
        assertEquals(power,(received[0]&65535)|(received[1]&65535)<<16);
        assertEquals(blip,(received[2]&65535)|(received[3]&65535)<<16);
    }
    @Test public void allListenersSeeTheSameDeltaAndNewListenersGetAFullSnapshot() {
        WindowPropertySync sync=new WindowPropertySync(40);int[] values=new int[40];sync.remember(values);values[7]=3;
        List<Integer> a=new ArrayList<>(),b=new ArrayList<>(),newcomer=new ArrayList<>();
        sync.send(values,(id,value)->a.add(id),false);sync.send(values,(id,value)->b.add(id),false);
        sync.remember(values);sync.send(values,(id,value)->newcomer.add(id),true);
        assertEquals(Collections.singletonList(7),a);assertEquals(a,b);assertEquals(40,newcomer.size());
    }
    @Test public void cacheSeparatesItemVariants() {
        assertNotEquals(ItemPreviewKey.of("mq9payload",0,null),ItemPreviewKey.of("mq9payload",1,null));
        assertNotEquals(ItemPreviewKey.of("stormshadow",0,null),ItemPreviewKey.of("assembleduav",0,null));
    }
    private NBTTagCompound cruise(String body) {
        NBTTagCompound root=new NBTTagCompound(),build=new NBTTagCompound();build.setString("BODY",body);root.setTag("CruiseBuild",build);return root;
    }
    @Test public void everyCruiseBodyHasItsOwnCacheEntry() {
        Set<String> keys=new HashSet<>();for(String body:new String[]{"body_light","body_classic","body_heavy","body_heavy_simple"})
            assertTrue(keys.add(ItemPreviewKey.of("assembledcruise",0,cruise(body))));
    }
    @Test public void renamingOrRetargetingAMissileDoesNotMeasureAllVerticesAgain() {
        NBTTagCompound root=cruise("body_heavy");String key=ItemPreviewKey.of("assembledcruise",0,root);
        root.getCompoundTag("CruiseBuild").setString("Name","Different name");root.setString("Mission","other target");
        assertEquals(key,ItemPreviewKey.of("assembledcruise",0,root));
        root.getCompoundTag("CruiseBuild").setString("BODY","body_light");
        assertNotEquals(key,ItemPreviewKey.of("assembledcruise",0,root));
    }
    @Test public void uavFrameAndPropellerAreGeometryKeysButNotCharge() {
        NBTTagCompound root=new NBTTagCompound(),build=new NBTTagCompound(),parts=new NBTTagCompound();
        parts.setString("AIRFRAME","frame_recon");parts.setString("PROPULSION","engine_economy");build.setTag("Parts",parts);root.setTag("WarTechUavBuild",build);
        String key=ItemPreviewKey.of("assembleduav",0,root);root.setInteger("Energy",10);
        assertEquals(key,ItemPreviewKey.of("assembleduav",0,root));parts.setString("PROPULSION","engine_heavy");
        assertNotEquals(key,ItemPreviewKey.of("assembleduav",0,root));
    }
}
