import 'dotenv/config';
import SockJS from 'sockjs-client';
import Stomp from 'stompjs';
import jwt from 'jsonwebtoken';
import fs from 'fs/promises';

// --- ⚙️ Configuration ---
const SECRET_KEY = process.env.SECRET_KEY;
const CHATBOT_USER_ID = "1"; // Differentiate from the other chatbot
const ROOM_ID = "1"; // Use a different room for the shopping chat
const WEBSOCKET_URL = process.env.WEBSOCKET_URL;
const CHAT_HISTORY_FILE = 'chat-history-shopping.json';
const SEND_INTERVAL_MS = 2000; // Send a message every 2 seconds

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

    async start() {
        if (this.isReplaying) return;

        console.log(`▶️ Starting chat replay from file: ${CHAT_HISTORY_FILE}`);

        try {
            const fileContent = await fs.readFile(CHAT_HISTORY_FILE, 'utf-8');
            this.commentBuffer = JSON.parse(fileContent);
            console.log(`[Loader] Successfully loaded ${this.commentBuffer.length} comments from file.`);
        } catch (e) {
            console.error(`[Loader] Error reading or parsing chat history file: ${e.message}`);
            console.error(`[Loader] Please make sure you have run 'python naver-shopping/crawler.py' first.`);
            return;
        }

        if (this.commentBuffer.length === 0) {
            console.log('[Loader] No comments to replay.');
            return;
        }

        this.isReplaying = true;
        this._processBuffer();
    }

    stop() {
        this.isReplaying = false;
        console.log('⏹️ Chat replay stopped.');
        if (this.stompClient && this.stompClient.connected) {
            this.stompClient.disconnect(() => {
                console.log('Disconnected from STOMP.');
            });
        }
    }

    async _processBuffer() {
        while (this.isReplaying && this.currentBufferIndex < this.commentBuffer.length) {
            const currentComment = this.commentBuffer[this.currentBufferIndex];
            
            // Wait for the fixed interval before sending the next message
            await new Promise(resolve => setTimeout(resolve, SEND_INTERVAL_MS));

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

function sendCommentToServer(chatMessage, client) {
    if (!client || !client.connected) {
        console.log(`[Dry Run] ${chatMessage.authorDetails.displayName}: ${chatMessage.snippet.displayMessage}`);
        return;
    }

    const messageToSend = {
        sender: {
            id: chatMessage.authorDetails.displayName, // Using name as ID as we don't have a unique channelId
            name: chatMessage.authorDetails.displayName,
            profileImageUrl: chatMessage.authorDetails.profileImageUrl
        },
        content: chatMessage.snippet.displayMessage,
        streamId: ROOM_ID
    };

    const sendDestination = `/publish/${ROOM_ID}`;
    client.send(sendDestination, { 'content-type': 'application/json' }, JSON.stringify(messageToSend));
    console.log(`[Sent] ${messageToSend.sender.name}: ${messageToSend.content}`);
}

// ----------------------------------------------------------------------------
// 🔌 STOMP Connection Logic
// ----------------------------------------------------------------------------

async function connect(enableServer = false) {
    if (!SECRET_KEY) {
        console.error('🚨 Please set SECRET_KEY in your .env file.');
        return;
    }

    if (enableServer) {
        const token = generateToken();
        const socket = new SockJS(WEBSOCKET_URL);
        stompClient = Stomp.over(socket);
        stompClient.debug = null; // Disable STOMP debug messages

        const headers = {
            'Authorization': `Bearer ${token}`,
        };

        stompClient.connect(headers, () => onConnected(stompClient), onError);
    } else {
        console.log('🔌 WebSocket connection is disabled. Running in dry-run mode.');
        onConnected(null);
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
console.log(`--- Naver Shopping Chat Replayer ---`);
// Pass true to send to the server, false for a dry run.
connect(true);