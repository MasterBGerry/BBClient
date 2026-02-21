package dev.bbclient.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ShieldItem;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Handles 1.7/1.8 sword blocking animation in first person.
 * - Legacy sword blocking pose
 * - Instant block (no "raise up" animation when blocking)
 * - Instant unblock (sword stays in position, no raise animation)
 * - Normal 1.7/1.8 equip animation when switching to sword
 * - No attack cooldown visual
 */
@Mixin(value = HeldItemRenderer.class, priority = 500)
public abstract class HeldItemRendererMixin {

    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    private float equipProgressMainHand;

    @Shadow
    private ItemStack mainHand;

    // Track if we were blocking last frame to handle unblock transition
    @Unique
    private boolean bbclient$wasBlocking = false;

    /**
     * Apply 1.7/1.8 sword blocking pose.
     * Intercepts getItem() call to return SHIELD for blocking animation path,
     * but with legacy transforms applied first.
     */
    @WrapOperation(
        method = "renderFirstPersonItem",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;getItem()Lnet/minecraft/item/Item;")
    )
    private Item bbclient$applySwordBlockTransform(
            ItemStack instance,
            Operation<Item> original,
            @Local(argsOnly = true) AbstractClientPlayerEntity player,
            @Local(argsOnly = true) Hand hand,
            @Local(argsOnly = true) MatrixStack matrices
    ) {
        // Only apply to swords, not shields
        if (instance.isIn(ItemTags.SWORDS) && !(instance.getItem() instanceof ShieldItem)) {
            int direction = getHandMultiplier(player, hand);

            // Apply legacy 1.7/1.8 sword blocking transforms
            applyLegacySwordBlockTransforms(matrices, direction);

            // Return SHIELD to trigger vanilla blocking animation code path
            return Items.SHIELD;
        }

        return original.call(instance);
    }

    /**
     * Remove the attack cooldown animation (1.7/1.8 had no cooldown).
     * This makes the sword always appear "ready" with no recharge animation.
     */
    @WrapOperation(
        method = "updateHeldItems",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getAttackCooldownProgress(F)F")
    )
    private float bbclient$noAttackCooldownVisual(ClientPlayerEntity player, float baseTime, Operation<Float> original) {
        // Always return 1.0 = no cooldown visual
        return 1.0F;
    }

    /**
     * Fix blocking and unblocking animations for swords:
     * - During blocking: force equipProgressMainHand to 1.0 (instant block, no raise animation)
     * - After releasing block: force equipProgressMainHand to 1.0 (no raise animation)
     */
    @Inject(
        method = "updateHeldItems",
        at = @At("HEAD")
    )
    private void bbclient$preventBlockAnimations(CallbackInfo ci) {
        if (this.client.player == null) return;

        ItemStack currentStack = this.client.player.getMainHandStack();
        boolean holdingSword = currentStack.isIn(ItemTags.SWORDS);
        boolean isCurrentlyBlocking = this.client.player.isUsingItem() &&
                                       this.client.player.getActiveItem().isIn(ItemTags.SWORDS);

        if (holdingSword) {
            if (isCurrentlyBlocking) {
                // Currently blocking - force equipProgress to 1.0 for instant block
                this.equipProgressMainHand = 1.0F;
                this.bbclient$wasBlocking = true;
            } else if (this.bbclient$wasBlocking) {
                // Just released block - force equipProgress to 1.0 to skip unblock animation
                this.equipProgressMainHand = 1.0F;
                this.bbclient$wasBlocking = false;
            }
        } else {
            this.bbclient$wasBlocking = false;
        }
    }

    /**
     * Apply the legacy 1.7/1.8 first person sword blocking transforms.
     */
    @Unique
    private void applyLegacySwordBlockTransforms(MatrixStack matrices, int direction) {
        // Pre-transform (wrapping for precision)
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(direction * 45.0F));
        matrices.scale(0.4F, 0.4F, 0.4F);

        // Sword blocking specific transforms (classic 1.7/1.8 pose)
        matrices.translate(direction * -0.5F, 0.2F, 0.0F);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(direction * 30.0F));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-80.0F));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(direction * 60.0F));

        // Post-transform (undo wrapping)
        matrices.scale(1 / 0.4F, 1 / 0.4F, 1 / 0.4F);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(direction * -45.0F));
    }

    /**
     * Get the direction multiplier based on which hand is being used.
     */
    @Unique
    private int getHandMultiplier(AbstractClientPlayerEntity player, Hand hand) {
        Arm arm = hand == Hand.MAIN_HAND
            ? player.getMainArm()
            : player.getMainArm().getOpposite();
        return arm == Arm.RIGHT ? 1 : -1;
    }
}
