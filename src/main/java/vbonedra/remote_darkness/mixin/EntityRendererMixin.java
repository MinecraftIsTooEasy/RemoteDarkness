package vbonedra.remote_darkness.mixin;

import net.minecraft.*;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vbonedra.remote_darkness.RemoteDarknessConfigs;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {

    @Shadow private Minecraft mc;
    @Shadow float torchFlickerX;
    @Shadow private float field_82831_U;
    @Shadow private float field_82832_V;
    @Final @Shadow private int[] lightmapColors;
    @Final @Shadow private DynamicTexture lightmapTexture;
    @Shadow private boolean lightmapUpdateNeeded;

    @Shadow protected abstract float getNightVisionBrightness(EntityPlayer entityPlayer, float partialTick);

    @Unique
    private static boolean hasNoNightVision() {
        EntityPlayer entityPlayer = Minecraft.getMinecraft().thePlayer;
        return !Minecraft.night_vision_override && (entityPlayer == null || !entityPlayer.isPotionActive(Potion.nightVision));
    }

    @Unique
    private static float computeMoonBrightness(WorldClient world, float partialTick) {
        float brightness = world.getMoonBrightness(partialTick, true);
        if (RemoteDarknessConfigs.EnableTrueDarkness.getBooleanValue()
                && world.isOverworld()
                && !world.isBloodMoon24HourPeriod()
                && !world.isHarvestMoon24HourPeriod()
                && !world.isBlueMoon24HourPeriod()) {
            float moonBrightness = world.getCurrentMoonPhaseFactor() * 1.25F;
            float rainStrength = world.getRainStrength(partialTick);
            if (moonBrightness > 0.75F && rainStrength > 0.0F) {
                moonBrightness = moonBrightness * (1.0F - rainStrength) + 0.75F * rainStrength;
            }
            return moonBrightness;
        }
        return brightness;
    }

    @Unique
    private static float computeNetherLightValue(int lightLevel) {
        float inverseLevel = 1.0F - (float) lightLevel / 15.0F;
        float lightValue = (1.0F - inverseLevel) / (inverseLevel * 3.0F + 1.0F);
        if (lightLevel > 0 && lightLevel < 15) {
            float falloffOffset = 0.001F * (float) (15 - lightLevel);
            if (falloffOffset > 0.0F) {
                lightValue -= falloffOffset;
            }
        }
        return Math.max(0.0F, lightValue);
    }

    @Unique
    private static float clamp01(float value) {
        return Math.max(Math.min(value, 1.0F), 0.0F);
    }

    @Inject(method = "updateLightmap", at = @At("HEAD"), cancellable = true)
    private void remoteDarkness$updateLightmap(float partialTick, CallbackInfo ci) {
        if (!RemoteDarknessConfigs.EnableTrueDarkness.getBooleanValue()) {
            return;
        }

        WorldClient world = this.mc.theWorld;
        if (world == null) {
            ci.cancel();
            return;
        }

        boolean nightVisionActive = this.mc.thePlayer != null
                && (this.mc.thePlayer.isPotionActive(Potion.nightVision) || Minecraft.night_vision_override);
        boolean darkDimensionsEnabled = RemoteDarknessConfigs.EnableDarkDimensions.getBooleanValue();
        boolean darkNetherEnabled = darkDimensionsEnabled
                && RemoteDarknessConfigs.OverrideNetherLightmap.getBooleanValue()
                && world.provider.dimensionId == -1;
        boolean removeEndMinimum = darkDimensionsEnabled
                && RemoteDarknessConfigs.RemoveEndMinValues.getBooleanValue()
                && hasNoNightVision();

        for (int cellIndex = 0; cellIndex < 256; ++cellIndex) {
            int skyLightIndex = cellIndex / 16;
            int blockLightIndex = cellIndex % 16;

            float skyLightRatio = (float) skyLightIndex / 15.0F;
            float blockLightRatio = (float) blockLightIndex / 15.0F;
            float sunBrightnessFactor = world.getSunBrightness(1.0F) * 0.95F + 0.05F;

            float skyBaseLight = darkNetherEnabled
                    ? computeNetherLightValue(skyLightIndex)
                    : world.provider.lightBrightnessTable[skyLightIndex];
            float blockBaseLight = darkNetherEnabled
                    ? computeNetherLightValue(blockLightIndex)
                    : world.provider.lightBrightnessTable[blockLightIndex];

            float skyLight = skyBaseLight * sunBrightnessFactor;
            float blockLight = blockBaseLight * (this.torchFlickerX * 0.1F + 1.5F);

            if (world.isOverworld() && world.getMoonAscensionFactor() > 0.0F) {
                float moonInfluence = MathHelper.clamp_float(world.getMoonAscensionFactor(), 0.0F, 0.2F) * 5.0F;
                skyLight = skyLight * (1.0F - moonInfluence) + skyLight * moonInfluence * computeMoonBrightness(world, partialTick);
            }

            if (world.lastLightningBolt > 0) {
                float lightningBlend = (float) Math.pow(this.mc.raining_strength_for_render_view_entity, 4.0D);
                skyLight = skyBaseLight * lightningBlend + skyLight * (1.0F - lightningBlend);
            }

            float sunTint = world.getSunBrightness(1.0F) * 0.65F + 0.35F;
            float r = skyLight * sunTint;
            float g = skyLight * sunTint;
            float b = skyLight;

            float blockG = blockLight * ((blockLight * 0.6F + 0.4F) * 0.6F + 0.4F);
            float blockB = blockLight * (blockLight * blockLight * 0.6F + 0.4F);

            r += blockLight;
            g += blockG;
            b += blockB;

            if (this.field_82831_U > 0.0F) {
                float damageTintBlend = this.field_82832_V + (this.field_82831_U - this.field_82832_V) * partialTick;
                r = r * (1.0F - damageTintBlend) + r * 0.7F * damageTintBlend;
                g = g * (1.0F - damageTintBlend) + g * 0.6F * damageTintBlend;
                b = b * (1.0F - damageTintBlend) + b * 0.6F * damageTintBlend;
            }

            if (world.provider.dimensionId == 1) {
                float endBaseR = removeEndMinimum ? 0.0F : 0.22F;
                float endBaseG = removeEndMinimum ? 0.0F : 0.28F;
                float endBaseB = removeEndMinimum ? 0.0F : 0.25F;
                r = endBaseR + blockLight * 0.75F;
                g = endBaseG + blockG * 0.75F;
                b = endBaseB + blockB * 0.75F;
            }

            r = clamp01(r);
            g = clamp01(g);
            b = clamp01(b);

            float gammaSetting = this.mc.gameSettings.gammaSetting;
            float gammaR = 1.0F - r;
            float gammaG = 1.0F - g;
            float gammaB = 1.0F - b;
            gammaR = 1.0F - gammaR * gammaR * gammaR * gammaR;
            gammaG = 1.0F - gammaG * gammaG * gammaG * gammaG;
            gammaB = 1.0F - gammaB * gammaB * gammaB * gammaB;

            r = r * (1.0F - gammaSetting) + gammaR * gammaSetting;
            g = g * (1.0F - gammaSetting) + gammaG * gammaSetting;
            b = b * (1.0F - gammaSetting) + gammaB * gammaSetting;

            if (world.isBloodMoonNight()) {
                float blockLightAttenuation = MathHelper.clamp_float(1.0F - blockLightRatio * 2.0F, 0.0F, 1.0F);
                r *= 1.0F + 1.0F * MathHelper.clamp_float(world.getMoonAscensionFactor(), 0.0F, 0.2F) * 5.0F * skyLightRatio * blockLightAttenuation;
                g *= 1.0F + 0.4F * MathHelper.clamp_float(world.getMoonAscensionFactor(), 0.0F, 0.2F) * 5.0F * skyLightRatio * blockLightAttenuation;
            } else if (world.isHarvestMoonNight()) {
                float blockLightAttenuation = MathHelper.clamp_float(1.0F - blockLightRatio * 2.0F, 0.0F, 1.0F);
                r *= 1.0F + 0.6F * MathHelper.clamp_float(world.getMoonAscensionFactor(), 0.0F, 0.2F) * 4.0F * skyLightRatio * blockLightAttenuation;
                g *= 1.0F + 0.4F * MathHelper.clamp_float(world.getMoonAscensionFactor(), 0.0F, 0.2F) * 4.0F * skyLightRatio * blockLightAttenuation;
                b *= 1.0F - 0.2F * MathHelper.clamp_float(world.getMoonAscensionFactor(), 0.0F, 0.2F) * 4.0F * skyLightRatio * blockLightAttenuation;
            } else if (world.isBlueMoonNight()) {
                float blockLightAttenuation = MathHelper.clamp_float(1.0F - blockLightRatio * 2.0F, 0.0F, 1.0F);
                b *= 1.0F + 0.5F * MathHelper.clamp_float(world.getMoonAscensionFactor(), 0.0F, 0.2F) * 2.5F * skyLightRatio * blockLightAttenuation;
                g *= 1.0F + 0.2F * MathHelper.clamp_float(world.getMoonAscensionFactor(), 0.0F, 0.2F) * 2.5F * skyLightRatio * blockLightAttenuation;
            }

            float baseR = clamp01(r);
            float baseG = clamp01(g);
            float baseB = clamp01(b);

            if (nightVisionActive) {
                float nightVisionPower = (Minecraft.night_vision_override || this.mc.thePlayer == null)
                        ? 1.0F
                        : this.getNightVisionBrightness(this.mc.thePlayer, partialTick);

                float nightVisionFloorR = baseR * 0.96F + 0.03F;
                float nightVisionFloorG = baseG * 0.96F + 0.03F;
                float nightVisionFloorB = baseB * 0.96F + 0.03F;

                float inverseMaxChannel = 1.0F / nightVisionFloorR;
                if (inverseMaxChannel > 1.0F / nightVisionFloorG) {
                    inverseMaxChannel = 1.0F / nightVisionFloorG;
                }
                if (inverseMaxChannel > 1.0F / nightVisionFloorB) {
                    inverseMaxChannel = 1.0F / nightVisionFloorB;
                }

                float nightVisionBoostedR = clamp01(nightVisionFloorR + nightVisionFloorR * (inverseMaxChannel - 1.0F));
                float nightVisionBoostedG = clamp01(nightVisionFloorG + nightVisionFloorG * (inverseMaxChannel - 1.0F));
                float nightVisionBoostedB = clamp01(nightVisionFloorB + nightVisionFloorB * (inverseMaxChannel - 1.0F));

                float nightVisionGammaR = 1.0F - nightVisionBoostedR;
                float nightVisionGammaG = 1.0F - nightVisionBoostedG;
                float nightVisionGammaB = 1.0F - nightVisionBoostedB;
                nightVisionGammaR = 1.0F - nightVisionGammaR * nightVisionGammaR * nightVisionGammaR * nightVisionGammaR;
                nightVisionGammaG = 1.0F - nightVisionGammaG * nightVisionGammaG * nightVisionGammaG * nightVisionGammaG;
                nightVisionGammaB = 1.0F - nightVisionGammaB * nightVisionGammaB * nightVisionGammaB * nightVisionGammaB;

                nightVisionBoostedR = clamp01(nightVisionBoostedR * (1.0F - gammaSetting) + nightVisionGammaR * gammaSetting);
                nightVisionBoostedG = clamp01(nightVisionBoostedG * (1.0F - gammaSetting) + nightVisionGammaG * gammaSetting);
                nightVisionBoostedB = clamp01(nightVisionBoostedB * (1.0F - gammaSetting) + nightVisionGammaB * gammaSetting);

                r = clamp01(baseR * (1.0F - nightVisionPower) + nightVisionBoostedR * nightVisionPower);
                g = clamp01(baseG * (1.0F - nightVisionPower) + nightVisionBoostedG * nightVisionPower);
                b = clamp01(baseB * (1.0F - nightVisionPower) + nightVisionBoostedB * nightVisionPower);
            } else {
                r = baseR;
                g = baseG;
                b = baseB;
            }

            int rByte = (int) (r * 255.0F);
            int gByte = (int) (g * 255.0F);
            int bByte = (int) (b * 255.0F);
            this.lightmapColors[cellIndex] = 255 << 24 | rByte << 16 | gByte << 8 | bByte;
        }

        this.lightmapTexture.updateDynamicTexture();
        this.lightmapUpdateNeeded = false;
        ci.cancel();
    }
}