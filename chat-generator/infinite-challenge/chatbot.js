import 'dotenv/config';
import SockJS from 'sockjs-client';
import Stomp from 'stompjs';
import jwt from 'jsonwebtoken';
import fs from 'fs/promises';

// --- ⚙️ Configuration ---
const SECRET_KEY = process.env.SECRET_KEY;
const YOUTUBE_VIDEO_ID = process.env.YOUTUBE_VIDEO_ID;
const CHATBOT_USER_ID = "2";
const ROOM_ID = "2";
const WEBSOCKET_URL = process.env.WEBSOCKET_URL;

// --- 🌐 Global State ---
let stompClient = null;

// ----------------------------------------------------------------------------
// ⭐️ Core Logic: Class to manage chat replay from a file
// ----------------------------------------------------------------------------
class ChatReplayer {
    constructor(stompClient) {
        this.stompClient = stompClient;
        this.commentBuffer = [];
        this.currentBufferIndex = 0;
        this.isReplaying = false;
    }

    // Start the replay
    async start() {
        if (this.isReplaying) return;

        const chatHistoryFile = `chat-history-${YOUTUBE_VIDEO_ID}.json`;
        console.log(`▶️ Starting chat replay from file: ${chatHistoryFile}`);

        // 1. Read chat history from file
        try {
            const fileContent = await fs.readFile(chatHistoryFile, 'utf-8');
            this.commentBuffer = JSON.parse(fileContent);
            console.log(`[Loader] Successfully loaded ${this.commentBuffer.length} comments from file.`);
        } catch (e) {
            console.error(`[Loader] Error reading or parsing chat history file: ${e.message}`);
            console.error(`[Loader] Please make sure you have run 'node save-chat.js' first.`);
            return; // Cannot start replay without the file
        }

        if (this.commentBuffer.length === 0) {
            console.log('[Loader] No comments to replay.');
            return;
        }

        this.isReplaying = true;
        this._processBuffer(); // Start processing the buffer
    }

    // Stop the replay
    stop() {
        this.isReplaying = false;
        console.log('⏹️ Chat replay stopped.');
        if (this.stompClient && this.stompClient.connected) {
            this.stompClient.disconnect(() => {
                console.log('Disconnected from STOMP.');
            });
        }
    }

    // Main loop to iterate through the buffer and send comments
    async _processBuffer() {
        while (this.isReplaying && this.currentBufferIndex < this.commentBuffer.length) {
            const currentComment = this.commentBuffer[this.currentBufferIndex];
            let delay = 0;

            // If not the first comment, calculate time difference from the previous one
            if (this.currentBufferIndex > 0) {
                const previousComment = this.commentBuffer[this.currentBufferIndex - 1];
                const currentTime = new Date(currentComment.snippet.publishedAt).getTime();
                const previousTime = new Date(previousComment.snippet.publishedAt).getTime();
                delay = currentTime - previousTime;
            }

            // Set a max delay for realism (e.g., 5 seconds)
            const realisticDelay = Math.min(delay, 5000);
            if (realisticDelay > 0) {
                await new Promise(resolve => setTimeout(resolve, realisticDelay));
            }

            if (!this.isReplaying) break; // Check if stopped during the delay

            sendCommentToServer(currentComment, this.stompClient);
            this.currentBufferIndex++;
        }

        if (this.isReplaying) {
            console.log('✅ All comments have been replayed. Finishing.');
            this.stop();
        }
    }
}

// ----------------------------------------------------------------------------
// 🛠️ Helper Functions
// ----------------------------------------------------------------------------

function generateToken() {
    const payload = {
        sub: CHATBOT_USER_ID,
        exp: Math.floor(Date.now() / 1000) + (60 * 60) // 1 hour expiration
    };
    return jwt.sign(payload, SECRET_KEY, { algorithm: 'HS512' });
}

function sendCommentToServer(youtubeMessage, client) {
    // If STOMP client is not connected, just print to console
    if (!client || !client.connected) {
        console.log(`[Dry Run] ${youtubeMessage.authorDetails.displayName}: ${youtubeMessage.snippet.displayMessage}`);
        return;
    }

    const chatMessage = {
        sender: {
            id: youtubeMessage.authorDetails.channelId,
            name: youtubeMessage.authorDetails.displayName,
            profileImageUrl: youtubeMessage.authorDetails.profileImageUrl
        },
        content: youtubeMessage.snippet.displayMessage,
        streamId: ROOM_ID
    };

    const sendDestination = `/publish/${ROOM_ID}`;
    client.send(sendDestination, { 'content-type': 'application/json' }, JSON.stringify(chatMessage));
    console.log(`[Sent] ${chatMessage.sender.name}: ${chatMessage.content}`);
}

// ----------------------------------------------------------------------------
// 🔌 STOMP Connection Logic
// ----------------------------------------------------------------------------

async function connect(enableServer = false) {
    if (!YOUTUBE_VIDEO_ID) {
        console.error('🚨 Please set YOUTUBE_VIDEO_ID in your .env file.');
        return;
    }

    if (enableServer) {
        const token = generateToken();
        const socket = new SockJS(WEBSOCKET_URL);
        stompClient = Stomp.over(socket);
        // stompClient.debug = null; // Disable STOMP debug messages

        const headers = {
            'Authorization': `Bearer ${token}`,
        };

        stompClient.connect(headers, () => onConnected(stompClient), onError);
    } else {
        console.log(' WebSocket connection is disabled. Running in dry-run mode.');
        onConnected(null); // Test without server connection
    }
}

function onConnected(client) {
    if (client) {
        console.log('✅ STOMP connection successful.');
        const subscriptionDestination = `/topic/stream/${ROOM_ID}/message`;
        client.subscribe(subscriptionDestination, (message) => {
            // console.log(`Received message from server: ${message.body}`);
        });
        console.log(`Subscribed to ${subscriptionDestination}`);
    }

    const replayer = new ChatReplayer(client);
    replayer.start();
}

function onError(error) {
    console.error('Connection error:', error);
    console.log('Attempting to reconnect in 5 seconds...');
    setTimeout(() => connect(true), 5000);
}

// --- ▶️ Main Execution ---
console.log(`--- YouTube Chat Replayer from File ---`);
// Pass true to actually send to the server, or false (or leave empty) to just print to the console.
connect(true);