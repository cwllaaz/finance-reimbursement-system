import api from './http'

export const purchaseApi = {
  list: (params) => api.get('/purchases', { params }),
  detail: (id) => api.get(`/purchases/${id}`),
  create: (data) => api.post('/purchases', data),
  update: (id, data) => api.put(`/purchases/${id}`, data),
  remove: (id) => api.delete(`/purchases/${id}`),
  submit: (id) => api.post(`/purchases/${id}/submit`),
}
