<template>
  <div v-if="currentStream" class="relative h-screen w-screen overflow-hidden">
    <!-- Video Player (Background) -->
    <div class="absolute inset-0 bg-black flex items-center justify-center">
      <div class="aspect-w-16 aspect-h-9 w-full">
        <p class="text-gray-400 text-2xl text-center">실시간 영상 스트리밍 화면</p>
      </div>
    </div>

    <!-- Overlay Content -->
    <div class="absolute inset-0 flex justify-between items-start p-4 md:p-8 pointer-events-none">
      <!-- Left Side: Info and Back Button -->
      <div class="w-full lg:w-2/3 text-white pointer-events-auto">
        <button @click="goHome" class="mb-4 bg-gray-800 bg-opacity-50 hover:bg-opacity-75 text-white font-bold py-2 px-4 rounded-lg transition duration-300">
          &larr; 목록으로 돌아가기
        </button>
        <h2 class="text-3xl font-bold text-shadow">{{ currentStream.title }}</h2>
        <div class="flex items-center mt-2 text-gray-200 text-shadow">
          <img :src="currentStream.host.profilePic" class="w-10 h-10 rounded-full mr-3">
          <span class="font-semibold">{{ currentStream.host.name }}</span>
          <span class="mx-3">|</span>
          <span>시청자 {{ currentStream.viewerCount }}명</span>
        </div>
      </div>

      <!-- Right Side: Chat and Analysis -->
      <div class="w-full lg:w-1/3 h-full flex flex-col gap-6 pointer-events-auto">
        <transition name="fade">
            <ChatAnalysis 
                v-if="isAnalysisVisible" 
                :analysis-data="analysisData" 
                @toggle-visibility="toggleAnalysisVisibility"
            />
            <div v-else>
                <button @click="toggleAnalysisVisibility" class="bg-gray-800 bg-opacity-70 hover:bg-opacity-90 text-white font-bold py-2 px-4 rounded-lg transition duration-300 backdrop-blur-sm">
                    실시간 댓글 분석 보기
                </button>
            </div>
        </transition>
        <ChatBox :messages="comments" @send-message="handleSendMessage" class="flex-grow min-h-0"/>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import api from '@/api';
import socket from '@/api/socket'; // Import the socket client
import ChatBox from '../components/ChatBox.vue';
import ChatAnalysis from '../components/ChatAnalysis.vue';

// --- Vue Router and Route ---
const route = useRoute();
const router = useRouter();
const streamId = route.params.id;

// --- Component State ---
const currentStream = ref(null);
const comments = ref([]); // Holds the list of chat messages
const analysisData = ref({}); // Holds real-time analysis data
const isAnalysisVisible = ref(true); // Controls visibility of the analysis component

// --- User Information State ---
const userInfo = ref({
    id: null,
    name: 'Anonymous',
    profileImageUrl: 'https://placehold.co/100x100/cccccc/FFFFFF?text=U'
});

// --- Navigation ---
const goHome = () => {
  leaveStream();
  router.push('/');
};

// --- API Interaction ---
let hasLeft = false;
/**
 * Sends a request to the backend to notify that the user is leaving the stream.
 * Uses `fetch` with `keepalive` to ensure the request is sent even if the page is closing.
 */
const leaveStream = () => {
  if (hasLeft || !streamId) return;
  hasLeft = true;

  const url = `${process.env.VUE_APP_BACKEND_URL}/api/v1/streams/leave`;
  const accessToken = localStorage.getItem('accessToken');
  const headers = { 'Content-Type': 'application/json' };
  if (accessToken) {
    headers['Authorization'] = `Bearer ${accessToken}`;
  }
  const body = JSON.stringify({ streamId: streamId });

  try {
    fetch(url, {
      method: 'POST',
      headers,
      body,
      keepalive: true,
    });
  } catch (e) {
    console.error('Error dispatching leaveStream request:', e);
  }
};

/**
 * Toggles the visibility of the chat analysis component.
 */
const toggleAnalysisVisibility = () => {
    isAnalysisVisible.value = !isAnalysisVisible.value;
}

/**
 * Handles sending a new chat message through the WebSocket connection.
 * Uses the pre-fetched user information.
 * @param {string} content - The text content of the message.
 */
const handleSendMessage = (content) => {
    if (!userInfo.value.id) {
        console.error("User information not available. Cannot send message.");
        return;
    }

    const message = {
        sender: {
            id: userInfo.value.id,
            name: userInfo.value.name,
            profileImageUrl: userInfo.value.profileImageUrl
        },
        content: content,
        streamId: streamId,
    };
    socket.sendMessage(`/publish/${streamId}`, message);
};


// --- Lifecycle Hooks ---
onMounted(async () => {
  // Fetch initial stream details from the API
  try {
    const response = await api.get(`/api/v1/streams/${streamId}`);
    currentStream.value = response.data;
  } catch (error) {
    console.error('Failed to fetch stream details:', error);
    router.push('/'); 
    return;
  }

  // --- WebSocket Connection and User Info Parsing ---
  const accessToken = localStorage.getItem('accessToken');
  if (accessToken) {
    // Parse token and local storage once
    try {
        const payload = JSON.parse(atob(accessToken.split('.')[1]));
        userInfo.value.id = payload.sub;
        userInfo.value.name = localStorage.getItem("userName") || 'Anonymous';
        userInfo.value.profileImageUrl = localStorage.getItem("userProfileImage") || 'https://placehold.co/100x100/cccccc/FFFFFF?text=U';
    } catch (e) {
        console.error("Failed to decode token or get user info:", e);
    }

    socket.connect(
      accessToken,
      () => {
        // On successful connection, subscribe to relevant topics
        
        // Subscribe to the main chat topic for this stream
        socket.subscribe(`/topic/stream/${streamId}/message`, (message) => {
          // Add incoming messages to the comments array, mapping the new structure
          comments.value.push({
            id: Date.now() + Math.random(), // Create a unique key for the v-for
            user: { name: message.sender.name, profilePic: message.sender.profileImageUrl },
            text: message.content,
          });
        });

        // Subscribe to viewer count updates
        socket.subscribe(`/topic/stream/${streamId}/user-count`, (message) => {
          if (currentStream.value) {
            currentStream.value.viewerCount = message.userCount;
          }
        });

        // Subscribe to the real-time analysis topic
        socket.subscribe(`/topic/stream/${streamId}/analysis`, (message) => {
          // console.log('Received analysis data:', message);
          analysisData.value = message; // Update the analysis data state
        });

        // Subscribe to the real-time summary topic
        socket.subscribe(`/topic/stream/${streamId}/summary`, (message) => {
          console.log('Received summary data:', message);
          // You might want to store this in a ref similar to analysisData
          // For now, just logging to console as requested
        });
      },
      (error) => {
        console.error('WebSocket connection failed:', error);
      }
    );
  } else {
      console.error("Authentication token not found, WebSocket not connected.");
  }
});

onBeforeUnmount(() => {
  // Clean up resources before the component is destroyed
  leaveStream();
  socket.disconnect();
});

</script>

<style scoped>
.text-shadow {
  text-shadow: 1px 1px 3px rgba(0,0,0,0.5);
}
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
