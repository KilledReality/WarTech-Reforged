package com.wartec.wartecmod.port;
import com.google.gson.*;
import com.wartec.wartecmod.port.cruise.*;
import com.wartec.wartecmod.port.entity.*;
import com.wartec.wartecmod.port.uav.*;
import net.minecraft.util.math.Vec3d;
import org.junit.Test;
public class AircraftMountAuditTest {
    private final JsonArray rows=new JsonArray();
    private void add(String carrier,int code,int slot,Vec3d point,double attach,double top,String mesh,double scale,int sign,double lift) {
        JsonObject o=new JsonObject();o.addProperty("carrier",carrier);o.addProperty("code",code);o.addProperty("slot",slot);
        JsonArray v=new JsonArray();v.add(point.x);v.add(point.y);v.add(point.z);o.add("point",v);
        o.addProperty("attach",attach);o.addProperty("body_top",top);o.addProperty("mesh",mesh);o.addProperty("scale",scale);
        o.addProperty("sign",sign);o.addProperty("lift",lift);rows.add(o);
    }
    @Test public void exportEveryCarrierStoreForFullMeshAudit() throws Exception {
        for(WarTechEntityProfile profile:new WarTechEntityProfile[]{WarTechEntityProfile.SU_27,WarTechEntityProfile.F_16C,WarTechEntityProfile.TU_95})
            for(CruisePartDefinition body:CruiseAirframes.bodies()) for(int slot=0;slot<CruiseAircraftLoadout.capacity(profile,body);slot++) {
                CruiseBuild b=CruiseBuild.starter(body);
                String file=body==CruisePartDefinition.BODY_LIGHT?"lastvika_modular.obj":body==CruisePartDefinition.BODY_CLASSIC?"neptune_rocket.obj":
                    body==CruisePartDefinition.BODY_LONG_RANGE?"strizh_modular.obj":"storm_modular.obj";
                add(profile.name(),body.ordinal()+100,slot,CruiseAircraftLoadout.mount(profile,b,slot),CruiseAircraftLoadout.attachY(profile,b,slot),
                    CruiseAircraftLoadout.storeBodyTop(b),"custom_cruise/"+file,CruiseAirframes.modelScale(body),0,0);
            }
        for(UavAirframe f:new UavAirframe[]{UavAirframe.RECON,UavAirframe.STRIKE})
            for(CruisePartDefinition b:new CruisePartDefinition[]{CruisePartDefinition.BODY_LIGHT,CruisePartDefinition.BODY_CLASSIC}) {
                if(f==UavAirframe.RECON && b==CruisePartDefinition.BODY_CLASSIC) continue;
                CruiseBuild build=CruiseBuild.starter(b);
                add("UAV_"+f.name(),b.ordinal()+100,0,new Vec3d(0,UavCruiseCarriage.mountY(f,build),0),UavCruiseCarriage.attachY(f),
                    UavCruiseCarriage.storeTop(build),"custom_cruise/"+(b==CruisePartDefinition.BODY_LIGHT?"lastvika_modular.obj":"neptune_rocket.obj"),CruiseAirframes.modelScale(b),0,0);
            }
        for(int code=10;code<=12;code++) for(int slot=0;slot<6;slot++)
            add("TU_95",code,slot,VehicleDimensions.tuStore(code,slot),VehicleDimensions.tuAttachY(code,slot),VehicleDimensions.tuStoreBodyTop(code),
                code==10?"strategic/kh555.obj":code==11?"mq9/mk82_bomb.obj":"tactical/kab500l.obj",code==10?1.1:code==11?5.1*1.15:3.4*1.15,code==10?0:1,code==10?-.35*1.1:0);
        String[] names={"tactical/agm114_hellfire.obj","tactical/gbu12_paveway.obj","mq9/mk82_bomb.obj","tactical/hj10.obj",
            "tactical/agm65_maverick.obj","tactical/kh29.obj","tactical/kab500l.obj","tactical/jdam.obj","tactical/hj10.obj"};
        double[] scales={1.2,1.45,3,1.3,1.55,1.85,1.6,1.55,1.45};
        for(int type=0;type<9;type++) {
            for(WarTechEntityProfile profile:new WarTechEntityProfile[]{WarTechEntityProfile.SU_27,WarTechEntityProfile.F_16C,WarTechEntityProfile.MQ_9_REAPER})
                for(int slot=0;slot<(profile==WarTechEntityProfile.F_16C?4:6);slot++)
                    add(profile.name(),type+1,slot,AircraftStores.mount(profile,type,slot),AircraftStores.anchor(profile,slot).y,
                        AircraftStores.bodyTop(type),names[type],scales[type]*1.15,1,0);
            for(int slot=0;slot<4;slot++)
                add("UAV_STRIKE",type+1,slot,VehicleDimensions.uavStore(UavAirframe.STRIKE,type,slot),VehicleDimensions.uavWingY(UavAirframe.STRIKE,slot),
                    AircraftStores.bodyTop(type),names[type],scales[type]*1.15,1,0);
        }
        java.nio.file.Path out=java.nio.file.Paths.get("build/qa/aircraft-mounts-dev66.json");java.nio.file.Files.createDirectories(out.getParent());
        java.nio.file.Files.write(out,new GsonBuilder().setPrettyPrinting().create().toJson(rows).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
