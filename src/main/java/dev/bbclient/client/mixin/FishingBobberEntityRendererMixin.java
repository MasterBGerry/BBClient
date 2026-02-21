package dev.bbclient.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.FishingBobberEntityRenderer;
import net.minecraft.entity.projectile.FishingBobberEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hides the fishing bobber when it is hooked on the local player.
 * In 1.7/1.8, the bobber was not visible stuck to your face when you got rod-hit.
 */
@Mixin(FishingBobberEntityRenderer.class)
public abstract class FishingBobberEntityRendererMixin {

    @Inject(method = "shouldRender(Lnet/minecraft/entity/projectile/FishingBobberEntity;Lnet/minecraft/client/render/Frustum;DDD)Z", at = @At("HEAD"), cancellable = true)
    private void bbclient$hideBobberOnLocalPlayer(FishingBobberEntity entity, Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && entity.getHookedEntity() == client.player) {
            cir.setReturnValue(false);
        }
    }
}
