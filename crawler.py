import sys
import json
import cloudscraper
from bs4 import BeautifulSoup

scraper = cloudscraper.create_scraper(browser={'browser': 'chrome', 'platform': 'windows', 'desktop': True})

def search_nhentai(query):
    # ID search
    if query.isdigit():
        url = f"https://nhentai.net/g/{query}/"
    # Keyword search
    
    else:
        # 這裡簡化，先示範 ID 搜尋，關鍵字搜尋邏輯類似
        return {"success": False, "error": "query search error"}

    try:
        response = scraper.get(url)
        
        if response.status_code == 200:
            soup = BeautifulSoup(response.text, 'html.parser')
            
            # 1. 基礎資訊
            title_tag = soup.select_one('#info h1.title')
            title = title_tag.text.strip() if title_tag else "Unknown Title"
            
            cover_tag = soup.select_one('#cover img')
            cover = ""
            if cover_tag:
                # 策略：優先找 data-src (通常是懶加載的高畫質圖)，如果沒有，就找 src
                if 'data-src' in cover_tag.attrs:
                    cover = cover_tag['data-src']
                elif 'src' in cover_tag.attrs:
                    cover = cover_tag['src']
            if cover.startswith("//"): 
                cover = "https:" + cover

            # 2. 進階標籤分類邏輯
            # 我們準備一個字典來裝分類好的資料
            tag_data = {
                "Parodies": [],
                "Characters": [],
                "Tags": [],
                "Artists": [],
                "Groups": [],
                "Languages": [],
                "Categories": [],
                "Pages": []
            }

            # nhentai 的標籤都在 class="tag-container" 裡面
            for container in soup.select('.tag-container'):
                # 取得該區塊的文字 (例如 "Artists: hino hino 257")
                # 我們只取前幾個字來判斷是哪一類
                text = container.text.strip()
                
                category = None
                if text.startswith("Parodies:"): category = "Parodies"
                elif text.startswith("Characters:"): category = "Characters"
                elif text.startswith("Tags:"): category = "Tags"
                elif text.startswith("Artists:"): category = "Artists"
                elif text.startswith("Groups:"): category = "Groups"
                elif text.startswith("Languages:"): category = "Languages"
                elif text.startswith("Categories:"): category = "Categories"
                elif text.startswith("Pages:"): category = "Pages"
                if category:
                    # 如果確認是我們要的分類，就抓裡面的 span.name
                    # 這樣可以避開後面的數字 (例如 18K)
                    tags = [t.text.strip() for t in container.select('.tags a .name')]
                    tag_data[category] = tags

            # 3. 回傳完整結構
            return {
                "success": True,
                "title": title,
                "cover": cover,
                "url": url,
                "artists": tag_data["Artists"],
                "groups": tag_data["Groups"],
                "languages": tag_data["Languages"],
                "tags": tag_data["Tags"], # 這裡只會剩下真正的「屬性」
                "parodies": tag_data["Parodies"],
                "characters": tag_data["Characters"],
                "pages": tag_data["Pages"]
                
            }
    except Exception as e:
        return {"success": False, "error": str(e)}

if __name__ == "__main__":
    # Java 會傳入參數，例如: python crawler.py 177013
    if len(sys.argv) > 1:
        query_arg = sys.argv[1]
        result = search_nhentai(query_arg)
        
        # 這是關鍵！把結果轉成 JSON 印出來，Java 才讀得懂
        print(json.dumps(result))
    else:
        print(json.dumps({"success": False, "error": "No query provided"}))