package com.wartec.wartecmod.port.client;

import java.lang.reflect.*;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Actual optimized parser, no GL context or model/resource rewrite. */
public class LegacyObjParserTest {
    @Test public void vectorsPreserveExponentNegativeWhitespaceAndUvValues() throws Exception {
        Method parse=LegacyObjModel.class.getDeclaredMethod("parseVector",String.class,int.class);parse.setAccessible(true);
        assertArrayEquals(new float[]{-1.25F,300,.5F},(float[])parse.invoke(null,"v -1.25\t3e2  .5",3),0);
        assertArrayEquals(new float[]{.125F,.875F},(float[])parse.invoke(null,"vt .125 .875",2),0);
    }
    private Object face(String line) throws Exception {
        Class<?> mesh=Class.forName(LegacyObjModel.class.getName()+"$Mesh");Constructor<?> ctor=mesh.getDeclaredConstructor();ctor.setAccessible(true);Object value=ctor.newInstance();
        for(String field:new String[]{"vertices","textures","normals"}) {
            Field member=mesh.getDeclaredField(field);member.setAccessible(true);List<float[]> list=(List<float[]>)member.get(value);
            list.add(new float[]{0,0,0});list.add(new float[]{1,0,0});list.add(new float[]{0,1,0});
        }
        Method parse=LegacyObjModel.class.getDeclaredMethod("parseFace",String.class,mesh);parse.setAccessible(true);return parse.invoke(null,line,value);
    }
    private int[] indices(Object face,String field) throws Exception { Field f=face.getClass().getDeclaredField(field);f.setAccessible(true);return (int[])f.get(face); }
    @Test public void fullAndNegativeFaceIndicesRemainIdentical() throws Exception {
        Object f=face("1/1/1\t2/2/2  -1/-1/-1");
        for(String kind:new String[]{"vertices","textures","normals"}) assertArrayEquals(new int[]{0,1,2},indices(f,kind));
    }
    @Test public void missingUvOrNormalAndDoubleSlashAreSupported() throws Exception {
        Object f=face("1//1 2/2 3");assertArrayEquals(new int[]{-1,1,-1},indices(f,"textures"));
        assertArrayEquals(new int[]{0,-1,-1},indices(f,"normals"));
    }
    @Test public void polygonsKeepAllCornersForUnchangedFanTriangulation() throws Exception {
        assertArrayEquals(new int[]{0,1,2,0},indices(face("1 2 3 1"),"vertices"));
    }
}
