<script setup>
import axios from 'axios'
import * as echarts from 'echarts'
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  DataAnalysis,
  Delete,
  Document,
  Edit,
  Finished,
  HomeFilled,
  Money,
  Plus,
  Refresh,
  Search,
  SwitchButton,
  Tickets,
  UploadFilled,
  User,
  Wallet,
} from '@element-plus/icons-vue'

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || '/api'
const fileBaseUrl = import.meta.env.VITE_FILE_BASE_URL || ''
const api = axios.create({ baseURL: apiBaseUrl, timeout: 12000 })

const token = ref(localStorage.getItem('finance_token') || '')
const currentUser = ref(null)
const activeMenu = ref('dashboard')
const backendOnline = ref(false)
const tableLoading = ref(false)
const pendingLoading = ref(false)
const budgetLoading = ref(false)
const userLoading = ref(false)
const operationLogLoading = ref(false)
const timelineLoading = ref(false)
const exportLoading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const approvalDialogVisible = ref(false)
const recordDialogVisible = ref(false)
const budgetDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const userDialogVisible = ref(false)
const dialogMode = ref('create')
const userDialogMode = ref('create')
const approvalAction = ref('APPROVE')
const selectedApprovalRow = ref(null)
const selectedBudget = ref(null)
const detailData = ref(null)
const detailTimeline = ref([])
const approvalOcrCheck = ref(null)
const approvalOcrCheckLoading = ref(false)
const invoiceOcrLoading = ref(false)
const invoiceOcrSaving = ref(false)
const reimbursements = ref([])
const pendingReimbursements = ref([])
const approvalRecords = ref([])
const budgets = ref([])
const users = ref([])
const operationLogs = ref([])
const departments = ref([])
const dashboardStats = ref({
  monthAmount: 0,
  pendingCount: 0,
  reimbursementCount: 0,
  statusCounts: {},
  budgets: [],
})

const statusChartRef = ref(null)
const budgetChartRef = ref(null)
let statusChart = null
let budgetChart = null

api.interceptors.request.use((config) => {
  if (token.value) config.headers['X-Auth-Token'] = token.value
  return config
})

const loginForm = reactive({ username: 'employee', password: '123456' })
const searchForm = reactive({ keyword: '', status: '' })
const pendingSearchForm = reactive({ keyword: '' })
const userSearchForm = reactive({ keyword: '', role: '' })
const operationLogSearchForm = reactive({ keyword: '', module: '', action: '' })
const approvalForm = reactive({ comment: '' })
const budgetForm = reactive({ totalAmount: 0 })
const profileForm = reactive({ realName: '', phone: '', email: '' })
const form = reactive({
  id: null,
  title: '',
  expenseType: '',
  amount: null,
  expenseDate: '',
  description: '',
})
const invoiceOcrForm = reactive({
  invoiceCode: '',
  invoiceNumber: '',
  invoiceDate: '',
  amount: null,
  taxAmount: null,
  amountMatched: null,
  amountDifference: null,
  verificationMessage: '',
  sellerName: '',
  buyerName: '',
  ocrStatus: 'UNRECOGNIZED',
})
const userForm = reactive({
  id: null,
  username: '',
  password: '',
  realName: '',
  role: 'EMPLOYEE',
  departmentId: null,
  phone: '',
  email: '',
  enabled: true,
})

const statusOptions = [
  { label: '草稿', value: 'DRAFT', type: 'info' },
  { label: '部门审批中', value: 'SUBMITTED', type: 'warning' },
  { label: '财务审批中', value: 'DEPARTMENT_APPROVED', type: 'primary' },
  { label: '已通过', value: 'APPROVED', type: 'success' },
  { label: '已驳回', value: 'REJECTED', type: 'danger' },
]
const expenseTypes = ['交通费', '住宿费', '餐饮费', '办公用品', '培训费', '其他']
const demoAccounts = [
  { username: 'employee', password: '123456', role: '员工', note: '提交自己的报销单' },
  { username: 'manager', password: '123456', role: '部门负责人', note: '审批本部门报销' },
  { username: 'finance', password: '123456', role: '财务人员', note: '财务审批并扣预算' },
  { username: 'admin', password: '123456', role: '管理员', note: '查看全部数据' },
]
const roleLabels = {
  EMPLOYEE: '员工',
  DEPARTMENT_MANAGER: '部门负责人',
  FINANCE: '财务人员',
  ADMIN: '管理员',
}
const roleOptions = Object.entries(roleLabels).map(([value, label]) => ({ value, label }))
const operationLogModules = ['认证', '报销管理', '审批管理', '预算管理', '附件管理', 'OCR识别', '报表导出', '用户管理', '个人资料']
const operationLogActions = [
  '登录成功',
  '新增报销单',
  '编辑报销单',
  '删除报销单',
  '提交报销单',
  '部门审批',
  '财务审批',
  '修改预算',
  '上传发票附件',
  'OCR 识别',
  'OCR 保存',
  'OCR 确认',
  '导出 Excel',
  '新增用户',
  '编辑用户',
  '删除用户',
  '启用用户',
  '禁用用户',
  '修改个人资料',
  '修改密码',
]
const ocrStatusLabels = {
  UNRECOGNIZED: '未识别',
  RECOGNIZED: '已识别',
  CONFIRMED: '已确认',
}
const rules = {
  title: [{ required: true, message: '请输入报销标题', trigger: 'blur' }],
  expenseType: [{ required: true, message: '请选择费用类型', trigger: 'change' }],
  amount: [{ required: true, message: '请输入金额', trigger: 'blur' }],
  expenseDate: [{ required: true, message: '请选择发生日期', trigger: 'change' }],
}
const userRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
}
const profileRules = {
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
}
const formRef = ref()
const userFormRef = ref()
const profileFormRef = ref()

const statusMap = computed(() => Object.fromEntries(statusOptions.map((item) => [item.value, item])))
const isLoggedIn = computed(() => Boolean(currentUser.value && token.value))
const canCreateReimbursement = computed(() => ['EMPLOYEE', 'ADMIN'].includes(currentUser.value?.role))
const canApprove = computed(() => ['DEPARTMENT_MANAGER', 'FINANCE', 'ADMIN'].includes(currentUser.value?.role))
const canManageBudget = computed(() => ['FINANCE', 'ADMIN'].includes(currentUser.value?.role))
const canManageUsers = computed(() => currentUser.value?.role === 'ADMIN')
const canViewOperationLogs = computed(() => currentUser.value?.role === 'ADMIN')
const canExportReimbursements = computed(() => ['FINANCE', 'ADMIN'].includes(currentUser.value?.role))
const menuItems = computed(() => {
  const role = currentUser.value?.role
  const items = [
    { index: 'dashboard', label: '首页仪表盘', icon: HomeFilled },
    { index: 'reimbursement', label: '报销申请', icon: Tickets },
    { index: 'profile', label: '个人资料', icon: User },
  ]
  if (['DEPARTMENT_MANAGER', 'FINANCE', 'ADMIN'].includes(role)) {
    items.push({ index: 'approval', label: '审批管理', icon: Finished })
  }
  if (['FINANCE', 'ADMIN'].includes(role)) {
    items.push({ index: 'budget', label: '预算管理', icon: Wallet })
    items.push({ index: 'report', label: '数据统计', icon: DataAnalysis })
  }
  if (role === 'ADMIN') {
    items.push({ index: 'users', label: '用户管理', icon: User })
    items.push({ index: 'operationLogs', label: '操作日志', icon: Document })
  }
  return items
})
const pageTitle = computed(() => menuItems.value.find((item) => item.index === activeMenu.value)?.label || '首页仪表盘')
const approvedCount = computed(() => reimbursements.value.filter((item) => item.status === 'APPROVED').length)
const budgetUsedPercent = computed(() => {
  const list = dashboardStats.value.budgets || []
  const total = list.reduce((sum, item) => sum + Number(item.totalAmount || 0), 0)
  const used = list.reduce((sum, item) => sum + Number(item.usedAmount || 0), 0)
  return total ? Math.round((used / total) * 100) : 0
})

