package net.frealac.iamod.ai.brain;

import net.frealac.iamod.IAMOD;
import net.frealac.iamod.ai.memory.Memory;
import net.frealac.iamod.ai.memory.VillagerMemory;

import java.util.List;
import java.util.UUID;

/**
 * Memory Brain Module - Manages memories and recalls past interactions.
 * Like the hippocampus in humans.
 *
 * This brain:
 * - Stores interaction memories
 * - Recalls relevant memories when needed
 * - Recognizes patterns in past interactions
 * - Influences decisions based on past experiences
 */
public class MemoryBrain extends BrainModule {

    private final VillagerMemory memorySystem;
    private UUID currentPlayerUuid;

    // STANFORD GENERATIVE AGENTS: Reflection System
    private int memoriesSinceLastReflection = 0;
    private long lastReflectionTime = System.currentTimeMillis();
    private static final int REFLECTION_THRESHOLD = 5; // Reflect after 5 important memories
    private static final long REFLECTION_INTERVAL_MS = 8 * 60 * 60 * 1000; // 8 hours (2-3 times per day)

    public MemoryBrain(VillagerMemory memorySystem) {
        super("MemoryBrain");
        this.memorySystem = memorySystem != null ? memorySystem : new VillagerMemory();

        IAMOD.LOGGER.info("🧠 MemoryBrain initialized with {} memories",
            this.memorySystem.getMemoryCount());
    }

    @Override
    public void receiveSignal(BrainSignal signal) {
        switch (signal.getType()) {
            case PLAYER_INTERACTION:
                // Un joueur interagit - mémoriser son UUID
                Object uuid = signal.getData("playerUuid");
                if (uuid instanceof UUID) {
                    currentPlayerUuid = (UUID) uuid;
                    IAMOD.LOGGER.debug("🧠 MemoryBrain: Player interaction detected, UUID={}", currentPlayerUuid);
                }
                break;

            case IMPORTANT_EVENT:
                // Un événement important s'est produit - le mémoriser
                String description = (String) signal.getData("description");
                if (description != null && currentPlayerUuid != null) {
                    sendSignal(new BrainSignal(BrainSignal.SignalType.MEMORY_STORED, moduleName)
                        .withData("description", description));
                    IAMOD.LOGGER.debug("🧠 MemoryBrain: Important event stored: {}", description);

                    // STANFORD GENERATIVE AGENTS: Check if reflection should trigger
                    memoriesSinceLastReflection++;
                    checkAndTriggerReflection();
                }
                break;

            case DECISION_REQUEST:
                // Une décision est demandée - rappeler les souvenirs pertinents
                if (currentPlayerUuid != null) {
                    List<Memory> playerMemories = memorySystem.getMemoriesWithPlayer(currentPlayerUuid);
                    sendSignal(new BrainSignal(BrainSignal.SignalType.MEMORY_RECALLED, moduleName)
                        .withData("memories", playerMemories)
                        .withData("memoryCount", playerMemories.size()));
                }
                break;

            default:
                // Ignore other signals
                break;
        }
    }

    /**
     * STANFORD GENERATIVE AGENTS: Check if reflection should trigger
     * Triggers based on:
     * 1. Number of important memories since last reflection (REFLECTION_THRESHOLD)
     * 2. Time elapsed since last reflection (REFLECTION_INTERVAL_MS)
     */
    private void checkAndTriggerReflection() {
        long currentTime = System.currentTimeMillis();
        long timeSinceLastReflection = currentTime - lastReflectionTime;

        boolean shouldReflect = memoriesSinceLastReflection >= REFLECTION_THRESHOLD
                             || timeSinceLastReflection >= REFLECTION_INTERVAL_MS;

        if (shouldReflect) {
            IAMOD.LOGGER.info("🧠💭 MemoryBrain: Triggering reflection (memories={}, hours={})",
                memoriesSinceLastReflection, timeSinceLastReflection / (1000.0 * 60.0 * 60.0));
            generateReflections();
            memoriesSinceLastReflection = 0;
            lastReflectionTime = currentTime;
        }
    }

