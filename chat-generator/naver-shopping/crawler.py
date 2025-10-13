import time
import json
from selenium import webdriver
from selenium.webdriver.chrome.service import Service
from webdriver_manager.chrome import ChromeDriverManager
from bs4 import BeautifulSoup

# --- Settings ---
TARGET_URL = "https://view.shoppinglive.naver.com/lives/1758048"
POLLING_INTERVAL = 3
OUTPUT_FILENAME = "chat-history-shopping.json"
CHATBOT_PROFILE_IMAGE_URL = "https://uxwing.com/wp-content/themes/uxwing/download/communication-chat-call/chatbot-icon.png"

def load_existing_chats():
    """
    Reads the existing chat history file to return a set of processed comments and a list of chat history.
    """
    processed_comments = set()
    chat_history = []
    try:
        with open(OUTPUT_FILENAME, 'r', encoding='utf-8') as f:
            chat_history = json.load(f)
        for chat in chat_history:
            nickname = chat["authorDetails"]["displayName"]
            comment_text = chat["snippet"]["displayMessage"]
            processed_comments.add((nickname, comment_text))
        print(f"Loaded {len(chat_history)} existing chats.")
    except FileNotFoundError:
        print("Existing chat history file not found. A new file will be created.")
    except json.JSONDecodeError:
        print("Existing chat history file is empty or corrupted. A new file will be created.")
    return processed_comments, chat_history

def save_chats(chat_history):
    """
    Saves the chat history to a JSON file.
    """
    with open(OUTPUT_FILENAME, 'w', encoding='utf-8') as f:
        json.dump(chat_history, f, ensure_ascii=False, indent=2)

def main():
    """
    Main execution function. Opens a web page using Selenium and periodically crawls chats, saving them to a JSON file.
    """
    processed_comments, chat_history = load_existing_chats()
    driver = None  # Initialize driver variable

    try:
        service = Service(ChromeDriverManager().install())
        driver = webdriver.Chrome(service=service)
        
        print(f"Connecting to {TARGET_URL}...")
        driver.get(TARGET_URL)

        print("Waiting 10 seconds for the chat window to load...")
        time.sleep(10)
        print("Crawling started. (Press Ctrl+C to stop)")

        while True:
            html = driver.page_source
            soup = BeautifulSoup(html, "html.parser")
            
            comment_wrappers = soup.find_all("div", class_="Comment_wrap_wRrdF")

            new_comments_found = False
            for comment in comment_wrappers:
                nickname_element = comment.find("strong", class_="NormalComment_nickname_K2+Tx")
                comment_element = comment.find("span", class_="NormalComment_comment_Yqlnf")

                if nickname_element and comment_element:
                    nickname = nickname_element.get_text(strip=True)
                    comment_text = comment_element.get_text(strip=True)
                    
                    comment_id = (nickname, comment_text)

                    if comment_id not in processed_comments:
                        new_comments_found = True
                        
                        chat_data = {
                            "authorDetails": {
                                "displayName": nickname,
                                "profileImageUrl": CHATBOT_PROFILE_IMAGE_URL
                            },
                            "snippet": {
                                "displayMessage": comment_text
                            }
                        }
                        
                        chat_history.append(chat_data)
                        processed_comments.add(comment_id)
                        print(f"New comment added: {nickname}: {comment_text}")

            if new_comments_found:
                save_chats(chat_history)
                print(f"Total {len(chat_history)} comments saved to {OUTPUT_FILENAME}.")

            time.sleep(POLLING_INTERVAL)

    except KeyboardInterrupt:
        print("\nProgram stopped by user.")
    except Exception as e:
        print(f"An error occurred: {e}")
    finally:
        if chat_history:
            save_chats(chat_history)
            print(f"Finally saving {len(chat_history)} comments before exiting.")
        if driver:
            print("Closing the browser.")
            driver.quit()

if __name__ == "__main__":
    main()