const formatMoney = (value) => `￥${Number(value || 0).toFixed(2)}`
const formatDateTime = (value) => value ? String(value).replace('T', ' ').slice(0, 19) : '-'
const getStatusLabel = (status) => statusMap.value[status]?.label || status
const getStatusType = (status) => statusMap.value[status]?.type || 'info'
const timelineStatusLabels = {
  COMPLETED: '已完成',
  IN_PROGRESS: '进行中',
  NOT_STARTED: '未开始',
  REJECTED: '已驳回',
}
const timelineStatusTypes = {
  COMPLETED: 'success',
  IN_PROGRESS: 'primary',
  NOT_STARTED: 'info',
  REJECTED: 'danger',
}
const timelineStatusColors = {
  COMPLETED: '#22c55e',
  IN_PROGRESS: '#3b82f6',
  NOT_STARTED: '#94a3b8',
  REJECTED: '#ef4444',
}
const getTimelineStatusLabel = (status) => timelineStatusLabels[status] || status || '-'
const getTimelineStatusType = (status) => timelineStatusTypes[status] || 'info'
const getTimelineColor = (status) => timelineStatusColors[status] || '#94a3b8'
const fillAccount = (account) => {
  loginForm.username = account.username
  loginForm.password = account.password
}
const fillProfileForm = (data) => {
  profileForm.realName = data?.realName || ''
  profileForm.phone = data?.phone || ''
  profileForm.email = data?.email || ''
}
const canEditRow = (row) => row.status === 'DRAFT' && (currentUser.value?.role === 'ADMIN' || row.applicantId === currentUser.value?.id)
const canSubmitRow = (row) => row.status === 'DRAFT' && row.applicantId === currentUser.value?.id
const canUploadRow = (row) => currentUser.value?.role === 'ADMIN' || row.applicantId === currentUser.value?.id
const ocrStatusLabel = (status) => ocrStatusLabels[status] || status || '未识别'
const ocrStatusType = (status) => {
  if (status === 'CONFIRMED') return 'success'
  if (status === 'RECOGNIZED') return 'warning'
  return 'info'
}
const getOcrVerificationLevel = (amountMatched, amount) => {
  if (amountMatched === true) return 'success'
  if (amount == null) return 'warning'
  return 'danger'
}
const getOcrVerificationType = (amountMatched, amount) => {
  const level = getOcrVerificationLevel(amountMatched, amount)
  if (level === 'success') return 'success'
  if (level === 'warning') return 'warning'
  return 'danger'
}
const fillInvoiceOcrForm = (data) => {
  invoiceOcrForm.invoiceCode = data?.invoiceCode || ''
  invoiceOcrForm.invoiceNumber = data?.invoiceNumber || ''
  invoiceOcrForm.invoiceDate = data?.invoiceDate || ''
  invoiceOcrForm.amount = data?.amount == null ? null : Number(data.amount)
  invoiceOcrForm.taxAmount = data?.taxAmount == null ? null : Number(data.taxAmount)
  invoiceOcrForm.amountMatched = data?.amountMatched ?? null
  invoiceOcrForm.amountDifference = data?.amountDifference == null ? null : Number(data.amountDifference)
  invoiceOcrForm.verificationMessage = data?.verificationMessage || ''
  invoiceOcrForm.sellerName = data?.sellerName || ''
  invoiceOcrForm.buyerName = data?.buyerName || ''
  invoiceOcrForm.ocrStatus = data?.ocrStatus || 'UNRECOGNIZED'
}

const checkBackend = async () => {
  try {
    await api.get('/health')
    backendOnline.value = true
  } catch {
    backendOnline.value = false
  }
}
const restoreLogin = async () => {
  if (!token.value) return
  try {
    const response = await api.get('/auth/me')
    currentUser.value = response.data
    fillProfileForm(response.data)
    await refreshAll()
  } catch {
    token.value = ''
    localStorage.removeItem('finance_token')
  }
}
const handleLogin = async () => {
  if (!loginForm.username || !loginForm.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  try {
    const response = await api.post('/auth/login', loginForm)
    token.value = response.data.token
    currentUser.value = response.data.user
    fillProfileForm(response.data.user)
    localStorage.setItem('finance_token', token.value)
    activeMenu.value = 'dashboard'
    await refreshAll()
    ElMessage.success('登录成功')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '登录失败')
  }
}
const handleLogout = async () => {
  try {
    await api.post('/auth/logout')
  } catch {
    // local cleanup still happens
  }
  token.value = ''
  currentUser.value = null
  activeMenu.value = 'dashboard'
  reimbursements.value = []
  pendingReimbursements.value = []
  budgets.value = []
  users.value = []
  operationLogs.value = []
  departments.value = []
  fillProfileForm(null)
  localStorage.removeItem('finance_token')
}
const refreshAll = async () => {
  await Promise.all([
    loadDashboardStats(),
    loadReimbursements(),
    loadPendingReimbursements(),
    loadBudgets(),
    loadUsers(),
    loadDepartments(),
    loadOperationLogs(),
  ])
  await nextTick()
  renderCharts()
}
const loadDashboardStats = async () => {
  if (!token.value) return
  try {
    dashboardStats.value = (await api.get('/dashboard/stats')).data
  } catch {
    // Dashboard is decorative; leave old values if it fails.
  }
}
const loadReimbursements = async () => {
  if (!token.value) return
  tableLoading.value = true
  try {
    const params = {}
    if (searchForm.keyword) params.keyword = searchForm.keyword
    if (searchForm.status) params.status = searchForm.status
    reimbursements.value = (await api.get('/reimbursements', { params })).data
    backendOnline.value = true
  } catch (error) {
    backendOnline.value = false
    ElMessage.error(error.response?.data?.message || '读取报销列表失败')
  } finally {
    tableLoading.value = false
  }
}
const exportReimbursements = async () => {
  exportLoading.value = true
  try {
    const response = await api.get('/reimbursements/export', { responseType: 'blob' })
    const blob = new Blob([response.data], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = '报销申请数据.xlsx'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('Excel 导出成功')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || 'Excel 导出失败')
  } finally {
    exportLoading.value = false
  }
}
const loadPendingReimbursements = async () => {
  if (!token.value || !canApprove.value) {
    pendingReimbursements.value = []
    return
  }
  pendingLoading.value = true
  try {
    const params = pendingSearchForm.keyword ? { keyword: pendingSearchForm.keyword } : {}
    pendingReimbursements.value = (await api.get('/reimbursements/pending', { params })).data
  } catch {
    ElMessage.error('读取待审批列表失败')
  } finally {
    pendingLoading.value = false
  }
}
const loadBudgets = async () => {
  if (!token.value || !canManageBudget.value) {
    budgets.value = []
    return
  }
  budgetLoading.value = true
  try {
    budgets.value = (await api.get('/budgets')).data
  } catch {
    ElMessage.error('读取预算列表失败')
  } finally {
    budgetLoading.value = false
  }
}
const loadDepartments = async () => {
  if (!token.value || !canManageUsers.value) {
    departments.value = []
    return
  }
  try {
    departments.value = (await api.get('/departments')).data
  } catch {
    ElMessage.error('读取部门列表失败')
  }
}
const loadUsers = async () => {
  if (!token.value || !canManageUsers.value) {
    users.value = []
    return
  }
  userLoading.value = true
  try {
    const params = {}
    if (userSearchForm.keyword) params.keyword = userSearchForm.keyword
    if (userSearchForm.role) params.role = userSearchForm.role
    users.value = (await api.get('/users', { params })).data
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '读取用户列表失败')
  } finally {
    userLoading.value = false
  }
}
const loadOperationLogs = async () => {
  if (!token.value || !canViewOperationLogs.value) {
    operationLogs.value = []
    return
  }
  operationLogLoading.value = true
  try {
    const params = {}
    if (operationLogSearchForm.keyword) params.keyword = operationLogSearchForm.keyword
    if (operationLogSearchForm.module) params.module = operationLogSearchForm.module
    if (operationLogSearchForm.action) params.action = operationLogSearchForm.action
    operationLogs.value = (await api.get('/operation-logs', { params })).data
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '读取操作日志失败')
  } finally {
    operationLogLoading.value = false
  }
}
const loadProfile = async () => {
  if (!token.value) return
  try {
    const response = await api.get('/profile')
    fillProfileForm(response.data)
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '读取个人资料失败')
  }
}

