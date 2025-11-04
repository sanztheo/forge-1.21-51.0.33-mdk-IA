package net.frealac.iamod.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.frealac.iamod.IAMOD;
import net.frealac.iamod.ai.behavior.BehaviorManager;
import net.frealac.iamod.event.AITickHandler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Commands for managing and testing the AI system.
 */
@Mod.EventBusSubscriber(modid = IAMOD.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AICommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("aitest")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("mode")
                .then(Commands.argument("mode", StringArgumentType.string())
                    .suggests((context, builder) -> {
                        builder.suggest("goals");
                        builder.suggest("behaviortree");
                        return builder.buildFuture();
                    })
                    .executes(AICommands::setAIMode)
                )
            )
            .then(Commands.literal("info")
                .executes(AICommands::showAIInfo)
            )
            .then(Commands.literal("count")
                .executes(AICommands::showAICount)
            )
        );
    }

    private static int setAIMode(CommandContext<CommandSourceStack> context) {
        String mode = StringArgumentType.getString(context, "mode");
        CommandSourceStack source = context.getSource();

        try {
            // Get nearby mobs
            Vec3 pos = source.getPosition();
            List<Mob> nearbyMobs = source.getLevel().getEntitiesOfClass(
                Mob.class,
                new AABB(pos.x - 10, pos.y - 10, pos.z - 10, pos.x + 10, pos.y + 10, pos.z + 10)
            );

            if (nearbyMobs.isEmpty()) {
                source.sendFailure(Component.literal("No mobs found nearby (10 block radius)"));
                return 0;
            }

            BehaviorManager.BehaviorMode newMode;
            if (mode.equalsIgnoreCase("goals")) {
                newMode = BehaviorManager.BehaviorMode.GOALS;
            } else if (mode.equalsIgnoreCase("behaviortree")) {
                newMode = BehaviorManager.BehaviorMode.BEHAVIOR_TREE;
            } else {
                source.sendFailure(Component.literal("Unknown mode: " + mode + ". Use 'goals' or 'behaviortree'"));
                return 0;
            }

            int count = 0;
            for (Mob mob : nearbyMobs) {
                BehaviorManager manager = AITickHandler.getBehaviorManager(mob);
                if (manager != null) {
                    manager.setMode(newMode);
                    count++;
                }
            }

            source.sendSuccess(
                () -> Component.literal("Set AI mode to " + mode + " for " + count + " entities"),
                true
            );
            return Command.SINGLE_SUCCESS;

        } catch (Exception e) {
            source.sendFailure(Component.literal("Error: " + e.getMessage()));
            IAMOD.LOGGER.error("Error setting AI mode", e);
            return 0;
        }
    }

    private static int showAIInfo(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        try {
            // Get nearest mob
            Vec3 pos = source.getPosition();
            List<Mob> nearbyMobs = source.getLevel().getEntitiesOfClass(
                Mob.class,
                new AABB(pos.x - 5, pos.y - 5, pos.z - 5, pos.x + 5, pos.y + 5, pos.z + 5)
            );

            if (nearbyMobs.isEmpty()) {
                source.sendFailure(Component.literal("No mobs found nearby (5 block radius)"));
                return 0;
            }

            Mob nearestMob = nearbyMobs.get(0);
            BehaviorManager manager = AITickHandler.getBehaviorManager(nearestMob);

            if (manager == null) {
                source.sendFailure(Component.literal("Entity " + nearestMob.getName().getString() + " has no AI manager"));
                return 0;
            }

            source.sendSuccess(
                () -> Component.literal("§a=== AI Info ===\n" +
                    "§7Entity: §f" + nearestMob.getName().getString() + "\n" +
                    "§7Mode: §f" + manager.getMode().name() + "\n" +
                    "§7Position: §f" + nearestMob.blockPosition().toShortString()
                ),
                false
            );

            // Show active goals if in GOALS mode
            if (manager.getMode() == BehaviorManager.BehaviorMode.GOALS && manager.getGoalManager() != null) {
                var activeGoals = manager.getGoalManager().getActiveGoals();
                if (!activeGoals.isEmpty()) {
                    source.sendSuccess(
                        () -> Component.literal("§7Active Goals: §f" +
                            activeGoals.stream()
                                .map(g -> g.getDescription())
                                .reduce((a, b) -> a + ", " + b)
                                .orElse("None")
                        ),
                        false
                    );
                }
            }

            return Command.SINGLE_SUCCESS;

        } catch (Exception e) {
            source.sendFailure(Component.literal("Error: " + e.getMessage()));
            IAMOD.LOGGER.error("Error showing AI info", e);
            return 0;
        }
    }

    private static int showAICount(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        int count = AITickHandler.getRegisteredCount();

        source.sendSuccess(
            () -> Component.literal("§aCurrently managing §f" + count + "§a AI entities"),
            false
        );

        return Command.SINGLE_SUCCESS;
    }
}
