/*
 * MoonLight Hacked Client
 *
 * A free and open-source hacked client for Minecraft.
 * Developed using Minecraft's resources.
 *
 * Repository: https://github.com/randomguy3725/MoonLight
 *
 * Author(s): [Randumbguy & opZywl & lucas]
 */
package wtf.moonlight.features.modules.impl.visual;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.BossStatus;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S45PacketTitle;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjglx.Sys;
import wtf.moonlight.Moonlight;
import wtf.moonlight.events.annotations.EventTarget;
import wtf.moonlight.events.impl.misc.TickEvent;
import wtf.moonlight.events.impl.misc.WorldEvent;
import wtf.moonlight.events.impl.packet.PacketEvent;
import wtf.moonlight.events.impl.render.Render2DEvent;
import wtf.moonlight.events.impl.render.RenderGuiEvent;
import wtf.moonlight.events.impl.render.Shader2DEvent;
import wtf.moonlight.features.modules.Module;
import wtf.moonlight.features.modules.ModuleCategory;
import wtf.moonlight.features.modules.ModuleInfo;
import wtf.moonlight.features.modules.impl.combat.KillAura;
import wtf.moonlight.features.modules.impl.player.Stealer;
import wtf.moonlight.features.modules.impl.visual.island.IslandRenderer;
import wtf.moonlight.features.values.impl.*;
import wtf.moonlight.gui.click.neverlose.NeverLose;
import wtf.moonlight.gui.font.FontRenderer;
import wtf.moonlight.gui.font.Fonts;
import wtf.moonlight.utils.animations.Animation;
import wtf.moonlight.utils.animations.Direction;
import wtf.moonlight.utils.animations.Translate;
import wtf.moonlight.utils.animations.impl.DecelerateAnimation;
import wtf.moonlight.utils.player.MovementUtils;
import wtf.moonlight.utils.render.ColorUtils;
import wtf.moonlight.utils.render.RenderUtils;
import wtf.moonlight.utils.render.RoundedUtils;

import java.awt.*;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static wtf.moonlight.gui.click.neverlose.NeverLose.*;

@ModuleInfo(name = "Interface", category = ModuleCategory.Visual)
public class Interface extends Module {
    public final TextValue clientName = new TextValue("Client Name", "Moonlight", this);

    public final MultiBoolValue elements = new MultiBoolValue("Elements", Arrays.asList(
            new BoolValue("Watermark",true),
            new BoolValue("Island",true),
            new BoolValue("Module List",true),
            new BoolValue("Armor",true),
            new BoolValue("Info",true),
            new BoolValue("Health",true),
            new BoolValue("Potion HUD",true),
            new BoolValue("Target HUD",true),
            new BoolValue("Inventory",true),
            new BoolValue("Notification",true),
            new BoolValue("Pointer", true),
            new BoolValue("Session Info",true),
            new BoolValue("Key Bind", true),
            new BoolValue("Version Info", true)
    ), this);

    public final BoolValue cFont = new BoolValue("C Fonts",true,this, () -> elements.isEnabled("Module List"));
    public final ModeValue fontMode = new ModeValue("C Fonts Mode", new String[]{"Bold","Semi Bold","Medium","Regular","Tahoma"}, "Semi Bold", this,() -> cFont.canDisplay() && cFont.get());
    public final SliderValue fontSize = new SliderValue("Font Size",15,10,25,this,cFont::get);
    public final ModeValue watemarkMode = new ModeValue("Watermark Mode", new String[]{"Text","Styles","Styles 2","Nursultan","Exhi","Exhi 2","Nursultan 2","NeverLose"}, "Text", this,() -> elements.isEnabled("Watermark"));
    public final ModeValue animation = new ModeValue("Animation", new String[]{"ScaleIn", "MoveIn","Slide In"}, "ScaleIn", this, () -> elements.isEnabled("Module List"));
    public final ModeValue arrayPosition = new ModeValue("Position", new String[]{"Right","Left"}, "Right", this, () -> elements.isEnabled("Module List"));
    public final SliderValue x = new SliderValue("Module List X", 0, -50, 50, this, () -> elements.isEnabled("Module List"));
    public final SliderValue y = new SliderValue("Module List Y", 0, -50, 50, this, () -> elements.isEnabled("Module List"));
    public final SliderValue textHeight = new SliderValue("Text Height", 2, 0, 10, this, () -> elements.isEnabled("Module List"));
    public final ModeValue tags = new ModeValue("Suffix", new String[]{"None", "Simple", "Bracket", "Dash"}, "None", this, () -> elements.isEnabled("Module List"));
    public final ModeValue line = new ModeValue("Line", new String[]{"Right","Left","None"}, "Right", this, () -> elements.isEnabled("Module List"));
    public final BoolValue outLine = new BoolValue("Outline",true,this,() -> !line.is("None"));
    public final ModeValue armorMode = new ModeValue("Armor Mode", new String[]{"Default"}, "Default", this,() -> elements.isEnabled("Armor"));
    public final ModeValue infoMode = new ModeValue("Info Mode", new String[]{"Exhi", "Moon", "Moon 2","Tenacity"}, "Default", this,() -> elements.isEnabled("Info"));
    public final ModeValue versionMode = new ModeValue("Version Mode", new String[]{"Default"}, "Default",this,() -> elements.isEnabled("Version Info"));
    public final ModeValue potionHudMode = new ModeValue("Potion Mode", new String[]{"Default","Nursultan","Exhi","Moon","Sexy","Type 1","NeverLose","Mod"}, "Default", this,() -> elements.isEnabled("Potion HUD"));
    public final ModeValue targetHudMode = new ModeValue("TargetHUD Mode", new String[]{"Astolfo", "Type 1", "Type 2","Exhi","Adjust","Moon","Augustus","New","Novo 1","Novo 2","Novo 3","Novo 4"}, "Astolfo", this,() -> elements.isEnabled("Target HUD"));
    public final BoolValue targetHudParticle = new BoolValue("TargetHUD Particle",true,this,() -> elements.isEnabled("Target HUD"));
    public final ModeValue notificationMode = new ModeValue("Notification Mode", new String[]{"Default", "Test","Type 2","Type 3","Type 4", "Test2","Exhi"}, "Default", this,() -> elements.isEnabled("Notification"));
    public final ModeValue keyBindMode = new ModeValue("Key Bind Mode", new String[]{"Type 1"}, "Type 1", this,() -> elements.isEnabled("Key Bind"));
    public final ModeValue sessionInfoMode = new ModeValue("Session Info Mode", new String[]{"Default","Exhi","Rise","Moon","Opai"}, "Default", this,() -> elements.isEnabled("Session Info"));
    public final BoolValue centerNotif = new BoolValue("Center Notification",true,this,() -> notificationMode.is("Exhi"));
    public final ModeValue color = new ModeValue("Color Setting", new String[]{"Custom", "Rainbow", "Dynamic", "Fade","Astolfo","NeverLose"}, "NeverLose", this);
    private final ColorValue mainColor = new ColorValue("Main Color", new Color(128, 128, 255), this,() -> !color.is("NeverLose"));
    private final ColorValue secondColor = new ColorValue("Second Color", new Color(128, 255, 255), this, () -> color.is("Fade"));
    private final SliderValue colorAlpha = new SliderValue("Color Alpha",100,1,255,1,this);
    public final SliderValue fadeSpeed = new SliderValue("Fade Speed", 1, 1, 10, 1, this, () -> color.is("Dynamic") || color.is("Fade"));
    public final BoolValue background = new BoolValue("Background",true,this, () -> elements.isEnabled("Module List"));
    public final ModeValue bgColor = new ModeValue("Background Color", new String[]{"Dark", "Synced","Custom","NeverLose"}, "Synced", this,background::get);
    private final ColorValue bgCustomColor = new ColorValue("Background Custom Color", new Color(32, 32, 64), this,() -> bgColor.canDisplay() && bgColor.is("Custom"));
    private final SliderValue bgAlpha = new SliderValue("Background Alpha",100,1,255,1,this);
    public final BoolValue customScoreboard = new BoolValue("Custom Scoreboard", false, this);
    public final BoolValue hideScoreboard = new BoolValue("Hide Scoreboard", false, this,() -> !customScoreboard.get());
    public final BoolValue hideScoreRed = new BoolValue("Hide Scoreboard Red Points", true, this, customScoreboard::get);
    public final BoolValue fixHeight = new BoolValue("Fix Height", true, this, customScoreboard::get);
    public final BoolValue hideBackground = new BoolValue("Hide Background", true, this, customScoreboard::get);
    public final BoolValue chatCombine = new BoolValue("Chat Combine", true, this);

