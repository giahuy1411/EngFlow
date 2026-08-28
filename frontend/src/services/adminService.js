import api from './api';

export const adminService = {
  // Stats
  getDashboardStats: async () => {
    const response = await api.get('/api/admin/stats');
    return response.data;
  },

  // Users
  getAllUsers: async (params = {}) => {
    const response = await api.get('/api/admin/users', { params });
    return response.data;
  },
  toggleUserActive: async (id) => {
    const response = await api.put(`/api/admin/users/${id}/toggle-active`);
    return response.data;
  },
  toggleUserAdmin: async (id) => {
    const response = await api.put(`/api/admin/users/${id}/toggle-admin`);
    return response.data;
  },
  toggleUserPremium: async (id) => {
    const response = await api.put(`/api/admin/users/${id}/toggle-premium`);
    return response.data;
  },
  revokeUserPremium: async (id) => {
    const response = await api.put(`/api/admin/users/${id}/revoke-premium`);
    return response.data;
  },

  // Lessons
  getAllLessons: async (params = {}) => {
    const response = await api.get('/api/admin/lessons', { params });
    return response.data;
  },
  getLesson: async (id) => {
    const response = await api.get(`/api/admin/lessons/${id}`);
    return response.data;
  },
  createLesson: async (lesson) => {
    const response = await api.post('/api/admin/lessons', lesson);
    return response.data;
  },
  updateLesson: async (id, lesson) => {
    const response = await api.put(`/api/admin/lessons/${id}`, lesson);
    return response.data;
  },
  deleteLesson: async (id) => {
    await api.delete(`/api/admin/lessons/${id}`);
  },
  toggleLessonPublish: async (id) => {
    const response = await api.put(`/api/admin/lessons/${id}/toggle-publish`);
    return response.data;
  },

  // Vocabulary
  getAllVocabulary: async (params = {}) => {
    const response = await api.get('/api/admin/vocabulary', { params });
    return response.data;
  },
  createVocabulary: async (vocabulary) => {
    const response = await api.post('/api/admin/vocabulary', vocabulary);
    return response.data;
  },
  updateVocabulary: async (id, vocabulary) => {
    const response = await api.put(`/api/admin/vocabulary/${id}`, vocabulary);
    return response.data;
  },
  deleteVocabulary: async (id) => {
    await api.delete(`/api/admin/vocabulary/${id}`);
  },

  // Exercises
  getAllExercises: async (params = {}) => {
    const response = await api.get('/api/admin/exercises', { params });
    return response.data;
  },
  getExercise: async (id) => {
    const response = await api.get(`/api/admin/exercises/${id}`);
    return response.data;
  },
  createExercise: async (exercise) => {
    const response = await api.post('/api/admin/exercises', exercise);
    return response.data;
  },
  updateExercise: async (id, exercise) => {
    const response = await api.put(`/api/admin/exercises/${id}`, exercise);
    return response.data;
  },
  deleteExercise: async (id) => {
    await api.delete(`/api/admin/exercises/${id}`);
  }
};
