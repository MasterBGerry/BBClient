package dev.bbclient.client.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Extends leaf particle render distance from 2 chunks (32 blocks) to 6 chunks (96 blocks).
 * Vanilla particles stay at default range. Only leaf drip particles get the extended range.
 */
@Mixin(ClientWorld.class)
public abstract class ClientWorldMixin {

    /**
     * After vanilla doRandomBlockDisplayTicks runs (default 2 chunk range),
     * do extra iterations specifically for leaf blocks in the extended 3-6 chunk range.
     */
    @Inject(method = "doRandomBlockDisplayTicks", at = @At("TAIL"))
    private void bbclient$extendLeafParticleRange(int centerX, int centerY, int centerZ, CallbackInfo ci) {
        Random rand = Random.create();
        BlockPos.Mutable pos = new BlockPos.Mutable();
        ClientWorld self = (ClientWorld)(Object) this;

        // Sample random blocks in the 33-96 block range (beyond vanilla's 32)
        // We do ~2000 iterations to get decent leaf coverage in the extended ring
        for (int i = 0; i < 2000; i++) {
            // Pick a random block in the 96-block radius cube
            int x = centerX + rand.nextInt(96) - rand.nextInt(96);
            int y = centerY + rand.nextInt(96) - rand.nextInt(96);
            int z = centerZ + rand.nextInt(96) - rand.nextInt(96);

            // Skip blocks within vanilla range (already handled)
            int dx = x - centerX;
            int dy = y - centerY;
            int dz = z - centerZ;
            if (Math.abs(dx) <= 32 && Math.abs(dy) <= 32 && Math.abs(dz) <= 32) {
                continue;
            }

            pos.set(x, y, z);
            BlockState state = self.getBlockState(pos);

            if (state.getBlock() instanceof LeavesBlock) {
                state.getBlock().randomDisplayTick(state, self, pos, rand);
            }
        }
    }
}
