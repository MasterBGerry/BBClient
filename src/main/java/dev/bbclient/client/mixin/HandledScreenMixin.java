package dev.bbclient.client.mixin;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
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

    // Offhand slot index in player inventory screen is 45
    private static final int OFFHAND_SLOT_INDEX = 45;
    // Offhand slot in inventory is 40
    private static final int OFFHAND_INVENTORY_SLOT = 40;

    /**
     * Block all interactions with the offhand slot
     */
    @Inject(
        method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void bbclient$blockOffhandSlotClick(Slot slot, int slotId, int button, SlotActionType actionType, CallbackInfo ci) {
        if (slot == null) return;

        // Block clicks on the offhand slot itself
        if (slot.id == OFFHAND_SLOT_INDEX || slot.getIndex() == OFFHAND_INVENTORY_SLOT) {
            ci.cancel();
            return;
        }

        // Block swap to offhand (number key for offhand slot - button 40)
        if (actionType == SlotActionType.SWAP && button == OFFHAND_INVENTORY_SLOT) {
            ci.cancel();
            return;
        }
    }
}
