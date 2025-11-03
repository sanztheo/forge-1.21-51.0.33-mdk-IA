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
    private Button tabChatBtn, tabSummaryBtn, tabHistoryBtn, tabHealthBtn, tabRelationsBtn, debugBtn;
    private Mode mode = Mode.SUMMARY;
    private boolean debugOverlay = false;
    private int chatScroll = 0; // lines scrolled up (0 = bottom)
    private int chatTotalLines = 0; // for clamping + scrollbar
    private int contentScroll = 0; // generic scroll for non-chat tabs
    private int contentTotalLines = 0; // updated each render of non-chat tabs
    private int contentMaxLines = 0; // for scrollbar calc
    private StringBuilder streamingBuffer = null;
    private boolean isLoading = true;
    private int loadingTicks = 0;
    private boolean introShown = false;
    private boolean draggingContentScrollbar = false;
    private boolean draggingChatScrollbar = false;
    private int dragStartY;
    private int dragStartScroll;
    
    private enum Mode { SUMMARY, CHAT, HISTORY, HEALTH, RELATIONS }
    
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
        tabChatBtn = Button.builder(Component.literal("Chat"), b -> { mode = Mode.CHAT; contentScroll = 0; })
                .bounds(bx, tabY, bw, bh).build(); bx += bw + gap;
        tabSummaryBtn = Button.builder(Component.literal("Résumé"), b -> { mode = Mode.SUMMARY; contentScroll = 0; })
                .bounds(bx, tabY, bw, bh).build(); bx += bw + gap;
        tabHistoryBtn = Button.builder(Component.literal("Histoire"), b -> { mode = Mode.HISTORY; contentScroll = 0; })
                .bounds(bx, tabY, bw, bh).build(); bx += bw + gap;
        tabHealthBtn = Button.builder(Component.literal("Santé"), b -> { mode = Mode.HEALTH; contentScroll = 0; })
                .bounds(bx, tabY, bw, bh).build(); bx += bw + gap;
        tabRelationsBtn = Button.builder(Component.literal("Relations"), b -> { mode = Mode.RELATIONS; contentScroll = 0; })
                .bounds(bx, tabY, bw, bh).build(); bx += bw + gap;
        debugBtn = Button.builder(Component.literal("Debug"), b -> { debugOverlay = !debugOverlay; })
                .bounds(dialogX + dialogW - 70, tabY, 70, bh).build();
        addRenderableWidget(tabChatBtn);
        addRenderableWidget(tabSummaryBtn);
        addRenderableWidget(tabHistoryBtn);
        addRenderableWidget(tabHealthBtn);
        addRenderableWidget(tabRelationsBtn);
        addRenderableWidget(debugBtn);
        
        // Bloquer les mouvements du joueur
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.input.leftImpulse = 0;
            this.minecraft.player.input.forwardImpulse = 0;
            this.minecraft.player.input.jumping = false;
            this.minecraft.player.input.shiftKeyDown = false;
        }
        // If story already cached when screen opens, mark loaded and show intro
        var cached = net.frealac.iamod.client.story.ClientStoryCache.get(villagerId);
        if (cached != null) {
            this.isLoading = false;
            this.isEnriching = (cached.bioLong == null || cached.bioLong.isBlank());
            showIntroFromStory(cached);
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
        loadingTicks++;
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
        
        // Nom du villageois en haut (prénom + nom)
        var storyHeader = net.frealac.iamod.client.story.ClientStoryCache.get(villagerId);
        String header = (storyHeader != null && storyHeader.nameGiven != null) ?
                ("§6§l" + storyHeader.nameGiven + (storyHeader.nameFamily!=null?" "+storyHeader.nameFamily:"")) :
                "§6§lVillageois";
        g.drawString(this.font, header, textX, textY, 0xFFFFFF);
        
        // Zone de scrolling & content area
        int historyY = textY + 14;
        int historyH = textH - 14;
        
        if (!debugOverlay) {
            switch (mode) {
                case SUMMARY -> renderSummary(g, textX, historyY, textW, historyH);
                case CHAT -> renderChat(g, textX, historyY, textW, historyH);
                case HISTORY -> renderHistory(g, textX, historyY, textW, historyH);
                case HEALTH -> renderHealth(g, textX, historyY, textW, historyH);
                case RELATIONS -> renderRelations(g, textX, historyY, textW, historyH);
            }
        } else {
            renderDebugOverlay(g, dialogX + 8, dialogY + 8, dialogW - 16, DIALOG_HEIGHT - 16);
        }
        
        // Loading indicator
        int cx = dialogX + dialogW - 180;
        int cy = dialogY + 16;
        char[] spin = new char[]{'|','/','-','\\'};
        char ch = spin[(loadingTicks / 8) % spin.length];
        if (isLoading) {
            g.drawString(this.font, Component.literal("§7Chargement de l'histoire... §e" + ch), cx, cy, 0xFFFFFF);
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
        chatTotalLines = wrapped.size();
        int maxScroll = Math.max(0, chatTotalLines - maxLines);
        if (chatScroll > maxScroll) chatScroll = maxScroll;
        int start = Math.max(0, chatTotalLines - maxLines - chatScroll);
        int yy = y;
        for (int i = start; i < wrapped.size() && yy < y + h; i++) {
            g.drawString(this.font, wrapped.get(i), x, yy, 0xFFFFFF);
            yy += lineH;
        }

        // Scrollbar indicator (thin bar on right)
        if (chatTotalLines > maxLines) {
            int barW = 3;
            int trackX1 = x + w - barW;
            int trackX2 = x + w;
            int trackY1 = y;
            int trackY2 = y + h;
            g.fill(trackX1, trackY1, trackX2, trackY2, 0x40000000);
            double ratio = maxLines / (double) chatTotalLines;
            int thumbH = Math.max(8, (int) Math.round(h * ratio));
            int thumbMaxTravel = h - thumbH;
            double scrollRatio = chatScroll / (double) Math.max(1, maxScroll);
            int thumbY = trackY1 + (int) Math.round(thumbMaxTravel * scrollRatio);
            g.fill(trackX1, thumbY, trackX2, thumbY + thumbH, 0x80FFFFFF);
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
                    .sorted((a,b) -> Integer.compare(b.age, a.age))
                    .forEach(ev -> {
                        String place = (ev.place!=null && !ev.place.isEmpty() && !looksLikeCoordBucket(ev.place)) ? (" à " + ev.place) : "";
                        lines.add(Component.literal("§7" + ev.age + " ans§r – " + ev.type + place + (ev.details!=null?": "+ev.details:"")));
                    });
        }
        drawWrappedScrollable(g, lines, x, y, w, h, true);
    }

    private void renderHealth(GuiGraphics g, int x, int y, int w, int h) {
        var story = net.frealac.iamod.client.story.ClientStoryCache.get(villagerId);
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("§6Santé & Psychologie"));
        if (story == null) { lines.add(Component.literal("Aucune donnée.")); drawWrappedScrollable(g, lines, x, y, w, h, true); return; }
        if (story.health != null) {
            lines.add(Component.literal("§eSanté:"));
            // Allergies
            lines.add(Component.literal("Allergies: " + (story.health.allergies!=null && !story.health.allergies.isEmpty()? String.join(", ", story.health.allergies) : "aucune")));
            // Phobies
            if (story.health.phobias != null && !story.health.phobias.isEmpty()) {
                for (var p : story.health.phobias) {
                    lines.add(Component.literal("Phobie: " + p.type + " (" + String.format("%.2f", p.severity) + ")"));
                }
            } else {
                lines.add(Component.literal("Phobies: aucune"));
            }
            // Addictions
            if (story.health.addictions != null && !story.health.addictions.isEmpty()) {
                for (var a : story.health.addictions) {
                    lines.add(Component.literal("Addiction: " + a.type + " (" + String.format("%.2f", a.severity) + ")"));
                }
            }
            // Blessures détaillées
            if (story.health.wounds != null && !story.health.wounds.isEmpty()) {
                for (var wdn : story.health.wounds) {
                    String perm = wdn.permanent ? ", permanent" : "";
                    lines.add(Component.literal("Blessure: " + wdn.type + (wdn.date!=null?" ["+wdn.date+"]":"") + " (" + String.format("%.2f", wdn.severity) + ")" + perm));
                }
            } else {
                lines.add(Component.literal("Blessures: aucune"));
            }
            lines.add(Component.literal(String.format("Endurance: %.2f  Douleur: %.2f  Sommeil: %.2f", story.health.stamina, story.health.painTolerance, story.health.sleepQuality)));
        }
        if (story.psychology != null) {
            lines.add(Component.literal("§ePsychologie:"));
            lines.add(Component.literal(String.format("Humeur: %.2f Stress: %.2f Résilience: %.2f", story.psychology.moodBaseline, story.psychology.stress, story.psychology.resilience)));
            if (story.psychology.trauma != null && story.psychology.trauma.events != null && !story.psychology.trauma.events.isEmpty()) {
                lines.add(Component.literal("Traumas:"));
                for (var t : story.psychology.trauma.events) {
                    String desc = t.description!=null && !t.description.isEmpty()? (": "+t.description) : "";
                    lines.add(Component.literal("- " + t.ageAt + " ans: " + t.type + " (" + String.format("%.2f", t.severity) + ")" + desc));
                }
                if (story.psychology.trauma.coping != null && !story.psychology.trauma.coping.isEmpty()) {
                    lines.add(Component.literal("Coping: " + String.join(", ", story.psychology.trauma.coping)));
                }
            }
        }
        drawWrappedScrollable(g, lines, x, y, w, h, true);

    }

    private void renderRelations(GuiGraphics g, int x, int y, int w, int h) {
        var story = net.frealac.iamod.client.story.ClientStoryCache.get(villagerId);
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("§6Relations"));
        if (story == null) { lines.add(Component.literal("Aucune donnée.")); drawWrappedScrollable(g, lines, x, y, w, h, true); return; }
        if (story.relationsKnown != null && !story.relationsKnown.isEmpty()) {
            for (Object o : story.relationsKnown) {
                String name = mapGet(o, "name");
                String rel = mapGet(o, "relation");
                String opin = mapGet(o, "opinion");
                lines.add(Component.literal("- " + (name!=null?name:"?") + " (" + (rel!=null?rel:"?") + ") · opinion: " + (opin!=null?opin:"0"))));
            }
        } else {
            lines.add(Component.literal("Aucun lien connu."));
        }
        lines.add(Component.literal("§eÉconomie/Légal"));
        if (story.economy != null) {
            lines.add(Component.literal("Richesse: " + story.economy.wealthTier + " · Épargne: " + story.economy.savings));
            if (story.economy.possessions != null && !story.economy.possessions.isEmpty())
                lines.add(Component.literal("Biens: " + String.join(", ", story.economy.possessions)));
        }
        if (story.legal != null) {
            lines.add(Component.literal(String.format("Réputation: %d · Fiabilité: %.2f", story.legal.reputationVillage, story.legal.trustworthiness)));
        }
        if (story.villageNews != null && !story.villageNews.isEmpty()) {
            lines.add(Component.literal("§eNouvelles du village"));
            for (String n : story.villageNews) lines.add(Component.literal("- " + n));
        }
        drawWrappedScrollable(g, lines, x, y, w, h, true);
    }

    private String mapGet(Object o, String key) {
        try {
            java.util.Map<?,?> m = (java.util.Map<?,?>) o;
            Object v = m.get(key);
            return v != null ? String.valueOf(v) : null;
        } catch (Exception ignored) { return null; }
    }

    private void renderSummary(GuiGraphics g, int x, int y, int w, int h) {
        var story = net.frealac.iamod.client.story.ClientStoryCache.get(villagerId);
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("§6Résumé"));
        if (story == null) { lines.add(Component.literal("Chargement...")); drawWrapped(g, lines, x, y, w, h); return; }
        String meta = (story.ageYears>0? (story.ageYears + " ans · ") : "") +
                (story.profession!=null? story.profession+" · ":"") +
                (story.cultureId!=null? story.cultureId:"");
        if (!meta.isEmpty()) lines.add(Component.literal(meta));
        String bio = story.bioLong != null && !story.bioLong.isBlank()? story.bioLong : story.bioBrief;
        if (bio != null && !bio.isBlank()) lines.add(Component.literal(bio));
        lines.add(Component.literal("§eFamille"));
        if (story.parents != null && !story.parents.isEmpty()) lines.add(Component.literal("Parents: " + String.join(", ", story.parents)));
        if (story.siblings != null && !story.siblings.isEmpty()) lines.add(Component.literal("Fratrie: " + String.join(", ", story.siblings)));
        if (story.children != null && !story.children.isEmpty()) lines.add(Component.literal("Enfants: " + String.join(", ", story.children)));
        if (story.spouse != null && !story.spouse.isBlank()) lines.add(Component.literal("Conjoint: " + story.spouse));
        if (story.goals != null && ((story.goals.shortTerm!=null && !story.goals.shortTerm.isEmpty()) || (story.goals.longTerm!=null && !story.goals.longTerm.isEmpty()))) {
            lines.add(Component.literal("§eObjectifs"));
            if (story.goals.shortTerm != null && !story.goals.shortTerm.isEmpty())
                lines.add(Component.literal("Court terme: " + String.join(", ", story.goals.shortTerm)));
            if (story.goals.longTerm != null && !story.goals.longTerm.isEmpty())
                lines.add(Component.literal("Long terme: " + String.join(", ", story.goals.longTerm)));
            if (story.goals.blockers != null && !story.goals.blockers.isEmpty())
                lines.add(Component.literal("Freins: " + String.join(", ", story.goals.blockers)));
        }
        drawWrappedScrollable(g, lines, x, y, w, h, true);
    }

    // Packet callbacks
    public void onStorySynced() { this.isLoading = false; }
    public void showIntroFromStory(net.frealac.iamod.common.story.VillagerStory story) {
        if (introShown) return;
        if (story != null && story.bioBrief != null && !story.bioBrief.isEmpty()) {
            appendNpc(story.bioBrief);
            introShown = true;
        }
    }
    // No-op (legacy hook removed for IA refinement)

    private void renderDebugOverlay(GuiGraphics g, int x, int y, int w, int h) {
        // Opaque background to avoid underlay bleed
        g.fill(x, y, x + w, y + h, 0xF0000000);
        // Border
        int border = 0xFF8B7355;
        g.fill(x-1, y-1, x + w + 1, y, border);
        g.fill(x-1, y + h, x + w + 1, y + h + 1, border);
        g.fill(x-1, y, x, y + h, border);
        g.fill(x + w, y, x + w + 1, y + h, border);
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
        int lineH = 12;
        int maxLines = Math.max(1, h / lineH);
        int start = 0;
        int yy = y;
        for (int i = start; i < wrapped.size() && yy < y + h; i++) {
            g.drawString(this.font, wrapped.get(i), x, yy, 0xFFFFFF);
            yy += lineH;
        }
    }

    private void drawWrappedScrollable(GuiGraphics g, List<Component> lines, int x, int y, int w, int h, boolean drawScrollbar) {
        List<FormattedCharSequence> wrapped = new ArrayList<>();
        for (Component c : lines) {
            wrapped.addAll(this.font.split(c, w));
        }
        int lineH = 12;
        int maxLines = Math.max(1, h / lineH);
        contentTotalLines = wrapped.size();
        contentMaxLines = maxLines;
        int maxScroll = Math.max(0, contentTotalLines - maxLines);
        if (contentScroll > maxScroll) contentScroll = maxScroll;
        int start = Math.max(0, contentTotalLines - maxLines - contentScroll);
        int yy = y;
        for (int i = start; i < wrapped.size() && yy < y + h; i++) {
            g.drawString(this.font, wrapped.get(i), x, yy, 0xFFFFFF);
            yy += lineH;
        }
        if (drawScrollbar && contentTotalLines > maxLines) {
            int barW = 3;
            int trackX1 = x + w - barW;
            int trackX2 = x + w;
            int trackY1 = y;
            int trackY2 = y + h;
            g.fill(trackX1, trackY1, trackX2, trackY2, 0x40000000);
            double ratio = maxLines / (double) contentTotalLines;
            int thumbH = Math.max(8, (int) Math.round(h * ratio));
            int thumbMaxTravel = h - thumbH;
            int maxSc = Math.max(1, maxScroll);
            double scrollRatio = contentScroll / (double) maxSc;
            int thumbY = trackY1 + (int) Math.round(thumbMaxTravel * scrollRatio);
            g.fill(trackX1, thumbY, trackX2, thumbY + thumbH, 0x80FFFFFF);
        }
    }

    private boolean looksLikeCoordBucket(String s) {
        return s != null && s.matches("C-?\\d+xC-?\\d+");
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta, double horizontalDelta) {
        if (mode == Mode.CHAT) {
            // Compute chat bounds to scroll only when cursor over it
            int dialogY = this.height - DIALOG_HEIGHT - MARGIN_BOTTOM;
            int dialogX = MARGIN_SIDE;
            int dialogW = this.width - (MARGIN_SIDE * 2);
            int portraitX = dialogX + PADDING;
            int portraitY = dialogY + PADDING;
            int textX = portraitX + PORTRAIT_SIZE + PADDING * 2;
            int textY = portraitY + 14; // below name
            int textW = dialogW - PORTRAIT_SIZE - PADDING * 4;
            int textH = (DIALOG_HEIGHT - INPUT_HEIGHT - PADDING * 3) - 14;

            if (mouseX >= textX && mouseX <= textX + textW && mouseY >= textY && mouseY <= textY + textH) {
                int lineH = 10;
                int maxLines = Math.max(1, textH / lineH);
                int maxScroll = Math.max(0, chatTotalLines - maxLines);
                int dir = delta > 0 ? 1 : -1; // up = positive
                chatScroll = Math.max(0, Math.min(maxScroll, chatScroll + dir * 3));
                return true;
            }
        } else {
            // Scroll non-chat panels
            int dialogY = this.height - DIALOG_HEIGHT - MARGIN_BOTTOM;
            int dialogX = MARGIN_SIDE;
            int dialogW = this.width - (MARGIN_SIDE * 2);
            int portraitX = dialogX + PADDING;
            int portraitY = dialogY + PADDING;
            int textX = portraitX + PORTRAIT_SIZE + PADDING * 2;
            int textY = portraitY + 14; // below name
            int textW = dialogW - PORTRAIT_SIZE - PADDING * 4;
            int textH = (DIALOG_HEIGHT - INPUT_HEIGHT - PADDING * 3) - 14;
            if (mouseX >= textX && mouseX <= textX + textW && mouseY >= textY && mouseY <= textY + textH) {
                int maxScroll = Math.max(0, contentTotalLines - Math.max(1, textH / 12));
                int dir = delta > 0 ? 1 : -1;
                contentScroll = Math.max(0, Math.min(maxScroll, contentScroll + dir * 3));
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta, horizontalDelta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int dialogY = this.height - DIALOG_HEIGHT - MARGIN_BOTTOM;
            int dialogX = MARGIN_SIDE;
            int dialogW = this.width - (MARGIN_SIDE * 2);
            int portraitX = dialogX + PADDING;
            int portraitY = dialogY + PADDING;
            int textX = portraitX + PORTRAIT_SIZE + PADDING * 2;
            int textY = portraitY + 14;
            int textW = dialogW - PORTRAIT_SIZE - PADDING * 4;
            int textH = (DIALOG_HEIGHT - INPUT_HEIGHT - PADDING * 3) - 14;
            int barW = 3;
            int trackX1 = textX + textW - barW;
            int trackX2 = textX + textW;
            int trackY1 = textY;
            int trackY2 = textY + textH;
            if (mouseX >= trackX1 && mouseX <= trackX2 && mouseY >= trackY1 && mouseY <= trackY2) {
                dragStartY = (int) mouseY;
                if (mode == Mode.CHAT) { draggingChatScrollbar = true; dragStartScroll = chatScroll; }
                else { draggingContentScrollbar = true; dragStartScroll = contentScroll; }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (button == 0 && (draggingChatScrollbar || draggingContentScrollbar)) {
            int dialogY = this.height - DIALOG_HEIGHT - MARGIN_BOTTOM;
            int dialogX = MARGIN_SIDE;
            int dialogW = this.width - (MARGIN_SIDE * 2);
            int portraitX = dialogX + PADDING;
            int portraitY = dialogY + PADDING;
            int textX = portraitX + PORTRAIT_SIZE + PADDING * 2;
            int textY = portraitY + 14;
            int textW = dialogW - PORTRAIT_SIZE - PADDING * 4;
            int textH = (DIALOG_HEIGHT - INPUT_HEIGHT - PADDING * 3) - 14;
            int h = textH;
            int lineH = (mode == Mode.CHAT) ? 10 : 12;
            int maxLines = Math.max(1, h / lineH);
            int total = (mode == Mode.CHAT) ? chatTotalLines : contentTotalLines;
            int maxScroll = Math.max(0, total - maxLines);
            int thumbH = Math.max(8, (int) Math.round(h * (maxLines / (double) Math.max(1, total))));
            int thumbMaxTravel = h - thumbH;
            // Map mouseY to scroll
            double pos = Math.min(Math.max(textY, mouseY - thumbH / 2.0), textY + thumbMaxTravel);
            double ratio = (pos - textY) / Math.max(1.0, thumbMaxTravel);
            int newScroll = (int) Math.round(maxScroll * ratio);
            if (mode == Mode.CHAT) chatScroll = newScroll; else contentScroll = newScroll;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) { draggingChatScrollbar = false; draggingContentScrollbar = false; }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    // Streaming API for AI replies
    public void beginAiStream() {
        if (streamingBuffer == null) {
            streamingBuffer = new StringBuilder();
            history.add(Entry.npc(""));
            chatScroll = 0;
        }
    }
    public void appendAiStream(String chunk) {
        if (streamingBuffer == null) beginAiStream();
        streamingBuffer.append(chunk);
        // update last entry text
        if (!history.isEmpty()) {
            history.get(history.size()-1).text = streamingBuffer.toString();
        }
    }
    public void endAiStream() { streamingBuffer = null; }

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

    public void appendNpc(String text) { history.add(Entry.npc(text)); chatScroll = 0; }
    private void appendPlayer(String text) { history.add(Entry.player(text)); chatScroll = 0; }

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
        final boolean isNpc; String text;
        private Entry(boolean isNpc, String text) { this.isNpc = isNpc; this.text = text; }
        static Entry npc(String t) { return new Entry(true, t); }
        static Entry player(String t) { return new Entry(false, t); }
    }
}
