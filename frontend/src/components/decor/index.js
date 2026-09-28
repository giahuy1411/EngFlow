/**
 * Barrel export các component trang trí (decor) — chỉ để gom import một chỗ:
 * `import { DecoConfetti, DotBackground } from '@/components/decor'`.
 *
 * Các component này thuần trình bày (SVG/hiệu ứng nền), không có logic nghiệp vụ:
 * - `DecoShape`       : hình khối trang trí rời (class `geoshape`, prop variant/size).
 * - `DecoConfetti`    : một mảnh confetti SVG (tròn/vuông/tam giác theo prop `kind`).
 * - `SquiggleDivider` : đường kẻ uốn lượn phân tách section.
 * - `DotBackground`   : nền chấm bi (class `geodot`, prop gap/tint).
 */
export { default as DecoShape } from './DecoShape.vue'
export { default as DecoConfetti } from './DecoConfetti.vue'
export { default as SquiggleDivider } from './SquiggleDivider.vue'
export { default as DotBackground } from './DotBackground.vue'
