package net.frealac.iamod.network;

import net.frealac.iamod.IAMOD;
import net.frealac.iamod.ai.OpenAiService;
import net.frealac.iamod.client.screen.VillagerDialogScreen;
import net.frealac.iamod.client.story.ClientStoryCache;
import net.frealac.iamod.common.story.VillagerStory;
import net.frealac.iamod.event.VillagerInteractHandler;
import net.frealac.iamod.network.packet.AiReplyS2CPacket;
import net.frealac.iamod.network.packet.AiReplyStreamChunkS2CPacket;
import net.frealac.iamod.network.packet.CloseDialogC2SPacket;
import net.frealac.iamod.network.packet.OpenDialogS2CPacket;
import net.frealac.iamod.network.packet.PlayerMessageC2SPacket;
import net.frealac.iamod.network.packet.SyncVillagerStoryS2CPacket;
import net.frealac.iamod.network.packet.OpenAIConfigS2CPacket;
import net.frealac.iamod.network.packet.UpdateAIConfigC2SPacket;
import net.frealac.iamod.network.packet.SyncVillagerDebugS2CPacket;
import net.frealac.iamod.client.hud.VillagerDebugHud;
import net.frealac.iamod.server.ConversationManager;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

import java.util.concurrent.CompletableFuture;

public class NetworkHandler {
    private static final int PROTOCOL_VERSION = 1;
    public static final SimpleChannel CHANNEL = ChannelBuilder
            .named(ResourceLocation.fromNamespaceAndPath(IAMOD.MOD_ID, "main"))
            .networkProtocolVersion(PROTOCOL_VERSION)
            .simpleChannel();

    private static int id = 0;

