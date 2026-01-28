package com.mybot.commands;

import com.mybot.handler.ButtonHandler;
import com.mybot.handler.SlashCommandHandler;
import com.mybot.service.JsonStorageService;
import com.mybot.service.NHentaiService;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class MyBot extends ListenerAdapter {

    private final JsonStorageService storageService = new JsonStorageService();
    private final NHentaiService nhService = new NHentaiService();

    private final SlashCommandHandler slashCommandHandler = new SlashCommandHandler(storageService, nhService);
    private final ButtonHandler buttonHandler = new ButtonHandler(storageService);

    public static void main(String[] args) throws InterruptedException {
        Dotenv dotenv = Dotenv.load();
        String token = dotenv.get("DISCORD_TOKEN");

        JDA jda = JDABuilder.createDefault(token)
                .setActivity(Activity.playing("slash commands"))
                .addEventListeners(new MyBot())
                .build();

        jda.awaitReady();

        jda.updateCommands().addCommands(
                Commands.slash("ping", "Check bot delay"),
                Commands.slash("search-by-number", "Look up by ID")
                        .addOption(OptionType.STRING, "number", "The nhentai ID", true),
                Commands.slash("search-by-keyword", "Search by keyword")
                        .addOption(OptionType.STRING, "keyword", "Keywords", true)
                        .addOption(OptionType.INTEGER, "min-pages",
                                "(Optional) Minimum pages (e.g. 20 means >= 20 pages)",
                                false)
                        .addOptions(new OptionData(OptionType.STRING, "sort-by",
                                "Sort order (Optional,All Time Popular as default)", false)
                                .addChoice("All Time Popular", "popular")
                                .addChoice("Today's Popular", "popular-today")
                                .addChoice("Week's Popular", "popular-week")
                                .addChoice("Recent", "recent")),
                Commands.slash("recent-uploads", "Get a random book from newest uploads"),
                Commands.slash("block-tag", "Block a specific tag(one tag per time)")
                        .addOption(OptionType.STRING, "tag", "Tag name (e.g., yaoi)", true),
                Commands.slash("unblock-tag", "Unblock a specific tag")
                        .addOption(OptionType.STRING, "tag", "Tag name", true),
                Commands.slash("save-list", "View your saved collection")).queue();

        System.out.println("✅ Bot started successfully!");
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        slashCommandHandler.handle(event);
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        buttonHandler.handle(event);
    }
}