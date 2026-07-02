import api from './http'

export const advanceApi = {
  list: (params) => api.get('/advances', { params }),
  detail: (id) => api.get(`/advances/${id}`),
  create: (data) => api.post('/advances', data),
  update: (id, data) => api.put(`/advances/${id}`, data),
  remove: (id) => api.delete(`/advances/${id}`),
  submit: (id) => api.post(`/advances/${id}/submit`),
}
