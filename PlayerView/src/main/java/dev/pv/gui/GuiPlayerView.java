package dev.pv.gui;

import dev.pv.stats.BedwarsStats;
import dev.pv.stats.BedwarsStats.Mode;
import dev.pv.stats.Cosmetics;
import dev.pv.util.Format;
import dev.pv.util.Prestige;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

import java.util.Map;

public class GuiPlayerView extends GuiScreen {

    private final BedwarsStats s;
    private final GameProfile profile;
    private Mode mode = Mode.OVERALL;

    private SkinRenderer skin;
    private boolean skinTried = false;

    private ModelRenderer3D model;
    private boolean modelTried = false;
    private boolean model3dOk = false;

    // colours
    private static final int GREEN  = 0x55FF55;
    private static final int RED     = 0xFF5555;
    private static final int PURPLE  = 0xD060FF;
    private static final int YELLOW  = 0xFFFF55;
    private static final int CYAN    = 0x55FFFF;
    private static final int WHITE   = 0xF0F0F0;
    private static final int GRAY    = 0xA8A8A8;
    private static final int GOLD    = 0xFFAA00;

    // layout (computed in initGui)
    private int left, top, panelW, panelH;
    private int statLeft, colW, c1, c2, c3;

    private GuiButton btnCores, btnOverall;

    public GuiPlayerView(BedwarsStats stats) {
        this(stats, null);
    }

    public GuiPlayerView(BedwarsStats stats, GameProfile profile) {
        this.s = stats;
        this.profile = profile;
    }

    @Override public boolean doesGuiPauseGame() { return false; }

    @Override
    public void initGui() {
        panelW = Math.min(this.width - 16, 560);
        panelH = Math.min(this.height - 16, 296);
        left = (this.width - panelW) / 2;
        top = (this.height - panelH) / 2;

        statLeft = left + 92;
        int statRight = left + panelW - 14;
        colW = (statRight - statLeft) / 3;
        c1 = statLeft;
        c2 = statLeft + colW;
        c3 = statLeft + 2 * colW;

        buttonList.clear();
        int by = top + panelH - 26;
        btnCores = new GuiButton(0, left + panelW - 220, by, 100, 20, "Core Modes");
        btnOverall = new GuiButton(1, left + panelW - 114, by, 100, 20, "Overall");
        buttonList.add(btnCores);
        buttonList.add(btnOverall);
        updateModeButtons();
    }

    /** The button for the currently-selected mode is disabled (shown highlighted/greyed). */
    private void updateModeButtons() {
        if (btnCores != null) btnCores.enabled = (mode != Mode.CORES);
        if (btnOverall != null) btnOverall.enabled = (mode != Mode.OVERALL);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0) mode = Mode.CORES;
        else if (button.id == 1) mode = Mode.OVERALL;
        updateModeButtons();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        // panel
        drawRect(left, top, left + panelW, top + panelH, 0xF00C0C12);
        drawRect(left, top, left + panelW, top + 1, 0xFF3A3A4A);
        drawRect(left, top + panelH - 1, left + panelW, top + panelH, 0xFF3A3A4A);
        drawRect(left, top, left + 1, top + panelH, 0xFF3A3A4A);
        drawRect(left + panelW - 1, top, left + panelW, top + panelH, 0xFF3A3A4A);

        drawSkin(mouseX, mouseY);
        drawHeader();
        drawNetworkBlock();
        drawStatGrid();
        drawCosmetics();

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    // ---------- sections ----------

