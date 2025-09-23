<template>
  <!-- 
    Main container for the analysis component.
    It has a semi-transparent, blurred background to create a modern overlay effect.
  -->
  <div class="bg-gray-800 bg-opacity-70 p-4 rounded-lg backdrop-blur-sm">
    
    <!-- Header section containing the title and the toggle button. -->
    <div class="flex justify-between items-center mb-4">
      <h3 class="text-xl font-bold">실시간 댓글 분석</h3>
      <!-- This button allows the user to collapse or expand the analysis view. -->
      <button @click="toggle" class="text-gray-400 hover:text-white transition">
        <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
        </svg>
      </button>
    </div>

    <!-- Display analysis data if available -->
    <div v-if="analysisData && analysisData.totalCount > 0">
      <!-- Ratios Analysis Section -->
      <div class="mt-4">
        <!-- Loop through each ratio and create a progress bar for it. -->
        <div v-for="(ratio, category) in analysisData.ratios" :key="category" class="mb-2">
          <div class="flex justify-between mb-1">
            <span class="text-base font-medium text-gray-300 capitalize">{{ category }}</span>
            <span class="text-sm font-medium text-gray-400">{{ (ratio * 100).toFixed(1) }}%</span>
          </div>
          <!-- The container for the ratio progress bar. -->
          <div class="w-full bg-gray-700 rounded-full h-2.5">
            <!-- The actual progress bar. Its width is dynamically bound to the ratio value. -->
            <div class="bg-indigo-500 h-2.5 rounded-full progress-bar" :style="{ width: (ratio * 100).toFixed(1) + '%' }"></div>
          </div>
        </div>
      </div>
      <div class="flex justify-end mt-4">
        <p class="text-sm text-gray-400">총 댓글 수: {{ analysisData.totalCount }}</p>
      </div>
    </div>
    <div v-else class="flex-grow flex items-center justify-center">
      <p class="text-gray-400">분석 데이터가 없습니다.</p>
    </div>
  </div>
</template>

<script setup>
// Import functions from Vue to define component properties and events.
import { defineProps, defineEmits } from 'vue';

// --- Component Properties (Props) ---
defineProps({
  // This component expects an `analysisData` object containing sentiment and category info.
  analysisData: {
    type: Object,
    required: true,
  },
});

// --- Component Events (Emits) ---
// Defines that this component can emit a `toggle-visibility` event.
const emit = defineEmits(['toggle-visibility']);

// --- Methods ---
// This function is called when the toggle button is clicked.
const toggle = () => {
  // It emits the event to the parent component (BroadcastRoom.vue) to handle the state change.
  emit('toggle-visibility');
};
</script>

<style scoped>
/* This class applies a smooth transition effect to the width property of an element. */
.progress-bar {
  transition: width 0.5s ease-in-out;
}
/* This class applies a backdrop blur effect, often used in overlay UIs. */
.backdrop-blur-sm {
    backdrop-filter: blur(4px);
}
</style>