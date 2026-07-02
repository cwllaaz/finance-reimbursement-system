import axios from 'axios'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 12000,
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('finance_token')
  if (token) config.headers['X-Auth-Token'] = token
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const isLoginRequest = String(error.config?.url || '').includes('/auth/login')
    if (error.response?.status === 401 && !isLoginRequest) {
      window.dispatchEvent(new CustomEvent('finance:session-expired'))
    }
    return Promise.reject(error)
  },
)

export default api
