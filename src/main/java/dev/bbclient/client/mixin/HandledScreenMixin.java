package dev.bbclient.client.mixin;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.7/1.8 style inventory:
 * - Block offhand slot interactions
 */
@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin<T extends ScreenHandler> {

    // Offhand slot in PlayerInventory is index 40
    private static final int OFFHAND_INVENTORY_SLOT = 40;

    /**
     * Block all interactions with the offhand slot.
     * Only blocks the actual offhand slot in the player's inventory,
     * not chest/container slots that happen to share the same index.
     */
    @Inject(
        method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void bbclient$blockOffhandSlotClick(Slot slot, int slotId, int button, SlotActionType actionType, CallbackInfo ci) {
        if (slot == null) return;

        // Only block the offhand slot if it belongs to the player's inventory
        // (not a chest/container slot with the same index)
        if (slot.inventory instanceof PlayerInventory && slot.getIndex() == OFFHAND_INVENTORY_SLOT) {
            ci.cancel();
            return;
        }

        // Block swap to offhand (F key swap)
        if (actionType == SlotActionType.SWAP && button == OFFHAND_INVENTORY_SLOT) {
            ci.cancel();
            return;
        }
    }
}
