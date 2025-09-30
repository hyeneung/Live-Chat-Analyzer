require('dotenv').config();
import SockJS from 'sockjs-client';
import Stomp from 'stompjs';
import jwt from 'jsonwebtoken';

// --- Configuration ---
const SECRET_KEY = process.env.SECRET_KEY;
const USER_ID = "chatbot";
const USER_NAME = "ChatBot";
const PROFILE_IMAGE_URL = "https://placehold.co/100x100/E83422/FFFFFF?text=BOT";
const ROOM_ID = "1";
const WEBSOCKET_URL = `http://localhost:8090/ws`; // Use http for SockJS

let stompClient = null;

// --- Helper Functions ---
function generateToken() {
    const payload = {
        sub: USER_ID,
        exp: Math.floor(Date.now() / 1000) + (60 * 60) // Token expires in 1 hour
    };
    return jwt.sign(payload, SECRET_KEY, { algorithm: 'HS256' });
}

function connect() {
    const token = generateToken();
    const socket = new SockJS(WEBSOCKET_URL);
    stompClient = Stomp.over(socket);

    const headers = {
        'Authorization': `Bearer ${token}`,
    };

    stompClient.connect(headers, onConnected, onError);
}

function onConnected() {
    console.log('STOMP connection successful.');

    // Subscribe to the chat message topic
    const subscriptionDestination = `/topic/stream/${ROOM_ID}/message`;
    stompClient.subscribe(subscriptionDestination, onMessageReceived);
    console.log(`Subscribed to ${subscriptionDestination}`);

    // Start sending messages
    sendMessages();
}

function onError(error) {
    console.error('Connection error:', error);
    console.log('Attempting to reconnect in 5 seconds...');
    setTimeout(connect, 5000);
}

function onMessageReceived(message) {
    console.log(`Received message body: ${message.body}`);
}

function sendMessages() {
    console.log('Starting to send messages...');
    let i = 0;
    const intervalId = setInterval(() => {
        if (i >= 10) {
            clearInterval(intervalId);
            console.log('Finished sending messages.');
            disconnect();
            return;
        }

        const chatMessage = {
            sender: {
                id: USER_ID,
                name: USER_NAME,
                profileImageUrl: PROFILE_IMAGE_URL
            },
            content: `Hello from the JS bot! Message ${i}`,
            streamId: ROOM_ID
        };
        const sendDestination = `/publish/${ROOM_ID}`;
        stompClient.send(sendDestination, { 'content-type': 'application/json' }, JSON.stringify(chatMessage));
        console.log(`Sent message ${i} to ${sendDestination}`);
        i++;
    }, 2000);
}

function disconnect() {
    if (stompClient && stompClient.connected) {
        stompClient.disconnect(() => {
            console.log('Disconnected from STOMP.');
        });
    }
}

// --- Main execution ---
console.log(`Initializing client to connect to ${WEBSOCKET_URL}...`);
connect();
