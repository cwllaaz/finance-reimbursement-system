import api from './http'

export const financeApi = {
  dashboard: () => api.get('/dashboard/stats'),
  incomes: (params) => api.get('/incomes', { params }),
  ledger: (params) => api.get('/ledger', { params }),
  budgets: () => api.get('/budgets'),
}
