<template>
  <!-- The main container for the entire application -->
  <div id="app" class="min-h-screen bg-gray-900 text-white">
    
    <!-- 
      The navigation bar.
      It is conditionally rendered using v-if. It will not be shown if the current route is the BroadcastRoom.
      It listens for `show-login` and `logout` events from the NavBar component.
    -->
    <NavBar v-if="!isBroadcastRoom" @show-login="showLoginModal('nav')" @logout="logout" ref="navBarRef" />

    <!-- 
      The main content area.
      The padding is applied conditionally. It's removed on the BroadcastRoom page to allow for a fullscreen experience.
    -->
    <main :class="{ 'p-4 md:p-8': !isBroadcastRoom }">
      <!-- 
        This is where the router displays the component for the current URL.
        It also listens for the `show-login` event that can be emitted from child components.
      -->
      <router-view @show-login="showLoginModal('watch')" />
    </main>

    <!-- 
      The login modal component.
      Its visibility is controlled by the `isLoginModalVisible` state.
      It listens for `close` and `login-success` events to hide itself or perform the login action.
    -->
    <LoginModal 
      :show="isLoginModalVisible" 
      :context="loginContext" 
      @close="hideLoginModal" 
      @login-success="handleLoginSuccess" 
    />
  </div>
</template>

<script setup>
// Import necessary functions and components from Vue and other files.
import { ref, computed, onMounted, onUnmounted } from 'vue'; // Added onMounted, onUnmounted
import { useRoute } from 'vue-router';
import NavBar from './components/NavBar.vue';
import LoginModal from './components/LoginModal.vue';

// --- State Management ---
// A reactive reference to control the visibility of the login modal.
const isLoginModalVisible = ref(false);
const loginContext = ref('watch'); // 'watch' or 'nav'
// A reactive reference to get a direct reference to the NavBar component instance.
const navBarRef = ref(null);

// --- Router --- 
// Get the current route object to access information about the current page.
const route = useRoute();

// --- Computed Properties ---
// A computed property that returns true if the current page is the BroadcastRoom.
// This is used to conditionally show/hide the NavBar and apply padding.
const isBroadcastRoom = computed(() => route.name === 'BroadcastRoom');

// --- Methods ---
// Function to set the login modal visibility to true.
const showLoginModal = (contextType) => {
  loginContext.value = contextType;
  isLoginModalVisible.value = true;
};

// Function to set the login modal visibility to false.
const hideLoginModal = () => {
  isLoginModalVisible.value = false;
};

// Function to handle a successful login.
const handleLoginSuccess = () => {
  // Hide the modal after login.
  hideLoginModal();
  // Explicitly call checkLoginStatus on NavBar to update its UI
  if (navBarRef.value) {
    navBarRef.value.checkLoginStatus();
  }
};

// Function to handle logout.
const logout = () => {
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
  localStorage.removeItem('userName');
  localStorage.removeItem('userProfileImage');
  // Explicitly call checkLoginStatus on NavBar to update its UI
  if (navBarRef.value) {
    navBarRef.value.checkLoginStatus();
  }
};

// --- Lifecycle Hooks ---
onMounted(() => {
  // Listen for changes in localStorage (e.g., from LoginModal)
  window.addEventListener('storage', handleStorageChange);
  // Listen for custom event to show login modal when re-login is required
  window.addEventListener('relogin-required', handleReloginRequired);
});

onUnmounted(() => {
  window.removeEventListener('storage', handleStorageChange);
  // Clean up custom event listener
  window.removeEventListener('relogin-required', handleReloginRequired);
});

const handleReloginRequired = () => {
  showLoginModal('nav');
  if (navBarRef.value) {
    navBarRef.value.checkLoginStatus();
  }
};

const handleStorageChange = (event) => {
  // This function will be called when localStorage changes in another tab/window.
  // For changes in the same window, components like NavBar will react via their own onMounted/checkLoginStatus.
  // This is primarily for cross-tab/window synchronization.
  console.log('localStorage changed:', event.key);
  // If App.vue needs to react to localStorage changes, it can do so here.
  // For now, we just log it. NavBar.vue already has its own listener.
};
</script>

<style>
/* 
  Global styles are imported from `src/assets/css/tailwind.css` via main.js.
  This style block can be used for component-specific styles if needed
*/
</style>