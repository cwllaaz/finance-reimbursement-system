import api from './http'

export const reimbursementApi = {
  list: (params) => api.get('/reimbursements', { params }),
  detail: (id) => api.get(`/reimbursements/${id}/detail`),
  create: (data) => api.post('/reimbursements', data),
  update: (id, data) => api.put(`/reimbursements/${id}`, data),
  remove: (id) => api.delete(`/reimbursements/${id}`),
  submit: (id) => api.post(`/reimbursements/${id}/submit`),
}
