package net.frealac.iamod.ai.brain;

import net.frealac.iamod.IAMOD;
import net.frealac.iamod.common.story.VillagerStory;

/**
 * Emotional Brain Module - Manages emotions, mood, and stress.
 * Like the limbic system in humans (amygdala, hippocampus).
 *
 * This brain:
 * - Tracks current mood and stress
 * - Reacts to events emotionally
 * - Influences decisions through emotional state
 * - Sends emotion signals to other brains
 */
public class EmotionalBrain extends BrainModule {

    private VillagerStory.Psychology psychology;
    private double currentMood;      // -1.0 (très triste) à +1.0 (très joyeux)
    private double currentStress;    // 0.0 (calme) à 1.0 (très stressé)
    private double resilience;       // Capacité à gérer le stress

    public EmotionalBrain(VillagerStory.Psychology psychology) {
        super("EmotionalBrain");
        this.psychology = psychology;

        if (psychology != null) {
            this.currentMood = psychology.moodBaseline;
            this.currentStress = psychology.stress;
            this.resilience = psychology.resilience;
        } else {
            this.currentMood = 0.0;
            this.currentStress = 0.3;
            this.resilience = 0.5;
        }

        IAMOD.LOGGER.info("🧠 EmotionalBrain initialized: mood={}, stress={}, resilience={}",
            currentMood, currentStress, resilience);
    }

    @Override
    public void receiveSignal(BrainSignal signal) {
        switch (signal.getType()) {
            case PHYSICAL_PAIN:
                // Être frappé augmente le stress et diminue l'humeur
                increaseStress(0.3);
                decreaseMood(0.2);
                sendSignal(new BrainSignal(BrainSignal.SignalType.NEGATIVE_FEELING, moduleName)
                    .withData("reason", "physical_pain")
                    .withData("intensity", 0.5));
                break;

            case PHYSICAL_PLEASURE:
                // Recevoir un cadeau améliore l'humeur et réduit le stress
                decreaseStress(0.2);
                increaseMood(0.3);
                sendSignal(new BrainSignal(BrainSignal.SignalType.POSITIVE_FEELING, moduleName)
                    .withData("reason", "gift_received")
                    .withData("intensity", 0.4));
                break;

            case CONVERSATION_START:
                // Début de conversation - légère réaction selon l'humeur actuelle
                if (currentStress > 0.7) {
                    increaseStress(0.1); // Plus stressé si déjà stressé
                } else if (currentMood > 0.5) {
                    increaseMood(0.05); // Plus joyeux si de bonne humeur
                }
                break;

            case RELATIONSHIP_UPDATE:
                // Mise à jour de relation affecte l'humeur
                Double sentiment = (Double) signal.getData("sentiment");
                if (sentiment != null) {
                    if (sentiment > 0.5) {
                        increaseMood(0.1);
                    } else if (sentiment < -0.5) {
                        decreaseMood(0.1);
                        increaseStress(0.1);
                    }
                }
                break;

            default:
                // Ignore other signals
                break;
        }
    }

    /**
     * Augmenter l'humeur (bonheur).
     */
    private void increaseMood(double amount) {
        double oldMood = currentMood;
        currentMood = Math.min(1.0, currentMood + amount);

        if (currentMood != oldMood) {
            IAMOD.LOGGER.debug("😊 Mood increased: {} → {}", oldMood, currentMood);
            sendSignal(new BrainSignal(BrainSignal.SignalType.EMOTION_CHANGE, moduleName)
                .withData("emotion", "happiness")
                .withData("oldValue", oldMood)
                .withData("newValue", currentMood));

            // Sync back to story
            if (psychology != null) {
                psychology.moodBaseline = currentMood;
            }
        }
    }

    /**
     * Diminuer l'humeur (tristesse).
     */
    private void decreaseMood(double amount) {
        double oldMood = currentMood;
        currentMood = Math.max(-1.0, currentMood - amount);

        if (currentMood != oldMood) {
            IAMOD.LOGGER.debug("😢 Mood decreased: {} → {}", oldMood, currentMood);
            sendSignal(new BrainSignal(BrainSignal.SignalType.EMOTION_CHANGE, moduleName)
                .withData("emotion", "sadness")
                .withData("oldValue", oldMood)
                .withData("newValue", currentMood));

            // Sync back to story
            if (psychology != null) {
                psychology.moodBaseline = currentMood;
            }
        }
    }

    /**
     * Augmenter le stress.
     */
    private void increaseStress(double amount) {
        double oldStress = currentStress;
        // La résilience réduit l'augmentation du stress
        double actualIncrease = amount * (1.0 - resilience * 0.5);
        currentStress = Math.min(1.0, currentStress + actualIncrease);

        if (currentStress != oldStress) {
            IAMOD.LOGGER.debug("😰 Stress increased: {} → {}", oldStress, currentStress);
            sendSignal(new BrainSignal(BrainSignal.SignalType.STRESS_INCREASE, moduleName)
                .withData("oldValue", oldStress)
                .withData("newValue", currentStress));

            // Sync back to story
            if (psychology != null) {
                psychology.stress = currentStress;
            }
        }
    }

    /**
     * Diminuer le stress (relaxation).
     */
    private void decreaseStress(double amount) {
        double oldStress = currentStress;
        currentStress = Math.max(0.0, currentStress - amount);

        if (currentStress != oldStress) {
            IAMOD.LOGGER.debug("😌 Stress decreased: {} → {}", oldStress, currentStress);
            sendSignal(new BrainSignal(BrainSignal.SignalType.STRESS_DECREASE, moduleName)
                .withData("oldValue", oldStress)
                .withData("newValue", currentStress));

            // Sync back to story
            if (psychology != null) {
                psychology.stress = currentStress;
            }
        }
    }

    @Override
    public String getStateDescription() {
        return String.format(
            "Mood: %.2f (%.0f%%), Stress: %.2f (%.0f%%), Resilience: %.2f",
            currentMood, (currentMood + 1.0) * 50,
            currentStress, currentStress * 100,
            resilience
        );
    }

    /**
     * Get current emotional state as text for AI prompt.
     */
    public String getEmotionalStateForPrompt() {
        StringBuilder state = new StringBuilder();

        // Mood description
        if (currentMood > 0.7) {
            state.append("Je me sens très joyeux et optimiste. ");
        } else if (currentMood > 0.3) {
            state.append("Je me sens de bonne humeur. ");
        } else if (currentMood > -0.3) {
            state.append("Je me sens neutre, ni joyeux ni triste. ");
        } else if (currentMood > -0.7) {
            state.append("Je me sens un peu triste. ");
        } else {
            state.append("Je me sens très triste et déprimé. ");
        }

        // Stress description
        if (currentStress > 0.8) {
            state.append("Je suis extrêmement stressé et tendu. ");
        } else if (currentStress > 0.6) {
            state.append("Je suis assez stressé. ");
        } else if (currentStress > 0.4) {
            state.append("Je ressens un peu de stress. ");
        } else if (currentStress > 0.2) {
            state.append("Je suis relativement calme. ");
        } else {
            state.append("Je suis très calme et détendu. ");
        }

        return state.toString();
    }

    // Getters
    public double getCurrentMood() { return currentMood; }
    public double getCurrentStress() { return currentStress; }
    public double getResilience() { return resilience; }
}
