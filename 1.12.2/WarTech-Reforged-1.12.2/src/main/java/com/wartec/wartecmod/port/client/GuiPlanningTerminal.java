package com.wartec.wartecmod.port.client;

import com.wartec.wartecmod.port.entity.EntityWarTechBase;
import com.wartec.wartecmod.port.gameplay.TileEntityPlanningTerminal;
import com.wartec.wartecmod.port.gui.ContainerPlanningTerminal;
import com.wartec.wartecmod.port.network.*;
import com.wartec.wartecmod.port.planning.*;
import java.io.IOException;
import java.util.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.*;

/** One server session, three distinct information sources and an editable operation timeline. */
public final class GuiPlanningTerminal extends GuiContainer {
    private final ContainerPlanningTerminal terminal;
    private final CommandCenterWorkspace workspace=new CommandCenterWorkspace(this);
    private final AttackPlanWizard wizard=new AttackPlanWizard(this);
    private NBTTagCompound snapshot=new NBTTagCompound();
    private AttackOperation operation=new AttackOperation();
    private AttackOperation.Step edited;
    private final List<GuiTextField> fields=new ArrayList<>();
    private GuiTextField search;
    private int tab,ticks,listScroll,stepScroll,selectedStep=-1,assetPage,logPage,requestSerial,acceptedSerial,pendingTicks;
    private int leftWidth,rightWidth,mapX,mapY,mapW,mapH,bottomY;
    private String selectedAsset="",selectedIntel="",status="",pendingAction="";
    private boolean dirty,awaitingSave,dragging,awaitingSelection;
    private int panel,operationPage,editorPage,allyPage;
    private GuiTextField panelName;
    private long importedSerial=-1;
    private boolean coverage=true;
    private String allyTeam="";
    private double viewX,viewZ,dragViewX,dragViewZ;
    private int radius=512,dragX,dragY,targetX,targetY=64,targetZ;
    private boolean targetSet;
    private static final int BG=0xFF101B20,LINE=0xFF344B54;
    public GuiPlanningTerminal(EntityPlayer player,TileEntityPlanningTerminal tile,EntityWarTechBase command){
        super(new ContainerPlanningTerminal(player,tile,command));terminal=(ContainerPlanningTerminal)inventorySlots;
        viewX=terminal.x();viewZ=terminal.z();operation.title=tr("new_operation");operation.scope=terminal.scope;operation.author=player.getUniqueID();
    }
    private static String tr(String key,Object... args){return I18n.format("planning."+key,args);}
    private static String number(double n){return String.format(Locale.ROOT,"%.1f",n);}
    @Override public void initGui(){
        xSize=Math.min(1100,width-12);ySize=Math.min(650,height-12);super.initGui();
        leftWidth=xSize<440?Math.max(72,xSize/4):xSize<750?122:166;rightWidth=xSize<440?Math.max(104,xSize/3):xSize<750?170:215;
        mapX=leftWidth+16;mapY=59;mapW=xSize-leftWidth-rightWidth-38;bottomY=ySize-Math.min(170,Math.max(112,ySize/3));mapH=bottomY-mapY-27;
        search=new GuiTextField(0,fontRenderer,guiLeft+10,guiTop+62,leftWidth-8,17);search.setMaxStringLength(64);rebuild();
        org.lwjgl.input.Keyboard.enableRepeatEvents(true);
    }
    private int editorY(int original){if(ySize<380){if(original==68)return 56;return 78+(int)Math.round((original-112)*(ySize-130)/248D);}if(original==68)return 68;return 56+(int)Math.round((original-68)*Math.min(1,(ySize-118)/320D));}
    private void button(int id,int x,int y,int w,String label){buttonList.add(new PlanningButton(id,guiLeft+x,guiTop+y,w,edited!=null && ySize<250 && id>=50 && id<=56?14:18,label));}
    private void rebuild(){
        buttonList.clear();fields.clear();
        if(panel!=0){panelControls();return;}if(edited!=null){editorControls();return;}
        int tw=Math.min(155,(xSize-144)/4);for(int i=0;i<3;i++)button(i,10+i*(tw+4),29,tw,tr("tab."+i));
        button(46,10+3*(tw+4),29,tw,tr("workspace"));
        button(10,mapX, bottomY-22,24,"+");button(11,mapX+28,bottomY-22,24,"-");button(12,mapX+56,bottomY-22,Math.min(100,mapW-56),tr("center"));
        button(13,xSize-rightWidth-10,bottomY-22,rightWidth,tr("copy_target"));
        button(14,10,bottomY-22,26,"<");button(15,40,bottomY-22,26,">");
        if(tab==1){
            String[] labels={"new","save","validate","start","pause","resume","cancel","copy","delete_operation"};int[] controls=operation.state==AttackOperation.State.DRAFT?new int[]{0,1,2,3,7,8}:new int[]{4,5,6,7,8};int bw=(xSize-20-2*(controls.length-1))/controls.length,y=ySize-25;for(int i=0;i<controls.length;i++)button(30+controls[i],10+i*(bw+2),y,bw,tr(labels[controls[i]]));
            int titleWidth=Math.min(205,(xSize-96)/2),stepWidth=Math.min(62,(xSize-titleWidth-84)/4);
            button(20,10,bottomY+4,titleWidth,tr("operations"));
            String[] steps={"add","edit","duplicate","delete_step"};for(int i=0;i<4;i++)button(21+i,titleWidth+16+i*(stepWidth+2),bottomY+4,stepWidth,tr(steps[i]));
            if(selectedStep>=0 && selectedStep<operation.steps.size() && operation.steps.get(selectedStep).phase==AttackOperation.Phase.UNCERTAIN)button(43,xSize-rightWidth-10,bottomY-43,rightWidth,tr("resolve_step"));
            button(25,xSize-62,bottomY+4,22,"↑");button(26,xSize-36,bottomY+4,22,"↓");
        }else if(tab==0){button(44,xSize-rightWidth-10,bottomY-43,rightWidth,tr("allies"));button(40,xSize-69,ySize-25,25,"<");button(41,xSize-39,ySize-25,25,">");}
        else button(42,10,ySize-25,Math.min(260,xSize-20),tr("use_intel"));
        button(45,xSize-112,29,102,tr(coverage?"coverage_on":"coverage_off"));
        if(terminal.command!=null){button(90,xSize-rightWidth-10,bottomY-85,rightWidth,tr("drive"));button(91,xSize-rightWidth-10,bottomY-64,rightWidth,tr(terminal.command.isDeployed()?"stow":"deploy"));}
        if(tab==1)button(27,xSize-rightWidth-10,bottomY-43,rightWidth,tr(wizard.mapPicking?"wizard.continue":"wizard.open"));
    }
    public void accept(NBTTagCompound value){
        if(value.getInteger("Window")!=inventorySlots.windowId || !terminal.scope.equals(value.getString("Scope")))return;
        int serial=value.getInteger("Serial");if(serial<acceptedSerial)return;acceptedSerial=serial;
        snapshot=value;workspace.accept();String error=value.getString("Error");
        if(panel==5)wizard.refresh();
        NBTTagCompound wizardTarget=value.getCompoundTag("ImportedTarget");if(wizard.active && wizardTarget.hasUniqueId("Step") && wizard.importId.equals(wizardTarget.getUniqueId("Step")) && wizardTarget.getLong("Serial")>importedSerial){importedSerial=wizardTarget.getLong("Serial");wizard.imported(wizardTarget);panel=5;panelControls();}
        NBTTagCompound imported=value.getCompoundTag("ImportedTarget");if(edited!=null && imported.hasUniqueId("Step") && edited.id.equals(imported.getUniqueId("Step")) && imported.getLong("Serial")>importedSerial){
            importedSerial=imported.getLong("Serial");edited.x=imported.getInteger("X");edited.y=imported.getInteger("Y");edited.z=imported.getInteger("Z");panel=0;editorPage=0;status=tr("target_imported");editorControls();
        }
        if(!error.isEmpty()){status=tr("error."+error);awaitingSave=false;pendingAction="";}
        else if(awaitingSave && "save".equals(value.getString("ResponseAction"))){awaitingSave=false;dirty=false;status=tr("saved");pendingTicks=5;}
        if(edited==null && !wizard.active && !dirty && !awaitingSave && value.hasKey("Operation",10) && (operation.id.equals(value.getCompoundTag("Operation").getUniqueId("Id")) || "copy".equals(value.getString("ResponseAction")))){operation=AttackOperation.read(value.getCompoundTag("Operation"));awaitingSelection=false;selectedStep=Math.min(selectedStep,operation.steps.size()-1);}
        if(error.isEmpty() && "validate".equals(value.getString("ResponseAction")))status=tr("validated");
        if(error.isEmpty() && "start".equals(value.getString("ResponseAction")))status=tr("started");
        if(error.isEmpty() && ("ally_add".equals(value.getString("ResponseAction")) || "ally_remove".equals(value.getString("ResponseAction"))))status=tr("allies_saved");
        if("delete".equals(value.getString("ResponseAction")) && error.isEmpty()){operation=new AttackOperation();operation.scope=terminal.scope;operation.author=mc.player.getUniqueID();operation.title=tr("new_operation");selectedStep=-1;dirty=false;}
        if(edited==null && panel==0)rebuild();
    }
    private NBTTagCompound createRequest(String action){
        NBTTagCompound n=new NBTTagCompound();n.setString("Action",action);n.setInteger("Serial",++requestSerial);n.setDouble("X",viewX);n.setDouble("Z",viewZ);n.setInteger("Radius",radius);n.setDouble("Aspect",mapW>0?(double)mapH/mapW:.5625);n.setInteger("Tab",tab);n.setInteger("Page",assetPage);n.setInteger("LogPage",logPage);n.setUniqueId("Selected",operation.id);
        if("ally_add".equals(action) || "ally_remove".equals(action))n.setString("Team",allyTeam);
        if("import_target".equals(action)){if(wizard.active)n.setUniqueId("StepId",wizard.importId);else if(edited!=null)n.setUniqueId("StepId",edited.id);}
        if(!"fetch".equals(action)){n.setInteger("Revision",operation.revision);n.setUniqueId("OperationId",operation.id);if("resolve_step".equals(action) && selectedStep>=0)n.setUniqueId("StepId",operation.steps.get(selectedStep).id);if("save".equals(action)){n.setTag("Operation",operation.write());awaitingSave=true;}}
        n.setBoolean("Coverage",coverage && tab==0 && panel==0 && !selectedAsset.isEmpty());n.setString("CoverageAsset",selectedAsset);workspace.extend(n,panel==4);return n;
    }
    private void request(String action){if(!PlanningRequestMessage.send(inventorySlots.windowId,createRequest(action))){awaitingSave=false;pendingAction="";status=I18n.format("planning.error.limits");}ticks=0;
    }
    @Override public void updateScreen(){
        super.updateScreen();if(search!=null)search.updateCursorCounter();if(panelName!=null)panelName.updateCursorCounter();for(GuiTextField f:fields)f.updateCursorCounter();
        workspace.tick();wizard.tick();if(terminal.command!=null)VehiclePanelControls.update(buttonList,terminal.command);
        if(pendingTicks>0 && --pendingTicks==0 && !pendingAction.isEmpty()){String action=pendingAction;pendingAction="";request(action);}
        if(++ticks>=20 && !awaitingSave)request("fetch");
    }
    private List<NBTTagCompound> rows(){
        NBTTagList list=tab==2?snapshot.getCompoundTag("Intel").getTagList("Contacts",10):snapshot.getTagList("Assets",10);
        List<NBTTagCompound> result=new ArrayList<>();String query=search==null?"":search.getText().toLowerCase(Locale.ROOT);
        for(int i=0;i<list.tagCount();i++){NBTTagCompound n=list.getCompoundTagAt(i);if(query.isEmpty() || n.getCompoundTag("CommandMeta").getString("Group").toLowerCase(Locale.ROOT).contains(query) || name(n).toLowerCase(Locale.ROOT).contains(query) || n.getString("Kind").contains(query))result.add(n);}return result;
    }
    private static String readable(String text){return text.replace((char)0xAB,'"').replace((char)0xBB,'"');}
    private String name(NBTTagCompound n){if(!n.getCompoundTag("CommandMeta").getString("Title").isEmpty())return readable(n.getCompoundTag("CommandMeta").getString("Title"));String raw=n.getString("Name");if(I18n.hasKey(raw))return readable(I18n.format(raw));String key="planning.unit."+raw;return readable(I18n.hasKey(key)?I18n.format(key):raw.replace('_',' '));}
    private NBTTagCompound selected(){String key=tab==2?selectedIntel:selectedAsset;for(NBTTagCompound n:rows())if(key.equals(n.getString("Key")))return n;return new NBTTagCompound();}
    @Override protected void actionPerformed(GuiButton b)throws IOException{
        if(panel!=0){panelAction(b.id);return;}
        if(b.id==90 || b.id==91){mc.playerController.sendEnchantPacket(inventorySlots.windowId,b.id);return;}
        if(b.id==46){panel=4;panelControls();request("fetch");return;}
        if(b.id==45){coverage=!coverage;rebuild();return;}
        if(b.id==44){panel=2;panelControls();return;}
        if(edited!=null){editorAction(b.id);return;}
        if(awaitingSelection || awaitingSave){status=tr("waiting_server");return;}
        if(b.id<3){tab=b.id;listScroll=0;status="";rebuild();request("fetch");return;}
        if(b.id==10)radius=Math.max(64,radius/2);if(b.id==11)radius=Math.min(16384,radius*2);
        if(b.id==12){viewX=terminal.x();viewZ=terminal.z();}
        if(b.id>=10 && b.id<=12){request("fetch");return;}
        if(b.id==13 && targetSet){GuiScreen.setClipboardString(targetX+" "+targetY+" "+targetZ);status=tr("copied");}
        if(b.id==14 || b.id==15){assetPage=Math.max(0,Math.min(Math.max(0,(snapshot.getInteger("Total")-1)/256),assetPage+(b.id==14?-1:1)));listScroll=0;request("fetch");}
        if(b.id==20){panel=1;operationPage=0;panelControls();return;}
        if(b.id==27){if(operation.state!=AttackOperation.State.DRAFT){status=tr("draft_only");return;}wizard.open(selectedAsset,targetX,targetY,targetZ,targetSet);panel=5;panelControls();return;}
        if(b.id==21){if(operation.state!=AttackOperation.State.DRAFT){status=tr("draft_only");return;}NBTTagCompound asset=selected();if(asset.getString("Key").isEmpty()){status=tr("select_asset");return;}
            edited=new AttackOperation.Step();edited.asset=asset.getString("Key");edited.x=targetSet?targetX:(int)viewX;edited.y=targetSet?targetY:64;edited.z=targetSet?targetZ:(int)viewZ;
            String[] actions=asset.getString("Actions").split(",");if(actions.length>0)try{edited.action=AttackOperation.Action.valueOf(actions[0]);}catch(IllegalArgumentException ignored){}
            for(int i=operation.steps.size()-1;i>=0;i--)if(edited.asset.equals(operation.steps.get(i).asset)){edited.after=operation.steps.get(i).id;edited.event=AttackOperation.Event.COMPLETE;break;}
            selectedStep=operation.steps.size();status="";editorPage=0;editorControls();return;}
        if(b.id==22 && selectedStep>=0 && selectedStep<operation.steps.size() && operation.state==AttackOperation.State.DRAFT){edited=AttackOperation.Step.read(operation.steps.get(selectedStep).write());status="";editorPage=0;editorControls();return;}
        if(b.id==23 && selectedStep>=0 && operation.state==AttackOperation.State.DRAFT){AttackOperation.Step copy=AttackOperation.Step.read(operation.steps.get(selectedStep).write());copy.id=UUID.randomUUID();copy.after=operation.steps.get(selectedStep).id;copy.offset=20;if(operation.steps.size()<256){operation.steps.add(selectedStep+1,copy);selectedStep++;dirty=true;}return;}
        if(b.id==24 && selectedStep>=0 && operation.state==AttackOperation.State.DRAFT){UUID removed=operation.steps.remove(selectedStep).id;for(AttackOperation.Step s:operation.steps)if(removed.equals(s.after))s.after=null;selectedStep=Math.min(selectedStep,operation.steps.size()-1);dirty=true;return;}
        if((b.id==25 || b.id==26) && selectedStep>=0 && operation.state==AttackOperation.State.DRAFT){int next=selectedStep+(b.id==25?-1:1);if(next>=0 && next<operation.steps.size()){Collections.swap(operation.steps,selectedStep,next);selectedStep=next;dirty=true;}return;}
        if(b.id==30){if(dirty){status=tr("save_first");return;}wizard.discard();operation=new AttackOperation();operation.title=tr("new_operation");operation.scope=terminal.scope;operation.author=mc.player.getUniqueID();selectedStep=-1;dirty=true;rebuild();return;}
        if(b.id>=31 && b.id<=38){String action=new String[]{"save","validate","start","pause","resume","cancel","copy","delete"}[b.id-31];
            if(b.id==32){panel=4;workspace.showPreflight();panelControls();}
            if(("start".equals(action) || "validate".equals(action)) && dirty){pendingAction=action;request("save");}else request(action);return;}
        if(b.id==40 || b.id==41){logPage=Math.max(0,Math.min(snapshot.getInteger("EventCount")/16,logPage+(b.id==40?-1:1)));request("fetch");}
        if(b.id==43){request("resolve_step");return;}
        if(b.id==42){NBTTagCompound n=selected();if(!n.hasNoTags()){targetX=(int)Math.floor(n.getDouble("X"));targetY=(int)Math.floor(n.getDouble("Y"));targetZ=(int)Math.floor(n.getDouble("Z"));targetSet=true;tab=1;status=tr("intel_copied");rebuild();request("fetch");}}
    }
    @Override protected void drawGuiContainerBackgroundLayer(float partial,int mouseX,int mouseY){
        GuiTheme.frame(guiLeft,guiTop,xSize,ySize);drawString(fontRenderer,tr("title"),guiLeft+12,guiTop+9,GuiTheme.TEXT);
        GuiTheme.text(fontRenderer,terminal.scope.startsWith("team:")?tr("team",terminal.scope.substring(5)):tr("private_terminal"),guiLeft+xSize-215,guiTop+9,200,GuiTheme.MUTED);
        if(panel!=0){drawPanel();return;}if(edited!=null){drawEditor();return;}
        if(snapshot.hasKey("Command",10)){NBTTagCompound sum=snapshot.getCompoundTag("Command").getCompoundTag("Summary");GuiTheme.text(fontRenderer,tr("workspace_summary",sum.getInteger("Ready"),sum.getInteger("Threats"),sum.getInteger("Empty"),sum.getInteger("Alerts")),guiLeft+12,guiTop+48,xSize-24,GuiTheme.ACCENT);}
        GuiTheme.panel(guiLeft+8,guiTop+56,leftWidth,Math.max(45,bottomY-82));GuiTheme.panel(guiLeft+mapX,guiTop+mapY,mapW,mapH);
        GuiTheme.panel(guiLeft+xSize-rightWidth-10,guiTop+56,rightWidth,Math.max(45,bottomY-82));
        drawMap();drawRows();drawCard();
        drawString(fontRenderer,tr("north"),guiLeft+mapX+mapW/2-12,guiTop+mapY+6,GuiTheme.TEXT);
        GuiTheme.text(fontRenderer,(int)viewX+" / "+(int)viewZ+"  ±"+radius+"  "+tr("blocks"),guiLeft+mapX+6,guiTop+mapY+mapH-12,mapW-12,GuiTheme.MUTED);
        GuiTheme.panel(guiLeft+8,guiTop+bottomY,xSize-16,ySize-bottomY-31);
        if(tab==1)drawTimeline();else if(tab==0)drawDefense();else drawIntelSummary();
        GuiTheme.wrapped(fontRenderer,status,guiLeft+12,guiTop+ySize-48,xSize-24,GuiTheme.WARN);
    }
    private void drawRows(){
        List<NBTTagCompound> rows=rows();int visible=Math.max(1,(bottomY-108)/28);listScroll=Math.max(0,Math.min(Math.max(0,rows.size()-visible),listScroll));
        for(int i=0;i<visible && i+listScroll<rows.size();i++){NBTTagCompound n=rows.get(i+listScroll);int y=guiTop+88+i*28;boolean chosen=n.getString("Key").equals(tab==2?selectedIntel:selectedAsset);
            if(chosen)drawRect(guiLeft+10,y-3,guiLeft+leftWidth+6,y+23,0xFF29434A);
            GuiTheme.text(fontRenderer,symbol(n.getString("Kind"))+" "+name(n),guiLeft+13,y,leftWidth-13,color(n.getString("Kind")));
            String detail=tab==2?tr("age",number(Math.max(0,snapshot.getLong("Tick")-n.getLong("Seen"))/20D)):tr("ammo",n.getInteger("Ammo"))+" · "+tr("state."+n.getString("State"));
            GuiTheme.text(fontRenderer,detail,guiLeft+13,y+11,leftWidth-13,GuiTheme.MUTED);
        }
        if(rows.isEmpty())GuiTheme.text(fontRenderer,tr(tab==2?"no_intel":"no_assets"),guiLeft+13,guiTop+90,leftWidth-14,GuiTheme.MUTED);
    }
    private java.util.List<String> cardLines(){java.util.List<String> result=new ArrayList<>();NBTTagCompound n=selected();if(n.hasNoTags()){result.add(tr("select_hint"));return result;}result.add(name(n));result.add(tr("kind."+n.getString("Kind")));result.add("X "+(int)n.getDouble("X")+" · Y "+(int)n.getDouble("Y")+" · Z "+(int)n.getDouble("Z"));
        if(tab==2){result.add(tr("age",number(Math.max(0,snapshot.getLong("Tick")-n.getLong("Seen"))/20D)));result.add(tr("quality",(int)(100*n.getFloat("Quality"))));result.add(tr("sensor."+n.getString("Sensor")));result.add(tr(n.getBoolean("Confirmed")?"observed_only":"unconfirmed"));return result;}
        result.add(n.getBoolean("Loaded")?tr("state."+n.getString("State")):tr("unloaded"));result.add(tr("ammo",n.getInteger("Ammo")));result.add(tr("power",n.getLong("Power")));if(n.getInteger("Tier")>0)result.add(tr("tier",n.getInteger("Tier")));if(n.getInteger("Range")>0)result.add(tr("range",n.getInteger("Range")));if(tab==0){result.add(tr(n.getBoolean("Network")?"network_online":"network_offline"));if(coverage && selectedAsset.equals(snapshot.getCompoundTag("Command").getCompoundTag("Coverage").getString("Asset")))result.add(tr("coverage_hint",(int)snapshot.getCompoundTag("Command").getCompoundTag("Coverage").getDouble("Altitude")));}
        NBTTagList stores=n.getTagList("Stores",10);for(int i=0;i<stores.tagCount();i++){NBTTagCompound store=stores.getCompoundTagAt(i);result.add("["+(store.getInteger("Slot")+1)+"] "+I18n.format(store.getString("Name")+".name")+" ×"+store.getInteger("Count"));}if(n.hasKey("Stats",10)){NBTTagCompound stats=n.getCompoundTag("Stats");result.add(tr("shots",stats.getInteger("Launched")));result.add(tr("kills",stats.getInteger("Intercepted")));}return result;
    }
    private int cardScroll;
    private boolean mapBatch;
    private void beginMapBatch(){net.minecraft.client.renderer.GlStateManager.enableBlend();net.minecraft.client.renderer.GlStateManager.disableTexture2D();net.minecraft.client.renderer.GlStateManager.tryBlendFuncSeparate(770,771,1,0);net.minecraft.client.renderer.GlStateManager.color(1,1,1,1);net.minecraft.client.renderer.Tessellator.getInstance().getBuffer().begin(7,net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION_COLOR);mapBatch=true;}
    private void endMapBatch(){mapBatch=false;net.minecraft.client.renderer.Tessellator.getInstance().draw();net.minecraft.client.renderer.GlStateManager.enableTexture2D();net.minecraft.client.renderer.GlStateManager.disableBlend();net.minecraft.client.renderer.GlStateManager.color(1,1,1,1);}
    private void drawCard(){int x=guiLeft+xSize-rightWidth-2,y=guiTop+64,w=rightWidth-16;drawString(fontRenderer,tr("details_short"),x,y,GuiTheme.ACCENT);y+=17;java.util.List<String> lines=new ArrayList<>();for(String detail:cardLines())lines.addAll(GuiTheme.wrap(fontRenderer,detail,w));int end=guiTop+bottomY-(terminal.command!=null?90:tab==1 || tab==0?46:27),visible=Math.max(1,(end-y)/10);cardScroll=Math.max(0,Math.min(Math.max(0,lines.size()-visible),cardScroll));for(int i=cardScroll;i<lines.size() && y+9<end;i++,y+=10)drawString(fontRenderer,lines.get(i),x,y,i==0?GuiTheme.TEXT:GuiTheme.MUTED);}
    private void drawMap(){
        beginMapBatch();
        int[] colors=snapshot.getIntArray("Terrain"),heights=snapshot.getIntArray("Heights");int cols=snapshot.hasKey("TerrainWidth")?snapshot.getInteger("TerrainWidth"):48,rows=snapshot.hasKey("TerrainHeight")?snapshot.getInteger("TerrainHeight"):27;
        double sourceX=snapshot.hasKey("X")?snapshot.getDouble("X"):viewX,sourceZ=snapshot.hasKey("Z")?snapshot.getDouble("Z"):viewZ;int sourceRadius=snapshot.hasKey("Radius")?snapshot.getInteger("Radius"):radius;
        double aspect=snapshot.hasKey("TerrainAspect")?snapshot.getDouble("TerrainAspect"):(double)CommandMapViewport.HEIGHT/CommandMapViewport.WIDTH;
        if(cols>0 && rows>0 && colors.length==cols*rows)for(int row=0;row<rows;row++)for(int col=0;col<cols;col++){
            int at=row*cols+col;if(colors[at]==0)continue;double x=sourceX+(col/(double)cols-.5)*2*sourceRadius,z=sourceZ+(row/(double)rows-.5)*2*sourceRadius*aspect;
            int rgb=colors[at];double light=.72;if(heights.length==colors.length && row>0)light=Math.max(.5,Math.min(.95,.72+(heights[at-cols]-heights[at])*.035));
            int shaded=((int)(((rgb>>16)&255)*light)<<16)|((int)(((rgb>>8)&255)*light)<<8)|(int)((rgb&255)*light);
            rect(px(x),py(z),px(x+2D*sourceRadius/cols)+1,py(z+2D*sourceRadius*aspect/rows)+1,0xFF000000|shaded);
        }
        if(coverage && tab==0){NBTTagCompound grid=snapshot.getCompoundTag("Command").getCompoundTag("Coverage");int[] mask=grid.getIntArray("Mask");if(mask.length==144){double gx=grid.getDouble("X"),gz=grid.getDouble("Z"),gr=grid.getInteger("Radius"),aspectGrid=grid.getDouble("Aspect");for(int row=0;row<9;row++)for(int col=0;col<16;col++){int code=mask[row*16+col];if(code==0 || code==3)continue;double xx=gx+(col/16D-.5)*2*gr,zz=gz+(row/9D-.5)*2*gr*aspectGrid;rect(px(xx),py(zz),px(xx+2*gr/16)+1,py(zz+2*gr*aspectGrid/9)+1,code==1?0x354CBB83:0x358D6E51);}}NBTTagList districts=snapshot.getCompoundTag("Command").getTagList("Districts",10);for(int i=0;i<districts.tagCount();i++){NBTTagCompound zone=districts.getCompoundTagAt(i);circle(px(zone.getDouble("X")),py(zone.getDouble("Z")),zone.getInteger("Radius")*mapW/(2D*radius),0xA0F0BC6F);}}
        if(coverage && tab==0)for(NBTTagCompound node:rows())if(node.getBoolean("Loaded") && node.getInteger("Range")>0 && ("radar".equals(node.getString("Kind")) || "pvo".equals(node.getString("Kind"))))circle(px(node.getDouble("X")),py(node.getDouble("Z")),Math.min(1600,node.getInteger("Range")*mapW/(2D*radius)),"radar".equals(node.getString("Kind"))?0x6066D8E9:0x60A9DD7C);
        if(tab==0){NBTTagList sectors=snapshot.getTagList("Sectors",10);for(int i=0;i<sectors.tagCount();i++){NBTTagCompound n=sectors.getCompoundTagAt(i);if(n.getBoolean("Loaded") && "command".equals(n.getString("Kind")) && "deployed".equals(n.getString("State")) && n.getLong("Power")>=120)mapLine(px(terminal.x()),py(terminal.z()),px(n.getDouble("X")),py(n.getDouble("Z")),0x668CE3B2);}}
        if(tab==1)for(AttackOperation.Step step:operation.steps)if(step.asset.equals(selectedAsset)){NBTTagCompound unit=selected();if(!unit.hasNoTags())mapLine(px(unit.getDouble("X")),py(unit.getDouble("Z")),px(step.x),py(step.z),0x88E7B774);int rx=step.x,rz=step.z;for(int i=step.route.tagCount()-1;i>=0;i--){NBTTagCompound point=step.route.getCompoundTagAt(i);mapLine(px(rx),py(rz),px(point.getInteger("X")),py(point.getInteger("Z")),0x88E7B774);rx=point.getInteger("X");rz=point.getInteger("Z");}}

        if(tab==2){NBTTagList cells=snapshot.getCompoundTag("Intel").getTagList("Cells",10);for(int i=0;i<cells.tagCount();i++){NBTTagCompound c=cells.getCompoundTagAt(i);int x=c.getInteger("X")*16,z=c.getInteger("Z")*16;rect(px(x),py(z),px(x+16)+1,py(z+16)+1,0xFF000000|((c.getInteger("Color")&0xFEFEFE)>>>1));}}
        int grid=Math.max(32,radius/4);for(double x=Math.floor((viewX-radius)/grid)*grid;x<=viewX+radius;x+=grid)rect(px(x),guiTop+mapY,px(x)+1,guiTop+mapY+mapH,0x403F7C7C);
        double zr=radius*(double)mapH/mapW;for(double z=Math.floor((viewZ-zr)/grid)*grid;z<=viewZ+zr;z+=grid)rect(guiLeft+mapX,py(z),guiLeft+mapX+mapW,py(z)+1,0x403F7C7C);
        endMapBatch();
        List<int[]> labels=new ArrayList<>();if(tab!=2){int tx=px(terminal.x()),ty=py(terminal.z());labels.add(new int[]{Math.max(guiLeft+mapX+3,tx-30),Math.max(guiTop+mapY+18,ty-18),Math.min(guiLeft+mapX+mapW-3,tx+62),ty-7});}
        for(NBTTagCompound node:rows()){int nx=px(node.getDouble("X")),ny=py(node.getDouble("Z"));if(inside(nx,ny))labels.add(new int[]{nx-6,ny-6,nx+7,ny+7});}
        Map<String,Integer> clusters=new HashMap<>();for(NBTTagCompound n:rows()){int x=px(n.getDouble("X")),y=py(n.getDouble("Z"));if(!inside(x,y))continue;
            String cell=(x/14)+":"+(y/14);int count=clusters.getOrDefault(cell,0)+1;clusters.put(cell,count);if(count>1){drawString(fontRenderer,"+"+(count-1),x+7,y-7,GuiTheme.WARN);continue;}
            String kind=n.getString("Kind");drawRect(x-4,y-4,x+5,y+5,0xFF000000|color(kind));drawString(fontRenderer,symbol(kind),x-3,y-4,0x112228);
            if(n.getString("Key").equals(tab==2?selectedIntel:selectedAsset)){drawRect(x-6,y-6,x+7,y-5,0xFFFFFFFF);drawRect(x-6,y+6,x+7,y+7,0xFFFFFFFF);}
            if(radius<=1024){int w=Math.min(100,Math.max(0,guiLeft+mapX+mapW-x-10));if(w<18)continue;for(int offset:new int[]{0,12,-12,24,-24,36,-36}){int ly=y-4+offset;boolean free=ly>=guiTop+mapY+24 && ly+10<guiTop+mapY+mapH-16;for(int[] box:labels)if(x+8<box[2] && x+8+w>box[0] && ly<box[3] && ly+10>box[1])free=false;if(free){if(offset!=0)mapLine(x+5,y,x+8,ly+4,0x805B898D);drawRect(x+7,ly-1,x+9+w,ly+10,0xCC13242A);GuiTheme.text(fontRenderer,name(n),x+8,ly,w,color(kind));labels.add(new int[]{x+7,ly-1,x+9+w,ly+10});break;}}}
        }
        if(tab==0){NBTTagList contacts=snapshot.getTagList("RadarContacts",10);for(int i=0;i<contacts.tagCount();i++){NBTTagCompound contact=contacts.getCompoundTagAt(i);int cx=px(contact.getDouble("X")),cy=py(contact.getDouble("Z")),colour=contact.getBoolean("Friendly")?0xFF96E3AE:contact.getBoolean("Assigned")?0xFFEAC078:0xFFE67878;if(!inside(cx,cy))continue;mapLine(cx-4,cy,cx,cy-4,colour);mapLine(cx,cy-4,cx+4,cy,colour);mapLine(cx+4,cy,cx,cy+4,colour);mapLine(cx,cy+4,cx-4,cy,colour);double speed=Math.hypot(contact.getDouble("VX"),contact.getDouble("VZ"));if(speed>.01)mapLine(cx,cy,cx+(int)(12*contact.getDouble("VX")/speed),cy+(int)(12*contact.getDouble("VZ")/speed),colour);}}
        if(tab!=2 && inside(px(terminal.x()),py(terminal.z()))){int tx=px(terminal.x()),tz=py(terminal.z());rect(tx-6,tz-6,tx+7,tz-5,0xFFFFFFFF);rect(tx-6,tz+6,tx+7,tz+7,0xFFFFFFFF);rect(tx,tz-5,tx+1,tz+6,0xFFFFFFFF);rect(tx-5,tz,tx+6,tz+1,0xFFFFFFFF);GuiTheme.text(fontRenderer,tr("here"),Math.max(guiLeft+mapX+3,tx-30),Math.max(guiTop+mapY+18,tz-18),90,GuiTheme.TEXT);}
        if(targetSet && inside(px(targetX),py(targetZ))){int x=px(targetX),y=py(targetZ);drawRect(x-7,y,x+8,y+1,0xFFEAC078);drawRect(x,y-7,x+1,y+8,0xFFEAC078);}
    }
    private void mapLine(int x,int y,int tx,int ty,int color){int count=Math.min(4096,Math.max(Math.abs(tx-x),Math.abs(ty-y)));for(int i=0;i<=count;i++){int px=x+(int)((long)(tx-x)*i/Math.max(1,count)),py=y+(int)((long)(ty-y)*i/Math.max(1,count));rect(px,py,px+1,py+1,color);}}
    private void circle(int x,int y,double radius,int color){for(int i=0;i<120;i++){double a=i*Math.PI/60,b=(i+1)*Math.PI/60;mapLine(x+(int)(Math.cos(a)*radius),y+(int)(Math.sin(a)*radius),x+(int)(Math.cos(b)*radius),y+(int)(Math.sin(b)*radius),color);}}
    private int surfaceHeight(int x,int z){if(tab==2){NBTTagList cells=snapshot.getCompoundTag("Intel").getTagList("Cells",10);for(int i=0;i<cells.tagCount();i++){NBTTagCompound cell=cells.getCompoundTagAt(i);if(Math.floorDiv(x,16)==cell.getInteger("X") && Math.floorDiv(z,16)==cell.getInteger("Z"))return cell.getInteger("Height");}return targetY;}
        int cols=snapshot.getInteger("TerrainWidth"),rows=snapshot.getInteger("TerrainHeight"),r=snapshot.getInteger("Radius");int[] heights=snapshot.getIntArray("Heights"),colors=snapshot.getIntArray("Terrain");double aspect=snapshot.getDouble("TerrainAspect");if(cols<1 || rows<1 || r<1 || aspect<=0)return targetY;int col=(int)Math.floor(((x-snapshot.getDouble("X"))/(2D*r)+.5)*cols),row=(int)Math.floor(((z-snapshot.getDouble("Z"))/(2D*r*aspect)+.5)*rows),at=row*cols+col;return col>=0 && col<cols && row>=0 && row<rows && heights.length==cols*rows && colors.length==heights.length && colors[at]!=0?heights[at]:targetY;
    }
    private int px(double x){return guiLeft+mapX+(int)Math.round(mapW/2D+(x-viewX)*mapW/(2D*radius));}
    private int py(double z){return guiTop+mapY+(int)Math.round(mapH/2D+(z-viewZ)*mapW/(2D*radius));}
    private boolean inside(int x,int y){return x>guiLeft+mapX+2 && x<guiLeft+mapX+mapW-3 && y>guiTop+mapY+2 && y<guiTop+mapY+mapH-3;}
    private void rect(int x,int y,int x2,int y2,int color){x=Math.max(x,guiLeft+mapX+1);y=Math.max(y,guiTop+mapY+1);x2=Math.min(x2,guiLeft+mapX+mapW-1);y2=Math.min(y2,guiTop+mapY+mapH-1);if(x2<=x || y2<=y)return;if(!mapBatch){drawRect(x,y,x2,y2,color);return;}
        net.minecraft.client.renderer.BufferBuilder buffer=net.minecraft.client.renderer.Tessellator.getInstance().getBuffer();int r=(color>>16)&255,g=(color>>8)&255,b=color&255,a=(color>>>24)&255;buffer.pos(x,y2,0).color(r,g,b,a).endVertex();buffer.pos(x2,y2,0).color(r,g,b,a).endVertex();buffer.pos(x2,y,0).color(r,g,b,a).endVertex();buffer.pos(x,y,0).color(r,g,b,a).endVertex();}
    private static int color(String kind){return "terminal".equals(kind)?0xFFFFFF:"command".equals(kind)?0xF0CB7E:"radar".equals(kind)?0x66D8E9:"pvo".equals(kind)?0xA9DD7C:"siren".equals(kind)?0xE8C66D:"relay".equals(kind)?0xBB9CE8:"aircraft".equals(kind)?0x7EBCF2:"uav".equals(kind)?0x8FE3D3:"missile".equals(kind)?0xE87872:0xD0D9DF;}
    private static String symbol(String kind){return "terminal".equals(kind)?"H":"command".equals(kind)?"C":"radar".equals(kind)?"R":"pvo".equals(kind)?"A":"siren".equals(kind)?"!":"relay".equals(kind)?"L":"aircraft".equals(kind)?"F":"uav".equals(kind)?"U":"mlrs".equals(kind)?"M":"tel".equals(kind)?"B":"P";}
    private void drawTimeline(){
        GuiTheme.text(fontRenderer,operation.title+" · "+tr("state."+operation.state.name())+" · "+tr("step_count",operation.steps.size()),guiLeft+16,guiTop+bottomY+25,xSize-32,GuiTheme.ACCENT);
        int rowY=guiTop+bottomY+39,visible=Math.max(1,(ySize-bottomY-88)/15);stepScroll=Math.max(0,Math.min(Math.max(0,operation.steps.size()-visible),stepScroll));
        for(int i=0;i<visible && i+stepScroll<operation.steps.size();i++){int index=i+stepScroll;AttackOperation.Step s=operation.steps.get(index);int y=rowY+i*15;
            if(index==selectedStep)drawRect(guiLeft+10,y-2,guiLeft+xSize-10,y+12,0xFF29434A);
            String time=s.after==null?"T+"+number(s.offset/20D):"#"+(indexOf(s.after)+1)+" +"+number(s.offset/20D);
            String text=(index+1)+"  "+time+"s  "+tr("action."+s.action.name())+"  "+assetName(s.asset)+"  ×"+s.count+" → "+s.x+" / "+s.y+" / "+s.z;
            GuiTheme.text(fontRenderer,text,guiLeft+14,y,xSize-165,GuiTheme.TEXT);
            GuiTheme.text(fontRenderer,tr("phase."+s.phase.name()),guiLeft+xSize-145,y,130,s.phase==AttackOperation.Phase.DONE?GuiTheme.GOOD:s.phase==AttackOperation.Phase.FAILED?0xE87872:GuiTheme.WARN);
        }
        if(operation.steps.isEmpty())GuiTheme.wrapped(fontRenderer,tr("plan_hint"),guiLeft+16,rowY,xSize-32,GuiTheme.MUTED);
    }
    private int indexOf(UUID id){for(int i=0;i<operation.steps.size();i++)if(operation.steps.get(i).id.equals(id))return i;return -1;}
    private String assetName(String key){NBTTagList list=snapshot.getTagList("Assets",10);for(int i=0;i<list.tagCount();i++)if(key.equals(list.getCompoundTagAt(i).getString("Key")))return name(list.getCompoundTagAt(i));return key.length()>18?key.substring(0,18):key;}
    private void drawDefense(){
        NBTTagCompound stats=snapshot.getCompoundTag("Stats");int x=guiLeft+16,y=guiTop+bottomY+10;
        GuiTheme.text(fontRenderer,tr("defense_summary",stats.getInteger("Launched"),stats.getInteger("Intercepted"),stats.getInteger("Breakthrough"),stats.getInteger("Unknown")),x,y,xSize-32,GuiTheme.TEXT);
        y+=15;String rate=!stats.hasKey("Rate") || stats.getDouble("Rate")<0?"—":number(stats.getDouble("Rate"))+"%",efficiency=!stats.hasKey("Efficiency") || stats.getDouble("Efficiency")<0?"—":number(stats.getDouble("Efficiency"))+"%";
        GuiTheme.text(fontRenderer,tr("defense_rates",rate,efficiency),x,y,xSize-32,GuiTheme.ACCENT);y+=12;GuiTheme.text(fontRenderer,tr("gun_summary",stats.getInteger("GunBursts"),stats.getInteger("GunRounds")),x,y,xSize-32,GuiTheme.MUTED);y+=12;GuiTheme.text(fontRenderer,tr("identified_summary",stats.getInteger("Identified"),stats.getInteger("Failed"),stats.getInteger("Flying")),x,y,xSize-32,GuiTheme.MUTED);y+=13;
        NBTTagList events=snapshot.getTagList("Events",10);for(int i=0;i<events.tagCount() && y<guiTop+ySize-48;i++,y+=12){NBTTagCompound n=events.getCompoundTagAt(i);GuiTheme.text(fontRenderer,number(n.getLong("Tick")/20D)+"s  "+name(n)+" · "+tr("outcome."+n.getString("Outcome"))+(n.getInteger("Tier")>0?" · T"+n.getInteger("Tier"):"")+(n.getString("TargetName").isEmpty()?"":" → "+n.getString("TargetName")),x,y,xSize-32,GuiTheme.MUTED);}
    }
    private void drawIntelSummary(){GuiTheme.wrapped(fontRenderer,tr("intel_hint"),guiLeft+16,guiTop+bottomY+12,xSize-32,GuiTheme.MUTED);}
    @Override public void drawScreen(int x,int y,float partial){
        positionSlots();drawDefaultBackground();super.drawScreen(x,y,partial);
        if(panel==5){wizard.fields();wizard.tooltip(x,y);}else if(panel==4){workspace.drawFields();workspace.tooltip(x,y);}else if(panel==1 || panel==2)panelName.drawTextBox();else if(panel==0 && edited==null)search.drawTextBox();else if(panel==0)for(GuiTextField f:fields)f.drawTextBox();
        renderHoveredToolTip(x,y);if(panel==0 && edited==null && x>=guiLeft+xSize-rightWidth-10 && x<guiLeft+xSize-10 && y>=guiTop+60 && y<guiTop+bottomY-90)drawHoveringText(cardLines(),x,y);
        if(panel==0 && edited==null && inside(x,y))for(NBTTagCompound n:rows())if(Math.abs(x-px(n.getDouble("X")))<8 && Math.abs(y-py(n.getDouble("Z")))<8){drawHoveringText(Arrays.asList(name(n),tr("kind."+n.getString("Kind")),"X "+(int)n.getDouble("X")+" Y "+(int)n.getDouble("Y")+" Z "+(int)n.getDouble("Z")),x,y);break;}
    }
    private void positionSlots(){for(net.minecraft.inventory.Slot slot:inventorySlots.inventorySlots){slot.xPos=-1000;slot.yPos=-1000;}if(panel!=3)return;inventorySlots.inventorySlots.get(0).xPos=20;inventorySlots.inventorySlots.get(0).yPos=70;
        int left=(xSize-162)/2,top=ySize-110;for(int i=1;i<37;i++){net.minecraft.inventory.Slot slot=inventorySlots.inventorySlots.get(i);int index=i-1;slot.xPos=left+(index%9)*18;slot.yPos=top+(index<27?index/9*18:58);}}

