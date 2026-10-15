package com.wartec.wartecmod.port.client;

import com.wartec.wartecmod.port.planning.*;
import java.util.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.resources.I18n;
import net.minecraft.nbt.*;

/** A workspace inside the existing container; all mutations use its original authorized session. */
final class CommandCenterWorkspace extends Gui {
    private final GuiPlanningTerminal host;
    private FontRenderer font;
    private List<GuiButton> buttons;
    private int x,y,w,h,page,scroll,unitPage,formPage,selectedRow=-1,diagnosticTarget=-1;
    private int left,top,bottom,detailScroll,diagnosticPage;
    private String selectedId="";private final List<String> detailLines=new ArrayList<>();private long editRevision=-1,districtRevision=-1;
    private String selectedKey="",intelKey="",notice="",district="",supportGroup="";
    private UUID replayTarget,linkFrom;
    private final Set<String> multi=new LinkedHashSet<>();
    private final Map<Integer,GuiTextField> fields=new LinkedHashMap<>();
    private int priority,reserveTier=3,lastControlPage=-1;
    private final Map<Integer,String> fleetEdit=new HashMap<>();
    private boolean loadFleet=true;
    private boolean playing,stretch,preflightView;
    private double timelineSpan=2400;
    private boolean timelineZoomed;private UUID timelineOperation;
    private int dragStep=-1,dragX;
    private long dragOffset,replayTick;
    private int dragCount;
    private NBTTagCompound data=new NBTTagCompound();
    CommandCenterWorkspace(GuiPlanningTerminal host){this.host=host;}
    private static String tr(String key,Object... args){return I18n.format("command."+key,args);}
    private static String number(double value){return String.format(Locale.ROOT,"%.1f",value);}
    void layout(FontRenderer font,int x,int y,int w,int h,List<GuiButton> buttons){this.font=font;this.x=x;this.y=y;this.w=w;this.h=h;this.buttons=buttons;left=Math.min(230,Math.max(130,w/3));top=y+83;bottom=y+h-70;controls();}
    private void button(int id,int bx,int by,int width,String label){buttons.add(new PlanningButton(id,x+bx,y+by,Math.max(20,width),18,label));}
    private void controls(){
        if(buttons==null)return;if(lastControlPage==7 && !loadFleet)for(Map.Entry<Integer,GuiTextField> e:fields.entrySet())fleetEdit.put(e.getKey(),e.getValue().getText());lastControlPage=page;bottom=y+h-(page==2?92:70);buttons.clear();fields.clear();int width=(w-36)/5;
        for(int i=0;i<9;i++)button(200+i,16+(i%5)*(width+1),32+(i/5)*21,width,tr("page."+i));
        button(209,16,h-25,w-32,tr("back"));
        int cy=h-48;button(210,16,cy,30,"<");button(211,49,cy,30,">");
        if(page==0){button(220,85,cy,Math.min(160,w-105),tr("ack"));button(221,w-140,cy,124,tr("show_map"));}
        if(page==1){button(250,85,cy,72,tr("batteries_prev"));button(251,160,cy,72,tr("batteries_next"));}
        if(page==2){button(222,85,cy,38,"+");button(223,126,cy,38,"−");button(224,168,cy,Math.min(100,w-300),tr("edit_step"));button(239,w-126,cy,110,tr("save_operation"));button(240,w-146,79,130,tr("start"));button(255,16,h-71,88,tr("preflight"));button(252,108,h-71,(w-132)/2,tr("revoke"));button(253,112+(w-132)/2,h-71,(w-132)/2,tr("return_carriers"));}
        if(page==3){districtRevision=configRevision();field(0,16,87,w-32,"");field(1,16,122,(w-40)/3,"0");field(2,20+(w-40)/3,122,(w-40)/3,"0");field(3,24+2*(w-40)/3,122,(w-40)/3,"256");button(225,85,cy,Math.min(170,(w-110)/2),tr("save_district"));button(226,w-Math.min(160,(w-40)/2)-16,cy,Math.min(160,(w-40)/2),tr("delete_district"));}
        if(page==4){button(227,85,cy,Math.min(160,(w-105)/2),tr("surveyor"));button(228,w-Math.min(175,(w-105)/2)-16,cy,Math.min(175,(w-105)/2),tr("resurvey"));}
        if(page==5){button(241,16,85,left-4,tr(supplyTasks?"supply_tasks":"supply_needs"));field(8,left+30,Math.max(87,h-105),(w-left-54)*2/3,supportGroup);field(9,left+36+(w-left-54)*2/3,Math.max(87,h-105),(w-left-54)/3,"0");button(229,85,cy,Math.min(120,(w-110)/3),tr("task_create"));button(230,90+Math.min(120,(w-110)/3),cy,Math.min(105,(w-110)/3),tr("task_kind.ammo"));button(231,w-Math.min(135,(w-110)/3)-16,cy,Math.min(135,(w-110)/3),tr("task_close"));}
        if(page==6){button(232,85,cy,Math.min(100,(w-110)/2),tr(playing?"pause":"play"));}
        if(page==7){
            if(selectedKey.isEmpty())selectedKey=host.ccSelectedAsset();
            NBTTagCompound meta=unit(selectedKey).getCompoundTag("CommandMeta");if(loadFleet){editRevision=configRevision();fleetEdit.clear();fleetEdit.put(0,meta.getString("Title"));fleetEdit.put(1,meta.getString("Group"));fleetEdit.put(2,""+meta.getInteger("Reserve"));fleetEdit.put(3,""+(meta.hasKey("ReserveTier")?meta.getInteger("ReserveTier"):3));district=meta.getString("District");priority=meta.getInteger("Priority");loadFleet=false;}
            field(0,left+30,112,w-left-46,fleetEdit.getOrDefault(0,""));field(1,left+30,150,w-left-46,fleetEdit.getOrDefault(1,""));
            field(2,left+30,112,(w-left-50)/2,fleetEdit.getOrDefault(2,"0"));field(3,left+34+(w-left-50)/2,112,(w-left-50)/2,fleetEdit.getOrDefault(3,"3"));
            fields.get(0).setVisible(formPage==0);fields.get(1).setVisible(formPage==0);fields.get(2).setVisible(formPage==1);fields.get(3).setVisible(formPage==1);
            if(formPage==1){button(234,left+30,139,w-left-46,tr("district_assignment"));button(235,left+30,161,w-left-46,tr("priority."+priority));}
            button(233,85,cy,Math.min(100,(w-110)/3),tr("form_next"));button(236,90+Math.min(100,(w-110)/3),cy,Math.min(110,(w-110)/3),tr("save_fleet"));button(237,w-Math.min(115,(w-110)/3)-16,cy,Math.min(115,(w-110)/3),tr("select_page"));button(254,w-90,79,74,tr("reload_fields"));
        }
        if(page==8)button(238,85,cy,Math.min(210,w-105),tr("change_role"));
    }
    private void field(int id,int bx,int by,int width,String text){GuiTextField f=new GuiTextField(300+id,font,x+bx,y+by,Math.max(30,width),17);f.setMaxStringLength(id==0?64:40);f.setText(text);fields.put(id,f);}
    private NBTTagCompound command(){return host.ccSnapshot().getCompoundTag("Command");}
    private long configRevision(){NBTTagCompound c=command();return c.hasKey("ConfigRevision")?c.getLong("ConfigRevision"):c.getLong("Revision");}
    private String identity(NBTTagCompound row){if(page==8)return row.hasUniqueId("Player")?row.getUniqueId("Player").toString():"";if(page==1)return "contact:"+row.getInteger("Id");if(row.hasUniqueId("Id"))return row.getUniqueId("Id").toString();return row.getString("Key");}
    void accept(){if(selectedId.isEmpty() && selectedRow>=0)selected();data=command();if(data.hasKey("DiagnosticPage"))diagnosticPage=data.getInteger("DiagnosticPage");if(!selectedId.isEmpty()){selectedRow=-1;List<NBTTagCompound> live=rows();for(int i=0;i<live.size();i++)if(selectedId.equals(identity(live.get(i))))selectedRow=i;if(selectedRow<0)notice=tr("selection_missing");}NBTTagCompound snapshot=host.ccSnapshot();if(!snapshot.getString("Error").isEmpty())notice="revision".equals(snapshot.getString("Error")) && snapshot.getString("ResponseAction").startsWith("cc_")?tr("edit_conflict"):I18n.format("planning.error."+snapshot.getString("Error"));else if(snapshot.getString("ResponseAction").startsWith("cc_") || "save".equals(snapshot.getString("ResponseAction")))notice=tr("saved");if(snapshot.getString("Error").isEmpty() && "cc_fleet".equals(snapshot.getString("ResponseAction"))){loadFleet=true;controls();}if(replayTarget!=null && replayTick==0){NBTTagCompound t=data.getCompoundTag("Replay").getCompoundTag("Target");replayTick=t.getLong("Tick");}}
    void showPreflight(){page=2;preflightView=true;detailScroll=0;selectedRow=-1;selectedId="";notice="";}
    void extend(NBTTagCompound request,boolean active){request.setBoolean("Workspace",active);request.setLong("CommandRevision",command().getLong("Revision"));String action=request.getString("Action");if(action.equals("cc_fleet"))request.setLong("ConfigRevision",editRevision);else if(action.equals("cc_district") || action.equals("cc_district_delete"))request.setLong("ConfigRevision",districtRevision);else if(action.equals("cc_role"))request.setLong("ConfigRevision",configRevision());if(!active)return;request.setInteger("WorkspaceTab",page);request.setInteger("WorkspacePage",unitPage);request.setInteger("DiagnosticPage",diagnosticPage);if(diagnosticTarget>=0)request.setInteger("DiagnosticTarget",diagnosticTarget);request.setString("IntelKey",intelKey);if(replayTarget!=null)request.setUniqueId("ReplayTarget",replayTarget);if(page==5 || page==2)request.setTag("Operation",host.ccOperation().write());}
    void tick(){for(GuiTextField f:fields.values())f.updateCursorCounter();if(playing){replayTick+=1;long end=replayEnd();if(replayTick>end){replayTick=end;playing=false;}}}
    private List<NBTTagCompound> list(NBTTagList raw){List<NBTTagCompound> out=new ArrayList<>();for(int i=0;i<raw.tagCount();i++)out.add(raw.getCompoundTagAt(i));return out;}
    private List<NBTTagCompound> rows(){
        List<NBTTagCompound> result=page==0?list(data.getTagList("Alerts",10)):page==1?list(data.getTagList("Contacts",10)):page==3?list(data.getTagList("Districts",10)):page==4?list(data.getCompoundTag("Intel").getTagList("Contacts",10)):page==5?list(data.getTagList(supplyTasks?"Tasks":"Units",10)):page==6?list(data.getTagList("Battles",10)):page==8?list(data.getTagList("Players",10)):list(data.getTagList("Units",10));
        if(page==5 && !supplyTasks)result.removeIf(n->!n.getBoolean("Attack") && !"pvo".equals(n.getString("Kind")) && !(n.getFloat("Capacity")>0 && n.getFloat("Health")<n.getFloat("Capacity")*.95));if(page==0 || page==6)Collections.reverse(result);if(page==1)result.removeIf(n->n.getBoolean("Friendly"));return result;
    }
    private NBTTagCompound selected(){List<NBTTagCompound> rows=rows();if(selectedId.isEmpty() && selectedRow>=0 && selectedRow<rows.size())selectedId=identity(rows.get(selectedRow));for(NBTTagCompound row:rows)if(selectedId.equals(identity(row)))return row;return new NBTTagCompound();}
    private NBTTagCompound unit(String key){for(NBTTagCompound n:list(data.getTagList("Units",10)))if(key.equals(n.getString("Key")))return n;for(NBTTagCompound n:list(host.ccSnapshot().getTagList("Assets",10)))if(key.equals(n.getString("Key")))return n;return new NBTTagCompound();}
    private String name(NBTTagCompound n){return host.ccName(n);}
    private String unitName(String key){NBTTagCompound n=unit(key);return n.hasNoTags()?tr("unknown_unit"):name(n);}
    private boolean operator(){String role=data.getString("Role");return "COMMANDER".equals(role) || "OPERATOR".equals(role);}
    private void send(String action,NBTTagCompound payload){host.ccSend(action,payload);notice=tr("waiting");}
    void action(int id){
        if(id>=200 && id<209){page=id-200;scroll=detailScroll=0;selectedRow=-1;selectedId="";notice="";controls();host.ccFetch();return;}
        if(id==255){preflightView=!preflightView;detailScroll=0;return;}
        if(id==250 || id==251){diagnosticPage=Math.max(0,diagnosticPage+(id==250?-1:1));detailScroll=0;host.ccFetch();return;}
        if(id==254){loadFleet=true;controls();notice=tr("fields_reloaded");return;}
        if(id==209){host.ccClose();return;}
        if(id==210 || id==211){if(page==2 && preflightView){detailScroll=Math.max(0,detailScroll+(id==210?-1:1)*Math.max(1,(bottom-top-10)/34));return;}if(page==7 || page==5){unitPage=Math.max(0,Math.min(Math.max(0,(data.getInteger("UnitTotal")-1)/64),unitPage+(id==210?-1:1)));scroll=0;host.ccFetch();}else scroll=Math.max(0,scroll+(id==210?-visible():visible()));return;}
        if(id==222 || id==223){timelineZoomed=true;timelineSpan=Math.max(200,Math.min(1728000,timelineSpan*(id==222?.5:2)));return;}
        if(id==224 && dragStep>=0){host.ccEdit(dragStep);return;}
        if(id==241){supplyTasks=!supplyTasks;selectedRow=-1;scroll=0;controls();return;}
        if(id==232){playing=!playing;return;}
        if(id==233){formPage=1-formPage;controls();return;}
        if(id==237){for(NBTTagCompound n:list(data.getTagList("Units",10)))if(multi.size()<64)multi.add(n.getString("Key"));return;}
        if(id==221){NBTTagCompound row=selected(),n=unit(row.getString("Asset"));if(row.getBoolean("Position"))host.ccRevealPosition(row);else if(!n.hasNoTags())host.ccReveal(n);return;}
        if(!operator()){notice=tr("read_only");return;}
        if(id==252 || id==253){host.ccControl(id==252?"cancel":"return_carriers");return;}
        if(id==239){host.ccControl("save");return;}
        if(id==240){host.ccControl(host.ccOperation().state==AttackOperation.State.RUNNING?"pause":host.ccOperation().state==AttackOperation.State.PAUSED?"resume":"start");return;}
        NBTTagCompound row=selected(),payload=new NBTTagCompound();
        if(id==220){payload.setString("AlertKey",row.getString("Key"));send("cc_ack",payload);}
        if(id==221){NBTTagCompound n=unit(row.getString("Asset"));if(!n.hasNoTags())host.ccReveal(n);}
        if(id==225){try{NBTTagCompound zone=new NBTTagCompound();if(row.hasUniqueId("Id"))zone.setUniqueId("Id",row.getUniqueId("Id"));zone.setString("Title",fields.get(0).getText());zone.setDouble("X",finite(fields.get(1).getText()));zone.setDouble("Z",finite(fields.get(2).getText()));zone.setInteger("Radius",Integer.parseInt(fields.get(3).getText()));payload.setTag("District",zone);send("cc_district",payload);}catch(NumberFormatException e){notice=tr("numbers");}}
        if(id==226 && row.hasUniqueId("Id")){payload.setUniqueId("DistrictId",row.getUniqueId("Id"));send("cc_district_delete",payload);}
        if(id==234){List<NBTTagCompound> zones=list(data.getTagList("Districts",10));int at=-1;for(int i=0;i<zones.size();i++)if(zones.get(i).getUniqueId("Id").toString().equals(district))at=i;district=at+1>=zones.size()?"":zones.get(at+1).getUniqueId("Id").toString();}
        if(id==235){priority=(priority+1)%3;}
        if(id==236){if(multi.isEmpty() && !selectedKey.isEmpty())multi.add(selectedKey);try{NBTTagList keys=new NBTTagList();for(String key:multi)keys.appendTag(new NBTTagString(key));NBTTagCompound meta=new NBTTagCompound();meta.setString("Title",fields.get(0).getText());meta.setString("Group",fields.get(1).getText());meta.setInteger("Reserve",Integer.parseInt(fields.get(2).getText()));meta.setInteger("ReserveTier",Integer.parseInt(fields.get(3).getText()));meta.setInteger("Priority",priority);meta.setString("District",district);payload.setTag("Fleet",meta);payload.setTag("FleetKeys",keys);payload.setBoolean("Rename",multi.size()==1);send("cc_fleet",payload);}catch(NumberFormatException e){notice=tr("numbers");}}
        if(id==227){List<NBTTagCompound> surveyors=list(data.getTagList("Surveyors",10));int at=-1;for(int i=0;i<surveyors.size();i++)if(selectedKey.equals(surveyors.get(i).getString("Key")))at=i;if(!surveyors.isEmpty())selectedKey=surveyors.get((at+1)%surveyors.size()).getString("Key");}
        if(id==228){NBTTagCompound intel=data.getCompoundTag("IntelDetail"),uav=new NBTTagCompound();for(NBTTagCompound n:list(data.getTagList("Surveyors",10)))if(selectedKey.equals(n.getString("Key")))uav=n;if(intel.hasNoTags() || uav.hasNoTags()){notice=tr("choose_surveyor");return;}host.ccRecon(intel,uav);}
        if(id==230){taskKind=(taskKind+1)%4;}
        if(id==229){String asset=selected().getString(supplyTasks?"Asset":"Key");if(asset.isEmpty()){notice=tr("select_unit");return;}payload.setString("Asset",asset);payload.setString("Kind",new String[]{"ammo","power","repair","return"}[taskKind]);supportGroup=fields.get(8).getText();payload.setString("SupportGroup",supportGroup);try{payload.setLong("Quantity",Long.parseLong(fields.get(9).getText()));}catch(NumberFormatException e){notice=tr("numbers");return;}send("cc_task",payload);}
        if(id==231 && row.hasUniqueId("Id")){payload.setUniqueId("TaskId",row.getUniqueId("Id"));send("cc_task_done",payload);}
        if(id==238 && row.hasUniqueId("Player")){if(!"COMMANDER".equals(data.getString("Role"))){notice=tr("read_only");return;}CommandCenterState.Role role=CommandCenterState.Role.valueOf(row.getString("Role"));payload.setUniqueId("Player",row.getUniqueId("Player"));payload.setString("Role",CommandCenterState.Role.values()[(role.ordinal()+1)%3].name());send("cc_role",payload);}
    }
    private int taskKind;
    private boolean supplyTasks;
    private double finite(String s){double value=Double.parseDouble(s.replace(',','.'));if(!Double.isFinite(value))throw new NumberFormatException();return value;}
    private void text(String text,int bx,int by,int width,int color){GuiTheme.text(font,text,bx,by,Math.max(1,width),color);}
    private void line(String text,int bx,int by,int width,int color){GuiTheme.wrapped(font,text,bx,by,Math.max(1,width),color);}
    void draw(NBTTagCompound snapshot,AttackOperation operation){
        data=snapshot.getCompoundTag("Command");text(tr("page."+page)+" · "+tr("role."+data.getString("Role")),x+16,y+21,w-32,GuiTheme.ACCENT);
        for(GuiButton b:buttons){if(b.id==230)b.displayString=tr("task_kind."+new String[]{"ammo","power","repair","return"}[taskKind]);if(b.id==235)b.displayString=tr("priority."+priority);if(b.id==234)b.displayString=tr("district_assignment")+": "+districtTitle();if(b.id==227){List<NBTTagCompound> surveyors=list(data.getTagList("Surveyors",10));boolean valid=false;for(NBTTagCompound v:surveyors)if(selectedKey.equals(v.getString("Key")))valid=true;if(!valid && !surveyors.isEmpty())selectedKey=surveyors.get(0).getString("Key");b.displayString=tr("surveyor")+": "+unitName(selectedKey);}if(b.id==240)b.displayString=tr(operation.state==AttackOperation.State.RUNNING?"pause":operation.state==AttackOperation.State.PAUSED?"resume":"start");if(Arrays.asList(220,225,226,228,229,231,234,235,236,239,240,252,253).contains(b.id))b.enabled=operator();if(b.id==238)b.enabled="COMMANDER".equals(data.getString("Role")) && selected().hasUniqueId("Player");if(b.id==220)b.enabled=operator() && selected().hasKey("Key");if(b.id==226 || b.id==231)b.enabled=operator() && selected().hasUniqueId("Id");if(b.id==221)b.enabled=selected().getBoolean("Position") || !unit(selected().getString("Asset")).hasNoTags();}
        if(page==2){drawTimeline(operation);return;}
        if(page==3){drawDistricts();return;}
        if(page==0){drawOverview();return;}
        drawTable();
        if(page==1)drawDiagnostics();if(page==4)drawIntel();if(page==5)drawSupplies();if(page==6)drawReplay();if(page==7)drawFleet();if(page==8)drawRoles();
        text(notice,x+16,y+h-64,w-32,GuiTheme.WARN);
    }
    private String districtTitle(){for(NBTTagCompound n:list(data.getTagList("Districts",10)))if(n.getUniqueId("Id").toString().equals(district))return n.getString("Title");return tr("all_districts");}
    private int rowTop(){return top+(page==5?26:0);}
    private int visible(){return Math.max(1,page==0?(bottom-top-76)/22:page==3?(bottom-y-155)/22:page==2?(bottom-top-56)/27:(bottom-rowTop()-10)/27);}
    private void drawTable(){
        GuiTheme.panel(x+12,top-4,left,bottom-top+4);List<NBTTagCompound> rows=rows();scroll=Math.min(scroll,Math.max(0,rows.size()-visible()));
        for(int i=scroll;i<rows.size() && i<scroll+visible();i++){NBTTagCompound n=rows.get(i);int yy=rowTop()+(i-scroll)*27;if(i==selectedRow)drawRect(x+16,yy-2,x+left+8,yy+23,0xff29434a);
            String label=page==1?name(n)+" · T"+n.getInteger("Tier"):page==5 && supplyTasks?tr("task_kind."+n.getString("Kind")):page==8?n.getString("Name"):page==6?name(n):name(n);
            if(page==7)label=(multi.contains(n.getString("Key"))?"☑ ":"□ ")+label;
            text(label,x+20,yy,left-16,GuiTheme.TEXT);
            String detail=page==1?"X "+(int)n.getDouble("X")+" Z "+(int)n.getDouble("Z"):page==4?tr("age",number((host.ccSnapshot().getLong("Tick")-n.getLong("Seen"))/20D)):page==5?supplyTasks?(n.getBoolean("Done")?"✓ ":"→ ")+unitName(n.getString("Asset"))+" · "+n.getLong("Goal"):tr("stock",n.getInteger("Ammo"),n.getLong("Power")):page==8?tr("role."+n.getString("Role")):page==6?tr("episode_count",n.getInteger("Count"),n.getInteger("Intercepted")):n.getCompoundTag("CommandMeta").getString("Group");
            text(detail,x+20,yy+11,left-16,GuiTheme.MUTED);
        }
        if(rows.isEmpty())line(tr("empty"),x+20,top+10,left-16,GuiTheme.MUTED);
    }
    private String alertName(NBTTagCompound row){if(row.hasKey("Name"))return name(row);String asset=row.getString("Asset");if(!asset.isEmpty())return unitName(asset);return row.getString("Title").isEmpty()?tr("alert."+row.getString("Kind")):row.getString("Title");}
    private void drawOverview(){
        NBTTagCompound s=data.getCompoundTag("Summary");String[] keys={"Ready","Threats","Assigned","Empty","Offline","Repair","StaleIntel","Alerts"};int cw=(w-40)/4;
        for(int i=0;i<keys.length;i++){int bx=x+16+(i%4)*(cw+2),by=top+(i/4)*35;GuiTheme.panel(bx,by,cw,31);text(tr("summary."+keys[i]),bx+6,by+4,cw-12,GuiTheme.MUTED);text(""+s.getInteger(keys[i]),bx+6,by+17,cw-12,i==0?GuiTheme.GOOD:GuiTheme.WARN);}
        List<NBTTagCompound> alerts=rows();int by=top+76;for(int i=scroll;i<alerts.size() && by+22<bottom;i++,by+=22){NBTTagCompound n=alerts.get(i);if(i==selectedRow)drawRect(x+16,by-2,x+w-16,by+20,0xff29434a);text((n.getBoolean("Acknowledged")?"✓ ":"! ")+tr("alert."+n.getString("Kind"))+" · "+alertName(n),x+22,by,w-44,GuiTheme.TEXT);if(!n.getString("Reason").isEmpty())text(n.getString("Title")+" · "+I18n.format("planning.error."+n.getString("Reason")),x+22,by+10,w-44,GuiTheme.MUTED);}
        text(notice,x+16,y+h-64,w-32,GuiTheme.WARN);
    }
    private void drawDiagnostics(){int bx=x+left+28,by=top;int width=w-left-44;NBTTagList list=data.getTagList("Diagnostics",10);if(list.tagCount()==0){line(tr("diagnostic_hint"),bx,by,width,GuiTheme.MUTED);return;}
        detailScroll=Math.min(detailScroll,Math.max(0,list.tagCount()-Math.max(1,(bottom-by)/36)));for(int i=detailScroll;i<list.tagCount() && by+35<bottom;i++,by+=36){NBTTagCompound n=list.getCompoundTagAt(i);text(name(n),bx,by,width,GuiTheme.TEXT);text(tr("diagnostic."+n.getString("Diagnostic")),bx,by+11,width,"ready".equals(n.getString("Diagnostic"))?GuiTheme.GOOD:GuiTheme.WARN);text(tr("stage."+n.getCompoundTag("Telemetry").getString("Stage"))+" · "+tr("stock",n.getInteger("Ammo"),n.getLong("Power")),bx,by+22,width,GuiTheme.MUTED);}
    }
    private void drawDistricts(){
        labels(new int[]{0,1,2,3},new String[]{"district_name","x","z","radius"});List<NBTTagCompound> zones=rows();int by=y+155;
        for(int i=scroll;i<zones.size() && by+22<bottom;i++,by+=22){NBTTagCompound n=zones.get(i);if(i==selectedRow)drawRect(x+16,by-2,x+w-16,by+20,0xff29434a);text(n.getString("Title")+" · X "+(int)n.getDouble("X")+" / Z "+(int)n.getDouble("Z")+" · "+n.getInteger("Radius"),x+20,by,w-40,GuiTheme.TEXT);}
    }
    private void drawFleet(){
        text(tr("selection",multi.size()),x+left+30,top,w-left-128,GuiTheme.ACCENT);
        labels(formPage==0?new int[]{0,1}:new int[]{2,3},formPage==0?new String[]{"unit_title","unit_group"}:new String[]{"reserve","reserve_tier"});
        int by=formPage==0?y+181:y+185;if(by+20<bottom)line(tr(formPage==0?"fleet_hint":"doctrine_hint"),x+left+30,by,w-left-46,GuiTheme.MUTED);
    }
    private void drawIntel(){
        NBTTagCompound n=data.getCompoundTag("IntelDetail");int bx=x+left+28,by=top,width=w-left-44;
        if(n.hasNoTags()){line(tr("intel_hint"),bx,by,width,GuiTheme.MUTED);return;}
        text(name(n),bx,by,width,GuiTheme.TEXT);by+=14;text(tr("age",number((host.ccSnapshot().getLong("Tick")-n.getLong("Seen"))/20D)),bx,by,width,GuiTheme.WARN);by+=14;
        line(tr("intel_source",I18n.format("planning.sensor."+n.getString("Sensor")),(int)(n.getFloat("Quality")*100)),bx,by,width,GuiTheme.MUTED);by+=26;text(tr(n.getBoolean("Confirmed")?"confirmed":"unconfirmed"),bx,by,width,GuiTheme.ACCENT);by+=17;
        int[] patch=n.getIntArray("Patch");int size=Math.min(72,Math.max(32,width/3));if(patch.length==16 && by+size+14<bottom){for(int row=0;row<4;row++)for(int col=0;col<4;col++){int c=patch[row*4+col];drawRect(bx+col*size/4,by+row*size/4,bx+(col+1)*size/4,by+(row+1)*size/4,c==0?0xff26373d:0xff000000|c);}text(tr("saved_view"),bx+size+8,by,width-size-8,GuiTheme.MUTED);by+=size+10;}
        NBTTagList history=n.getTagList("History",10);for(int i=history.tagCount()-1-detailScroll;i>=0 && by+12<bottom;i--,by+=12){NBTTagCompound old=history.getCompoundTagAt(i);text(number(old.getLong("Seen")/20D)+"s · X "+(int)old.getDouble("X")+" Z "+(int)old.getDouble("Z"),bx,by,width,GuiTheme.MUTED);}
    }
    private void drawSupplies(){
        int bx=x+left+28,by=top,width=w-left-44;NBTTagList requirements=data.getTagList("Supplies",10);
        text(tr("supply_operation",host.ccOperation().title),bx,by,width,GuiTheme.ACCENT);by+=16;
        List<String> lines=detailLines;lines.clear();for(int i=0;i<requirements.tagCount();i++){NBTTagCompound n=requirements.getCompoundTagAt(i);lines.add(name(n)+" · "+tr("ammo_need",n.getInteger("RequiredAmmo"),n.getInteger("MissingAmmo")));lines.add(n.getBoolean("UnknownCarrierPower")?tr("supply_unknown"):tr("power_need",n.getLong("RequiredPower"),n.getLong("MissingPower")));NBTTagList stores=n.getTagList("RequiredStores",10);for(int j=0;j<stores.tagCount();j++){NBTTagCompound item=stores.getCompoundTagAt(j);String label=item.getString("Name");label=label.isEmpty()?tr("unknown_unit"):I18n.format(label+".name");lines.add(tr("store_need",item.getInteger("Slot")+1,label,item.getInteger("Required"),item.getInteger("Missing")));}}
        int visible=Math.max(1,(y+h-120-by)/13);detailScroll=Math.min(detailScroll,Math.max(0,lines.size()-visible));for(int i=detailScroll;i<lines.size() && i<detailScroll+visible;i++,by+=13)text(lines.get(i),bx,by,width,GuiTheme.TEXT);
        labels(new int[]{8,9},new String[]{"support_group","task_quantity"});
    }

