package net.frealac.iamod.client.screen;

import net.frealac.iamod.network.NetworkHandler;
import net.frealac.iamod.network.packet.UpdateAIConfigC2SPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;

/**
 * Screen for configuring AI behavior and settings.
 */
public class AIConfigScreen extends Screen {
    private final int entityId;
    private final Map<String, Boolean> goalStates = new HashMap<>();

    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 25;

    public AIConfigScreen(int entityId) {
        super(Component.literal("AI Configuration"));
        this.entityId = entityId;

        // Default goal states
        goalStates.put("patrol", false);
        goalStates.put("follow_player", false);
        goalStates.put("collect_resources", false);
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int startY = 60;

        // Title
        // (will be rendered in render method)

        // Patrol Goal Button
        addRenderableWidget(Button.builder(
            getGoalButtonText("Patrol", goalStates.get("patrol")),
            button -> {
                toggleGoal("patrol");
                button.setMessage(getGoalButtonText("Patrol", goalStates.get("patrol")));
            }
        ).bounds(centerX - BUTTON_WIDTH / 2, startY, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        // Follow Player Goal Button
        addRenderableWidget(Button.builder(
            getGoalButtonText("Follow Player", goalStates.get("follow_player")),
            button -> {
                toggleGoal("follow_player");
                button.setMessage(getGoalButtonText("Follow Player", goalStates.get("follow_player")));
            }
        ).bounds(centerX - BUTTON_WIDTH / 2, startY + BUTTON_SPACING, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        // Collect Resources Goal Button
        addRenderableWidget(Button.builder(
            getGoalButtonText("Collect Resources", goalStates.get("collect_resources")),
            button -> {
                toggleGoal("collect_resources");
                button.setMessage(getGoalButtonText("Collect Resources", goalStates.get("collect_resources")));
            }
        ).bounds(centerX - BUTTON_WIDTH / 2, startY + BUTTON_SPACING * 2, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        // Info Button
        addRenderableWidget(Button.builder(
            Component.literal("View AI Info"),
            button -> {
                // Open info screen or show tooltip
            }
        ).bounds(centerX - BUTTON_WIDTH / 2, startY + BUTTON_SPACING * 3, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        // Done Button
        addRenderableWidget(Button.builder(
            Component.literal("Done"),
            button -> this.onClose()
        ).bounds(centerX - BUTTON_WIDTH / 2, this.height - 40, BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }

    private Component getGoalButtonText(String goalName, boolean enabled) {
        String status = enabled ? "§aEnabled" : "§cDisabled";
        return Component.literal(goalName + ": " + status);
    }

    private void toggleGoal(String goalName) {
        boolean newState = !goalStates.get(goalName);
        goalStates.put(goalName, newState);

        // Send packet to server
        NetworkHandler.CHANNEL.send(new UpdateAIConfigC2SPacket(entityId, goalName, newState), PacketDistributor.SERVER.noArg());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Render background
        this.renderBackground(graphics, mouseX, mouseY, partialTick);

        // Render title
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);

        // Render subtitle
        graphics.drawCenteredString(
            this.font,
            Component.literal("Configure AI Goals and Behavior"),
            this.width / 2,
            35,
            0xAAAAAA
        );

        // Render buttons and widgets
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false; // Don't pause the game
    }

    public void setGoalState(String goalName, boolean enabled) {
        goalStates.put(goalName, enabled);
    }

    public Map<String, Boolean> getGoalStates() {
        return new HashMap<>(goalStates);
    }
}