const ensureChart = (chart, element) => {
  if (!element) return null
  if (!chart || chart.getDom() !== element) {
    chart?.dispose()
    return echarts.init(element)
  }
  return chart
}

const renderCharts = () => {
  statusChart = ensureChart(statusChart, statusChartRef.value)
  budgetChart = ensureChart(budgetChart, budgetChartRef.value)
  if (!statusChart || !budgetChart) return

  const counts = dashboardStats.value.statusCounts || {}
  statusChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: ['42%', '68%'],
      center: ['50%', '45%'],
      data: statusOptions.map((item) => ({ name: item.label, value: counts[item.value] || 0 })),
    }],
  })

  const budgetList = dashboardStats.value.budgets || []
  budgetChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { top: 0 },
    grid: { left: 48, right: 24, top: 42, bottom: 32 },
    xAxis: { type: 'category', data: budgetList.map((item) => item.departmentName || '部门') },
    yAxis: { type: 'value' },
    series: [
      { name: '已使用', type: 'bar', stack: 'budget', data: budgetList.map((item) => Number(item.usedAmount || 0)) },
      { name: '剩余额度', type: 'bar', stack: 'budget', data: budgetList.map((item) => Number(item.remainingAmount || 0)) },
    ],
  })
  requestAnimationFrame(() => resizeCharts())
}
const resizeCharts = () => {
  statusChart?.resize()
  budgetChart?.resize()
}

