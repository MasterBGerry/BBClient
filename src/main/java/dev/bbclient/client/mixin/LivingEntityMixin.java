package dev.bbclient.client.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Handle living entity blocking state
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    /**
     * When swinging hand while blocking with sword, don't interrupt the block
     */
    @Inject(method = "swingHand(Lnet/minecraft/util/Hand;Z)V", at = @At("HEAD"))
    private void onSwingHand(Hand hand, boolean fromServerPlayer, CallbackInfo ci) {
        // Allow swinging while blocking - this is part of block & hit
        // The swing animation can play without interrupting the block state
    }
}
