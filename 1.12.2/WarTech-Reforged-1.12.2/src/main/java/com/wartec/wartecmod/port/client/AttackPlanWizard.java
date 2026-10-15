package com.wartec.wartecmod.port.client;

import com.wartec.wartecmod.port.planning.*;
import java.util.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraft.util.ResourceLocation;

/** A guided draft editor within the original authenticated container session. */
final class AttackPlanWizard {
    private final GuiPlanningTerminal host;
    private FontRenderer font;
    private int left,top,width,height,page,listPage;
    private final Map<Integer,GuiTextField> fields=new LinkedHashMap<>();
    private List<GuiButton> buttons;
    private String assetKey="",weaponKey="",message="",title="",query="";
    private int x,y=64,z,quantity=1,interval=20;
    private long delay;
    private boolean targetSet,returnHome=true;
    UUID importId=UUID.randomUUID();
    boolean active,mapPicking;
    AttackPlanWizard(GuiPlanningTerminal host){this.host=host;}
    private static String tr(String key,Object... args){return I18n.format("planning.wizard."+key,args);}
    void open(String selected,int tx,int ty,int tz,boolean hasTarget){
        if(!active){active=true;importId=UUID.randomUUID();page=0;listPage=0;assetKey=selected;weaponKey="";title=host.ccOperation().title;x=tx;y=ty;z=tz;targetSet=hasTarget;quantity=1;delay=0;interval=20;returnHome=true;message="";query="";}
        if(mapPicking){x=tx;y=ty;z=tz;targetSet=hasTarget;page=2;mapPicking=false;}
    }
    void imported(NBTTagCompound target){x=target.getInteger("X");y=target.getInteger("Y");z=target.getInteger("Z");targetSet=true;page=2;message=tr("imported");}
    private NBTTagCompound asset(){NBTTagList rows=host.ccSnapshot().getTagList("Assets",10);for(int i=0;i<rows.tagCount();i++)if(assetKey.equals(rows.getCompoundTagAt(i).getString("Key")))return rows.getCompoundTagAt(i);return new NBTTagCompound();}
    private List<NBTTagCompound> assets(){List<NBTTagCompound> result=new ArrayList<>();NBTTagList rows=host.ccSnapshot().getTagList("Assets",10);for(int i=0;i<rows.tagCount();i++){NBTTagCompound n=rows.getCompoundTagAt(i);String actions=","+n.getString("Actions")+",";if(n.getBoolean("Attack") && (actions.contains(",LAUNCH,") || actions.contains(",RELEASE,")) && (query.isEmpty() || host.ccName(n).toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT))))result.add(n);}return result;}
    private List<NBTTagCompound> weapons(){return AttackPlanBuilder.weapons(asset(),host.ccOperation());}
    private NBTTagCompound weapon(){for(NBTTagCompound n:weapons())if(weaponKey.equals(n.getString("WeaponKey")))return n;return new NBTTagCompound();}
    private ItemStack stack(NBTTagCompound n){if(n.hasKey("Stack",10))return new ItemStack(n.getCompoundTag("Stack"));Item item=Item.REGISTRY.getObject(new ResourceLocation(n.getString("Item")));return item==null?ItemStack.EMPTY:new ItemStack(item,1,n.getInteger("Meta"));}
    private String weaponName(NBTTagCompound n){if(!n.getString("Item").isEmpty()){ItemStack stack=stack(n);if(!stack.isEmpty())return stack.getDisplayName();}String name=n.getString("Name");return I18n.hasKey(name)?I18n.format(name):I18n.hasKey(name+".name")?I18n.format(name+".name"):name;}
    private void button(int id,int x,int y,int w,int h,String label){buttons.add(new PlanningButton(id,left+x,top+y,w,h,label));}
    private void field(int id,int x,int y,int w,String value){GuiTextField f=new GuiTextField(id,font,left+x,top+y,w,18);f.setMaxStringLength(id==0?64:32);f.setText(value);fields.put(id,f);}
    void layout(FontRenderer font,int left,int top,int width,int height,List<GuiButton> buttons){this.font=font;this.left=left;this.top=top;this.width=width;this.height=height;this.buttons=buttons;controls();}
    private int visibleRows(){return Math.max(1,(height-174)/30);}
    private void controls(){
        buttons.clear();fields.clear();int half=(width-40)/2;
        for(int i=0;i<4;i++){button(330+i,12+i*(width-24)/4,43,(width-24)/4-3,20,(i==page?"> ":"")+(i+1)+". "+tr("page."+i));buttons.get(buttons.size()-1).enabled=i<=page+1;}
        if(page<2){List<NBTTagCompound> rows=page==0?assets():weapons();int visible=visibleRows();listPage=Math.max(0,Math.min(Math.max(0,(rows.size()-1)/visible),listPage));
            if(page==0)field(0,12,80,width-24,query);
            for(int i=0;i<visible && i+listPage*visible<rows.size();i++){NBTTagCompound n=rows.get(i+listPage*visible);boolean selected=page==0?assetKey.equals(n.getString("Key")):weaponKey.equals(n.getString("WeaponKey"));
                String label=page==0?host.ccName(n)+" · "+I18n.format("planning.ammo",n.getInteger("Ammo")):weaponName(n)+" · "+tr("available",n.getInteger("Available"));button(350+i,36,104+i*30,width-48,26,(selected?"> ":"")+label);}
            button(320,12,height-63,26,18,"<");button(321,42,height-63,26,18,">");
            if(page==0){button(322,75,height-63,half-70,18,tr("search"));button(323,half+20,height-63,half,18,tr("asset_page"));}
        }else if(page==2){int col=(width-48)/3;for(int i=0;i<3;i++)field(i+1,12+i*(col+12),105,col,!targetSet && i!=1?"":""+(i==0?x:i==1?y:z));
            button(310,12,135,half,22,tr("designator"));button(311,half+28,135,half,22,tr("map"));button(312,12,163,width-24,22,tr("paste"));
        }else {int shift=height<285?14:0;field(0,12,94-shift,width-24,title);field(4,12,137-shift,half,""+quantity);field(5,half+28,137-shift,half,String.format(Locale.ROOT,"%.1f",delay/20D));field(6,12,178-shift,half,String.format(Locale.ROOT,"%.1f",interval/20D));if(asset().getString("Actions").contains("RELEASE") && !asset().getBoolean("OneWay"))button(313,half+28,178-shift,half,18,tr(returnHome?"return_on":"return_off"));}
        button(300,12,height-27,half,20,tr(page==0?"close":"back"));button(301,half+28,height-27,half,20,tr(page==3?"add":"next"));
    }
    private boolean read(){try{
        if(page==0 && fields.containsKey(0))query=fields.get(0).getText();
        if(page==2){x=Integer.parseInt(fields.get(1).getText().trim());y=Integer.parseInt(fields.get(2).getText().trim());z=Integer.parseInt(fields.get(3).getText().trim());targetSet=true;}
        if(page==3){title=fields.get(0).getText().trim();quantity=Integer.parseInt(fields.get(4).getText().trim());delay=ticks(fields.get(5).getText());interval=(int)ticks(fields.get(6).getText());}
        return true;
    }catch(NumberFormatException e){message=I18n.format("planning.error.numbers");return false;}}
    private long ticks(String text){double seconds=Double.parseDouble(text.trim().replace(',','.'));if(!Double.isFinite(seconds) || seconds<0 || seconds>86400)throw new NumberFormatException();return Math.round(seconds*20);}
    private boolean canAdvance(){if(page==0 && asset().hasNoTags()){message=tr("select_unit");return false;}if(page==1 && weapon().hasNoTags()){message=tr("select_weapon");return false;}if(page==2 && (!targetSet || Math.abs((long)x)>29999984 || Math.abs((long)z)>29999984 || y<0 || y>255)){message=I18n.format("planning.error.coordinates");return false;}return true;}
    void action(int id){
        if(id==300){if(page==0){active=false;host.ccClose();}else{read();page--;listPage=0;controls();}return;}
        if(id==310){read();host.wizardDesignator();return;}
        if(id==311){read();message="";mapPicking=true;host.ccClose();return;}
        if(id==312){try{String[] xyz=GuiScreen.getClipboardString().trim().split("[\\s,;]+");if(xyz.length!=3)throw new NumberFormatException();x=Integer.parseInt(xyz[0]);y=Integer.parseInt(xyz[1]);z=Integer.parseInt(xyz[2]);targetSet=true;message=tr("imported");controls();}catch(NumberFormatException e){message=tr("paste_error");}return;}
        if(!read())return;
        if(id==313){returnHome=!returnHome;controls();return;}
        if(id==322){listPage=0;controls();return;}
        if(id==323){host.wizardAssetPage();return;}
        if(id==320 || id==321){listPage=Math.max(0,listPage+(id==320?-1:1));controls();return;}
        if(id>=350){List<NBTTagCompound> rows=page==0?assets():weapons();int index=id-350+listPage*visibleRows();if(index<rows.size()){if(page==0){assetKey=rows.get(index).getString("Key");weaponKey="";quantity=1;}else weaponKey=rows.get(index).getString("WeaponKey");message="";controls();}return;}
        if(id>=330 && id<=333){int next=id-330;if(next<=page){page=next;controls();}else if(next==page+1 && canAdvance()){page=next;listPage=0;controls();}return;}
        if(id==301){if(!canAdvance())return;if(page<3){page++;listPage=0;message="";controls();return;}
            try{List<AttackOperation.Step> steps=AttackPlanBuilder.build(host.ccOperation(),asset(),weaponKey,quantity,x,y,z,delay,interval,returnHome);if(title.isEmpty()){message=tr("name_required");return;}host.ccOperation().title=title;host.wizardApply(steps);active=false;}catch(IllegalArgumentException e){message=I18n.format("planning."+e.getMessage());}}
    }
    void draw(){
        GuiTheme.text(font,tr("title"),left+12,top+29,width-24,GuiTheme.ACCENT);
        if(page==0)label(tr("find"),12,68,width-24);if(page==1)label(tr("ammo_hint"),12,76,width-24);
        if(page<2){List<NBTTagCompound> rows=page==0?assets():weapons();if(rows.isEmpty())GuiTheme.wrapped(font,tr(page==0?"no_units":"no_ammo"),left+12,top+107,width-24,GuiTheme.WARN);
            else for(int i=0;i<visibleRows() && i+listPage*visibleRows()<rows.size();i++){NBTTagCompound row=rows.get(i+listPage*visibleRows());if(page==1 && !row.getString("Item").isEmpty())host.wizardIcon(stack(row),left+14,top+109+i*30);}
            if(page==1)GuiTheme.text(font,host.ccName(asset()),left+80,top+height-59,width-92,GuiTheme.MUTED);
        }else if(page==2){int col=(width-48)/3;for(int i=0;i<3;i++)label(i==0?"X":i==1?tr("height"):"Z",12+i*(col+12),90,col);if(height>285)GuiTheme.wrapped(font,tr("target_hint"),left+12,top+196,width-24,GuiTheme.MUTED);
        }else {int half=(width-40)/2,shift=height<285?14:0;label(tr("name"),12,78-shift,width-24);label(tr("quantity",weapon().getInteger("Available")),12,121-shift,half);label(tr(previous()?"delay_previous":"delay"),half+28,121-shift,half);label(tr("interval"),12,162-shift,half);
            if(height>340){String sequence=host.ccName(asset())+" → "+weaponName(weapon())+" ×"+fields.get(4).getText()+" → X "+x+" Y "+y+" Z "+z; GuiTheme.wrapped(font,sequence,left+12,top+210,width-24,GuiTheme.ACCENT);GuiTheme.wrapped(font,tr("automatic"),left+12,top+246,width-24,GuiTheme.MUTED);}}
        label(message.isEmpty()?tr("hint."+page):message,12,height-43,width-24);
    }
    private boolean previous(){for(AttackOperation.Step s:host.ccOperation().steps)if(s.asset.equals(assetKey))return true;return false;}
    private void label(String text,int x,int y,int w){float scale=Math.min(1,w/(float)Math.max(1,font.getStringWidth(text)));net.minecraft.client.renderer.GlStateManager.pushMatrix();net.minecraft.client.renderer.GlStateManager.translate(left+x,top+y,0);net.minecraft.client.renderer.GlStateManager.scale(scale,scale,1);font.drawString(text,0,0,GuiTheme.MUTED);net.minecraft.client.renderer.GlStateManager.popMatrix();}
    void fields(){for(GuiTextField field:fields.values())field.drawTextBox();}
    void click(int x,int y,int button){for(GuiTextField field:fields.values())field.mouseClicked(x,y,button);}
    boolean key(char character,int key){for(GuiTextField field:fields.values())if(field.textboxKeyTyped(character,key))return true;return false;}
    void tick(){for(GuiTextField field:fields.values())field.updateCursorCounter();}
    void discard(){active=false;mapPicking=false;}
    void refresh(){if(page<2 && font!=null){if(page==0 && fields.containsKey(0))query=fields.get(0).getText();controls();}}
    void tooltip(int mx,int my){if(page<2)for(GuiButton button:buttons)if(button.id>=350 && mx>=button.x && mx<button.x+button.width && my>=button.y && my<button.y+button.height){List<NBTTagCompound> rows=page==0?assets():weapons();int at=button.id-350+listPage*visibleRows();if(at<rows.size()){NBTTagCompound n=rows.get(at);host.ccTooltip(Arrays.asList(page==0?host.ccName(n):weaponName(n),page==0?I18n.format("planning.state."+n.getString("State")):tr("available",n.getInteger("Available")),page==0?I18n.format(n.getString("ReadyReason").isEmpty()?"planning.state.ready":"planning.error."+n.getString("ReadyReason")):tr("ammo_hint")),mx,my);}}}
}
