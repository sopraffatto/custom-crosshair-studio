package net.customcrosshairstudio.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.customcrosshairstudio.CrosshairStudioClient;
import net.customcrosshairstudio.config.CrosshairStudioConfig;
import net.customcrosshairstudio.render.CrosshairRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Intercepts crosshair rendering in InGameHud,
 * preserving vanilla attack indicator rendering while supporting first-person replacement
 * and optional third-person (F5) visibility.
 */
@Mixin(InGameHud.class)
public class InGameHudMixin {

    /**
     * In third-person perspective (F5), vanilla renderCrosshair returns early at HEAD.
     * When showInThirdPerson is enabled, render the custom crosshair before vanilla returns.
     */
    @Inject(method = "renderCrosshair", at = @At("HEAD"))
    private void ccs$renderThirdPersonCrosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        CrosshairStudioConfig config = CrosshairStudioClient.getConfig();
        if (config != null && config.enabled && config.showInThirdPerson && client != null && client.options != null
                && !client.options.getPerspective().isFirstPerson()) {
            if (CrosshairRenderer.INSTANCE.shouldRender(client)) {
                CrosshairRenderer.INSTANCE.render(context, tickCounter);
            }
        }
    }

    /**
     * In first-person perspective, intercepts ONLY the crosshair sprite drawing in InGameHud,
     * allowing vanilla's native attack indicator rendering to run completely untouched.
     */
    @WrapOperation(
            method = "renderCrosshair",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIII)V",
                    ordinal = 0
            )
    )
    private void ccs$renderCustomCrosshairOnly(DrawContext context, RenderPipeline pipeline, Identifier texture,
                                                      int x, int y, int width, int height,
                                                      Operation<Void> original,
                                                      @Local(argsOnly = true) RenderTickCounter tickCounter) {
        if (CrosshairStudioClient.getConfig() != null && CrosshairStudioClient.getConfig().enabled
                && CrosshairRenderer.INSTANCE.shouldRender(MinecraftClient.getInstance())) {
            CrosshairRenderer.INSTANCE.render(context, tickCounter);
        } else {
            original.call(context, pipeline, texture, x, y, width, height);
        }
    }
}

