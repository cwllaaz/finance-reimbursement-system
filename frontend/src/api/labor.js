import api from './http'

export const laborApi = {
  list: (params) => api.get('/labor-applications', { params }),
  detail: (id) => api.get(`/labor-applications/${id}`),
  create: (data) => api.post('/labor-applications', data),
  update: (id, data) => api.put(`/labor-applications/${id}`, data),
  remove: (id) => api.delete(`/labor-applications/${id}`),
  submit: (id) => api.post(`/labor-applications/${id}/submit`),
}
