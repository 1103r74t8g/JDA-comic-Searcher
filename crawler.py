import sys
import json
import cloudscraper
import random
from bs4 import BeautifulSoup
from urllib.parse import quote_plus

# 初始化爬蟲，模擬瀏覽器行為以繞過 Cloudflare
scraper = cloudscraper.create_scraper(browser={'browser': 'chrome', 'platform': 'windows', 'desktop': True})

def search_nhentai(query):
    target_url = ""

    # ==========================================
    # 模式 1: 抓取最新 (CMD_RECENT)
    # ==========================================
    if query == "CMD_RECENT":
        # 直接去首頁抓取
        search_url = "https://nhentai.net/"
        
        try:
            search_response = scraper.get(search_url)
            if search_response.status_code == 200:
                search_soup = BeautifulSoup(search_response.text, 'html.parser')
                all_results = search_soup.select('.gallery a.cover')
                
                if all_results:
                    # 從首頁的 25 本中隨機抽一本
                    random_entry = random.choice(all_results)
                    if 'href' in random_entry.attrs:
                        target_url = "https://nhentai.net" + random_entry['href']
                    else:
                        return {"success": False, "error": "Invalid result link in recent"}
                else:
                    return {"success": False, "error": "No recent results found"}
            else:
                return {"success": False, "error": f"Recent page unreachable: {search_response.status_code}"}
        except Exception as e:
            return {"success": False, "error": "Recent search error: " + str(e)}

    # ==========================================
    # 模式 2: ID 直接搜尋 (例如: 177013)
    # ==========================================
    elif query.isdigit():
        target_url = f"https://nhentai.net/g/{query}/"
    
    # ==========================================
    # 模式 3: 關鍵字搜尋 (支援排序)
    # ==========================================
    else:
        # 1. 解析參數：Java 傳過來的格式是 "keyword###sort_type"
        if "###" in query:
            search_term, sort_type = query.split("###")
        else:
            # 防呆：如果沒傳排序，預設用 popular (歷史熱門)
            search_term = query
            sort_type = "popular"

        # 2. 處理排序參數
        # nhentai 規則: &sort=popular, &sort=popular-today, &sort=popular-week
        # 如果是 "recent" 則不需要加 &sort 參數
        sort_param = ""
        if sort_type != "recent":
            sort_param = f"&sort={sort_type}"

        # 3. 隨機頁數策略：從前 5 頁中隨機選一頁來爬
        random_page = random.randint(1, 5)
        
        # 組合搜尋網址
        search_url = f"https://nhentai.net/search/?q={quote_plus(search_term)}&page={random_page}{sort_param}"
        
        try:
            search_response = scraper.get(search_url)
            search_soup = BeautifulSoup(search_response.text, 'html.parser')
            all_results = search_soup.select('.gallery a.cover')

            # --- 安全網機制 (Fallback) ---
            # 如果隨機抽到第 5 頁但沒東西 (代表結果少於 5 頁)，自動退回第 1 頁重抓
            if not all_results:
                fallback_url = f"https://nhentai.net/search/?q={quote_plus(search_term)}&page=1{sort_param}"
                fallback_response = scraper.get(fallback_url)
                fallback_soup = BeautifulSoup(fallback_response.text, 'html.parser')
                all_results = fallback_soup.select('.gallery a.cover')

            # 如果有抓到結果，從中隨機抽一本
            if all_results:
                random_entry = random.choice(all_results)
                if 'href' in random_entry.attrs:
                    target_url = "https://nhentai.net" + random_entry['href']
                else:
                    return {"success": False, "error": "Invalid result link"}
            else:
                return {"success": False, "error": "No results found"}

        except Exception as e:
            return {"success": False, "error": "Search loop error: " + str(e)}

    # ==========================================
    # 通用階段: 進入詳細頁面抓取資料
    # ==========================================
    try:
        response = scraper.get(target_url)
        
        if response.status_code == 200:
            soup = BeautifulSoup(response.text, 'html.parser')
            
            # 1. 抓標題
            title_tag = soup.select_one('#info h1.title')
            title = title_tag.text.strip() if title_tag else "Unknown Title"
            
            # 2. 抓封面 (優先找 data-src)
            cover_tag = soup.select_one('#cover img')
            cover = ""
            if cover_tag:
                if 'data-src' in cover_tag.attrs:
                    cover = cover_tag['data-src']
                elif 'src' in cover_tag.attrs:
                    cover = cover_tag['src']
            if cover.startswith("//"): 
                cover = "https:" + cover

            # 3. 抓取所有 Tag 資訊
            tag_data = {
                "Parodies": [], "Characters": [], "Tags": [],
                "Artists": [], "Groups": [], "Languages": [],
                "Categories": [], "Pages": []
            }

            for container in soup.select('.tag-container'):
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
                    tags = [t.text.strip() for t in container.select('.tags a .name')]
                    tag_data[category] = tags

            # 4. 回傳完整 JSON 結構
            return {
                "success": True,
                "title": title,
                "cover": cover,
                "url": target_url,
                "artists": tag_data["Artists"],
                "groups": tag_data["Groups"],
                "languages": tag_data["Languages"],
                "tags": tag_data["Tags"],
                "parodies": tag_data["Parodies"],
                "characters": tag_data["Characters"],
                "pages": tag_data["Pages"]
            }
        else:
            return {"success": False, "error": f"Gallery page error: {response.status_code}"}
            
    except Exception as e:
        return {"success": False, "error": str(e)}

if __name__ == "__main__":
    # 支援接收多個參數 (例如 Java 可能會把 "chinese###popular" 拆開傳，這裡把它們接回去)
    if len(sys.argv) > 1:
        query_arg = " ".join(sys.argv[1:]) 
        result = search_nhentai(query_arg)
        print(json.dumps(result))
    else:
        print(json.dumps({"success": False, "error": "No query provided"}))