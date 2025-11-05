package net.frealac.iamod.ai.brain;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for AI models used by each brain module.
 * Allows optimization of speed and cost by assigning appropriate models.
 *
 * GAME OPTIMIZATION STRATEGY:
 * - All brains use gpt-4o-mini by default (fastest & cheapest)
 * - Most processing is LOCAL (no AI calls) for speed
 * - AI calls only when necessary for complex decisions
 *
 * PERFORMANCE METRICS (gpt-4o-mini):
 * - Speed: ~500ms response time
 * - Cost: $0.150 per 1M input tokens, $0.600 per 1M output tokens
 * - Perfect for real-time game interactions
 */
public class BrainModelConfig {

    // Default model for all brains (optimized for games)
    public static final String DEFAULT_MODEL = "gpt-4o-mini";

    // Model assignments per brain type
    private static final Map<String, BrainConfig> BRAIN_CONFIGS = new HashMap<>();

    static {
        // EmotionalBrain: Fast emotion analysis
        BRAIN_CONFIGS.put("EmotionalBrain", new BrainConfig(
            "gpt-4o-mini",      // Model
            0.7,                // Temperature (un peu de variation)
            100,                // Max tokens
            true                // Enable AI assistance
        ));

        // MemoryBrain: Fast memory search
        BRAIN_CONFIGS.put("MemoryBrain", new BrainConfig(
            "gpt-4o-mini",      // Model
            0.3,                // Temperature (très précis)
            150,                // Max tokens
            false               // Mostly local processing (faster)
        ));

        // SocialBrain: Fast relationship analysis
        BRAIN_CONFIGS.put("SocialBrain", new BrainConfig(
            "gpt-4o-mini",      // Model
            0.5,                // Temperature (équilibré)
            100,                // Max tokens
            false               // Mostly local processing (faster)
        ));

        // GeneralBrain: Comprehensive synthesis
        BRAIN_CONFIGS.put("GeneralBrain", new BrainConfig(
            "gpt-4o-mini",      // Model (peut être changé en gpt-4o pour plus de qualité)
            0.7,                // Temperature (créatif)
            300,                // Max tokens
            true                // Always uses AI
        ));
    }

    /**
     * Brain configuration data.
     */
    public static class BrainConfig {
        public final String model;
        public final double temperature;
        public final int maxTokens;
        public final boolean aiEnabled;

        public BrainConfig(String model, double temperature, int maxTokens, boolean aiEnabled) {
            this.model = model;
            this.temperature = temperature;
            this.maxTokens = maxTokens;
            this.aiEnabled = aiEnabled;
        }
    }

    /**
     * Get configuration for a brain module.
     */
    public static BrainConfig getConfig(String brainModuleName) {
        return BRAIN_CONFIGS.getOrDefault(brainModuleName,
            new BrainConfig(DEFAULT_MODEL, 0.5, 200, false));
    }

    /**
     * Get model for a brain module.
     */
    public static String getModel(String brainModuleName) {
        return getConfig(brainModuleName).model;
    }

    /**
     * Get temperature for a brain module.
     */
    public static double getTemperature(String brainModuleName) {
        return getConfig(brainModuleName).temperature;
    }

    /**
     * Get max tokens for a brain module.
     */
    public static int getMaxTokens(String brainModuleName) {
        return getConfig(brainModuleName).maxTokens;
    }

    /**
     * Check if AI is enabled for a brain module.
     */
    public static boolean isAiEnabled(String brainModuleName) {
        return getConfig(brainModuleName).aiEnabled;
    }

    /**
     * Override model for a brain type (for testing or customization).
     */
    public static void setModel(String brainModuleName, String model) {
        BrainConfig oldConfig = getConfig(brainModuleName);
        BRAIN_CONFIGS.put(brainModuleName, new BrainConfig(
            model,
            oldConfig.temperature,
            oldConfig.maxTokens,
            oldConfig.aiEnabled
        ));
    }

    /**
     * Enable/disable AI for a brain type.
     */
    public static void setAiEnabled(String brainModuleName, boolean enabled) {
        BrainConfig oldConfig = getConfig(brainModuleName);
        BRAIN_CONFIGS.put(brainModuleName, new BrainConfig(
            oldConfig.model,
            oldConfig.temperature,
            oldConfig.maxTokens,
            enabled
        ));
    }

    /**
     * Get all brain configs as a formatted string (for debugging).
     */
    public static String getAllConfigs() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== BRAIN AI MODEL CONFIGURATION ===\n\n");

        for (Map.Entry<String, BrainConfig> entry : BRAIN_CONFIGS.entrySet()) {
            String name = entry.getKey();
            BrainConfig config = entry.getValue();

            sb.append(String.format("%s:\n", name));
            sb.append(String.format("  Model: %s\n", config.model));
            sb.append(String.format("  Temperature: %.1f\n", config.temperature));
            sb.append(String.format("  Max Tokens: %d\n", config.maxTokens));
            sb.append(String.format("  AI Enabled: %s\n", config.aiEnabled ? "YES" : "NO (local only)"));
            sb.append("\n");
        }

        sb.append("OPTIMIZATION:\n");
        sb.append("- EmotionalBrain: AI enabled for complex emotion analysis\n");
        sb.append("- MemoryBrain: LOCAL only (faster, no API cost)\n");
        sb.append("- SocialBrain: LOCAL only (faster, no API cost)\n");
        sb.append("- GeneralBrain: AI enabled for final decision synthesis\n");

        return sb.toString();
    }

    /**
     * Alternative model options for different scenarios.
     */
    public enum ModelOption {
        FASTEST("gpt-4o-mini", "Fastest & cheapest - best for games"),
        BALANCED("gpt-4o", "Balanced speed & quality"),
        QUALITY("gpt-4-turbo", "Highest quality, slower"),
        LEGACY("gpt-3.5-turbo", "Legacy model, very fast");

        public final String modelName;
        public final String description;

        ModelOption(String modelName, String description) {
            this.modelName = modelName;
            this.description = description;
        }
    }

    /**
     * Apply a model preset to all brains.
     */
    public static void applyPreset(ModelOption preset) {
        for (String brainName : BRAIN_CONFIGS.keySet()) {
            setModel(brainName, preset.modelName);
        }
    }
}
