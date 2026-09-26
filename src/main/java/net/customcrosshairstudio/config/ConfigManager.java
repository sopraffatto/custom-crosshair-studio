package net.customcrosshairstudio.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Robust JSON configuration manager with corruption recovery, safe defaults, and schema migration.
 */
public class ConfigManager {
    public static final Logger LOGGER = LoggerFactory.getLogger("Custom Crosshair Studio");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE_NAME = "customcrosshairstudio.json";
    /** Config file used before the rename to Custom Crosshair Studio; imported once if present. */
    private static final String LEGACY_CONFIG_FILE_NAME = "crosssight.json";

    private static CrosshairStudioConfig activeConfig = new CrosshairStudioConfig();

    public static CrosshairStudioConfig getConfig() {
        if (activeConfig == null) {
            activeConfig = new CrosshairStudioConfig();
        }
        return activeConfig;
    }

    private static File getConfigFile() {
        return FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE_NAME).toFile();
    }

    /**
     * Loads config from disk with graceful enum migration and corruption recovery.
     */
    public static void load() {
        File file = getConfigFile();
        importLegacyConfig(file);
        if (!file.exists() || file.length() == 0) {
            LOGGER.info("[Custom Crosshair Studio] Config file not found or empty. Creating default config at {}", file.getAbsolutePath());
            activeConfig = new CrosshairStudioConfig();
            save();
            return;
        }

        try (FileReader reader = new FileReader(file)) {
            CrosshairStudioConfig loaded = GSON.fromJson(reader, CrosshairStudioConfig.class);
            if (loaded == null) {
                LOGGER.warn("[Custom Crosshair Studio] Config parsed as null. Reverting to safe defaults.");
                backupCorruptFile(file);
                activeConfig = new CrosshairStudioConfig();
                save();
                return;
            }

            if (loaded.configVersion != CrosshairStudioConfig.CURRENT_CONFIG_VERSION) {
                LOGGER.info("[Custom Crosshair Studio] Migrating config from version {} to {}", loaded.configVersion, CrosshairStudioConfig.CURRENT_CONFIG_VERSION);
            }
            loaded.normalize();
            activeConfig = loaded;
            LOGGER.info("[Custom Crosshair Studio] Config successfully loaded.");
        } catch (JsonSyntaxException | IOException e) {
            LOGGER.error("[Custom Crosshair Studio] Error reading config file. Performing fail-safe recovery: {}", e.getMessage());
            backupCorruptFile(file);
            activeConfig = new CrosshairStudioConfig();
            save();
        }
    }

    /**
     * Saves active configuration to disk.
     */
    public static void save() {
        File file = getConfigFile();
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            try (FileWriter writer = new FileWriter(file)) {
                GSON.toJson(activeConfig, writer);
            }
        } catch (IOException e) {
            LOGGER.error("[Custom Crosshair Studio] Failed to write config to file: {}", e.getMessage());
        }
    }

    /** Copies the pre-rename config into place when the new file does not exist yet; the old file is kept. */
    private static void importLegacyConfig(File file) {
        if (file.exists() && file.length() > 0) return;
        File legacy = FabricLoader.getInstance().getConfigDir().resolve(LEGACY_CONFIG_FILE_NAME).toFile();
        if (!legacy.exists() || legacy.length() == 0) return;
        try {
            Files.copy(legacy.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            LOGGER.info("[Custom Crosshair Studio] Imported settings from {}", legacy.getName());
        } catch (IOException e) {
            LOGGER.warn("[Custom Crosshair Studio] Could not import {}: {}", legacy.getName(), e.getMessage());
        }
    }

    private static void backupCorruptFile(File corruptFile) {
        try {
            File backupFile = new File(corruptFile.getParentFile(), CONFIG_FILE_NAME + ".corrupted." + System.currentTimeMillis());
            Files.copy(corruptFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            LOGGER.warn("[Custom Crosshair Studio] Backed up corrupted config to {}", backupFile.getAbsolutePath());
        } catch (Exception ex) {
            LOGGER.error("[Custom Crosshair Studio] Could not create backup of corrupted config: {}", ex.getMessage());
        }
    }
}
