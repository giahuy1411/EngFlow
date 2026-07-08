<template>
  <div class="bg-geo-bg min-h-screen py-16">
    <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
      <router-link :to="'/decks/' + deckId"
        class="inline-flex items-center gap-2 font-bold text-sm uppercase tracking-wider text-muted-foreground hover:text-accent transition-colors mb-6"
      >
        <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5"><path stroke-linecap="round" stroke-linejoin="round" d="M15 19l-7-7 7-7"/></svg>
        Quay lại
      </router-link>
      <div class="bg-card border-2 border-foreground rounded-md p-8 shadow-pop-xl text-center">
        <div class="inline-flex items-center justify-center w-16 h-16 bg-accent border-2 border-foreground rounded-full mb-6 shadow-pop-sm">
          <Shuffle class="w-8 h-8 text-white" />
        </div>
        <h1 class="font-black text-3xl uppercase tracking-tight mb-2">Tổng hợp</h1>
        <p class="font-bold text-sm uppercase tracking-wider text-muted-foreground mb-10">Kết hợp các dạng bài tập</p>
        <p class="font-medium text-muted-foreground">Tính năng đang phát triển</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRoute } from 'vue-router'
import { Shuffle } from 'lucide-vue-next'
const route = useRoute()
const deckId = route.params.id
</script>
