import 'dotenv/config';
import SockJS from 'sockjs-client';
import Stomp from 'stompjs';
import jwt from 'jsonwebtoken';

// --- ⚙️ Configuration ---
const SECRET_KEY = process.env.SECRET_KEY;
const WEBSOCKET_URL = process.env.WEBSOCKET_URL;
const NUM_CLIENTS = parseInt(process.env.NUM_CLIENTS, 10) || 10;
const MESSAGES_PER_SECOND_PER_CLIENT = parseFloat(process.env.MESSAGES_PER_SECOND_PER_CLIENT) || 0.5; // Avg messages per second per client
const ROOM_ID = "1"; // Room to connect to
const RAMP_UP_TIME_SECONDS = parseInt(process.env.RAMP_UP_TIME_SECONDS, 10) || 10; // Time to ramp up all clients

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
// 💬 STOMP Message Sending Logic (for a single client)
// ----------------------------------------------------------------------------

function startSingleClientMessaging(client, userId) {
    const messageDestination = `/publish/${ROOM_ID}`;
    const baseInterval = 1000 / MESSAGES_PER_SECOND_PER_CLIENT;

    const intervalId = setInterval(() => {
        if (client && client.connected) {
            try {
                const message = {
                    sender: {
                        id: `${userId}`,
                        name: `Load Tester ${userId}`,
                        profileImageUrl: 'https://uxwing.com/wp-content/themes/uxwing/download/communication-chat-call/chatbot-icon.png'
                    },
                    content: `(Load Test) Message from client ${userId} at ${new Date().toISOString()}`,
                    streamId: ROOM_ID
                };
                client.send(messageDestination, { 'content-type': 'application/json' }, JSON.stringify(message));
            } catch (e) {
                console.error(`[Client ${userId}] Error sending message:`, e);
            }
        } else {
            clearInterval(intervalId);
        }
    }, baseInterval);

    return intervalId;
}


// ----------------------------------------------------------------------------
// 🔌 STOMP Connection Logic for a single client
// ----------------------------------------------------------------------------

function connectClient(userId) {
    return new Promise((resolve) => {
        console.log(`[Client ${userId}] Initializing...`);

        if (!WEBSOCKET_URL || !SECRET_KEY) {
            return resolve({ status: 'failed', userId, reason: new Error('Missing WEBSOCKET_URL or SECRET_KEY') });
        }

        const token = generateToken(userId.toString());
        const socket = new SockJS(WEBSOCKET_URL);
        const stompClient = Stomp.over(socket);
        stompClient.debug = null; // Disable STOMP debug messages

        let hasConnected = false;
        const headers = { 'Authorization': `Bearer ${token}` };

        stompClient.connect(headers,
            () => { // onConnected
                hasConnected = true;
                console.log(`[Client ${userId}] ✅ STOMP connection successful.`);
                const subscriptionDestination = `/topic/stream/${ROOM_ID}/message`;
                stompClient.subscribe(subscriptionDestination, () => {});
                
                const intervalId = startSingleClientMessaging(stompClient, userId);
                
                resolve({ status: 'connected', client: stompClient, userId, intervalId });
            },
            (error) => { // onError
                if (hasConnected) {
                    console.error(`[Client ${userId}] 🔌 Connection lost:`, error.body || error);
                } else {
                    console.error(`[Client ${userId}] ❌ Initial connection failed:`, error);
                    const errorMessage = (typeof error === 'object' && error.body) ? error.body : String(error);
                    resolve({ status: 'failed', userId, reason: new Error(errorMessage) });
                }
            }
        );
    });
}

// ----------------------------------------------------------------------------
// ▶️ Main Execution
// ----------------------------------------------------------------------------

async function runLoadTest() {
    console.log(`--- WebSocket Load Tester ---`);
    console.log(`🚀 Starting ${NUM_CLIENTS} clients over ${RAMP_UP_TIME_SECONDS} seconds...`);

    const rampUpInterval = (RAMP_UP_TIME_SECONDS * 1000) / NUM_CLIENTS;

    const connectionPromises = [];
    for (let i = 1; i <= NUM_CLIENTS; i++) {
        await new Promise(resolve => setTimeout(resolve, rampUpInterval));
        connectionPromises.push(connectClient(i));
    }

    try {
        const results = await Promise.all(connectionPromises);

        const successfulConnections = results.filter(res => res.status === 'connected');
        const failedConnections = results.filter(res => res.status === 'failed');

        console.log(`\n✅ ${successfulConnections.length} / ${NUM_CLIENTS} clients connected successfully.`);

        if (failedConnections.length > 0) {
            console.error(`\n🚨 ${failedConnections.length} / ${NUM_CLIENTS} clients failed to connect.`);
            failedConnections.slice(0, 5).forEach((result, i) => {
                console.error(`  - Failure ${i + 1} (Client ${result.userId}): ${result.reason.message || result.reason}`);
            });
            if (failedConnections.length > 5) {
                console.error('  - ... and more failures.');
            }
        }

        function shutdown() {
            console.log('\n[Shutdown] Test duration finished. Disconnecting all clients...');
            successfulConnections.forEach(({ client, userId, intervalId }) => {
                clearInterval(intervalId);
                if (client && client.connected) {
                    client.disconnect(() => console.log(`[Client ${userId}] Disconnected.`));
                }
            });
            setTimeout(() => {
                console.log('[Shutdown] Exiting process.');
                process.exit(0);
            }, 2000);
        }

        if (successfulConnections.length > 0) {
            console.log(`\nAll connection attempts are complete. Running test for 10 seconds before automatic shutdown...`);
            setTimeout(shutdown, 10000);
        } else {
            console.error('\n❌ No clients could connect. Exiting now.');
            process.exit(1);
        }

    } catch (error) {
        console.error('\n❌ An unexpected and critical error occurred during the load test:', error);
        process.exit(1);
    }
}

runLoadTest();
