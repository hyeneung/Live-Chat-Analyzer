import { createRouter, createWebHistory } from 'vue-router';
import BroadcastList from '../views/BroadcastList.vue';
import BroadcastRoom from '../views/BroadcastRoom.vue';

const routes = [
  {
    path: '/',
    name: 'BroadcastList',
    component: BroadcastList,
  },
  {
    path: '/room/:id',
    name: 'BroadcastRoom',
    component: BroadcastRoom,
    props: true,
  },
];

const router = createRouter({
  history: createWebHistory(process.env.BASE_URL),
  routes,
});

export default router;