package com.mybot.handler;

import com.mybot.model.Book;
import com.mybot.model.User;
import com.mybot.service.JsonStorageService;
import com.mybot.service.NHentaiService;
import com.mybot.utils.CommandHelper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class SlashCommandHandler {
    private final JsonStorageService storageService;
    private final NHentaiService nhService;

    public SlashCommandHandler(JsonStorageService storageService, NHentaiService nhService) {
        this.storageService = storageService;
        this.nhService = nhService;
    }

    public void handle(SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "help":
                help(event);
                break;
            case "ping":
                ping(event);
                break;
            case "search-by-number":
                searchByNumber(event);
                break;
            case "search-by-keyword":
                searchByKeyword(event);
                break;
            case "recent-uploads":
                recentUploads(event);
                break;
            case "block-tag":
                blockTag(event);
                break;
            case "unblock-tag":
                unblockTag(event);
                break;
            case "save-list":
                saveList(event);
                break;
        }
    }

    // --- Command Implementations ---

    private void help(SlashCommandInteractionEvent event) {

    }

    private void ping(SlashCommandInteractionEvent event) {
        long ping = event.getJDA().getGatewayPing();

        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(Color.green);
        embed.setTitle("Pong!");
        embed.setDescription("delay: **" + ping + " ms**");

        event.replyEmbeds(embed.build()).setEphemeral(true).queue();
    }

    private void searchByNumber(SlashCommandInteractionEvent event) {
        String number = event.getOption("number").getAsString();
        event.deferReply().queue();// bot is thinking

        // call nhService to search
        Book book = nhService.search(number);

        // call embedResult to send embed message
        CommandHelper.embedResult(event, book);
    }

    private void searchByKeyword(SlashCommandInteractionEvent event) {
        String keyword = event.getOption("keyword").getAsString();

        // check min-pages option
        if (event.getOption("min-pages") != null) {
            int minPages = event.getOption("min-pages").getAsInt();
            keyword += " pages:>=" + minPages;
        }

        // user data for Blocked Tags
        String userId = event.getUser().getId();
        User user = storageService.getUser(userId);

        // get sort option
        String sort = "popular";
        if (event.getOption("sort-by") != null) {
            sort = event.getOption("sort-by").getAsString();
        }

        event.deferReply().queue();

        // Build search query with blocked tags
        String filteredKeyword = CommandHelper.buildFilteredQuery(keyword, user);

        // ex : "keyword -tag:\"yaoi\"###popular"
        String query = filteredKeyword + "###" + sort;

        Book book = nhService.search(query);

        CommandHelper.embedResult(event, book);
    }

    private void random(SlashCommandInteractionEvent event) {
        // Implementation can be added similarly to searchByNumber
    }

    private void recentUploads(SlashCommandInteractionEvent event) {
        String userId = event.getUser().getId();
        User user = storageService.getUser(userId);

        event.deferReply().queue();

        // if no block list -> "?"
        // else -> " -tag:\"yaoi\""
        String filteredKeyword = "?";
        if (user.getBlockedTags().isEmpty()) {
            // no blocked tags, just search recent uploads
            filteredKeyword = "?";
        } else {
            // has blocked tags, need to filter
            filteredKeyword = CommandHelper.buildFilteredQuery("", user);
        }

        // ex : " -tag:\"yaoi\"###recent"
        String query = filteredKeyword + "###recent";

        Book book = nhService.search(query);

        CommandHelper.embedResult(event, book);
    }

    private void saveList(SlashCommandInteractionEvent event) {
        // Implementation can be added to show user's saved list
    }

    private void blockTag(SlashCommandInteractionEvent event) {
        String tag = event.getOption("tag").getAsString().trim().toLowerCase();
        String userId = event.getUser().getId();
        User user = storageService.getUser(userId);

        EmbedBuilder embed = new EmbedBuilder();
        if (user.isTagBlocked(tag)) {
            embed.setColor(Color.YELLOW);
            embed.setTitle("⚠️ Already Blocked");
            embed.setDescription("Tag **" + tag + "** is already in your blocked list.");

            // Reply with Embed
            event.replyEmbeds(embed.build()).setEphemeral(true).queue();
            return;
        }

        // 3. Add to blocked list and save
        user.addBlockedTag(tag);
        storageService.saveDatabase();

        // 4. Build success blocked Embed
        embed.setColor(Color.RED); // Red represents blocked
        embed.setTitle("Successfully Blocked");
        embed.setDescription("Tag **" + tag + "** has been blocked.");
        embed.setFooter("Future searches and recommends will filter out works containing this tag.");

        // Reply with Embed
        event.replyEmbeds(embed.build()).setEphemeral(true).queue();
    }

    private void unblockTag(SlashCommandInteractionEvent event) {
        String tag = event.getOption("tag").getAsString().trim().toLowerCase();
        String userId = event.getUser().getId();
        User user = storageService.getUser(userId);

        EmbedBuilder embed = new EmbedBuilder();
        if (!user.isTagBlocked(tag)) {
            embed.setColor(Color.YELLOW);
            embed.setTitle("⚠️ Not Blocked");
            embed.setDescription("Tag **" + tag + "** is not in your blocked list.");

            // Reply with Embed
            event.replyEmbeds(embed.build()).setEphemeral(true).queue();
            return;
        }
        // 3. Remove from blocked list and save
        user.removeBlockedTag(tag);
        storageService.saveDatabase();
        // 4. Build success unblocked Embed
        embed.setColor(Color.GREEN); // Green represents unblocked
        embed.setTitle("Successfully Unblocked");
        embed.setDescription("Tag **" + tag + "** has been unblocked.");

        // Reply with Embed
        event.replyEmbeds(embed.build()).setEphemeral(true).queue();
    }

}
