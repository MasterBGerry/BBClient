package dev.bbclient.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.CraftingTableBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.ButtonBlock;
import net.minecraft.block.LeverBlock;
import net.minecraft.block.AbstractSignBlock;
import net.minecraft.block.BedBlock;
import net.minecraft.block.CakeBlock;
import net.minecraft.block.NoteBlock;
import net.minecraft.block.ComposterBlock;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.block.DragonEggBlock;
import net.minecraft.block.FlowerPotBlock;
import net.minecraft.block.LecternBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.bbclient.client.combat.CpsParticleHandler;

/**
 * 1.7/1.8 style dual actions:
 * - Mining while using item (eating, blocking, charging bow)
 * - Using items while mining
 * - Sword blocking takes priority over block/entity interaction
 *
 * NOTE: Hit & Block (hit then block) is vanilla behavior - no changes needed
 * Block & Hit (block then hit) is NOT allowed - vanilla behavior blocks attacks while using item
 */
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    @Shadow
    public ClientPlayerEntity player;

    @Shadow
    public ClientPlayerInteractionManager interactionManager;

    @Shadow
    public ClientWorld world;

    /**
     * Release sword blocking and cancel block breaking when a GUI screen opens.
     * - Sword blocking: right-clicking a sword in lobby opens a menu but the blocking
     *   animation stays frozen. This releases it.
     * - Block breaking: our simultaneous actions mixins allow mining while using items,
     *   but when a GUI opens we must cancel the break to prevent ghost blocks (client
     *   breaks the block visually but server rejects it because player is in a GUI).
     */
    @Inject(method = "setScreen", at = @At("HEAD"))
    private void bbclient$releaseBlockingOnScreenOpen(Screen screen, CallbackInfo ci) {
        if (screen != null && this.player != null) {
            // Release sword blocking
            if (this.player.isUsingItem() && this.player.getActiveItem().isIn(ItemTags.SWORDS)) {
                this.player.stopUsingItem();
            }
            // Cancel any ongoing block breaking to prevent ghost blocks
            if (this.interactionManager != null) {
                this.interactionManager.cancelBlockBreaking();
            }
        }
    }

    /**
     * CPS Particles: spawn particles on every left click, not just on hit.
     * This hooks into doAttack() which is called on every attack key press.
     */
    @Inject(method = "doAttack", at = @At("HEAD"))
    private void bbclient$onCpsClick(CallbackInfoReturnable<Boolean> cir) {
        CpsParticleHandler.onLeftClick();
    }

    /**
     * Allow using items while mining (startUseItem checks isDestroying)
     * In 1.7/1.8, you could eat/block while breaking blocks
     */
    @WrapOperation(
        method = "doItemUse",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;isBreakingBlock()Z")
    )
    private boolean bbclient$allowItemUseWhileMining(ClientPlayerInteractionManager instance, Operation<Boolean> original) {
        // Return false to bypass the "is breaking block" check
        // This allows using items (eating, blocking) while mining
        return false;
    }

    @Shadow
    public Screen currentScreen;

    /**
     * Allow mining while using item (continueAttack checks isUsingItem)
     * In 1.7/1.8, you could continue mining while eating/blocking.
     * EXCEPTION: If a GUI screen is open, return true (block mining) to prevent
     * ghost blocks — the server won't process block breaks while player is in a GUI.
     */
    @WrapOperation(
        method = "handleBlockBreaking",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z")
    )
    private boolean bbclient$allowMiningWhileUsingItem(ClientPlayerEntity instance, Operation<Boolean> original) {
        // If a GUI is open, don't bypass — block mining to prevent ghost blocks
        if (this.currentScreen != null) {
            return original.call(instance);
        }
        // Return false to bypass the "is using item" check
        // This allows mining while using items (eating, blocking)
        return false;
    }

    /**
     * 1.7/1.8 sword blocking priority over block interaction.
     * When holding a sword:
     * - Non-interactive blocks (stone, dirt...): skip interactBlock entirely → sword blocking
     * - Interactive blocks (chest, furnace, door...): let interaction happen normally
     * - Sneaking + interactive block: force sword blocking (skip interaction)
     *
     * IMPORTANT: We check block interactivity BEFORE calling the original to avoid
     * sending UseItemOnPacket to the server (which would trigger server-side blocking).
     */
    @WrapOperation(
        method = "doItemUse",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;interactBlock(Lnet/minecraft/client/network/ClientPlayerEntity;Lnet/minecraft/util/Hand;Lnet/minecraft/util/hit/BlockHitResult;)Lnet/minecraft/util/ActionResult;")
    )
    private ActionResult bbclient$swordBlockingOverBlock(ClientPlayerInteractionManager instance, ClientPlayerEntity player, Hand hand, BlockHitResult hitResult, Operation<ActionResult> original) {
        if (player.getStackInHand(hand).isIn(ItemTags.SWORDS)) {
            if (player.isSneaking()) {
                // Sneaking + sword = always force sword blocking, even on interactive blocks
                return ActionResult.PASS;
            }
            // Check if the targeted block is interactive BEFORE calling original (no packet sent)
            BlockState blockState = this.world.getBlockState(hitResult.getBlockPos());
            if (bbclient$isInteractiveBlock(blockState)) {
                // Block is interactive (chest, door, etc.) — let vanilla handle it
                return original.call(instance, player, hand, hitResult);
            }
            // Block is NOT interactive — skip interactBlock, fall through to interactItem() for sword blocking
            return ActionResult.PASS;
        }
        return original.call(instance, player, hand, hitResult);
    }

    /**
     * Check if a block is interactive (responds to right-click).
     * Covers containers (chest, furnace, etc.), doors, levers, buttons, etc.
     */
    @Unique
    private static boolean bbclient$isInteractiveBlock(BlockState blockState) {
        net.minecraft.block.Block block = blockState.getBlock();
        // BlockWithEntity covers: Chest, Furnace, Hopper, Dispenser, Dropper, Brewing Stand,
        // Enchanting Table, Beacon, Shulker Box, Barrel, Smoker, Blast Furnace, etc.
        if (block instanceof BlockWithEntity) return true;
        // Other interactive blocks without block entities
        if (block instanceof DoorBlock) return true;
        if (block instanceof FenceGateBlock) return true;
        if (block instanceof TrapdoorBlock) return true;
        if (block instanceof ButtonBlock) return true;
        if (block instanceof LeverBlock) return true;
        if (block instanceof CraftingTableBlock) return true;
        if (block instanceof AbstractSignBlock) return true;
        if (block instanceof BedBlock) return true;
        if (block instanceof CakeBlock) return true;
        if (block instanceof NoteBlock) return true;
        if (block instanceof ComposterBlock) return true;
        if (block instanceof RespawnAnchorBlock) return true;
        if (block instanceof DragonEggBlock) return true;
        if (block instanceof FlowerPotBlock) return true;
        if (block instanceof LecternBlock) return true;
        return false;
    }

    /**
     * 1.7/1.8 sword blocking priority over entity interaction (interactEntityAtLocation).
     * When holding a sword and right-clicking an entity, skip the entity interaction
     * and let the flow fall through to interactItem() for sword blocking.
     */
    @WrapOperation(
        method = "doItemUse",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;interactEntityAtLocation(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/entity/Entity;Lnet/minecraft/util/hit/EntityHitResult;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/ActionResult;")
    )
    private ActionResult bbclient$swordBlockingOverEntityAt(ClientPlayerInteractionManager instance, PlayerEntity player, Entity entity, EntityHitResult hitResult, Hand hand, Operation<ActionResult> original) {
        if (player.getStackInHand(hand).isIn(ItemTags.SWORDS)) {
            return ActionResult.PASS;
        }
        return original.call(instance, player, entity, hitResult, hand);
    }

    /**
     * 1.7/1.8 sword blocking priority over entity interaction (interactEntity).
     * This is the fallback entity interaction call after interactEntityAtLocation.
     */
    @WrapOperation(
        method = "doItemUse",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;interactEntity(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/entity/Entity;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/ActionResult;")
    )
    private ActionResult bbclient$swordBlockingOverEntity(ClientPlayerInteractionManager instance, PlayerEntity player, Entity entity, Hand hand, Operation<ActionResult> original) {
        if (player.getStackInHand(hand).isIn(ItemTags.SWORDS)) {
            return ActionResult.PASS;
        }
        return original.call(instance, player, entity, hand);
    }
}