    public final BoolValue cape = new BoolValue("Cape", true, this);
    public final BoolValue wavey = new BoolValue("Wavey Cape", true, this);
    public final BoolValue enchanted = new BoolValue("Enchanted", true, this, () -> cape.get() && !wavey.get());
    private final DecimalFormat bpsFormat = new DecimalFormat("0.00");
    private final DecimalFormat xyzFormat = new DecimalFormat("0");
    private final DecimalFormat fpsFormat = new DecimalFormat("0");
    private final DecimalFormat healthFormat = new DecimalFormat("0.#", new DecimalFormatSymbols(Locale.ENGLISH));
    private final DateFormat dateFormat = new SimpleDateFormat("hh:mm");
    private final DateFormat dateFormat2 = new SimpleDateFormat("hh:mm:ss");
    public final Map<EntityPlayer, DecelerateAnimation> animationEntityPlayerMap = new HashMap<>();
    public int lost = 0, killed = 0, won = 0;
    public int prevMatchKilled = 0,matchKilled = 0,match;
    private final Random random = new Random();
    private int scoreBoardHeight = 0;
    private final Pattern LINK_PATTERN = Pattern.compile("(http(s)?://.)?(www\\.)?[-a-zA-Z0-9@:%._+~#=]{2,256}\\.[A-z]{2,6}\\b([-a-zA-Z0-9@:%_+.~#?&//=]*)");

    @EventTarget
    public void onRender2D(Render2DEvent event) {

        if (elements.isEnabled("Island")) {
            IslandRenderer.INSTANCE.render(event.scaledResolution(),false);
        }

        if (watemarkMode.canDisplay()) {
            switch (watemarkMode.get()) {
                case "Text":
                    Fonts.interBold.get(30).drawStringWithShadow(clientName.get(), 10, 10, color(0));
                    break;
                case "Styles":
                    String dateString = dateFormat.format(new Date());

                    String name = " | " + Moonlight.INSTANCE.getVersion() +
                            EnumChatFormatting.GRAY + " | " + EnumChatFormatting.WHITE + dateString +
                            EnumChatFormatting.GRAY + " | " + EnumChatFormatting.WHITE + mc.thePlayer.getName() +
                            EnumChatFormatting.GRAY + " | " + EnumChatFormatting.WHITE + mc.getCurrentServerData().serverIP;

                    int x = 7;
                    int y = 7;
                    int width = Fonts.interBold.get(17).getStringWidth("ML") + Fonts.interRegular.get(17).getStringWidth(name) + 5;
                    int height = Fonts.interRegular.get(17).getHeight() + 3;

                    RoundedUtils.drawRound(x, y, width, height, 4, new Color(getModule(Interface.class).bgColor(), true));
                    Fonts.interBold.get(17).drawOutlinedString("ML", x + 2, y + 4.5f, -1, color());
                    Fonts.interRegular.get(17).drawStringWithShadow(name, Fonts.interBold.get(17).getStringWidth("ML") + x + 2, y + 4.5f, -1);
                    break;
                case "Styles 2":
                    String dateString2 = dateFormat2.format(new Date());

                    String serverString;
                    if (mc.isSingleplayer()) {
                        serverString = "singleplayer";
                    } else
                        serverString = mc.getCurrentServerData().serverIP.toLowerCase();

                    String stylesname = "moonlight" + EnumChatFormatting.WHITE +
                            " | " + mc.thePlayer.getName() +
                            " | " + Minecraft.getDebugFPS() + "fps" +
                            " | " + serverString + " | " + dateString2;

                    x = 7;
                    y = 7;
                    width = Fonts.interSemiBold.get(17).getStringWidth("") + Fonts.interSemiBold.get(17).getStringWidth(stylesname) + 5;
                    height = Fonts.interSemiBold.get(17).getHeight() + 3;

                    RoundedUtils.drawRound(x, y, width, height, 4, new Color(getModule(Interface.class).bgColor(), true));
                    Fonts.interSemiBold.get(17).drawString(stylesname, Fonts.interBold.get(17).getStringWidth("") + x + 2, y + 4.5f, new Color(color(1)).getRGB());
                    break;
                case "Nursultan":
                    RoundedUtils.drawRound(7, 7.5f, 20 + Fonts.interMedium.get(15).getStringWidth(INSTANCE.getVersion()) + 5, 15, 4, new Color(bgColor(0)));
                    Fonts.nursultan.get(16).drawString("P", 13, 14, color(0));
                    RenderUtils.drawRect(25, 10.5f, 1, 8.5f, new Color(47, 47, 47).getRGB());
                    Fonts.interMedium.get(15).drawString(INSTANCE.getVersion(), 29, 13, color(0));

                    RenderUtils.drawRect(7 + 20 + Fonts.interMedium.get(15).getStringWidth(INSTANCE.getVersion()) + 2.5f + 11 + 15, 10.5f, 1, 8.5f, new Color(47, 47, 47).getRGB());
                    RoundedUtils.drawRound(7 + 20 + Fonts.interMedium.get(15).getStringWidth(INSTANCE.getVersion()) + 2.5f + 11, 7.5f, Fonts.interMedium.get(15).getStringWidth("user") + 25, 15, 4, new Color(bgColor(0)));
                    Fonts.nursultan.get(16).drawString("W", 7 + 20 + Fonts.interMedium.get(15).getStringWidth(INSTANCE.getVersion()) + 2.5f + 11 + 5, 14, color(0));
                    Fonts.interMedium.get(15).drawString("user", 7 + 20 + Fonts.interMedium.get(15).getStringWidth(INSTANCE.getVersion()) + 2.5f + 11 + 15 + 5, 13, -1);
                    break;
                case "Exhi":
                    boolean shouldChange = RenderUtils.COLOR_PATTERN.matcher(clientName.get()).find();
                    String text = shouldChange ? "§r" + clientName.get() : clientName.get().charAt(0) + "§r§f" + clientName.get().substring(1) +
                            "§7[§f" + Minecraft.getDebugFPS() + " FPS§7]§r ";
                    mc.fontRendererObj.drawStringWithShadow(text, 2.0f, 2.0f, color());
                    break;
                case "Exhi 2":
                    shouldChange = RenderUtils.COLOR_PATTERN.matcher(clientName.get()).find();
                    text = shouldChange ? "§r" + clientName.get() : clientName.get().charAt(0) + "§r§f" + clientName.get().substring(1) +
                            " §7[§f" + Minecraft.getDebugFPS() + " FPS§7]§r ";
                    Fonts.Tahoma.get(15).drawStringWithShadow(text, 1.0f, 2.0f, color());
                    break;
                case "Nursultan 2":
                    float posX = 7f;
                    float posY = 7.5f;
                    float fontSize = 15f;
                    float iconSize = 5.0F;
                    float rectWidth = 10.0F;
                    String title = " | MoonLight";
                    float titleWidth = Fonts.interMedium.get(fontSize).getStringWidth(title);

                    RoundedUtils.drawRound(posX, posY, rectWidth + iconSize * 2.5F + titleWidth, 15, 4.0F, new Color(getModule(Interface.class).bgColor(), true));

                    Fonts.nursultan.get(18).drawString("S", posX + iconSize, posY + 2 + iconSize - 1.0F, color());

                    Fonts.interMedium.get(fontSize).drawString(title, posX + rectWidth + iconSize * 1.5F, posY + rectWidth / 2.0F + 1.5F, color());

                    String playerName = mc.thePlayer.getName();
                    float playerNameWidth = Fonts.interMedium.get(fontSize).getStringWidth(playerName);
                    float playerNameX = posX + rectWidth + iconSize * 2.5F + titleWidth + iconSize;

                    RoundedUtils.drawRound(playerNameX, posY, rectWidth + iconSize * 2.5F + playerNameWidth, 15, 4.0F, new Color(getModule(Interface.class).bgColor(), true));

                    Fonts.nursultan.get(fontSize).drawString("W", playerNameX + iconSize, posY + 1 + iconSize, color());

                    Fonts.interMedium.get(fontSize).drawString(playerName, playerNameX + iconSize * 1.5F + rectWidth, posY + rectWidth / 2.0F + 1.5F, -1);

                    int fps = Minecraft.getDebugFPS();
                    String fpsText = fps + " Fps";
                    float fpsTextWidth = Fonts.interMedium.get(fontSize).getStringWidth(fpsText);
                    float fpsX = playerNameX + rectWidth + iconSize * 2.5F + playerNameWidth + iconSize;

                    RoundedUtils.drawRound(fpsX, posY, rectWidth + iconSize * 2.5F + fpsTextWidth, 15, 4.0F, new Color(getModule(Interface.class).bgColor(), true));

                    Fonts.nursultan.get(18).drawString("X", fpsX + iconSize, posY + 1 + iconSize, color());

                    Fonts.interMedium.get(fontSize).drawString(fpsText, fpsX + rectWidth + iconSize * 1.5F, posY + rectWidth / 2.0F + 1.5F, -1);

                    String playerPosition = (int) mc.thePlayer.posX + " " + (int) mc.thePlayer.posY + " " + (int) mc.thePlayer.posZ;
                    float positionTextWidth = Fonts.interMedium.get(fontSize).getStringWidth(playerPosition);
                    float positionY = posY + 15 + iconSize;

                    RoundedUtils.drawRound(posX, positionY, rectWidth + iconSize * 2.5F + positionTextWidth, 15, 4.0F, new Color(getModule(Interface.class).bgColor(), true));

                    Fonts.nursultan.get(18).drawString("F", posX + iconSize, positionY + 1.5F + iconSize, color());

                    Fonts.interMedium.get(fontSize).drawString(playerPosition, posX + iconSize * 1.5F + rectWidth, positionY + rectWidth / 2.0F + 1.5F, -1);

                    String pingText = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID()).getResponseTime() + " Ping";
                    float pingTextWidth = Fonts.interMedium.get(fontSize).getStringWidth(pingText);
                    float pingX = posX + rectWidth + iconSize * 2.5F + positionTextWidth + iconSize;

                    RoundedUtils.drawRound(pingX, positionY, rectWidth + iconSize * 2.5F + pingTextWidth, 15, 4.0F, new Color(getModule(Interface.class).bgColor(), true));

                    Fonts.nursultan.get(18).drawString("Q", pingX + iconSize, positionY + 1 + iconSize, color());

                    Fonts.interMedium.get(fontSize).drawString(pingText, pingX + iconSize * 1.5F + rectWidth, positionY + rectWidth / 2.0F + 1.5F, -1);
                    break;
                case "NeverLose":
                    //title
                    FontRenderer titleFont = Fonts.interBold.get(20);

                    //info
                    FontRenderer info = Fonts.interRegular.get(16);
                    String userIcon = "W ";
                    String fpsIcon = "X ";
                    String timeIcon = "V ";
                    float userIconX = 3 + titleFont.getStringWidth(clientName.getText()) + 9 + 7;
                    float fpsIconX = Fonts.nursultan.get(20).getStringWidth(userIcon) + userIconX + info.getStringWidth(mc.thePlayer.getName()) + Fonts.nursultan.get(20).getStringWidth(fpsIcon) - 10;
                    float clockIconX = fpsIconX + info.getStringWidth(Minecraft.getDebugFPS() + "fps") + Fonts.nursultan.get(20).getStringWidth(timeIcon);
                    String times = dateFormat.format(new Date());

                    int bgY = 5;

                    int textY = 11;

                    //title
                    RoundedUtils.drawRound(3, bgY, titleFont.getStringWidth(clientName.getText()) + 10, Fonts.interRegular.get(20).getHeight() + 2, 4, ColorUtils.applyOpacity(NeverLose.bgColor, 1f));
                    titleFont.drawOutlinedString(clientName.getText(), 8, textY - 2, textRGB, outlineTextRGB);


                    //info
                    RoundedUtils.drawRound(3 + titleFont.getStringWidth(clientName.getText()) + 14, bgY,
                            Fonts.nursultan.get(20).getStringWidth(userIcon) +
                                    info.getStringWidth(mc.thePlayer.getName()) +
                                    Fonts.nursultan.get(20).getStringWidth(fpsIcon) +
                                    info.getStringWidth(String.valueOf(Minecraft.getDebugFPS())) +
                                    Fonts.nursultan.get(20).getStringWidth(timeIcon) +
                                    info.getStringWidth(times)
                                    + 27
                            , Fonts.interRegular.get(20).getHeight() + 2, 4, ColorUtils.applyOpacity(NeverLose.bgColor, 1f));

                    Fonts.nursultan.get(20).drawString(userIcon, userIconX, textY, iconRGB);
                    info.drawString(mc.thePlayer.getName(), userIconX + Fonts.nursultan.get(20).getStringWidth(userIcon), 11, textRGB);

                    Fonts.nursultan.get(20).drawString(fpsIcon, fpsIconX, textY, iconRGB);
                    info.drawString(Minecraft.getDebugFPS() + "fps", fpsIconX + Fonts.nursultan.get(20).getStringWidth(fpsIcon), textY, textRGB);

                    Fonts.nursultan.get(20).drawString(timeIcon, clockIconX, textY, iconRGB);
                    info.drawString(times, clockIconX + Fonts.nursultan.get(20).getStringWidth(timeIcon) - 7, textY, textRGB);

                    break;
            }
        }