    private void drawRoles(){int bx=x+left+28;line(tr("roles_hint"),bx,top,w-left-44,GuiTheme.MUTED);}
    private void labels(int[] ids,String[] labels){for(int i=0;i<ids.length;i++){GuiTextField f=fields.get(ids[i]);if(f!=null && f.getVisible())line(tr(labels[i]),f.x,f.y-12,f.width,GuiTheme.MUTED);}}
    void drawFields(){for(GuiTextField f:fields.values())if(f.getVisible())f.drawTextBox();}
    boolean key(char character,int key){for(GuiTextField f:fields.values())if(f.getVisible() && f.textboxKeyTyped(character,key))return true;return false;}
    void wheel(int delta,int mx,int my){if(delta==0)return;if(((page==1 || page==4 || page==5) && mx>x+left+16 || page==2 && preflightView))detailScroll=Math.max(0,detailScroll+(delta>0?-1:1));else scroll=Math.max(0,scroll+(delta>0?-1:1));}
    void wheel(int delta){wheel(delta,x,top);}
    void click(int mx,int my,int button){
        for(GuiTextField f:fields.values())if(f.getVisible())f.mouseClicked(mx,my,button);
        if(page==2){if(preflightView && my>=top+10 && my<bottom){NBTTagList rows=data.getTagList("Preflight",10);int index=detailScroll+(my-top-10)/34;if(index>=0 && index<rows.tagCount()){java.util.UUID id=rows.getCompoundTagAt(index).getUniqueId("Step");for(int i=0;i<host.ccOperation().steps.size();i++)if(id.equals(host.ccOperation().steps.get(i).id))dragStep=i;}}else timelineClick(mx,my,button);return;}
        if(page==6 && mx>x+left+28 && my>bottom-14 && my<bottom){seek(mx);return;}
        int row=-1;
        if(page==0 && mx>x+16 && mx<x+w-16 && my>=top+76 && my<bottom)row=scroll+(my-top-76)/22;
        else if(page==3 && mx>x+16 && mx<x+w-16 && my>=y+155 && my<bottom)row=scroll+(my-y-155)/22;
        else if(mx>x+12 && mx<x+left+12 && my>=rowTop() && my<bottom)row=scroll+(my-rowTop())/27;
        List<NBTTagCompound> rows=rows();if(row<0 || row>=rows.size())return;selectedRow=row;detailScroll=0;NBTTagCompound selected=rows.get(row);selectedId=identity(selected);
        if(page==1){diagnosticTarget=selected.getInteger("Id");host.ccFetch();}
        if(page==3){districtRevision=configRevision();fields.get(0).setText(selected.getString("Title"));fields.get(1).setText(""+(int)selected.getDouble("X"));fields.get(2).setText(""+(int)selected.getDouble("Z"));fields.get(3).setText(""+selected.getInteger("Radius"));}
        if(page==4){intelKey=selected.getString("Key");host.ccFetch();}
        if(page==5)selectedKey=selected.getString(supplyTasks?"Asset":"Key");
        if(page==6){replayTarget=selected.getUniqueId("Id");replayTick=selected.getLong("Tick");playing=false;host.ccFetch();}
        if(page==7){loadFleet=true;selectedKey=selected.getString("Key");if(!GuiScreen.isCtrlKeyDown())multi.clear();if(!multi.add(selectedKey) && GuiScreen.isCtrlKeyDown())multi.remove(selectedKey);controls();}
    }
    // Timeline and replay share small, bounded map primitives and never inspect the live world.
    private void drawTimeline(AttackOperation operation){drawTimelineGraph(operation);}
    private void drawReplay(){drawReplayMap();}
    private final List<int[]> bars=new ArrayList<>();
    private final Map<UUID,Long> durations=new HashMap<>(),estimates=new HashMap<>();
    private int timelineLeft,timelineTop,timelineWidth;
    private void drawTimelineGraph(AttackOperation operation){
        if(preflightView){bars.clear();NBTTagList report=data.getTagList("Preflight",10);int count=Math.max(1,(bottom-top-10)/34);detailScroll=Math.min(detailScroll,Math.max(0,report.tagCount()-count));for(int i=detailScroll;i<report.tagCount() && i<detailScroll+count;i++){NBTTagCompound row=report.getCompoundTagAt(i);int by=top+10+(i-detailScroll)*34;String error=row.getString("Error"),waiting=row.getString("Waiting");text(row.getInteger("Number")+" · "+unitName(row.getString("Asset")),x+20,by,w-40,GuiTheme.TEXT);text(error.isEmpty()?waiting.isEmpty()?tr("preflight_ok"):tr("preflight_wait",I18n.format("planning.error."+waiting)):I18n.format("planning.error."+error),x+20,by+12,w-40,error.isEmpty()?GuiTheme.GOOD:GuiTheme.WARN);}return;}

        bars.clear();durations.clear();estimates.clear();NBTTagList preview=data.getTagList("Timeline",10);for(int i=0;i<preview.tagCount();i++){NBTTagCompound n=preview.getCompoundTagAt(i);if(n.hasUniqueId("Id")){durations.put(n.getUniqueId("Id"),n.getLong("Duration"));estimates.put(n.getUniqueId("Id"),n.getLong("Flight"));}}
        for(AttackOperation.Step s:operation.steps)durations.putIfAbsent(s.id,OperationTimeline.duration(s));
        if(!operation.id.equals(timelineOperation)){timelineOperation=operation.id;timelineZoomed=false;}if(!timelineZoomed){long end=200;for(AttackOperation.Step step:operation.steps)end=Math.max(end,OperationTimeline.start(operation,step,durations,estimates)+durations.get(step.id));timelineSpan=Math.max(200,Math.min(1728000,Math.ceil(end*1.2/200)*200));}
        timelineLeft=x+Math.min(170,w/3);timelineTop=top+46;timelineWidth=x+w-16-timelineLeft;int height=bottom-timelineTop-22;
        GuiTheme.panel(x+16,timelineTop-19,w-32,height+22);text(operation.title,x+20,top,w-175,GuiTheme.ACCENT);
        for(int i=0;i<=5;i++){int gx=timelineLeft+(int)(timelineWidth*i/5D);drawRect(gx,timelineTop,gx+1,timelineTop+height,0x5548666b);text(number(timelineSpan*i/100D)+"s",gx-10,timelineTop-13,70,GuiTheme.MUTED);}
        List<String> lanes=new ArrayList<>();for(AttackOperation.Step step:operation.steps)if(!lanes.contains(step.asset))lanes.add(step.asset);int visible=Math.max(1,height/27);scroll=Math.min(scroll,Math.max(0,lanes.size()-visible));
        for(int i=scroll;i<lanes.size() && i<scroll+visible;i++){int by=timelineTop+(i-scroll)*27;text(unitName(lanes.get(i)),x+20,by+5,timelineLeft-x-26,GuiTheme.TEXT);drawRect(timelineLeft,by+24,x+w-16,by+25,0x4448666b);}
        Set<UUID> conflict=OperationTimeline.conflicts(operation,durations,estimates);
        for(int i=0;i<operation.steps.size();i++){AttackOperation.Step step=operation.steps.get(i);int lane=lanes.indexOf(step.asset);if(lane<scroll || lane>=scroll+visible)continue;long start=OperationTimeline.start(operation,step,durations,estimates);int bx=timelineLeft+(int)(start*timelineWidth/timelineSpan),by=timelineTop+(lane-scroll)*27;int end=timelineLeft+(int)((start+durations.get(step.id))*timelineWidth/timelineSpan);end=Math.max(bx+9,end);if(bx>=x+w-16 || end<timelineLeft)continue;bx=Math.max(timelineLeft,bx);end=Math.min(x+w-16,end);
            int color=step.phase==AttackOperation.Phase.DONE?0xff467e62:step.phase==AttackOperation.Phase.FAILED || conflict.contains(step.id)?0xffa95848:step.condition!=AttackOperation.Condition.NONE?0xff785b8f:0xff376b7f;
            drawRect(bx,by+2,end,by+21,color);text((i+1)+" "+I18n.format("planning.action."+step.action.name()),bx+3,by+7,Math.max(1,end-bx-5),GuiTheme.TEXT);drawRect(end-3,by+2,end,by+21,0xffb8d9df);bars.add(new int[]{i,bx,by+2,end,by+21});
            if(step.actual>=0 && operation.started>=0){int actual=timelineLeft+(int)((step.actual-operation.started)*timelineWidth/timelineSpan);if(actual>=timelineLeft && actual<x+w-16)drawRect(actual,by,actual+2,by+23,0xffffd083);}
        }
        for(int[] bar:bars){AttackOperation.Step step=operation.steps.get(bar[0]);if(step.after==null)continue;for(int[] parent:bars)if(operation.steps.get(parent[0]).id.equals(step.after)){segment(parent[3],parent[2]+9,bar[1],bar[2]+9,0xffddc58a,timelineLeft,timelineTop,x+w-16,bottom);break;}}
        if(operation.started>=0){long tick=operation.state==AttackOperation.State.PAUSED?operation.pausedAt:host.ccSnapshot().getLong("Tick");int cursor=timelineLeft+(int)((tick-operation.started)*timelineWidth/timelineSpan);if(cursor>=timelineLeft && cursor<x+w-16)drawRect(cursor,timelineTop,cursor+1,timelineTop+height,0xffec9c75);}
        NBTTagList check=data.getTagList("Preflight",10);int issues=0;String first="";for(int i=0;i<check.tagCount();i++){NBTTagCompound item=check.getCompoundTagAt(i);if(!item.getString("Error").isEmpty()){issues++;if(first.isEmpty())first=tr("preflight_issue",item.getInteger("Number"),I18n.format("planning.error."+item.getString("Error")));}}text(issues==0?tr("preflight_ok"):first,x+20,top+12,w-175,issues==0?GuiTheme.GOOD:GuiTheme.WARN);
        text(tr("timeline_hint"),x+20,bottom-14,w-40,GuiTheme.MUTED);text(notice,x+16,y+h-64,w-32,GuiTheme.WARN);
    }
    private void timelineClick(int mx,int my,int button){
        if(host.ccOperation().state!=AttackOperation.State.DRAFT || !operator())return;
        for(int[] bar:bars)if(mx>=bar[1] && mx<=bar[3] && my>=bar[2] && my<=bar[4]){dragStep=bar[0];AttackOperation.Step step=host.ccOperation().steps.get(dragStep);
            if(button==1){if(GuiScreen.isShiftKeyDown()){step.after=null;host.ccDirty();linkFrom=null;return;}if(linkFrom==null){linkFrom=step.id;notice=tr("choose_dependency");}else if(!linkFrom.equals(step.id)){UUID old=step.after;step.after=linkFrom;if(!host.ccOperation().validate().isEmpty()){step.after=old;notice=tr("cycle");}else{step.event=AttackOperation.Event.ACTUAL;host.ccDirty();notice=tr("linked");}linkFrom=null;}return;}
            timelineZoomed=true;dragX=mx;dragOffset=step.offset;dragCount=step.count;stretch=GuiScreen.isShiftKeyDown() || mx>=bar[3]-4;return;
        }
    }
    void drag(int mx,int my,int button){if(page!=2 || dragX==0 || dragStep<0 || button!=0)return;AttackOperation.Step step=host.ccOperation().steps.get(dragStep);long delta=Math.round((mx-dragX)*timelineSpan/Math.max(1,timelineWidth));if(stretch){NBTTagCompound a=unit(step.asset);int max=a.getString("Kind").equals("mlrs") || a.getString("Kind").equals("aircraft") || a.getString("Kind").equals("uav")?40:1;step.count=Math.max(1,Math.min(max,dragCount+(int)(delta/Math.max(1,step.interval))));}else step.offset=Math.max(0,Math.min(1728000,dragOffset+delta));}
    void release(int mx,int my){if(dragX!=0){host.ccDirty();host.ccFetch();}dragX=0;}
    private long replayEnd(){NBTTagCompound replay=data.getCompoundTag("Replay");NBTTagList events=replay.getTagList("Events",10);long end=replay.getCompoundTag("Target").getLong("Tick");for(int i=0;i<events.tagCount();i++)end=Math.max(end,events.getCompoundTagAt(i).getLong("Tick"));return end;}
    private void seek(int mx){NBTTagCompound target=data.getCompoundTag("Replay").getCompoundTag("Target");long begin=target.getLong("Tick"),end=replayEnd();replayTick=begin+(long)(Math.max(0,Math.min(1,(mx-(x+left+28))/(double)Math.max(1,w-left-44)))*(end-begin));playing=false;}
    private void drawReplayMap(){
        NBTTagCompound replay=data.getCompoundTag("Replay"),target=replay.getCompoundTag("Target");int bx=x+left+28,width=w-left-44,by=top+5,height=Math.max(35,bottom-by-53);
        if(target.hasNoTags()){line(tr("replay_hint"),bx,by,width,GuiTheme.MUTED);return;}
        List<NBTTagCompound> paths=new ArrayList<>(list(replay.getTagList("Targets",10)));if(paths.isEmpty())paths.add(target);int hostileCount=paths.size();paths.addAll(list(replay.getTagList("Flights",10)));double minX=Double.POSITIVE_INFINITY,minZ=minX,maxX=Double.NEGATIVE_INFINITY,maxZ=maxX;
        for(NBTTagCompound path:paths)for(NBTTagCompound p:list(path.getTagList("Trace",10))){minX=Math.min(minX,p.getDouble("X"));maxX=Math.max(maxX,p.getDouble("X"));minZ=Math.min(minZ,p.getDouble("Z"));maxZ=Math.max(maxZ,p.getDouble("Z"));}
        if(!Double.isFinite(minX)){minX=target.getDouble("X")-50;maxX=minX+100;minZ=target.getDouble("Z")-50;maxZ=minZ+100;}
        double span=Math.max(100,Math.max(maxX-minX,(maxZ-minZ)*width/height))+80,cx=(minX+maxX)*.5,cz=(minZ+maxZ)*.5;
        GuiTheme.panel(bx,by,width,height);for(int i=1;i<6;i++){drawRect(bx+width*i/6,by,bx+width*i/6+1,by+height,0x44416d70);drawRect(bx,by+height*i/6,bx+width,by+height*i/6+1,0x44416d70);}
        for(int index=0;index<paths.size();index++){NBTTagList trace=paths.get(index).getTagList("Trace",10);int[] previous=null;for(int i=0;i<trace.tagCount();i++){NBTTagCompound p=trace.getCompoundTagAt(i);if(p.getLong("Tick")>replayTick)break;int px=bx+width/2+(int)((p.getDouble("X")-cx)*width/span),py=by+height/2+(int)((p.getDouble("Z")-cz)*width/span);if(previous!=null)segment(previous[0],previous[1],px,py,index<hostileCount?0xffee9b7c:0xff9adca5,bx,by,bx+width,by+height);drawRect(px-2,py-2,px+3,py+3,index<hostileCount?0xffee9b7c:0xff9adca5);previous=new int[]{px,py};}}
        NBTTagList events=replay.getTagList("Events",10);NBTTagCompound active=new NBTTagCompound();for(int i=0;i<events.tagCount();i++)if(events.getCompoundTagAt(i).getLong("Tick")<=replayTick)active=events.getCompoundTagAt(i);
        text(tr("replay_time",number((replayTick-target.getLong("Tick"))/20D))+" · "+tr("outcome."+active.getString("Outcome")),bx,by+height+7,width,GuiTheme.ACCENT);
        drawRect(bx,bottom-12,bx+width,bottom-8,0xff4a6970);long end=replayEnd(),begin=target.getLong("Tick");int cursor=bx+(int)(Math.max(0,Math.min(1,(replayTick-begin)/(double)Math.max(1,end-begin)))*width);drawRect(cursor-2,bottom-15,cursor+3,bottom-5,0xffffd083);
    }
    void tooltip(int mx,int my){if(page==1 && mx>x+left+16 && my>=top && my<bottom){NBTTagList rows=data.getTagList("Diagnostics",10);int index=detailScroll+(my-top)/36;if(index>=0 && index<rows.tagCount()){NBTTagCompound row=rows.getCompoundTagAt(index);host.ccTooltip(Arrays.asList(name(row),tr("diagnostic."+row.getString("Diagnostic")),tr("stock",row.getInteger("Ammo"),row.getLong("Power"))),mx,my);}}if(page==5 && mx>x+left+16 && my>=top+16 && my<y+h-120){int index=detailScroll+(my-top-16)/13;if(index>=0 && index<detailLines.size())host.ccTooltip(Collections.singletonList(detailLines.get(index)),mx,my);}if(page==2 && preflightView && my>=top+10 && my<bottom){NBTTagList rows=data.getTagList("Preflight",10);int index=detailScroll+(my-top-10)/34;if(index>=0 && index<rows.tagCount()){NBTTagCompound row=rows.getCompoundTagAt(index);String reason=row.getString("Error").isEmpty()?row.getString("Waiting"):row.getString("Error");host.ccTooltip(Arrays.asList(unitName(row.getString("Asset")),reason.isEmpty()?tr("preflight_ok"):I18n.format("planning.error."+reason)),mx,my);}}if(mx>x+12 && mx<x+left+12 && my>=rowTop() && my<bottom && page!=2 && page!=3){int index=scroll+(my-rowTop())/27;List<NBTTagCompound> rows=rows();if(index>=0 && index<rows.size()){NBTTagCompound n=rows.get(index);List<String> lines=new ArrayList<>();lines.add(page==8?n.getString("Name"):page==0?alertName(n):name(n));if(n.hasKey("Asset"))lines.add(unitName(n.getString("Asset")));if(n.hasKey("Goal"))lines.add(tr("task_progress",n.getLong("Baseline"),n.getLong("Goal")));if(!n.getString("Reason").isEmpty())lines.add(I18n.format("planning.error."+n.getString("Reason")));if(n.hasKey("ReadyReason") && !n.getString("ReadyReason").isEmpty())lines.add(I18n.format("planning.error."+n.getString("ReadyReason")));host.ccTooltip(lines,mx,my);}}if(page==2)for(int[] bar:bars)if(mx>=bar[1] && mx<=bar[3] && my>=bar[2] && my<=bar[4]){AttackOperation.Step s=host.ccOperation().steps.get(bar[0]);List<String> lines=new ArrayList<>();lines.add(unitName(s.asset));lines.add(I18n.format("planning.action."+s.action.name())+" · "+s.count);lines.add("X "+s.x+" Y "+s.y+" Z "+s.z);if(!s.reason.isEmpty())lines.add(I18n.format("planning.error."+s.reason));host.ccTooltip(lines,mx,my);}}
    private void segment(int ax,int ay,int bx,int by,int color,int left,int top,int right,int bottom){
        double dx=bx-(double)ax,dy=by-(double)ay,lo=0,hi=1;double[] p={-dx,dx,-dy,dy},q={ax-left,right-ax,ay-top,bottom-ay};
        for(int i=0;i<4;i++){if(Math.abs(p[i])<.00001){if(q[i]<0)return;}else{double t=q[i]/p[i];if(p[i]<0)lo=Math.max(lo,t);else hi=Math.min(hi,t);if(lo>hi)return;}}
        net.minecraft.client.renderer.GlStateManager.enableBlend();net.minecraft.client.renderer.GlStateManager.disableTexture2D();net.minecraft.client.renderer.GlStateManager.tryBlendFuncSeparate(770,771,1,0);
        net.minecraft.client.renderer.BufferBuilder b=net.minecraft.client.renderer.Tessellator.getInstance().getBuffer();b.begin(1,net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION_COLOR);int r=(color>>16)&255,g=(color>>8)&255,blue=color&255,a=(color>>>24)&255;
        b.pos(ax+dx*lo,ay+dy*lo,0).color(r,g,blue,a).endVertex();b.pos(ax+dx*hi,ay+dy*hi,0).color(r,g,blue,a).endVertex();net.minecraft.client.renderer.Tessellator.getInstance().draw();net.minecraft.client.renderer.GlStateManager.enableTexture2D();net.minecraft.client.renderer.GlStateManager.disableBlend();net.minecraft.client.renderer.GlStateManager.color(1,1,1,1);
    }
}
