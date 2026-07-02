import api from './http'

export const systemApi = {
  login: (data) => api.post('/auth/login', data),
  me: () => api.get('/auth/me'),
  logout: () => api.post('/auth/logout'),
  users: (params) => api.get('/users', { params }),
  operationLogs: (params) => api.get('/operation-logs', { params }),
}
