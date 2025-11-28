import 'dotenv/config';
import SockJS from 'sockjs-client';
import Stomp from 'stompjs';
import jwt from 'jsonwebtoken';

// --- ⚙️ Configuration ---
const SECRET_KEY = process.env.SECRET_KEY;
const WEBSOCKET_URL = process.env.WEBSOCKET_URL;
const NUM_CLIENTS = parseInt(process.env.NUM_CLIENTS, 10) || 10;
const ROOM_ID = "1"; // Room to connect to

// ----------------------------------------------------------------------------
// 🛠️ Helper Functions
// ----------------------------------------------------------------------------

function generateToken(userId) {
    const payload = {
        sub: userId,
        exp: Math.floor(Date.now() / 1000) + (60 * 60) // 1 hour expiration
    };
    return jwt.sign(payload, SECRET_KEY, { algorithm: 'HS512' });
}

// ----------------------------------------------------------------------------
// 🔌 STOMP Connection Logic for a single client
// ----------------------------------------------------------------------------

function connectClient(userId) {
    return new Promise((resolve, reject) => {
        console.log(`[Client ${userId}] Initializing...`);

        if (!WEBSOCKET_URL || !SECRET_KEY) {
            return reject(new Error(`[Client ${userId}] Missing WEBSOCKET_URL or SECRET_KEY in .env file.`));
        }

        const token = generateToken(userId.toString());
        const socket = new SockJS(WEBSOCKET_URL);
        const stompClient = Stomp.over(socket);
        stompClient.debug = null; // Disable STOMP debug messages

        const headers = {
            'Authorization': `Bearer ${token}`,
        };

        stompClient.connect(headers,
            () => { // onConnected
                console.log(`[Client ${userId}] ✅ STOMP connection successful.`);
                const subscriptionDestination = `/topic/stream/${ROOM_ID}/message`;
                stompClient.subscribe(subscriptionDestination, (message) => {
                    // This client is idle, just listening.
                });
                console.log(`[Client ${userId}] Subscribed to ${subscriptionDestination}`);
                resolve(stompClient); // Resolve the promise with the client
            },
            (error) => { // onError
                console.error(`[Client ${userId}] ❌ Connection error:`, error);
                reject(error);
            }
        );
    });
}

// ----------------------------------------------------------------------------
// ▶️ Main Execution
// ----------------------------------------------------------------------------

async function runLoadTest() {
    console.log(`--- WebSocket Load Tester ---`);
    console.log(`🚀 Starting ${NUM_CLIENTS} concurrent clients...`);

    const connectionPromises = [];
    for (let i = 1; i <= NUM_CLIENTS; i++) {
        // Start each connection with a small delay to avoid overwhelming the server instantly
        await new Promise(resolve => setTimeout(resolve, 50));
        connectionPromises.push(connectClient(i + 100)); // Use user IDs from 101 onwards
    }

    try {
        const clients = await Promise.all(connectionPromises);
        console.log(`\n✅ All ${clients.length} clients connected successfully!`);
        console.log('Load test is running. Press Ctrl+C to stop.');

        // Keep the script running
        process.stdin.resume();
        process.on('SIGINT', () => {
            console.log('\nSIGINT received. Disconnecting clients...');
            clients.forEach((client, index) => {
                if (client && client.connected) {
                    client.disconnect(() => console.log(`[Client ${index + 101}] Disconnected.`));
                }
            });
            process.exit(0);
        });

    } catch (error) {
        console.error('\n❌ An error occurred during the load test:', error.message);
        console.error('Not all clients may have connected. Please check the server logs and your configuration.');
        process.exit(1);
    }
}

runLoadTest();
