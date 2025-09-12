<template>
  <!-- The main container for the entire application -->
  <div id="app" class="min-h-screen bg-gray-900 text-white">
    
    <!-- 
      The navigation bar.
      It is conditionally rendered using v-if. It will not be shown if the current route is the BroadcastRoom.
      It receives the `user` object as a prop to display user info.
      It listens for `show-login` and `logout` events from the NavBar component.
    -->
    <NavBar v-if="!isBroadcastRoom" :user="user" @show-login="showLoginModal" @logout="logout" />

    <!-- 
      The main content area.
      The padding is applied conditionally. It's removed on the BroadcastRoom page to allow for a fullscreen experience.
    -->
    <main :class="{ 'p-4 md:p-8': !isBroadcastRoom }">
      <!-- 
        This is where the router displays the component for the current URL.
        It passes the `user` object down to the page component (e.g., BroadcastList).
        It also listens for the `show-login` event that can be emitted from child components.
      -->
      <router-view :user="user" @show-login="showLoginModal" />
    </main>

    <!-- 
      The login modal component.
      Its visibility is controlled by the `isLoginModalVisible` state.
      It listens for `close` and `login-success` events to hide itself or perform the login action.
    -->
    <LoginModal :show="isLoginModalVisible" @close="hideLoginModal" @login-success="login" />
  </div>
</template>

<script setup>
// Import necessary functions and components from Vue and other files.
import { ref, computed } from 'vue';
import { useRoute } from 'vue-router';
import NavBar from './components/NavBar.vue';
import LoginModal from './components/LoginModal.vue';

// --- State Management ---
// A reactive reference to store the currently logged-in user object. null if no user is logged in.
const user = ref(null);
// A reactive reference to control the visibility of the login modal.
const isLoginModalVisible = ref(false);

// --- Router --- 
// Get the current route object to access information about the current page.
const route = useRoute();

// --- Computed Properties ---
// A computed property that returns true if the current page is the BroadcastRoom.
// This is used to conditionally show/hide the NavBar and apply padding.
const isBroadcastRoom = computed(() => route.name === 'BroadcastRoom');

// --- Methods ---
// Function to set the login modal visibility to true.
const showLoginModal = () => {
  isLoginModalVisible.value = true;
};

// Function to set the login modal visibility to false.
const hideLoginModal = () => {
  isLoginModalVisible.value = false;
};

// Function to handle a successful login.
const login = () => {
  // In a real application, this would involve a call to a backend API.
  // Here, we simulate it by creating a mock user object.
  user.value = {
    name: '임시사용자',
    profilePic: 'https://placehold.co/100x100/4F46E5/FFFFFF?text=U',
  };
  // Hide the modal after login.
  hideLoginModal();
};

// Function to handle logout.
const logout = () => {
  // Set the user state back to null.
  user.value = null;
};
</script>

<style>
/* 
  Global styles are imported from `src/assets/css/tailwind.css` via main.js.
  This style block can be used for component-specific styles if needed
*/
</style>