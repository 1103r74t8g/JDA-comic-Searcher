import sys
import json
import cloudscraper
import random
from bs4 import BeautifulSoup
from urllib.parse import quote_plus

scraper = cloudscraper.create_scraper(browser={'browser': 'chrome', 'platform': 'windows', 'desktop': True})

def search_nhentai(query):
    target_url = ""

    # ==========================================
    # ID search
    # ==========================================
    if query.isdigit():
        target_url = f"https://nhentai.net/g/{query}/"
    
    # ==========================================
    # Keyword/Recent search (including recent)
    # ==========================================
    else:
        # 1. Parse parameters
        if "###" in query:
            search_term, sort_type = query.split("###")
        else:
            search_term = query
            sort_type = "popular"

        # 2. Handle sorting
        # recent -> no sort parameter (default is recent)
        # popular -> &sort=popular
        sort_param = ""
        if sort_type != "recent":
            sort_param = f"&sort={sort_type}"

        # 3. 隨機頁數 (前 5 頁)
        random_page = random.randint(1, 5)
        
        # ★ 這裡就是關鍵：
        # 如果 Java 傳來的是空字串 "" (代表 recent)，這裡拼出來就會是 /search/?q=&page=...
        # 這在 nhentai 就等於「瀏覽最新」，而且我們還能加上 -tag 過濾！
        search_url = f"https://nhentai.net/search/?q={quote_plus(search_term)}&page={random_page}{sort_param}"
        
        try:
            search_response = scraper.get(search_url)
            search_soup = BeautifulSoup(search_response.text, 'html.parser')
            all_results = search_soup.select('.gallery a.cover')

            # Fallback 機制 (如果該頁沒東西，退回第 1 頁)
            if not all_results:
                fallback_url = f"https://nhentai.net/search/?q={quote_plus(search_term)}&page=1{sort_param}"
                fallback_response = scraper.get(fallback_url)
                fallback_soup = BeautifulSoup(fallback_response.text, 'html.parser')
                all_results = fallback_soup.select('.gallery a.cover')

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
    # 通用階段: 進入詳細頁面抓取資料 (完全不用動)
    # ==========================================
    try:
        response = scraper.get(target_url)
        
        if response.status_code == 200:
            soup = BeautifulSoup(response.text, 'html.parser')
            
            title_tag = soup.select_one('#info h1.title')
            title = title_tag.text.strip() if title_tag else "Unknown Title"
            
            cover_tag = soup.select_one('#cover img')
            cover = ""
            if cover_tag:
                if 'data-src' in cover_tag.attrs:
                    cover = cover_tag['data-src']
                elif 'src' in cover_tag.attrs:
                    cover = cover_tag['src']
            if cover.startswith("//"): 
                cover = "https:" + cover

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
    if len(sys.argv) > 1:
        query_arg = " ".join(sys.argv[1:]) 
        result = search_nhentai(query_arg)
        print(json.dumps(result))
    else:
        print(json.dumps({"success": False, "error": "No query provided"}))