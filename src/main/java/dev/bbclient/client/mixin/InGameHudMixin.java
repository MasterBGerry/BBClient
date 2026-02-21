package dev.bbclient.client.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;
import net.minecraft.item.ThrowablePotionItem;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.7/1.8 shiny potion glint on the hotbar.
 * Draws a pulsing purple overlay on potion slots in the hotbar.
 */
@Mixin(InGameHud.class)
public abstract class InGameHudMixin {

    /**
     * After each hotbar item is rendered, draw the potion glint overlay
     * on the full 16x16 slot area if the item is a potion.
     */
    @Inject(
        method = "renderHotbarItem",
        at = @At("TAIL")
    )
    private void bbclient$hotbarPotionGlint(DrawContext context, int x, int y, RenderTickCounter tickCounter, PlayerEntity player, ItemStack stack, int seed, CallbackInfo ci) {
        if (stack.isEmpty()) return;
        if (!(stack.getItem() instanceof PotionItem) && !(stack.getItem() instanceof ThrowablePotionItem)) return;

        // Pulsing alpha for the shimmer effect (same as inventory)
        long time = Util.getMeasuringTimeMs();
        double pulse = (Math.sin(time / 300.0) + 1.0) / 2.0;

        // Pink glint color: RGB(255, 100, 200)
        int r = 255, g = 100, b = 200;

        // Border (stronger) — 1px edges
        int borderAlpha = (int)(120 + pulse * 135); // 120 to 255
        int borderColor = (borderAlpha << 24) | (r << 16) | (g << 8) | b;
        context.fill(x, y, x + 16, y + 1, borderColor);           // top
        context.fill(x, y + 15, x + 16, y + 16, borderColor);     // bottom
        context.fill(x, y + 1, x + 1, y + 15, borderColor);       // left
        context.fill(x + 15, y + 1, x + 16, y + 15, borderColor); // right

        // Center (softer)
        int centerAlpha = (int)(50 + pulse * 80); // 50 to 130
        int centerColor = (centerAlpha << 24) | (r << 16) | (g << 8) | b;
        context.fill(x + 1, y + 1, x + 15, y + 15, centerColor);
    }
}
