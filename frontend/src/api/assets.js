import api from './http'

export const assetApi = {
  list: (params) => api.get('/assets', { params }),
  detail: (id) => api.get(`/assets/${id}`),
  eligiblePurchases: () => api.get('/assets/eligible-purchases'),
  claimantOptions: () => api.get('/assets/claimants'),
  accept: (data) => api.post('/assets/acceptance', data),
  claim: (id, data) => api.post(`/assets/${id}/claim`, data),
  claimApplications: () => api.get('/assets/claim-applications'),
  requestClaim: (id, data) => api.post(`/assets/${id}/claim-applications`, data),
  reviewClaim: (id, data) => api.post(`/assets/claim-applications/${id}/review`, data),
}
