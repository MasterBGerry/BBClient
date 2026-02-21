package dev.bbclient.client.mixin;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Disable offhand swap (F key) - 1.7/1.8 style
 */
@Mixin(PlayerInventory.class)
public class PlayerInventoryMixin {

    /**
     * Block the F key swap between main hand and offhand
     */
    @Inject(
        method = "swapSlotWithHotbar",
        at = @At("HEAD"),
        cancellable = true
    )
    private void bbclient$blockOffhandSwap(int slot, CallbackInfo ci) {
        // Cancel the swap entirely - no offhand in 1.7/1.8
        ci.cancel();
    }
}
