package com.wartec.wartecmod.port;

import com.wartec.wartecmod.port.cruise.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class CruiseLightAirframeTest {
    @Test public void lightAndHeavyDroneVisualFamiliesAreSeparate() {
        assertFalse(CruiseAirframes.usesStrizh(CruisePartDefinition.BODY_LIGHT));
        assertTrue(CruiseAirframes.usesStrizh(CruisePartDefinition.BODY_LONG_RANGE));
        assertEquals(1.10,CruiseAirframes.modelScale(CruisePartDefinition.BODY_LIGHT),1e-6);
        assertEquals(1.60,CruiseAirframes.modelScale(CruisePartDefinition.BODY_LONG_RANGE),1e-6);
    }
    @Test public void lightNoseAndExhaustMatchTheNewThreePointSixBlockMesh() {
        assertEquals(1.8*CruiseAirframes.modelScale(CruisePartDefinition.BODY_LIGHT),CruiseAirframes.noseOffset(CruisePartDefinition.BODY_LIGHT),0);
        assertEquals(1.86*CruiseAirframes.modelScale(CruisePartDefinition.BODY_LIGHT),CruiseAirframes.exhaustOffset(CruisePartDefinition.BODY_LIGHT),0);
        for(CruisePartDefinition body:new CruisePartDefinition[]{CruisePartDefinition.BODY_CLASSIC,CruisePartDefinition.BODY_HEAVY,CruisePartDefinition.BODY_LONG_RANGE}) {
            assertEquals(2.3*CruiseAirframes.modelScale(body),CruiseAirframes.noseOffset(body),0);
            assertEquals(2.4*CruiseAirframes.modelScale(body),CruiseAirframes.exhaustOffset(body),0);
        }
    }
}
