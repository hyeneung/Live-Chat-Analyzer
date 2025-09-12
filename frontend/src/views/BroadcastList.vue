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

// This function generates mock data for the broadcast list.
const generateMockData = () => {
    const hosts = [
        { name: '게임스트리머', profilePic: 'https://placehold.co/100x100/7E22CE/FFFFFF?text=G' },
        { name: '뮤직크리에이터', profilePic: 'https://placehold.co/100x100/DB2777/FFFFFF?text=M' },
        { name: '일상브이로거', profilePic: 'https://placehold.co/100x100/16A34A/FFFFFF?text=V' },
        { name: 'IT전문가', profilePic: 'https://placehold.co/100x100/D97706/FFFFFF?text=IT' },
        { name: '요리왕', profilePic: 'https://placehold.co/100x100/DC2626/FFFFFF?text=C' },
        { name: '프로그래머', profilePic: 'https://placehold.co/100x100/0284C7/FFFFFF?text=P' },
    ];
    const titles = [
        'Vue.js 3 완벽 마스터하기', '오늘 밤은 치킨 먹방!', '여러분의 고민을 들어드려요', 
        '새로운 인디게임 플레이', '직장인 리얼 라이프', '코딩하며 함께 밤새기'
    ];

    broadcasts.value = Array.from({ length: 6 }).map((_, i) => ({
        id: i + 1,
        title: titles[i % titles.length],
        host: hosts[i % hosts.length],
        thumbnailUrl: `https://placehold.co/600x400/1F2937/FFFFFF?text=방송+${i+1}`,
        viewerCount: Math.floor(Math.random() * 500) + 50,
        startTime: new Date(Date.now() - Math.random() * 1000 * 60 * 120), // Random time within the last 2 hours
    }));
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
  // Generate the initial data for the list.
  generateMockData();
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