        if (infoMode.canDisplay()) {
            switch (infoMode.get()) {
                case "Exhi":
                    float textY = (event.scaledResolution().getScaledHeight() - 9) + (mc.currentScreen instanceof GuiChat ? -14.0f : -3.0f);
                    mc.fontRendererObj.drawStringWithShadow("XYZ: " +  EnumChatFormatting.WHITE +
                                    xyzFormat.format(mc.thePlayer.posX) + " " +
                                    xyzFormat.format(mc.thePlayer.posY) + " " +
                                    xyzFormat.format(mc.thePlayer.posZ) + " " + EnumChatFormatting.RESET + "BPS: " + EnumChatFormatting.WHITE + this.bpsFormat.format(MovementUtils.getBPS())
                            , 2, textY, color(0));
                    break;
                case "Moon":
                    textY = (event.scaledResolution().getScaledHeight() - 9) + (mc.currentScreen instanceof GuiChat ? -14.0f : -3.0f);
                    mc.fontRendererObj.drawStringWithShadow("FPS: " + EnumChatFormatting.WHITE + Minecraft.getDebugFPS(), 2, textY, color(0));
                    break;
                case "Moon 2":
                    textY = (event.scaledResolution().getScaledHeight() - 6.5F) + (mc.currentScreen instanceof GuiChat ? -14.0f : -3.0f);
                    Fonts.interSemiBold.get(19).drawStringWithShadow("§fFPS: " + Minecraft.getDebugFPS(), 1.5F, textY, color(0));
                    break;
                case "Tenacity":
                    float XYZText = (event.scaledResolution().getScaledHeight() - 9);
                    float SpeedText = (event.scaledResolution().getScaledHeight() - 18);
                    float FPSText = (event.scaledResolution().getScaledHeight() - 27);

                    Fonts.psBold.get(19).drawStringWithShadow("XYZ: ", 2, XYZText, color(0));
                    Fonts.psRegular.get(19).drawStringWithShadow(EnumChatFormatting.WHITE + xyzFormat.format(mc.thePlayer.posX) + " " + xyzFormat.format(mc.thePlayer.posY) + " " + xyzFormat.format(mc.thePlayer.posZ), 26, XYZText, color(0));
                    Fonts.psBold.get(19).drawStringWithShadow("Speed:", 2, SpeedText, color(0));
                    Fonts.psRegular.get(19).drawStringWithShadow(EnumChatFormatting.WHITE + bpsFormat.format(MovementUtils.getBPS()), 35, SpeedText, color(0));
                    Fonts.psBold.get(19).drawStringWithShadow("FPS:", 2, FPSText, color(0));
                    Fonts.psRegular.get(19).drawStringWithShadow(EnumChatFormatting.WHITE + fpsFormat.format(Minecraft.getDebugFPS()), 24, FPSText, color(0));
            }
        }
        if (versionMode.canDisplay()) {
            switch (versionMode.get()) {
                case "Default":
                    float textY = (event.scaledResolution().getScaledHeight() - 9) + (mc.currentScreen instanceof GuiChat ? -14.0f : -3.0f);

                    Fonts.interMedium.get(17).drawStringWithShadow(EnumChatFormatting.WHITE + Moonlight.INSTANCE.getVersion() + " §7- " + EnumChatFormatting.WHITE + Moonlight.INSTANCE.getDiscordRP().getName() + " §7- " + EnumChatFormatting.WHITE + "1.0", (float) event.scaledResolution().getScaledWidth() - Fonts.interMedium.get(17).getStringWidth(Moonlight.INSTANCE.getVersion() + " - "  + Moonlight.INSTANCE.getDiscordRP().getName() + " §7- " + EnumChatFormatting.WHITE + "1.0") - 2.0f, textY + 3.5, color (0));
                    break;
            }
        }

        if (armorMode.canDisplay()) {
            switch (armorMode.get()) {
                case "Default":
                    ArrayList<ItemStack> stuff = new ArrayList<>();
                    boolean onWater = mc.thePlayer.isEntityAlive() && mc.thePlayer.isInsideOfMaterial(Material.water);
                    int split = -3;
                    for (int index = 3; index >= 0; --index) {
                        ItemStack armor = mc.thePlayer.inventory.armorInventory[index];
                        if (armor == null) continue;
                        stuff.add(armor);
                    }
                    if (mc.thePlayer.getCurrentEquippedItem() != null) {
                        stuff.add(mc.thePlayer.getCurrentEquippedItem());
                    }
                    for (ItemStack everything : stuff) {
                        split += 16;
                        RenderUtils.renderItemStack(everything, split + (double) event.scaledResolution().getScaledWidth() / 2 - 4, event.scaledResolution().getScaledHeight() - (onWater ? 65 : 55) + (mc.thePlayer.capabilities.isCreativeMode ? 14 : 0), 1, true, 0.5f);
                    }
                    break;
            }
        }

