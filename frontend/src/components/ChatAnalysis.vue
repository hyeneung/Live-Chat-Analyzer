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

    <!-- Sentiment Analysis Section -->
    <!-- This block is only rendered if sentiment data is available. -->
    <div v-if="analysisData.sentiment">
      <p class="font-semibold mb-2">감정 분석</p>
      <!-- The container for the progress bar. -->
      <div class="w-full bg-gray-700 rounded-full h-6 flex overflow-hidden text-xs font-medium text-center">
        <!-- Positive sentiment bar. Its width is dynamically bound to the data. -->
        <div 
          class="bg-blue-500 h-6 flex items-center justify-center progress-bar"
          :style="{ width: analysisData.sentiment.positive + '%' }" 
          title="긍정">
          {{ analysisData.sentiment.positive }}%
        </div>
        <!-- Negative sentiment bar. -->
        <div 
          class="bg-red-500 h-6 flex items-center justify-center progress-bar"
          :style="{ width: analysisData.sentiment.negative + '%' }" 
          title="부정">
          {{ analysisData.sentiment.negative }}%
        </div>
      </div>
    </div>

    <!-- Category Analysis Section -->
    <!-- This block is only rendered if category data is available. -->
    <div class="mt-4" v-if="analysisData.categories">
      <p class="font-semibold mb-2">주요 카테고리</p>
      <!-- Loop through each category and create a progress bar for it. -->
      <div v-for="(value, key) in analysisData.categories" :key="key" class="mb-2">
        <div class="flex justify-between mb-1">
          <span class="text-base font-medium text-gray-300 capitalize">{{ key }}</span>
          <span class="text-sm font-medium text-gray-400">{{ value }}%</span>
        </div>
        <!-- The container for the category progress bar. -->
        <div class="w-full bg-gray-700 rounded-full h-2.5">
          <!-- The actual progress bar. Its width is dynamically bound to the category value. -->
          <div class="bg-indigo-500 h-2.5 rounded-full progress-bar" :style="{ width: value + '%' }"></div>
        </div>
      </div>
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