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

const outputFilename = `chat-history-${YOUTUBE_VIDEO_ID}.json`;

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
            // Check if the stream has already ended.
            if (video && video.liveStreamingDetails && video.liveStreamingDetails.actualEndTime) {
                 console.error('❌ This live stream has already ended.');
            } else {
                 console.error('❌ Could not find an active live chat. Is this a valid YouTube live stream or premiere that is currently active?');
            }
            return null;
        }
    } catch (e) {
        console.error('Error fetching video details:', e.message);
        return null;
    }
}

/**
 * Continuously polls for new chat messages and appends them to a file.
 */
async function pollChatMessages(liveChatId) {
    let nextPageToken = null;
    let chatMessages = [];
    let isFirstPoll = true;

    // Load existing messages if the file exists
    try {
        await fs.access(outputFilename);
        const data = await fs.readFile(outputFilename, 'utf8');
        if (data) {
            chatMessages = JSON.parse(data);
            console.log(`📝 Resuming from ${chatMessages.length} saved messages.`);
            isFirstPoll = false;
        }
    } catch (error) {
        if (error.code === 'ENOENT') {
            console.log('📄 No previous chat history found. Starting a new log.');
        } else {
            console.error('❌ Error reading chat history file:', error);
            return; // Stop if we can't read the file (and it's not a "not found" error)
        }
    }
    
    // If starting fresh, create the file with an empty array
    if (isFirstPoll) {
        await fs.writeFile(outputFilename, '[]', 'utf8');
    }

    console.log('📡 Starting to poll for live chat messages... Press Ctrl+C to stop.');

    while (true) {
        try {
            const response = await youtube.liveChatMessages.list({
                liveChatId: liveChatId,
                part: 'snippet,authorDetails',
                pageToken: nextPageToken,
                maxResults: 2000,
            });

            const { items: newMessages, pollingIntervalMillis, nextPageToken: newNextPageToken } = response.data;
            
            // The token for the *next* poll is the new one we just received.
            nextPageToken = newNextPageToken;

            if (newMessages && newMessages.length > 0) {
                console.log(`📥 Fetched ${newMessages.length} new message(s).`);
                chatMessages.push(...newMessages);
                
                // Atomically write the updated chat log.
                await fs.writeFile(outputFilename, JSON.stringify(chatMessages, null, 2));
                console.log(`💾 Total messages saved: ${chatMessages.length}`);
            }

            // User requested a 5-minute polling interval to conserve API quota.
            const FIVE_MINUTES_IN_MS = 5 * 60 * 1000;
            console.log(`...waiting 5 minutes for the next poll...`);
            await new Promise(resolve => setTimeout(resolve, FIVE_MINUTES_IN_MS));

        } catch (e) {
            const errorReason = e.response?.data?.error?.errors?.[0]?.reason;
            if (errorReason === 'liveChatEnded') {
                console.log('🔴 Live stream has officially ended. Finalizing chat log.');
                break;
            } else if (errorReason === 'forbidden') {
                 console.log('🤔 Chat is disabled or private. Exiting.');
                 break;
            }
            else {
                console.error(`An error occurred during polling: ${e.message}. Retrying in 10 seconds...`);
                await new Promise(resolve => setTimeout(resolve, 10000));
            }
        }
    }
    console.log('✅ Chat polling finished.');
}


// ----------------------------------------------------------------------------
// ▶️ Main Execution
// ----------------------------------------------------------------------------

async function main() {
    console.log('--- YouTube Real-time Chat Saver ---');
    
    const liveChatId = await getLiveChatId(YOUTUBE_VIDEO_ID);
    if (!liveChatId) {
        console.log('❌ Could not proceed without a Live Chat ID. Exiting.');
        return;
    }

    await pollChatMessages(liveChatId);
}

main();