        if (elements.isEnabled("Module List")) {
            int count = 1;
            float lastModuleLeftSide = 0.0f;
            float lastModuleWidth = 0.0f;
            int screenWidth = event.scaledResolution().getScaledWidth();
            float y = this.y.get();
            Comparator<Module> sort = (m1, m2) -> {
                double ab = cFont.get() ? getFr().getStringWidth(m1.getName() + m1.getTag()) : mc.fontRendererObj.getStringWidth(m1.getName() + m1.getTag());
                double bb = cFont.get() ? getFr().getStringWidth(m2.getName() + m2.getTag()) : mc.fontRendererObj.getStringWidth(m2.getName() + m2.getTag());
                return Double.compare(bb, ab);
            };
            ArrayList<Module> enabledMods = new ArrayList<>(INSTANCE.getModuleManager().getModules());

            if (animation.is("Slide In")) {
                enabledMods.sort(sort);

                for (final Module module : enabledMods) {
                    if (module.isHidden())
                        continue;
                    Translate translate = module.getTranslate();
                    float moduleWidth = cFont.get() ? getFr().getStringWidth(module.getName() + module.getTag()) : mc.fontRendererObj.getStringWidth(module.getName() + module.getTag());
                    if (arrayPosition.is("Right")) {
                        if (module.isEnabled() && !module.isHidden()) {
                            translate.translate((screenWidth - moduleWidth - 1.0f) + this.x.get(), y);
                            y += (cFont.get() ? getFr().getHeight() : mc.fontRendererObj.FONT_HEIGHT) + textHeight.get();
                        } else {
                            translate.animate((screenWidth - 1) + this.x.get(), -25.0);
                        }
                    } else if (module.isEnabled() && !module.isHidden()) {
                        translate.translate((2.0f) + this.x.get(), y);
                        y += (cFont.get() ? getFr().getHeight() : mc.fontRendererObj.FONT_HEIGHT) + textHeight.get();
                    } else {
                        translate.animate((-moduleWidth) + this.x.get(), -25.0);
                    }
                    if (translate.getX() >= screenWidth) {
                        continue;
                    }

                    final float leftSide = (float) (translate.getX() - 2f);
                    final float bottom = (cFont.get() ? getFr().getHeight() : mc.fontRendererObj.FONT_HEIGHT) + textHeight.get();

                    if (background.get()) {
                        RenderUtils.drawRect(leftSide, (float) translate.getY(), moduleWidth + 3, bottom, bgColor(count));
                    }

                    if (line.is("Left")) {
                        RenderUtils.drawRect(leftSide - 1, (float) translate.getY(), 1, bottom, color(count));
                    }

                    if (line.is("Right")) {
                        RenderUtils.drawRect((float) (translate.getX() + moduleWidth), (float) translate.getY(), 1, bottom, color(count));
                    }

                    /*if (!line.is("None")) {
                        if (outLine.get()) {
                            if (arrayPosition.is("Right")) {
                                if (lastModuleWidth - moduleWidth > 0) {
                                    RenderUtils.drawRect(lastModuleLeftSide, y, lastModuleWidth - moduleWidth, 1.5f, color(count));
                                }

                                if (i == size - 1) {
                                    RenderUtils.drawRect(lastModuleLeftSide, y + (cFont.get() ? getFr().getHeight() : mc.fontRendererObj.FONT_HEIGHT) + textHeight.get(), moduleWidth, 1.5f, color(count));
                                }
                            } else {
                                if (lastModuleWidth - moduleWidth > 0) {
                                    RenderUtils.drawRect((float) (translate.getX() + lastModuleWidth), y, moduleWidth - lastModuleWidth, 1.5f, color(count));
                                }

                                if (i == size - 1) {
                                    RenderUtils.drawRect((float) (translate.getX() + lastModuleWidth), y + (cFont.get() ? getFr().getHeight() : mc.fontRendererObj.FONT_HEIGHT) + textHeight.get(), moduleWidth - lastModuleWidth, 1.5f, color(count));
                                }
                            }
                        }
                    }*/

                    if (cFont.get()) {
                        getFr().drawStringWithShadow(module.getName() + module.getTag(), (float) translate.getX() - 1, (float) translate.getY() + 2f, color(count));
                    } else {
                        mc.fontRendererObj.drawStringWithShadow(module.getName() + module.getTag(), (float) translate.getX() - 1, (float) translate.getY() + 2f, color(count));
                    }

                    count -= 1;
                    lastModuleLeftSide = leftSide;
                    lastModuleWidth = moduleWidth;
                }
            }

            if (!animation.is("Slide In")) {
                enabledMods.sort(sort);
                for (int i = 0, size = enabledMods.size(); i < size; i++) {
                    final Module module = enabledMods.get(i);
                    if (module.isHidden())
                        continue;
                    Animation moduleAnimation = module.getAnimation();
                    moduleAnimation.setDirection(module.isEnabled() ? Direction.FORWARDS : Direction.BACKWARDS);
                    if (!module.isEnabled() && moduleAnimation.finished(Direction.BACKWARDS)) continue;
                    float moduleWidth = cFont.get() ? getFr().getStringWidth(module.getName() + module.getTag()) : mc.fontRendererObj.getStringWidth(module.getName() + module.getTag());
                    float x = (arrayPosition.is("Right") ? screenWidth - moduleWidth - 1.0f : 2) + this.x.get();
                    float alphaAnimation = 1.0f;

                    switch (animation.get()) {
                        case "MoveIn": {
                            x += (float) Math.abs((moduleAnimation.getOutput() - 1.0) * (2.0 + moduleWidth));
                            break;
                        }
                        case "ScaleIn": {
                            RenderUtils.scaleStart(x + (moduleWidth / 2.0f), y + mc.fontRendererObj.FONT_HEIGHT, (float) moduleAnimation.getOutput());
                            alphaAnimation = (float) moduleAnimation.getOutput();
                        }
                    }

                    final float leftSide = x - 2f;
                    final float bottom = (cFont.get() ? getFr().getHeight() : mc.fontRendererObj.FONT_HEIGHT) + textHeight.get();

                    if (background.get()) {
                        RenderUtils.drawRect(leftSide, y, moduleWidth + 3, bottom, bgColor(count));
                    }

                    if (line.is("Left")) {
                        RenderUtils.drawRect(leftSide - 1, y, 1, bottom, color(count));
                    }

                    if(line.is("Right")){
                        RenderUtils.drawRect(x + moduleWidth, y, 1, bottom,  color(count));
                    }

                    if (!line.is("None")) {
                        if (outLine.get()) {
                            if (arrayPosition.is("Right")) {
                                if (lastModuleWidth - moduleWidth > 0) {
                                    RenderUtils.drawRect(lastModuleLeftSide, y, lastModuleWidth - moduleWidth, 1.5f, color(count));
                                }

                                if (i == size - 1) {
                                    RenderUtils.drawRect(x, y + (cFont.get() ? getFr().getHeight() : mc.fontRendererObj.FONT_HEIGHT) + textHeight.get(), moduleWidth, 1.5f, color(count));
                                }
                            } else {
                                if (lastModuleWidth - moduleWidth > 0) {
                                    RenderUtils.drawRect(x + lastModuleWidth, y, moduleWidth - lastModuleWidth, 1.5f, color(count));
                                }

                                if (i == size - 1) {
                                    RenderUtils.drawRect(x + lastModuleWidth, y + (cFont.get() ? getFr().getHeight() : mc.fontRendererObj.FONT_HEIGHT) + textHeight.get(), moduleWidth - lastModuleWidth, 1.5f, color(count));
                                }
                            }
                        }
                    }

                    if (cFont.get()) {
                        getFr().drawStringWithShadow(module.getName() + module.getTag(), x - 1, y + 2f, ColorUtils.swapAlpha(color(count), (int) alphaAnimation * colorAlpha.get()));
                    } else {
                        mc.fontRendererObj.drawStringWithShadow(module.getName() + module.getTag(), x - 1, y + 2f, ColorUtils.swapAlpha(color(count), (int) alphaAnimation * colorAlpha.get()));
                    }

                    if (animation.get().equals("ScaleIn")) {
                        RenderUtils.scaleEnd();
                    }

                    lastModuleLeftSide = leftSide;
                    lastModuleWidth = moduleWidth;
                    y += (float) (moduleAnimation.getOutput() * ((cFont.get() ? getFr().getHeight() : mc.fontRendererObj.FONT_HEIGHT) + textHeight.get()));
                    count -= 2;
                }
            }

            scoreBoardHeight = (int) y;
        }

