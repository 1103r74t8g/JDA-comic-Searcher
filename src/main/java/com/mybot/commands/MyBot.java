package com.mybot.commands;

import java.awt.Color;
import java.util.stream.Collectors;

import com.mybot.model.BookResult;
import com.mybot.service.NHentaiService;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class MyBot extends ListenerAdapter {

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

        )
                .queue();

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

        // 3. 在背景執行搜尋
        // 這裡使用了 thread (執行緒) 概念，雖然 JDA 的 deferReply 已經爭取了 15 分鐘，
        // 但為了不卡住機器人主執行緒，正規做法還是建議分開，不過目前先簡單寫：
        NHentaiService nhService = new NHentaiService();
        BookResult result = nhService.search(number);

        // 4. 回傳結果
        // 注意：因為剛剛用了 deferReply()，這裡要用 getHook().sendMessage()
        embedResult(event, result);
    }

    private void searchByKeyword(SlashCommandInteractionEvent event) {
        // Implementation can be added similarly to searchByNumber
    }

    private void random(SlashCommandInteractionEvent event) {
        // Implementation can be added similarly to searchByNumber
    }

    private void embedResult(SlashCommandInteractionEvent event, BookResult result) {
        if (result.isSuccess()) {
            EmbedBuilder embed = new EmbedBuilder();
            embed.setColor(Color.decode("#ED2553"));

            // nhentai icon
            embed.setAuthor("nhentai", result.getUrl(), "https://i.imgur.com/uLAimaY.png");

            embed.setTitle(result.getTitle(), result.getUrl());

            embed.addField("#️⃣ ID", extractIdFromUrl(result.getUrl()), true);
            // language
            if (!result.getLanguages().isEmpty()) {
                String langStr;
                if (result.getLanguages().contains("translated") && result.getLanguages().size() > 1) {
                    langStr = result.getLanguages().get(1); // skip "translated"
                } else {
                    langStr = String.join(", ", result.getLanguages());
                }
                embed.addField("🌐 Language", langStr, true);
            }

            // 3. 處理原作 (Parody) 與 角色 (Character)
            if (!result.getParodies().isEmpty()) {
                String parodyStr = String.join(", ", result.getParodies());
                embed.addField("🎬 Parody", parodyStr, true);
            }

            // 4. 處理屬性 (Tags) - 放在最下面獨佔一行
            if (!result.getTags().isEmpty()) {
                // 這裡只取前 15 個，避免 Discord 顯示錯誤
                String tagsStr = result.getTags().stream().limit(15).collect(Collectors.joining(", "));
                if (result.getTags().size() > 15)
                    tagsStr += " ...";
                embed.addField("🏷️ Tags", tagsStr, false);
            }
            // artist
            if (!result.getArtists().isEmpty()) {
                String artist = String.join(", ", result.getArtists());
                embed.addField("🎨 Artist", artist, true);
            }

            embed.setImage(result.getCoverUrl());

            event.getHook().sendMessageEmbeds(embed.build()).queue();

        } else {
            // 失敗時保持紅色警告
            EmbedBuilder errorEmbed = new EmbedBuilder();
            errorEmbed.setColor(Color.RED);
            errorEmbed.setTitle("❌ 搜尋失敗");
            errorEmbed.setDescription(result.getError());
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