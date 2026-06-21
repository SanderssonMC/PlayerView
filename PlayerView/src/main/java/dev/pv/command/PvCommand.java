package dev.pv.command;

import dev.pv.PlayerView;
import dev.pv.api.HypixelApi;
import dev.pv.api.MojangApi;
import dev.pv.api.SukieApi;
import dev.pv.gui.GuiPlayerView;
import dev.pv.stats.BedwarsStats;
import dev.pv.util.ChatCard;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

public class PvCommand extends CommandBase {

    private static final String PREFIX =
            EnumChatFormatting.AQUA + "[PV] " + EnumChatFormatting.RESET;

    @Override public String getCommandName() { return "pv"; }

    @Override public String getCommandUsage(ICommandSender sender) {
        return "/pv [player] [-c]  |  /pv key <key>  |  /pv sukie <key>";
    }

    @Override public int getRequiredPermissionLevel() { return 0; }

    @Override public boolean canCommandSenderUseCommand(ICommandSender sender) { return true; }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        // --- subcommands ---
        if (args.length >= 1 && args[0].equalsIgnoreCase("key")) {
            if (args.length < 2) { msg(sender, EnumChatFormatting.RED + "Usage: /pv key <hypixelKey>"); return; }
            PlayerView.config.setHypixelKey(args[1].trim());
            msg(sender, EnumChatFormatting.GREEN + "Saved Hypixel API key.");
            return;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("sukie")) {
            if (args.length < 2) { msg(sender, EnumChatFormatting.RED + "Usage: /pv sukie <sukieKey>"); return; }
            PlayerView.config.setSukieKey(args[1].trim());
            msg(sender, EnumChatFormatting.GREEN + "Saved sukie.net API key.");
            return;
        }

        // parse: optional -c/card flag anywhere, optional player (defaults to self)
        String player = null;
        boolean wantCard = false;
        for (String a : args) {
            if (a.equalsIgnoreCase("-c") || a.equalsIgnoreCase("card")) wantCard = true;
            else if (player == null) player = a;
        }
        if (player == null) {
            if (Minecraft.getMinecraft().thePlayer == null) {
                msg(sender, EnumChatFormatting.RED + "Usage: " + getCommandUsage(sender));
                return;
            }
            player = Minecraft.getMinecraft().thePlayer.getGameProfile().getName();
        }
        final String target = player;
        final boolean card = wantCard;

        final String hypixelKey = PlayerView.config.getHypixelKey();
        if (hypixelKey.isEmpty()) {
            msg(sender, EnumChatFormatting.RED + "No Hypixel API key set. Run "
                    + EnumChatFormatting.YELLOW + "/pv key <key>" + EnumChatFormatting.RED + " first.");
            return;
        }

        msg(sender, EnumChatFormatting.GRAY + "Fetching stats for " + target + "...");

        final ICommandSender s = sender;
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    String uuid = MojangApi.uuidForName(target);
                    if (uuid == null) {
                        msg(s, EnumChatFormatting.RED + "No such player: " + target);
                        return;
                    }

                    BedwarsStats stats = HypixelApi.fetch(hypixelKey, uuid, target);

                    // best-effort cosmetics (never fatal); self-resolves auth + query style
                    stats.cosmetics = SukieApi.fetch(PlayerView.config, stats.name, uuid);

                    // resolve a real GameProfile (for the 3D model) + skin png (2D fallback)
                    MojangApi.Resolved resolved = MojangApi.resolve(uuid, stats.name);
                    final GameProfile profile = resolved.profile;
                    try {
                        if (resolved.skinUrl != null) {
                            byte[] png = dev.pv.api.HttpUtil.getBytes(resolved.skinUrl);
                            stats.skin = ImageIO.read(new ByteArrayInputStream(png));
                        }
                    } catch (Exception ignored) {}

                    // primary skin display: a clean, evenly-lit server render (never dark).
                    // Tries Crafatar, then Visage, then mc-heads.
                    stats.bodyRender = fetchImage(
                            "https://crafatar.com/renders/body/" + uuid + "?overlay&scale=10",
                            "https://visage.surgeplay.com/full/512/" + uuid,
                            "https://mc-heads.net/body/" + uuid + "/512");

                    final BedwarsStats finalStats = stats;
                    final boolean asCard = card;
                    Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                        @Override public void run() {
                            if (asCard) {
                                ChatCard.print(s, finalStats);
                            } else {
                                Minecraft.getMinecraft().displayGuiScreen(
                                        new GuiPlayerView(finalStats, profile));
                            }
                        }
                    });
                } catch (HypixelApi.HypixelException he) {
                    msg(s, EnumChatFormatting.RED + he.getMessage());
                } catch (Exception e) {
                    msg(s, EnumChatFormatting.RED + "Failed: " + e.getMessage());
                }
            }
        }, "PlayerView-fetch").start();
    }

    /** Try each URL in turn; return the first that decodes to an image, or null. */
    private static BufferedImage fetchImage(String... urls) {
        for (String u : urls) {
            try {
                byte[] bytes = dev.pv.api.HttpUtil.getBytes(u);
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
                if (img != null) return img;
            } catch (Exception ignored) {}
        }
        return null;
    }

    private static void msg(final ICommandSender sender, final String text) {
        // always deliver on the client thread
        Minecraft.getMinecraft().addScheduledTask(new Runnable() {
            @Override public void run() {
                sender.addChatMessage(new ChatComponentText(PREFIX + text));
            }
        });
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length == 1) {
            List<String> names = new ArrayList<String>();
            names.add("key");
            names.add("sukie");
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.getNetHandler() != null) {
                for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
                    names.add(info.getGameProfile().getName());
                }
            }
            return getListOfStringsMatchingLastWord(args, names.toArray(new String[0]));
        }
        return null;
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return index == 0;
    }
}
