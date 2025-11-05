package net.frealac.iamod.ai.memory;

/**
 * Types of memories a villager can have about interactions.
 * Each memory type has an emotional impact on the villager's perception.
 */
public enum MemoryType {
    // POSITIVE MEMORIES
    PLAYER_NAME_LEARNED("Nom du joueur appris", 0.1),
    HELP_RECEIVED("A reçu de l'aide", 0.3),
    GIFT_RECEIVED("A reçu un cadeau", 0.4),
    LIFE_SAVED("Vie sauvée par le joueur", 0.8),
    PLEASANT_CONVERSATION("Conversation agréable", 0.2),
    COMPLIMENT_RECEIVED("A reçu un compliment", 0.2),

    // NEGATIVE MEMORIES
    WAS_HIT("A été frappé", -0.5),
    WAS_INSULTED("A été insulté", -0.3),
    WAS_BETRAYED("A été trahi", -0.7),
    WAS_THREATENED("A été menacé", -0.4),
    WITNESSED_VIOLENCE("A vu de la violence", -0.2),
    PROMISE_BROKEN("Promesse non tenue", -0.3),

    // NEUTRAL MEMORIES
    GENERAL_INTERACTION("Interaction générale", 0.0),
    INFORMATION_SHARED("Information partagée", 0.0),
    REQUEST_MADE("Demande effectuée", 0.0),
    QUEST_GIVEN("Quête donnée", 0.1),

    // STANFORD GENERATIVE AGENTS: Reflection memories
    REFLECTION("Réflexion synthétisée", 0.0);

    private final String description;
    private final double baseEmotionalImpact;

    MemoryType(String description, double baseEmotionalImpact) {
        this.description = description;
        this.baseEmotionalImpact = baseEmotionalImpact;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Base emotional impact of this memory type.
     * Range: -1.0 (very negative) to +1.0 (very positive)
     */
    public double getBaseEmotionalImpact() {
        return baseEmotionalImpact;
    }

    /**
     * Check if this memory is positive.
     */
    public boolean isPositive() {
        return baseEmotionalImpact > 0;
    }

    /**
     * Check if this memory is negative.
     */
    public boolean isNegative() {
        return baseEmotionalImpact < 0;
    }
}
