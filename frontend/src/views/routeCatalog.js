export const routeCatalog = [
  ['dashboard', '/dashboard', '首页仪表盘'],
  ['myApplications', '/my-applications', '我的申请'],
  ['myTodos', '/my-todos', '我的待办'],
  ['doneItems', '/done-items', '已办事项'],
  ['reimbursement', '/reimbursements', '报销申请'],
  ['purchases', '/purchases', '申购管理'],
  ['assets', '/assets', '资产出入库'],
  ['labor', '/labor', '劳务酬金'],
  ['advances', '/advances', '暂借款 / 预付款'],
  ['approval', '/approvals', '审批管理'],
  ['paymentTasks', '/payment-tasks', '付款任务'],
  ['incomes', '/incomes', '收入登记'],
  ['ledger', '/ledger', '财务总台账'],
  ['budget', '/budgets', '预算管理'],
  ['report', '/reports', '数据统计'],
  ['users', '/users', '用户管理'],
  ['operationLogs', '/operation-logs', '操作日志'],
  ['profile', '/profile', '个人资料'],
]

export const menuPathMap = Object.fromEntries(routeCatalog.map(([menu, path]) => [menu, path]))
