package dev.pv.util;

import dev.pv.stats.BedwarsStats;
import dev.pv.stats.BedwarsStats.Mode;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

/** Prints the compact card from image 2 into chat (client-side only). */
public final class ChatCard {

    private ChatCard() {}

    public static void print(ICommandSender sender, BedwarsStats s) {
        Mode m = Mode.OVERALL;
        long wins = s.wins(m), losses = s.losses(m);
        long fk = s.finalKills(m), fd = s.finalDeaths(m);
        long bb = s.bedsBroken(m), bl = s.bedsLost(m);

        String pink = EnumChatFormatting.LIGHT_PURPLE.toString();
        String green = EnumChatFormatting.GREEN.toString();
        String gray = EnumChatFormatting.GRAY.toString();

        line(sender, Prestige.tag(s.level.level) + " " + EnumChatFormatting.WHITE + s.name);
        line(sender, gray + "FKDR: " + pink + Format.ratio(s.ratio(fk, fd)));
        line(sender, gray + "WLR: "  + pink + Format.ratio(s.ratio(wins, losses)));
        line(sender, gray + "BBLR: " + pink + Format.ratio(s.ratio(bb, bl)));
        line(sender, gray + "Wins: " + green + Format.grp(wins));
        line(sender, gray + "Beds: " + green + Format.grp(bb));
    }

    private static void line(ICommandSender sender, String text) {
        sender.addChatMessage(new ChatComponentText(text));
    }
}
