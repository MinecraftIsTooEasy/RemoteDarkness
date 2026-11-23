package vbonedra.remote_darkness.mixin;

import net.minecraft.WorldProviderHell;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vbonedra.remote_darkness.RemoteDarknessConfigs;

@Mixin(WorldProviderHell.class)
public class WorldProviderHellMixin {

    @Inject(method = "generateLightBrightnessTable", at = @At("RETURN"))
    protected void generateLightBrightnessTable_Darkness(CallbackInfo ci) {
        if (RemoteDarknessConfigs.EnableDarkNether.getBooleanValue()) {
            WorldProviderHell self = (WorldProviderHell) (Object) this;

            float var1 = 0.0F;
            for(int var2 = 0; var2 <= 15; ++var2) {
                float var3 = 1.0F - (float)var2 / 15.0F;
                self.lightBrightnessTable[var2] = (1.0F - var3) / (var3 * 3.0F + 1.0F) * (1.0F - var1) + var1;
                if (var2 > 0 && var2 < 15) {
                    float extra_drop_off = 0.001F * (float)(15 - var2);
                    if (extra_drop_off > 0.0F) {
                        float[] var10000 = self.lightBrightnessTable;
                        var10000[var2] -= extra_drop_off;
                    }
                }
            }

        }
    }

}
