<template>
  <!-- This is the main container for the Broadcast List page (the lobby). -->
  <div>
    <h2 class="text-3xl font-bold mb-6">진행중인 방송</h2>

    <!-- 
      A responsive grid layout for the broadcast cards.
      It changes the number of columns based on the screen size (e.g., 1 on small, 2 on medium, etc.).
    -->
    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
      
      <!-- 
        Loop through the `broadcasts` array using v-for.
        For each broadcast object, render a BroadcastCard component.
        `:key` is a special attribute required by Vue for list rendering, for performance and identity.
        `:broadcast` passes the broadcast object down to the child component as a prop.
        `@join` listens for the `join` event emitted by the BroadcastCard.
      -->
      <BroadcastCard 
        v-for="broadcast in broadcasts"
        :key="broadcast.id"
        :broadcast="broadcast" 
        @join="joinBroadcast" 
      />
    </div>
  </div>
</template>

<script setup>
// Import necessary functions and components from Vue and other files.
import { ref, onMounted, onBeforeUnmount, defineProps, defineEmits } from 'vue';
import { useRouter } from 'vue-router';
import BroadcastCard from '../components/BroadcastCard.vue';
import api from '@/api';

// --- Component Properties (Props) ---
// This component receives the `user` object from its parent (App.vue via router-view).
const props = defineProps({
  user: {
    type: Object,
    default: null,
  },
});

// --- Component Events (Emits) ---
// Defines that this component can emit a `show-login` event to its parent.
const emit = defineEmits(['show-login']);

// --- Router ---
// Get the router instance to navigate to the broadcast room.
const router = useRouter();

// --- State Management ---
// A reactive array to hold the list of broadcast streams.
const broadcasts = ref([]);
// A variable to hold the EventSource instance for SSE.
let eventSource = null;

// --- Methods ---
// This function is called when the `join` event is received from a BroadcastCard.
const joinBroadcast = async (broadcastId) => {
  // Check if a user is logged in.
  if (props.user) {
    try {
      // If logged in, call the API to enter the stream.
      // The request body is { streamId: broadcastId }.
      // The Authorization header is automatically attached by the Axios interceptor in api.js.
      await api.post(`/api/v1/streams/enter`, { streamId: broadcastId });
      
      // After successfully entering, navigate to the corresponding room URL.
      router.push(`/room/${broadcastId}`);
    } catch (error) {
      console.error('Error entering broadcast:', error);
      // @TODO: Handle the error appropriately, e.g., show a notification to the user.
    }
  } else {
    // If not logged in, emit an event to the parent (App.vue) to show the login modal.
    emit('show-login');
  }
};

const updateUserCount = (update) => {
  const broadcastToUpdate = broadcasts.value.find(
    b => String(b.id) === String(update.streamId)
  );

  if (broadcastToUpdate) {
    broadcastToUpdate.viewerCount = update.userCount;
  } else {
    console.warn(`Stream with ID ${update.streamId} not found.`);
  }
};

const fetchBroadcasts = async () => {
  try {
    const response = await api.get(`/api/v1/streams`, {
      params: { page: 0 },
      skipAuth: true
    });

    broadcasts.value = response.data.streams.map(stream => ({
      id: stream.id,
      title: stream.title,
      host: {
        name: stream.hostname,
        profilePic: stream.hostprofile,
      },
      thumbnailUrl: stream.thumbnailUrl,
      viewerCount: stream.viewerCount,
      startTime: stream.createdAt,
    }));
  } catch (error) {
    console.error('Error fetching broadcasts:', error);
    // Handle error appropriately, e.g., show a message to the user
  }
};

const setupSse = () => {
  // Prevent creating duplicate connections, e.g., during HMR in development.
  if (eventSource) {
    console.log('SSE connection already exists, skipping setup.');
    return;
  }
  // Establish an SSE connection to the backend.
  eventSource = new EventSource(`${process.env.VUE_APP_BACKEND_URL}/api/v1/streams/subscribe`);

  // Handle incoming 'userCountUpdate' events.
  eventSource.addEventListener('userCountUpdate', (event) => {
    const update = JSON.parse(event.data);
    console.log('sse message received:', update);
    updateUserCount(update);
  });

  // Optional: Handle connection open and error events.
  eventSource.onopen = () => {
    console.log('SSE connection established.');
  };

  eventSource.onerror = (error) => {
    console.error('SSE error:', error);
    // The browser will automatically try to reconnect.
  };
};


// --- Lifecycle Hooks ---
// `onMounted` is a function that runs after the component has been inserted into the DOM.
onMounted(async () => {
  // Wait for the initial list of broadcasts to be fetched.
  await fetchBroadcasts();
  // Now that we have the broadcasts, set up the Server-Sent Events connection.
  setupSse();
});

// `onBeforeUnmount` is a function that runs right before the component is removed from the DOM.
onBeforeUnmount(() => {
  // This is important to prevent memory leaks.
  // It closes the SSE connection when the user navigates away from this page.
  if (eventSource) {
    eventSource.close();
    eventSource = null; // Reset the variable to allow for a new connection if the component remounts.
    console.log('SSE connection closed.');
  }
});

</script>