    /**
     * STANFORD GENERATIVE AGENTS: Generate reflections from recent memories
     *
     * Process:
     * 1. Retrieve recent high-importance memories (last 20)
     * 2. Send to LLM (gpt-4o-mini) for synthesis
     * 3. LLM generates high-level conclusions
     * 4. Store reflections as new memories
     *
     * Example:
     * Memories: "Player helped me 3 times", "Player gave me bread", "Player protected me"
     * Reflection: "This player is trustworthy and kind - they consistently help me"
     */
    private void generateReflections() {
        if (currentPlayerUuid == null) {
            IAMOD.LOGGER.warn("🧠 MemoryBrain: Cannot generate reflection without player context");
            return;
        }

        // Get recent important memories with player
        List<Memory> recentMemories = memorySystem.getMemoriesWithPlayer(currentPlayerUuid);
        if (recentMemories.isEmpty()) {
            IAMOD.LOGGER.debug("🧠 MemoryBrain: No memories to reflect on");
            return;
        }

        // Sort by importance * recency to get most significant recent memories
        recentMemories.sort((m1, m2) -> {
            double score1 = m1.getWeightedImportance();
            double score2 = m2.getWeightedImportance();
            return Double.compare(score2, score1); // Descending
        });

        // Take top 20 most significant memories
        List<Memory> topMemories = recentMemories.stream()
            .limit(20)
            .toList();

        IAMOD.LOGGER.info("🧠💭 MemoryBrain: Generating reflection from {} memories", topMemories.size());

        // Build reflection prompt
        StringBuilder reflectionPrompt = new StringBuilder();
        reflectionPrompt.append("=== MEMORY REFLECTION SYNTHESIS ===\n\n");
        reflectionPrompt.append("You are a villager reflecting on recent experiences with a player.\n");
        reflectionPrompt.append("Analyze these memories and generate 1-3 high-level conclusions.\n\n");

        reflectionPrompt.append("RECENT MEMORIES:\n");
        for (Memory memory : topMemories) {
            reflectionPrompt.append(String.format("- %s (%s, importance=%.2f)\n",
                memory.getDescription(),
                memory.getTimeDescription(),
                memory.getImportance()));
        }

        reflectionPrompt.append("\n=== REFLECTION TASK ===\n");
        reflectionPrompt.append("Synthesize these memories into broader patterns or conclusions.\n");
        reflectionPrompt.append("Examples:\n");
        reflectionPrompt.append("- 'This player is trustworthy - they helped me multiple times'\n");
        reflectionPrompt.append("- 'This player is dangerous - they attacked me twice'\n");
        reflectionPrompt.append("- 'This player is generous - they gave me valuable items'\n\n");

        reflectionPrompt.append("Generate 1-3 reflections as a JSON array:\n");
        reflectionPrompt.append("[\"reflection 1\", \"reflection 2\", \"reflection 3\"]\n");

        // Call LLM to generate reflections
        try {
            // Build OpenAI request payload
            com.google.gson.JsonObject payload = new com.google.gson.JsonObject();
            com.google.gson.JsonArray messages = new com.google.gson.JsonArray();

            // System message
            com.google.gson.JsonObject systemMsg = new com.google.gson.JsonObject();
            systemMsg.addProperty("role", "system");
            systemMsg.addProperty("content", "You are a memory reflection system. Synthesize memories into high-level conclusions.");
            messages.add(systemMsg);

            // User message with reflection prompt
            com.google.gson.JsonObject userMsg = new com.google.gson.JsonObject();
            userMsg.addProperty("role", "user");
            userMsg.addProperty("content", reflectionPrompt.toString());
            messages.add(userMsg);

            payload.add("messages", messages);
            payload.addProperty("model", BrainModelConfig.getModel("MemoryBrain"));
            payload.addProperty("temperature", BrainModelConfig.getTemperature("MemoryBrain"));
            payload.addProperty("max_tokens", BrainModelConfig.getMaxTokens("MemoryBrain"));

            // Call OpenAI
            net.frealac.iamod.ai.openai.OpenAiClient client = new net.frealac.iamod.ai.openai.OpenAiClient();
            String responseBody = client.sendChatRequest(payload);

            // Extract content from response
            String reflectionResponse = extractContentFromResponse(responseBody);

            // Parse reflections from response
            List<String> reflections = parseReflectionsFromResponse(reflectionResponse);

            // Store each reflection as a new high-importance memory
            for (String reflection : reflections) {
                Memory reflectionMemory = new Memory(
                    net.frealac.iamod.ai.memory.MemoryType.REFLECTION,
                    reflection,
                    currentPlayerUuid,
                    "Reflection"
                );
                reflectionMemory.setImportance(0.9); // High importance for reflections
                memorySystem.addMemory(reflectionMemory);

                IAMOD.LOGGER.info("🧠💭 Generated reflection: {}", reflection);
            }

        } catch (Exception e) {
            IAMOD.LOGGER.error("🧠 MemoryBrain: Failed to generate reflections", e);
        }
    }

    /**
     * Extract content from OpenAI response.
     */
    private String extractContentFromResponse(String json) throws java.io.IOException {
        try {
            com.google.gson.JsonObject obj = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
            com.google.gson.JsonArray choices = obj.getAsJsonArray("choices");
            if (choices == null || choices.isEmpty()) {
                throw new java.io.IOException("OpenAI response without 'choices'");
            }
            com.google.gson.JsonObject first = choices.get(0).getAsJsonObject();
            com.google.gson.JsonObject message = first.getAsJsonObject("message");
            if (message == null) {
                throw new java.io.IOException("OpenAI response without 'message'");
            }
            String content = message.get("content").getAsString();
            if (content == null || content.isBlank()) {
                throw new java.io.IOException("OpenAI response with blank content");
            }
            return content;
        } catch (Exception e) {
            throw new java.io.IOException("Failed to parse OpenAI response: " + e.getMessage(), e);
        }
    }

