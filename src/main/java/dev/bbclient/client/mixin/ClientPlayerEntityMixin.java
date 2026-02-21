package dev.bbclient.client.mixin;

import dev.bbclient.client.combat.ClientCombatHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Handle player state for 1.7/1.8 combat
 */
@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin extends LivingEntity {

    protected ClientPlayerEntityMixin() {
        super(null, null);
    }

    /**
     * Block sprint during W-tap window only
     * Sprint blocking during item usage is handled server-side to avoid sync issues
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void bbclient$onTick(CallbackInfo ci) {
        ClientPlayerEntity self = (ClientPlayerEntity)(Object)this;

        // Block sprint during W-tap window only
        if (ClientCombatHandler.shouldBlockSprint() && self.isSprinting()) {
            self.setSprinting(false);
        }
    }
}
