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
    }

    // simulated splitter removed (true SSE in use)
}
