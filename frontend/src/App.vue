<template>
  <div id="app">
    <router-view />
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

onMounted(() => {
  // 刷新页面时从localStorage恢复登录状态
  userStore.restoreLoginState()
})
</script>

<style>
#app {
  font-family: 'Noto Serif SC', 'Source Han Serif CN', 'SimSun', 'Songti SC',
    'PingFang SC', 'Microsoft YaHei', sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  width: 100%;
  min-height: 100vh;
  background: var(--color-rice);
}
</style>
