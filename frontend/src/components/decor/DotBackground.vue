<template>
  <div
    class="geodot"
    :style="{ '--deco-dot-opacity': String(tint), backgroundSize: `${gap}px ${gap}px` }"
    aria-hidden="true"
  />
</template>
<script setup>
/*
 * DotBackground — nền chấm bi (dot grid) phủ kín khối cha, dùng làm hoạ tiết nền
 * trang trí. Luôn aria-hidden. Lớp phủ `position: absolute; inset: 0` nên component
 * này phải đặt bên trong một phần tử cha `position: relative`.
 *
 * Props:
 *   - gap  (Number | String): khoảng cách giữa các chấm theo px, gắn vào
 *                             `background-size`. Mặc định 20.
 *   - tint (Number): độ đậm của chấm (0..1), truyền vào biến CSS
 *                    --deco-dot-opacity để ghi đè opacity mặc định 0.18.
 *
 * Không slot, không emit. Chấm được vẽ bằng radial-gradient với màu --geo-fg
 * (class .geodot trong <style scoped>).
 */
defineProps({
  gap: { type: [Number, String], default: 20 },
  tint: { type: Number, default: 0.18 },
})
</script>

<style scoped>
.geodot {
  position: absolute;
  inset: 0;
  pointer-events: none;
  opacity: var(--deco-dot-opacity, 0.18);
  background-image: radial-gradient(circle, var(--geo-fg) 1px, transparent 1px);
}
</style>