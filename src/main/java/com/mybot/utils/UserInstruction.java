package com.mybot.utils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

// 這個類別專門負責生成說明書的內容和導航按鈕，讓 SlashCommandHandler 可以專注在指令邏輯上
public class UserInstruction {

    public static final int MAX_PAGES = 3;

    // 負責根據頁數，產生對應畫面的核心框架
    public static EmbedBuilder buildHelpEmbed(int page) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(Color.CYAN); // 您可以自訂您喜歡的顏色
        embed.setFooter("Page " + page + " / " + MAX_PAGES);

        switch (page) {
            case 1:
                embed.setTitle("📖 機器人使用指南 - 基礎指令");
                // 懇請您在此處填入您想寫的說明文字
                // 例如: embed.addField("/search-by-number", "透過 ID 尋找特定作品", false);
                break;
            case 2:
                embed.setTitle("📖 機器人使用指南 - 搜尋與過濾");
                // 懇請您在此處填入第 2 頁的內容
                break;
            case 3:
                embed.setTitle("📖 機器人使用指南 - 收藏與進階功能");
                // 懇請您在此處填入第 3 頁的內容
                break;
            default:
                embed.setTitle("❌ 發生錯誤");
                embed.setDescription("找不到該頁面。");
        }
        return embed;
    }

    // 負責自動產生上一頁、下一頁按鈕的邏輯 (已包含按鈕禁用防呆)
    public static List<Button> buildNavigationButtons(int currentPage) {
        List<Button> buttons = new ArrayList<>();

        // 建立上一頁按鈕 (ID 帶有目標頁數)
        Button prevButton = Button.primary("help_nav:" + (currentPage - 1), "⬅️ 上一頁");
        if (currentPage <= 1) {
            prevButton = prevButton.asDisabled(); // 如果是第 1 頁，自動反灰不可按
        }

        // 建立下一頁按鈕
        Button nextButton = Button.primary("help_nav:" + (currentPage + 1), "下一頁 ➡️");
        if (currentPage >= MAX_PAGES) {
            nextButton = nextButton.asDisabled(); // 如果是最後 1 頁，自動反灰不可按
        }

        buttons.add(prevButton);
        buttons.add(nextButton);
        return buttons;
    }
}