        if (elements.isEnabled("Potion HUD") && potionHudMode.is("Exhi")) {
            ArrayList<PotionEffect> potions = new ArrayList<>(mc.thePlayer.getActivePotionEffects());
            potions.sort(Comparator.comparingDouble(effect -> -mc.fontRendererObj.getStringWidth(I18n.format(Potion.potionTypes[effect.getPotionID()].getName()))));
            float y = mc.currentScreen instanceof GuiChat ? -14.0f : -3.0f;
            for (PotionEffect potionEffect : potions) {
                Potion potionType = Potion.potionTypes[potionEffect.getPotionID()];
                String potionName = I18n.format(potionType.getName());
                String type = "";
                if (potionEffect.getAmplifier() == 1) {
                    potionName = potionName + " II";
                } else if (potionEffect.getAmplifier() == 2) {
                    potionName = potionName + " III";
                } else if (potionEffect.getAmplifier() == 3) {
                    potionName = potionName + " IV";
                }
                if (potionEffect.getDuration() < 600 && potionEffect.getDuration() > 300) {
                    type = type + " §6" + Potion.getDurationString(potionEffect);
                } else if (potionEffect.getDuration() < 300) {
                    type = type + " §c" + Potion.getDurationString(potionEffect);
                } else if (potionEffect.getDuration() > 600) {
                    type = type + " §7" + Potion.getDurationString(potionEffect);
                }
                GlStateManager.pushMatrix();
                mc.fontRendererObj.drawString(potionName, (float) event.scaledResolution().getScaledWidth() - mc.fontRendererObj.getStringWidth(type + potionName) - 2.0f, (event.scaledResolution().getScaledHeight()  - (elements.isEnabled("Version Info") ? 15 : 9)) + y, new Color(potionType.getLiquidColor()).getRGB(), true);
                mc.fontRendererObj.drawString(type, (float) event.scaledResolution().getScaledWidth() - mc.fontRendererObj.getStringWidth(type) - 2.0f, (event.scaledResolution().getScaledHeight() - (elements.isEnabled("Version Info") ? 15 : 9)) + y, new Color(255, 255, 255).getRGB(), true);

                GlStateManager.popMatrix();
                y -= 9.0f;
            }
        }
        if (elements.isEnabled("Potion HUD") && potionHudMode.is("Moon")) {
            ArrayList<PotionEffect> potions = new ArrayList<>(mc.thePlayer.getActivePotionEffects());
            potions.sort(Comparator.comparingDouble(effect -> -Fonts.interMedium.get(19).getStringWidth(I18n.format(Potion.potionTypes[effect.getPotionID()].getName()))));
            float y = mc.currentScreen instanceof GuiChat ? -14.0f : -3.0f;
            for (PotionEffect potionEffect : potions) {
                Potion potionType = Potion.potionTypes[potionEffect.getPotionID()];
                String potionName = I18n.format(potionType.getName());
                String type = " §7-";
                if (potionEffect.getAmplifier() == 1) {
                    potionName = potionName + " 2";
                } else if (potionEffect.getAmplifier() == 2) {
                    potionName = potionName + " 3";
                } else if (potionEffect.getAmplifier() == 3) {
                    potionName = potionName + " 4";
                }
                if (potionEffect.getDuration() < 600 && potionEffect.getDuration() > 300) {
                    type = type + " §f" + Potion.getDurationString(potionEffect);
                } else if (potionEffect.getDuration() < 300) {
                    type = type + " §f" + Potion.getDurationString(potionEffect);
                } else if (potionEffect.getDuration() > 600) {
                    type = type + " §f" + Potion.getDurationString(potionEffect);
                }
                GlStateManager.pushMatrix();
                Fonts.interMedium.get(17).drawStringWithShadow(potionName, (float) event.scaledResolution().getScaledWidth() - Fonts.interSemiBold.get(17).getStringWidth(type + potionName) - 2.0f, (event.scaledResolution().getScaledHeight() - (elements.isEnabled("Version Info") ? 15 : 9)) + y, new Color(potionType.getLiquidColor()).getRGB());
                Fonts.interMedium.get(17).drawStringWithShadow(type, (float) event.scaledResolution().getScaledWidth() - Fonts.interMedium.get(17).getStringWidth(type) - 2.0f, (event.scaledResolution().getScaledHeight() - (elements.isEnabled("Version Info") ? 15 : 9)) + y, new Color(255, 255, 255).getRGB());

                GlStateManager.popMatrix();
                y -= 9.5f;
            }
        }

        if (elements.isEnabled("Potion HUD") && potionHudMode.is("Mod")) {
            GL11.glPushMatrix();
            GL11.glTranslatef(25, event.scaledResolution().getScaledHeight() / 2f, 0F);
            float yPos = -75F;
            float width = 0F;
            for (final PotionEffect effect : mc.thePlayer.getActivePotionEffects()) {
                final Potion potion = Potion.potionTypes[effect.getPotionID()];
                final String number = intToRomanByGreedy(effect.getAmplifier());
                final String name = I18n.format(potion.getName()) + " " + number;
                final float stringWidth = mc.fontRendererObj.getStringWidth(name)
                        + mc.fontRendererObj.getStringWidth("§f" + Potion.getDurationString(effect));

                if (width < stringWidth)
                    width = stringWidth;
                final float finalY = yPos;
                mc.fontRendererObj.drawString(name, 2f, finalY - 7f, Color.white.getRGB(), true);
                mc.fontRendererObj.drawStringWithShadow("§f" + Potion.getDurationString(effect), 2f, finalY + 4, -1);
                if (potion.hasStatusIcon()) {
                    GL11.glPushMatrix();
                    final boolean is2949 = GL11.glIsEnabled(2929);
                    final boolean is3042 = GL11.glIsEnabled(3042);
                    if (is2949)
                        GL11.glDisable(2929);
                    if (!is3042)
                        GL11.glEnable(3042);
                    GL11.glDepthMask(false);
                    OpenGlHelper.glBlendFunc(770, 771, 1, 0);
                    GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
                    final int statusIconIndex = potion.getStatusIconIndex();
                    mc.getTextureManager().bindTexture(new ResourceLocation("textures/gui/container/inventory.png"));
                    drawTexturedModalRect(-20F, finalY - 5, statusIconIndex % 8 * 18, 198 + statusIconIndex / 8 * 18, 18, 18);
                    GL11.glDepthMask(true);
                    if (!is3042)
                        GL11.glDisable(3042);
                    if (is2949)
                        GL11.glEnable(2929);
                    GL11.glPopMatrix();
                }

                yPos += mc.fontRendererObj.FONT_HEIGHT + 15;
            }
            GL11.glPopMatrix();
        }

        if(elements.isEnabled("Health")){
            renderHealth();
        }

        if (elements.isEnabled("Session Info") && sessionInfoMode.is("Exhi")) {
            mc.fontRendererObj.drawStringWithShadow(RenderUtils.sessionTime(), event.scaledResolution().getScaledWidth() / 2.0f - mc.fontRendererObj.getStringWidth(RenderUtils.sessionTime()) / 2.0f,BossStatus.bossName != null && BossStatus.statusBarTime > 0 ? 47 : 30.0f, -1);
        }

