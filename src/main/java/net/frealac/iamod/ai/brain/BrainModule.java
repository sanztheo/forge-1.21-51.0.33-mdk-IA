package net.frealac.iamod.ai.brain;

import java.util.HashMap;
import java.util.Map;

/**
 * Base class for all brain modules.
 * Each module represents a specialized part of the villager's brain,
 * similar to how the human brain has specialized regions.
 */
public abstract class BrainModule {

    protected final String moduleName;
    protected BrainHub hub;
    protected Map<String, Object> state;

    public BrainModule(String moduleName) {
        this.moduleName = moduleName;
        this.state = new HashMap<>();
    }

    /**
     * Connect this module to the brain hub (central communication).
     */
    public void connectToHub(BrainHub hub) {
        this.hub = hub;
        onConnected();
    }

    /**
     * Called when the module is connected to the hub.
     */
    protected void onConnected() {
        // Override in subclasses if needed
    }

    /**
     * Process incoming signal from another brain module.
     */
    public abstract void receiveSignal(BrainSignal signal);

    /**
     * Send a signal to other brain modules through the hub.
     */
    protected void sendSignal(BrainSignal signal) {
        if (hub != null) {
            hub.broadcastSignal(signal, this);
        }
    }

    /**
     * Get the module's current state as a string for debugging.
     */
    public abstract String getStateDescription();

    /**
     * Get internal state value.
     */
    protected Object getState(String key) {
        return state.get(key);
    }

    /**
     * Set internal state value.
     */
    protected void setState(String key, Object value) {
        state.put(key, value);
    }

    /**
     * Get module name.
     */
    public String getModuleName() {
        return moduleName;
    }
}
