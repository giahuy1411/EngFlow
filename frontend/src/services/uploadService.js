import api from './api'

/**
 * Admin media upload (first-party files served back under /api/resources/**).
 *
 * audit-v15: extracted from the deleted `lessonStructureService` (Lesson Builder).
 * The endpoint itself is shared infrastructure — `AdminExercises.vue` uses it to
 * upload exercise images/audio, and speaking media is served by the same route —
 * so only the Lesson Builder methods were removed, not the upload.
 */
export default {
  uploadFile(file) {
    const form = new FormData()
    form.append('file', file)
    return api.post('/api/admin/upload', form, {
      headers: { 'Content-Type': 'multipart/form-data' }
    }).then(r => r.data)
  }
}
