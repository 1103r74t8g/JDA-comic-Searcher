package com.mybot.commands;

import java.awt.Color;
import java.awt.List;
import java.util.stream.Collectors;

import com.mybot.model.Book;
import com.mybot.service.JsonStorageService;
import com.mybot.service.NHentaiService;
import com.mybot.model.UserData;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

public class MyBot extends ListenerAdapter {
    private final JsonStorageService storageService = new JsonStorageService();
    private final NHentaiService nhService = new NHentaiService();

    public static void main(String[] args) throws InterruptedException {
        Dotenv dotenv = Dotenv.load();
        String token = dotenv.get("DISCORD_TOKEN");

        JDA jda = JDABuilder.createDefault(token)
                .setActivity(Activity.playing("slash commands"))

                .addEventListeners(new MyBot())
                .build();

        jda.awaitReady();

        // add slash commands here
        jda.updateCommands().addCommands(
                Commands.slash("ping", "delay"), // name, description
                Commands.slash("hello", "say hello"),
                Commands.slash("search-by-number", "look up a specific one by number")
                        .addOption(OptionType.STRING, "number", "the number of the nhentai", true),
                Commands.slash("search-by-keyword", "look up by keyword with sorting")
                        .addOption(OptionType.STRING, "keyword", "the keyword to search", true)
                        .addOptions(
                                new OptionData(OptionType.STRING, "sort-by", "select time range", true)
                                        .addChoice("All Time Popular", "popular")
                                        .addChoice("Today's Popular", "popular-today")
                                        .addChoice(
                                                "Week's Popular", "popular-week")
                                        .addChoice("Most Recent", "recent")),
                Commands.slash("random", "get a random one"),
                Commands.slash("save-list", "view your saved list")).queue();

        System.out.println("successfully started bot");
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "ping":
                ping(event);
                break;
            case "hello":
                hello(event);
                break;
            case "search-by-number":
                searchByNumber(event);
                break;
            case "search-by-keyword":
                searchByKeyword(event);
                break;
            case "random":
                random(event);
                break;
            case "save-list":
                saveList(event);
                break;
            default:
                break;
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        // split String like "save:123456" into ["save", "123456"] = [action, bookId]
        String[] parts = event.getComponentId().split(":");
        String action = parts[0];
        String bookId = parts.length > 1 ? parts[1] : "";

        String userId = event.getUser().getId();
        UserData user = storageService.getUser(userId);

        switch (action) {
            case "save":
                handleSave(event, user, bookId);
                break;
            case "block":
                handleBlock(event, user, bookId);
                break;
            case "undo_save":
                handleUndoSave(event, user, bookId);
                break;
            case "undo_block":
                handleUndoBlock(event, user, bookId);
                break;
            default:
                event.reply("❌ Unknown action").setEphemeral(true).queue();
        }
    }

    // --- Helper Methods for Button Actions ---

    private void handleSave(ButtonInteractionEvent event, UserData user, String bookId) {
        EmbedBuilder embed = new EmbedBuilder();

        // Condition A: Originally blocked -> Move to saved
        if (user.isBookBlocked(bookId)) {
            user.removeBlockedBook(bookId);
            user.addSavedBook(bookId);
            storageService.saveDatabase();

            embed.setColor(Color.ORANGE);
            embed.setTitle("🔄 updated");
            embed.setDescription("removed from **blocked list** and added to **saved list**.");
            embed.setFooter("ID: " + bookId);

            event.replyEmbeds(embed.build()).setEphemeral(true).queue();
        }
        // Condition B: Already saved
        else if (user.isBookSaved(bookId)) {
            embed.setColor(Color.YELLOW);
            embed.setTitle("⚠️ already saved");
            embed.setDescription("This book is already in your saved list!");

            event.replyEmbeds(embed.build()).setEphemeral(true).queue();
        }
        // Condition C: Normal save
        else {
            user.addSavedBook(bookId);
            storageService.saveDatabase();

            embed.setColor(Color.GREEN);
            embed.setTitle("❤️ saved");
            embed.setDescription("Successfully added to your saved list!");
            embed.setFooter("ID: " + bookId);

            Button undoButton = Button.danger("undo_save:" + bookId, "↩️ Undo Save");

            // reply by Embeds
            event.replyEmbeds(embed.build())
                    .setEphemeral(true)
                    .addActionRow(undoButton)
                    .queue();
        }
    }

    private void handleBlock(ButtonInteractionEvent event, UserData user, String bookId) {
        EmbedBuilder embed = new EmbedBuilder();

        // Condition A: Originally saved -> Move to blocked
        if (user.isBookSaved(bookId)) {
            user.removeSavedBook(bookId);
            user.addBlockedBook(bookId);
            storageService.saveDatabase();

            embed.setColor(Color.ORANGE);
            embed.setTitle("🔄 updated");
            embed.setDescription("removed from **saved list** and added to **blocked list**.");
            embed.setFooter("ID: " + bookId);

            event.replyEmbeds(embed.build()).setEphemeral(true).queue();
        }
        // Condition B: Already blocked
        else if (user.isBookBlocked(bookId)) {
            embed.setColor(Color.YELLOW);
            embed.setTitle("⚠️ already blocked");
            embed.setDescription("This book is already in your blocked list!");
            event.replyEmbeds(embed.build()).setEphemeral(true).queue();
        }
        // Condition C: Normal block
        else {
            user.addBlockedBook(bookId);
            storageService.saveDatabase();

            embed.setColor(Color.RED);
            embed.setTitle("🚫 blocked");
            embed.setDescription("This book is now blocked and will not appear in recommendations.");
            embed.setFooter("ID: " + bookId);

            Button undoBlockButton = Button.success("undo_block:" + bookId, "↩️ Undo Block");

            event.replyEmbeds(embed.build())
                    .setEphemeral(true)
                    .addActionRow(undoBlockButton)
                    .queue();
        }
    }