const resetForm = () => {
  form.id = null
  form.title = ''
  form.expenseType = ''
  form.amount = null
  form.expenseDate = ''
  form.description = ''
  formRef.value?.clearValidate()
}
const openCreateDialog = () => {
  dialogMode.value = 'create'
  resetForm()
  dialogVisible.value = true
}
const openEditDialog = (row) => {
  dialogMode.value = 'edit'
  form.id = row.id
  form.title = row.title
  form.expenseType = row.expenseType
  form.amount = Number(row.amount)
  form.expenseDate = row.expenseDate
  form.description = row.description || ''
  dialogVisible.value = true
}
const submitForm = async () => {
  await formRef.value.validate()
  saving.value = true
  const payload = {
    title: form.title,
    expenseType: form.expenseType,
    amount: Number(form.amount),
    expenseDate: form.expenseDate,
    description: form.description,
  }
  try {
    if (dialogMode.value === 'create') {
      await api.post('/reimbursements', payload)
      ElMessage.success('报销单新增成功')
    } else {
      await api.put(`/reimbursements/${form.id}`, payload)
      ElMessage.success('报销单修改成功')
    }
    dialogVisible.value = false
    await refreshAll()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}
const submitReimbursement = async (row) => {
  await ElMessageBox.confirm(`确定提交“${row.title}”进入审批吗？`, '提交确认', { type: 'info' })
  try {
    await api.post(`/reimbursements/${row.id}/submit`)
    ElMessage.success('提交成功，进入部门审批中')
    await refreshAll()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '提交失败')
  }
}
const deleteReimbursement = async (row) => {
  await ElMessageBox.confirm(`确定删除“${row.title}”吗？`, '删除确认', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消',
  })
  try {
    await api.delete(`/reimbursements/${row.id}`)
    ElMessage.success('删除成功')
    await refreshAll()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '删除失败')
  }
}
const openApprovalDialog = async (row, action) => {
  selectedApprovalRow.value = row
  approvalAction.value = action
  approvalForm.comment = action === 'APPROVE' ? '同意报销申请' : '材料不完整，退回修改'
  approvalDialogVisible.value = true
  approvalOcrCheck.value = null
  approvalOcrCheckLoading.value = true
  try {
    approvalOcrCheck.value = (await api.get(`/reimbursements/${row.id}/invoice-ocr`)).data
  } catch {
    approvalOcrCheck.value = null
  } finally {
    approvalOcrCheckLoading.value = false
  }
}
const submitApproval = async () => {
  if (!selectedApprovalRow.value) return
  saving.value = true
  try {
    await api.post(`/reimbursements/${selectedApprovalRow.value.id}/approval`, {
      action: approvalAction.value,
      comment: approvalForm.comment,
    })
    ElMessage.success(approvalAction.value === 'APPROVE' ? '审批通过' : '已驳回')
    approvalDialogVisible.value = false
    await refreshAll()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '审批失败')
  } finally {
    saving.value = false
  }
}
const openRecords = async (row) => {
  try {
    approvalRecords.value = (await api.get(`/reimbursements/${row.id}/approval-records`)).data
    recordDialogVisible.value = true
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '读取审批记录失败')
  }
}
const openDetail = async (row) => {
  try {
    timelineLoading.value = true
    const [detailResponse, timelineResponse] = await Promise.all([
      api.get(`/reimbursements/${row.id}/detail`),
      api.get(`/reimbursements/${row.id}/timeline`),
    ])
    detailData.value = detailResponse.data
    detailTimeline.value = timelineResponse.data
    fillInvoiceOcrForm(detailData.value.invoiceOcr)
    detailDialogVisible.value = true
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '读取详情失败')
  } finally {
    timelineLoading.value = false
  }
}
const uploadInvoice = async ({ file }) => {
  if (!detailData.value?.reimbursement?.id) return
  const formData = new FormData()
  formData.append('file', file)
  try {
    await api.post(`/reimbursements/${detailData.value.reimbursement.id}/attachments`, formData)
    ElMessage.success('发票上传成功')
    await openDetail(detailData.value.reimbursement)
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '上传失败')
  }
}
const runOcr = async () => {
  if (!detailData.value?.reimbursement?.id) return
  invoiceOcrLoading.value = true
  try {
    const response = await api.post(`/reimbursements/${detailData.value.reimbursement.id}/ocr/baidu`)
    detailData.value.invoiceOcr = response.data
    fillInvoiceOcrForm(response.data)
    ElMessage.success('OCR 识别完成')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || 'OCR 识别失败')
  } finally {
    invoiceOcrLoading.value = false
  }
}
const saveInvoiceOcr = async () => {
  if (!detailData.value?.reimbursement?.id) return
  invoiceOcrSaving.value = true
  try {
    const payload = {
      invoiceCode: invoiceOcrForm.invoiceCode,
      invoiceNumber: invoiceOcrForm.invoiceNumber,
      invoiceDate: invoiceOcrForm.invoiceDate,
      amount: invoiceOcrForm.amount == null ? null : Number(invoiceOcrForm.amount),
      taxAmount: invoiceOcrForm.taxAmount == null ? null : Number(invoiceOcrForm.taxAmount),
      sellerName: invoiceOcrForm.sellerName,
      buyerName: invoiceOcrForm.buyerName,
    }
    const response = await api.put(`/reimbursements/${detailData.value.reimbursement.id}/invoice-ocr`, payload)
    detailData.value.invoiceOcr = response.data
    fillInvoiceOcrForm(response.data)
    ElMessage.success('发票信息已保存')
    return true
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '保存发票信息失败')
    return false
  } finally {
    invoiceOcrSaving.value = false
  }
}
const confirmInvoiceOcr = async () => {
  if (!detailData.value?.reimbursement?.id) return
  try {
    const saved = await saveInvoiceOcr()
    if (!saved) return
    const response = await api.post(`/reimbursements/${detailData.value.reimbursement.id}/invoice-ocr/confirm`)
    detailData.value.invoiceOcr = response.data
    fillInvoiceOcrForm(response.data)
    ElMessage.success('发票信息已确认')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '确认发票信息失败')
  }
}
const openBudgetDialog = (row) => {
  selectedBudget.value = row
  budgetForm.totalAmount = Number(row.totalAmount || 0)
  budgetDialogVisible.value = true
}
const submitBudget = async () => {
  if (!selectedBudget.value) return
  saving.value = true
  try {
    await api.put(`/budgets/${selectedBudget.value.id}`, { totalAmount: Number(budgetForm.totalAmount) })
    ElMessage.success('预算设置成功')
    budgetDialogVisible.value = false
    await refreshAll()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '预算设置失败')
  } finally {
    saving.value = false
  }
}
const resetUserForm = () => {
  userForm.id = null
  userForm.username = ''
  userForm.password = ''
  userForm.realName = ''
  userForm.role = 'EMPLOYEE'
  userForm.departmentId = null
  userForm.phone = ''
  userForm.email = ''
  userForm.enabled = true
  userFormRef.value?.clearValidate()
}
const openCreateUserDialog = async () => {
  userDialogMode.value = 'create'
  resetUserForm()
  await loadDepartments()
  userDialogVisible.value = true
}
const openEditUserDialog = async (row) => {
  userDialogMode.value = 'edit'
  userForm.id = row.id
  userForm.username = row.username
  userForm.password = ''
  userForm.realName = row.realName
  userForm.role = row.role
  userForm.departmentId = row.departmentId || null
  userForm.phone = row.phone || ''
  userForm.email = row.email || ''
  userForm.enabled = Boolean(row.enabled)
  await loadDepartments()
  userDialogVisible.value = true
}
const submitUserForm = async () => {
  await userFormRef.value.validate()
  if (userDialogMode.value === 'create' && !userForm.password) {
    ElMessage.warning('新增用户需要填写初始密码')
    return
  }
  saving.value = true
  const payload = {
    username: userForm.username,
    password: userForm.password,
    realName: userForm.realName,
    role: userForm.role,
    departmentId: userForm.departmentId,
    phone: userForm.phone,
    email: userForm.email,
    enabled: userForm.enabled,
  }
  try {
    if (userDialogMode.value === 'create') {
      await api.post('/users', payload)
      ElMessage.success('用户新增成功')
    } else {
      await api.put(`/users/${userForm.id}`, payload)
      ElMessage.success('用户修改成功')
    }
    userDialogVisible.value = false
    await loadUsers()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '保存用户失败')
  } finally {
    saving.value = false
  }
}
const deleteUser = async (row) => {
  await ElMessageBox.confirm(`确定删除用户“${row.realName}”吗？`, '删除确认', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消',
  })
  try {
    await api.delete(`/users/${row.id}`)
    ElMessage.success('用户删除成功')
    await loadUsers()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '删除用户失败')
  }
}
const toggleUserStatus = async (row) => {
  try {
    await api.put(`/users/${row.id}/status`, { enabled: row.enabled })
    ElMessage.success(row.enabled ? '账号已启用' : '账号已禁用')
  } catch (error) {
    row.enabled = !row.enabled
    ElMessage.error(error.response?.data?.message || '账号状态修改失败')
  }
}
const resetUserSearch = async () => {
  userSearchForm.keyword = ''
  userSearchForm.role = ''
  await loadUsers()
}
const resetOperationLogSearch = async () => {
  operationLogSearchForm.keyword = ''
  operationLogSearchForm.module = ''
  operationLogSearchForm.action = ''
  await loadOperationLogs()
}
const submitProfile = async () => {
  await profileFormRef.value.validate()
  saving.value = true
  try {
    await api.put('/profile', {
      realName: profileForm.realName,
      phone: profileForm.phone,
      email: profileForm.email,
    })
    currentUser.value = (await api.get('/auth/me')).data
    fillProfileForm(currentUser.value)
    ElMessage.success('个人资料已保存')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '保存个人资料失败')
  } finally {
    saving.value = false
  }
}
const resetSearch = async () => {
  searchForm.keyword = ''
  searchForm.status = ''
  await loadReimbursements()
}
const resetPendingSearch = async () => {
  pendingSearchForm.keyword = ''
  await loadPendingReimbursements()
}
const changeMenu = async (menu) => {
  activeMenu.value = menu
  if (menu === 'dashboard' || menu === 'report') {
    await loadDashboardStats()
    await nextTick()
    renderCharts()
  }
  if (menu === 'reimbursement') await loadReimbursements()
  if (menu === 'approval') await loadPendingReimbursements()
  if (menu === 'budget') await loadBudgets()
  if (menu === 'users') await Promise.all([loadUsers(), loadDepartments()])
  if (menu === 'operationLogs') await loadOperationLogs()
  if (menu === 'profile') await loadProfile()
}

