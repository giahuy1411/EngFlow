import api from './api'

/**
 * Upload media cho admin (file first-party được phục vụ lại dưới `/api/resources/**`).
 *
 * audit-v15: tách ra từ `lessonStructureService` đã bị xoá (Lesson Builder). Bản thân
 * endpoint là hạ tầng dùng chung — `AdminExercises.vue` dùng nó để tải ảnh/audio của
 * bài tập, và media speaking cũng được phục vụ qua cùng route — nên chỉ các method
 * của Lesson Builder bị bỏ, không phải phần upload.
 *
 * CẢNH BÁO BẢO MẬT: upload → `/api/resources/**` là bề mặt stored-XSS. TUYỆT ĐỐI không
 * đi vòng qua các helper sinh tên file an toàn phía server (kiểm tra loại file/tên file)
 * — làm vậy là mở đường cho HTML/JS độc hại được phục vụ cùng origin.
 */
export default {
  /**
   * Tải một file lên qua multipart/form-data.
   *
   * Luôn để axios tự set boundary: header chỉ khai báo `multipart/form-data`, KHÔNG
   * tự ghép boundary (sai boundary là server trả 400). Dùng instance `api` chung nên
   * request tự có Bearer token; route này yêu cầu quyền admin.
   * @param {File} file - file người dùng chọn
   * @returns {Promise<object>} dữ liệu file đã upload (kèm URL trả về)
   */
  uploadFile(file) {
    const form = new FormData()
    form.append('file', file)
    return api.post('/api/admin/upload', form, {
      headers: { 'Content-Type': 'multipart/form-data' }
    }).then(r => r.data)
  }
}
