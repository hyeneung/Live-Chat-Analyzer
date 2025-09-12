<template>
  <!-- 
    The <transition> component from Vue provides animated transitions.
    Here, it applies a fade effect when the modal appears or disappears.
    The animation is defined in the <style> block below.
  -->
  <transition name="fade">
    <!-- The modal container. It's only rendered if the `show` prop is true. -->
    <!-- It's a full-screen overlay with a dark, semi-transparent background. -->
    <div v-if="show" class="fixed inset-0 bg-black bg-opacity-70 flex items-center justify-center z-50">
      
      <!-- The modal dialog box itself. -->
      <div class="bg-white text-gray-800 p-8 rounded-lg shadow-xl text-center w-full max-w-md mx-4">
        
        <!-- Modal content -->
        <h2 class="text-2xl font-bold mb-4">로그인이 필요합니다</h2>
        <p class="mb-6">방송을 시청하려면 로그인을 해주세요.</p>
        
        <!-- Google Login Button -->
        <button @click="loginWithGoogle" class="w-full bg-blue-500 hover:bg-blue-600 text-white font-bold py-3 px-4 rounded-lg flex items-center justify-center transition">
          <svg class="w-6 h-6 mr-3" viewBox="0 0 48 48"><path fill="#FFC107" d="M43.611,20.083H42V20H24v8h11.303c-1.649,4.657-6.08,8-11.303,8c-6.627,0-12-5.373-12-12c0-6.627,5.373-12,12-12c3.059,0,5.842,1.154,7.961,3.039l5.657-5.657C34.046,6.053,29.268,4,24,4C12.955,4,4,12.955,4,24c0,11.045,8.955,20,20,20c11.045,0,20-8.955,20-20C44,22.659,43.862,21.35,43.611,20.083z"></path><path fill="#FF3D00" d="M6.306,14.691l6.571,4.819C14.655,15.108,18.961,12,24,12c3.059,0,5.842,1.154,7.961,3.039l5.657-5.657C34.046,6.053,29.268,4,24,4C16.318,4,9.656,8.337,6.306,14.691z"></path><path fill="#4CAF50" d="M24,44c5.166,0,9.86-1.977,13.409-5.192l-6.19-5.238C29.211,35.091,26.715,36,24,36c-5.222,0-9.618-3.234-11.303-7.584l-6.522,5.025C9.505,39.556,16.227,44,24,44z"></path><path fill="#1976D2" d="M43.611,20.083H42V20H24v8h11.303c-0.792,2.237-2.231,4.166-4.087,5.574l6.19,5.238C42.012,36.49,44,30.638,44,24C44,22.659,43.862,21.35,43.611,20.083z"></path></svg>
          Google 계정으로 로그인하기
        </button>
        
        <!-- Close Button -->
        <button @click="closeModal" class="mt-4 text-gray-500 hover:text-gray-700">나중에 하기</button>
      </div>
    </div>
  </transition>
</template>

<script setup>
// Import functions from Vue to define component properties and events.
import { defineProps, defineEmits } from 'vue';

// --- Component Properties (Props) ---
defineProps({
  // The `show` prop is a boolean that determines whether the modal is visible.
  // It is required, meaning the parent component must provide it.
  show: {
    type: Boolean,
    required: true,
  },
});

// --- Component Events (Emits) ---
// Defines the custom events that this component can send to its parent.
const emit = defineEmits(['close', 'login-success']);

// --- Methods ---
// This function is called when the "Login with Google" button is clicked.
const loginWithGoogle = () => {
  // It emits the 'login-success' event to the parent component (App.vue).
  emit('login-success');
};

// This function is called when the "나중에 하기" (Do it later) button is clicked.
const closeModal = () => {
  // It emits the 'close' event to the parent component (App.vue).
  emit('close');
};
</script>

<style scoped>
/* 
  These styles define the fade animation for the <transition> component.
*/

/* The state during which the enter/leave transitions are active. */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

/* The starting state for the enter transition. */
.fade-enter-from,
/* The ending state for the leave transition. */
.fade-leave-to {
  opacity: 0;
}
</style>