        if (elements.isEnabled("Notification")) {
            Moonlight.INSTANCE.getNotificationManager().publish(new ScaledResolution(mc),false);
        }
    }

    @EventTarget
    public void onShader2D(Shader2DEvent event) {

        if (elements.isEnabled("Island")) {
            IslandRenderer.INSTANCE.render(new ScaledResolution(mc), true);
        }

        if (watemarkMode.canDisplay()) {
            switch (watemarkMode.get()) {
                case "Text":
                    Fonts.interBold.get(30).drawStringWithShadow(clientName.get(), 10, 10, color(0));
                    break;
                case "Styles":
                    String dateString = dateFormat.format(new Date());

                    String name = " | " + Moonlight.INSTANCE.getVersion() +
                            EnumChatFormatting.GRAY + " | " + EnumChatFormatting.WHITE + dateString +
                            EnumChatFormatting.GRAY + " | " + EnumChatFormatting.WHITE + mc.thePlayer.getName() +
                            EnumChatFormatting.GRAY + " | " + EnumChatFormatting.WHITE + mc.getCurrentServerData().serverIP;

                    int x = 7;
                    int y = 7;
                    int width = Fonts.interBold.get(17).getStringWidth("ML") + Fonts.interRegular.get(17).getStringWidth(name) + 5;
                    int height = Fonts.interRegular.get(17).getHeight() + 3;

                    RoundedUtils.drawRound(x, y, width, height, 4, new Color(color()));
                    break;

                case "Styles 2":
                    String dateString2 = dateFormat2.format(new Date());

                    String serverString;
                    if (mc.isSingleplayer()) {
                        serverString = "singleplayer";
                    } else
                        serverString = mc.getCurrentServerData().serverIP.toLowerCase();

                    String stylesname = "moonlight" + EnumChatFormatting.WHITE +
                            " | " + mc.thePlayer.getName() +
                            " | " + Minecraft.getDebugFPS() + "fps" +
                            " | " + serverString + " | " + dateString2;

                    x = 7;
                    y = 7;
                    width = Fonts.interSemiBold.get(17).getStringWidth("") + Fonts.interSemiBold.get(17).getStringWidth(stylesname) + 5;
                    height = Fonts.interRegular.get(17).getHeight() + 3;

                    RoundedUtils.drawRound(x, y, width, height, 4, new Color(color()));
                    break;

                case "NeverLose":
                    //title
                    FontRenderer title = Fonts.interBold.get(20);

                    //info
                    FontRenderer info = Fonts.interRegular.get(16);
                    String userIcon = "W ";
                    String fpsIcon = "X ";
                    String timeIcon = "V ";
                    float userIconX = 3 + title.getStringWidth(clientName.getText()) + 9 + 7;
                    float fpsIconX = Fonts.nursultan.get(20).getStringWidth(userIcon) + userIconX + info.getStringWidth(mc.thePlayer.getName()) + Fonts.nursultan.get(20).getStringWidth(fpsIcon) - 10;
                    String times = dateFormat.format(new Date());

                    int bgY = 5;

                    RoundedUtils.drawRound(3 + title.getStringWidth(clientName.getText()) + 14, bgY,
                            Fonts.nursultan.get(20).getStringWidth(userIcon) +
                                    info.getStringWidth(mc.thePlayer.getName()) +
                                    Fonts.nursultan.get(20).getStringWidth(fpsIcon) +
                                    info.getStringWidth(String.valueOf(Minecraft.getDebugFPS())) +
                                    Fonts.nursultan.get(20).getStringWidth(timeIcon) +
                                    info.getStringWidth(times)
                                    + 27
                            , Fonts.interRegular.get(20).getHeight() + 2, 4, NeverLose.bgColor);
                    RoundedUtils.drawRound(3, bgY, title.getStringWidth(clientName.getText()) + 10, Fonts.interRegular.get(20).getHeight() + 2, 4, NeverLose.bgColor);

            }
        }

        if (elements.isEnabled("Module List")) {
            int count = 1;
            float lastModuleLeftSide = 0.0f;
            float lastModuleWidth = 0.0f;
            int screenWidth = new ScaledResolution(mc).getScaledWidth();
            float y = this.y.get();
            Comparator<Module> sort = (m1, m2) -> {
                double ab = cFont.get() ? getFr().getStringWidth(m1.getName() + m1.getTag()) : mc.fontRendererObj.getStringWidth(m1.getName() + m1.getTag());
                double bb = cFont.get() ? getFr().getStringWidth(m2.getName() + m2.getTag()) : mc.fontRendererObj.getStringWidth(m2.getName() + m2.getTag());
                return Double.compare(bb, ab);
            };
            ArrayList<Module> enabledMods = new ArrayList<>(INSTANCE.getModuleManager().getModules());

            if (animation.is("Slide In")) {
                enabledMods.sort(sort);
                for (final Module module : enabledMods) {
                    if (module.isHidden())
                        continue;
                    Translate translate = module.getTranslate();
                    float moduleWidth = cFont.get() ? getFr().getStringWidth(module.getName() + module.getTag()) : mc.fontRendererObj.getStringWidth(module.getName() + module.getTag());
                    if (arrayPosition.is("Right")) {
                        if (module.isEnabled() && !module.isHidden()) {
                            translate.translate((screenWidth - moduleWidth - 1.0f) + this.x.get(), y);
                            y += (cFont.get() ? getFr().getHeight() : mc.fontRendererObj.FONT_HEIGHT) + textHeight.get();
                        } else {
                            translate.animate((screenWidth - 1) + this.x.get(), -25.0);
                        }
                    } else if (module.isEnabled() && !module.isHidden()) {
                        translate.translate((2.0f) + this.x.get(), y);
                        y += (cFont.get() ? getFr().getHeight() : mc.fontRendererObj.FONT_HEIGHT) + textHeight.get();
                    } else {
                        translate.animate((-moduleWidth) + this.x.get(), -25.0);
                    }
                    if (translate.getX() >= screenWidth) {
                        continue;
                    }


                    final float leftSide = (float) (translate.getX() - 2f);
                    final float bottom = (cFont.get() ? getFr().getHeight() : mc.fontRendererObj.FONT_HEIGHT) + textHeight.get();

                    if (background.get()) {
                        if (event.getShaderType() == Shader2DEvent.ShaderType.BLUR || event.getShaderType() == Shader2DEvent.ShaderType.SHADOW) {
                            RenderUtils.drawRect(leftSide, y, moduleWidth + 3, bottom, color(count));
                        }
                        if (event.getShaderType() == Shader2DEvent.ShaderType.GLOW) {
                            RenderUtils.drawRect(leftSide, y, moduleWidth + 3, bottom, color(count));
                        }
                    }

                    if (event.getShaderType() == Shader2DEvent.ShaderType.GLOW) {

                        if (line.is("Left")) {
                            RenderUtils.drawRect(leftSide - 1, (float) translate.getY(), 1, bottom, color(count));
                        }

                        if (line.is("Right")) {
                            RenderUtils.drawRect((float) (translate.getX() + moduleWidth), (float) translate.getY(), 1, bottom, color(count));
                        }

                        /*if (!line.is("None")) {
                            if (outLine.get()) {
                                if (arrayPosition.is("Right")) {
                                    if (lastModuleWidth - moduleWidth > 0) {
                                        RenderUtils.drawRect(lastModuleLeftSide, y, lastModuleWidth - moduleWidth, 1.5f, color(count));
                                    }

                                    if (i == size - 1) {
                                        RenderUtils.drawRect(lastModuleLeftSide, y + (cFont.get() ? getFr().getHeight() : mc.fontRendererObj.FONT_HEIGHT) + textHeight.get(), moduleWidth, 1.5f, color(count));
                                    }
                                } else {
                                    if (lastModuleWidth - moduleWidth > 0) {
                                        RenderUtils.drawRect((float) (translate.getX() + lastModuleWidth), y, moduleWidth - lastModuleWidth, 1.5f, color(count));
                                    }

                                    if (i == size - 1) {
                                        RenderUtils.drawRect((float) (translate.getX() + lastModuleWidth), y + (cFont.get() ? getFr().getHeight() : mc.fontRendererObj.FONT_HEIGHT) + textHeight.get(), moduleWidth - lastModuleWidth, 1.5f, color(count));
                                    }
                                }
                            }
                        }*/

                        if (cFont.get()) {
                            getFr().drawStringWithShadow(module.getName() + module.getTag(), (float) translate.getX() - 1, (float) translate.getY() + 2f, color(count));
                        } else {
                            mc.fontRendererObj.drawStringWithShadow(module.getName() + module.getTag(), (float) translate.getX() - 1, (float) translate.getY() + 2f, color(count));
                        }
                    }

                    count -= 1;
                    lastModuleLeftSide = leftSide;
                    lastModuleWidth = moduleWidth;
                }
            }

            if (!animation.is("Slide In")) {
                enabledMods.sort(sort);
                for (int i = 0, size = enabledMods.size(); i < size; i++) {
                    final Module module = enabledMods.get(i);
                    if (module.isHidden())
                        continue;
                    Animation moduleAnimation = module.getAnimation();
                    moduleAnimation.setDirection(module.isEnabled() ? Direction.FORWARDS : Direction.BACKWARDS);
                    if (!module.isEnabled() && moduleAnimation.finished(Direction.BACKWARDS)) continue;
                    float moduleWidth = cFont.get() ? getFr().getStringWidth(module.getName() + module.getTag()) : mc.fontRendererObj.getStringWidth(module.getName() + module.getTag());
                    float x = (arrayPosition.is("Right") ? screenWidth - moduleWidth - 1.0f : 2) + this.x.get();

                    switch (animation.get()) {
                        case "MoveIn": {
                            x += (float) Math.abs((moduleAnimation.getOutput() - 1.0) * (2.0 + moduleWidth));
                            break;
                        }
                        case "ScaleIn": {
                            RenderUtils.scaleStart(x + (moduleWidth / 2.0f), y + mc.fontRendererObj.FONT_HEIGHT, (float) moduleAnimation.getOutput());
                        }
                    }

                    final float leftSide = x - 2f;
                    final float bottom = (cFont.get() ? getFr().getHeight() : mc.fontRendererObj.FONT_HEIGHT) + textHeight.get();

                    if (background.get()) {
                        if (event.getShaderType() == Shader2DEvent.ShaderType.BLUR || event.getShaderType() == Shader2DEvent.ShaderType.SHADOW) {
                            RenderUtils.drawRect(leftSide, y, moduleWidth + 3, bottom, color(count));
                        }
                        if (event.getShaderType() == Shader2DEvent.ShaderType.GLOW) {
                            RenderUtils.drawRect(leftSide, y, moduleWidth + 3, bottom, color(count));
                        }
                    }

                    if(event.getShaderType() == Shader2DEvent.ShaderType.GLOW) {
                        if (line.is("Left")) {
                            RenderUtils.drawRect(leftSide - 1, y, 1, bottom, color(count));
                        }

                        if (line.is("Right")) {
                            RenderUtils.drawRect(x + moduleWidth, y, 1, bottom, color(count));
                        }

                        if (!line.is("None")) {
                            if (outLine.get()) {
                                if (arrayPosition.is("Right")) {
                                    if (lastModuleWidth - moduleWidth > 0) {
                                        RenderUtils.drawRect(lastModuleLeftSide, y, lastModuleWidth - moduleWidth, 1.5f, color(count));
                                    }

                                    if (i == size - 1) {
                                        RenderUtils.drawRect(x, y + (cFont.get() ? getFr().getHeight() : mc.fontRendererObj.FONT_HEIGHT) + textHeight.get(), moduleWidth, 1.5f, color(count));
                                    }
                                } else {
                                    if (lastModuleWidth - moduleWidth > 0) {
                                        RenderUtils.drawRect(x + lastModuleWidth, y, moduleWidth - lastModuleWidth, 1.5f, color(count));
                                    }

                                    if (i == size - 1) {
                                        RenderUtils.drawRect(x + lastModuleWidth, y + (cFont.get() ? getFr().getHeight() : mc.fontRendererObj.FONT_HEIGHT) + textHeight.get(), moduleWidth - lastModuleWidth, 1.5f, color(count));
                                    }
                                }
                            }
                        }
                    }

                    if(event.getShaderType() == Shader2DEvent.ShaderType.GLOW) {
                        if (cFont.get()) {
                            getFr().drawStringWithShadow(module.getName() + module.getTag(), x - 1, y + 2f, ColorUtils.swapAlpha(color(count), colorAlpha.get()));
                        } else {
                            mc.fontRendererObj.drawStringWithShadow(module.getName() + module.getTag(), x - 1, y + 2f, ColorUtils.swapAlpha(color(count), colorAlpha.get()));
                        }
                    }

                    if (animation.get().equals("ScaleIn")) {
                        RenderUtils.scaleEnd();
                    }

                    lastModuleLeftSide = leftSide;
                    lastModuleWidth = moduleWidth;
                    y += (float) (moduleAnimation.getOutput() * ((cFont.get() ? getFr().getHeight() : mc.fontRendererObj.FONT_HEIGHT) + textHeight.get()));
                    count -= 2;
                }
            }
        }

        if (elements.isEnabled("Notification")) {
            Moonlight.INSTANCE.getNotificationManager().publish(new ScaledResolution(mc),true);
        }
    }

    @EventTarget
    public void onRenderGui(RenderGuiEvent event){
        if(elements.isEnabled("Health")) {
            if (mc.currentScreen instanceof GuiInventory || mc.currentScreen instanceof GuiChest && !getModule(Stealer.class).isStealing || mc.currentScreen instanceof GuiContainerCreative) {
                renderHealth();
            }
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        mainColor.setRainbow(color.is("Rainbow"));
        KillAura aura = getModule(KillAura.class);
        if (aura.isEnabled()) {
            animationEntityPlayerMap.entrySet().removeIf(entry -> entry.getKey().isDead || (!aura.targets.contains(entry.getKey()) && entry.getKey() != mc.thePlayer));
        }
        if (!aura.isEnabled() && !(mc.currentScreen instanceof GuiChat)) {
            Iterator<Map.Entry<EntityPlayer, DecelerateAnimation>> iterator = animationEntityPlayerMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<EntityPlayer, DecelerateAnimation> entry = iterator.next();
                DecelerateAnimation animation = entry.getValue();

                animation.setDirection(Direction.BACKWARDS);
                if (animation.finished(Direction.BACKWARDS)) {
                    iterator.remove();
                }
            }
        }
        if (aura.targets != null && !(mc.currentScreen instanceof GuiChat)) {
            for (EntityLivingBase entity : aura.targets) {
                if (entity instanceof EntityPlayer && entity != mc.thePlayer) {
                    animationEntityPlayerMap.putIfAbsent((EntityPlayer) entity, new DecelerateAnimation(175, 1));
                    animationEntityPlayerMap.get(entity).setDirection(Direction.FORWARDS);
                }
            }
        }
        if (aura.isEnabled() && aura.target == null && !(mc.currentScreen instanceof GuiChat)) {
            Iterator<Map.Entry<EntityPlayer, DecelerateAnimation>> iterator = animationEntityPlayerMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<EntityPlayer, DecelerateAnimation> entry = iterator.next();
                DecelerateAnimation animation = entry.getValue();

                animation.setDirection(Direction.BACKWARDS);
                if (animation.finished(Direction.BACKWARDS)) {
                    iterator.remove();
                }
            }
        }
        if (mc.currentScreen instanceof GuiChat) {
            animationEntityPlayerMap.putIfAbsent(mc.thePlayer, new DecelerateAnimation(175, 1));
            animationEntityPlayerMap.get(mc.thePlayer).setDirection(Direction.FORWARDS);
        }
    }

    @EventTarget
    public void onWorld(WorldEvent event){
        prevMatchKilled = matchKilled;
        matchKilled = 0;
        match += 1;

        if(match > 6)
            match = 6;
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        Packet<?> packet = event.getPacket();

        if (packet instanceof S02PacketChat) {
            S02PacketChat s02 = (S02PacketChat) event.getPacket();
            String xd = s02.getChatComponent().getUnformattedText();
            if (xd.contains("was killed by " + mc.thePlayer.getName())) {
                ++this.killed;
                prevMatchKilled = matchKilled;
                ++matchKilled;
            }

            if (xd.contains("You Died! Want to play again?")) {
                ++lost;
            }
        }

        if (packet instanceof S45PacketTitle && ((S45PacketTitle) packet).getType().equals(S45PacketTitle.Type.TITLE)) {
            String unformattedText = ((S45PacketTitle) packet).getMessage().getUnformattedText();
            if (unformattedText.contains("VICTORY!")) {
                ++this.won;
            }
            if (unformattedText.contains("GAME OVER!") || unformattedText.contains("DEFEAT!") || unformattedText.contains("YOU DIED!")) {
                ++this.lost;
            }
        }
    }

    public void renderHealth(){
        ScaledResolution sr = new ScaledResolution(mc);
        int xWidth = 0;
        GuiScreen screen = mc.currentScreen;
        float absorptionHealth = mc.thePlayer.getAbsorptionAmount();
        String string = this.healthFormat.format(mc.thePlayer.getHealth() / 2.0f) + "§c\u2764 " + (absorptionHealth <= 0.0f ? "" : "§e" + this.healthFormat.format(absorptionHealth / 2.0f) + "§6\u2764");
        int offsetY = 0;
        if (mc.thePlayer.getHealth() >= 0.0f && mc.thePlayer.getHealth() < 10.0f || mc.thePlayer.getHealth() >= 10.0f && mc.thePlayer.getHealth() < 100.0f) {
            xWidth = 3;
        }
        if (screen instanceof GuiInventory) {
            offsetY = 70;
        } else if (screen instanceof GuiContainerCreative) {
            offsetY = 80;
        } else if (screen instanceof GuiChest) {
            offsetY = ((GuiChest)screen).ySize / 2 - 15;
        }
        int x = new ScaledResolution(mc).getScaledWidth() / 2 - xWidth;
        int y = new ScaledResolution(mc).getScaledHeight() / 2 + 25 + offsetY;
        Color color = new Color(ColorUtils.getHealthColor(mc.thePlayer));
        mc.fontRendererObj.drawString(string, absorptionHealth > 0.0f ? x - 15.5f : x - 3.5f, y, color.getRGB(), true);
        GL11.glPushMatrix();
        mc.getTextureManager().bindTexture(Gui.icons);
        random.setSeed(mc.ingameGUI.getUpdateCounter() * 312871L);
        float width = sr.getScaledWidth() / 2.0f - mc.thePlayer.getMaxHealth() / 2.5f * 10.0f / 2.0f;
        float maxHealth = mc.thePlayer.getMaxHealth();
        int lastPlayerHealth = mc.ingameGUI.lastPlayerHealth;
        int healthInt = MathHelper.ceiling_float_int(mc.thePlayer.getHealth());
        int l2 = -1;
        boolean flag = mc.ingameGUI.healthUpdateCounter > (long) mc.ingameGUI.getUpdateCounter() && (mc.ingameGUI.healthUpdateCounter - (long) mc.ingameGUI.getUpdateCounter()) / 3L % 2L == 1L;
        if (mc.thePlayer.isPotionActive(Potion.regeneration)) {
            l2 = mc.ingameGUI.getUpdateCounter() % MathHelper.ceiling_float_int(maxHealth + 5.0f);
        }
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        for (int i6 = MathHelper.ceiling_float_int(maxHealth / 2.0f) - 1; i6 >= 0; --i6) {
            int xOffset = 16;
            if (mc.thePlayer.isPotionActive(Potion.poison)) {
                xOffset += 36;
            } else if (mc.thePlayer.isPotionActive(Potion.wither)) {
                xOffset += 72;
            }
            int k3 = 0;
            if (flag) {
                k3 = 1;
            }
            float renX = width + (float)(i6 % 10 * 8);
            float renY = (float)sr.getScaledHeight() / 2.0f + 15.0f + (float)offsetY;
            if (healthInt <= 4) {
                renY += (float)random.nextInt(2);
            }
            if (i6 == l2) {
                renY -= 2.0f;
            }
            int yOffset = 0;
            if (mc.theWorld.getWorldInfo().isHardcoreModeEnabled()) {
                yOffset = 5;
            }
            Gui.drawTexturedModalRect(renX, renY, 16 + k3 * 9, 9 * yOffset, 9, 9);
            if (flag) {
                if (i6 * 2 + 1 < lastPlayerHealth) {
                    Gui.drawTexturedModalRect(renX, renY, xOffset + 54, 9 * yOffset, 9, 9);
                }
                if (i6 * 2 + 1 == lastPlayerHealth) {
                    Gui.drawTexturedModalRect(renX, renY, xOffset + 63, 9 * yOffset, 9, 9);
                }
            }
            if (i6 * 2 + 1 < healthInt) {
                Gui.drawTexturedModalRect(renX, renY, xOffset + 36, 9 * yOffset, 9, 9);
            }
            if (i6 * 2 + 1 != healthInt) continue;
            Gui.drawTexturedModalRect(renX, renY, xOffset + 45, 9 * yOffset, 9, 9);
        }
        GL11.glPopMatrix();
    }

    public void drawScoreboard(ScaledResolution scaledRes, ScoreObjective objective, Scoreboard scoreboard, Collection<Score> scores) {
        List<Score> list = Lists.newArrayList(Iterables.filter(scores, p_apply_1_ -> p_apply_1_.getPlayerName() != null && !p_apply_1_.getPlayerName().startsWith("#")));

        if (list.size() > 15)
        {
            scores = Lists.newArrayList(Iterables.skip(list, scores.size() - 15));
        }
        else
        {
            scores = list;
        }

        int i = mc.fontRendererObj.getStringWidth(objective.getDisplayName());

        for (Score score : scores)
        {
            ScorePlayerTeam scoreplayerteam = scoreboard.getPlayersTeam(score.getPlayerName());
            String s = ScorePlayerTeam.formatPlayerName(scoreplayerteam, score.getPlayerName()) + ": " + EnumChatFormatting.RED + score.getScorePoints();
            i = Math.max(i, mc.fontRendererObj.getStringWidth(s));
        }

        int i1 = scores.size() * mc.fontRendererObj.FONT_HEIGHT;
        int j1 = scaledRes.getScaledHeight() / 2 + i1 / 3;
        int k1 = 3;
        int l1 = scaledRes.getScaledWidth() - i - k1;
        int j = 0;

        if (this.fixHeight.get()) {
            j1 = Math.max(j1, scoreBoardHeight + i1 + mc.fontRendererObj.FONT_HEIGHT + 2);
        }

        for (Score score1 : scores)
        {
            ++j;

            ScorePlayerTeam scoreplayerteam1 = scoreboard.getPlayersTeam(score1.getPlayerName());
            String s1 = ScorePlayerTeam.formatPlayerName(scoreplayerteam1, score1.getPlayerName());
            String s2 = EnumChatFormatting.RED + "" + score1.getScorePoints();
            int k = j1 - j * mc.fontRendererObj.FONT_HEIGHT;

            int l = scaledRes.getScaledWidth() - k1 + 2;
            drawRect(l1 - 2, k, l, k + mc.fontRendererObj.FONT_HEIGHT, 1342177280);

            final Matcher linkMatcher = LINK_PATTERN.matcher(s1);
            if(Moonlight.INSTANCE.getModuleManager().getModule(Interface.class).isEnabled() && linkMatcher.find()) {
                s1 = "Moonlight.wtf";
                mc.fontRendererObj.drawGradientWithShadow(s1, l1, k,(index) -> new Color(Moonlight.INSTANCE.getModuleManager().getModule(Interface.class).color(index)));
            } else {
                mc.fontRendererObj.drawString(s1, l1, k, 553648127, true);
            }

            if(!(Moonlight.INSTANCE.getModuleManager().getModule(Interface.class).isEnabled() && Moonlight.INSTANCE.getModuleManager().getModule(Interface.class).
                    hideScoreRed.get()))
                mc.fontRendererObj.drawString(s2, l - mc.fontRendererObj.getStringWidth(s2), k, 553648127);

            if (j == scores.size())
            {
                String s3 = objective.getDisplayName();
                drawRect(l1 - 2, k - mc.fontRendererObj.FONT_HEIGHT - 1, l, k - 1, 1610612736);
                drawRect(l1 - 2, k - 1, l, k, 1342177280);
                mc.fontRendererObj.drawString(s3, l1 + i / 2 - mc.fontRendererObj.getStringWidth(s3) / 2, k - mc.fontRendererObj.FONT_HEIGHT, 553648127);
            }
        }
    }

    private String intToRomanByGreedy(int num) {
        int[] values = { 1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1 };
        String[] symbols = { "M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I" };
        StringBuilder stringBuilder = new StringBuilder();
        int i = 0;
        while (i < values.length && num >= 0) {
            while (values[i] <= num) {
                num -= values[i];
                stringBuilder.append(symbols[i]);
            }
            i++;
        }
        return stringBuilder.toString();
    }

    public FontRenderer getFr() {

        FontRenderer fr = switch (fontMode.get()) {
            case "Bold" -> Fonts.interBold.get(fontSize.get());
            case "Semi Bold" -> Fonts.interSemiBold.get(fontSize.get());
            case "Regular" -> Fonts.interRegular.get(fontSize.get());
            case "Tahoma" -> Fonts.Tahoma.get(fontSize.get());
            default -> null;
        };

        return fr;
    }

    public Color getMainColor() {
        return mainColor.get();
    }

    public Color getSecondColor() {
        return secondColor.get();
    }

    public int getRainbow(int counter) {
        return Color.HSBtoRGB(getRainbowHSB(counter)[0], getRainbowHSB(counter)[1], getRainbowHSB(counter)[2]);
    }
    public static int astolfoRainbow(final int offset, final float saturation, final float brightness) {
        double currentColor = Math.ceil((double)(System.currentTimeMillis() + offset * 20L)) / 6.0;
        return Color.getHSBColor(((float)((currentColor %= 360.0) / 360.0) < 0.5) ? (-(float)(currentColor / 360.0)) : ((float)(currentColor / 360.0)), saturation, brightness).getRGB();
    }

    public float[] getRainbowHSB(int counter) {
        final int width = 20;

        double rainbowState = Math.ceil(System.currentTimeMillis() - (long) counter * width) / 8;
        rainbowState %= 360;

        float hue = (float) (rainbowState / 360);
        float saturation = mainColor.getSaturation();
        float brightness = mainColor.getBrightness();

        return new float[]{hue, saturation, brightness};
    }

    public int color() {
        return color(0);
    }

    public int color(int counter, int alpha) {
        int colors = getMainColor().getRGB();
        colors = switch (color.get()) {
            case "Rainbow" -> ColorUtils.swapAlpha(getRainbow(counter), alpha);
            case "Dynamic" ->
                    ColorUtils.swapAlpha(ColorUtils.colorSwitch(getMainColor(), new Color(ColorUtils.darker(getMainColor().getRGB(), 0.25F)), 2000.0F, counter, 75L, fadeSpeed.get()).getRGB(), alpha);
            case "Fade" ->
                    ColorUtils.swapAlpha((ColorUtils.colorSwitch(getMainColor(), getSecondColor(), 2000.0F, counter, 75L, fadeSpeed.get()).getRGB()), alpha);
            case "Astolfo" ->
                    ColorUtils.swapAlpha(astolfoRainbow(counter, mainColor.getSaturation(), mainColor.getBrightness()), alpha);
            case "NeverLose" -> ColorUtils.swapAlpha(iconRGB, alpha);
            case "Custom" -> ColorUtils.swapAlpha(mainColor.get().getRGB(), alpha);
            default -> colors;
        };
        return colors;
    }

    public int color(int counter) {
        return color(counter, (int) colorAlpha.get());
    }

    public int bgColor(int counter, int alpha) {
        int colors = getMainColor().getRGB();
        colors = switch (bgColor.get()) {
            case "Dark" -> (new Color(21, 21, 21, alpha)).getRGB();
            case "Synced" ->
                    new Color(ColorUtils.applyOpacity(color(counter, alpha), alpha / 255f), true).darker().darker().getRGB();
            case "None" -> new Color(0, 0, 0, 0).getRGB();
            case "Custom" -> ColorUtils.swapAlpha(bgCustomColor.get().getRGB(), alpha);
            case "NeverLose" -> ColorUtils.swapAlpha(NeverLose.bgColor.getRGB(), alpha);
            default -> colors;
        };
        return colors;
    }
    public int bgColor(int counter) {
        return bgColor(counter, (int) bgAlpha.get());
    }

    public int bgColor() {
        return bgColor(0);
    }
}
