<script setup>
/**
 * AppLogo — logo EngFlow tái sử dụng.
 *
 * Hợp đồng: `href` rỗng → render `<span>` tĩnh; `href` có giá trị → render
 * `<router-link>` trỏ về home. Logo không nhận slot và không phụ thuộc store.
 *
 * Lưu ý: khối hình học bên trong được đánh `aria-hidden` vì chỉ là trang trí;
 * phần chữ "EngFlow" mới mang nghĩa. Nhãn `aria-label` đổi theo biến thể
 * ("EngFlow home" khi là link) để screen reader đọc đúng vai trò.
 */
defineProps({
  href: { type: String, default: '' },
})
</script>

<template>
  <router-link v-if="href" :to="href" class="app-logo" aria-label="EngFlow home">
    <span class="app-logo__mark" aria-hidden="true">
      <span class="app-logo__square" />
      <span class="app-logo__circle" />
      <span class="app-logo__dot" />
    </span>
    <span class="app-logo__word">EngFlow</span>
  </router-link>
  <span v-else class="app-logo" aria-label="EngFlow">
    <span class="app-logo__mark" aria-hidden="true">
      <span class="app-logo__square" />
      <span class="app-logo__circle" />
      <span class="app-logo__dot" />
    </span>
    <span class="app-logo__word">EngFlow</span>
  </span>
</template>