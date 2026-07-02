export const routeAccess = {
  dashboard: ['DEPARTMENT_MANAGER', 'FINANCE', 'EXECUTIVE', 'ADMIN'],
  myApplications: ['EMPLOYEE', 'DEPARTMENT_MANAGER', 'FINANCE', 'OFFICE', 'EXECUTIVE', 'CASHIER', 'ADMIN'],
  myTodos: ['DEPARTMENT_MANAGER', 'FINANCE', 'EXECUTIVE', 'CASHIER', 'ADMIN'],
  doneItems: ['DEPARTMENT_MANAGER', 'FINANCE', 'EXECUTIVE', 'CASHIER', 'ADMIN'],
  reimbursement: ['EMPLOYEE', 'DEPARTMENT_MANAGER', 'FINANCE', 'EXECUTIVE', 'ADMIN'],
  purchases: ['EMPLOYEE', 'DEPARTMENT_MANAGER', 'FINANCE', 'OFFICE', 'EXECUTIVE', 'ADMIN'],
  assets: ['EMPLOYEE', 'DEPARTMENT_MANAGER', 'FINANCE', 'OFFICE', 'EXECUTIVE', 'CASHIER', 'ADMIN'],
  labor: ['EMPLOYEE', 'DEPARTMENT_MANAGER', 'FINANCE', 'EXECUTIVE', 'CASHIER', 'ADMIN'],
  advances: ['EMPLOYEE', 'DEPARTMENT_MANAGER', 'FINANCE', 'EXECUTIVE', 'CASHIER', 'ADMIN'],
  approval: ['DEPARTMENT_MANAGER', 'FINANCE', 'EXECUTIVE', 'ADMIN'],
  paymentTasks: ['FINANCE', 'CASHIER', 'ADMIN'],
  incomes: ['FINANCE', 'ADMIN'],
  ledger: ['DEPARTMENT_MANAGER', 'FINANCE', 'EXECUTIVE', 'ADMIN'],
  budget: ['FINANCE', 'ADMIN'],
  report: ['FINANCE', 'EXECUTIVE', 'ADMIN'],
  users: ['ADMIN'],
  operationLogs: ['ADMIN'],
  profile: ['EMPLOYEE', 'DEPARTMENT_MANAGER', 'FINANCE', 'OFFICE', 'EXECUTIVE', 'CASHIER', 'ADMIN'],
}

export const defaultMenuForRole = (role) => {
  if (role === 'EMPLOYEE' || role === 'OFFICE') return 'myApplications'
  if (role === 'CASHIER') return 'myTodos'
  if (routeAccess.dashboard.includes(role)) return 'dashboard'
  return 'myApplications'
}

export const canAccessMenu = (role, menu) => Boolean(role && routeAccess[menu]?.includes(role))
