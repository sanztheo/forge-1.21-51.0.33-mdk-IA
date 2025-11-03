package net.frealac.iamod.client.screen;

import net.frealac.iamod.network.NetworkHandler;
import net.frealac.iamod.network.packet.CloseDialogC2SPacket;
import net.frealac.iamod.network.packet.PlayerMessageC2SPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

public class VillagerDialogScreen extends Screen {
    private final int villagerId;
    private final List<Entry> history = new ArrayList<>();
    private EditBox input;
    private Button tabChatBtn, tabHistoryBtn, tabHealthBtn, debugBtn;
    private Mode mode = Mode.CHAT;
    private boolean debugOverlay = false;
    
    private enum Mode { CHAT, HISTORY, HEALTH }
    
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
        
        // Tabs + Debug button
        int tabY = dialogY - 22;
        int bw = 80; int bh = 18; int gap = 8;
        int bx = dialogX;
        tabChatBtn = Button.builder(Component.literal("Chat"), b -> { mode = Mode.CHAT; })
                .bounds(bx, tabY, bw, bh).build(); bx += bw + gap;
        tabHistoryBtn = Button.builder(Component.literal("Histoire"), b -> { mode = Mode.HISTORY; })
                .bounds(bx, tabY, bw, bh).build(); bx += bw + gap;
        tabHealthBtn = Button.builder(Component.literal("Santé"), b -> { mode = Mode.HEALTH; })
                .bounds(bx, tabY, bw, bh).build(); bx += bw + gap;
        debugBtn = Button.builder(Component.literal("Debug"), b -> { debugOverlay = !debugOverlay; })
                .bounds(dialogX + dialogW - 70, tabY, 70, bh).build();
        addRenderableWidget(tabChatBtn);
        addRenderableWidget(tabHistoryBtn);
        addRenderableWidget(tabHealthBtn);
        addRenderableWidget(debugBtn);
        
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
        
        // Zone de scrolling & content area
        int historyY = textY + 14;
        int historyH = textH - 14;
        
        switch (mode) {
            case CHAT -> renderChat(g, textX, historyY, textW, historyH);
            case HISTORY -> renderHistory(g, textX, historyY, textW, historyH);
            case HEALTH -> renderHealth(g, textX, historyY, textW, historyH);
        }
        
        if (debugOverlay) {
            renderDebugOverlay(g, dialogX + 8, dialogY + 8, dialogW - 16, DIALOG_HEIGHT - 16);
        }
        
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void renderChat(GuiGraphics g, int x, int y, int w, int h) {
        List<FormattedCharSequence> wrapped = new ArrayList<>();
        for (Entry e : history) {
            String color = e.isNpc ? "§e" : "§b";
            String prefix = e.isNpc ? "" : "§7Vous: §r";
            var lines = this.font.split(Component.literal(color + prefix + e.text), w);
            wrapped.addAll(lines);
            wrapped.add(FormattedCharSequence.EMPTY);
        }
        int lineH = 10;
        int maxLines = Math.max(1, h / lineH);
        int start = Math.max(0, wrapped.size() - maxLines);
        int yy = y;
        for (int i = start; i < wrapped.size() && yy < y + h; i++) {
            g.drawString(this.font, wrapped.get(i), x, yy, 0xFFFFFF);
            yy += lineH;
        }
    }

