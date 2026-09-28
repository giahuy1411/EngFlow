/**
 * Bề mặt export các primitive UI.
 *
 * Điểm gom chung cho các primitive của design system Playful Geometric.
 * Component import từ index này KHÔNG được fetch dữ liệu, import store, hay biết
 * routing — giữ chúng thuần trình bày và không chứa business logic.
 *
 * Lưu ý: đây là barrel chỉ export 9 primitive. Một số primitive khác trong thư
 * mục `ui/` (AppAvatar, AppDropdown, AppProgress, AppStepper, AppTable, AppTabs,
 * AppTooltip) không nằm ở đây nên phải import trực tiếp theo đường dẫn file.
 */
export { default as AppButton } from './AppButton.vue'
export { default as AppInput } from './AppInput.vue'
export { default as FormField } from './FormField.vue'
export { default as StickerCard } from './StickerCard.vue'
export { default as AppAlert } from './AppAlert.vue'
export { default as AppToast } from './AppToast.vue'
export { default as AppSkeleton } from './AppSkeleton.vue'
export { default as AppEmptyState } from './AppEmptyState.vue'
export { default as AppErrorState } from './AppErrorState.vue'
