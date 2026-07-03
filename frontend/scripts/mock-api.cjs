const http = require('node:http')
const port = Number(process.env.MOCK_API_PORT || 8080)

const roles = {
  employee: 'EMPLOYEE',
  manager: 'DEPARTMENT_MANAGER',
  finance: 'FINANCE',
  office: 'OFFICE',
  executive: 'EXECUTIVE',
  committee: 'COMMITTEE',
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
      reimbursementCount: 16,
      statusCounts: {
        DRAFT: 2,
        FINANCE_INITIAL_APPROVED: 1,
        EXECUTIVE_APPROVED: 1,
        PAID: 1,
        COMPLETED: 1,
        APPROVED: 9,
        REJECTED: 1,
      },
      budgets: [
        { departmentName: '科研管理部', totalAmount: 120000, usedAmount: 42000, remainingAmount: 78000 },
        { departmentName: '办公室', totalAmount: 80000, usedAmount: 26000, remainingAmount: 54000 },
      ],
      pendingOffsetCount: 2,
      overdueAdvanceCount: 1,
    })
  }
  if (path === '/api/workbench/MY_TODOS') {
    const now = Date.now()
    const samples = [
      ['REIMBURSEMENT', 101, 'BX20260702001', '培训费用报销', 'SUBMITTED', 3250, 10],
      ['PURCHASE', 201, 'CG20260702001', '实验室显示器申购', 'FINANCE_APPROVED', 12680, 25],
      ['LABOR', 301, 'LW20260702001', '专家咨询费发放', 'DEPARTMENT_APPROVED', 5600, 70],
      ['ADVANCE', 401, 'YF20260702001', '会议场地预付款', 'EXECUTIVE_APPROVED', 18000, 180],
      ['REIMBURSEMENT', 102, 'BX20260701002', '差旅交通费报销', 'PAID', 980, 360],
      ['PURCHASE', 202, 'CG20260701002', '办公用品申购', 'SUBMITTED', 1680, 1440],
    ]
    return json(response, samples.map(([businessType, businessId, number, title, status, amount, minutes]) => ({
      businessType,
      businessId,
      number,
      title,
      applicantId: 1,
      applicantName: '张同学',
      departmentId: 1,
      departmentName: '科研管理部',
      amount,
      status,
      time: new Date(now - minutes * 60000).toISOString(),
    })))
  }
  if (path === '/api/reimbursements/payment-tasks') {
    return json(response, [
      { id: 501, approvalNumber: 'BX20260702011', title: '待付款报销一' },
      { id: 502, approvalNumber: 'BX20260702012', title: '待付款报销二' },
      { id: 503, approvalNumber: 'BX20260702013', title: '待付款报销三' },
    ])
  }
  if (path === '/api/ledger') {
    return json(response, { entries: [], totalIncome: 0, totalExpense: 0, balance: 0 })
  }
  return json(response, [])
}).listen(port, '127.0.0.1', () => {
  console.log(`Mock API listening on http://127.0.0.1:${port}`)
})
