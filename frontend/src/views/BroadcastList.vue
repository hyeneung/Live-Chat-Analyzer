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
import axios from 'axios';

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
// A variable to hold the ID of the interval timer for the simulation.
let viewerCountInterval = null;

// --- Methods ---
// This function is called when the `join` event is received from a BroadcastCard.
const joinBroadcast = (broadcastId) => {
  // Check if a user is logged in.
  if (props.user) {
    // If logged in, navigate to the corresponding room URL.
    router.push(`/room/${broadcastId}`);
  } else {
    // If not logged in, emit an event to the parent (App.vue) to show the login modal.
    emit('show-login');
  }
};

const fetchBroadcasts = async () => {
  try {
    const response = await axios.get(`${process.env.VUE_APP_BACKEND_URL}/api/v1/streams`, {
      params: { page: 0 }
    });

    broadcasts.value = response.data.streams.map(stream => ({
      id: stream.id,
      title: stream.title,
      host: {
        name: stream.hostname,
        profilePic: stream.hostprofile,
      },
      thumbnailUrl: stream.thumbnailUrl,
      viewerCount: Math.floor(Math.random() * 500) + 50, // Mock viewer count
      startTime: stream.createdAt,
    }));
  } catch (error) {
    console.error('Error fetching broadcasts:', error);
    // Handle error appropriately, e.g., show a message to the user
  }
};


// This function simulates real-time updates for viewer counts.
const startLobbySimulation = () => {
    // setInterval repeatedly calls a function with a fixed time delay between each call.
    viewerCountInterval = setInterval(() => {
        // For each stream, randomly change the viewer count slightly.
        broadcasts.value.forEach(stream => {
            if (Math.random() > 0.5) {
                const change = Math.floor(Math.random() * 5) - 2; // -2 to +2
                stream.viewerCount = Math.max(0, stream.viewerCount + change);
            }
        });
    }, 2000); // Update every 2 seconds
};

// --- Lifecycle Hooks ---
// `onMounted` is a function that runs after the component has been inserted into the DOM.
onMounted(() => {
  fetchBroadcasts();
  // Start the real-time simulation.
  startLobbySimulation();
});

// `onBeforeUnmount` is a function that runs right before the component is removed from the DOM.
onBeforeUnmount(() => {
  // This is important to prevent memory leaks.
  // It stops the simulation when the user navigates away from this page.
  clearInterval(viewerCountInterval);
});

</script>