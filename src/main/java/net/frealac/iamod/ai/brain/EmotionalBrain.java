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

    // SCIENTIFIC EMOTIONAL INERTIA
    private double moodMomentum;     // Rate of mood change (prevents instant shifts)
    private double stressMomentum;   // Rate of stress change
    private long lastUpdateTime;     // For decay calculations

    // Emotional regulation parameters
    private static final double MOOD_CHANGE_RATE_LIMIT = 0.05;    // Max mood change per event
    private static final double STRESS_CHANGE_RATE_LIMIT = 0.08;  // Max stress change per event
    private static final double MOOD_DECAY_RATE = 0.01;           // Decay toward baseline per hour
    private static final double STRESS_DECAY_RATE = 0.02;         // Stress naturally reduces over time

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

        this.moodMomentum = 0.0;
        this.stressMomentum = 0.0;
        this.lastUpdateTime = System.currentTimeMillis();

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

            case POSITIVE_FEELING:
                // Message positif du joueur → améliore humeur, réduit stress
                Double positiveIntensity = (Double) signal.getData("intensity");
                double posAmount = positiveIntensity != null ? positiveIntensity * 0.1 : 0.05;
                increaseMood(posAmount);
                decreaseStress(posAmount * 0.5);
                IAMOD.LOGGER.info("💚 Positive feeling from message: mood +{}, stress -{}",
                    posAmount, posAmount * 0.5);
                break;

            case NEGATIVE_FEELING:
                // Message négatif du joueur → diminue humeur, augmente stress
                Double negativeIntensity = (Double) signal.getData("intensity");
                double negAmount = negativeIntensity != null ? negativeIntensity * 0.1 : 0.05;
                decreaseMood(negAmount);
                increaseStress(negAmount * 0.5);
                IAMOD.LOGGER.info("💔 Negative feeling from message: mood -{}, stress +{}",
                    negAmount, negAmount * 0.5);
                break;

            default:
                // Ignore other signals
                break;
        }
    }

    /**
     * Apply emotional decay (mood returns to baseline, stress reduces over time).
     * SCIENTIFIC BASIS: Emotional homeostasis - emotions naturally regulate toward baseline.
     */
    private void applyEmotionalDecay() {
        long currentTime = System.currentTimeMillis();
        double hoursElapsed = (currentTime - lastUpdateTime) / (1000.0 * 60.0 * 60.0);
        lastUpdateTime = currentTime;

        if (hoursElapsed > 0) {
            // Mood decay toward baseline
            double moodBaseline = psychology != null ? psychology.moodBaseline : 0.0;
            if (currentMood > moodBaseline) {
                currentMood = Math.max(moodBaseline, currentMood - (MOOD_DECAY_RATE * hoursElapsed));
            } else if (currentMood < moodBaseline) {
                currentMood = Math.min(moodBaseline, currentMood + (MOOD_DECAY_RATE * hoursElapsed));
            }

            // Stress naturally reduces over time
            if (currentStress > 0.1) {
                currentStress = Math.max(0.1, currentStress - (STRESS_DECAY_RATE * hoursElapsed));
            }

            // Sync back to story
            if (psychology != null) {
                psychology.moodBaseline = currentMood;
                psychology.stress = currentStress;
            }
        }
    }

    /**
     * Augmenter l'humeur (bonheur) avec inertie émotionnelle.
     * SCIENTIFIC BASIS: Emotions change gradually, not instantly.
     */
    private void increaseMood(double amount) {
        applyEmotionalDecay(); // Apply decay first

        double oldMood = currentMood;

        // RATE LIMITING: Prevent instant mood shifts
        double actualChange = Math.min(amount, MOOD_CHANGE_RATE_LIMIT);

        // Apply momentum: resist large changes
        if (Math.abs(amount - moodMomentum) > 0.1) {
            actualChange *= 0.5; // Reduce change if direction shifts suddenly
        }

        currentMood = Math.min(1.0, currentMood + actualChange);
        moodMomentum = actualChange;

        if (Math.abs(currentMood - oldMood) > 0.01) {
            IAMOD.LOGGER.debug("😊 Mood increased: {} → {} (requested +{}, applied +{})",
                oldMood, currentMood, amount, actualChange);
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
     * Diminuer l'humeur (tristesse) avec inertie émotionnelle.
     * SCIENTIFIC BASIS: Negative emotions also change gradually.
     */
    private void decreaseMood(double amount) {
        applyEmotionalDecay(); // Apply decay first

        double oldMood = currentMood;

        // RATE LIMITING: Prevent instant mood shifts
        double actualChange = Math.min(amount, MOOD_CHANGE_RATE_LIMIT);

        // Apply momentum: resist large changes
        if (Math.abs(-amount - moodMomentum) > 0.1) {
            actualChange *= 0.5; // Reduce change if direction shifts suddenly
        }

        currentMood = Math.max(-1.0, currentMood - actualChange);
        moodMomentum = -actualChange;

        if (Math.abs(currentMood - oldMood) > 0.01) {
            IAMOD.LOGGER.debug("😢 Mood decreased: {} → {} (requested -{}, applied -{})",
                oldMood, currentMood, amount, actualChange);
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
     * Augmenter le stress avec rate limiting.
     * SCIENTIFIC BASIS: Stress accumulates but not instantly.
     */
    private void increaseStress(double amount) {
        applyEmotionalDecay(); // Apply decay first

        double oldStress = currentStress;

        // La résilience réduit l'augmentation du stress
        double resilienceReduction = (1.0 - resilience * 0.5);
        double actualIncrease = Math.min(amount * resilienceReduction, STRESS_CHANGE_RATE_LIMIT);

        currentStress = Math.min(1.0, currentStress + actualIncrease);
        stressMomentum = actualIncrease;

        if (Math.abs(currentStress - oldStress) > 0.01) {
            IAMOD.LOGGER.debug("😰 Stress increased: {} → {} (requested +{}, applied +{})",
                oldStress, currentStress, amount, actualIncrease);
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
     * Diminuer le stress (relaxation) avec rate limiting.
     */
    private void decreaseStress(double amount) {
        applyEmotionalDecay(); // Apply decay first

        double oldStress = currentStress;
        double actualDecrease = Math.min(amount, STRESS_CHANGE_RATE_LIMIT);
        currentStress = Math.max(0.0, currentStress - actualDecrease);
        stressMomentum = -actualDecrease;

        if (Math.abs(currentStress - oldStress) > 0.01) {
            IAMOD.LOGGER.debug("😌 Stress decreased: {} → {} (requested -{}, applied -{})",
                oldStress, currentStress, amount, actualDecrease);
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
