<template>
  <header class="bg-gray-800 shadow-md p-4 flex justify-between items-center sticky top-0 z-50">
    <h1 class="text-2xl font-bold text-indigo-400 cursor-pointer" @click="goToHome">
      <svg xmlns="http://www.w3.org/2000/svg" class="h-8 w-8 inline-block mr-2" viewBox="0 0 20 20" fill="currentColor">
        <path d="M10 12a2 2 0 100-4 2 2 0 000 4z" />
        <path fill-rule="evenodd" d="M.458 10C1.732 5.943 5.522 3 10 3s8.268 2.943 9.542 7c-1.274 4.057-5.022 7-9.542 7S1.732 14.057.458 10zM14 10a4 4 0 11-8 0 4 4 0 018 0z" clip-rule="evenodd" />
      </svg>
      <span>Live Stream</span>
    </h1>

    <div v-if="!currentUser.name">
      <button @click="showLoginModal" class="bg-indigo-500 hover:bg-indigo-600 text-white font-bold py-2 px-4 rounded-lg transition duration-300">
        로그인
      </button>
    </div>

    <div v-else class="flex items-center space-x-4">
      <span class="font-medium">환영합니다, {{ currentUser.name }}님!</span>
      <img :src="currentUser.profilePic" class="w-10 h-10 rounded-full border-2 border-indigo-400">
      <button @click="logout" class="bg-gray-600 hover:bg-gray-700 text-white font-bold py-2 px-4 rounded-lg transition duration-300">
        로그아웃
      </button>
    </div>
  </header>
</template>

<script setup>
import { ref, onMounted, onUnmounted, defineEmits, defineExpose } from 'vue'; // Import ref, onMounted, onUnmounted, defineEmits, and defineExpose
import { useRouter, useRoute } from 'vue-router';

// --- Component Events (Emits) ---
const emit = defineEmits(['show-login']); // Removed 'logout' emit as it's handled internally

// --- Router ---
const router = useRouter();
const route = useRoute();

// --- Reactive State for Current User ---
const currentUser = ref({
  name: null,
  profilePic: null,
});

// --- Methods ---
const goToHome = () => {
  if (route.path !== '/') {
    router.push('/');
  }
};

const showLoginModal = () => {
  emit('show-login');
};

const logout = () => {
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
  localStorage.removeItem('userName');
  localStorage.removeItem('userProfileImage');
  currentUser.value = { name: null, profilePic: null }; // Clear current user state
  router.push('/'); // Redirect to home after logout
};

const checkLoginStatus = () => {
  const userName = localStorage.getItem('userName');
  const userProfileImage = localStorage.getItem('userProfileImage');
  const accessToken = localStorage.getItem('accessToken');
  if (userName && userProfileImage && accessToken) {
    currentUser.value.name = userName;
    currentUser.value.profilePic = userProfileImage;
  } else {
    currentUser.value = { name: null, profilePic: null }; // Ensure user is null if not logged in
  }
};

// --- Lifecycle Hooks ---
onMounted(() => {
  checkLoginStatus();
  // Listen for changes in localStorage (e.g., from AuthCallback)
  window.addEventListener('storage', checkLoginStatus);
});

// Clean up the event listener when the component is unmounted
onUnmounted(() => {
  window.removeEventListener('storage', checkLoginStatus);
});

// Expose checkLoginStatus so parent components can call it
defineExpose({ checkLoginStatus });
</script>