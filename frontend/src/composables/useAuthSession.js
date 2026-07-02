import { ref } from 'vue'

const TOKEN_KEY = 'finance_token'
const USER_KEY = 'finance_user'

const token = ref(localStorage.getItem(TOKEN_KEY) || '')
const currentUser = ref(readStoredUser())

function readStoredUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY) || 'null')
  } catch {
    localStorage.removeItem(USER_KEY)
    return null
  }
}

export function useAuthSession() {
  const setSession = (nextToken, user) => {
    token.value = nextToken || ''
    currentUser.value = user || null
    if (token.value) localStorage.setItem(TOKEN_KEY, token.value)
    else localStorage.removeItem(TOKEN_KEY)
    if (currentUser.value) localStorage.setItem(USER_KEY, JSON.stringify(currentUser.value))
    else localStorage.removeItem(USER_KEY)
  }

  const updateUser = (user) => {
    currentUser.value = user || null
    if (currentUser.value) localStorage.setItem(USER_KEY, JSON.stringify(currentUser.value))
    else localStorage.removeItem(USER_KEY)
  }

  const clearSession = () => setSession('', null)

  return { token, currentUser, setSession, updateUser, clearSession }
}

export function getStoredSession() {
  return {
    token: localStorage.getItem(TOKEN_KEY) || '',
    user: readStoredUser(),
  }
}
