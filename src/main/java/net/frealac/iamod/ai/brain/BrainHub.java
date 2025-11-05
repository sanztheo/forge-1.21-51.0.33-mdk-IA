package net.frealac.iamod.ai.brain;

import net.frealac.iamod.IAMOD;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Central hub for brain module communication.
 * Like the corpus callosum in the human brain, this connects all modules
 * and allows them to communicate with each other.
 */
public class BrainHub {

    private final List<BrainModule> modules;
    private final List<BrainSignal> signalHistory;
    private static final int MAX_SIGNAL_HISTORY = 50;

    public BrainHub() {
        this.modules = new CopyOnWriteArrayList<>();
        this.signalHistory = new ArrayList<>();
    }

    /**
     * Register a brain module to the hub.
     */
    public void registerModule(BrainModule module) {
        if (!modules.contains(module)) {
            modules.add(module);
            module.connectToHub(this);
            IAMOD.LOGGER.info("Brain module registered: {}", module.getModuleName());
        }
    }

    /**
     * Unregister a brain module from the hub.
     */
    public void unregisterModule(BrainModule module) {
        modules.remove(module);
        IAMOD.LOGGER.info("Brain module unregistered: {}", module.getModuleName());
    }

    /**
     * Broadcast a signal to all modules except the sender.
     * This simulates how brain regions communicate with each other.
     * @param sender The module sending the signal, or null for external signals
     */
    public void broadcastSignal(BrainSignal signal, BrainModule sender) {
        // Add to history
        addToHistory(signal);

        String senderName = (sender != null) ? sender.getModuleName() : "External";
        IAMOD.LOGGER.debug("Broadcasting signal: {} from {}", signal.getType(), senderName);

        // Send to all modules except sender
        for (BrainModule module : modules) {
            if (module != sender) {
                try {
                    module.receiveSignal(signal);
                } catch (Exception e) {
                    IAMOD.LOGGER.error("Error sending signal to module {}: {}",
                        module.getModuleName(), e.getMessage());
                }
            }
        }
    }

    /**
     * Send a signal to a specific module.
     */
    public void sendToModule(BrainSignal signal, String targetModuleName) {
        addToHistory(signal);

        for (BrainModule module : modules) {
            if (module.getModuleName().equals(targetModuleName)) {
                try {
                    module.receiveSignal(signal);
                    IAMOD.LOGGER.debug("Signal sent to {}: {}", targetModuleName, signal.getType());
                } catch (Exception e) {
                    IAMOD.LOGGER.error("Error sending signal to {}: {}",
                        targetModuleName, e.getMessage());
                }
                return;
            }
        }

        IAMOD.LOGGER.warn("Module not found: {}", targetModuleName);
    }

    /**
     * Add signal to history (for debugging and analysis).
     */
    private void addToHistory(BrainSignal signal) {
        signalHistory.add(signal);

        // Keep only recent signals
        if (signalHistory.size() > MAX_SIGNAL_HISTORY) {
            signalHistory.remove(0);
        }
    }

    /**
     * Get all registered modules.
     */
    public List<BrainModule> getModules() {
        return new ArrayList<>(modules);
    }

    /**
     * Get a module by name.
     */
    public BrainModule getModule(String moduleName) {
        for (BrainModule module : modules) {
            if (module.getModuleName().equals(moduleName)) {
                return module;
            }
        }
        return null;
    }

    /**
     * Get signal history.
     */
    public List<BrainSignal> getSignalHistory() {
        return new ArrayList<>(signalHistory);
    }

    /**
     * Get recent signals of a specific type.
     */
    public List<BrainSignal> getRecentSignals(BrainSignal.SignalType type, int count) {
        List<BrainSignal> result = new ArrayList<>();

        // Go backwards through history
        for (int i = signalHistory.size() - 1; i >= 0 && result.size() < count; i--) {
            BrainSignal signal = signalHistory.get(i);
            if (signal.getType() == type) {
                result.add(signal);
            }
        }

        return result;
    }

    /**
     * Clear signal history.
     */
    public void clearHistory() {
        signalHistory.clear();
    }

    /**
     * Get a summary of all brain module states (for debugging).
     */
    public String getSystemState() {
        StringBuilder state = new StringBuilder();
        state.append("=== BRAIN SYSTEM STATE ===\n");
        state.append("Modules: ").append(modules.size()).append("\n");
        state.append("Signal history: ").append(signalHistory.size()).append("\n\n");

        for (BrainModule module : modules) {
            state.append("Module: ").append(module.getModuleName()).append("\n");
            state.append(module.getStateDescription()).append("\n\n");
        }

        return state.toString();
    }
}