    /**
     * Parse reflections from LLM response.
     * Expects JSON array format: ["reflection 1", "reflection 2"]
     */
    private List<String> parseReflectionsFromResponse(String response) {
        try {
            // Simple JSON array parsing
            response = response.trim();
            if (response.startsWith("[") && response.endsWith("]")) {
                response = response.substring(1, response.length() - 1);
                String[] parts = response.split("\",\\s*\"");
                List<String> reflections = new java.util.ArrayList<>();
                for (String part : parts) {
                    String cleaned = part.replace("\"", "").trim();
                    if (!cleaned.isEmpty()) {
                        reflections.add(cleaned);
                    }
                }
                return reflections;
            }
        } catch (Exception e) {
            IAMOD.LOGGER.warn("🧠 MemoryBrain: Failed to parse reflections, using fallback", e);
        }

        // Fallback: treat entire response as single reflection
        return List.of(response.trim());
    }

    /**
     * Get memories with the current player formatted for AI prompt.
     */
    public String getMemoriesForPrompt(UUID playerUuid) {
        if (playerUuid == null) {
            return "Aucun souvenir particulier avec ce joueur.";
        }

        currentPlayerUuid = playerUuid;
        return memorySystem.formatMemoriesForPrompt(playerUuid);
    }

    /**
     * Get all memories with a specific player.
     */
    public List<Memory> getMemoriesWithPlayer(UUID playerUuid) {
        return memorySystem.getMemoriesWithPlayer(playerUuid);
    }

    /**
     * Check if villager remembers being hit by this player.
     */
    public boolean remembersBeingHit(UUID playerUuid) {
        if (playerUuid == null) return false;

        List<Memory> memories = memorySystem.getMemoriesWithPlayer(playerUuid);
        for (Memory memory : memories) {
            if (memory.getType() == net.frealac.iamod.ai.memory.MemoryType.WAS_HIT) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if villager remembers receiving a gift from this player.
     */
    public boolean remembersGiftFrom(UUID playerUuid) {
        if (playerUuid == null) return false;

        List<Memory> memories = memorySystem.getMemoriesWithPlayer(playerUuid);
        for (Memory memory : memories) {
            if (memory.getType() == net.frealac.iamod.ai.memory.MemoryType.GIFT_RECEIVED) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get sentiment towards player based on memories.
     */
    public double getSentimentTowardsPlayer(UUID playerUuid) {
        if (playerUuid == null) return 0.0;
        return memorySystem.getSentimentTowardsPlayer(playerUuid);
    }

    /**
     * Get total memory count.
     */
    public int getMemoryCount() {
        return memorySystem.getMemoryCount();
    }

    @Override
    public String getStateDescription() {
        return String.format(
            "Total memories: %d, Current player memories: %d, Current player sentiment: %.2f",
            memorySystem.getMemoryCount(),
            currentPlayerUuid != null ? memorySystem.getMemoriesWithPlayer(currentPlayerUuid).size() : 0,
            currentPlayerUuid != null ? memorySystem.getSentimentTowardsPlayer(currentPlayerUuid) : 0.0
        );
    }

    /**
     * Get memory summary for AI prompt.
     */
    public String getMemorySummaryForPrompt(UUID playerUuid) {
        if (playerUuid == null) {
            return "Je ne connais pas ce joueur.";
        }

        List<Memory> memories = memorySystem.getMemoriesWithPlayer(playerUuid);
        double sentiment = memorySystem.getSentimentTowardsPlayer(playerUuid);

        StringBuilder summary = new StringBuilder();

        if (memories.isEmpty()) {
            summary.append("C'est notre première rencontre. ");
        } else {
            summary.append(String.format("J'ai %d souvenirs avec ce joueur. ", memories.size()));

            // Sentiment description
            if (sentiment > 0.5) {
                summary.append("Je ressens de l'affection pour cette personne. ");
            } else if (sentiment > 0.2) {
                summary.append("J'ai une impression plutôt positive de cette personne. ");
            } else if (sentiment > -0.2) {
                summary.append("Je suis neutre envers cette personne. ");
            } else if (sentiment > -0.5) {
                summary.append("J'ai une impression plutôt négative de cette personne. ");
            } else {
                summary.append("Je n'aime pas cette personne et je me méfie d'elle. ");
            }

            // Key memories
            if (remembersBeingHit(playerUuid)) {
                summary.append("Je me souviens qu'elle m'a frappé. ");
            }
            if (remembersGiftFrom(playerUuid)) {
                summary.append("Je me souviens qu'elle m'a donné des cadeaux. ");
            }
        }

        return summary.toString();
    }

    // Getters
    public VillagerMemory getMemorySystem() { return memorySystem; }
}
