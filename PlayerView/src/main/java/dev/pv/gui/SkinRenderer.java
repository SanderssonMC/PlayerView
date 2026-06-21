package dev.pv.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import java.awt.image.BufferedImage;

/**
 * Draws a player skin as a flat, front-facing body (head, torso, arms, legs + overlays).
 * Extends Gui so we can use drawScaledCustomSizeModalRect for UV-mapped scaling.
 * Handles modern 64x64 skins and legacy 64x32 (mirrors the right limbs).
 */
public class SkinRenderer extends Gui {

    private final ResourceLocation texture;
    private final boolean slim64; // true if 64x64 (has separate left arm/leg)

    private SkinRenderer(ResourceLocation texture, boolean is64) {
        this.texture = texture;
        this.slim64 = is64;
    }

    /** Build from a BufferedImage. MUST be called on the render thread (uploads to GL). */
    public static SkinRenderer from(BufferedImage img, String id) {
        if (img == null) return null;
        try {
            boolean is64 = img.getHeight() >= 64;
            DynamicTexture dyn = new DynamicTexture(img);
            ResourceLocation rl = Minecraft.getMinecraft().getTextureManager()
                    .getDynamicTextureLocation("pv_skin_" + id, dyn);
            return new SkinRenderer(rl, is64);
        } catch (Exception e) {
            return null;
        }
    }

    /** Draws the body with top-left at (ox, oy), each skin pixel = scale screen pixels. */
    public void draw(int ox, int oy, int scale) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        int s = scale;
        // base layer
        part(ox + 4 * s, oy,             8,  8, 8,  8,  s); // head     (8,8)
        part(ox + 4 * s, oy + 8 * s,    20, 20, 8, 12,  s); // torso    (20,20)
        part(ox,         oy + 8 * s,    44, 20, 4, 12,  s); // right arm(44,20)
        part(ox + 4 * s, oy + 20 * s,    4, 20, 4, 12,  s); // right leg(4,20)

        if (slim64) {
            part(ox + 12 * s, oy + 8 * s,  36, 52, 4, 12, s); // left arm (36,52)
            part(ox + 8 * s,  oy + 20 * s, 20, 52, 4, 12, s); // left leg (20,52)
        } else {
            part(ox + 12 * s, oy + 8 * s,  44, 20, 4, 12, s); // mirror right arm
            part(ox + 8 * s,  oy + 20 * s,  4, 20, 4, 12, s); // mirror right leg
        }

        // overlay layer (hat / jacket / sleeves / pants)
        part(ox + 4 * s, oy,             40, 8,  8,  8, s); // hat      (40,8)
        part(ox + 4 * s, oy + 8 * s,     20, 36, 8, 12, s); // jacket   (20,36)
        part(ox,         oy + 8 * s,     44, 36, 4, 12, s); // r sleeve (44,36)
        part(ox + 4 * s, oy + 20 * s,     4, 36, 4, 12, s); // r pants  (4,36)
        if (slim64) {
            part(ox + 12 * s, oy + 8 * s,  52, 52, 4, 12, s); // l sleeve (52,52)
            part(ox + 8 * s,  oy + 20 * s,  4, 52, 4, 12, s); // l pants  (4,52)
        }
        GlStateManager.disableBlend();
    }

    private void part(int x, int y, int u, int v, int w, int h, int scale) {
        // drawScaledCustomSizeModalRect(x, y, u, v, uWidth, vHeight, drawW, drawH, texW, texH)
        drawScaledCustomSizeModalRect(x, y, u, v, w, h, w * scale, h * scale, 64f, 64f);
    }
}
