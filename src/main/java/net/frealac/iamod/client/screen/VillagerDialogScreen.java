package net.frealac.iamod.client.screen;

import net.frealac.iamod.network.NetworkHandler;
import net.frealac.iamod.network.packet.CloseDialogC2SPacket;
import net.frealac.iamod.network.packet.PlayerMessageC2SPacket;
import net.minecraft.client.gui.GuiGraphics;
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
    
    // Layout constants - Style RPG en bas de l'écran
    private static final int DIALOG_HEIGHT = 180;
    private static final int PORTRAIT_SIZE = 64;
    private static final int PADDING = 12;
    private static final int INPUT_HEIGHT = 24;
    private static final int MARGIN_BOTTOM = 20;
    private static final int MARGIN_SIDE = 40;

    public VillagerDialogScreen(int villagerId, String greeting) {
        super(Component.literal("Villageois"));
        this.villagerId = villagerId;
        this.history.add(Entry.npc(greeting));
    }

    public int getVillagerId() { return villagerId; }

    @Override
    protected void init() {
        // Calculer la position de la boîte de dialogue en bas de l'écran
        int dialogY = this.height - DIALOG_HEIGHT - MARGIN_BOTTOM;
        int dialogX = MARGIN_SIDE;
        int dialogW = this.width - (MARGIN_SIDE * 2);
        
        // Position du champ de texte en bas de la boîte
        int inputY = dialogY + DIALOG_HEIGHT - PADDING - INPUT_HEIGHT;
        int inputX = dialogX + PORTRAIT_SIZE + PADDING * 2;
        int inputW = dialogW - PORTRAIT_SIZE - PADDING * 3;

        input = new EditBox(this.font, inputX, inputY, inputW, INPUT_HEIGHT, Component.literal("Votre réponse..."));
        input.setMaxLength(500);
        input.setBordered(true);
        addRenderableWidget(input);

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
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Ne rien faire - empêche le rendu du flou et de l'arrière-plan
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Calculer les positions
        int dialogY = this.height - DIALOG_HEIGHT - MARGIN_BOTTOM;
        int dialogX = MARGIN_SIDE;
        int dialogW = this.width - (MARGIN_SIDE * 2);
        
        // Fond principal de la boîte de dialogue - Style RPG sombre
        g.fill(dialogX, dialogY, dialogX + dialogW, dialogY + DIALOG_HEIGHT, 0xE0000000);
        
        // Bordure dorée/claire autour de la boîte
        int borderColor = 0xFF8B7355;
        g.fill(dialogX - 2, dialogY - 2, dialogX + dialogW + 2, dialogY, borderColor); // Top
        g.fill(dialogX - 2, dialogY + DIALOG_HEIGHT, dialogX + dialogW + 2, dialogY + DIALOG_HEIGHT + 2, borderColor); // Bottom
        g.fill(dialogX - 2, dialogY, dialogX, dialogY + DIALOG_HEIGHT, borderColor); // Left
        g.fill(dialogX + dialogW, dialogY, dialogX + dialogW + 2, dialogY + DIALOG_HEIGHT, borderColor); // Right
        
        // Zone portrait du villageois à gauche
        int portraitX = dialogX + PADDING;
        int portraitY = dialogY + PADDING;
        
        // Fond du portrait
        g.fill(portraitX, portraitY, portraitX + PORTRAIT_SIZE, portraitY + PORTRAIT_SIZE, 0xFF2A1810);
        // Bordure du portrait
        g.fill(portraitX - 1, portraitY - 1, portraitX + PORTRAIT_SIZE + 1, portraitY, 0xFF8B7355);
        g.fill(portraitX - 1, portraitY + PORTRAIT_SIZE, portraitX + PORTRAIT_SIZE + 1, portraitY + PORTRAIT_SIZE + 1, 0xFF8B7355);
        g.fill(portraitX - 1, portraitY, portraitX, portraitY + PORTRAIT_SIZE, 0xFF8B7355);
        g.fill(portraitX + PORTRAIT_SIZE, portraitY, portraitX + PORTRAIT_SIZE + 1, portraitY + PORTRAIT_SIZE, 0xFF8B7355);
        
        // Icône du villageois (simplifié - juste un "V" pour l'instant)
        g.drawCenteredString(this.font, "§6⚒", portraitX + PORTRAIT_SIZE / 2, portraitY + PORTRAIT_SIZE / 2 - 4, 0xFFFFFF);
        
        // Zone de texte à droite du portrait
        int textX = portraitX + PORTRAIT_SIZE + PADDING * 2;
        int textY = portraitY;
        int textW = dialogW - PORTRAIT_SIZE - PADDING * 4;
        int textH = DIALOG_HEIGHT - INPUT_HEIGHT - PADDING * 3;
        
        // Nom du villageois en haut
        g.drawString(this.font, "§6§lVillageois", textX, textY, 0xFFFFFF);
        
        // Zone de scrolling pour l'historique
        int historyY = textY + 14;
        int historyH = textH - 14;
        
        // Afficher l'historique de conversation
        List<FormattedCharSequence> wrapped = new ArrayList<>();
        for (Entry e : history) {
            String color = e.isNpc ? "§e" : "§b";
            String prefix = e.isNpc ? "" : "§7Vous: §r";
            var lines = this.font.split(Component.literal(color + prefix + e.text), textW);
            wrapped.addAll(lines);
            wrapped.add(FormattedCharSequence.EMPTY); // Ligne vide entre messages
        }
        
        int lineH = 10;
        int maxLines = Math.max(1, historyH / lineH);
        int start = Math.max(0, wrapped.size() - maxLines);
        
        int y = historyY;
        for (int i = start; i < wrapped.size() && y < historyY + historyH; i++) {
            g.drawString(this.font, wrapped.get(i), textX, y, 0xFFFFFF);
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
