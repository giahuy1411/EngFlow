<template>
  <div class="bg-white border-2 border-foreground p-10 text-center shadow-pop max-w-xl mx-auto mt-4" role="note">
    <h3 class="text-2xl font-black mb-2">{{ title }}</h3>
    <p class="text-muted-foreground mb-6">Tạo tài khoản miễn phí để làm bài tập tương tác, lưu lịch sử học và theo dõi tiến độ.</p>
    <div class="flex flex-col sm:flex-row gap-3 justify-center">
      <router-link to="/register"
        class="inline-flex items-center justify-center rounded-full border-2 border-foreground bg-accent-strong px-6 py-3 font-black uppercase tracking-wider text-white shadow-pop transition-all duration-300 ease-bounce hover:-translate-x-0.5 hover:-translate-y-0.5 hover:shadow-pop-hover active:translate-x-0.5 active:translate-y-0.5 active:shadow-pop-sm">
        Đăng ký miễn phí
      </router-link>
      <router-link to="/login"
        class="inline-flex items-center justify-center rounded-full border-2 border-foreground bg-white px-6 py-3 font-black uppercase tracking-wider shadow-pop-sm transition-all duration-300 ease-bounce hover:bg-tertiary">
        Đăng nhập
      </router-link>
    </div>
  </div>
</template>

<script setup>
defineProps({ title: { type: String, default: 'Cần đăng nhập' } })
</script>