    public static void register() {
        CHANNEL.messageBuilder(OpenDialogS2CPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenDialogS2CPacket::encode)
                .decoder(OpenDialogS2CPacket::decode)
                .consumerMainThread((msg, ctx) -> {
                    Minecraft mc = Minecraft.getInstance();
                    mc.setScreen(new VillagerDialogScreen(msg.getVillagerId(), msg.getGreeting()));
                })
                .add();

        CHANNEL.messageBuilder(SyncVillagerStoryS2CPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncVillagerStoryS2CPacket::encode)
                .decoder(SyncVillagerStoryS2CPacket::decode)
                .consumerMainThread((msg, ctx) -> {
                    var story = msg.getStory();
                    ClientStoryCache.put(msg.getVillagerId(), story);
                    // If dialog open, mark as loaded and append intro once
                    var mc = Minecraft.getInstance();
                    if (mc.screen instanceof VillagerDialogScreen s && s.getVillagerId() == msg.getVillagerId()) {
                        s.onStorySynced();
                        s.showIntroFromStory(story);
                    }
                })
                .add();

        CHANNEL.messageBuilder(PlayerMessageC2SPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(PlayerMessageC2SPacket::encode)
                .decoder(PlayerMessageC2SPacket::decode)
                .consumerMainThread((msg, ctx) -> {
                    ServerPlayer sender = ctx.getSender();
                    if (sender == null) return;
                    var key = ConversationManager.key(sender.getUUID(), msg.getVillagerId());

                    // Build an in-character system prompt from villager story (server-authoritative)
                    var level = sender.serverLevel();
                    var ent = level.getEntity(msg.getVillagerId());
                    String systemPromptDefault = "Tu es un villageois amical. Réponds en français, immersif, concis.";
                    String systemPromptFinal = systemPromptDefault;
                    if (ent instanceof net.minecraft.world.entity.npc.Villager v) {
                        var cap = v.getCapability(net.frealac.iamod.common.story.VillagerStoryProvider.CAPABILITY).orElse(null);
                        if (cap != null) {
                            var s = cap.getStory();
                            if (s != null) systemPromptFinal = OpenAiService.buildSystemPromptFromStory(s);
                        }
                    }
                    ConversationManager.ensureSystem(key, systemPromptFinal);
                    var history = ConversationManager.appendUserAndGetHistory(key, msg.getMessage());
                    final int idVillager = msg.getVillagerId();

                    // Track AI activity start (NEW)
                    net.frealac.iamod.server.AIActivityTracker.startAiProcessing(idVillager);

                    // PROCESS MESSAGE THROUGH BRAIN MODULES (sentiment analysis + brain signals)
                    if (ent instanceof net.minecraft.world.entity.npc.Villager villager) {
                        try {
                            // Get villager's unique story (personality, psychology, health)
                            var cap = villager.getCapability(net.frealac.iamod.common.story.VillagerStoryProvider.CAPABILITY).orElse(null);
                            if (cap != null) {
                                var story = cap.getStory();

                                // 1. Get brain system for this villager FIRST (need mood for analysis)
                                net.frealac.iamod.ai.openai.OpenAiBrainService brainService = new net.frealac.iamod.ai.openai.OpenAiBrainService();
                                net.frealac.iamod.ai.brain.VillagerBrainSystem brainSystem =
                                    brainService.getBrainSystem(idVillager);

                                // 2. Get current emotional state for MOOD-CONGRUENT PROCESSING
                                double currentMood = 0.0;
                                double currentStress = 0.3;
                                if (brainSystem != null) {
                                    currentMood = brainSystem.getEmotionalBrain().getCurrentMood();
                                    currentStress = brainSystem.getEmotionalBrain().getCurrentStress();
                                }

                                // 3. ANALYZE MESSAGE with AI including mood-congruent bias
                                net.frealac.iamod.ai.brain.MessageAnalyzer.MessageImpact impact =
                                    net.frealac.iamod.ai.brain.MessageAnalyzer.analyzeMessage(
                                        msg.getMessage(), currentMood, currentStress);

                                net.frealac.iamod.IAMOD.LOGGER.info("💬 GUI Message impact: sentiment={} (mood={}, stress={}) ({})",
                                    impact.overallSentiment, currentMood, currentStress, impact.getDescription());

                                // 4. SEND SIGNALS to brain modules based on message impact
                                if (brainSystem != null) {
                                    net.frealac.iamod.ai.brain.MessageAnalyzer.sendBrainSignals(
                                        impact, brainSystem, sender.getUUID());
                                }

                                // 5. Add interaction memory based on MESSAGE IMPACT
                                if (story.interactionMemory == null) {
                                    story.interactionMemory = new net.frealac.iamod.ai.memory.VillagerMemory();
                                }

                                // Create memory based on sentiment
                                net.frealac.iamod.ai.memory.MemoryType memoryType;
                                String description;
                                double emotionalImpact;

                                if (impact.affectionImpact > 0.3 || impact.positiveImpact > 0.3) {
                                    memoryType = net.frealac.iamod.ai.memory.MemoryType.PLEASANT_CONVERSATION;
                                    description = String.format("M'a dit via GUI: '%s' - C'était agréable",
                                        msg.getMessage().substring(0, Math.min(50, msg.getMessage().length())));
                                    emotionalImpact = Math.max(impact.positiveImpact, impact.affectionImpact);
                                    net.frealac.iamod.IAMOD.LOGGER.info("💚 Creating POSITIVE GUI memory (impact={})", emotionalImpact);
                                } else if (impact.aggressionImpact > 0.3 || impact.negativeImpact > 0.3) {
                                    memoryType = net.frealac.iamod.ai.memory.MemoryType.WAS_INSULTED;
                                    description = String.format("M'a dit via GUI: '%s' - C'était désagréable",
                                        msg.getMessage().substring(0, Math.min(50, msg.getMessage().length())));
                                    emotionalImpact = -Math.max(impact.negativeImpact, impact.aggressionImpact);
                                    net.frealac.iamod.IAMOD.LOGGER.info("💔 Creating NEGATIVE GUI memory (impact={})", emotionalImpact);
                                } else {
                                    memoryType = net.frealac.iamod.ai.memory.MemoryType.GENERAL_INTERACTION;
                                    description = String.format("A dit via GUI: '%s'",
                                        msg.getMessage().substring(0, Math.min(50, msg.getMessage().length())));
                                    emotionalImpact = impact.overallSentiment * 0.5;
                                    net.frealac.iamod.IAMOD.LOGGER.info("💬 Creating NEUTRAL GUI memory (impact={})", emotionalImpact);
                                }

                                // Create memory with AI-analyzed emotional impact
                                net.frealac.iamod.ai.memory.Memory memory = new net.frealac.iamod.ai.memory.Memory(
                                    memoryType, description, sender.getUUID(), sender.getName().getString());
                                memory.setEmotionalImpact(emotionalImpact);
                                story.interactionMemory.addMemory(memory);

                                net.frealac.iamod.IAMOD.LOGGER.info("✓ GUI Memory created, brain modules updated");
                            }
                        } catch (Exception e) {
                            net.frealac.iamod.IAMOD.LOGGER.error("Failed to process GUI message through brain modules", e);
                        }
                    }

                    OpenAiService service = new OpenAiService();
                    service.chatStreamSSE(
                            history,
                            () -> sender.getServer().execute(() -> CHANNEL.send(new AiReplyStreamChunkS2CPacket(idVillager, "", true, false), PacketDistributor.PLAYER.with(sender))),
                            chunk -> sender.getServer().execute(() -> CHANNEL.send(new AiReplyStreamChunkS2CPacket(idVillager, chunk, false, false), PacketDistributor.PLAYER.with(sender))),
                            () -> sender.getServer().execute(() -> CHANNEL.send(new AiReplyStreamChunkS2CPacket(idVillager, "", false, true), PacketDistributor.PLAYER.with(sender)))
                    ).handle((full, ex) -> {
                        sender.getServer().execute(() -> {
                            String reply = (ex == null) ? full : ("Erreur IA: " + ex.getMessage());
                            ConversationManager.appendAssistant(key, reply);

                            // Track AI activity finish (NEW)
                            net.frealac.iamod.server.AIActivityTracker.finishAiProcessing(idVillager, ex == null);
                        });
                        return null;
                    });
                })
                .add();

        CHANNEL.messageBuilder(AiReplyS2CPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(AiReplyS2CPacket::encode)
                .decoder(AiReplyS2CPacket::decode)
                .consumerMainThread((msg, ctx) -> {
                    var mc = Minecraft.getInstance();
                    if (mc.screen instanceof VillagerDialogScreen s && s.getVillagerId() == msg.getVillagerId()) {
                        s.appendNpc(msg.getReply());
                    }
                })
                .add();

        CHANNEL.messageBuilder(AiReplyStreamChunkS2CPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(AiReplyStreamChunkS2CPacket::encode)
                .decoder(AiReplyStreamChunkS2CPacket::decode)
                .consumerMainThread((msg, ctx) -> {
                    var mc = Minecraft.getInstance();
                    if (mc.screen instanceof VillagerDialogScreen s && s.getVillagerId() == msg.getVillagerId()) {
                        if (msg.isStart()) s.beginAiStream();
                        String c = msg.getChunk();
                        if (c != null && !c.isEmpty()) s.appendAiStream(c);
                        if (msg.isDone()) s.endAiStream();
                    }
                })
                .add();

        CHANNEL.messageBuilder(CloseDialogC2SPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(CloseDialogC2SPacket::encode)
                .decoder(CloseDialogC2SPacket::decode)
                .consumerMainThread((msg, ctx) -> {
                    VillagerInteractHandler.endConversation(msg.getVillagerId());
                })
                .add();

        // AI Configuration packets
        CHANNEL.messageBuilder(OpenAIConfigS2CPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenAIConfigS2CPacket::encode)
                .decoder(OpenAIConfigS2CPacket::new)
                .consumerMainThread((msg, ctx) -> {
                    Minecraft.getInstance().setScreen(new net.frealac.iamod.client.screen.AIConfigScreen(msg.getEntityId()));
                })
                .add();

        CHANNEL.messageBuilder(UpdateAIConfigC2SPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(UpdateAIConfigC2SPacket::encode)
                .decoder(UpdateAIConfigC2SPacket::new)
                .consumerMainThread((msg, ctx) -> {
                    ServerPlayer player = ctx.getSender();
                    if (player == null) return;

                    var entity = player.level().getEntity(msg.getEntityId());
                    if (!(entity instanceof net.minecraft.world.entity.Mob mob)) return;

                    // Update AI data capability
                    mob.getCapability(net.frealac.iamod.ai.data.AIDataProvider.CAPABILITY).ifPresent(aiData -> {
                        aiData.getData().setGoalEnabled(msg.getGoalName(), msg.isEnabled());
                    });
                })
                .add();

        // Villager Debug HUD sync packet
        CHANNEL.messageBuilder(SyncVillagerDebugS2CPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncVillagerDebugS2CPacket::encode)
                .decoder(SyncVillagerDebugS2CPacket::decode)
                .consumerMainThread((msg, ctx) -> {
                    if (msg.hasVillager()) {
                        VillagerDebugHud.setDebugInfo(msg.getDebugInfo());
                    } else {
                        VillagerDebugHud.clearDebugInfo();
                    }
                })
                .add();
    }

    // simulated splitter removed (true SSE in use)
}
