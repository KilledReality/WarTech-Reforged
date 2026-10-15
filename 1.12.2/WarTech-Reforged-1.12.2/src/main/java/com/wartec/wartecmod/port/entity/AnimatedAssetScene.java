package com.wartec.wartecmod.port.entity;

import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;

/** Rigid GLB animation data; pure math shared with asset and socket verification. */
public final class AnimatedAssetScene {
    public Node[] nodes;
    public int[] roots;
    public Part[][] meshes;
    public Map<String, Track[]> clips;
    public static final class Node {
        public String name;
        public int mesh = -1;
        public int[] children = new int[0];
        public float[] translation = {0,0,0}, rotation = {0,0,0,1}, scale = {1,1,1};
        public float[] weights = new float[0];
    }
    public static final class Part {
        public String group,texture,alphaMode;
        public boolean doubleSided;
        public Morph morph;
    }
    public static final class Morph {
        public float[][] positions,normals,uvs;
        public int[] indices;
        public float[][][] positionDeltas,normalDeltas;
    }
    public static final class Track {
        public int node;
        public String path;
        public float[] times;
        public float[][] values;
        public boolean step;
        public float[] sample(float time) {
            if(time<=times[0]) return values[0].clone();
            int last=times.length-1;
            if(time>=times[last]) return values[last].clone();
            int index=Arrays.binarySearch(times,time);
            if(index>=0) return values[index].clone();
            int hi=-index-1,lo=hi-1;
            float t=step?0:(time-times[lo])/(times[hi]-times[lo]);
            float[] a=values[lo],b=values[hi],result=new float[a.length];
            float sign=1;
            if("rotation".equals(path)) {
                float dot=0;for(int i=0;i<4;i++) dot+=a[i]*b[i];
                if(dot<0) sign=-1;
            }
            float length=0;
            for(int i=0;i<a.length;i++) { result[i]=a[i]+(b[i]*sign-a[i])*t;length+=result[i]*result[i]; }
            if("rotation".equals(path)) {
                float inverse=1/(float)Math.sqrt(Math.max(1e-12,length));
                for(int i=0;i<4;i++) result[i]*=inverse;
            }
            return result;
        }
    }
    public Pose pose() { return new Pose(this); }
    public static final class Pose {
        private final AnimatedAssetScene scene;
        public final float[][] translation,rotation,scale,weights;
        Pose(AnimatedAssetScene scene) {
            this.scene=scene;int count=scene.nodes.length;
            translation=new float[count][];rotation=new float[count][];scale=new float[count][];weights=new float[count][];
            for(int i=0;i<count;i++) {
                translation[i]=scene.nodes[i].translation.clone();rotation[i]=scene.nodes[i].rotation.clone();scale[i]=scene.nodes[i].scale.clone();
                weights[i]=scene.nodes[i].weights.clone();
            }
        }
        /** Filter partial clips so exported rest-pose tracks cannot reset another subsystem. */
        public void apply(String clip,float seconds,java.util.function.Predicate<Node> filter) {
            Track[] tracks=scene.clips.get(clip);if(tracks==null) return;
            for(Track track:tracks) if(filter.test(scene.nodes[track.node])) {
                float[] value=track.sample(seconds);
                if("translation".equals(track.path)) translation[track.node]=value;
                else if("rotation".equals(track.path)) rotation[track.node]=value;
                else if("scale".equals(track.path)) scale[track.node]=value;
                else if("weights".equals(track.path)) weights[track.node]=value;
            }
        }
        public float[] matrix(int node) { return compose(translation[node],rotation[node],scale[node]); }
        public float[] worldMatrix(int node) {
            float[] local=matrix(node);
            for(int p=0;p<scene.nodes.length;p++) for(int child:scene.nodes[p].children)
                if(child==node) return multiply(worldMatrix(p),local);
            return local;
        }
    }
    public static float[] compose(float[] t,float[] q,float[] s) {
        float x=q[0],y=q[1],z=q[2],w=q[3];
        return new float[]{(1-2*y*y-2*z*z)*s[0],(2*x*y+2*z*w)*s[0],(2*x*z-2*y*w)*s[0],0,
            (2*x*y-2*z*w)*s[1],(1-2*x*x-2*z*z)*s[1],(2*y*z+2*x*w)*s[1],0,
            (2*x*z+2*y*w)*s[2],(2*y*z-2*x*w)*s[2],(1-2*x*x-2*y*y)*s[2],0,t[0],t[1],t[2],1};
    }
    public static float[] multiply(float[] a,float[] b) {
        float[] c=new float[16];
        for(int col=0;col<4;col++) for(int row=0;row<4;row++) for(int k=0;k<4;k++) c[col*4+row]+=a[k*4+row]*b[col*4+k];
        return c;
    }
}
