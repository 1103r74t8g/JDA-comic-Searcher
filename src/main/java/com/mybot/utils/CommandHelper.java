package com.mybot.utils;

import com.mybot.model.Book;
import com.mybot.model.UserData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.Color;
import java.util.stream.Collectors;

public class CommandHelper {

    // build search query with blocked tags
    public static String buildFilteredQuery(String keyword, UserData user) {
        StringBuilder sb = new StringBuilder(keyword);
        for (String tag : user.getBlockedTags()) {
            sb.append(" -tag:\"").append(tag).append("\"");
        }
        return sb.toString().trim();
    }

    // Extract ID from URL
    public static String extractIdFromUrl(String url) {
        try {
            String[] parts = url.split("/");
            return parts[parts.length - 1];
        } catch (Exception e) {
            return "Unknown";
        }
    }

    // Embed the result
    public static void embedResult(SlashCommandInteractionEvent event, Book book) {
        if (book.isSuccess()) {
            EmbedBuilder embed = new EmbedBuilder();
            embed.setColor(Color.decode("#ED2553"));
            embed.setAuthor("nhentai", book.getUrl(), "https://i.imgur.com/uLAimaY.png");
            embed.setTitle(book.getTitle(), book.getUrl());

            String id = extractIdFromUrl(book.getUrl());
            embed.addField("#️⃣ ID", id, true);

            // Language
            if (!book.getLanguages().isEmpty()) {
                String langStr;
                if (book.getLanguages().contains("translated") && book.getLanguages().size() > 1) {
                    langStr = book.getLanguages().get(1);
                } else {
                    langStr = String.join(", ", book.getLanguages());
                }
                embed.addField("🌐 Language", langStr, true);
            }

            // Parody
            if (!book.getParodies().isEmpty()) {
                String parodyStr = String.join(", ", book.getParodies());
                embed.addField("🎬 Parody", parodyStr, true);
            }

            // Tags only show top 15 tags
            if (!book.getTags().isEmpty()) {
                String tagsStr = book.getTags().stream().limit(15).collect(Collectors.joining(", "));
                if (book.getTags().size() > 15)
                    tagsStr += " ...";
                embed.addField("🏷️ Tags", tagsStr, false);
            }

            // Artist
            if (!book.getArtists().isEmpty()) {
                String artist = String.join(", ", book.getArtists());
                embed.addField("🎨 Artist", artist, true);
            }

            embed.setImage(book.getCoverUrl());

            // Buttons
            Button saveButton = Button.primary("save:" + id, "❤️ Save");
            Button blockButton = Button.danger("block:" + id, "🚫 Block");
            Button linkButton = Button.link(book.getUrl(), "🔗 Open Link");

            // Send via Hook (because deferReply was called earlier)
            event.getHook().sendMessageEmbeds(embed.build())
                    .addActionRow(saveButton, blockButton, linkButton)
                    .queue();

        } else {
            // Error Embed
            EmbedBuilder errorEmbed = new EmbedBuilder();
            errorEmbed.setColor(Color.RED);
            errorEmbed.setTitle("❌ Not Found");
            errorEmbed.setDescription(book.getError());

            event.getHook().sendMessageEmbeds(errorEmbed.build()).queue();
        }
    }
}