package net.customcrosshairstudio.render;

import net.customcrosshairstudio.CrosshairStudioClient;
import net.customcrosshairstudio.config.CrosshairStudioConfig;
import net.customcrosshairstudio.config.CrosshairProfile;
import net.customcrosshairstudio.render.styles.ClassicCrosshairStyle;
import net.customcrosshairstudio.render.styles.DotCrosshairStyle;
import net.customcrosshairstudio.render.styles.DrawnCrosshairStyle;
import net.customcrosshairstudio.render.styles.InvertedBlendCrosshairStyle;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.GameMode;

import java.util.EnumMap;
import java.util.Map;

/**
 * Core Crosshair Renderer engine.
 * Dispatches draw calls to the selected geometry style.
 */
public class CrosshairRenderer {
    public static final CrosshairRenderer INSTANCE = new CrosshairRenderer();

    private final RenderContext renderContext = new RenderContext();
    private final Map<CrosshairStudioConfig.StyleType, CrosshairStyle> styleMap = new EnumMap<>(CrosshairStudioConfig.StyleType.class);

    private CrosshairRenderer() {
        styleMap.put(CrosshairStudioConfig.StyleType.CLASSIC, new ClassicCrosshairStyle());
        styleMap.put(CrosshairStudioConfig.StyleType.DOT, new DotCrosshairStyle());
        styleMap.put(CrosshairStudioConfig.StyleType.INVERTED, new InvertedBlendCrosshairStyle());
        styleMap.put(CrosshairStudioConfig.StyleType.DRAWN, new DrawnCrosshairStyle());
    }

    /**
     * Determines whether the custom crosshair should render under current vanilla-parity HUD rules.
     * Note: ChatScreen is explicitly exempted so the crosshair remains visible while chatting (vanilla parity).
     */
    public boolean shouldRender(MinecraftClient client) {
        CrosshairStudioConfig config = CrosshairStudioClient.getConfig();
        if (config == null || !config.enabled) {
            return false;
        }

        if (client == null || client.player == null || client.world == null) {
            return false;
        }

        // F1 mode (HUD completely hidden)
        if (client.options.hudHidden) {
            return false;
        }

        // Third-person perspective check (F5) - Defaults to false for vanilla parity
        if (!config.showInThirdPerson && !client.options.getPerspective().isFirstPerson()) {
            return false;
        }

        // GUI screen check: Allow ChatScreen, hide on Inventory, Pause, Death, Chests, etc.
        if (client.currentScreen != null && !(client.currentScreen instanceof ChatScreen)) {
            return false;
        }

        // Spyglass aiming overlay active
        if (client.player.isUsingSpyglass()) {
            return false;
        }

        // Spectator mode: Crosshair only appears when targeting an interactable entity or block
        if (client.interactionManager != null && client.interactionManager.getCurrentGameMode() == GameMode.SPECTATOR) {
            if (client.crosshairTarget == null || client.crosshairTarget.getType() == HitResult.Type.MISS) {
                return false;
            }
        }

        return true;
    }

    /**
     * Main render entrypoint invoked by InGameHudMixin (replaces only the crosshair sprite draw call).
     * Vanilla's native attack indicator continues executing immediately afterwards untouched.
     */
    public void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!shouldRender(client)) {
            return;
        }

        CrosshairStudioConfig config = CrosshairStudioClient.getConfig();
        float tickProgress = tickCounter.getTickProgress(false);

        int screenWidth = context.getScaledWindowWidth();
        int screenHeight = context.getScaledWindowHeight();

        // Update single shared context (zero allocation)
        CrosshairProfile profile = config.activeProfile(isEnemyTargeted(client));
        renderContext.update(screenWidth, screenHeight, tickProgress, profile.color, false);

        // Render selected geometry style
        drawProfile(context, profile);
    }

    private void drawProfile(DrawContext context, CrosshairProfile profile) {
        CrosshairStyle style = styleMap.get(profile.style);
        if (style == null) {
            style = styleMap.get(CrosshairStudioConfig.StyleType.CLASSIC);
        }
        style.render(context, renderContext, profile);
    }

    /**
     * True when the vanilla crosshair target is a living, non-spectator player or a hostile mob.
     * Uses the game's own crosshairTarget (same one that drives the vanilla attack indicator).
     */
    public static boolean isEnemyTargeted(MinecraftClient client) {
        if (client == null || !(client.crosshairTarget instanceof EntityHitResult hit)) return false;
        Entity target = hit.getEntity();
        if (target == null || target == client.player || !target.isAlive()) return false;
        if (target instanceof PlayerEntity player) return !player.isSpectator();
        return target instanceof Monster;
    }

    /**
     * Helper for live preview rendering inside the configuration GUI.
     */
    public void renderPreview(DrawContext context, CrosshairProfile config, int previewX, int previewY, int width, int height) {
        renderContext.update(width, height, 0.0f, config.color, true);
        renderContext.centerX = previewX + width / 2.0f;
        renderContext.centerY = previewY + height / 2.0f;

        drawProfile(context, config);
    }
}
