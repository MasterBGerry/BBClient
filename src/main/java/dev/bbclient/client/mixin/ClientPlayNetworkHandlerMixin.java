package dev.bbclient.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.DamageTiltS2CPacket;
import net.minecraft.sound.SoundEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Plays hurt sound when the local player receives a DamageTilt packet.
 *
 * The server sends DamageTiltS2CPacket for fishing rod bobber hits, which only
 * triggers animateDamage() (red tilt). The hurt sound is normally played by
 * onDamaged() which requires an EntityDamageS2CPacket that the server doesn't
 * send for bobber hits. This mixin bridges that gap.
 */
@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {

    @Shadow
    private ClientWorld world;

    @Inject(method = "onDamageTilt", at = @At("TAIL"))
    private void bbclient$playHurtSoundOnDamageTilt(DamageTiltS2CPacket packet, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || this.world == null) return;

        Entity entity = this.world.getEntityById(packet.id());
        if (entity == client.player) {
            float pitch = (client.player.getRandom().nextFloat() - client.player.getRandom().nextFloat()) * 0.2F + 1.0F;
            client.player.playSound(SoundEvents.ENTITY_PLAYER_HURT, 1.0F, pitch);
        }
    }
}