    // edit message to show undo save or block
    private void handleUndoSave(ButtonInteractionEvent event, UserData user, String bookId) {
        EmbedBuilder embed = new EmbedBuilder();

        if (user.isBookSaved(bookId)) {
            user.removeSavedBook(bookId);
            storageService.saveDatabase();

            embed.setColor(Color.DARK_GRAY);
            embed.setTitle("🗑️ Unsaved");
            embed.setDescription("ID: " + bookId + " was removed from your saved list.");

            event.editMessageEmbeds(embed.build())
                    .setComponents()
                    .queue();
        } else {
            event.reply("⚠️ It's not in your saved list.").setEphemeral(true).queue();
        }
    }

    private void handleUndoBlock(ButtonInteractionEvent event, UserData user, String bookId) {
        EmbedBuilder embed = new EmbedBuilder();

        if (user.isBookBlocked(bookId)) {
            user.removeBlockedBook(bookId);
            storageService.saveDatabase();

            embed.setColor(Color.DARK_GRAY);
            embed.setTitle("🕊️ UnBlocked");
            embed.setDescription("ID: " + bookId + " was removed from your blocked list.");

            event.editMessageEmbeds(embed.build())
                    .setComponents()
                    .queue();
        } else {
            event.reply("⚠️ It's not in your blocked list.").setEphemeral(true).queue();
        }
    }

    private void ping(SlashCommandInteractionEvent event) {

        event.reply("Pong! delay: " + event.getJDA().getGatewayPing() + "ms")
                .setEphemeral(true)
                .queue();
    }

    private void hello(SlashCommandInteractionEvent event) {
        String userName = event.getUser().getName();
        event.reply("Hello, " + userName + "!")
                .setEphemeral(true)
                .queue();
    }

    private void searchByNumber(SlashCommandInteractionEvent event) {
        String number = event.getOption("number").getAsString();
        event.deferReply().queue();// bot is thinking

        // call nhService to search
        Book book = nhService.search(number);

        // call embedResult to send embed message
        embedResult(event, book);
    }

    private void searchByKeyword(SlashCommandInteractionEvent event) {
        String keyword = event.getOption("keyword").getAsString();

        // all time popular as default
        String sort = "popular";
        if (event.getOption("sort-by") != null) {
            sort = event.getOption("sort-by").getAsString();
        }

        event.deferReply().queue();

        // send to service
        // ex： "chinese###popular-today"
        String query = keyword + "###" + sort;

        Book book = nhService.search(query);

        embedResult(event, book);
    }

    private void random(SlashCommandInteractionEvent event) {
        // Implementation can be added similarly to searchByNumber
    }

    private void saveList(SlashCommandInteractionEvent event) {
        // Implementation can be added to show user's saved list
    }

    private void embedResult(SlashCommandInteractionEvent event, Book book) {
        if (book.isSuccess()) {
            EmbedBuilder embed = new EmbedBuilder();
            embed.setColor(Color.decode("#ED2553"));

            // nhentai icon
            embed.setAuthor("nhentai", book.getUrl(), "https://i.imgur.com/uLAimaY.png");

            embed.setTitle(book.getTitle(), book.getUrl());

            String id = extractIdFromUrl(book.getUrl());
            embed.addField("#️⃣ ID", id, true);
            // language
            if (!book.getLanguages().isEmpty()) {
                String langStr;
                if (book.getLanguages().contains("translated") && book.getLanguages().size() > 1) {
                    langStr = book.getLanguages().get(1); // skip "translated"
                } else {
                    langStr = String.join(", ", book.getLanguages());
                }
                embed.addField("🌐 Language", langStr, true);
            }

            // parody
            if (!book.getParodies().isEmpty()) {
                String parodyStr = String.join(", ", book.getParodies());
                embed.addField("🎬 Parody", parodyStr, true);
            }

            // tags
            if (!book.getTags().isEmpty()) {
                // only show first 15 tags
                String tagsStr = book.getTags().stream().limit(15).collect(Collectors.joining(", "));
                if (book.getTags().size() > 15)
                    tagsStr += " ...";
                embed.addField("🏷️ Tags", tagsStr, false);
            }
            // artist
            if (!book.getArtists().isEmpty()) {
                String artist = String.join(", ", book.getArtists());
                embed.addField("🎨 Artist", artist, true);
            }
            // date added(have not implemented yet)
            /*
             * if (book.getUploadDate() != null && !book.getUploadDate().isEmpty()) {
             * embed.addField("📅 Added on", book.getUploadDate(), true);
             * }
             */

            embed.setImage(book.getCoverUrl());

            // --- buttons under embed ---

            // save button
            Button saveButton = Button.primary("save:" + id, "❤️ save this book");

            // block button
            Button blockButton = Button.danger("block:" + id, "🚫 block this book");
            // link button
            Button linkButton = Button.link(book.getUrl(), "🔗 open link");

            // send embed with buttons
            event.getHook().sendMessageEmbeds(embed.build())
                    .addActionRow(saveButton, blockButton, linkButton)
                    .queue();

        } else {
            EmbedBuilder errorEmbed = new EmbedBuilder();
            errorEmbed.setColor(Color.RED);
            errorEmbed.setTitle("❌ not found");
            errorEmbed.setDescription(book.getError());
            event.getHook().sendMessageEmbeds(errorEmbed.build()).queue();
        }
    }

    // helper to extract ID from url
    private String extractIdFromUrl(String url) {
        try {
            String[] parts = url.split("/");
            return parts[parts.length - 1];
        } catch (Exception e) {
            return "Unknown";
        }
    }
}