onMounted(async () => {
  window.addEventListener('resize', resizeCharts)
  await checkBackend()
  await restoreLogin()
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  statusChart?.dispose()
  budgetChart?.dispose()
})
</script>

<template>
  <main v-if="!isLoggedIn" class="login-page">
    <section class="login-panel">
      <div class="login-copy">
        <div class="brand-large">
          <span>财</span>
          <strong>财务报销与预算管理系统</strong>
        </div>
        <h1>项目后台</h1>
        <div class="connection-line">
          <el-tag :type="backendOnline ? 'success' : 'danger'">{{ backendOnline ? '后端已连接' : '后端未连接' }}</el-tag>
          <el-button link type="primary" @click="checkBackend">检测接口</el-button>
        </div>
      </div>
      <div class="login-card">
        <h2>登录</h2>
        <el-form label-position="top" @submit.prevent>
          <el-form-item label="用户名"><el-input v-model="loginForm.username" :prefix-icon="User" /></el-form-item>
          <el-form-item label="密码"><el-input v-model="loginForm.password" show-password /></el-form-item>
          <el-button class="full-width" type="primary" size="large" @click="handleLogin">进入系统</el-button>
        </el-form>
        <div class="account-list">
          <div v-for="account in demoAccounts" :key="account.username" class="account-item" @click="fillAccount(account)">
            <strong>{{ account.username }}</strong>
            <span>{{ account.role }} / {{ account.password }}</span>
            <small>{{ account.note }}</small>
          </div>
        </div>
      </div>
    </section>
  </main>

  <el-container v-else class="app-shell">
    <el-aside class="sidebar" width="236px">
      <div class="brand">
        <div class="brand-mark">财</div>
        <div>
          <strong>财务报销系统</strong>
          <span>Finance Admin</span>
        </div>
      </div>
      <el-menu :default-active="activeMenu" class="menu" @select="changeMenu">
        <el-menu-item v-for="item in menuItems" :key="item.index" :index="item.index">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="topbar">
        <div>
          <h1>{{ pageTitle }}</h1>
          <p>
            当前登录：{{ currentUser.realName }} / {{ roleLabels[currentUser.role] }}
            <span v-if="currentUser.departmentName">/ {{ currentUser.departmentName }}</span>
          </p>
        </div>
        <div class="topbar-actions">
          <el-tag :type="backendOnline ? 'success' : 'danger'">{{ backendOnline ? '后端在线' : '后端离线' }}</el-tag>
          <el-button :icon="Refresh" @click="refreshAll">刷新</el-button>
          <el-button :icon="SwitchButton" @click="handleLogout">退出</el-button>
        </div>
      </el-header>

      <el-main class="content">
        <section v-if="activeMenu === 'dashboard' || activeMenu === 'report'" class="dashboard">
          <div class="metric-grid">
            <div class="metric-card">
              <div class="metric-icon blue"><el-icon><Document /></el-icon></div>
              <span>报销单总数</span>
              <strong>{{ dashboardStats.reimbursementCount }}</strong>
            </div>
            <div class="metric-card">
              <div class="metric-icon orange"><el-icon><Money /></el-icon></div>
              <span>本月报销金额</span>
              <strong>{{ formatMoney(dashboardStats.monthAmount) }}</strong>
            </div>
            <div class="metric-card">
              <div class="metric-icon green"><el-icon><Finished /></el-icon></div>
              <span>已通过</span>
              <strong>{{ approvedCount }}</strong>
            </div>
            <div class="metric-card">
              <div class="metric-icon red"><el-icon><Tickets /></el-icon></div>
              <span>待审批</span>
              <strong>{{ dashboardStats.pendingCount }}</strong>
            </div>
          </div>

          <div class="chart-grid">
            <section class="chart-card">
              <div class="section-header">
                <div>
                  <h2>报销状态统计</h2>
                  <p>按当前角色可见数据汇总</p>
                </div>
              </div>
              <div ref="statusChartRef" class="chart-box"></div>
            </section>
            <section class="chart-card">
              <div class="section-header">
                <div>
                  <h2>各部门预算使用情况</h2>
                  <p>当前预算使用率：{{ budgetUsedPercent }}%</p>
                </div>
              </div>
              <div ref="budgetChartRef" class="chart-box"></div>
            </section>
          </div>
        </section>

        <section v-if="activeMenu === 'reimbursement'" class="page-panel">
          <div class="panel-toolbar">
            <el-form :inline="true" :model="searchForm" class="search-form">
              <el-form-item label="关键词">
                <el-input v-model="searchForm.keyword" clearable placeholder="标题或说明" :prefix-icon="Search" @keyup.enter="loadReimbursements" />
              </el-form-item>
              <el-form-item label="状态">
                <el-select v-model="searchForm.status" clearable placeholder="全部状态" class="status-select">
                  <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :icon="Search" @click="loadReimbursements">查询</el-button>
                <el-button @click="resetSearch">重置</el-button>
              </el-form-item>
            </el-form>
            <div class="toolbar-actions">
              <el-button v-if="canExportReimbursements" :loading="exportLoading" @click="exportReimbursements">导出 Excel</el-button>
              <el-button v-if="canCreateReimbursement" type="primary" :icon="Plus" @click="openCreateDialog">新增报销</el-button>
            </div>
          </div>

          <el-table v-loading="tableLoading" :data="reimbursements" border stripe class="data-table" empty-text="暂无报销数据">
            <el-table-column prop="id" label="ID" width="72" />
            <el-table-column prop="title" label="报销标题" min-width="170" show-overflow-tooltip />
            <el-table-column prop="applicantName" label="申请人" width="105" />
            <el-table-column prop="departmentName" label="部门" width="130" />
            <el-table-column prop="expenseType" label="费用类型" width="110" />
            <el-table-column label="金额" width="120"><template #default="{ row }"><strong>{{ formatMoney(row.amount) }}</strong></template></el-table-column>
            <el-table-column prop="expenseDate" label="发生日期" width="120" />
            <el-table-column label="状态" width="120"><template #default="{ row }"><el-tag :type="getStatusType(row.status)">{{ getStatusLabel(row.status) }}</el-tag></template></el-table-column>
            <el-table-column prop="description" label="说明" min-width="160" show-overflow-tooltip />
            <el-table-column label="操作" width="300" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" :icon="Edit" :disabled="!canEditRow(row)" @click="openEditDialog(row)">编辑</el-button>
                <el-button link type="success" :disabled="!canSubmitRow(row)" @click="submitReimbursement(row)">提交</el-button>
                <el-button link type="info" @click="openDetail(row)">详情</el-button>
                <el-button link type="info" @click="openRecords(row)">记录</el-button>
                <el-button link type="danger" :icon="Delete" :disabled="!canEditRow(row)" @click="deleteReimbursement(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </section>

        <section v-if="activeMenu === 'approval'" class="page-panel">
          <div class="section-header"><div><h2>待审批报销单</h2><p>部门负责人处理“部门审批中”，财务人员处理“财务审批中”。</p></div></div>
          <div class="panel-toolbar compact-toolbar">
            <el-form :inline="true" :model="pendingSearchForm" class="search-form">
              <el-form-item label="关键词"><el-input v-model="pendingSearchForm.keyword" clearable placeholder="标题或说明" :prefix-icon="Search" @keyup.enter="loadPendingReimbursements" /></el-form-item>
              <el-form-item><el-button type="primary" :icon="Search" @click="loadPendingReimbursements">查询</el-button><el-button @click="resetPendingSearch">重置</el-button></el-form-item>
            </el-form>
          </div>
          <el-table v-loading="pendingLoading" :data="pendingReimbursements" border stripe class="data-table" empty-text="暂无待审批数据">
            <el-table-column prop="id" label="ID" width="72" />
            <el-table-column prop="title" label="报销标题" min-width="180" show-overflow-tooltip />
            <el-table-column prop="applicantName" label="申请人" width="105" />
            <el-table-column prop="departmentName" label="部门" width="130" />
            <el-table-column label="金额" width="120"><template #default="{ row }"><strong>{{ formatMoney(row.amount) }}</strong></template></el-table-column>
            <el-table-column label="状态" width="120"><template #default="{ row }"><el-tag :type="getStatusType(row.status)">{{ getStatusLabel(row.status) }}</el-tag></template></el-table-column>
            <el-table-column prop="description" label="说明" min-width="180" show-overflow-tooltip />
            <el-table-column label="审批操作" width="230" fixed="right">
              <template #default="{ row }">
                <el-button link type="success" @click="openApprovalDialog(row, 'APPROVE')">通过</el-button>
                <el-button link type="danger" @click="openApprovalDialog(row, 'REJECT')">驳回</el-button>
                <el-button link type="info" @click="openDetail(row)">详情</el-button>
                <el-button link type="info" @click="openRecords(row)">记录</el-button>
              </template>
            </el-table-column>
          </el-table>
        </section>

        <section v-if="activeMenu === 'budget'" class="page-panel">
          <div class="section-header"><div><h2>部门预算管理</h2><p>财务审批通过后，系统会自动扣减对应部门当年预算。</p></div></div>
          <el-table v-loading="budgetLoading" :data="budgets" border stripe class="data-table budget-table">
            <el-table-column prop="departmentName" label="部门" min-width="150" />
            <el-table-column prop="budgetYear" label="年份" width="100" />
            <el-table-column label="总预算" width="150"><template #default="{ row }">{{ formatMoney(row.totalAmount) }}</template></el-table-column>
            <el-table-column label="已使用" width="150"><template #default="{ row }">{{ formatMoney(row.usedAmount) }}</template></el-table-column>
            <el-table-column label="剩余额度" width="150"><template #default="{ row }"><strong>{{ formatMoney(row.remainingAmount) }}</strong></template></el-table-column>
            <el-table-column label="使用率" width="180">
              <template #default="{ row }">
                <el-progress :percentage="row.totalAmount ? Math.round((Number(row.usedAmount) / Number(row.totalAmount)) * 100) : 0" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120"><template #default="{ row }"><el-button link type="primary" @click="openBudgetDialog(row)">设置</el-button></template></el-table-column>
          </el-table>
        </section>

        <section v-if="activeMenu === 'users'" class="page-panel">
          <div class="section-header">
            <div><h2>用户管理</h2><p>管理员维护系统账号、角色、部门和账号状态。</p></div>
            <el-button type="primary" :icon="Plus" @click="openCreateUserDialog">新增用户</el-button>
          </div>
          <div class="panel-toolbar compact-toolbar">
            <el-form :inline="true" :model="userSearchForm" class="search-form">
              <el-form-item label="关键词">
                <el-input v-model="userSearchForm.keyword" clearable placeholder="用户名、姓名、手机或邮箱" :prefix-icon="Search" @keyup.enter="loadUsers" />
              </el-form-item>
              <el-form-item label="角色">
                <el-select v-model="userSearchForm.role" clearable placeholder="全部角色" class="status-select">
                  <el-option v-for="item in roleOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :icon="Search" @click="loadUsers">查询</el-button>
                <el-button @click="resetUserSearch">重置</el-button>
              </el-form-item>
            </el-form>
          </div>
          <el-table v-loading="userLoading" :data="users" border stripe class="data-table" empty-text="暂无用户数据">
            <el-table-column prop="id" label="ID" width="72" />
            <el-table-column prop="username" label="用户名" min-width="130" show-overflow-tooltip />
            <el-table-column prop="realName" label="姓名" width="110" />
            <el-table-column label="角色" width="130"><template #default="{ row }"><el-tag>{{ roleLabels[row.role] }}</el-tag></template></el-table-column>
            <el-table-column prop="departmentName" label="部门" width="140" />
            <el-table-column prop="phone" label="手机号" width="140" />
            <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
            <el-table-column label="状态" width="110">
              <template #default="{ row }">
                <el-switch v-model="row.enabled" active-text="启用" inactive-text="禁用" @change="toggleUserStatus(row)" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="160" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" :icon="Edit" @click="openEditUserDialog(row)">编辑</el-button>
                <el-button link type="danger" :icon="Delete" @click="deleteUser(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </section>

        <section v-if="activeMenu === 'operationLogs'" class="page-panel">
          <div class="section-header">
            <div><h2>操作日志</h2><p>记录登录、报销、审批、预算、OCR、导出和用户管理等关键操作。</p></div>
          </div>
          <div class="panel-toolbar compact-toolbar">
            <el-form :inline="true" :model="operationLogSearchForm" class="search-form">
              <el-form-item label="模块">
                <el-select v-model="operationLogSearchForm.module" clearable placeholder="全部模块" class="status-select">
                  <el-option v-for="item in operationLogModules" :key="item" :label="item" :value="item" />
                </el-select>
              </el-form-item>
              <el-form-item label="操作">
                <el-select v-model="operationLogSearchForm.action" clearable placeholder="全部操作" class="status-select">
                  <el-option v-for="item in operationLogActions" :key="item" :label="item" :value="item" />
                </el-select>
              </el-form-item>
              <el-form-item label="关键词">
                <el-input
                  v-model="operationLogSearchForm.keyword"
                  clearable
                  placeholder="用户、对象、详情或 IP"
                  :prefix-icon="Search"
                  @keyup.enter="loadOperationLogs"
                />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :icon="Search" @click="loadOperationLogs">查询</el-button>
                <el-button @click="resetOperationLogSearch">重置</el-button>
              </el-form-item>
            </el-form>
          </div>
          <el-table v-loading="operationLogLoading" :data="operationLogs" border stripe class="data-table" empty-text="暂无操作日志">
            <el-table-column label="时间" width="170"><template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template></el-table-column>
            <el-table-column label="用户" width="150">
              <template #default="{ row }">
                <strong>{{ row.realName || row.username || '-' }}</strong>
                <small v-if="row.username" class="muted-line">{{ row.username }}</small>
              </template>
            </el-table-column>
            <el-table-column label="角色" width="120"><template #default="{ row }">{{ roleLabels[row.role] || row.role || '-' }}</template></el-table-column>
            <el-table-column prop="module" label="模块" width="110" />
            <el-table-column prop="action" label="操作" width="120" />
            <el-table-column prop="targetName" label="对象" min-width="150" show-overflow-tooltip />
            <el-table-column prop="detail" label="详情" min-width="220" show-overflow-tooltip />
            <el-table-column prop="ipAddress" label="IP" width="130" />
          </el-table>
        </section>

        <section v-if="activeMenu === 'profile'" class="page-panel profile-panel">
          <div class="section-header">
            <div><h2>个人资料</h2><p>可以维护自己的姓名、手机号和邮箱，角色与部门由管理员管理。</p></div>
          </div>
          <el-descriptions border :column="2" class="profile-summary">
            <el-descriptions-item label="用户名">{{ currentUser.username }}</el-descriptions-item>
            <el-descriptions-item label="角色">{{ roleLabels[currentUser.role] }}</el-descriptions-item>
            <el-descriptions-item label="所属部门">{{ currentUser.departmentName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="用户ID">{{ currentUser.id }}</el-descriptions-item>
          </el-descriptions>
          <el-form ref="profileFormRef" :model="profileForm" :rules="profileRules" label-width="92px" class="profile-form">
            <el-form-item label="真实姓名" prop="realName"><el-input v-model="profileForm.realName" maxlength="80" /></el-form-item>
            <el-form-item label="手机号"><el-input v-model="profileForm.phone" maxlength="40" placeholder="请输入手机号" /></el-form-item>
            <el-form-item label="邮箱"><el-input v-model="profileForm.email" maxlength="100" placeholder="请输入邮箱" /></el-form-item>
            <el-form-item><el-button type="primary" :loading="saving" @click="submitProfile">保存资料</el-button></el-form-item>
          </el-form>
        </section>
      </el-main>
    </el-container>

    <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? '新增报销单' : '编辑报销单'" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-form-item label="报销标题" prop="title"><el-input v-model="form.title" maxlength="120" show-word-limit /></el-form-item>
        <el-form-item label="费用类型" prop="expenseType"><el-select v-model="form.expenseType" class="full-width" placeholder="请选择费用类型"><el-option v-for="item in expenseTypes" :key="item" :label="item" :value="item" /></el-select></el-form-item>
        <el-form-item label="报销金额" prop="amount"><el-input-number v-model="form.amount" :min="0.01" :precision="2" :step="10" class="full-width" /></el-form-item>
        <el-form-item label="发生日期" prop="expenseDate"><el-date-picker v-model="form.expenseDate" type="date" value-format="YYYY-MM-DD" placeholder="请选择日期" class="full-width" /></el-form-item>
        <el-form-item label="报销说明"><el-input v-model="form.description" type="textarea" :rows="4" maxlength="500" show-word-limit /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="submitForm">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="detailDialogVisible" title="报销详情" width="820px">
      <div v-if="detailData" class="detail-grid">
        <el-descriptions border :column="2">
          <el-descriptions-item label="标题">{{ detailData.reimbursement.title }}</el-descriptions-item>
          <el-descriptions-item label="状态"><el-tag :type="getStatusType(detailData.reimbursement.status)">{{ getStatusLabel(detailData.reimbursement.status) }}</el-tag></el-descriptions-item>
          <el-descriptions-item label="申请人">{{ detailData.reimbursement.applicantName }}</el-descriptions-item>
          <el-descriptions-item label="部门">{{ detailData.reimbursement.departmentName }}</el-descriptions-item>
          <el-descriptions-item label="费用类型">{{ detailData.reimbursement.expenseType }}</el-descriptions-item>
          <el-descriptions-item label="金额">{{ formatMoney(detailData.reimbursement.amount) }}</el-descriptions-item>
          <el-descriptions-item label="说明" :span="2">{{ detailData.reimbursement.description || '-' }}</el-descriptions-item>
        </el-descriptions>

        <section class="detail-section timeline-section">
          <div class="section-header">
            <div><h2>审批流程</h2><p>展示报销单从创建、提交、审批到最终结果的全过程。</p></div>
          </div>
          <el-timeline v-loading="timelineLoading" class="approval-timeline">
            <el-timeline-item
              v-for="node in detailTimeline"
              :key="node.nodeType"
              :timestamp="formatDateTime(node.time)"
              :color="getTimelineColor(node.status)"
              placement="top"
            >
              <div class="timeline-card">
                <div class="timeline-title-row">
                  <strong>{{ node.title }}</strong>
                  <el-tag :type="getTimelineStatusType(node.status)" size="small">{{ getTimelineStatusLabel(node.status) }}</el-tag>
                </div>
                <div class="timeline-meta">
                  <span>操作人：{{ node.operatorName || '待处理' }}</span>
                  <span v-if="node.operatorRole">角色：{{ roleLabels[node.operatorRole] || node.operatorRole }}</span>
                </div>
                <p>{{ node.comment || '-' }}</p>
              </div>
            </el-timeline-item>
          </el-timeline>
        </section>

        <section class="detail-section">
          <div class="section-header">
            <div><h2>发票附件</h2><p>上传图片后识别发票信息，提交人确认后审批人可查看。</p></div>
            <el-upload v-if="canUploadRow(detailData.reimbursement)" :show-file-list="false" :http-request="uploadInvoice" accept="image/*,.pdf">
              <el-button type="primary" :icon="UploadFilled">上传发票</el-button>
            </el-upload>
          </div>
          <el-table :data="detailData.attachments" border empty-text="暂无附件">
            <el-table-column prop="fileName" label="文件名" min-width="180" show-overflow-tooltip />
            <el-table-column label="大小" width="110"><template #default="{ row }">{{ Math.round((row.fileSize || 0) / 1024) }} KB</template></el-table-column>
            <el-table-column label="操作" width="180">
              <template #default="{ row }"><el-button link type="primary" :href="fileBaseUrl + row.fileUrl" target="_blank">查看</el-button></template>
            </el-table-column>
          </el-table>
          <div class="ocr-panel">
            <div class="ocr-title-row">
              <div>
                <strong>发票 OCR 信息</strong>
                <span>未配置百度密钥时会自动使用模拟识别结果。</span>
              </div>
              <el-tag :type="ocrStatusType(invoiceOcrForm.ocrStatus)">{{ ocrStatusLabel(invoiceOcrForm.ocrStatus) }}</el-tag>
            </div>
            <div
              v-if="invoiceOcrForm.verificationMessage"
              :class="['ocr-check-card', `is-${getOcrVerificationLevel(invoiceOcrForm.amountMatched, invoiceOcrForm.amount)}`]"
            >
              <div class="ocr-check-title">
                <strong>{{ invoiceOcrForm.verificationMessage }}</strong>
                <el-tag :type="getOcrVerificationType(invoiceOcrForm.amountMatched, invoiceOcrForm.amount)" size="small">
                  {{ invoiceOcrForm.amountMatched ? '金额一致' : '需要核对' }}
                </el-tag>
              </div>
              <div class="ocr-check-grid">
                <span>报销金额：{{ formatMoney(detailData.reimbursement.amount) }}</span>
                <span>发票金额：{{ invoiceOcrForm.amount == null ? '未识别' : formatMoney(invoiceOcrForm.amount) }}</span>
                <span>差额：{{ invoiceOcrForm.amountDifference == null ? '-' : formatMoney(invoiceOcrForm.amountDifference) }}</span>
              </div>
            </div>
            <el-form class="invoice-ocr-form" :model="invoiceOcrForm" label-width="92px" :disabled="!canUploadRow(detailData.reimbursement)">
              <div class="invoice-ocr-grid">
                <el-form-item label="发票代码"><el-input v-model="invoiceOcrForm.invoiceCode" placeholder="自动识别或手动填写" /></el-form-item>
                <el-form-item label="发票号码"><el-input v-model="invoiceOcrForm.invoiceNumber" placeholder="自动识别或手动填写" /></el-form-item>
                <el-form-item label="开票日期"><el-date-picker v-model="invoiceOcrForm.invoiceDate" type="date" value-format="YYYY-MM-DD" class="full-width" /></el-form-item>
                <el-form-item label="金额"><el-input-number v-model="invoiceOcrForm.amount" :min="0" :precision="2" class="full-width" /></el-form-item>
                <el-form-item label="税额"><el-input-number v-model="invoiceOcrForm.taxAmount" :min="0" :precision="2" class="full-width" /></el-form-item>
                <el-form-item label="销售方"><el-input v-model="invoiceOcrForm.sellerName" placeholder="销售方名称" /></el-form-item>
                <el-form-item label="购买方" class="wide"><el-input v-model="invoiceOcrForm.buyerName" placeholder="购买方名称" /></el-form-item>
              </div>
            </el-form>
            <div class="ocr-actions">
              <el-button type="success" :loading="invoiceOcrLoading" :disabled="!canUploadRow(detailData.reimbursement)" @click="runOcr">OCR 识别</el-button>
              <el-button type="primary" :loading="invoiceOcrSaving" :disabled="!canUploadRow(detailData.reimbursement)" @click="saveInvoiceOcr">保存识别结果</el-button>
              <el-button type="warning" :disabled="!canUploadRow(detailData.reimbursement) || invoiceOcrForm.ocrStatus === 'CONFIRMED'" @click="confirmInvoiceOcr">确认无误</el-button>
            </div>
          </div>
        </section>

        <section class="detail-section">
          <div class="section-header"><div><h2>审批记录</h2><p>展示部门与财务审批意见。</p></div></div>
          <el-table :data="detailData.approvalRecords" border empty-text="暂无审批记录">
            <el-table-column prop="approvalNode" label="节点" width="120" />
            <el-table-column prop="approverName" label="审批人" width="130" />
            <el-table-column label="动作" width="100"><template #default="{ row }"><el-tag :type="row.action === 'APPROVE' ? 'success' : 'danger'">{{ row.action === 'APPROVE' ? '通过' : '驳回' }}</el-tag></template></el-table-column>
            <el-table-column prop="comment" label="意见" min-width="180" show-overflow-tooltip />
          </el-table>
        </section>
      </div>
    </el-dialog>

    <el-dialog v-model="approvalDialogVisible" :title="approvalAction === 'APPROVE' ? '审批通过' : '审批驳回'" width="520px">
      <el-form label-width="84px">
        <el-form-item label="报销单"><el-input :model-value="selectedApprovalRow?.title" disabled /></el-form-item>
        <div v-loading="approvalOcrCheckLoading">
          <div
            v-if="approvalOcrCheck && approvalOcrCheck.amountMatched !== true"
            :class="['ocr-check-card', 'approval-check-card', `is-${getOcrVerificationLevel(approvalOcrCheck.amountMatched, approvalOcrCheck.amount)}`]"
          >
            <div class="ocr-check-title">
              <strong>{{ approvalOcrCheck.verificationMessage }}</strong>
              <el-tag :type="getOcrVerificationType(approvalOcrCheck.amountMatched, approvalOcrCheck.amount)" size="small">审批提醒</el-tag>
            </div>
            <div class="ocr-check-grid">
              <span>报销金额：{{ formatMoney(selectedApprovalRow?.amount) }}</span>
              <span>发票金额：{{ approvalOcrCheck.amount == null ? '未识别' : formatMoney(approvalOcrCheck.amount) }}</span>
              <span>差额：{{ approvalOcrCheck.amountDifference == null ? '-' : formatMoney(approvalOcrCheck.amountDifference) }}</span>
            </div>
          </div>
        </div>
        <el-form-item label="审批意见"><el-input v-model="approvalForm.comment" type="textarea" :rows="4" maxlength="500" show-word-limit /></el-form-item>
      </el-form>
      <template #footer><el-button @click="approvalDialogVisible = false">取消</el-button><el-button :type="approvalAction === 'APPROVE' ? 'success' : 'danger'" :loading="saving" @click="submitApproval">确认{{ approvalAction === 'APPROVE' ? '通过' : '驳回' }}</el-button></template>
    </el-dialog>

    <el-dialog v-model="recordDialogVisible" title="审批记录" width="620px">
      <el-table :data="approvalRecords" border empty-text="暂无审批记录">
        <el-table-column prop="approvalNode" label="节点" width="120" />
        <el-table-column prop="approverName" label="审批人" width="120" />
        <el-table-column label="动作" width="100"><template #default="{ row }"><el-tag :type="row.action === 'APPROVE' ? 'success' : 'danger'">{{ row.action === 'APPROVE' ? '通过' : '驳回' }}</el-tag></template></el-table-column>
        <el-table-column prop="comment" label="意见" min-width="180" show-overflow-tooltip />
      </el-table>
    </el-dialog>

    <el-dialog v-model="budgetDialogVisible" title="设置部门预算" width="460px">
      <el-form label-width="86px">
        <el-form-item label="部门"><el-input :model-value="selectedBudget?.departmentName" disabled /></el-form-item>
        <el-form-item label="已使用"><el-input :model-value="formatMoney(selectedBudget?.usedAmount)" disabled /></el-form-item>
        <el-form-item label="总预算"><el-input-number v-model="budgetForm.totalAmount" :min="0" :precision="2" :step="1000" class="full-width" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="budgetDialogVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="submitBudget">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="userDialogVisible" :title="userDialogMode === 'create' ? '新增用户' : '编辑用户'" width="620px" destroy-on-close>
      <el-form ref="userFormRef" :model="userForm" :rules="userRules" label-width="92px">
        <el-form-item label="用户名" prop="username"><el-input v-model="userForm.username" maxlength="60" /></el-form-item>
        <el-form-item label="初始密码" prop="password">
          <el-input v-model="userForm.password" show-password :placeholder="userDialogMode === 'create' ? '请输入初始密码' : '不填则不修改密码'" maxlength="120" />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName"><el-input v-model="userForm.realName" maxlength="80" /></el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="userForm.role" class="full-width" placeholder="请选择角色">
            <el-option v-for="item in roleOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属部门">
          <el-select v-model="userForm.departmentId" clearable class="full-width" placeholder="请选择部门">
            <el-option v-for="item in departments" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="手机号"><el-input v-model="userForm.phone" maxlength="40" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="userForm.email" maxlength="100" /></el-form-item>
        <el-form-item label="账号状态"><el-switch v-model="userForm.enabled" active-text="启用" inactive-text="禁用" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="userDialogVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="submitUserForm">保存</el-button></template>
    </el-dialog>
  </el-container>
</template>