    private void drawSkin(int mouseX, int mouseY) {
        int scale = 4;
        int bodyW = 16 * scale;          // 64 px figure column
        int ox = left + 12 + (64 - bodyW) / 2;
        int oy = top + 70;
        int frameBottom = oy + 32 * scale;

        // subtle frame behind the figure
        drawRect(ox - 4, oy - 4, ox + bodyW + 4, frameBottom + 4, 0x30000000);
        drawRect(ox - 4, oy - 4, ox + bodyW + 4, oy - 3, 0xFF2A2A38);
        drawRect(ox - 4, frameBottom + 3, ox + bodyW + 4, frameBottom + 4, 0xFF2A2A38);

        // 1) try the true 3D model
        if (!modelTried) {
            modelTried = true;
            model = ModelRenderer3D.create(profile);
        }
        boolean drew = false;
        if (model != null) {
            int cx = ox + bodyW / 2;
            int feetY = frameBottom - 4;
            model3dOk = model.draw(cx, feetY, 42, (float) mouseX, (float) mouseY);
            drew = model3dOk;
        }

        // 2) fall back to the 2D body if 3D is unavailable
        if (!drew) {
            if (!skinTried) {
                skinTried = true;
                skin = SkinRenderer.from(s.skin, s.uuid);
            }
            if (skin != null) {
                skin.draw(ox, oy, scale);
            } else {
                drawRect(ox, oy, ox + bodyW, frameBottom, 0x40FFFFFF);
                drawCenteredString(fontRendererObj, "?", ox + bodyW / 2, oy + 16 * scale, GRAY);
            }
        }

        // star caption under the figure (name is already the header title)
        drawCenteredString(fontRendererObj, Prestige.tag(s.level.level),
                ox + bodyW / 2, frameBottom + 8, WHITE);
    }

    private void drawHeader() {
        int cx = (statLeft + (left + panelW - 120)) / 2;
        int y = top + 10;
        drawCenteredString(fontRendererObj, "\u00a7f\u00a7l" + s.name, cx, y, WHITE); y += 12;

        String star = Prestige.tag(s.level.level); // already has section codes
        drawCenteredString(fontRendererObj, "\u00a77Level: " + star, cx, y, WHITE); y += 12;

        String exp = "\u00a77EXP Progress: \u00a7a"
                + Format.grp(s.level.progress) + "\u00a77/\u00a7a" + Format.grp(s.level.needed);
        drawCenteredString(fontRendererObj, exp, cx, y, WHITE); y += 14;

        // progress bar with current/next star tags
        String cur = Prestige.tag(s.level.level);
        String next = Prestige.tag(s.level.level + 1);
        int barW = 150;
        int barX = cx - barW / 2;
        int barY = y + 1;
        // tags flanking the bar
        int curW = fontRendererObj.getStringWidth(stripCodes(cur));
        drawString(fontRendererObj, cur, barX - curW - 6, y, WHITE);
        drawString(fontRendererObj, next, barX + barW + 6, y, WHITE);
        // bar background + fill
        drawRect(barX, barY, barX + barW, barY + 6, 0xFF202028);
        double frac = s.level.needed == 0 ? 0 : (double) s.level.progress / (double) s.level.needed;
        if (frac < 0) frac = 0; if (frac > 1) frac = 1;
        int fill = (int) (barW * frac);
        drawRect(barX, barY, barX + fill, barY + 6, 0xFF1FE0E0);
    }

    private void drawNetworkBlock() {
        int x = left + panelW - 112;
        int y = top + 10;
        kv(x, y, GRAY, "Provider: ", GOLD, "Hypixel"); y += 12;
        kv(x, y, GOLD, "NW Level: ", WHITE, String.valueOf(s.networkLevel)); y += 12;
        kv(x, y, 0xFF55FF, "Karma: ", WHITE, Format.grp(s.karma)); y += 12;
        kv(x, y, 0x55AAFF, "Gifted: ", WHITE, Format.grp(s.gifted)); y += 12;
        kv(x, y, YELLOW, "AP: ", WHITE, Format.grp(s.achievementPoints));
    }

