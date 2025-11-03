package net.frealac.iamod.network;

import net.frealac.iamod.IAMOD;
import net.frealac.iamod.ai.OpenAiService;
import net.frealac.iamod.client.screen.VillagerDialogScreen;
import net.frealac.iamod.client.story.ClientStoryCache;
import net.frealac.iamod.common.story.VillagerStory;
import net.frealac.iamod.event.VillagerInteractHandler;
import net.frealac.iamod.network.packet.AiReplyS2CPacket;
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
                    // If dialog open, append the bioBrief line for quick visibility (Phase 1 UX)
                    var mc = Minecraft.getInstance();
                    if (mc.screen instanceof VillagerDialogScreen s && s.getVillagerId() == msg.getVillagerId()) {
                        if (story != null && story.bioBrief != null && !story.bioBrief.isEmpty()) {
                            s.appendNpc(story.bioBrief);
                        }
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
                    var history = ConversationManager.appendUserAndGetHistory(key, msg.getMessage());
                    CompletableFuture
                            .supplyAsync(() -> {
                                try {
                                    return new OpenAiService().chat(history);
                                } catch (Exception e) {
                                    return "Erreur IA: " + e.getMessage();
                                }
                            })
                            .thenAccept(reply -> sender.getServer().execute(() -> {
                                ConversationManager.appendAssistant(key, reply);
                                CHANNEL.send(new AiReplyS2CPacket(msg.getVillagerId(), reply),
                                        PacketDistributor.PLAYER.with(sender));
                            }));
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

        CHANNEL.messageBuilder(CloseDialogC2SPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(CloseDialogC2SPacket::encode)
                .decoder(CloseDialogC2SPacket::decode)
                .consumerMainThread((msg, ctx) -> {
                    VillagerInteractHandler.endConversation(msg.getVillagerId());
                })
                .add();
    }
}
