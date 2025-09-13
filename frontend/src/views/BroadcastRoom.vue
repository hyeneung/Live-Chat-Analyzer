
<template>
  <div v-if="currentStream" class="relative h-screen w-screen overflow-hidden">
    <!-- Video Player (Background) -->
    <div class="absolute inset-0 bg-black flex items-center justify-center">
      <div class="aspect-w-16 aspect-h-9 w-full">
        <p class="text-gray-400 text-2xl text-center">실시간 영상 스트리밍 화면</p>
      </div>
    </div>

    <!-- Overlay Content -->
    <div class="absolute inset-0 flex justify-between items-start p-4 md:p-8 pointer-events-none">
      <!-- Left Side: Info and Back Button -->
      <div class="w-full lg:w-2/3 text-white pointer-events-auto">
        <button @click="goHome" class="mb-4 bg-gray-800 bg-opacity-50 hover:bg-opacity-75 text-white font-bold py-2 px-4 rounded-lg transition duration-300">
          &larr; 목록으로 돌아가기
        </button>
        <h2 class="text-3xl font-bold text-shadow">{{ currentStream.title }}</h2>
        <div class="flex items-center mt-2 text-gray-200 text-shadow">
          <img :src="currentStream.host.profilePic" class="w-10 h-10 rounded-full mr-3">
          <span class="font-semibold">{{ currentStream.host.name }}</span>
          <span class="mx-3">|</span>
          <span>시청자 {{ currentStream.viewerCount }}명</span>
        </div>
      </div>

      <!-- Right Side: Chat and Analysis -->
      <div class="w-full lg:w-1/3 h-full flex flex-col gap-6 pointer-events-auto">
        <transition name="fade">
            <ChatAnalysis 
                v-if="isAnalysisVisible" 
                :analysis-data="analysisData" 
                @toggle-visibility="toggleAnalysisVisibility"
            />
            <div v-else>
                <button @click="toggleAnalysisVisibility" class="bg-gray-800 bg-opacity-70 hover:bg-opacity-90 text-white font-bold py-2 px-4 rounded-lg transition duration-300 backdrop-blur-sm">
                    실시간 댓글 분석 보기
                </button>
            </div>
        </transition>
        <ChatBox :messages="comments" class="flex-grow min-h-0"/>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import ChatBox from '../components/ChatBox.vue';
import ChatAnalysis from '../components/ChatAnalysis.vue';

const route = useRoute();
const router = useRouter();

const currentStream = ref(null);
const comments = ref([]);
const analysisData = ref({});
const isAnalysisVisible = ref(true);

let commentInterval = null;
let analysisInterval = null;

const goHome = () => {
  router.push('/');
};

const toggleAnalysisVisibility = () => {
    isAnalysisVisible.value = !isAnalysisVisible.value;
}

// --- Real-time Data Simulation ---
const startStreamSimulation = () => {
  const mockComments = [
      { user: { name: '익명1', profilePic: 'https://placehold.co/100x100/cccccc/FFFFFF?text=1' }, text: '와 정말 유익하네요!' },
      { user: { name: '코딩꿈나무', profilePic: 'https://placehold.co/100x100/9333EA/FFFFFF?text=코' }, text: '이 부분 다시 설명해주실 수 있나요?' },
      { user: { name: '스트리머팬', profilePic: 'https://placehold.co/100x100/E11D48/FFFFFF?text=팬' }, text: '오늘 방송도 너무 재밌어요! 짱짱' },
      { user: { name: '지나가던행인', profilePic: 'https://placehold.co/100x100/3B82F6/FFFFFF?text=행' }, text: '이건 좀 아닌 것 같은데...' },
  ];

  commentInterval = setInterval(() => {
      const randomComment = mockComments[Math.floor(Math.random() * mockComments.length)];
      comments.value.push({ id: Date.now(), ...randomComment });
  }, 3000);

  analysisInterval = setInterval(() => {
      let pos = analysisData.value.sentiment?.positive ?? 50;
      pos += Math.floor(Math.random() * 7) - 3;
      pos = Math.max(10, Math.min(90, pos));
      
      const newCategories = {};
      const keys = ['칭찬', '질문', '조언', '비난', '기타'];
      let total = 100;
      keys.forEach((key, index) => {
          if (index === keys.length - 1) {
              newCategories[key] = total;
          } else {
              const val = Math.floor(Math.random() * (total / 2));
              newCategories[key] = val;
              total -= val;
          }
      });

      analysisData.value = {
        sentiment: { positive: pos, negative: 100 - pos },
        categories: newCategories
      };
  }, 2500);
};

const stopStreamSimulation = () => {
  clearInterval(commentInterval);
  clearInterval(analysisInterval);
};

// Mock fetching stream data based on route ID
onMounted(() => {
  const streamId = route.params.id;
  currentStream.value = {
    id: streamId,
    title: `방송 #${streamId}: Tailwind CSS 라이브 코딩`,
    host: { name: 'Gemini-Dev', profilePic: 'https://placehold.co/100x100/16A34A/FFFFFF?text=G' },
    viewerCount: Math.floor(Math.random() * 500) + 50,
  };
  startStreamSimulation();
});

onBeforeUnmount(() => {
  stopStreamSimulation();
});

</script>

<style scoped>
.text-shadow {
  text-shadow: 1px 1px 3px rgba(0,0,0,0.5);
}
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>