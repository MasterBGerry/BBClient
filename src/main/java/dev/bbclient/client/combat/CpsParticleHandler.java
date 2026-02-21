package dev.bbclient.client.combat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

/**
 * Handles spawning sharpness + crit particles on every left click (CPS-based)
 * instead of only on successful hits.
 *
 * Uses the EXACT same method as vanilla: particleManager.addEmitter(entity, particleType)
 * which creates an emitter that follows the entity and spawns particles over a few ticks.
 *
 * Vanilla sends EntityAnimationS2CPacket with animation ID 4 (CRIT) or 5 (ENCHANTED_HIT)
 * from the server, which calls particleManager.addEmitter on the client.
 * We replicate that same call but triggered on every CPS click instead of on server hit.
 */
public class CpsParticleHandler {

    private static boolean enabled = true;

    /**
     * Called on every left click (attack key press).
     * Checks vanilla conditions and spawns the appropriate particles
     * using the exact same method vanilla uses (particleManager.addEmitter).
     */
    public static void onLeftClick() {
        if (!enabled) return;

        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;

        if (player == null || client.world == null) return;

        // Only spawn particles when aiming at an entity
        HitResult crosshairTarget = client.crosshairTarget;
        if (crosshairTarget == null) return;

        if (crosshairTarget.getType() != HitResult.Type.ENTITY || !(crosshairTarget instanceof EntityHitResult entityHit)) {
            return;
        }

        Entity target = entityHit.getEntity();

        // Check conditions — same logic as vanilla but triggered on CPS instead of hit
        boolean hasDamageEnchant = hasAttackEnchantments(player);
        boolean isCrit = isCriticalHit(player);

        // Use particleManager.addEmitter() — the EXACT same method vanilla uses
        // in ClientPlayNetworkHandler.onEntityAnimation() for animation IDs 4 and 5
        if (isCrit) {
            client.particleManager.addEmitter(target, ParticleTypes.CRIT);
        }

        if (hasDamageEnchant) {
            client.particleManager.addEmitter(target, ParticleTypes.ENCHANTED_HIT);
        }
    }

    /**
     * Check if the player's main hand item has damage enchantments (sharpness, smite, bane, etc.)
     */
    private static boolean hasAttackEnchantments(ClientPlayerEntity player) {
        ItemStack mainHand = player.getMainHandStack();
        if (mainHand.isEmpty()) return false;

        ItemEnchantmentsComponent enchantments = mainHand.get(DataComponentTypes.ENCHANTMENTS);
        return enchantments != null && !enchantments.isEmpty();
    }

    /**
     * Check if the player currently meets critical hit conditions (1.7/1.8 style).
     */
    private static boolean isCriticalHit(ClientPlayerEntity player) {
        return player.fallDistance > 0.0F
            && !player.isOnGround()
            && !player.isClimbing()
            && !player.isTouchingWater()
            && !player.hasStatusEffect(StatusEffects.BLINDNESS)
            && !player.hasVehicle();
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static boolean isEnabled() {
        return enabled;
    }
}
