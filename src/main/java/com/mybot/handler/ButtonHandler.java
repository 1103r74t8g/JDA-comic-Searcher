package com.mybot.handler;

import com.mybot.model.UserData;
import com.mybot.service.JsonStorageService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import java.awt.Color;

public class ButtonHandler {

    private final JsonStorageService storageService;

    public ButtonHandler(JsonStorageService storageService) {
        this.storageService = storageService;
    }

    public void handle(ButtonInteractionEvent event) {
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

}