    private void drawStatGrid() {
        // title
        int titleY = top + 78;
        String title = "\u00a7cBedWars \u00a7fStats \u00a77(" + mode.label + ")";
        drawCenteredString(fontRendererObj, title, statLeft + (colW * 3) / 2, titleY, WHITE);

        Mode m = mode;
        long wins = s.wins(m), losses = s.losses(m);
        long kills = s.kills(m), deaths = s.deaths(m);
        long fk = s.finalKills(m), fd = s.finalDeaths(m);
        long bb = s.bedsBroken(m), bl = s.bedsLost(m);
        long games = s.gamesPlayed(m);
        if (games == 0) games = wins + losses;
        long winstreak = s.winstreak(m);
        int star = Math.max(1, s.level.level);

        // misc row
        int y = titleY + 16;
        stat(c1, y, Format.grp(s.coins), GOLD, "Coins", GRAY);
        stat(c2, y, Format.grp(games), WHITE, "Games", GRAY);
        stat(c3, y, Format.pct(s.ratio(wins, wins + losses)), PURPLE, "Win Rate", GRAY);
        y += 11;
        stat(c1, y, Format.grp(winstreak), GOLD, "Winstreak", GRAY);

        // main block
        y += 18;
        stat(c1, y, Format.grp(wins), GREEN, "Wins", GRAY);
        stat(c2, y, Format.grp(losses), RED, "Losses", GRAY);
        stat(c3, y, Format.ratio(s.ratio(wins, losses)), PURPLE, "WLR", GRAY);
        y += 12;
        stat(c1, y, Format.grp(kills), GREEN, "Kills", GRAY);
        stat(c2, y, Format.grp(deaths), RED, "Deaths", GRAY);
        stat(c3, y, Format.ratio(s.ratio(kills, deaths)), PURPLE, "KDR", GRAY);
        y += 12;
        stat(c1, y, Format.grp(fk), GREEN, "Final Kills", GRAY);
        stat(c2, y, Format.grp(fd), RED, "Final Deaths", GRAY);
        stat(c3, y, Format.ratio(s.ratio(fk, fd)), PURPLE, "FKDR", GRAY);
        y += 12;
        stat(c1, y, Format.grp(bb), GREEN, "Beds Broken", GRAY);
        stat(c2, y, Format.grp(bl), RED, "Beds Lost", GRAY);
        stat(c3, y, Format.ratio(s.ratio(bb, bl)), PURPLE, "BBLR", GRAY);

        // per game / per star
        y += 18;
        stat(c1, y, Format.ratio(s.ratio(kills, games)), YELLOW, "Kills/Game", GRAY);
        stat(c2, y, Format.ratio(s.ratio(fk, games)), YELLOW, "Finals/Game", GRAY);
        stat(c3, y, Format.ratio(s.ratio(bb, games)), YELLOW, "Beds/Game", GRAY);
        y += 12;
        stat(c1, y, Format.ratio((double) kills / star), CYAN, "Kills/Star", GRAY);
        stat(c2, y, Format.ratio((double) fk / star), CYAN, "Finals/Star", GRAY);
        stat(c3, y, Format.ratio((double) bb / star), CYAN, "Beds/Star", GRAY);
    }

    private void drawCosmetics() {
        Cosmetics c = s.cosmetics;
        int y = top + panelH - 64;
        boolean hasData = c != null && (c.available || c.finalKills >= 0 || c.bedsDestroyed >= 0);
        if (!hasData) {
            // quiet diagnostic so first-run auth issues are visible (skip the "no key" case)
            if (c != null && c.error != null && !c.error.equals("no sukie key")) {
                drawString(fontRendererObj, "\u00a78cosmetics: " + c.error, statLeft, y, GRAY);
            }
            return;
        }
        drawString(fontRendererObj, "\u00a7bCosmetics", statLeft, y, CYAN); y += 11;
        int shown = 0;
        if (c.finalKills >= 0) { stat(statLeft, y, Format.grp(c.finalKills), GREEN, "Final Kills (sukie)", GRAY); y += 11; shown++; }
        if (c.bedsDestroyed >= 0) { stat(statLeft, y, Format.grp(c.bedsDestroyed), GREEN, "Beds (sukie)", GRAY); y += 11; shown++; }
        for (Map.Entry<String, String> e : c.entries.entrySet()) {
            if (shown >= 3) break;
            kv(statLeft, y, GRAY, e.getKey() + ": ", WHITE, e.getValue());
            y += 11; shown++;
        }
    }

    // ---------- draw helpers ----------

    /** "<value> <label>" with value coloured and label coloured. */
    private void stat(int x, int y, String value, int valueColor, String label, int labelColor) {
        drawString(fontRendererObj, value, x, y, valueColor);
        int w = fontRendererObj.getStringWidth(value);
        drawString(fontRendererObj, label, x + w + 4, y, labelColor);
    }

    /** "<key><value>" with key and value coloured. */
    private void kv(int x, int y, int keyColor, String key, int valColor, String value) {
        drawString(fontRendererObj, key, x, y, keyColor);
        int w = fontRendererObj.getStringWidth(key);
        drawString(fontRendererObj, value, x + w, y, valColor);
    }

    private static String stripCodes(String s) {
        return s.replaceAll("\u00a7.", "");
    }
}
