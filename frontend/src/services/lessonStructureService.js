import api from './api';

export default {
  getStructure(lessonId) {
    return api.get(`/api/lessons/${lessonId}/structure`).then(r => r.data);
  },
  getAdminStructure(lessonId) {
    return api.get(`/api/admin/lessons/${lessonId}/structure`).then(r => r.data);
  },
  addSection(lessonId, data) {
    return api.post(`/api/admin/lessons/${lessonId}/sections`, data).then(r => r.data);
  },
  updateSection(sectionId, data) {
    return api.put(`/api/admin/sections/${sectionId}`, data).then(r => r.data);
  },
  deleteSection(sectionId) {
    return api.delete(`/api/admin/sections/${sectionId}`);
  },
  addBlock(sectionId, data) {
    return api.post(`/api/admin/sections/${sectionId}/blocks`, data).then(r => r.data);
  },
  updateBlock(blockId, data) {
    return api.put(`/api/admin/blocks/${blockId}`, data).then(r => r.data);
  },
  deleteBlock(blockId) {
    return api.delete(`/api/admin/blocks/${blockId}`);
  },
  uploadFile(file) {
    const form = new FormData();
    form.append('file', file);
    return api.post('/api/admin/upload', form, {
      headers: { 'Content-Type': 'multipart/form-data' }
    }).then(r => r.data);
  },

  getSnapshots(lessonId) {
    return api.get(`/api/admin/lessons/${lessonId}/snapshots`).then(r => r.data);
  },

  takeSnapshot(lessonId) {
    return api.post(`/api/admin/lessons/${lessonId}/snapshots`);
  },

  restoreSnapshot(lessonId, snapshotId) {
    return api.post(`/api/admin/lessons/${lessonId}/snapshots/${snapshotId}/restore`).then(r => r.data);
  }
};
