import 'dotenv/config';
import { google } from 'googleapis';
import fs from 'fs/promises';

// --- ⚙️ Configuration ---
const YOUTUBE_API_KEY = process.env.YOUTUBE_API_KEY;
const YOUTUBE_VIDEO_ID = process.env.YOUTUBE_VIDEO_ID;

if (!YOUTUBE_API_KEY || !YOUTUBE_VIDEO_ID) {
    console.error('🚨 Please set YOUTUBE_API_KEY and YOUTUBE_VIDEO_ID in your .env file.');
    process.exit(1);
}

const youtube = google.youtube({
    version: 'v3',
    auth: YOUTUBE_API_KEY,
});

// ----------------------------------------------------------------------------
// 🛠️ Helper Functions
// ----------------------------------------------------------------------------

/**
 * Fetches the activeLiveChatId for a given video ID.
 * This ID is essential for fetching live chat messages.
 */
async function getLiveChatId(videoId) {
    console.log(`🔍 Fetching live chat ID for video: ${videoId}`);
    try {
        const response = await youtube.videos.list({
            part: 'liveStreamingDetails',
            id: videoId,
        });

        const video = response.data.items[0];
        if (video && video.liveStreamingDetails && video.liveStreamingDetails.activeLiveChatId) {
            const liveChatId = video.liveStreamingDetails.activeLiveChatId;
            console.log(`✅ Found Live Chat ID: ${liveChatId}`);
            return liveChatId;
        } else {
            console.error('Could not find activeLiveChatId. Is this a valid YouTube live stream or premiere?');
            return null;
        }
    } catch (e) {
        console.error('Error fetching video details:', e.message);
        return null;
    }
}

/**
 * Fetches all chat messages for a given liveChatId and returns them as an array.
 * It iterates through all pages using nextPageToken.
 */
async function getAllChatMessages(liveChatId) {
    console.log('💬 Fetching all chat messages... This may take a while for long streams.');
    let allMessages = [];
    let nextPageToken = null;
    let pageCount = 0;

    try {
        do {
            pageCount++;
            console.log(`- Fetching page ${pageCount}...`);
            const response = await youtube.liveChatMessages.list({
                liveChatId: liveChatId,
                part: 'snippet,authorDetails',
                pageToken: nextPageToken,
                maxResults: 2000, // API maximum
            });

            const newMessages = response.data.items;
            if (newMessages && newMessages.length > 0) {
                allMessages.push(...newMessages);
            }

            nextPageToken = response.data.nextPageToken;

            // Add a small delay to avoid hitting API quota limits.
            if (nextPageToken) {
                await new Promise(resolve => setTimeout(resolve, 200));
            }

        } while (nextPageToken);

        console.log(`✨ Fetched a total of ${allMessages.length} messages.`);
        return allMessages;

    } catch (e) {
        console.error('Error fetching chat messages:', e.message);
        if (e.response && e.response.data) {
            console.error('API Error Details:', JSON.stringify(e.response.data.error, null, 2));
        }
        return null;
    }
}

// ----------------------------------------------------------------------------
// ▶️ Main Execution
// ----------------------------------------------------------------------------

async function main() {
    console.log('--- YouTube Chat Saver ---');
    
    const liveChatId = await getLiveChatId(YOUTUBE_VIDEO_ID);
    if (!liveChatId) {
        console.log('❌ Could not proceed without a Live Chat ID. Exiting.');
        return;
    }

    const messages = await getAllChatMessages(liveChatId);
    if (!messages) {
        console.log('❌ Failed to fetch chat messages. Exiting.');
        return;
    }

    if (messages.length === 0) {
        console.log('🤔 No messages were found for this live stream.');
        return;
    }

    const outputFilename = `chat-history-${YOUTUBE_VIDEO_ID}.json`;
    try {
        await fs.writeFile(outputFilename, JSON.stringify(messages, null, 2));
        console.log(`
💾 Successfully saved ${messages.length} messages to ${outputFilename}`);
    } catch (e) {
        console.error(`Error writing to file ${outputFilename}:`, e.message);
    }
}

main();