    private void renderHistory(GuiGraphics g, int x, int y, int w, int h) {
        var story = net.frealac.iamod.client.story.ClientStoryCache.get(villagerId);
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("§6Histoire de vie"));
        if (story == null || story.lifeTimeline == null || story.lifeTimeline.isEmpty()) {
            lines.add(Component.literal("Aucune entrée."));
        } else {
            story.lifeTimeline.stream()
                    .sorted((a,b) -> Integer.compare(a.age, b.age))
                    .forEach(ev -> lines.add(Component.literal("§7" + ev.age + " ans§r – " + ev.type + (ev.place!=null?" @"+ev.place:"") + (ev.details!=null?": "+ev.details:""))));
        }
        drawWrapped(g, lines, x, y, w, h);
    }

    private void renderHealth(GuiGraphics g, int x, int y, int w, int h) {
        var story = net.frealac.iamod.client.story.ClientStoryCache.get(villagerId);
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("§6Santé & Psychologie"));
        if (story == null) { lines.add(Component.literal("Aucune donnée.")); drawWrapped(g, lines, x, y, w, h); return; }
        if (story.health != null) {
            lines.add(Component.literal("§eSanté:"));
            if (!story.health.allergies.isEmpty()) lines.add(Component.literal("Allergies: " + String.join(", ", story.health.allergies)));
            if (!story.health.wounds.isEmpty()) lines.add(Component.literal("Blessures: " + story.health.wounds.size()));
            lines.add(Component.literal(String.format("Endurance: %.2f Sommeil: %.2f", story.health.stamina, story.health.sleepQuality)));
        }
        if (story.psychology != null) {
            lines.add(Component.literal("§ePsychologie:"));
            lines.add(Component.literal(String.format("Humeur: %.2f Stress: %.2f Résilience: %.2f", story.psychology.moodBaseline, story.psychology.stress, story.psychology.resilience)));
            if (story.psychology.trauma != null && story.psychology.trauma.events != null && !story.psychology.trauma.events.isEmpty()) {
                lines.add(Component.literal("Traumas:"));
                for (var t : story.psychology.trauma.events) {
                    lines.add(Component.literal("- " + t.type + " (" + t.severity + ") @" + t.ageAt));
                }
            }
        }
        drawWrapped(g, lines, x, y, w, h);
    }

    private void renderDebugOverlay(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xC0000000);
        var story = net.frealac.iamod.client.story.ClientStoryCache.get(villagerId);
        if (story == null) {
            g.drawString(this.font, Component.literal("(debug) story indisponible"), x + 6, y + 6, 0xFFFFFF);
            return;
        }
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("§6DEBUG: PNJ Story complète"));
        lines.add(Component.literal("UUID: " + story.uuid));
        lines.add(Component.literal("Nom: " + story.nameGiven + " " + story.nameFamily));
        lines.add(Component.literal("Sexe: " + story.sex + " Âge: " + story.ageYears));
        lines.add(Component.literal("Culture: " + story.cultureId + " Profession: " + story.profession));
        lines.add(Component.literal("Traits: " + String.join(", ", story.traits)));
        if (!story.parents.isEmpty()) lines.add(Component.literal("Parents: " + String.join(", ", story.parents)));
        if (!story.children.isEmpty()) lines.add(Component.literal("Enfants: " + String.join(", ", story.children)));
        if (!story.siblings.isEmpty()) lines.add(Component.literal("Fratrie: " + String.join(", ", story.siblings)));
        if (!story.memories.isEmpty()) lines.add(Component.literal("Souvenirs simples: " + String.join(" | ", story.memories)));
        if (story.health != null) lines.add(Component.literal("Health: wounds=" + (story.health.wounds!=null?story.health.wounds.size():0) + ", allergies=" + (story.health.allergies!=null?story.health.allergies.size():0)));
        if (story.psychology != null && story.psychology.trauma != null) lines.add(Component.literal("Trauma events=" + (story.psychology.trauma.events!=null?story.psychology.trauma.events.size():0)));
        lines.add(Component.literal("bioBrief: " + story.bioBrief));
        drawWrapped(g, lines, x + 6, y + 6 + 12, w - 12, h - 18);
    }

    private void drawWrapped(GuiGraphics g, List<Component> lines, int x, int y, int w, int h) {
        List<FormattedCharSequence> wrapped = new ArrayList<>();
        for (Component c : lines) {
            wrapped.addAll(this.font.split(c, w));
        }
        int lineH = 10;
        int maxLines = Math.max(1, h / lineH);
        int start = 0;
        int yy = y;
        for (int i = start; i < wrapped.size() && yy < y + h; i++) {
            g.drawString(this.font, wrapped.get(i), x, yy, 0xFFFFFF);
            yy += lineH;
        }
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
