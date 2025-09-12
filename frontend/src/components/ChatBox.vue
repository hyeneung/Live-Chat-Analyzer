
<template>
  <!-- 
    Main container for the chat box component.
    It has a semi-transparent, blurred background and is laid out as a vertical flex column.
  -->
  <div class="bg-gray-800 bg-opacity-70 rounded-lg flex flex-col backdrop-blur-sm">
    <!-- Chat box header -->
    <h3 class="text-xl font-bold p-4 border-b border-gray-700">실시간 댓글</h3>
    
    <!-- 
      This is the container for the chat messages.
      `ref` gives us a direct reference to this DOM element for auto-scrolling.
      `flex-grow` makes it take up available space, and `overflow-y-auto` enables scrolling.
    -->
    <div ref="messageContainerRef" class="flex-grow p-4 overflow-y-auto chat-container">
      <!-- 
        Vue's <transition-group> component is used to animate a list of items.
        When a new message is added, it will apply the animation defined in the <style> block.
      -->
      <transition-group name="list" tag="div">
        <!-- Loop through each message in the `messages` prop and display it. -->
        <div v-for="message in messages" :key="message.id" class="flex items-start mb-4">
          <img :src="message.user.profilePic" class="w-8 h-8 rounded-full mr-3 mt-1">
          <div class="bg-gray-700 rounded-lg p-3 w-full">
            <p class="font-semibold text-indigo-300">{{ message.user.name }}</p>
            <p class="text-gray-200 break-words">{{ message.text }}</p>
          </div>
        </div>
      </transition-group>
    </div>

    <!-- The input area at the bottom of the chat box. -->
    <div class="p-4 border-t border-gray-700">
      <!-- The form handles submission. `@submit.prevent` stops the default browser form submission. -->
      <form @submit.prevent="sendMessage" class="flex gap-2">
        <!-- The text input for a new message. `v-model` creates a two-way binding with the `newMessage` state. -->
        <input v-model="newMessage" type="text" placeholder="댓글을 입력하세요..." class="flex-grow bg-gray-700 text-white rounded-lg p-2 focus:outline-none focus:ring-2 focus:ring-indigo-500">
        <!-- The send button. -->
        <button type="submit" class="bg-indigo-500 hover:bg-indigo-600 text-white font-bold py-2 px-4 rounded-lg transition">전송</button>
      </form>
    </div>
  </div>
</template>

<script setup>
// Import necessary functions and components from Vue.
import { ref, watch, nextTick, defineProps } from 'vue';

// --- Component Properties (Props) ---
const props = defineProps({
  // This component expects a `messages` array to display.
  messages: {
    type: Array,
    required: true,
  },
});

// --- State Management ---
// A reactive reference to store the content of the new message input field.
const newMessage = ref('');
// A reference to the message container DOM element, used for auto-scrolling.
const messageContainerRef = ref(null);

// --- Watchers ---
// This watcher function runs whenever the number of messages changes.
watch(() => props.messages.length, () => {
  // `nextTick` waits for the DOM to be updated with the new message.
  nextTick(() => {
    const container = messageContainerRef.value;
    if (container) {
      // Scroll the container to the bottom to show the latest message.
      container.scrollTop = container.scrollHeight;
    }
  });
});

// --- Methods ---
// This function is called when the user submits the form (by clicking send or pressing Enter).
const sendMessage = () => {
  // Check if the message is not just empty spaces.
  if (newMessage.value.trim() !== '') {
    // In a real app, you would emit an event here to send the message to the server.
    console.log('Sending message:', newMessage.value);
    // Clear the input field after sending.
    newMessage.value = '';
  }
};
</script>

<style scoped>
/* 
  These styles define the animation for new messages entering the list.
*/

/* The state during which the enter transition is active. */
.list-enter-active {
  transition: all 0.5s ease;
}

/* The starting state for the enter transition (invisible and slightly scaled down). */
.list-enter-from {
  opacity: 0;
  transform: translateY(20px) scale(0.9);
}

/* This class applies a backdrop blur effect. */
.backdrop-blur-sm {
    backdrop-filter: blur(4px);
}
</style>