    @Override protected void mouseClicked(int x,int y,int button)throws IOException{
        if(panel==5){wizard.click(x,y,button);super.mouseClicked(x,y,button);return;}
        if(panel==4){workspace.click(x,y,button);super.mouseClicked(x,y,button);return;}
        if(panel==1 || panel==2){panelName.mouseClicked(x,y,button);super.mouseClicked(x,y,button);return;}
        if(panel==3){super.mouseClicked(x,y,button);return;}
        if(edited!=null){for(GuiTextField f:fields)f.mouseClicked(x,y,button);super.mouseClicked(x,y,button);return;}
        search.mouseClicked(x,y,button);
        if(button==0 && x>=guiLeft+10 && x<guiLeft+leftWidth+8 && y>=guiTop+85 && y<guiTop+bottomY-28){int index=listScroll+(y-guiTop-85)/28;List<NBTTagCompound> rows=rows();if(index<rows.size()){NBTTagCompound n=rows.get(index);if(tab==2)selectedIntel=n.getString("Key");else selectedAsset=n.getString("Key");viewX=n.getDouble("X");viewZ=n.getDouble("Z");request("fetch");}}
        if(inside(x,y)){
            NBTTagCompound closest=null;double best=81;for(NBTTagCompound n:rows()){double d=Math.pow(x-px(n.getDouble("X")),2)+Math.pow(y-py(n.getDouble("Z")),2);if(d<best){best=d;closest=n;}}
            if(button==0 && closest!=null){if(tab==2)selectedIntel=closest.getString("Key");else selectedAsset=closest.getString("Key");}
            else if(button==1){targetX=(int)Math.floor(viewX+(x-guiLeft-mapX-mapW/2D)*2*radius/mapW);targetZ=(int)Math.floor(viewZ+(y-guiTop-mapY-mapH/2D)*2*radius/mapW);targetY=surfaceHeight(targetX,targetZ);targetSet=true;status=tr("target_set",targetX,targetY,targetZ);}
            else if(button==0){dragging=true;dragX=x;dragY=y;dragViewX=viewX;dragViewZ=viewZ;}
        }
        if(tab==1 && button==0 && y>=guiTop+bottomY+37 && y<guiTop+ySize-46){int index=stepScroll+(y-guiTop-bottomY-37)/15;if(index<operation.steps.size()){selectedStep=index;AttackOperation.Step s=operation.steps.get(index);status=tr("step_details",s.accepted<0?"—":number(s.accepted/20D),s.actual<0?"—":number(s.actual/20D),s.reason.isEmpty()?"":tr("error."+s.reason));}}
        super.mouseClicked(x,y,button);
    }
    @Override protected void mouseClickMove(int x,int y,int button,long elapsed){if(panel==4){workspace.drag(x,y,button);return;}if(dragging && button==0){viewX=dragViewX-(x-dragX)*2D*radius/mapW;viewZ=dragViewZ-(y-dragY)*2D*radius/mapW;}else super.mouseClickMove(x,y,button,elapsed);}
    @Override protected void mouseReleased(int x,int y,int state){if(panel==4)workspace.release(x,y);if(dragging){dragging=false;request("fetch");}super.mouseReleased(x,y,state);}
    @Override public void handleMouseInput()throws IOException{super.handleMouseInput();int wheel=org.lwjgl.input.Mouse.getEventDWheel();if(panel==4){int cx=org.lwjgl.input.Mouse.getEventX()*width/mc.displayWidth,cy=height-org.lwjgl.input.Mouse.getEventY()*height/mc.displayHeight-1;workspace.wheel(wheel,cx,cy);return;}if(wheel==0 || edited!=null || panel!=0)return;int x=org.lwjgl.input.Mouse.getEventX()*width/mc.displayWidth,y=height-org.lwjgl.input.Mouse.getEventY()*height/mc.displayHeight-1;
        if(inside(x,y)){radius=Math.max(64,Math.min(16384,wheel>0?radius/2:radius*2));request("fetch");}else if(y>=guiTop+bottomY && tab==1)stepScroll=Math.max(0,stepScroll+(wheel>0?-1:1));else if(x>=guiLeft+xSize-rightWidth-10 && y<guiTop+bottomY)cardScroll=Math.max(0,cardScroll+(wheel>0?-1:1));else listScroll=Math.max(0,listScroll+(wheel>0?-1:1));}
    @Override protected void keyTyped(char character,int key)throws IOException{if(panel==5){if(key==1){wizard.action(300);return;}if(wizard.key(character,key))return;}if(panel==4 && workspace.key(character,key))return;if(panel!=0){if(key==1){panel=0;rebuild();return;}if(panelName!=null && panelName.textboxKeyTyped(character,key))return;}else if(edited!=null){for(GuiTextField f:fields)if(f.textboxKeyTyped(character,key))return;if(key==1){edited=null;rebuild();return;}}else if(search.textboxKeyTyped(character,key)){listScroll=0;return;}super.keyTyped(character,key);}
    @Override public void onGuiClosed(){org.lwjgl.input.Keyboard.enableRepeatEvents(false);snapshot=new NBTTagCompound();super.onGuiClosed();}
    private void field(int id,int x,int y,int w,String value){GuiTextField f=new GuiTextField(id,fontRenderer,guiLeft+x,guiTop+y,w,16);f.setMaxStringLength(id==0?64:40);f.setText(value);fields.add(f);}
    private void editorControls(){
        buttonList.clear();fields.clear();int col=(xSize-44)/2,r=col+28,row=ySize<300?31:Math.max(31,Math.min(44,(ySize-130)/3)),top=88;
        String[] values={operation.title,""+edited.x,""+edited.y,""+edited.z,number(edited.offset/20D),number(edited.deadline/20D),""+(edited.slot+1),""+edited.count,number(edited.interval/20D),edited.group,number(edited.intelAge/20D),edited.conditionGroup};
        for(int i=0;i<12;i++)field(i,-1000,-1000,col,values[i]);
        int[][] ids=editorPage==0?new int[][]{{0,-1},{1,2},{3,-1}}:editorPage==3?new int[][]{{6,7}}:editorPage==4?new int[][]{{10,11}}:new int[][]{{4,5},{8,9}};
        for(int line=0;line<ids.length;line++)for(int c=0;c<2;c++){int id=ids[line][c];if(id<0)continue;GuiTextField field=fields.get(id);field.x=guiLeft+(c==0?16:r);field.y=guiTop+top+line*row;field.width=col;}
        for(GuiTextField field:fields)field.setVisible(field.x>=guiLeft);
        int tabWidth=(xSize-48)/5;for(int i=0;i<5;i++)button(60+i,16+i*(tabWidth+4),43,tabWidth,tr("editor_tab."+i));
        int y=editorPage==0?top+2*row+21:top+2*row+10;
        if(editorPage==0){fields.get(0).width=xSize-32;button(59,r,top+2*row,col,tr("designator_import"));}
        else if(editorPage==3){button(51,16,top+row+12,xSize-32,tr("action."+edited.action.name()));}
        else if(editorPage==4){button(65,16,top+row+12,xSize-32,tr("condition."+edited.condition.name()));}
        else if(editorPage==1){button(50,16,y,col,tr("failure."+edited.failure.name()));button(52,r,y,col,edited.after==null?tr("relative_t0"):tr("after_step",indexOf(edited.after)+1));button(53,16,y+24,col,tr("event."+edited.event.name()));button(54,r,y+24,col,tr(edited.arrival?"arrival_time":"launch_time"));}
        else {for(GuiTextField field:fields)field.setVisible(false);for(int i=1;i<=3;i++){GuiTextField field=fields.get(i);field.x=guiLeft+(i==2?r:16);field.y=guiTop+top+(i==3?row:0);field.setVisible(true);}button(55,16,top+2*row,col,tr("route_add")+" ("+edited.route.tagCount()+"/7)");button(56,r,top+2*row,col,tr("route_clear"));}
        button(57,16,ySize-26,col,tr("apply"));button(58,r,ySize-26,col,tr("back"));
    }
    private void drawEditor(){
        GuiTheme.text(fontRenderer,tr("editor",selectedStep+1)+" · "+assetName(edited.asset),guiLeft+16,guiTop+29,xSize-32,GuiTheme.ACCENT);
        String[] labels={"operation_name","x","y","z","offset","deadline","slot","count","interval","group","intel_age","condition_group"};
        for(int i=0;i<fields.size();i++){GuiTextField field=fields.get(i);if(!field.getVisible())continue;java.util.List<String> lines=GuiTheme.wrap(fontRenderer,tr(labels[i]),field.width);int yy=field.y-lines.size()*10-2;for(String label:lines){drawString(fontRenderer,label,field.x,yy,GuiTheme.MUTED);yy+=10;}}
        if(editorPage==2){int y=guiTop+88+2*Math.max(31,Math.min(44,(ySize-130)/3))+28;GuiTheme.text(fontRenderer,tr("route_count",edited.route.tagCount()),guiLeft+16,y,xSize-32,GuiTheme.MUTED);for(int i=0;i<edited.route.tagCount() && y+24<guiTop+ySize-50;i++){NBTTagCompound point=edited.route.getCompoundTagAt(i);y+=12;drawString(fontRenderer,(i+1)+". X "+point.getInteger("X")+" · Y "+point.getInteger("Y")+" · Z "+point.getInteger("Z"),guiLeft+16,y,GuiTheme.TEXT);}}
        GuiTheme.wrapped(fontRenderer,status,guiLeft+16,guiTop+ySize-46,xSize-32,GuiTheme.WARN);
    }
    private void quickAttack(){
        if(operation.state!=AttackOperation.State.DRAFT){status=tr("draft_only");return;}NBTTagCompound unit=selected();if(unit.hasNoTags()){status=tr("select_asset");return;}if(!targetSet){status=tr("choose_target");return;}
        String actions=unit.getString("Actions");java.util.List<AttackOperation.Action> orders=new ArrayList<>();if(actions.contains("PREPARE"))orders.add(AttackOperation.Action.PREPARE);if(actions.contains("TAKEOFF"))orders.add(AttackOperation.Action.TAKEOFF);
        if(actions.contains("LAUNCH"))orders.add(AttackOperation.Action.LAUNCH);else if(actions.contains("RELEASE"))orders.add(AttackOperation.Action.RELEASE);else if(actions.contains("OBSERVE"))orders.add(AttackOperation.Action.OBSERVE);
        if(orders.isEmpty() || operation.steps.size()+orders.size()>256){status=tr("error.unsupported");return;}
        AttackOperation.Step previous=null;for(AttackOperation.Action action:orders){AttackOperation.Step step=new AttackOperation.Step();step.asset=unit.getString("Key");step.action=action;step.x=targetX;step.y=targetY;step.z=targetZ;step.deadline=6000;if(previous!=null){step.after=previous.id;step.event=AttackOperation.Event.COMPLETE;}operation.steps.add(step);previous=step;}selectedStep=operation.steps.size()-1;dirty=true;status=tr("quick_added");
    }
    private void panelControls(){
        if(panel==5){fields.clear();panelName=null;wizard.layout(fontRenderer,guiLeft,guiTop,xSize,ySize,buttonList);return;}
        if(panel==4){panelName=null;fields.clear();workspace.layout(fontRenderer,guiLeft,guiTop,xSize,ySize,buttonList);return;}
        buttonList.clear();int col=(xSize-44)/2;panelName=null;
        if(panel==1 || panel==2){panelName=new GuiTextField(70,fontRenderer,guiLeft+16,guiTop+64,xSize-32,18);panelName.setMaxStringLength(64);panelName.setText(panel==1?operation.title:allyTeam);}
        if(panel==1){int visible=Math.max(1,(ySize-165)/24);NBTTagList operations=snapshot.getTagList("Operations",10);operationPage=Math.max(0,Math.min(Math.max(0,(operations.tagCount()-1)/visible),operationPage));for(int i=0;i<visible && i+operationPage*visible<operations.tagCount();i++){NBTTagCompound n=operations.getCompoundTagAt(i+operationPage*visible);button(100+i,16,113+i*24,xSize-32,n.getString("Title")+" · "+tr("state."+n.getString("State"))+" · "+tr("step_count",n.getInteger("Steps")));}
            button(72,16,87,col,tr("rename"));button(73,col+28,87,col,tr("new_named"));button(74,16,ySize-52,50,"<");button(75,70,ySize-52,50,">");}
        if(panel==2){button(76,16,88,col,tr("ally_add"));button(77,col+28,88,col,tr("ally_remove"));button(80,16,ySize-66,50,"<");button(81,70,ySize-66,50,">");}
        if(panel==3)button(78,48,68,xSize-64,tr("import_target"));
        button(79,16,ySize-26,xSize-32,tr("back"));
    }
    private void panelAction(int id){
        if(panel==5){wizard.action(id);return;}
        if(panel==4){workspace.action(id);return;}
        if(id==79){panel=panel==3 && wizard.active?5:0;rebuild();return;}
        if(panel==1){
            if(id==74 || id==75){operationPage=Math.max(0,operationPage+(id==74?-1:1));panelControls();return;}
            if(id==72){if(operation.state!=AttackOperation.State.DRAFT){status=tr("draft_only");return;}operation.title=panelName.getText();dirty=true;request("save");return;}
            if(id==73){if(dirty){status=tr("save_first");return;}operation=new AttackOperation();operation.scope=terminal.scope;operation.author=mc.player.getUniqueID();operation.title=panelName.getText().trim().isEmpty()?tr("new_operation"):panelName.getText();selectedStep=-1;dirty=true;request("save");return;}
            if(id>=100){if(dirty || awaitingSave){status=tr("save_first");return;}int visible=Math.max(1,(ySize-165)/24),index=id-100+operationPage*visible;NBTTagList operations=snapshot.getTagList("Operations",10);if(index>=operations.tagCount())return;operation=AttackOperation.read(operations.getCompoundTagAt(index));operation.scope=terminal.scope;awaitingSelection=true;panel=0;selectedStep=-1;rebuild();request("fetch");return;}
        }
        if(panel==2 && (id==76 || id==77)){allyTeam=panelName.getText();request(id==76?"ally_add":"ally_remove");return;}
        if(panel==2 && (id==80 || id==81)){allyPage=Math.max(0,allyPage+(id==80?-1:1));return;}
        if(panel==3 && id==78){request("import_target");status=tr("waiting_target");}
    }
    private void drawPanel(){
        if(panel==5){wizard.draw();return;}
        if(panel==4){workspace.draw(snapshot,operation);return;}
        GuiTheme.text(fontRenderer,tr(panel==1?"operations":panel==2?"allies":"designator_import"),guiLeft+16,guiTop+30,xSize-32,GuiTheme.ACCENT);
        if(panel==1 || panel==2)drawString(fontRenderer,tr(panel==1?"operation_name":"ally_team"),guiLeft+16,guiTop+52,GuiTheme.MUTED);
        if(panel==2){NBTTagList allies=snapshot.getTagList("Allies",8);int visible=Math.max(1,(ySize-194)/14),pages=Math.max(1,(allies.tagCount()+visible-1)/visible);allyPage=Math.min(allyPage,pages-1);int y=guiTop+118;for(int i=allyPage*visible;i<allies.tagCount() && i<(allyPage+1)*visible;i++,y+=14)GuiTheme.text(fontRenderer,"✓ "+allies.getStringTagAt(i),guiLeft+20,y,xSize/2-32,GuiTheme.GOOD);drawString(fontRenderer,(allyPage+1)+" / "+pages,guiLeft+126,guiTop+ySize-62,GuiTheme.MUTED);GuiTheme.wrapped(fontRenderer,tr("allies_hint"),guiLeft+xSize/2,guiTop+120,xSize/2-24,GuiTheme.MUTED);}
        if(panel==3){GuiTheme.wrapped(fontRenderer,tr("designator_hint"),guiLeft+16,guiTop+102,xSize-32,GuiTheme.MUTED);drawString(fontRenderer,I18n.format("container.inventory"),guiLeft+(xSize-162)/2,guiTop+ySize-123,GuiTheme.MUTED);for(net.minecraft.inventory.Slot slot:inventorySlots.inventorySlots)if(slot.xPos>=0){drawRect(guiLeft+slot.xPos-1,guiTop+slot.yPos-1,guiLeft+slot.xPos+17,guiTop+slot.yPos+17,0xFF49636B);drawRect(guiLeft+slot.xPos+1,guiTop+slot.yPos+1,guiLeft+slot.xPos+15,guiTop+slot.yPos+15,0xFF15252B);}}
        GuiTheme.wrapped(fontRenderer,status,guiLeft+16,guiTop+ySize-46,xSize-32,GuiTheme.WARN);
    }

