<template>
  <!-- The header element serves as the main container for the navigation bar. -->
  <!-- It uses Tailwind CSS for styling, making it a sticky bar at the top (sticky top-0 z-50). -->
  <header class="bg-gray-800 shadow-md p-4 flex justify-between items-center sticky top-0 z-50">
    
    <!-- The application logo and title. Clicking it navigates to the home page. -->
    <h1 class="text-2xl font-bold text-indigo-400 cursor-pointer" @click="goToHome">
      <svg xmlns="http://www.w3.org/2000/svg" class="h-8 w-8 inline-block mr-2" viewBox="0 0 20 20" fill="currentColor">
        <path d="M10 12a2 2 0 100-4 2 2 0 000 4z" />
        <path fill-rule="evenodd" d="M.458 10C1.732 5.943 5.522 3 10 3s8.268 2.943 9.542 7c-1.274 4.057-5.022 7-9.542 7S1.732 14.057.458 10zM14 10a4 4 0 11-8 0 4 4 0 018 0z" clip-rule="evenodd" />
      </svg>
      <span>Live Stream</span>
    </h1>

    <!-- This section is shown only if there is no logged-in user (v-if="!user"). -->
    <div v-if="!user">
      <!-- The login button. When clicked, it calls the showLoginModal method. -->
      <button @click="showLoginModal" class="bg-indigo-500 hover:bg-indigo-600 text-white font-bold py-2 px-4 rounded-lg transition duration-300">
        로그인
      </button>
    </div>

    <!-- This section is shown only if a user is logged in (v-else). -->
    <div v-else class="flex items-center space-x-4">
      <!-- Welcome message for the user. -->
      <span class="font-medium">환영합니다, {{ user.name }}님!</span>
      <!-- User's profile picture. -->
      <img :src="user.profilePic" class="w-10 h-10 rounded-full border-2 border-indigo-400">
      <!-- The logout button. When clicked, it calls the logout method. -->
      <button @click="logout" class="bg-gray-600 hover:bg-gray-700 text-white font-bold py-2 px-4 rounded-lg transition duration-300">
        로그아웃
      </button>
    </div>
  </header>
</template>

<script setup>
// Import functions from Vue to define component properties and events.
import { defineProps, defineEmits } from 'vue';
// Import functions from Vue Router to handle navigation.
import { useRouter, useRoute } from 'vue-router';

// --- Component Properties (Props) ---
// Defines the properties that this component accepts from its parent (App.vue).
defineProps({
  // The `user` prop is an object containing user data. It defaults to null if not provided.
  user: {
    type: Object,
    default: null,
  },
});

// --- Component Events (Emits) ---
// Defines the custom events that this component can send (emit) to its parent.
const emit = defineEmits(['show-login', 'logout']);

// --- Router --- 
// Get the router instance to programmatically navigate between pages.
const router = useRouter();
// Get the current route object to check the current URL path.
const route = useRoute();

// --- Methods ---
// This function is called when the logo/title is clicked.
const goToHome = () => {
  // It checks if the user is not already on the home page.
  if (route.path !== '/') {
    // If not, it navigates to the home page.
    router.push('/');
  }
};

// This function is called when the login button is clicked.
const showLoginModal = () => {
  // It emits the 'show-login' event to the parent component (App.vue).
  emit('show-login');
};

// This function is called when the logout button is clicked.
const logout = () => {
  // It emits the 'logout' event to the parent component (App.vue).
  emit('logout');
};
</script>