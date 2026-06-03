<template>
  <main class="w-full">
    <!-- Hero Section -->
    <section class="min-h-[80vh] flex flex-col lg:flex-row border-b-4 border-black">
      <!-- Left text area -->
      <div class="w-full lg:w-1/2 p-8 lg:p-16 flex flex-col justify-center bg-background relative overflow-hidden">
        <!-- Abstract shape in background -->
        <div class="absolute -top-12 -left-12 w-64 h-64 rounded-full border-4 border-black bg-primary-yellow opacity-10"></div>
        
        <div class="relative z-10">
          <h1 class="font-black text-6xl md:text-7xl lg:text-8xl uppercase leading-[0.9] tracking-tighter text-foreground mb-6">
            LÀM CHỦ <br/> TIẾNG ANH <br/> <span class="text-primary-red">ENGFLOW</span>
          </h1>
          <p class="font-bold text-lg md:text-xl max-w-md mb-8 border-l-4 border-black pl-4">
            NỀN TẢNG HỌC TẬP TƯƠNG TÁC THẾ HỆ MỚI. CẤU TRÚC HÓA NGỮ PHÁP VÀ TỪ VỰNG THÀNH CÁC KHỐI KIẾN THỨC VỮNG CHẮC.
          </p>
          <div class="flex gap-4">
            <BauhausButton variant="primary" size="lg" @click="$router.push('/lessons')">BẮT ĐẦU HỌC</BauhausButton>
          </div>
        </div>
      </div>
      
      <!-- Right graphic area -->
      <div class="w-full lg:w-1/2 bg-primary-blue border-t-4 lg:border-t-0 lg:border-l-4 border-black flex items-center justify-center p-12 relative overflow-hidden">
        <!-- Geometric Composition -->
        <div class="relative w-full max-w-md aspect-square">
          <div class="absolute inset-0 bg-primary-red rounded-full border-4 border-black shadow-[8px_8px_0px_0px_black] transform translate-x-8 translate-y-8"></div>
          <div class="absolute inset-4 bg-primary-yellow border-4 border-black shadow-[8px_8px_0px_0px_black] rotate-12"></div>
          <div class="absolute inset-12 bg-white border-4 border-black flex items-center justify-center -rotate-6">
            <div class="w-0 h-0 border-l-[40px] border-l-transparent border-r-[40px] border-r-transparent border-b-[70px] border-b-primary-blue"></div>
          </div>
        </div>
      </div>
    </section>

    <!-- Features Section -->
    <section class="bg-primary-yellow border-b-4 border-black p-8 lg:p-24">
      <div class="max-w-7xl mx-auto">
        <h2 class="font-black text-4xl md:text-5xl uppercase tracking-tight mb-16 border-b-4 border-black pb-4 inline-block">HỆ THỐNG KIẾN THỨC</h2>
        
        <div class="grid grid-cols-1 md:grid-cols-3 gap-8">
          <BauhausCard decorationColor="red" decorationShape="circle" class="bg-white">
            <div class="w-16 h-16 border-4 border-black bg-primary-blue rounded-none flex items-center justify-center mb-6 shadow-hard-sm">
              <BookOpenIcon class="w-8 h-8 text-white" />
            </div>
            <h3 class="font-black text-2xl uppercase mb-4">BÀI HỌC HỆ THỐNG</h3>
            <p class="font-medium text-lg leading-relaxed">Hệ thống bài học biên soạn chi tiết theo nhiều cấp độ từ Beginner đến Advanced, xây dựng kiến thức nền tảng vững chắc.</p>
          </BauhausCard>

          <BauhausCard decorationColor="blue" decorationShape="square" class="bg-white translate-y-0 md:translate-y-8">
            <div class="w-16 h-16 border-4 border-black bg-primary-red rounded-full flex items-center justify-center mb-6 shadow-hard-sm">
              <ZapIcon class="w-8 h-8 text-white" />
            </div>
            <h3 class="font-black text-2xl uppercase mb-4">FLASHCARD LẬT MẶT</h3>
            <p class="font-medium text-lg leading-relaxed">Học từ vựng trực quan sinh động với Flashcards lật bài mượt mà kèm phiên âm và dịch nghĩa, áp dụng Spaced Repetition.</p>
          </BauhausCard>

          <BauhausCard decorationColor="yellow" decorationShape="triangle" class="bg-white">
            <div class="w-16 h-16 border-4 border-black bg-black rounded-none rotate-45 flex items-center justify-center mb-6 shadow-hard-sm mx-auto md:mx-0">
              <div class="rotate-[-45deg]"><TrophyIcon class="w-8 h-8 text-primary-yellow" /></div>
            </div>
            <h3 class="font-black text-2xl uppercase mb-4">THÀNH TÍCH CAO</h3>
            <p class="font-medium text-lg leading-relaxed">Tích lũy điểm số, thăng hạng cấp độ và mở khóa các huy hiệu học tập cao quý qua từng chặng đường.</p>
          </BauhausCard>
        </div>
      </div>
    </section>
  </main>
</template>

<script setup>
import BauhausButton from '@/components/bauhaus/BauhausButton.vue'
import BauhausCard from '@/components/bauhaus/BauhausCard.vue'
import { BookOpen as BookOpenIcon, Zap as ZapIcon, Trophy as TrophyIcon } from 'lucide-vue-next'
</script>
