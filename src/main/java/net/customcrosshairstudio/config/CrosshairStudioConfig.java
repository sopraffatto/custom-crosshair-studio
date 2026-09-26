package net.customcrosshairstudio.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Authoritative configuration model for Custom Crosshair Studio.
 * The config itself is the normal crosshair profile (flat JSON keys, unchanged from earlier
 * versions); {@link #enemy} is a fully independent profile used while targeting an enemy.
 */
public class CrosshairStudioConfig extends CrosshairProfile {
    public static final int CURRENT_CONFIG_VERSION = 5;

    public int configVersion = CURRENT_CONFIG_VERSION;
    public boolean enabled = true;

    // Third Person (F5) visibility toggle (Default: false to maintain vanilla parity)
    public boolean showInThirdPerson = false;

    // Custom Enemy Crosshair: separate profile shown while the crosshair targets an enemy
    public boolean enemyEnabled = false;
    public CrosshairProfile enemy = CrosshairProfile.enemyDefault();

    // User-Saved Custom Presets (normal crosshair only)
    public List<CustomPreset> savedPresets = new ArrayList<>();

    public enum StyleType {
        CLASSIC("Classic"),
        DOT("Dot"),
        INVERTED("Inverted"),
        DRAWN("Drawn");

        private final String displayName;

        StyleType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /** Profile to render: the enemy profile only when enabled and an enemy is targeted. */
    public CrosshairProfile activeProfile(boolean enemyTargeted) {
        return enemyEnabled && enemyTargeted && enemy != null ? enemy : this;
    }

    public static class CustomPreset {
        public String name = "Preset";
        public StyleType style = StyleType.CLASSIC;
        public float thickness = 1.0f;
        public float length = 1.0f;
        public float gap = 0.0f;
        public int color = 0xFFFFFFFF;
        public boolean outline = false;
        public float outlineThickness = 1.0f;
        public int outlineColor = 0xFF000000;
        public boolean dot = false;
        public float dotSize = 2.0f;
        public List<String> drawn = DrawnPattern.defaultRows();
        public boolean drawnInvert = false;
        public boolean showInThirdPerson = false;

        public CustomPreset() {}

        public CustomPreset(String name, CrosshairStudioConfig config) {
            this.name = name;
            this.style = config.style != null ? config.style : StyleType.CLASSIC;
            this.thickness = config.thickness;
            this.length = config.length;
            this.gap = config.gap;
            this.color = config.color;
            this.outline = config.outline;
            this.outlineThickness = config.outlineThickness;
            this.outlineColor = config.outlineColor;
            this.dot = config.dot;
            this.dotSize = config.dotSize;
            this.drawn = new ArrayList<>(DrawnPattern.normalize(config.drawn));
            this.drawnInvert = config.drawnInvert;
            this.showInThirdPerson = config.showInThirdPerson;
        }

        public CustomPreset(CustomPreset other) {
            if (other == null) return;
            this.name = other.name;
            this.style = other.style;
            this.thickness = other.thickness;
            this.length = other.length;
            this.gap = other.gap;
            this.color = other.color;
            this.outline = other.outline;
            this.outlineThickness = other.outlineThickness;
            this.outlineColor = other.outlineColor;
            this.dot = other.dot;
            this.dotSize = other.dotSize;
            this.drawn = new ArrayList<>(DrawnPattern.normalize(other.drawn));
            this.drawnInvert = other.drawnInvert;
            this.showInThirdPerson = other.showInThirdPerson;
        }

        /** Snapshot of any profile's look (normal or enemy). */
        public static CustomPreset of(String name, CrosshairProfile look, boolean showInThirdPerson) {
            CustomPreset preset = new CustomPreset();
            preset.name = name;
            preset.style = look.style != null ? look.style : StyleType.CLASSIC;
            preset.thickness = look.thickness;
            preset.length = look.length;
            preset.gap = look.gap;
            preset.color = look.color;
            preset.outline = look.outline;
            preset.outlineThickness = look.outlineThickness;
            preset.outlineColor = look.outlineColor;
            preset.dot = look.dot;
            preset.dotSize = look.dotSize;
            preset.drawn = new ArrayList<>(DrawnPattern.normalize(look.drawn));
            preset.drawnInvert = look.drawnInvert;
            preset.showInThirdPerson = showInThirdPerson;
            return preset;
        }

        /** Applies only the look to {@code target}; global settings are left alone. */
        public void applyLookTo(CrosshairProfile target) {
            target.style = this.style != null ? this.style : StyleType.CLASSIC;
            target.thickness = this.thickness;
            target.length = this.length;
            target.gap = this.gap;
            target.color = this.color;
            target.outline = this.outline;
            target.outlineThickness = this.outlineThickness;
            target.outlineColor = this.outlineColor;
            target.dot = this.dot;
            target.dotSize = this.dotSize;
            target.drawn = new ArrayList<>(DrawnPattern.normalize(this.drawn));
            target.drawnInvert = this.drawnInvert;
            target.normalizeProfile();
        }

        public void applyTo(CrosshairStudioConfig config) {
            config.style = this.style != null ? this.style : StyleType.CLASSIC;
            config.thickness = this.thickness;
            config.length = this.length;
            config.gap = this.gap;
            config.color = this.color;
            config.outline = this.outline;
            config.outlineThickness = this.outlineThickness;
            config.outlineColor = this.outlineColor;
            config.dot = this.dot;
            config.dotSize = this.dotSize;
            config.drawn = new ArrayList<>(DrawnPattern.normalize(this.drawn));
            config.drawnInvert = this.drawnInvert;
            config.showInThirdPerson = this.showInThirdPerson;
            config.normalize();
        }
    }

    /** Normalizes untrusted JSON and presets to values the UI and renderer support. */
    public void normalize() {
        configVersion = CURRENT_CONFIG_VERSION;
        normalizeProfile();
        if (enemy == null) enemy = CrosshairProfile.enemyDefault();
        enemy.normalizeProfile();
        if (savedPresets == null) savedPresets = new ArrayList<>();
        List<CustomPreset> normalized = new ArrayList<>(savedPresets.size());
        for (CustomPreset preset : savedPresets) {
            if (preset == null) continue;
            preset.style = preset.style != null ? preset.style : StyleType.CLASSIC;
            preset.thickness = Math.round(clampFinite(preset.thickness, 1.0f, 4.0f, 1.0f));
            preset.length = Math.round(clampFinite(preset.length, 1.0f, 6.0f, 1.0f));
            preset.gap = Math.round(clampFinite(preset.gap, 0.0f, 4.0f, 0.0f));
            preset.dotSize = Math.round(clampFinite(preset.dotSize, 1.0f, 4.0f, 2.0f));
            preset.outlineThickness = Math.round(clampFinite(preset.outlineThickness, 1.0f, 4.0f, 1.0f));
            preset.outlineColor |= 0xFF000000;
            preset.drawn = DrawnPattern.normalize(preset.drawn);
            if (preset.name == null || preset.name.isBlank()) preset.name = "Preset";
            normalized.add(preset);
        }
        savedPresets = normalized;
    }

    /** Resets the normal crosshair and global toggles; the enemy profile is left untouched. */
    public void resetToDefaults() {
        this.configVersion = CURRENT_CONFIG_VERSION;
        this.enabled = true;
        resetProfile();
        this.showInThirdPerson = false;
    }

    public void copyFrom(CrosshairStudioConfig other) {
        if (other == null) return;
        this.configVersion = other.configVersion;
        this.enabled = other.enabled;
        copyProfileFrom(other);
        this.showInThirdPerson = other.showInThirdPerson;
        this.enemyEnabled = other.enemyEnabled;
        this.enemy = CrosshairProfile.enemyDefault();
        this.enemy.copyProfileFrom(other.enemy);
        this.savedPresets = new ArrayList<>();
        if (other.savedPresets != null) {
            for (CustomPreset preset : other.savedPresets) {
                if (preset != null) this.savedPresets.add(new CustomPreset(preset));
            }
        }
        normalize();
    }
}
