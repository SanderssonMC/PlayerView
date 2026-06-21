package dev.pv.gui;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.gui.inventory.GuiInventory;

/**
 * Renders a true 3D player model in the GUI by constructing a real client-side
 * player entity from the target's GameProfile and drawing it with the same routine
 * the vanilla inventory uses. Everything is wrapped defensively: if the world is
 * missing or rendering throws, the caller falls back to the 2D skin renderer.
 */
public class ModelRenderer3D {

    private final EntityOtherPlayerMP entity;

    private ModelRenderer3D(EntityOtherPlayerMP entity) {
        this.entity = entity;
    }

    /** Build a model entity, or null if it can't be created (e.g. no world yet). */
    public static ModelRenderer3D create(GameProfile profile) {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (profile == null || mc.theWorld == null) return null;
            EntityOtherPlayerMP e = new EntityOtherPlayerMP(mc.theWorld, profile);
            e.rotationYaw = 0f;
            e.rotationPitch = 0f;
            e.rotationYawHead = 0f;
            e.prevRotationYawHead = 0f;
            return new ModelRenderer3D(e);
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Draw the model with its feet at (cx, feetY), scaled by {@code scale}.
     * The figure gently turns to follow the cursor, like the inventory model.
     * Returns false if rendering threw (caller should fall back to 2D).
     */
    public boolean draw(int cx, int feetY, int scale, float mouseX, float mouseY) {
        try {
            GuiInventory.drawEntityOnScreen(cx, feetY, scale,
                    (float) cx - mouseX, (float) (feetY - 100) - mouseY, entity);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
