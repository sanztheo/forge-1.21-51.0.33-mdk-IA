package net.frealac.iamod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.frealac.iamod.IAMOD;
import net.frealac.iamod.ai.OpenAiService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.concurrent.CompletableFuture;

public class AiCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("ai")
                .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(AiCommand::execute));

        dispatcher.register(root);
    }

    private static int execute(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String msg = StringArgumentType.getString(ctx, "message");
        ServerPlayer player = source.getPlayer();
        String who = player != null ? player.getName().getString() : source.getTextName();

        source.sendSuccess(() -> Component.literal("[IA] Je réfléchis…"), false);

        OpenAiService service = new OpenAiService();
        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return service.ask(msg, who);
                    } catch (Exception e) {
                        return "Erreur: " + e.getMessage();
                    }
                })
                .thenAccept(reply -> source.getServer().execute(() ->
                        source.sendSuccess(() -> Component.literal("[IA] " + reply), false)
                ));

        return 1;
    }
}

