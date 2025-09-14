<template>
  <!-- 
    This is the main container for a single broadcast card.
    It uses Tailwind CSS for styling, including background color, rounded corners, shadow, and a hover effect.
    `flex flex-col h-full` ensures that cards in the same row have the same height and the content inside is laid out in a column.
  -->
  <div class="bg-gray-800 rounded-lg overflow-hidden shadow-lg transform hover:scale-105 transition-transform duration-300 flex flex-col h-full">
    
    <!-- Top section of the card containing the thumbnail image. -->
    <div class="relative">
      <!-- The thumbnail image. `:src` binds the image source to the broadcast data. -->
      <img :src="broadcast.thumbnailUrl" alt="Stream Thumbnail" class="w-full h-48 object-cover">
      <!-- "LIVE" badge, positioned at the top-left corner. -->
      <div class="absolute top-2 left-2 bg-red-600 text-white text-xs font-bold px-2 py-1 rounded-md">LIVE</div>
      <!-- Viewer count badge, positioned at the top-right corner. -->
      <div class="absolute top-2 right-2 bg-black bg-opacity-50 text-white text-xs font-bold px-2 py-1 rounded-md flex items-center">
        <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 mr-1" viewBox="0 0 20 20" fill="currentColor"><path d="M13 6a3 3 0 11-6 0 3 3 0 016 0zM18 8a2 2 0 11-4 0 2 2 0 014 0zM14 15a4 4 0 00-8 0v3h8v-3zM6 8a2 2 0 11-4 0 2 2 0 014 0zM16 18v-3a5.972 5.972 0 00-.75-2.906A3.005 3 0 0115 15v3h1zM4.75 12.094A5.973 5.973 0 004 15v3H3v-3a3.005 3.005 0 011.25-2.406z" /></svg>
        <span :class="{'viewer-count-animation': isAnimatingViewerCount}">{{ broadcast.viewerCount }}</span>
      </div>
    </div>

    <!-- Bottom section of the card containing the stream information. -->
    <!-- `flex-grow flex flex-col` makes this section fill the remaining vertical space. -->
    <div class="p-4 flex-grow flex flex-col">
      <!-- Stream title. `truncate` class prevents long titles from breaking the layout. -->
      <h3 class="text-lg font-bold mb-2 truncate">{{ broadcast.title }}</h3>
      
      <!-- Host information section. -->
      <div class="flex items-center mb-4">
        <img :src="broadcast.host.profilePic" class="w-8 h-8 rounded-full mr-3">
        <span class="text-gray-300">{{ broadcast.host.name }}</span>
      </div>

      <!-- Stream start time. `mt-auto` pushes this to the bottom of the card. -->
      <div class="text-sm text-gray-400 mt-auto">
        <span>{{ formatTimeAgo(broadcast.startTime) }} 전 시작</span>
      </div>

      <!-- "Watch" button. -->
      <button @click="join" class="w-full mt-4 bg-indigo-500 hover:bg-indigo-600 text-white font-bold py-2 px-4 rounded-lg transition duration-300">
        시청하기
      </button>
    </div>
  </div>
</template>

<script setup>
// Import functions from Vue to define component properties and events.
import { defineProps, defineEmits, ref, watch } from 'vue';

// --- Component Properties (Props) ---
// Defines that this component expects a `broadcast` object from its parent.
const props = defineProps({
  broadcast: {
    type: Object,
    required: true,
  },
});

// --- Component Events (Emits) ---
// Defines that this component can emit a `join` event.
const emit = defineEmits(['join']);

const isAnimatingViewerCount = ref(false);

watch(() => props.broadcast.viewerCount, (newVal, oldVal) => {
  if (newVal !== oldVal && oldVal !== undefined) {
    isAnimatingViewerCount.value = true;
    setTimeout(() => {
      isAnimatingViewerCount.value = false;
    }, 500); // Animation duration is 0.5s
  }
});

// --- Methods ---
// A utility function to format a date into a relative time string (e.g., "1 hour ago").
const formatTimeAgo = (date) => {
    const now = new Date();
    const seconds = Math.floor((now - new Date(date)) / 1000);
    let interval = seconds / 31536000;
    if (interval > 1) return Math.floor(interval) + "년";
    interval = seconds / 2592000;
    if (interval > 1) return Math.floor(interval) + "달";
    interval = seconds / 86400;
    if (interval > 1) return Math.floor(interval) + "일";
    interval = seconds / 3600;
    if (interval > 1) return Math.floor(interval) + "시간";
    interval = seconds / 60;
    if (interval > 1) return Math.floor(interval) + "분";
    return Math.floor(seconds) + "초";
};

// This function is called when the "Watch" button is clicked.
const join = () => {
  // It emits the `join` event to the parent component, passing the broadcast ID as a payload.
  emit('join', props.broadcast.id);
};
</script>

<style scoped>
/* Viewer count animation */
.viewer-count-animation {
  display: inline-block; /* Required for transform to work */
  animation: viewerCountChange 0.5s ease-out;
}

@keyframes viewerCountChange {
  0% {
    transform: scale(1);
    color: inherit;
  }
  50% {
    transform: scale(1.2);
    color: #a0a390; /* A vibrant green for emphasis */
  }
  100% {
    transform: scale(1);
    color: inherit;
  }
}
</style>