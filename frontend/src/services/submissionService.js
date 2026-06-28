import api from './api';

export const submissionService = {
  submitLessonSkill: async (lessonId, skillType, submissionText, audioUrl) => {
    const response = await api.post('/api/lesson-submissions/submit', {
      lessonId,
      skillType,
      submissionText,
      audioUrl
    });
    return response.data;
  },

  uploadAudio: async (file) => {
    const formData = new FormData();
    formData.append('file', file);
    const response = await api.post('/api/lesson-submissions/upload-audio', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    });
    return response.data;
  },

  getMySubmission: async (lessonId, skillType) => {
    const response = await api.get(`/api/lesson-submissions/my/lesson/${lessonId}/skill/${skillType}`);
    return response.data;
  },

  adminGetSubmissions: async () => {
    const response = await api.get('/api/admin/submissions');
    return response.data;
  },

  adminGradeSubmission: async (submissionId, score, feedback) => {
    const response = await api.put(`/api/admin/submissions/${submissionId}/grade`, {
      score,
      feedback
    });
    return response.data;
  }
};
