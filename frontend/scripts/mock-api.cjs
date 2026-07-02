const http = require('node:http')

const roles = {
  employee: 'EMPLOYEE',
  manager: 'DEPARTMENT_MANAGER',
  finance: 'FINANCE',
  office: 'OFFICE',
  executive: 'EXECUTIVE',
  cashier: 'CASHIER',
  admin: 'ADMIN',
}

const users = Object.fromEntries(Object.entries(roles).map(([username, role], index) => [
  role,
  {
    id: index + 1,
    username,
    realName: `${username} 演示用户`,
    role,
    departmentId: role === 'EXECUTIVE' ? 4 : 1,
    departmentName: role === 'EXECUTIVE' ? '院领导' : '科研管理部',
    enabled: true,
  },
]))

const json = (response, body, status = 200) => {
  response.writeHead(status, { 'Content-Type': 'application/json; charset=utf-8' })
  response.end(JSON.stringify(body))
}

const readJson = (request) => new Promise((resolve) => {
  let body = ''
  request.on('data', (chunk) => { body += chunk })
  request.on('end', () => {
    try { resolve(JSON.parse(body || '{}')) } catch { resolve({}) }
  })
})

http.createServer(async (request, response) => {
  const path = request.url.split('?')[0]
  if (path === '/api/health') return json(response, { status: 'ok' })
  if (path === '/api/auth/login') {
    const body = await readJson(request)
    const role = roles[body.username] || 'EMPLOYEE'
    return json(response, { token: `mock-${role}`, user: users[role] })
  }
  if (path === '/api/auth/logout') return json(response, { message: 'ok' })

  const token = request.headers['x-auth-token'] || ''
  const role = token.replace('mock-', '') || 'EMPLOYEE'
  const user = users[role] || users.EMPLOYEE
  if (path === '/api/auth/me' || path === '/api/profile') return json(response, user)
  if (path === '/api/dashboard/stats') {
    return json(response, {
      monthAmount: 32860,
      pendingCount: 4,
      reimbursementCount: 13,
      statusCounts: { DRAFT: 3, SUBMITTED: 2, COMPLETED: 7, REJECTED: 1 },
      budgets: [
        { departmentName: '科研管理部', totalAmount: 120000, usedAmount: 42000, remainingAmount: 78000 },
        { departmentName: '办公室', totalAmount: 80000, usedAmount: 26000, remainingAmount: 54000 },
      ],
      pendingOffsetCount: 2,
      overdueAdvanceCount: 1,
    })
  }
  if (path === '/api/ledger') {
    return json(response, { entries: [], totalIncome: 0, totalExpense: 0, balance: 0 })
  }
  return json(response, [])
}).listen(8080, '127.0.0.1', () => {
  console.log('Mock API listening on http://127.0.0.1:8080')
})
