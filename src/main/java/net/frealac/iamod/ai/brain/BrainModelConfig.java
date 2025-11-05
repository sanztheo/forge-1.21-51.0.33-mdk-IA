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

    // Default models per function
    public static final String ANALYSIS_MODEL = "gpt-3.5-turbo";  // Ultra rapide pour analyses
    public static final String CONVERSATION_MODEL = "gpt-4o-mini"; // Qualité pour conversations

    // Model assignments per brain type
    private static final Map<String, BrainConfig> BRAIN_CONFIGS = new HashMap<>();

    static {
        // EmotionalBrain: gpt-3.5-turbo pour analyse rapide des émotions
        BRAIN_CONFIGS.put("EmotionalBrain", new BrainConfig(
            "gpt-3.5-turbo",    // Ultra rapide
            0.3,                // Temperature (précis)
            2000,                // Max tokens (augmenté pour analyse détaillée)
            true                // Enable AI assistance
        ));

        // MemoryBrain: Local uniquement (pas d'AI = ultra rapide)
        BRAIN_CONFIGS.put("MemoryBrain", new BrainConfig(
            "gpt-3.5-turbo",    // Backup si besoin
            0.2,                // Temperature (très précis)
            2000,                // Max tokens
            false               // LOCAL ONLY - pas d'appel IA
        ));

        // SocialBrain: Local uniquement (pas d'AI = ultra rapide)
        BRAIN_CONFIGS.put("SocialBrain", new BrainConfig(
            "gpt-3.5-turbo",    // Backup si besoin
            0.3,                // Temperature (précis)
            2000,                // Max tokens
            false               // LOCAL ONLY - pas d'appel IA
        ));

        // GeneralBrain: gpt-4o-mini pour conversation de qualité
        BRAIN_CONFIGS.put("GeneralBrain", new BrainConfig(
            "gpt-4o-mini",      // Qualité conversation
            0.7,                // Temperature (créatif)
            3000,               // Max tokens (3000 pour PLEIN de contexte)
            true                // Always uses AI
        ));

        // MessageAnalyzer: gpt-3.5-turbo pour analyse rapide
        BRAIN_CONFIGS.put("MessageAnalyzer", new BrainConfig(
            "gpt-3.5-turbo",    // Ultra rapide
            0.3,                // Temperature (précis)
            2000,                // Max tokens (augmenté pour analyse détaillée)
            true                // AI enabled
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
            new BrainConfig(ANALYSIS_MODEL, 0.5, 200, false));
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
        sb.append("- MessageAnalyzer: gpt-3.5-turbo (ultra rapide, JSON strict)\n");
        sb.append("- EmotionalBrain: gpt-3.5-turbo (analyse rapide émotions)\n");
        sb.append("- MemoryBrain: LOCAL only (pas d'appel IA = ultra rapide)\n");
        sb.append("- SocialBrain: LOCAL only (pas d'appel IA = ultra rapide)\n");
        sb.append("- GeneralBrain: gpt-4o-mini (qualité conversation)\n");

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
