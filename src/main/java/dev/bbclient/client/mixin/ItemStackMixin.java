package dev.bbclient.client.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;
import net.minecraft.item.ThrowablePotionItem;
import net.minecraft.item.consume.UseAction;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 1.7/1.8 style item behavior:
 * - Sword blocking (getUseAction → BLOCK, getMaxUseTime → 72000, use() → CONSUME)
 * - Potion glint (hasGlint → true for all potions)
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    /**
     * Make swords return BLOCK use action
     */
    @Inject(method = "getUseAction", at = @At("HEAD"), cancellable = true)
    private void onGetUseAction(CallbackInfoReturnable<UseAction> cir) {
        ItemStack self = (ItemStack)(Object)this;
        if (!self.isEmpty() && self.isIn(ItemTags.SWORDS)) {
            cir.setReturnValue(UseAction.BLOCK);
        }
    }

    /**
     * Make swords have shield-like use duration
     */
    @Inject(method = "getMaxUseTime", at = @At("HEAD"), cancellable = true)
    private void onGetMaxUseTime(LivingEntity user, CallbackInfoReturnable<Integer> cir) {
        ItemStack self = (ItemStack)(Object)this;
        if (!self.isEmpty() && self.isIn(ItemTags.SWORDS)) {
            cir.setReturnValue(72000);
        }
    }

    /**
     * Make swords actually trigger blocking when used.
     * Without this, Item.use() returns PASS for swords because they don't have
     * BLOCKS_ATTACKS component client-side (server strips it for BBClient players).
     * This mixin intercepts use() to call startUsingItem() and return CONSUME,
     * mimicking shield behavior.
     */
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void onUse(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        ItemStack self = (ItemStack)(Object)this;
        if (!self.isEmpty() && self.isIn(ItemTags.SWORDS)) {
            user.setCurrentHand(hand);
            cir.setReturnValue(ActionResult.CONSUME);
        }
    }

    /**
     * 1.7/1.8 potion glint: potions always have enchantment shimmer
     */
    @Inject(method = "hasGlint", at = @At("HEAD"), cancellable = true)
    private void bbclient$potionGlint(CallbackInfoReturnable<Boolean> cir) {
        ItemStack self = (ItemStack)(Object)this;
        if (!self.isEmpty() && (self.getItem() instanceof PotionItem || self.getItem() instanceof ThrowablePotionItem)) {
            cir.setReturnValue(true);
        }
    }
}
