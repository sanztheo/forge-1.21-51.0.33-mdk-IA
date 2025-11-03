package net.frealac.iamod.event;

import net.frealac.iamod.IAMOD;
import net.frealac.iamod.network.NetworkHandler;
import net.frealac.iamod.network.packet.OpenDialogS2CPacket;
import net.frealac.iamod.network.packet.SyncVillagerStoryS2CPacket;
import net.frealac.iamod.common.story.StoryGenerator;
import net.frealac.iamod.common.story.IVillagerStory;
import net.frealac.iamod.common.story.VillagerStory;
import net.frealac.iamod.common.story.VillagerStoryProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = IAMOD.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class VillagerInteractHandler {
    // Map pour suivre les villageois en conversation : ID du villageois -> UUID du joueur
    private static final Map<Integer, UUID> activeConversations = new HashMap<>();
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Villager villager)) return;
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) return;
        event.setCanceled(true); // empêche l'écran de trade vanilla

        // Ensure deterministic story exists server-side and sync minimal to client
        var story = ensureStoryAndSync(villager, sp);

        // Enregistrer la conversation active
        activeConversations.put(villager.getId(), sp.getUUID());
        String name = (story!=null && story.nameGiven!=null? story.nameGiven: "Villageois") +
                (story!=null && story.nameFamily!=null? " " + story.nameFamily : "");
        String greeting = "Bonjour " + sp.getName().getString() + ", je suis " + name + ".";
        NetworkHandler.CHANNEL.send(new OpenDialogS2CPacket(villager.getId(), greeting),
                PacketDistributor.PLAYER.with(sp));
    }

    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (!(event.getTarget() instanceof Villager villager)) return;
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) return;
        event.setCanceled(true);

        // Ensure deterministic story exists server-side and sync minimal to client
        var story = ensureStoryAndSync(villager, sp);

        // Enregistrer la conversation active
        activeConversations.put(villager.getId(), sp.getUUID());
        
        String name = (story!=null && story.nameGiven!=null? story.nameGiven: "Villageois") +
                (story!=null && story.nameFamily!=null? " " + story.nameFamily : "");
        String greeting = "Bonjour " + sp.getName().getString() + ", je suis " + name + ".";
        NetworkHandler.CHANNEL.send(new OpenDialogS2CPacket(villager.getId(), greeting),
                PacketDistributor.PLAYER.with(sp));
    }
    
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        // Parcourir toutes les conversations actives
        activeConversations.entrySet().removeIf(entry -> {
            int villagerId = entry.getKey();
            UUID playerUUID = entry.getValue();
            
            // Trouver le villageois dans le monde
            var level = event.getServer().getAllLevels().iterator().next();
            var villager = level.getEntity(villagerId);
            
            if (!(villager instanceof Villager v)) return true; // Retirer si introuvable
            
            var player = level.getPlayerByUUID(playerUUID);
            if (player == null) return true; // Retirer si joueur absent
            
            // Vérifier si le joueur a fermé l'écran (trop loin)
            if (v.distanceToSqr(player) > 64) return true; // Retirer si trop loin
            
            // Bloquer le mouvement du villageois
            v.setDeltaMovement(Vec3.ZERO);
            v.setYRot(v.getYRot()); // Évite les rotations involontaires
            
            // Faire regarder le joueur
            Vec3 lookVec = player.position().subtract(v.position());
            double yaw = Math.toDegrees(Math.atan2(lookVec.z, lookVec.x)) - 90;
            double pitch = -Math.toDegrees(Math.atan2(lookVec.y, Math.sqrt(lookVec.x * lookVec.x + lookVec.z * lookVec.z)));
            
            v.setYRot((float) yaw);
            v.setXRot((float) pitch);
            v.yHeadRot = (float) yaw;
            v.yBodyRot = (float) yaw;
            
            return false; // Garder dans la map
        });
    }
    
    public static void endConversation(int villagerId) {
        activeConversations.remove(villagerId);
    }

    private static VillagerStory ensureStoryAndSync(Villager villager, ServerPlayer sp) {
        var level = (net.minecraft.server.level.ServerLevel) villager.level();

        // Capability must exist; story is server-authoritative and persisted on the entity NBT
        final VillagerStory[] out = new VillagerStory[1];
        villager.getCapability(VillagerStoryProvider.CAPABILITY).ifPresent(cap -> {
            VillagerStory story = cap.getStory();
            if (story == null) {
                story = StoryGenerator.generate(level, villager);
                cap.setStory(story);
            }
            out[0] = story;
            NetworkHandler.CHANNEL.send(new SyncVillagerStoryS2CPacket(villager.getId(), story.toJson()),
                    PacketDistributor.PLAYER.with(sp));
        });
        return out[0];
    }
}
