
import Stomp from 'stompjs';
import SockJS from 'sockjs-client';
import api from '@/api'; // Import the API client

class SocketClient {
    constructor() {
        this.stompClient = null;
        this.subscriptions = new Map();
        this.reconnecting = false; // Flag to prevent multiple reconnection attempts
    }

    async reissueToken() {
        const refreshToken = localStorage.getItem('refreshToken');
        if (!refreshToken) {
            console.error('No refresh token found for re-issuance.');
            return null;
        }

        try {
            const response = await api.post('/api/v1/users/reissue', { refreshToken });
            const newAccessToken = response.data.accessToken;
            const newRefreshToken = response.data.refreshToken;

            localStorage.setItem('accessToken', newAccessToken);
            localStorage.setItem('refreshToken', newRefreshToken);
            console.log('Tokens re-issued successfully.');
            return newAccessToken;
        } catch (error) {
            console.error('Failed to re-issue token:', error);
            // Clear tokens if re-issuance fails to force re-login
            localStorage.removeItem('accessToken');
            localStorage.removeItem('refreshToken');
            return null;
        }
    }

    connect(token, onConnected, onError) {
        if (this.reconnecting) {
            console.log('Already attempting to reconnect. Aborting new connection attempt.');
            return;
        }

        const socket = new SockJS(`${process.env.VUE_APP_CHAT_SERVER_URL}/ws`);
        this.stompClient = Stomp.over(socket);

        const headers = {
            'Authorization': `Bearer ${token}`,
        };

        this.stompClient.connect(
            headers,
            () => {
                this.reconnecting = false; // Reset flag on successful connection
                if (onConnected) {
                    onConnected();
                }
            },
            async (error) => {
                console.error('Connection error:', error);
                // Check if the error is due to JWT expiration (this might need refinement based on actual server error messages)
                // For now, we'll assume any connection error might warrant a token re-issuance attempt if not explicitly handled.
                // A more robust solution would involve specific error codes from the server.

                if (!this.reconnecting) { // Only attempt re-issuance if not already reconnecting
                    this.reconnecting = true;
                    console.log('Attempting to re-issue token and reconnect...');
                    const newAccessToken = await this.reissueToken();
                    if (newAccessToken) {
                        console.log('Reconnecting with new token...');
                        // Disconnect existing client before reconnecting
                        if (this.stompClient && this.stompClient.connected) {
                            this.stompClient.disconnect(() => {
                                this.subscriptions.clear();
                            });
                        }
                        this.connect(newAccessToken, onConnected, onError); // Reconnect with the new token
                    } else {
                        console.error('Token re-issuance failed. Cannot reconnect.');
                        this.reconnecting = false; // Reset flag if re-issuance fails
                        if (onError) {
                            onError(error); // Call original onError if re-issuance fails
                        }
                    }
                } else {
                    console.log('Reconnection attempt already in progress or failed.');
                    this.reconnecting = false; // Reset flag if this path is reached unexpectedly
                    if (onError) {
                        onError(error);
                    }
                }
            }
        );
    }

    disconnect() {
        if (this.stompClient) {
            this.stompClient.disconnect(() => {
                this.subscriptions.clear();
                this.reconnecting = false; // Ensure flag is reset on explicit disconnect
            });
        }
    }

    subscribe(destination, callback) {
        if (this.stompClient && this.stompClient.connected) {
            const subscription = this.stompClient.subscribe(destination, (message) => {
                callback(JSON.parse(message.body));
            });
            this.subscriptions.set(destination, subscription);
        }
    }

    unsubscribe(destination) {
        const subscription = this.subscriptions.get(destination);
        if (subscription) {
            subscription.unsubscribe();
            this.subscriptions.delete(destination);
        }
    }

    sendMessage(destination, message) {
        if (this.stompClient && this.stompClient.connected) {
            this.stompClient.send(destination, {}, JSON.stringify(message));
        } else {
            console.warn('STOMP client not connected. Message not sent:', message);
            // Optionally, attempt to re-issue token and reconnect if not connected
            // This would make sendMessage more resilient but adds complexity.
            // For now, we'll rely on the connect method's error handling.
        }
    }
}

export default new SocketClient();
