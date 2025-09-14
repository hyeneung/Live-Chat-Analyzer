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
        It passes the `user` object down to the routed component.
        It also listens for the `show-login` event that can be emitted from child components.
      -->
      <router-view :user="user" @show-login="showLoginModal('watch')" />
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
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRoute } from 'vue-router';
import NavBar from './components/NavBar.vue';
import LoginModal from './components/LoginModal.vue';

// --- State Management ---
const isLoginModalVisible = ref(false);
const loginContext = ref('watch'); // 'watch' or 'nav'
const navBarRef = ref(null);
const user = ref(null); // Holds the logged-in user's data

// --- Router --- 
const route = useRoute();

// --- Computed Properties ---
const isBroadcastRoom = computed(() => route.name === 'BroadcastRoom');

// --- Methods ---
const checkLoginStatus = () => {
  const userName = localStorage.getItem('userName');
  const userProfileImage = localStorage.getItem('userProfileImage');
  if (userName && userProfileImage) {
    user.value = { name: userName, profilePic: userProfileImage };
  } else {
    user.value = null;
  }
};

const showLoginModal = (contextType) => {
  loginContext.value = contextType;
  isLoginModalVisible.value = true;
};

const hideLoginModal = () => {
  isLoginModalVisible.value = false;
};

const handleLoginSuccess = () => {
  hideLoginModal();
  checkLoginStatus(); // Update the user state in App.vue
  if (navBarRef.value) {
    navBarRef.value.checkLoginStatus();
  }
};

const logout = () => {
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
  localStorage.removeItem('userName');
  localStorage.removeItem('userProfileImage');
  checkLoginStatus(); // Update the user state in App.vue
  if (navBarRef.value) {
    navBarRef.value.checkLoginStatus();
  }
};

// --- Lifecycle Hooks ---
onMounted(() => {
  checkLoginStatus(); // Check login status when the app loads
  window.addEventListener('storage', handleStorageChange);
  window.addEventListener('relogin-required', handleReloginRequired);
});

onUnmounted(() => {
  window.removeEventListener('storage', handleStorageChange);
  window.removeEventListener('relogin-required', handleReloginRequired);
});

const handleReloginRequired = () => {
  logout(); // Clear any stale login data
  showLoginModal('nav');
};

const handleStorageChange = (event) => {
  // When localStorage changes (e.g., login/logout in another tab), update the state.
  if (['userName', 'accessToken', 'refreshToken'].includes(event.key)) {
    checkLoginStatus();
    if (navBarRef.value) {
      navBarRef.value.checkLoginStatus();
    }
  }
};
</script>

<style>
/* 
  Global styles are imported from `src/assets/css/tailwind.css` via main.js.
  This style block can be used for component-specific styles if needed
*/
</style>