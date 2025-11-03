package net.frealac.iamod.client.screen;

import net.frealac.iamod.network.NetworkHandler;
import net.frealac.iamod.network.packet.CloseDialogC2SPacket;
import net.frealac.iamod.network.packet.PlayerMessageC2SPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

public class VillagerDialogScreen extends Screen {
    private final int villagerId;
    private final List<Entry> history = new ArrayList<>();
    private EditBox input;
    private Button send;
    private int panelX, panelY, panelW, panelH;

    private static final int PADDING = 8;
    private static final int INPUT_H = 20;
    private static final int BUTTON_W = 60;

    public VillagerDialogScreen(int villagerId, String greeting) {
        super(Component.literal("Villageois"));
        this.villagerId = villagerId;
        this.history.add(Entry.npc(greeting));
    }

    public int getVillagerId() { return villagerId; }

    @Override
    protected void init() {
        this.panelW = Math.min(340, this.width - 40);
        this.panelH = Math.min(240, this.height - 40);
        this.panelX = (this.width - panelW) / 2;
        this.panelY = (this.height - panelH) / 2;

        int inputY = panelY + panelH - PADDING - INPUT_H;
        int inputX = panelX + PADDING;
        int inputW = panelW - (PADDING * 3) - BUTTON_W;

        input = new EditBox(this.font, inputX, inputY, inputW, INPUT_H, Component.literal("Message"));
        input.setMaxLength(500);
        addRenderableWidget(input);

        send = Button.builder(Component.literal("Envoyer"), b -> send()).bounds(inputX + inputW + PADDING, inputY, BUTTON_W, INPUT_H).build();
        addRenderableWidget(send);

        setInitialFocus(input);
        
        // Bloquer les mouvements du joueur
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.input.leftImpulse = 0;
            this.minecraft.player.input.forwardImpulse = 0;
            this.minecraft.player.input.jumping = false;
            this.minecraft.player.input.shiftKeyDown = false;
        }
    }

    @Override
    public void tick() {
        super.tick();
        // Bloquer continuellement les mouvements du joueur
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.input.leftImpulse = 0;
            this.minecraft.player.input.forwardImpulse = 0;
            this.minecraft.player.input.jumping = false;
            this.minecraft.player.input.shiftKeyDown = false;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderTransparentBackground(GuiGraphics g) {
        // Ne rien faire pour désactiver l'arrière-plan flou
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int bg = 0xC0101010;
        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, bg);

        int titleY = panelY + PADDING;
        g.drawCenteredString(this.font, this.title, panelX + panelW / 2, titleY, 0xFFFFFF);

        int top = titleY + 12 + PADDING;
        int bottom = panelY + panelH - (PADDING * 2) - INPUT_H;
        int left = panelX + PADDING;
        int right = panelX + panelW - PADDING;
        int contentW = right - left;

        List<FormattedCharSequence> wrapped = new ArrayList<>();
        for (Entry e : history) {
            var prefix = e.isNpc ? "§eVillageois: §r" : "§bMoi: §r";
            var lines = this.font.split(Component.literal(prefix + e.text), contentW);
            wrapped.addAll(lines);
        }
        int lineH = 9;
        int maxLines = Math.max(1, (bottom - top) / lineH);
        int start = Math.max(0, wrapped.size() - maxLines);

        int y = top;
        for (int i = start; i < wrapped.size(); i++) {
            g.drawString(this.font, wrapped.get(i), left, y, 0xFFFFFF);
            y += lineH;
        }

        super.render(g, mouseX, mouseY, partialTick);
    }

    private void send() {
        String txt = input.getValue().trim();
        if (txt.isEmpty()) return;
        input.setValue("");
        appendPlayer(txt);
        NetworkHandler.CHANNEL.send(new PlayerMessageC2SPacket(villagerId, txt), net.minecraftforge.network.PacketDistributor.SERVER.noArg());
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.input.keyPressed(keyCode, scanCode, modifiers)) return true;
        if (keyCode == 257 || keyCode == 335) { // Enter
            send(); return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public void appendNpc(String text) { history.add(Entry.npc(text)); }
    private void appendPlayer(String text) { history.add(Entry.player(text)); }

    @Override
    public void onClose() {
        // Notifier le serveur que la conversation est terminée
        NetworkHandler.CHANNEL.send(new CloseDialogC2SPacket(villagerId), 
                net.minecraftforge.network.PacketDistributor.SERVER.noArg());
        super.onClose();
    }

    @Override
    public boolean shouldCloseOnEsc() { return true; }

    private static class Entry {
        final boolean isNpc; final String text;
        private Entry(boolean isNpc, String text) { this.isNpc = isNpc; this.text = text; }
        static Entry npc(String t) { return new Entry(true, t); }
        static Entry player(String t) { return new Entry(false, t); }
    }
}
