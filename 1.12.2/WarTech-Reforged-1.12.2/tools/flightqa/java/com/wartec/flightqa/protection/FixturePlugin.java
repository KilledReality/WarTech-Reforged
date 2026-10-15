package com.wartec.flightqa.protection;
import java.util.Map;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.SortingIndex(1001)
@IFMLLoadingPlugin.TransformerExclusions({"com.wartec.flightqa.protection"})
public final class FixturePlugin implements IFMLLoadingPlugin {
    public String[] getASMTransformerClass(){return new String[]{"com.wartec.flightqa.protection.FixtureTransformer"};}
    public String getModContainerClass(){return null;}
    public String getSetupClass(){return null;}
    public void injectData(Map<String,Object> data){}
    public String getAccessTransformerClass(){return null;}
}
