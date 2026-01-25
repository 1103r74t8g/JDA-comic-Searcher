package com.mybot.commands;

import java.awt.Color;
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
                Commands.slash("search-by-keyword", "look up by keyword")
                        .addOption(OptionType.STRING, "keyword", "the keyword to search", true),
                Commands.slash("random", "get a random one")

        ).queue();

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
            default:
                break;
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String componentId = event.getComponentId();
        String userId = event.getUser().getId();
        UserData user = storageService.getUser(userId);

        // ==========================
        // save book botton
        // ==========================
        if (componentId.startsWith("save:")) {
            String bookId = componentId.split(":")[1];

            // condition A: originally blocked -> move to saved
            if (user.isBookBlocked(bookId)) {
                user.removeBlockedBook(bookId);
                user.addSavedBook(bookId);
                storageService.saveDatabase();

                event.reply("🔄 (ID: " + bookId + ") was removed from blocked list and added to saved list!")
                        .setEphemeral(true).queue();
            }
            // condition B: already saved
            else if (user.isBookSaved(bookId)) {
                event.reply("⚠️ You already saved this book!").setEphemeral(true).queue();
            }
            // condition C: normal save
            else {
                user.addSavedBook(bookId);
                storageService.saveDatabase();
                event.reply("✅ Successfully saved ID: **" + bookId + "**").setEphemeral(true).queue();
            }
        }

        // ==========================
        // block book button
        // ==========================
        else if (componentId.startsWith("block:")) {
            String bookId = componentId.split(":")[1];

            // condition A: originally saved -> move to blocked
            if (user.isBookSaved(bookId)) {
                user.removeSavedBook(bookId);
                user.addBlockedBook(bookId);
                storageService.saveDatabase();

                event.reply("🔄 (ID: " + bookId + ") was removed from saved list and added to blocked list!")
                        .setEphemeral(true).queue();
            }
            // condition B: already blocked
            else if (user.isBookBlocked(bookId)) {
                event.reply("⚠️ You already blocked this book!").setEphemeral(true).queue();
            }
            // condition C: normal block
            else {
                user.addBlockedBook(bookId);
                storageService.saveDatabase();
                event.reply("🚫 Successfully blocked ID: **" + bookId + "**").setEphemeral(true).queue();
            }
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
        // Implementation can be added similarly to searchByNumber
    }

    private void random(SlashCommandInteractionEvent event) {
        // Implementation can be added similarly to searchByNumber
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
                // 這裡只取前 15 個，避免 Discord 顯示錯誤
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

            embed.setImage(book.getCoverUrl());

            // --- ★ 新增按鈕邏輯 ★ ---

            // save button
            Button saveButton = Button.primary("save:" + id, "❤️ save this book");

            // block button
            Button blockButton = Button.primary("block:" + id, "🚫 block this book");
            // link button
            Button linkButton = Button.link(book.getUrl(), "🔗 open link");

            // 3. 送出時掛載按鈕 (addActionRow)
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