    private boolean readEditor(){try{
        operation.title=fields.get(0).getText();edited.x=Integer.parseInt(fields.get(1).getText());edited.y=Integer.parseInt(fields.get(2).getText());edited.z=Integer.parseInt(fields.get(3).getText());
        edited.offset=Math.round(Double.parseDouble(fields.get(4).getText().replace(',','.'))*20);edited.deadline=Math.round(Double.parseDouble(fields.get(5).getText().replace(',','.'))*20);
        edited.slot=Integer.parseInt(fields.get(6).getText())-1;edited.count=Integer.parseInt(fields.get(7).getText());edited.interval=(int)Math.round(Double.parseDouble(fields.get(8).getText().replace(',','.'))*20);edited.group=fields.get(9).getText();edited.intelAge=Math.round(Double.parseDouble(fields.get(10).getText().replace(',','.'))*20);edited.conditionGroup=fields.get(11).getText();return true;
        }catch(NumberFormatException e){status=tr("error.numbers");return false;}}
    private void editorAction(int id){
        if(id==58){edited=null;rebuild();return;}if(!readEditor())return;
        if(id>=60 && id<=64){editorPage=id-60;editorControls();return;}
        if(id==65)edited.condition=AttackOperation.Condition.values()[(edited.condition.ordinal()+1)%AttackOperation.Condition.values().length];
        if(id==59){panel=3;panelControls();return;}
        if(id==50)edited.failure=AttackOperation.Failure.values()[(edited.failure.ordinal()+1)%AttackOperation.Failure.values().length];
        if(id==51){List<AttackOperation.Action> allowed=new ArrayList<>();NBTTagList list=snapshot.getTagList("Assets",10);for(int i=0;i<list.tagCount();i++)if(edited.asset.equals(list.getCompoundTagAt(i).getString("Key")))for(String s:list.getCompoundTagAt(i).getString("Actions").split(","))try{allowed.add(AttackOperation.Action.valueOf(s));}catch(IllegalArgumentException ignored){}
            if(!allowed.isEmpty())edited.action=allowed.get((allowed.indexOf(edited.action)+1)%allowed.size());}
        if(id==52){int current=indexOf(edited.after),next=current+1;while(next<operation.steps.size() && operation.steps.get(next).id.equals(edited.id))next++;edited.after=next>=operation.steps.size()?null:operation.steps.get(next).id;}
        if(id==53)edited.event=AttackOperation.Event.values()[(edited.event.ordinal()+1)%AttackOperation.Event.values().length];if(id==54)edited.arrival=!edited.arrival;
        if(id==55 && edited.route.tagCount()<7){NBTTagCompound p=new NBTTagCompound();p.setInteger("X",edited.x);p.setInteger("Y",edited.y);p.setInteger("Z",edited.z);p.setByte("Mode",(byte)0);p.setInteger("Hold",0);edited.route.appendTag(p);}
        if(id==56)edited.route=new NBTTagList();
        if(id==57){if(selectedStep==operation.steps.size())operation.steps.add(edited);else operation.steps.set(selectedStep,edited);dirty=true;edited=null;status=tr("edited");rebuild();return;}
        editorControls();
    }
    NBTTagCompound ccSnapshot(){return snapshot;}
    void wizardDesignator(){panel=3;panelControls();status=tr("wizard.designator_hint");}
    void wizardAssetPage(){assetPage=(assetPage+1)%Math.max(1,(snapshot.getInteger("Total")+255)/256);request("fetch");status=tr("waiting_server");}
    void wizardIcon(net.minecraft.item.ItemStack stack,int x,int y){net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting();itemRender.renderItemAndEffectIntoGUI(stack,x,y);net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting();}
    void wizardApply(List<AttackOperation.Step> steps){operation.steps.addAll(steps);selectedStep=operation.steps.size()-1;dirty=true;panel=0;tab=1;status=tr("wizard.added",steps.size());rebuild();}
    AttackOperation ccOperation(){return operation;}
    String ccName(NBTTagCompound row){return name(row);}
    String ccSelectedAsset(){return selectedAsset;}
    void ccDirty(){dirty=true;status=tr("edited");}
    void ccClose(){panel=0;rebuild();}
    void ccSend(String action,NBTTagCompound payload){NBTTagCompound request=createRequest(action);request.merge(payload);if(!PlanningRequestMessage.send(inventorySlots.windowId,request))status=I18n.format("planning.error.limits");ticks=0;}
    void ccFetch(){request("fetch");}
    void ccControl(String action){if(dirty && ("start".equals(action) || "validate".equals(action))){pendingAction=action;request("save");}else request(action);}
    void ccEdit(int index){if(operation.state!=AttackOperation.State.DRAFT || index<0 || index>=operation.steps.size())return;selectedStep=index;edited=AttackOperation.Step.read(operation.steps.get(index).write());panel=0;editorPage=0;editorControls();}
    void ccTooltip(java.util.List<String> text,int x,int y){java.util.List<String> display=new ArrayList<>();for(String line:text)display.add(readable(line));drawHoveringText(display,x,y);}
    void ccRevealPosition(NBTTagCompound row){viewX=row.getDouble("X");viewZ=row.getDouble("Z");selectedAsset="";tab=0;panel=0;rebuild();request("fetch");}
    void ccReveal(NBTTagCompound row){selectedAsset=row.getString("Key");viewX=row.getDouble("X");viewZ=row.getDouble("Z");tab=row.getBoolean("Attack")?1:0;panel=0;rebuild();request("fetch");}
    void ccRecon(NBTTagCompound intel,NBTTagCompound uav){
        if(dirty || awaitingSave){status=tr("save_first");return;}if(!uav.getString("Actions").contains("OBSERVE")){status=tr("error.unsupported");return;}
        operation=new AttackOperation();operation.scope=terminal.scope;operation.author=mc.player.getUniqueID();operation.title=AttackOperation.bounded(tr("survey_operation")+" "+name(intel),64);
        AttackOperation.Step previous=null;for(AttackOperation.Action action:!uav.getBoolean("Airborne")?new AttackOperation.Action[]{AttackOperation.Action.TAKEOFF,AttackOperation.Action.OBSERVE,AttackOperation.Action.RETURN}:new AttackOperation.Action[]{AttackOperation.Action.OBSERVE,AttackOperation.Action.RETURN}){
            AttackOperation.Step step=new AttackOperation.Step();step.asset=uav.getString("Key");step.action=action;step.x=(int)Math.floor(intel.getDouble("X"));step.y=(int)Math.max(1,Math.min(255,intel.getDouble("Y")+70));step.z=(int)Math.floor(intel.getDouble("Z"));step.deadline=12000;if(previous!=null){step.after=previous.id;step.event=AttackOperation.Event.COMPLETE;}operation.steps.add(step);previous=step;}
        dirty=true;selectedStep=0;tab=1;panel=0;viewX=intel.getDouble("X");viewZ=intel.getDouble("Z");rebuild();request("save");
    }

}
