package dev.bbclient.client.combat;

import dev.bbclient.client.BBClientMod;
import dev.bbclient.client.protocol.BBProtocol;
import dev.bbclient.client.protocol.BBServerConnection;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * Handles client-side combat mechanics for 1.7/1.8 PvP
 */
public class ClientCombatHandler {

    // Extended reach for 1.7/1.8 style combat
    public static final double EXTENDED_REACH = 3.0;
    public static final double HITBOX_EXPANSION = 0.1;

    // Sprint reset state
    private static long lastAttackTime = 0;
    private static boolean wasSprintingBeforeAttack = false;
    private static int sprintBlockTicks = 0;

    /**
     * Called when player attacks an entity
     */
    public static void onAttack(Entity target) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;

        if (player == null || target == null) return;

        // Sprint reset disabled - players must W-tap manually
        // This is more skill-based and fair for PvP

        lastAttackTime = System.currentTimeMillis();

        // Send combat action to server if connected to BB server
        if (BBServerConnection.isConnectedToBBServer()) {
            double distance = player.getPos().distanceTo(target.getPos());
            BBServerConnection.sendCombatAction(BBProtocol.COMBAT_ATTACK, target.getId(), distance);
        }
    }

    /**
     * Check if sprint should be blocked (for W-tap timing)
     * Disabled - players must W-tap manually
     */
    public static boolean shouldBlockSprint() {
        return false; // Disabled - manual W-tap only
    }

    /**
     * Get extended entity hit result for 1.7/1.8 hitbox
     */
    public static EntityHitResult getExtendedEntityHitResult(ClientPlayerEntity player, double reach) {
        Vec3d eyePos = player.getCameraPosVec(1.0F);
        Vec3d lookVec = player.getRotationVec(1.0F);
        Vec3d endPos = eyePos.add(lookVec.multiply(reach));

        Box searchBox = player.getBoundingBox().stretch(lookVec.multiply(reach)).expand(1.0, 1.0, 1.0);

        EntityHitResult result = null;
        double closestDistance = reach * reach;

        for (Entity entity : player.getWorld().getOtherEntities(player, searchBox, e -> !e.isSpectator() && e.canHit())) {
            // Expand hitbox slightly for 1.7/1.8 feel (only on BB servers)
            double expansion = BBServerConnection.isConnectedToBBServer() ? HITBOX_EXPANSION : 0.0;
            Box entityBox = entity.getBoundingBox().expand(expansion);
            var hitResult = entityBox.raycast(eyePos, endPos);

            if (hitResult.isPresent()) {
                double dist = eyePos.squaredDistanceTo(hitResult.get());
                if (dist < closestDistance) {
                    closestDistance = dist;
                    result = new EntityHitResult(entity, hitResult.get());
                }
            } else if (entityBox.contains(eyePos)) {
                if (closestDistance >= 0) {
                    result = new EntityHitResult(entity, eyePos);
                    closestDistance = 0;
                }
            }
        }

        return result;
    }

    /**
     * Get effective reach distance
     * Only extends reach on BadBuck servers — vanilla reach on other servers
     */
    public static double getEffectiveReach() {
        if (BBServerConnection.isConnectedToBBServer()) {
            return EXTENDED_REACH;
        }
        return 3.0; // Vanilla reach
    }

    /**
     * Check if extended hitbox should be used
     * Only enabled on BadBuck servers to avoid anti-cheat flags
     */
    public static boolean shouldUseExtendedHitbox() {
        return BBServerConnection.isConnectedToBBServer();
    }

    /**
     * Check if sprint was reset recently (for animation purposes)
     */
    public static boolean wasSprintResetRecently() {
        return wasSprintingBeforeAttack && (System.currentTimeMillis() - lastAttackTime) < 100;
    }

    /**
     * Tick handler - called every client tick
     */
    public static void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;

        // Decrement sprint block ticks
        if (sprintBlockTicks > 0) {
            sprintBlockTicks--;
            // Force stop sprint during block period
            if (player != null && player.isSprinting()) {
                player.setSprinting(false);
            }
        }

        // Reset sprint flag after delay
        if (wasSprintingBeforeAttack && (System.currentTimeMillis() - lastAttackTime) > 100) {
            wasSprintingBeforeAttack = false;
        }
    }
}
