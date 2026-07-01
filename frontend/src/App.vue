<script setup>
import axios from 'axios'
import * as echarts from 'echarts'
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  DataAnalysis,
  Box,
  Delete,
  Document,
  Edit,
  Finished,
  HomeFilled,
  Money,
  Plus,
  Refresh,
  Search,
  ShoppingCart,
  SwitchButton,
  Tickets,
  UploadFilled,
  User,
  Wallet,
} from '@element-plus/icons-vue'

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || '/api'
const api = axios.create({ baseURL: apiBaseUrl, timeout: 12000 })

const token = ref(localStorage.getItem('finance_token') || '')
const currentUser = ref(null)
const activeMenu = ref('reimbursement')
const backendOnline = ref(false)
const tableLoading = ref(false)
const pendingLoading = ref(false)
const paymentTaskLoading = ref(false)
const budgetLoading = ref(false)
const userLoading = ref(false)
const operationLogLoading = ref(false)
const timelineLoading = ref(false)
const workbenchLoading = ref(false)
const exportLoading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const approvalDialogVisible = ref(false)
const recordDialogVisible = ref(false)
const budgetDialogVisible = ref(false)
const paymentDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const userDialogVisible = ref(false)
const dialogMode = ref('create')
const userDialogMode = ref('create')
const approvalAction = ref('APPROVE')
const selectedApprovalRow = ref(null)
const selectedBudget = ref(null)
const detailData = ref(null)
const attachmentType = ref('INVOICE')
const detailTimeline = ref([])
const approvalOcrCheck = ref(null)
const approvalOcrCheckLoading = ref(false)
const invoiceOcrLoading = ref(false)
const invoiceOcrSaving = ref(false)
const reimbursements = ref([])
const pendingReimbursements = ref([])
const paymentTasks = ref([])
const approvalRecords = ref([])
const budgets = ref([])
const users = ref([])
const operationLogs = ref([])
const departments = ref([])
const purchases = ref([])
const pendingPurchases = ref([])
const purchaseLoading = ref(false)
const purchaseDialogVisible = ref(false)
const purchaseDetailVisible = ref(false)
const purchaseApprovalVisible = ref(false)
const purchaseDialogMode = ref('create')
const selectedPurchase = ref(null)
const purchaseAttachmentType = ref('OTHER')
const assets = ref([])
const eligibleAssetPurchases = ref([])
const assetLoading = ref(false)
const assetDetailVisible = ref(false)
const acceptanceDialogVisible = ref(false)
const claimDialogVisible = ref(false)
const selectedAsset = ref(null)
const laborApplications = ref([])
const laborLoading = ref(false)
const laborExportLoading = ref(false)
const laborDialogVisible = ref(false)
const laborDetailVisible = ref(false)
const laborApprovalVisible = ref(false)
const laborPaymentVisible = ref(false)
const laborDialogMode = ref('create')
const selectedLabor = ref(null)
const laborAttachmentType = ref('SERVICE_MATERIAL')
const advances = ref([])
const advanceLoading = ref(false)
const advanceDialogVisible = ref(false)
const advanceDetailVisible = ref(false)
const advanceApprovalVisible = ref(false)
const advancePaymentVisible = ref(false)
const advanceOffsetVisible = ref(false)
const advanceDialogMode = ref('create')
const selectedAdvance = ref(null)
const advanceAttachmentType = ref('APPLICATION_MATERIAL')
const incomeLoading = ref(false)
const incomeDialogVisible = ref(false)
const incomeDetailVisible = ref(false)
const incomeDialogMode = ref('create')
const incomeExportLoading = ref(false)
const ledgerLoading = ref(false)
const ledgerExportLoading = ref(false)
const selectedIncome = ref(null)
const incomeRecords = ref([])
const ledgerEntries = ref([])
const incomeAttachments = ref([])
const ledgerSummary = ref({ totalIncome: 0, totalExpense: 0, balance: 0 })
const myApplicationItems = ref([])
const myTodoItems = ref([])
const doneItems = ref([])
const dashboardStats = ref({
  monthAmount: 0,
  pendingCount: 0,
  reimbursementCount: 0,
  statusCounts: {},
  budgets: [],
  pendingOffsetCount: 0,
  overdueAdvanceCount: 0,
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
const workbenchSearchForm = reactive({ businessType: '', status: '', keyword: '' })
const approvalForm = reactive({ comment: '' })
const budgetForm = reactive({ totalAmount: 0 })
const paymentForm = reactive({ paymentDate: '', paymentAmount: null, voucherNumber: '', comment: '' })
const profileForm = reactive({ realName: '', phone: '', email: '' })
const form = reactive({
  id: null,
  title: '',
  expenseType: '',
  amount: null,
  expenseDate: '',
  description: '',
  applicantPhone: '',
  budgetNumber: '',
  reimbursementReason: '',
  paymentDate: '',
  payeeName: '',
  bankAccount: '',
  bankName: '',
  paymentTotal: null,
  relatedPurchaseNumber: '',
  highValueExplanation: '',
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
const purchaseSearchForm = reactive({ keyword: '', status: '' })
const purchaseApprovalForm = reactive({ action: 'APPROVE', comment: '' })
const purchaseForm = reactive({
  id: null,
  applicantPhone: '',
  budgetNumber: '',
  purchaseMethod: '询价采购',
  taxExempt: false,
  useLocation: '',
  purchaseReason: '',
  assetAcceptanceNumber: '',
  items: [{ itemName: '', specification: '', manufacturer: '', unitPrice: 0, quantity: 1 }],
})
const assetSearchForm = reactive({ keyword: '', status: '' })
const acceptanceForm = reactive({
  purchaseApplicationId: null,
  receivedAt: '',
  storageLocation: '',
  remark: '',
})
const claimForm = reactive({ useLocation: '', remark: '' })
const laborSearchForm = reactive({ keyword: '', status: '' })
const laborApprovalForm = reactive({ action: 'APPROVE', comment: '' })
const laborPaymentForm = reactive({ paymentDate: '', paymentAmount: null, voucherNumber: '', comment: '' })
const emptyLaborRecipient = () => ({
  name: '', phone: '', idCard: '', organization: '', position: '',
  serviceContent: '', netAmount: 0, bankAccount: '', bankName: '',
})
const laborForm = reactive({
  id: null,
  category: 'RESEARCH_LABOR',
  title: '',
  description: '',
  budgetNumber: '',
  recipients: [emptyLaborRecipient()],
})
const advanceSearchForm = reactive({ keyword: '', type: '', status: '', settlementStatus: '' })
const advanceApprovalForm = reactive({ action: 'APPROVE', comment: '' })
const advancePaymentForm = reactive({ paymentDate: '', paymentAmount: null, voucherNumber: '', comment: '' })
const advanceOffsetForm = reactive({ amount: null, comment: '' })
const advanceForm = reactive({
  id: null,
  type: 'TEMPORARY_LOAN',
  reason: '',
  amount: null,
  paymentMethod: '银行转账',
  payeeName: '',
  bankAccount: '',
  bankName: '',
  expectedRepaymentDate: '',
  partnerName: '',
  expectedSettlementDate: '',
})
const incomeSearchForm = reactive({ keyword: '', startDate: '', endDate: '' })
const ledgerSearchForm = reactive({ startDate: '', endDate: '', departmentId: '', businessType: '' })
const incomeForm = reactive({
  id: null,
  receiptDate: '',
  voucherNumber: '',
  payerName: '',
  incomeCategory: '',
  amount: null,
  fundingSource: '',
  arrivalAccount: '',
  invoiceStatus: '',
  remark: '',
  departmentId: null,
})

const statusOptions = [
  { label: '草稿', value: 'DRAFT', type: 'info' },
  { label: '财务初审中', value: 'SUBMITTED', type: 'warning' },
  { label: '部门负责人审批中', value: 'FINANCE_INITIAL_APPROVED', type: 'warning' },
  { label: '执行院长审批中', value: 'DEPARTMENT_APPROVED', type: 'primary' },
  { label: '待出纳付款', value: 'EXECUTIVE_APPROVED', type: 'warning' },
  { label: '财务复核中', value: 'PAID', type: 'primary' },
  { label: '已完成', value: 'COMPLETED', type: 'success' },
  { label: '已通过（历史）', value: 'APPROVED', type: 'success' },
  { label: '财务已审批（历史）', value: 'FINANCE_APPROVED', type: 'primary' },
  { label: '已驳回', value: 'REJECTED', type: 'danger' },
]
const expenseTypes = ['交通费', '住宿费', '餐饮费', '办公用品', '培训费', '其他']
const attachmentTypeOptions = [
  { value: 'INVOICE', label: '发票' },
  { value: 'CONTRACT', label: '合同' },
  { value: 'MEETING_MINUTES', label: '会议审议材料' },
  { value: 'BANK_RECEIPT', label: '银行回执' },
  { value: 'OTHER', label: '其他附件' },
]
const attachmentTypeLabels = Object.fromEntries(attachmentTypeOptions.map((item) => [item.value, item.label]))
const purchaseStatusOptions = [
  { label: '草稿', value: 'DRAFT', type: 'info' },
  { label: '财务审核中', value: 'SUBMITTED', type: 'warning' },
  { label: '部门负责人审批中', value: 'FINANCE_APPROVED', type: 'warning' },
  { label: '执行院长审批中', value: 'DEPARTMENT_APPROVED', type: 'primary' },
  { label: '审批完成', value: 'COMPLETED', type: 'success' },
  { label: '已驳回', value: 'REJECTED', type: 'danger' },
]
const purchaseAttachmentTypes = [
  { value: 'QUOTATION', label: '报价/询价材料' },
  { value: 'CONTRACT', label: '合同' },
  { value: 'MEETING_MINUTES', label: '院务委员会审议材料' },
  { value: 'OTHER', label: '其他附件' },
]
const purchaseAttachmentTypeLabels = Object.fromEntries(purchaseAttachmentTypes.map(item => [item.value, item.label]))
const assetStatusOptions = [
  { label: '库存中', value: 'IN_STOCK', type: 'success' },
  { label: '使用中', value: 'IN_USE', type: 'primary' },
  { label: '维修中', value: 'UNDER_MAINTENANCE', type: 'warning' },
  { label: '已报废', value: 'SCRAPPED', type: 'info' },
]
const assetStatusMap = Object.fromEntries(assetStatusOptions.map(item => [item.value, item]))
const assetHistoryActionLabels = {
  ACCEPTED_INBOUND: '验收入库',
  CLAIMED: '领用',
  LOCATION_CHANGED: '位置变更',
  STATUS_CHANGED: '状态变更',
}
const laborCategories = [
  { value: 'RESEARCH_LABOR', label: '科研劳务费' },
  { value: 'EXPERT_CONSULTING', label: '专家咨询费' },
  { value: 'TRAINING', label: '培训费' },
  { value: 'DESIGN_TRANSLATION', label: '设计翻译费' },
  { value: 'OTHER', label: '其他酬金' },
]
const laborCategoryLabels = Object.fromEntries(laborCategories.map(item => [item.value, item.label]))
const laborStatusOptions = statusOptions.filter(item => !['APPROVED', 'FINANCE_APPROVED', 'REJECTED'].includes(item.value))
const laborStatusMap = Object.fromEntries(laborStatusOptions.map(item => [item.value, item]))
const laborAttachmentTypes = [
  { value: 'SERVICE_MATERIAL', label: '劳务证明材料' },
  { value: 'CONTRACT', label: '合同' },
  { value: 'BANK_RECEIPT', label: '银行回执' },
  { value: 'MEETING_MINUTES', label: '会议材料' },
  { value: 'OTHER', label: '其他附件' },
]
const laborAttachmentTypeLabels = Object.fromEntries(laborAttachmentTypes.map(item => [item.value, item.label]))
const advanceTypes = [
  { value: 'TEMPORARY_LOAN', label: '暂借款' },
  { value: 'PREPAYMENT', label: '预付款' },
]
const advanceTypeLabels = Object.fromEntries(advanceTypes.map(item => [item.value, item.label]))
const advanceStatusOptions = [
  { label: '草稿', value: 'DRAFT', type: 'info' },
  { label: '部门审批中', value: 'SUBMITTED', type: 'warning' },
  { label: '财务审核中', value: 'DEPARTMENT_APPROVED', type: 'warning' },
  { label: '执行院长审批中', value: 'FINANCE_APPROVED', type: 'primary' },
  { label: '待出纳付款', value: 'EXECUTIVE_APPROVED', type: 'warning' },
  { label: '财务复核中', value: 'PAID', type: 'primary' },
  { label: '审批完成', value: 'COMPLETED', type: 'success' },
]
const advanceStatusMap = Object.fromEntries(advanceStatusOptions.map(item => [item.value, item]))
const settlementStatusOptions = [
  { label: '待还款/冲账', value: 'PENDING_OFFSET', type: 'warning' },
  { label: '部分冲账', value: 'PARTIAL_OFFSET', type: 'primary' },
  { label: '已冲账', value: 'OFFSET_COMPLETED', type: 'success' },
  { label: '已逾期', value: 'OVERDUE', type: 'danger' },
]
const settlementStatusMap = Object.fromEntries(settlementStatusOptions.map(item => [item.value, item]))
const advanceAttachmentTypes = [
  { value: 'APPLICATION_MATERIAL', label: '申请材料' },
  { value: 'CONTRACT', label: '合同' },
  { value: 'BANK_RECEIPT', label: '银行回执' },
  { value: 'OFFSET_VOUCHER', label: '还款/冲账凭证' },
  { value: 'OTHER', label: '其他附件' },
]
const advanceAttachmentTypeLabels = Object.fromEntries(advanceAttachmentTypes.map(item => [item.value, item.label]))
const incomeCategories = ['科研经费收入', '财政拨款', '横向项目收入', '培训收入', '资产处置收入', '其他收入']
const invoiceStatusOptions = ['未开票', '已开票', '无需开票']
const ledgerBusinessTypes = ['收入', '报销', '劳务酬金', '申购', '暂借款/预付款']
const demoAccounts = [
  { username: 'employee', password: '123456', role: '员工', note: '提交自己的报销单' },
  { username: 'manager', password: '123456', role: '部门负责人', note: '审批本部门报销' },
  { username: 'finance', password: '123456', role: '财务人员', note: '财务审批并扣预算' },
  { username: 'office', password: '123456', role: '办公室', note: '办理申购与资产验收' },
  { username: 'executive', password: '123456', role: '执行院长', note: '查看全院财务情况' },
  { username: 'cashier', password: '123456', role: '出纳', note: '查看待付款任务' },
  { username: 'admin', password: '123456', role: '管理员', note: '查看全部数据' },
]
const roleLabels = {
  EMPLOYEE: '员工',
  DEPARTMENT_MANAGER: '部门负责人',
  FINANCE: '财务人员',
  OFFICE: '办公室',
  EXECUTIVE: '执行院长',
  CASHIER: '出纳',
  ADMIN: '管理员',
}
const roleOptions = Object.entries(roleLabels).map(([value, label]) => ({ value, label }))
const operationLogModules = ['认证', '报销管理', '审批管理', '付款管理', '预算管理', '申购管理', '申购审批', '申购附件', '资产管理', '劳务管理', '资金往来', '附件管理', 'OCR识别', '报表导出', '用户管理', '个人资料']
const operationLogActions = [
  '登录成功',
  '新增报销单',
  '编辑报销单',
  '删除报销单',
  '提交报销单',
  '部门审批',
  '财务审批',
  '财务初审',
  '执行院长审批',
  '出纳付款',
  '财务复核',
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
const incomeRules = {
  receiptDate: [{ required: true, message: '请输入收款日期', trigger: 'change' }],
  payerName: [{ required: true, message: '请输入缴款单位/个人', trigger: 'blur' }],
  incomeCategory: [{ required: true, message: '请选择收入分类', trigger: 'change' }],
  amount: [{ required: true, message: '请输入收入金额', trigger: 'blur' }],
}
const formRef = ref()
const userFormRef = ref()
const profileFormRef = ref()
const incomeFormRef = ref()

const statusMap = computed(() => Object.fromEntries(statusOptions.map((item) => [item.value, item])))
const isHighValueForm = computed(() => Number(form.amount || 0) > 50000)
const isLoggedIn = computed(() => Boolean(currentUser.value && token.value))
const dashboardRoles = ['DEPARTMENT_MANAGER', 'FINANCE', 'EXECUTIVE', 'ADMIN']
const reimbursementRoles = ['EMPLOYEE', 'DEPARTMENT_MANAGER', 'FINANCE', 'EXECUTIVE', 'ADMIN']
const paymentTaskRoles = ['CASHIER', 'FINANCE', 'ADMIN']
const canViewDashboard = computed(() => dashboardRoles.includes(currentUser.value?.role))
const canViewReimbursements = computed(() => reimbursementRoles.includes(currentUser.value?.role))
const canViewPaymentTasks = computed(() => paymentTaskRoles.includes(currentUser.value?.role))
const canCreateReimbursement = computed(() => ['EMPLOYEE', 'ADMIN'].includes(currentUser.value?.role))
const canApprove = computed(() => ['DEPARTMENT_MANAGER', 'FINANCE', 'EXECUTIVE', 'ADMIN'].includes(currentUser.value?.role))
const canManageBudget = computed(() => ['FINANCE', 'ADMIN'].includes(currentUser.value?.role))
const canManageUsers = computed(() => currentUser.value?.role === 'ADMIN')
const canViewOperationLogs = computed(() => currentUser.value?.role === 'ADMIN')
const canExportReimbursements = computed(() => ['FINANCE', 'ADMIN'].includes(currentUser.value?.role))
const canViewIncome = computed(() => ['FINANCE', 'EXECUTIVE', 'ADMIN', 'DEPARTMENT_MANAGER'].includes(currentUser.value?.role))
const canManageIncome = computed(() => ['FINANCE', 'ADMIN'].includes(currentUser.value?.role))
const canViewLedger = computed(() => ['FINANCE', 'EXECUTIVE', 'ADMIN', 'DEPARTMENT_MANAGER'].includes(currentUser.value?.role))
const canExportLedger = computed(() => ['FINANCE', 'EXECUTIVE', 'ADMIN'].includes(currentUser.value?.role))
const canViewWorkbench = computed(() => Boolean(currentUser.value))
const purchaseRoles = ['EMPLOYEE', 'DEPARTMENT_MANAGER', 'FINANCE', 'OFFICE', 'EXECUTIVE', 'ADMIN']
const canViewPurchases = computed(() => purchaseRoles.includes(currentUser.value?.role))
const canCreatePurchase = computed(() => ['EMPLOYEE', 'OFFICE', 'ADMIN'].includes(currentUser.value?.role))
const canApprovePurchase = computed(() => ['DEPARTMENT_MANAGER', 'FINANCE', 'EXECUTIVE', 'ADMIN'].includes(currentUser.value?.role))
const canAcceptAssets = computed(() => ['OFFICE', 'ADMIN'].includes(currentUser.value?.role))
const canViewLabor = computed(() => currentUser.value?.role !== 'OFFICE')
const canViewAdvances = computed(() => currentUser.value?.role !== 'OFFICE')
const canCreateLabor = computed(() => !['CASHIER', 'OFFICE'].includes(currentUser.value?.role))
const canExportLabor = computed(() => ['FINANCE', 'ADMIN'].includes(currentUser.value?.role))
const purchaseStatusMap = Object.fromEntries(purchaseStatusOptions.map(item => [item.value, item]))
const purchaseTotal = computed(() => purchaseForm.items.reduce(
  (sum, item) => sum + Number(item.unitPrice || 0) * Number(item.quantity || 0), 0,
))
const laborTotal = computed(() => laborForm.recipients.reduce(
  (sum, item) => sum + Number(item.netAmount || 0), 0,
))
const defaultMenuForRole = (role) => {
  if (role === 'EMPLOYEE') return 'myApplications'
  if (role === 'CASHIER') return 'myTodos'
  if (role === 'OFFICE') return 'myApplications'
  if (dashboardRoles.includes(role)) return 'dashboard'
  return 'myApplications'
}
const menuItems = computed(() => {
  const role = currentUser.value?.role
  const items = []
  if (dashboardRoles.includes(role)) {
    items.push({ index: 'dashboard', label: '首页仪表盘', icon: HomeFilled })
  }
  items.push({ index: 'myApplications', label: '我的申请', icon: Document })
  if (['DEPARTMENT_MANAGER', 'FINANCE', 'EXECUTIVE', 'CASHIER', 'ADMIN'].includes(role)) {
    items.push({ index: 'myTodos', label: '我的待办', icon: Finished })
    items.push({ index: 'doneItems', label: '已办事项', icon: Tickets })
  }
  if (reimbursementRoles.includes(role)) {
    items.push({ index: 'reimbursement', label: '报销申请', icon: Tickets })
  }
  if (purchaseRoles.includes(role)) {
    items.push({ index: 'purchases', label: '申购管理', icon: ShoppingCart })
  }
  items.push({ index: 'assets', label: '资产出入库', icon: Box })
  if (canViewLabor.value) {
    items.push({ index: 'labor', label: '劳务酬金', icon: Money })
  }
  if (canViewAdvances.value) {
    items.push({ index: 'advances', label: '暂借款 / 预付款', icon: Wallet })
  }
  if (['DEPARTMENT_MANAGER', 'FINANCE', 'EXECUTIVE', 'ADMIN'].includes(role)) {
    items.push({ index: 'approval', label: '审批管理', icon: Finished })
  }
  if (paymentTaskRoles.includes(role)) {
    items.push({ index: 'paymentTasks', label: '付款任务', icon: Money })
  }
  if (['FINANCE', 'ADMIN'].includes(role)) {
    items.push({ index: 'incomes', label: '收入登记', icon: Money })
  }
  if (['DEPARTMENT_MANAGER', 'FINANCE', 'EXECUTIVE', 'ADMIN'].includes(role)) {
    items.push({ index: 'ledger', label: '财务总台账', icon: DataAnalysis })
  }
  if (['FINANCE', 'ADMIN'].includes(role)) {
    items.push({ index: 'budget', label: '预算管理', icon: Wallet })
    items.push({ index: 'report', label: '数据统计', icon: DataAnalysis })
  } else if (role === 'EXECUTIVE') {
    items.push({ index: 'report', label: '数据统计', icon: DataAnalysis })
  }
  if (role === 'ADMIN') {
    items.push({ index: 'users', label: '用户管理', icon: User })
    items.push({ index: 'operationLogs', label: '操作日志', icon: Document })
  }
  items.push({ index: 'profile', label: '个人资料', icon: User })
  return items
})
const pageTitle = computed(() => menuItems.value.find((item) => item.index === activeMenu.value)?.label || '个人资料')
const approvedCount = computed(() => reimbursements.value.filter((item) => ['COMPLETED', 'APPROVED'].includes(item.status)).length)
const budgetUsedPercent = computed(() => {
  const list = dashboardStats.value.budgets || []
  const total = list.reduce((sum, item) => sum + Number(item.totalAmount || 0), 0)
  const used = list.reduce((sum, item) => sum + Number(item.usedAmount || 0), 0)
  return total ? Math.round((used / total) * 100) : 0
})

const formatMoney = (value) => `￥${Number(value || 0).toFixed(2)}`
const formatDateTime = (value) => value ? String(value).replace('T', ' ').slice(0, 19) : '-'
const downloadAttachment = async (module, row) => {
  try {
    const response = await api.get(`/attachments/${module}/${row.id}/download`, { responseType: 'blob' })
    const blobUrl = URL.createObjectURL(response.data)
    window.open(blobUrl, '_blank', 'noopener,noreferrer')
    window.setTimeout(() => URL.revokeObjectURL(blobUrl), 60000)
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '附件下载失败或无权访问')
  }
}
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
const currentUserId = computed(() => currentUser.value?.id)
const workflowAmount = item => item.amount ?? item.totalAmount ?? item.paymentAmount ?? item.totalPrice ?? 0
const workflowDate = item => item.createdAt || item.submittedAt || item.receiptDate || item.expenseDate || ''
const workflowStatus = item => item.status || item.settlementStatus || '-'
const workflowStatusLabel = (type, status) => {
  if (type === '报销') return getStatusLabel(status)
  if (type === '申购') return getPurchaseStatusLabel(status)
  if (type === '劳务') return getLaborStatusLabel(status)
  if (type === '借款') return getAdvanceStatusLabel(status)
  if (type === '收入') return '已登记'
  if (type === '资产') return getAssetStatusLabel(status)
  return status || '-'
}
const workflowStatusType = (type, status) => {
  if (type === '报销') return getStatusType(status)
  if (type === '申购') return getPurchaseStatusType(status)
  if (type === '劳务') return getLaborStatusType(status)
  if (type === '借款') return getAdvanceStatusType(status)
  if (type === '收入') return 'success'
  if (type === '资产') return getAssetStatusType(status)
  return 'info'
}
const workbenchTypeLabels = {
  REIMBURSEMENT: '报销',
  PURCHASE: '申购',
  LABOR: '劳务',
  ADVANCE: '借款',
}
const toWorkflowItem = (type, item, source = item) => ({
  key: `${type}-${item.id}`,
  type,
  id: item.id,
  number: item.approvalNumber || item.applicationNumber || item.incomeNumber || item.assetNumber || `#${item.id}`,
  title: item.title || item.purchaseReason || item.reason || item.payerName || item.itemName || item.summary || '-',
  departmentName: item.departmentName || '-',
  amount: workflowAmount(item),
  status: workflowStatus(item),
  date: workflowDate(item),
  source,
})
const rebuildWorkbench = () => {
  if (!currentUser.value) {
    myApplicationItems.value = []
    myTodoItems.value = []
    doneItems.value = []
    return
  }
  const mine = [
    ...reimbursements.value.filter(item => item.applicantId === currentUserId.value).map(item => toWorkflowItem('报销', item)),
    ...purchases.value.filter(item => item.applicantId === currentUserId.value).map(item => toWorkflowItem('申购', item)),
    ...laborApplications.value.filter(item => item.applicantId === currentUserId.value).map(item => toWorkflowItem('劳务', item)),
    ...advances.value.filter(item => item.applicantId === currentUserId.value).map(item => toWorkflowItem('借款', item)),
    ...incomeRecords.value.filter(item => item.createdById === currentUserId.value).map(item => toWorkflowItem('收入', item)),
    ...assets.value.filter(item => item.custodianId === currentUserId.value).map(item => toWorkflowItem('资产', item)),
  ]
  myApplicationItems.value = mine.sort((a, b) => String(b.date).localeCompare(String(a.date)))

  const todos = [
    ...pendingReimbursements.value.map(item => toWorkflowItem('报销', item)),
    ...pendingPurchases.value.map(item => toWorkflowItem('申购', item)),
    ...laborApplications.value.filter(item => canApproveLabor(item) || canPayLabor(item)).map(item => toWorkflowItem('劳务', item)),
    ...advances.value.filter(item => canApproveAdvance(item) || canPayAdvance(item) || canOffsetAdvance(item)).map(item => toWorkflowItem('借款', item)),
  ]
  myTodoItems.value = todos.sort((a, b) => String(b.date).localeCompare(String(a.date)))

  const doneStatuses = ['COMPLETED', 'APPROVED', 'REJECTED', 'PAID']
  doneItems.value = [
    ...reimbursements.value.filter(item => doneStatuses.includes(item.status)).map(item => toWorkflowItem('报销', item)),
    ...purchases.value.filter(item => ['COMPLETED', 'REJECTED'].includes(item.status)).map(item => toWorkflowItem('申购', item)),
    ...laborApplications.value.filter(item => ['COMPLETED', 'PAID'].includes(item.status)).map(item => toWorkflowItem('劳务', item)),
    ...advances.value.filter(item => ['COMPLETED', 'PAID'].includes(item.status)).map(item => toWorkflowItem('借款', item)),
  ].sort((a, b) => String(b.date).localeCompare(String(a.date)))
}
const refreshWorkbench = async () => {
  const scopeMap = {
    myApplications: 'MY_APPLICATIONS',
    myTodos: 'MY_TODOS',
    doneItems: 'DONE',
  }
  const scope = scopeMap[activeMenu.value]
  if (!scope) return
  workbenchLoading.value = true
  try {
    const params = {}
    if (workbenchSearchForm.businessType) params.businessType = workbenchSearchForm.businessType
    if (workbenchSearchForm.status) params.status = workbenchSearchForm.status
    if (workbenchSearchForm.keyword) params.keyword = workbenchSearchForm.keyword
    const data = (await api.get(`/workbench/${scope}`, { params })).data.map(item => ({
      key: `${item.businessType}-${item.businessId}`,
      type: workbenchTypeLabels[item.businessType] || item.businessType,
      businessType: item.businessType,
      id: item.businessId,
      number: item.number || `#${item.businessId}`,
      title: item.title,
      applicantName: item.applicantName,
      departmentName: item.departmentName,
      amount: item.amount,
      status: item.status,
      date: formatDateTime(item.time),
      source: { id: item.businessId },
    }))
    if (scope === 'MY_APPLICATIONS') myApplicationItems.value = data
    if (scope === 'MY_TODOS') myTodoItems.value = data
    if (scope === 'DONE') doneItems.value = data
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '工作台数据加载失败')
  } finally {
    workbenchLoading.value = false
  }
}
const resetWorkbenchSearch = async () => {
  Object.assign(workbenchSearchForm, { businessType: '', status: '', keyword: '' })
  await refreshWorkbench()
}
const openWorkflowItem = (item) => {
  if (item.type === '报销') return openDetail(item.source)
  if (item.type === '申购') return openPurchaseDetail(item.source)
  if (item.type === '劳务') return openLaborDetail(item.source)
  if (item.type === '借款') return openAdvanceDetail(item.source)
  if (item.type === '收入') return openIncomeDetail(item.source)
  if (item.type === '资产') return openAssetDetail(item.source)
  return null
}
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
const canUploadRow = (row) => currentUser.value?.role === 'ADMIN'
  || row.applicantId === currentUser.value?.id
  || (currentUser.value?.role === 'CASHIER' && row.status === 'EXECUTIVE_APPROVED')
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
    activeMenu.value = defaultMenuForRole(response.data.role)
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
    activeMenu.value = defaultMenuForRole(response.data.user.role)
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
  activeMenu.value = 'reimbursement'
  reimbursements.value = []
  pendingReimbursements.value = []
  paymentTasks.value = []
  budgets.value = []
  users.value = []
  operationLogs.value = []
  departments.value = []
  purchases.value = []
  pendingPurchases.value = []
  assets.value = []
  eligibleAssetPurchases.value = []
  advances.value = []
  incomeRecords.value = []
  ledgerEntries.value = []
  ledgerSummary.value = { totalIncome: 0, totalExpense: 0, balance: 0 }
  fillProfileForm(null)
  localStorage.removeItem('finance_token')
}
const refreshAll = async () => {
  const tasks = []
  if (canViewDashboard.value) tasks.push(loadDashboardStats())
  if (canViewReimbursements.value) tasks.push(loadReimbursements())
  if (canApprove.value) tasks.push(loadPendingReimbursements())
  if (canViewPaymentTasks.value) tasks.push(loadPaymentTasks())
  if (canManageBudget.value) tasks.push(loadBudgets())
  if (canManageUsers.value) tasks.push(loadUsers(), loadDepartments())
  if (canViewOperationLogs.value) tasks.push(loadOperationLogs())
  if (canViewPurchases.value) tasks.push(loadPurchases())
  if (canApprovePurchase.value) tasks.push(loadPendingPurchases())
  if (canViewIncome.value) tasks.push(loadIncomes())
  if (canViewLedger.value) tasks.push(loadLedger())
  if (canManageIncome.value || canViewLedger.value) tasks.push(loadDepartments())
  tasks.push(loadAssets())
  if (canAcceptAssets.value) tasks.push(loadEligibleAssetPurchases())
  if (canViewAdvances.value) tasks.push(loadAdvances())
  await Promise.all(tasks)
  if (['myApplications', 'myTodos', 'doneItems'].includes(activeMenu.value)) {
    await refreshWorkbench()
  }
  await nextTick()
  if (canViewDashboard.value) renderCharts()
}
const loadDashboardStats = async () => {
  if (!token.value || !canViewDashboard.value) {
    dashboardStats.value = {
      monthAmount: 0,
      pendingCount: 0,
      reimbursementCount: 0,
      statusCounts: {},
      budgets: [],
      pendingOffsetCount: 0,
      overdueAdvanceCount: 0,
    }
    return
  }
  try {
    dashboardStats.value = (await api.get('/dashboard/stats')).data
  } catch {
    // Dashboard is decorative; leave old values if it fails.
  }
}
const loadReimbursements = async () => {
  if (!token.value || !canViewReimbursements.value) {
    reimbursements.value = []
    return
  }
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
const loadPaymentTasks = async () => {
  if (!token.value || !canViewPaymentTasks.value) {
    paymentTasks.value = []
    return
  }
  paymentTaskLoading.value = true
  try {
    paymentTasks.value = (await api.get('/reimbursements/payment-tasks')).data
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '读取付款任务失败')
  } finally {
    paymentTaskLoading.value = false
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
  if (!token.value || !(canManageUsers.value || canManageIncome.value || canViewLedger.value)) {
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
const loadIncomes = async () => {
  if (!token.value || !canViewIncome.value) {
    incomeRecords.value = []
    return
  }
  incomeLoading.value = true
  try {
    const params = {}
    if (incomeSearchForm.keyword) params.keyword = incomeSearchForm.keyword
    if (incomeSearchForm.startDate) params.startDate = incomeSearchForm.startDate
    if (incomeSearchForm.endDate) params.endDate = incomeSearchForm.endDate
    incomeRecords.value = (await api.get('/incomes', { params })).data
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '读取收入记录失败')
  } finally {
    incomeLoading.value = false
  }
}
const resetIncomeForm = () => {
  Object.assign(incomeForm, {
    id: null,
    receiptDate: new Date().toISOString().slice(0, 10),
    voucherNumber: '',
    payerName: '',
    incomeCategory: '',
    amount: null,
    fundingSource: '',
    arrivalAccount: '',
    invoiceStatus: '',
    remark: '',
    departmentId: currentUser.value?.departmentId || null,
  })
}
const openIncomeCreate = async () => {
  await loadDepartments()
  incomeDialogMode.value = 'create'
  resetIncomeForm()
  incomeDialogVisible.value = true
}
const openIncomeEdit = async row => {
  try {
    await loadDepartments()
    const data = (await api.get(`/incomes/${row.id}`)).data
    incomeDialogMode.value = 'edit'
    Object.keys(incomeForm).forEach(key => { incomeForm[key] = data[key] ?? (key === 'amount' ? null : '') })
    incomeDialogVisible.value = true
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '读取收入详情失败')
  }
}
const saveIncome = async () => {
  const valid = await incomeFormRef.value?.validate?.().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const payload = { ...incomeForm }
    delete payload.id
    if (incomeDialogMode.value === 'create') await api.post('/incomes', payload)
    else await api.put(`/incomes/${incomeForm.id}`, payload)
    ElMessage.success('收入记录已保存')
    incomeDialogVisible.value = false
    await Promise.all([loadIncomes(), canViewLedger.value ? loadLedger() : Promise.resolve()])
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '保存收入记录失败')
  } finally {
    saving.value = false
  }
}
const deleteIncome = async row => {
  try {
    await ElMessageBox.confirm(`确认删除收入记录 ${row.incomeNumber} 吗？`, '删除确认', { type: 'warning' })
    await api.delete(`/incomes/${row.id}`)
    ElMessage.success('收入记录已删除')
    await Promise.all([loadIncomes(), canViewLedger.value ? loadLedger() : Promise.resolve()])
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error.response?.data?.message || '删除收入记录失败')
  }
}
const openIncomeDetail = async row => {
  try {
    selectedIncome.value = (await api.get(`/incomes/${row.id}`)).data
    incomeAttachments.value = selectedIncome.value.attachments || []
    incomeDetailVisible.value = true
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '读取收入详情失败')
  }
}
const uploadIncomeAttachment = async options => {
  const formData = new FormData()
  formData.append('file', options.file)
  try {
    await api.post(`/incomes/${selectedIncome.value.id}/attachments`, formData)
    selectedIncome.value = (await api.get(`/incomes/${selectedIncome.value.id}`)).data
    incomeAttachments.value = selectedIncome.value.attachments || []
    ElMessage.success('附件上传成功')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '附件上传失败')
  }
}
const loadLedger = async () => {
  if (!token.value || !canViewLedger.value) {
    ledgerEntries.value = []
    return
  }
  ledgerLoading.value = true
  try {
    const params = {}
    if (ledgerSearchForm.startDate) params.startDate = ledgerSearchForm.startDate
    if (ledgerSearchForm.endDate) params.endDate = ledgerSearchForm.endDate
    if (ledgerSearchForm.departmentId) params.departmentId = ledgerSearchForm.departmentId
    if (ledgerSearchForm.businessType) params.businessType = ledgerSearchForm.businessType
    const data = (await api.get('/ledger', { params })).data
    ledgerEntries.value = data.entries || []
    ledgerSummary.value = {
      totalIncome: data.totalIncome || 0,
      totalExpense: data.totalExpense || 0,
      balance: data.balance || 0,
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '读取财务总台账失败')
  } finally {
    ledgerLoading.value = false
  }
}
const exportLedgerExcel = async () => {
  ledgerExportLoading.value = true
  try {
    const params = {}
    if (ledgerSearchForm.startDate) params.startDate = ledgerSearchForm.startDate
    if (ledgerSearchForm.endDate) params.endDate = ledgerSearchForm.endDate
    if (ledgerSearchForm.departmentId) params.departmentId = ledgerSearchForm.departmentId
    if (ledgerSearchForm.businessType) params.businessType = ledgerSearchForm.businessType
    const response = await api.get('/ledger/export', { params, responseType: 'blob' })
    const url = URL.createObjectURL(response.data)
    const link = document.createElement('a')
    link.href = url
    link.download = '单位收支总台账.xlsx'
    link.click()
    URL.revokeObjectURL(url)
    ElMessage.success('总台账已导出')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '导出总台账失败')
  } finally {
    ledgerExportLoading.value = false
  }
}
const loadPurchases = async () => {
  if (!token.value || !canViewPurchases.value) {
    purchases.value = []
    return
  }
  purchaseLoading.value = true
  try {
    const params = {}
    if (purchaseSearchForm.keyword) params.keyword = purchaseSearchForm.keyword
    if (purchaseSearchForm.status) params.status = purchaseSearchForm.status
    purchases.value = (await api.get('/purchases', { params })).data
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '读取申购列表失败')
  } finally {
    purchaseLoading.value = false
  }
}
const loadPendingPurchases = async () => {
  if (!token.value || !canApprovePurchase.value) {
    pendingPurchases.value = []
    return
  }
  try {
    pendingPurchases.value = (await api.get('/purchases/pending')).data
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '读取待审批申购单失败')
  }
}
const loadAssets = async () => {
  if (!token.value) return
  assetLoading.value = true
  try {
    const params = {}
    if (assetSearchForm.keyword) params.keyword = assetSearchForm.keyword
    if (assetSearchForm.status) params.status = assetSearchForm.status
    assets.value = (await api.get('/assets', { params })).data
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '读取资产台账失败')
  } finally {
    assetLoading.value = false
  }
}
const loadEligibleAssetPurchases = async () => {
  if (!token.value || !canAcceptAssets.value) {
    eligibleAssetPurchases.value = []
    return
  }
  try {
    eligibleAssetPurchases.value = (await api.get('/assets/eligible-purchases')).data
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '读取待验收申购单失败')
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
  form.applicantPhone = currentUser.value?.phone || ''
  form.budgetNumber = ''
  form.reimbursementReason = ''
  form.paymentDate = ''
  form.payeeName = ''
  form.bankAccount = ''
  form.bankName = ''
  form.paymentTotal = null
  form.relatedPurchaseNumber = ''
  form.highValueExplanation = ''
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
  form.applicantPhone = row.applicantPhone || currentUser.value?.phone || ''
  form.budgetNumber = row.budgetNumber || ''
  form.reimbursementReason = row.reimbursementReason || ''
  form.paymentDate = row.paymentDate || ''
  form.payeeName = row.payeeName || ''
  form.bankAccount = row.bankAccount || ''
  form.bankName = row.bankName || ''
  form.paymentTotal = row.paymentTotal == null ? Number(row.amount) : Number(row.paymentTotal)
  form.relatedPurchaseNumber = row.relatedPurchaseNumber || ''
  form.highValueExplanation = row.highValueExplanation || ''
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
    applicantPhone: form.applicantPhone,
    budgetNumber: form.budgetNumber,
    reimbursementReason: form.reimbursementReason,
    paymentDate: form.paymentDate || null,
    payeeName: form.payeeName,
    bankAccount: form.bankAccount,
    bankName: form.bankName,
    paymentTotal: form.paymentTotal == null ? Number(form.amount) : Number(form.paymentTotal),
    relatedPurchaseNumber: form.relatedPurchaseNumber,
    highValueExplanation: form.highValueExplanation,
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
  if (approvalAction.value === 'REJECT' && !approvalForm.comment.trim()) {
    ElMessage.warning('驳回时必须填写原因')
    return
  }
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
    if (currentUser.value?.role === 'CASHIER') attachmentType.value = 'BANK_RECEIPT'
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
const openPaymentDialog = (row) => {
  selectedApprovalRow.value = row
  paymentForm.paymentDate = new Date().toISOString().slice(0, 10)
  paymentForm.paymentAmount = Number(row.paymentTotal ?? row.amount)
  paymentForm.voucherNumber = ''
  paymentForm.comment = '已完成银行转账'
  paymentDialogVisible.value = true
}
const submitPayment = async () => {
  if (!selectedApprovalRow.value) return
  if (!paymentForm.paymentDate || !paymentForm.paymentAmount || !paymentForm.voucherNumber.trim()) {
    ElMessage.warning('请完整填写付款日期、金额和凭证号')
    return
  }
  saving.value = true
  try {
    await api.post(`/reimbursements/${selectedApprovalRow.value.id}/payment`, {
      paymentDate: paymentForm.paymentDate,
      paymentAmount: Number(paymentForm.paymentAmount),
      voucherNumber: paymentForm.voucherNumber,
      comment: paymentForm.comment,
    })
    ElMessage.success('付款信息已登记，进入财务复核')
    paymentDialogVisible.value = false
    await refreshAll()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '付款登记失败，请先在详情中上传银行回执')
  } finally {
    saving.value = false
  }
}
const uploadAttachment = async ({ file }) => {
  if (!detailData.value?.reimbursement?.id) return
  const formData = new FormData()
  formData.append('file', file)
  formData.append('attachmentType', attachmentType.value)
  try {
    await api.post(`/reimbursements/${detailData.value.reimbursement.id}/attachments`, formData)
    ElMessage.success(`${attachmentTypeLabels[attachmentType.value]}上传成功`)
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
const resetPurchaseForm = () => {
  purchaseForm.id = null
  purchaseForm.applicantPhone = currentUser.value?.phone || ''
  purchaseForm.budgetNumber = ''
  purchaseForm.purchaseMethod = '询价采购'
  purchaseForm.taxExempt = false
  purchaseForm.useLocation = ''
  purchaseForm.purchaseReason = ''
  purchaseForm.assetAcceptanceNumber = ''
  purchaseForm.items = [{ itemName: '', specification: '', manufacturer: '', unitPrice: 0, quantity: 1 }]
}
const addPurchaseItem = () => {
  purchaseForm.items.push({ itemName: '', specification: '', manufacturer: '', unitPrice: 0, quantity: 1 })
}
const removePurchaseItem = (index) => {
  if (purchaseForm.items.length > 1) purchaseForm.items.splice(index, 1)
}
const openPurchaseCreate = () => {
  purchaseDialogMode.value = 'create'
  resetPurchaseForm()
  purchaseDialogVisible.value = true
}
const openPurchaseEdit = async (row) => {
  const data = (await api.get(`/purchases/${row.id}`)).data
  purchaseDialogMode.value = 'edit'
  purchaseForm.id = data.id
  purchaseForm.applicantPhone = data.applicantPhone || ''
  purchaseForm.budgetNumber = data.budgetNumber || ''
  purchaseForm.purchaseMethod = data.purchaseMethod
  purchaseForm.taxExempt = data.taxExempt
  purchaseForm.useLocation = data.useLocation || ''
  purchaseForm.purchaseReason = data.purchaseReason
  purchaseForm.assetAcceptanceNumber = data.assetAcceptanceNumber || ''
  purchaseForm.items = data.items.map(item => ({
    itemName: item.itemName,
    specification: item.specification || '',
    manufacturer: item.manufacturer || '',
    unitPrice: Number(item.unitPrice),
    quantity: item.quantity,
  }))
  purchaseDialogVisible.value = true
}
const savePurchase = async () => {
  if (!purchaseForm.purchaseReason.trim() || !purchaseForm.purchaseMethod.trim()) {
    ElMessage.warning('请填写采购方式和购置理由')
    return
  }
  if (purchaseForm.items.some(item => !item.itemName.trim() || Number(item.unitPrice) <= 0 || Number(item.quantity) < 1)) {
    ElMessage.warning('请完整填写每条采购明细的物品名称、单价和数量')
    return
  }
  saving.value = true
  try {
    const payload = {
      applicantPhone: purchaseForm.applicantPhone,
      budgetNumber: purchaseForm.budgetNumber,
      purchaseMethod: purchaseForm.purchaseMethod,
      taxExempt: purchaseForm.taxExempt,
      useLocation: purchaseForm.useLocation,
      purchaseReason: purchaseForm.purchaseReason,
      assetAcceptanceNumber: purchaseForm.assetAcceptanceNumber,
      items: purchaseForm.items,
    }
    if (purchaseDialogMode.value === 'create') await api.post('/purchases', payload)
    else await api.put(`/purchases/${purchaseForm.id}`, payload)
    ElMessage.success('申购单保存成功')
    purchaseDialogVisible.value = false
    await loadPurchases()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '申购单保存失败')
  } finally {
    saving.value = false
  }
}
const submitPurchase = async (row) => {
  await ElMessageBox.confirm(`确定提交申购单“${row.applicationNumber}”吗？`, '提交确认', { type: 'info' })
  try {
    await api.post(`/purchases/${row.id}/submit`)
    ElMessage.success('申购单已提交财务审核')
    await Promise.all([loadPurchases(), loadPendingPurchases()])
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '提交失败')
  }
}
const deletePurchase = async (row) => {
  await ElMessageBox.confirm(`确定删除“${row.applicationNumber}”吗？`, '删除确认', { type: 'warning' })
  try {
    await api.delete(`/purchases/${row.id}`)
    ElMessage.success('申购单已删除')
    await loadPurchases()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '删除失败')
  }
}
const openPurchaseDetail = async (row) => {
  try {
    selectedPurchase.value = (await api.get(`/purchases/${row.id}`)).data
    purchaseDetailVisible.value = true
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '读取申购详情失败')
  }
}
const uploadPurchaseAttachment = async ({ file }) => {
  const formData = new FormData()
  formData.append('file', file)
  try {
    await api.post(`/purchases/${selectedPurchase.value.id}/attachments`, formData, {
      params: { attachmentType: purchaseAttachmentType.value },
    })
    ElMessage.success('申购附件上传成功')
    selectedPurchase.value = (await api.get(`/purchases/${selectedPurchase.value.id}`)).data
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '附件上传失败')
  }
}
const openPurchaseApproval = (row, action) => {
  selectedPurchase.value = row
  purchaseApprovalForm.action = action
  purchaseApprovalForm.comment = action === 'APPROVE' ? '同意申购申请' : ''
  purchaseApprovalVisible.value = true
}
const approvePurchase = async () => {
  if (purchaseApprovalForm.action === 'REJECT' && !purchaseApprovalForm.comment.trim()) {
    ElMessage.warning('驳回时必须填写原因')
    return
  }
  saving.value = true
  try {
    await api.post(`/purchases/${selectedPurchase.value.id}/approval`, purchaseApprovalForm)
    ElMessage.success(purchaseApprovalForm.action === 'APPROVE' ? '申购审批通过' : '申购单已退回')
    purchaseApprovalVisible.value = false
    await Promise.all([loadPurchases(), loadPendingPurchases()])
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '审批失败')
  } finally {
    saving.value = false
  }
}
const getPurchaseStatusLabel = status => purchaseStatusMap[status]?.label || status
const getPurchaseStatusType = status => purchaseStatusMap[status]?.type || 'info'
const canEditPurchase = row => row.status === 'DRAFT'
  && (currentUser.value?.role === 'ADMIN' || row.applicantId === currentUser.value?.id)
const openAcceptanceDialog = async () => {
  await loadEligibleAssetPurchases()
  acceptanceForm.purchaseApplicationId = null
  acceptanceForm.receivedAt = new Date().toISOString().slice(0, 16)
  acceptanceForm.storageLocation = ''
  acceptanceForm.remark = ''
  acceptanceDialogVisible.value = true
}
const submitAssetAcceptance = async () => {
  if (!acceptanceForm.purchaseApplicationId || !acceptanceForm.receivedAt || !acceptanceForm.storageLocation.trim()) {
    ElMessage.warning('请选择申购单并填写接收时间和存放地点')
    return
  }
  saving.value = true
  try {
    await api.post('/assets/acceptance', acceptanceForm)
    ElMessage.success('验收入库完成，资产台账已自动生成')
    acceptanceDialogVisible.value = false
    await Promise.all([loadAssets(), loadEligibleAssetPurchases(), loadPurchases()])
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '验收入库失败')
  } finally {
    saving.value = false
  }
}
const openAssetDetail = async row => {
  try {
    selectedAsset.value = (await api.get(`/assets/${row.id}`)).data
    assetDetailVisible.value = true
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '读取资产详情失败')
  }
}
const openClaimDialog = row => {
  selectedAsset.value = row
  claimForm.useLocation = row.location || ''
  claimForm.remark = ''
  claimDialogVisible.value = true
}
const submitAssetClaim = async () => {
  if (!claimForm.useLocation.trim()) {
    ElMessage.warning('请填写实际使用地点')
    return
  }
  saving.value = true
  try {
    selectedAsset.value = (await api.post(`/assets/${selectedAsset.value.id}/claim`, claimForm)).data
    ElMessage.success('资产领用成功，领用单号已生成')
    claimDialogVisible.value = false
    await loadAssets()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '资产领用失败')
  } finally {
    saving.value = false
  }
}
const getAssetStatusLabel = status => assetStatusMap[status]?.label || status
const getAssetStatusType = status => assetStatusMap[status]?.type || 'info'
const getLaborStatusLabel = status => laborStatusMap[status]?.label || status
const getLaborStatusType = status => laborStatusMap[status]?.type || 'info'
const canEditLabor = row => row.status === 'DRAFT'
  && (row.applicantId === currentUser.value?.id || currentUser.value?.role === 'ADMIN')
const canApproveLabor = row => currentUser.value?.role === 'ADMIN'
  || (row.status === 'SUBMITTED' && currentUser.value?.role === 'FINANCE')
  || (row.status === 'FINANCE_INITIAL_APPROVED' && currentUser.value?.role === 'DEPARTMENT_MANAGER')
  || (row.status === 'DEPARTMENT_APPROVED' && currentUser.value?.role === 'EXECUTIVE')
  || (row.status === 'PAID' && currentUser.value?.role === 'FINANCE')
const canPayLabor = row => row.status === 'EXECUTIVE_APPROVED'
  && ['CASHIER', 'ADMIN'].includes(currentUser.value?.role)
const loadLaborApplications = async () => {
  laborLoading.value = true
  try {
    const params = {}
    if (laborSearchForm.keyword) params.keyword = laborSearchForm.keyword
    if (laborSearchForm.status) params.status = laborSearchForm.status
    laborApplications.value = (await api.get('/labor-applications', { params })).data
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '读取劳务单失败')
  } finally {
    laborLoading.value = false
  }
}
const resetLaborForm = () => {
  laborForm.id = null
  laborForm.category = 'RESEARCH_LABOR'
  laborForm.title = ''
  laborForm.description = ''
  laborForm.budgetNumber = ''
  laborForm.recipients = [emptyLaborRecipient()]
}
const openLaborCreate = () => {
  laborDialogMode.value = 'create'
  resetLaborForm()
  laborDialogVisible.value = true
}
const openLaborEdit = async row => {
  try {
    const data = (await api.get(`/labor-applications/${row.id}`)).data
    laborDialogMode.value = 'edit'
    laborForm.id = data.id
    laborForm.category = data.category
    laborForm.title = data.title
    laborForm.description = data.description || ''
    laborForm.budgetNumber = data.budgetNumber || ''
    laborForm.recipients = data.recipients.map(item => ({
      name: item.name, phone: item.phone || '', idCard: item.idCard,
      organization: item.organization || '', position: item.position || '',
      serviceContent: item.serviceContent, netAmount: Number(item.netAmount),
      bankAccount: item.bankAccount, bankName: item.bankName,
    }))
    laborDialogVisible.value = true
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '读取劳务单失败')
  }
}
const addLaborRecipient = () => laborForm.recipients.push(emptyLaborRecipient())
const removeLaborRecipient = index => {
  if (laborForm.recipients.length > 1) laborForm.recipients.splice(index, 1)
}
const saveLabor = async () => {
  if (!laborForm.title.trim() || laborForm.recipients.some(item =>
    !item.name.trim() || !item.idCard.trim() || !item.serviceContent.trim()
    || Number(item.netAmount) <= 0 || !item.bankAccount.trim() || !item.bankName.trim())) {
    ElMessage.warning('请完整填写标题及所有领款人必填信息')
    return
  }
  saving.value = true
  try {
    const payload = {
      category: laborForm.category,
      title: laborForm.title,
      description: laborForm.description,
      budgetNumber: laborForm.budgetNumber,
      recipients: laborForm.recipients,
    }
    if (laborDialogMode.value === 'create') await api.post('/labor-applications', payload)
    else await api.put(`/labor-applications/${laborForm.id}`, payload)
    ElMessage.success('劳务单已保存')
    laborDialogVisible.value = false
    await loadLaborApplications()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '保存劳务单失败')
  } finally {
    saving.value = false
  }
}
const openLaborDetail = async row => {
  try {
    selectedLabor.value = (await api.get(`/labor-applications/${row.id}`)).data
    laborDetailVisible.value = true
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '读取劳务详情失败')
  }
}
const submitLabor = async row => {
  try {
    await ElMessageBox.confirm('确认提交该劳务单进入审批流程吗？', '提交确认', { type: 'warning' })
    await api.post(`/labor-applications/${row.id}/submit`)
    ElMessage.success('劳务单已提交')
    await loadLaborApplications()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error.response?.data?.message || '提交失败')
  }
}
const deleteLabor = async row => {
  try {
    await ElMessageBox.confirm(`确认删除 ${row.applicationNumber} 吗？`, '删除确认', { type: 'warning' })
    await api.delete(`/labor-applications/${row.id}`)
    ElMessage.success('劳务单已删除')
    await loadLaborApplications()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error.response?.data?.message || '删除失败')
  }
}
const openLaborApproval = (row, action) => {
  selectedLabor.value = row
  laborApprovalForm.action = action
  laborApprovalForm.comment = action === 'APPROVE' ? '同意' : ''
  laborApprovalVisible.value = true
}
const submitLaborApproval = async () => {
  if (laborApprovalForm.action === 'REJECT' && !laborApprovalForm.comment.trim()) {
    ElMessage.warning('驳回时必须填写原因')
    return
  }
  saving.value = true
  try {
    await api.post(`/labor-applications/${selectedLabor.value.id}/approval`, laborApprovalForm)
    ElMessage.success(laborApprovalForm.action === 'APPROVE' ? '审批通过' : '已退回申请人')
    laborApprovalVisible.value = false
    laborDetailVisible.value = false
    await loadLaborApplications()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '审批失败')
  } finally {
    saving.value = false
  }
}
const uploadLaborAttachment = async options => {
  const formData = new FormData()
  formData.append('file', options.file)
  try {
    await api.post(`/labor-applications/${selectedLabor.value.id}/attachments`, formData, {
      params: { attachmentType: laborAttachmentType.value },
    })
    selectedLabor.value = (await api.get(`/labor-applications/${selectedLabor.value.id}`)).data
    ElMessage.success('附件上传成功')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '附件上传失败')
  }
}
const openLaborPayment = row => {
  selectedLabor.value = row
  laborPaymentForm.paymentDate = new Date().toISOString().slice(0, 10)
  laborPaymentForm.paymentAmount = Number(row.totalAmount)
  laborPaymentForm.voucherNumber = ''
  laborPaymentForm.comment = ''
  laborPaymentVisible.value = true
}
const submitLaborPayment = async () => {
  if (!laborPaymentForm.paymentDate || !laborPaymentForm.voucherNumber.trim()) {
    ElMessage.warning('请填写付款日期和凭证号')
    return
  }
  saving.value = true
  try {
    await api.post(`/labor-applications/${selectedLabor.value.id}/payment`, laborPaymentForm)
    ElMessage.success('付款已登记，等待财务复核')
    laborPaymentVisible.value = false
    await loadLaborApplications()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '付款登记失败')
  } finally {
    saving.value = false
  }
}
const exportLaborExcel = async () => {
  laborExportLoading.value = true
  try {
    const response = await api.get('/labor-applications/export', { responseType: 'blob' })
    const url = URL.createObjectURL(response.data)
    const link = document.createElement('a')
    link.href = url
    link.download = '劳务酬金发放.xlsx'
    link.click()
    URL.revokeObjectURL(url)
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '导出失败')
  } finally {
    laborExportLoading.value = false
  }
}
const getAdvanceStatusLabel = status => advanceStatusMap[status]?.label || status
const getAdvanceStatusType = status => advanceStatusMap[status]?.type || 'info'
const getSettlementLabel = status => status ? (settlementStatusMap[status]?.label || status) : '尚未付款'
const getSettlementType = status => status ? (settlementStatusMap[status]?.type || 'info') : 'info'
const canEditAdvance = row => row.status === 'DRAFT'
  && (row.applicantId === currentUser.value?.id || currentUser.value?.role === 'ADMIN')
const canApproveAdvance = row => currentUser.value?.role === 'ADMIN'
  || (row.status === 'SUBMITTED' && currentUser.value?.role === 'DEPARTMENT_MANAGER')
  || (row.status === 'DEPARTMENT_APPROVED' && currentUser.value?.role === 'FINANCE')
  || (row.status === 'FINANCE_APPROVED' && currentUser.value?.role === 'EXECUTIVE')
  || (row.status === 'PAID' && currentUser.value?.role === 'FINANCE')
const canPayAdvance = row => row.status === 'EXECUTIVE_APPROVED'
  && ['CASHIER', 'ADMIN'].includes(currentUser.value?.role)
const canOffsetAdvance = row => ['PAID', 'COMPLETED'].includes(row.status)
  && row.settlementStatus !== 'OFFSET_COMPLETED'
  && ['FINANCE', 'ADMIN'].includes(currentUser.value?.role)
const loadAdvances = async () => {
  if (!token.value) return
  advanceLoading.value = true
  try {
    const params = {}
    Object.entries(advanceSearchForm).forEach(([key, value]) => { if (value) params[key] = value })
    advances.value = (await api.get('/advances', { params })).data
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '读取资金申请失败')
  } finally {
    advanceLoading.value = false
  }
}
const resetAdvanceForm = () => {
  advanceForm.id = null
  advanceForm.type = 'TEMPORARY_LOAN'
  advanceForm.reason = ''
  advanceForm.amount = null
  advanceForm.paymentMethod = '银行转账'
  advanceForm.payeeName = currentUser.value?.realName || ''
  advanceForm.bankAccount = ''
  advanceForm.bankName = ''
  advanceForm.expectedRepaymentDate = ''
  advanceForm.partnerName = ''
  advanceForm.expectedSettlementDate = ''
}
const openAdvanceCreate = () => {
  advanceDialogMode.value = 'create'
  resetAdvanceForm()
  advanceDialogVisible.value = true
}
const openAdvanceEdit = async row => {
  try {
    const data = (await api.get(`/advances/${row.id}`)).data
    advanceDialogMode.value = 'edit'
    Object.keys(advanceForm).forEach(key => {
      advanceForm[key] = data[key] ?? (['amount'].includes(key) ? null : '')
    })
    advanceDialogVisible.value = true
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '读取申请失败')
  }
}
const saveAdvance = async () => {
  if (!advanceForm.reason.trim() || !advanceForm.amount || !advanceForm.payeeName.trim()
    || !advanceForm.bankAccount.trim() || !advanceForm.bankName.trim()
    || (advanceForm.type === 'TEMPORARY_LOAN' && !advanceForm.expectedRepaymentDate)
    || (advanceForm.type === 'PREPAYMENT' && (!advanceForm.partnerName.trim() || !advanceForm.expectedSettlementDate))) {
    ElMessage.warning('请完整填写必填信息')
    return
  }
  saving.value = true
  try {
    const payload = { ...advanceForm }
    delete payload.id
    if (advanceForm.type === 'TEMPORARY_LOAN') {
      payload.partnerName = null
      payload.expectedSettlementDate = null
    } else {
      payload.expectedRepaymentDate = null
    }
    if (advanceDialogMode.value === 'create') await api.post('/advances', payload)
    else await api.put(`/advances/${advanceForm.id}`, payload)
    ElMessage.success('资金申请已保存')
    advanceDialogVisible.value = false
    await loadAdvances()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}
const openAdvanceDetail = async row => {
  try {
    selectedAdvance.value = (await api.get(`/advances/${row.id}`)).data
    if (canPayAdvance(selectedAdvance.value)) advanceAttachmentType.value = 'BANK_RECEIPT'
    else if (canOffsetAdvance(selectedAdvance.value)) advanceAttachmentType.value = 'OFFSET_VOUCHER'
    else advanceAttachmentType.value = 'APPLICATION_MATERIAL'
    advanceDetailVisible.value = true
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '读取详情失败')
  }
}
const submitAdvance = async row => {
  try {
    await ElMessageBox.confirm('确认提交申请进入审批流程吗？', '提交确认', { type: 'warning' })
    await api.post(`/advances/${row.id}/submit`)
    ElMessage.success('申请已提交')
    await loadAdvances()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error.response?.data?.message || '提交失败')
  }
}
const deleteAdvance = async row => {
  try {
    await ElMessageBox.confirm(`确认删除 ${row.applicationNumber} 吗？`, '删除确认', { type: 'warning' })
    await api.delete(`/advances/${row.id}`)
    ElMessage.success('申请已删除')
    await loadAdvances()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error.response?.data?.message || '删除失败')
  }
}
const openAdvanceApproval = (row, action) => {
  selectedAdvance.value = row
  advanceApprovalForm.action = action
  advanceApprovalForm.comment = action === 'APPROVE' ? '同意' : ''
  advanceApprovalVisible.value = true
}
const submitAdvanceApproval = async () => {
  if (advanceApprovalForm.action === 'REJECT' && !advanceApprovalForm.comment.trim()) {
    ElMessage.warning('驳回时必须填写原因')
    return
  }
  saving.value = true
  try {
    await api.post(`/advances/${selectedAdvance.value.id}/approval`, advanceApprovalForm)
    ElMessage.success(advanceApprovalForm.action === 'APPROVE' ? '审批通过' : '已退回申请人')
    advanceApprovalVisible.value = false
    advanceDetailVisible.value = false
    await Promise.all([loadAdvances(), canViewDashboard.value ? loadDashboardStats() : Promise.resolve()])
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '审批失败')
  } finally {
    saving.value = false
  }
}
const uploadAdvanceAttachment = async options => {
  const formData = new FormData()
  formData.append('file', options.file)
  try {
    await api.post(`/advances/${selectedAdvance.value.id}/attachments`, formData, {
      params: { attachmentType: advanceAttachmentType.value },
    })
    selectedAdvance.value = (await api.get(`/advances/${selectedAdvance.value.id}`)).data
    ElMessage.success('附件上传成功')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '附件上传失败')
  }
}
const openAdvancePayment = row => {
  selectedAdvance.value = row
  advancePaymentForm.paymentDate = new Date().toISOString().slice(0, 10)
  advancePaymentForm.paymentAmount = Number(row.amount)
  advancePaymentForm.voucherNumber = ''
  advancePaymentForm.comment = ''
  advancePaymentVisible.value = true
}
const submitAdvancePayment = async () => {
  if (!advancePaymentForm.paymentDate || !advancePaymentForm.voucherNumber.trim()) {
    ElMessage.warning('请填写付款日期和凭证号')
    return
  }
  saving.value = true
  try {
    await api.post(`/advances/${selectedAdvance.value.id}/payment`, advancePaymentForm)
    ElMessage.success('付款已登记')
    advancePaymentVisible.value = false
    await loadAdvances()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '付款失败')
  } finally {
    saving.value = false
  }
}
const openAdvanceOffset = row => {
  selectedAdvance.value = row
  advanceOffsetForm.amount = Number(row.remainingAmount)
  advanceOffsetForm.comment = ''
  advanceOffsetVisible.value = true
}
const submitAdvanceOffset = async () => {
  if (!advanceOffsetForm.amount || Number(advanceOffsetForm.amount) <= 0) {
    ElMessage.warning('请输入本次冲账金额')
    return
  }
  saving.value = true
  try {
    await api.post(`/advances/${selectedAdvance.value.id}/offset`, advanceOffsetForm)
    ElMessage.success('冲账记录已保存')
    advanceOffsetVisible.value = false
    await Promise.all([loadAdvances(), loadDashboardStats()])
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '冲账失败')
  } finally {
    saving.value = false
  }
}
const changeMenu = async (menu) => {
  if (!menuItems.value.some((item) => item.index === menu)) {
    activeMenu.value = defaultMenuForRole(currentUser.value?.role)
    return
  }
  activeMenu.value = menu
  if (menu === 'dashboard' || menu === 'report') {
    await loadDashboardStats()
    await nextTick()
    renderCharts()
  }
  if (menu === 'myApplications' || menu === 'myTodos' || menu === 'doneItems') await refreshWorkbench()
  if (menu === 'reimbursement') await loadReimbursements()
  if (menu === 'approval') await loadPendingReimbursements()
  if (menu === 'paymentTasks') await loadPaymentTasks()
  if (menu === 'budget') await loadBudgets()
  if (menu === 'users') await Promise.all([loadUsers(), loadDepartments()])
  if (menu === 'operationLogs') await loadOperationLogs()
  if (menu === 'purchases') await Promise.all([loadPurchases(), loadPendingPurchases()])
  if (menu === 'assets') await Promise.all([loadAssets(), loadEligibleAssetPurchases()])
  if (menu === 'labor') await loadLaborApplications()
  if (menu === 'advances') await loadAdvances()
  if (menu === 'incomes') await Promise.all([loadIncomes(), loadDepartments()])
  if (menu === 'ledger') await Promise.all([loadLedger(), loadDepartments()])
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
        <section v-if="canViewDashboard && (activeMenu === 'dashboard' || activeMenu === 'report')" class="dashboard">
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
            <div class="metric-card">
              <div class="metric-icon orange"><el-icon><Wallet /></el-icon></div>
              <span>待还款 / 冲账</span>
              <strong>{{ dashboardStats.pendingOffsetCount || 0 }}</strong>
            </div>
            <div class="metric-card">
              <div class="metric-icon red"><el-icon><Refresh /></el-icon></div>
              <span>逾期资金单</span>
              <strong>{{ dashboardStats.overdueAdvanceCount || 0 }}</strong>
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

        <section v-if="['myApplications', 'myTodos', 'doneItems'].includes(activeMenu)" class="page-panel">
          <div class="section-header">
            <div>
              <h2>{{ activeMenu === 'myApplications' ? '我的申请' : activeMenu === 'myTodos' ? '我的待办' : '已办事项' }}</h2>
              <p>统一查看报销、申购、劳务和借款流程。</p>
            </div>
            <el-button :icon="Refresh" @click="refreshWorkbench">刷新</el-button>
          </div>
          <el-form :inline="true" :model="workbenchSearchForm" class="search-form">
            <el-form-item label="业务类型">
              <el-select v-model="workbenchSearchForm.businessType" clearable placeholder="全部类型" class="status-select">
                <el-option label="报销" value="REIMBURSEMENT" />
                <el-option label="申购" value="PURCHASE" />
                <el-option label="劳务" value="LABOR" />
                <el-option label="借款/预付款" value="ADVANCE" />
              </el-select>
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="workbenchSearchForm.status" clearable filterable allow-create placeholder="全部状态" class="status-select">
                <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="关键词">
              <el-input v-model="workbenchSearchForm.keyword" clearable placeholder="编号、标题、申请人或部门" :prefix-icon="Search" @keyup.enter="refreshWorkbench" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :icon="Search" @click="refreshWorkbench">查询</el-button>
              <el-button @click="resetWorkbenchSearch">重置</el-button>
            </el-form-item>
          </el-form>
          <el-table
            v-loading="workbenchLoading"
            :data="activeMenu === 'myApplications' ? myApplicationItems : activeMenu === 'myTodos' ? myTodoItems : doneItems"
            border stripe class="data-table"
            empty-text="暂无数据"
          >
            <el-table-column prop="type" label="类型" width="95" />
            <el-table-column prop="number" label="单号" width="160" />
            <el-table-column prop="title" label="标题/摘要" min-width="210" show-overflow-tooltip />
            <el-table-column prop="applicantName" label="申请人" width="110" />
            <el-table-column prop="departmentName" label="部门" width="120" />
            <el-table-column label="金额" width="120"><template #default="{ row }">{{ formatMoney(row.amount) }}</template></el-table-column>
            <el-table-column label="状态" width="120">
              <template #default="{ row }">
                <el-tag :type="workflowStatusType(row.type, row.status)">{{ workflowStatusLabel(row.type, row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="date" label="时间" width="120" />
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openWorkflowItem(row)">查看</el-button>
              </template>
            </el-table-column>
          </el-table>
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
            <el-table-column prop="approvalNumber" label="审批编号" width="150">
              <template #default="{ row }">{{ row.approvalNumber || `旧单 #${row.id}` }}</template>
            </el-table-column>
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

        <section v-if="activeMenu === 'purchases'" class="page-panel">
          <div class="section-header">
            <div>
              <h2>申购管理</h2>
              <p>采购申请依次经过财务审核、部门负责人审批和执行院长审批，可供资产验收及报销关联。</p>
            </div>
            <el-button v-if="canCreatePurchase" type="primary" :icon="Plus" @click="openPurchaseCreate">新增申购</el-button>
          </div>
          <div class="panel-toolbar compact-toolbar">
            <el-form :inline="true" :model="purchaseSearchForm" class="search-form">
              <el-form-item label="关键词">
                <el-input v-model="purchaseSearchForm.keyword" clearable placeholder="编号、申请人或购置理由" :prefix-icon="Search" @keyup.enter="loadPurchases" />
              </el-form-item>
              <el-form-item label="状态">
                <el-select v-model="purchaseSearchForm.status" clearable placeholder="全部状态" class="status-select">
                  <el-option v-for="item in purchaseStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :icon="Search" @click="loadPurchases">查询</el-button>
                <el-button @click="purchaseSearchForm.keyword = ''; purchaseSearchForm.status = ''; loadPurchases()">重置</el-button>
              </el-form-item>
            </el-form>
          </div>
          <el-table v-loading="purchaseLoading" :data="purchases" border stripe class="data-table" empty-text="暂无申购数据">
            <el-table-column prop="applicationNumber" label="申购编号" width="155" />
            <el-table-column prop="applicantName" label="申请人" width="105" />
            <el-table-column prop="departmentName" label="部门" width="130" />
            <el-table-column prop="purchaseMethod" label="采购方式" width="120" />
            <el-table-column label="金额" width="125"><template #default="{ row }"><strong>{{ formatMoney(row.amount) }}</strong></template></el-table-column>
            <el-table-column label="免税" width="75"><template #default="{ row }">{{ row.taxExempt ? '是' : '否' }}</template></el-table-column>
            <el-table-column prop="purchaseReason" label="购置理由" min-width="180" show-overflow-tooltip />
            <el-table-column label="状态" width="140"><template #default="{ row }"><el-tag :type="getPurchaseStatusType(row.status)">{{ getPurchaseStatusLabel(row.status) }}</el-tag></template></el-table-column>
            <el-table-column label="操作" width="250" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" :disabled="!canEditPurchase(row)" @click="openPurchaseEdit(row)">编辑</el-button>
                <el-button link type="success" :disabled="!canEditPurchase(row)" @click="submitPurchase(row)">提交</el-button>
                <el-button link type="info" @click="openPurchaseDetail(row)">详情/附件</el-button>
                <el-button link type="danger" :disabled="!canEditPurchase(row)" @click="deletePurchase(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div v-if="canApprovePurchase" class="purchase-pending-section">
            <div class="section-header">
              <div><h2>待我审批</h2><p>仅显示当前角色和当前审批节点可以处理的申购单。</p></div>
              <el-button :icon="Refresh" @click="loadPendingPurchases">刷新</el-button>
            </div>
            <el-table :data="pendingPurchases" border stripe empty-text="当前没有待审批申购单">
              <el-table-column prop="applicationNumber" label="申购编号" width="155" />
              <el-table-column prop="applicantName" label="申请人" width="110" />
              <el-table-column prop="departmentName" label="部门" width="135" />
              <el-table-column label="金额" width="125"><template #default="{ row }">{{ formatMoney(row.amount) }}</template></el-table-column>
              <el-table-column prop="purchaseReason" label="购置理由" min-width="200" show-overflow-tooltip />
              <el-table-column label="操作" width="210">
                <template #default="{ row }">
                  <el-button link type="success" @click="openPurchaseApproval(row, 'APPROVE')">通过</el-button>
                  <el-button link type="danger" @click="openPurchaseApproval(row, 'REJECT')">驳回</el-button>
                  <el-button link type="info" @click="openPurchaseDetail(row)">详情</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </section>

        <section v-if="activeMenu === 'assets'" class="page-panel">
          <div class="section-header">
            <div>
              <h2>资产出入库与验收</h2>
              <p>从已审批申购单验收入库，自动建立资产台账，并记录领用人、位置、状态和完整流转历史。</p>
            </div>
            <el-button v-if="canAcceptAssets" type="primary" :icon="Plus" @click="openAcceptanceDialog">办理验收入库</el-button>
          </div>
          <div class="panel-toolbar compact-toolbar">
            <el-form :inline="true" :model="assetSearchForm" class="search-form">
              <el-form-item label="关键词">
                <el-input v-model="assetSearchForm.keyword" clearable placeholder="资产编号、物品、保管人或位置" :prefix-icon="Search" @keyup.enter="loadAssets" />
              </el-form-item>
              <el-form-item label="资产状态">
                <el-select v-model="assetSearchForm.status" clearable placeholder="全部状态" class="status-select">
                  <el-option v-for="item in assetStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :icon="Search" @click="loadAssets">查询</el-button>
                <el-button @click="assetSearchForm.keyword = ''; assetSearchForm.status = ''; loadAssets()">重置</el-button>
              </el-form-item>
            </el-form>
          </div>
          <el-table v-loading="assetLoading" :data="assets" border stripe class="data-table" empty-text="暂无资产台账">
            <el-table-column prop="assetNumber" label="资产编号" width="155" />
            <el-table-column prop="itemName" label="物品名称" min-width="150" show-overflow-tooltip />
            <el-table-column prop="specification" label="规格" width="130" show-overflow-tooltip />
            <el-table-column prop="manufacturer" label="厂商" width="135" show-overflow-tooltip />
            <el-table-column prop="quantity" label="数量" width="70" />
            <el-table-column label="总价" width="120"><template #default="{ row }">{{ formatMoney(row.totalPrice) }}</template></el-table-column>
            <el-table-column prop="custodianName" label="当前保管人" width="120"><template #default="{ row }">{{ row.custodianName || '办公室库存' }}</template></el-table-column>
            <el-table-column prop="location" label="当前位置" min-width="140" show-overflow-tooltip />
            <el-table-column label="状态" width="100"><template #default="{ row }"><el-tag :type="getAssetStatusType(row.status)">{{ getAssetStatusLabel(row.status) }}</el-tag></template></el-table-column>
            <el-table-column label="操作" width="155" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openAssetDetail(row)">详情</el-button>
                <el-button link type="success" :disabled="row.status !== 'IN_STOCK'" @click="openClaimDialog(row)">领用</el-button>
              </template>
            </el-table-column>
          </el-table>
        </section>

        <section v-if="activeMenu === 'labor'" class="page-panel">
          <div class="section-header">
            <div>
              <h2>劳务 / 酬金发放</h2>
              <p>管理科研劳务、专家咨询、培训、设计翻译等费用，支持多人发放、分级审批和出纳付款。</p>
            </div>
            <div class="header-actions">
              <el-button v-if="canExportLabor" :loading="laborExportLoading" :icon="Document" @click="exportLaborExcel">导出 Excel</el-button>
              <el-button v-if="canCreateLabor" type="primary" :icon="Plus" @click="openLaborCreate">新增劳务单</el-button>
            </div>
          </div>
          <div class="panel-toolbar compact-toolbar">
            <el-form :inline="true" :model="laborSearchForm" class="search-form">
              <el-form-item label="关键词">
                <el-input v-model="laborSearchForm.keyword" clearable placeholder="编号、标题或申请人" :prefix-icon="Search" @keyup.enter="loadLaborApplications" />
              </el-form-item>
              <el-form-item label="状态">
                <el-select v-model="laborSearchForm.status" clearable placeholder="全部状态" class="status-select">
                  <el-option v-for="item in laborStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :icon="Search" @click="loadLaborApplications">查询</el-button>
                <el-button @click="laborSearchForm.keyword = ''; laborSearchForm.status = ''; loadLaborApplications()">重置</el-button>
              </el-form-item>
            </el-form>
          </div>
          <el-table v-loading="laborLoading" :data="laborApplications" border stripe class="data-table" empty-text="暂无劳务数据">
            <el-table-column prop="applicationNumber" label="劳务编号" width="158" />
            <el-table-column label="类别" width="125"><template #default="{ row }">{{ laborCategoryLabels[row.category] || row.category }}</template></el-table-column>
            <el-table-column prop="title" label="发放事项" min-width="170" show-overflow-tooltip />
            <el-table-column prop="applicantName" label="申请人" width="105" />
            <el-table-column prop="departmentName" label="部门" width="125" />
            <el-table-column label="领款人数" width="90"><template #default="{ row }">{{ row.recipients.length }}</template></el-table-column>
            <el-table-column label="总额" width="125"><template #default="{ row }"><strong>{{ formatMoney(row.totalAmount) }}</strong></template></el-table-column>
            <el-table-column label="状态" width="135"><template #default="{ row }"><el-tag :type="getLaborStatusType(row.status)">{{ getLaborStatusLabel(row.status) }}</el-tag></template></el-table-column>
            <el-table-column label="操作" width="260" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openLaborDetail(row)">详情</el-button>
                <el-button v-if="canEditLabor(row)" link type="primary" @click="openLaborEdit(row)">编辑</el-button>
                <el-button v-if="canEditLabor(row)" link type="success" @click="submitLabor(row)">提交</el-button>
                <el-button v-if="canApproveLabor(row)" link type="success" @click="openLaborApproval(row, 'APPROVE')">审批</el-button>
                <el-button v-if="canPayLabor(row)" link type="warning" @click="openLaborPayment(row)">付款</el-button>
                <el-button v-if="canEditLabor(row)" link type="danger" @click="deleteLabor(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </section>

        <section v-if="activeMenu === 'advances'" class="page-panel">
          <div class="section-header">
            <div><h2>暂借款 / 预付款</h2><p>跟踪申请、审批、付款、还款与冲账，逾期项目会自动标红提醒。</p></div>
            <el-button v-if="currentUser.role !== 'CASHIER'" type="primary" :icon="Plus" @click="openAdvanceCreate">新增资金申请</el-button>
          </div>
          <div class="panel-toolbar compact-toolbar">
            <el-form :inline="true" :model="advanceSearchForm" class="search-form">
              <el-form-item label="关键词"><el-input v-model="advanceSearchForm.keyword" clearable placeholder="编号、理由或收款人" :prefix-icon="Search" @keyup.enter="loadAdvances" /></el-form-item>
              <el-form-item label="类型"><el-select v-model="advanceSearchForm.type" clearable placeholder="全部类型" class="status-select"><el-option v-for="item in advanceTypes" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
              <el-form-item label="冲账状态"><el-select v-model="advanceSearchForm.settlementStatus" clearable placeholder="全部状态" class="status-select"><el-option v-for="item in settlementStatusOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
              <el-form-item><el-button type="primary" :icon="Search" @click="loadAdvances">查询</el-button><el-button @click="Object.assign(advanceSearchForm, { keyword: '', type: '', status: '', settlementStatus: '' }); loadAdvances()">重置</el-button></el-form-item>
            </el-form>
          </div>
          <el-table v-loading="advanceLoading" :data="advances" border stripe class="data-table" empty-text="暂无资金申请">
            <el-table-column prop="applicationNumber" label="申请编号" width="158" />
            <el-table-column label="类型" width="95"><template #default="{ row }">{{ advanceTypeLabels[row.type] }}</template></el-table-column>
            <el-table-column prop="reason" label="申请理由" min-width="180" show-overflow-tooltip />
            <el-table-column prop="applicantName" label="申请人" width="100" />
            <el-table-column prop="payeeName" label="收款人" width="120" />
            <el-table-column label="金额" width="120"><template #default="{ row }"><strong>{{ formatMoney(row.amount) }}</strong></template></el-table-column>
            <el-table-column label="审批状态" width="130"><template #default="{ row }"><el-tag :type="getAdvanceStatusType(row.status)">{{ getAdvanceStatusLabel(row.status) }}</el-tag></template></el-table-column>
            <el-table-column label="冲账状态" width="125"><template #default="{ row }"><el-tag :type="getSettlementType(row.settlementStatus)">{{ getSettlementLabel(row.settlementStatus) }}</el-tag></template></el-table-column>
            <el-table-column label="剩余待冲" width="120"><template #default="{ row }">{{ formatMoney(row.remainingAmount) }}</template></el-table-column>
            <el-table-column label="操作" width="270" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openAdvanceDetail(row)">详情</el-button>
                <el-button v-if="canEditAdvance(row)" link type="primary" @click="openAdvanceEdit(row)">编辑</el-button>
                <el-button v-if="canEditAdvance(row)" link type="success" @click="submitAdvance(row)">提交</el-button>
                <el-button v-if="canApproveAdvance(row)" link type="success" @click="openAdvanceApproval(row, 'APPROVE')">审批</el-button>
                <el-button v-if="canPayAdvance(row)" link type="warning" @click="openAdvancePayment(row)">付款</el-button>
                <el-button v-if="canOffsetAdvance(row)" link type="warning" @click="openAdvanceOffset(row)">冲账</el-button>
                <el-button v-if="canEditAdvance(row)" link type="danger" @click="deleteAdvance(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </section>

        <section v-if="activeMenu === 'approval'" class="page-panel">
          <div class="section-header"><div><h2>待审批报销单</h2><p>系统按财务初审、部门负责人、执行院长、财务复核依次分派任务。</p></div></div>
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

        <section v-if="activeMenu === 'paymentTasks'" class="page-panel">
          <div class="section-header">
            <div>
              <h2>待付款报销单</h2>
              <p>执行院长审批通过后，由出纳上传银行回执并登记付款信息。</p>
            </div>
            <el-button :icon="Refresh" @click="loadPaymentTasks">刷新</el-button>
          </div>
          <el-table v-loading="paymentTaskLoading" :data="paymentTasks" border stripe class="data-table" empty-text="暂无待付款任务">
            <el-table-column prop="id" label="ID" width="72" />
            <el-table-column prop="title" label="报销标题" min-width="190" show-overflow-tooltip />
            <el-table-column prop="applicantName" label="申请人" width="110" />
            <el-table-column prop="departmentName" label="部门" width="140" />
            <el-table-column prop="expenseType" label="费用类型" width="110" />
            <el-table-column label="付款金额" width="130">
              <template #default="{ row }"><strong>{{ formatMoney(row.amount) }}</strong></template>
            </el-table-column>
            <el-table-column prop="expenseDate" label="发生日期" width="120" />
            <el-table-column label="状态" width="110">
              <template #default="{ row }"><el-tag type="success">{{ getStatusLabel(row.status) }}</el-tag></template>
            </el-table-column>
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openDetail(row)">查看详情</el-button>
                <el-button link type="success" @click="openPaymentDialog(row)">登记付款</el-button>
              </template>
            </el-table-column>
          </el-table>
        </section>

        <section v-if="activeMenu === 'incomes'" class="page-panel">
          <div class="section-header">
            <div><h2>收入登记</h2><p>登记收款日期、凭证号、缴款单位、收入分类、到账账户和发票附件，自动形成收入台账。</p></div>
            <el-button v-if="canManageIncome" type="primary" :icon="Plus" @click="openIncomeCreate">新增收入</el-button>
          </div>
          <div class="panel-toolbar compact-toolbar">
            <el-form :inline="true" :model="incomeSearchForm" class="search-form">
              <el-form-item label="关键词"><el-input v-model="incomeSearchForm.keyword" clearable placeholder="编号、缴款方、分类或凭证号" :prefix-icon="Search" @keyup.enter="loadIncomes" /></el-form-item>
              <el-form-item label="开始日期"><el-date-picker v-model="incomeSearchForm.startDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
              <el-form-item label="结束日期"><el-date-picker v-model="incomeSearchForm.endDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
              <el-form-item>
                <el-button type="primary" :icon="Search" @click="loadIncomes">查询</el-button>
                <el-button @click="Object.assign(incomeSearchForm, { keyword: '', startDate: '', endDate: '' }); loadIncomes()">重置</el-button>
              </el-form-item>
            </el-form>
          </div>
          <el-table v-loading="incomeLoading" :data="incomeRecords" border stripe class="data-table" empty-text="暂无收入记录">
            <el-table-column prop="incomeNumber" label="收入编号" width="155" />
            <el-table-column prop="receiptDate" label="收款日期" width="120" />
            <el-table-column prop="voucherNumber" label="凭证号" width="125" />
            <el-table-column prop="payerName" label="缴款单位/个人" min-width="170" show-overflow-tooltip />
            <el-table-column prop="incomeCategory" label="收入分类" width="140" />
            <el-table-column prop="departmentName" label="部门" width="125" />
            <el-table-column label="金额" width="125"><template #default="{ row }"><strong>{{ formatMoney(row.amount) }}</strong></template></el-table-column>
            <el-table-column prop="fundingSource" label="资金来源" min-width="140" show-overflow-tooltip />
            <el-table-column prop="arrivalAccount" label="到账账户" min-width="150" show-overflow-tooltip />
            <el-table-column prop="invoiceStatus" label="开票情况" width="110" />
            <el-table-column label="操作" width="210" fixed="right">
              <template #default="{ row }">
                <el-button link type="info" @click="openIncomeDetail(row)">详情</el-button>
                <el-button v-if="canManageIncome" link type="primary" @click="openIncomeEdit(row)">编辑</el-button>
                <el-button v-if="canManageIncome" link type="danger" @click="deleteIncome(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </section>

        <section v-if="activeMenu === 'ledger'" class="page-panel">
          <div class="section-header">
            <div><h2>财务总台账</h2><p>汇总收入、报销、劳务、申购、暂借款/预付款，形成单位收支总台账。</p></div>
            <el-button v-if="canExportLedger" :loading="ledgerExportLoading" :icon="Document" @click="exportLedgerExcel">导出 Excel</el-button>
          </div>
          <div class="metric-grid compact-metrics">
            <div class="metric-card"><span>总收入</span><strong>{{ formatMoney(ledgerSummary.totalIncome) }}</strong></div>
            <div class="metric-card"><span>总支出</span><strong>{{ formatMoney(ledgerSummary.totalExpense) }}</strong></div>
            <div class="metric-card"><span>收支结余</span><strong>{{ formatMoney(ledgerSummary.balance) }}</strong></div>
          </div>
          <div class="panel-toolbar compact-toolbar">
            <el-form :inline="true" :model="ledgerSearchForm" class="search-form">
              <el-form-item label="开始日期"><el-date-picker v-model="ledgerSearchForm.startDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
              <el-form-item label="结束日期"><el-date-picker v-model="ledgerSearchForm.endDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
              <el-form-item v-if="currentUser.role !== 'DEPARTMENT_MANAGER'" label="部门">
                <el-select v-model="ledgerSearchForm.departmentId" clearable placeholder="全部部门" class="status-select">
                  <el-option v-for="dept in departments" :key="dept.id" :label="dept.name" :value="dept.id" />
                </el-select>
              </el-form-item>
              <el-form-item label="业务类型">
                <el-select v-model="ledgerSearchForm.businessType" clearable placeholder="全部类型" class="status-select">
                  <el-option v-for="item in ledgerBusinessTypes" :key="item" :label="item" :value="item" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :icon="Search" @click="loadLedger">查询</el-button>
                <el-button @click="Object.assign(ledgerSearchForm, { startDate: '', endDate: '', departmentId: '', businessType: '' }); loadLedger()">重置</el-button>
              </el-form-item>
            </el-form>
          </div>
          <el-table v-loading="ledgerLoading" :data="ledgerEntries" border stripe class="data-table" empty-text="暂无台账数据">
            <el-table-column prop="businessDate" label="日期" width="115" />
            <el-table-column label="方向" width="80"><template #default="{ row }"><el-tag :type="row.direction === '收入' ? 'success' : 'warning'">{{ row.direction }}</el-tag></template></el-table-column>
            <el-table-column prop="businessType" label="业务类型" width="125" />
            <el-table-column prop="businessNumber" label="业务编号" width="155" />
            <el-table-column prop="departmentName" label="部门" width="125" />
            <el-table-column prop="operatorName" label="经办/申请人" width="120" />
            <el-table-column prop="summary" label="摘要" min-width="210" show-overflow-tooltip />
            <el-table-column label="收入" width="120"><template #default="{ row }">{{ row.incomeAmount ? formatMoney(row.incomeAmount) : '-' }}</template></el-table-column>
            <el-table-column label="支出" width="120"><template #default="{ row }">{{ row.expenseAmount ? formatMoney(row.expenseAmount) : '-' }}</template></el-table-column>
            <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
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

    <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? '新增报销单' : '编辑报销单'" width="820px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="108px">
        <div class="reimbursement-form-grid">
          <el-form-item label="报销标题" prop="title" class="wide"><el-input v-model="form.title" maxlength="120" show-word-limit /></el-form-item>
          <el-form-item label="费用类型" prop="expenseType"><el-select v-model="form.expenseType" class="full-width" placeholder="请选择"><el-option v-for="item in expenseTypes" :key="item" :label="item" :value="item" /></el-select></el-form-item>
          <el-form-item label="发生日期" prop="expenseDate"><el-date-picker v-model="form.expenseDate" type="date" value-format="YYYY-MM-DD" class="full-width" /></el-form-item>
          <el-form-item label="报销金额" prop="amount"><el-input-number v-model="form.amount" :min="0.01" :precision="2" :step="10" class="full-width" /></el-form-item>
          <el-form-item label="付款总金额"><el-input-number v-model="form.paymentTotal" :min="0" :precision="2" :step="10" class="full-width" /></el-form-item>
          <el-form-item label="申请人电话"><el-input v-model="form.applicantPhone" maxlength="40" /></el-form-item>
          <el-form-item label="预算编号"><el-input v-model="form.budgetNumber" maxlength="60" /></el-form-item>
          <el-form-item label="关联申购单">
            <el-select v-model="form.relatedPurchaseNumber" clearable filterable allow-create class="full-width" placeholder="选择已完成申购单或手动输入">
              <el-option
                v-for="purchase in purchases.filter(item => item.status === 'COMPLETED')"
                :key="purchase.id"
                :label="`${purchase.applicationNumber} · ${purchase.purchaseReason}`"
                :value="purchase.applicationNumber"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="付款日期"><el-date-picker v-model="form.paymentDate" type="date" value-format="YYYY-MM-DD" class="full-width" /></el-form-item>
          <el-form-item label="收款人全称"><el-input v-model="form.payeeName" maxlength="120" /></el-form-item>
          <el-form-item label="开户行"><el-input v-model="form.bankName" maxlength="160" /></el-form-item>
          <el-form-item label="银行账号" class="wide"><el-input v-model="form.bankAccount" maxlength="80" /></el-form-item>
          <el-form-item label="报销事由" class="wide"><el-input v-model="form.reimbursementReason" type="textarea" :rows="2" maxlength="500" show-word-limit /></el-form-item>
          <el-form-item label="补充说明" class="wide"><el-input v-model="form.description" type="textarea" :rows="2" maxlength="500" show-word-limit /></el-form-item>
          <el-form-item v-if="isHighValueForm" label="大额报销说明" class="wide" required>
            <el-input v-model="form.highValueExplanation" type="textarea" :rows="3" maxlength="1000" show-word-limit placeholder="金额超过 5 万元时必填，保存后还需上传会议审议材料方可提交" />
          </el-form-item>
        </div>
        <el-alert v-if="isHighValueForm" type="warning" :closable="false" show-icon title="该报销金额超过 5 万元，提交前必须填写大额报销说明并上传会议审议材料。" />
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="submitForm">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="purchaseDialogVisible" :title="purchaseDialogMode === 'create' ? '新增申购单' : '编辑申购单'" width="1040px" destroy-on-close>
      <el-form label-width="96px">
        <div class="purchase-form-grid">
          <el-form-item label="申请人电话"><el-input v-model="purchaseForm.applicantPhone" maxlength="40" /></el-form-item>
          <el-form-item label="预算编号"><el-input v-model="purchaseForm.budgetNumber" maxlength="80" /></el-form-item>
          <el-form-item label="采购方式" required>
            <el-select v-model="purchaseForm.purchaseMethod" class="full-width">
              <el-option v-for="method in ['询价采购', '竞争性谈判', '公开招标', '单一来源', '网上商城', '其他']" :key="method" :label="method" :value="method" />
            </el-select>
          </el-form-item>
          <el-form-item label="是否免税"><el-switch v-model="purchaseForm.taxExempt" active-text="免税" inactive-text="含税" /></el-form-item>
          <el-form-item label="使用地点"><el-input v-model="purchaseForm.useLocation" maxlength="200" /></el-form-item>
          <el-form-item label="验收单号"><el-input v-model="purchaseForm.assetAcceptanceNumber" maxlength="80" placeholder="后续验收后填写" /></el-form-item>
          <el-form-item label="购置理由" class="wide" required><el-input v-model="purchaseForm.purchaseReason" type="textarea" :rows="3" maxlength="1000" show-word-limit /></el-form-item>
        </div>
        <div class="purchase-items-title">
          <div><h3>采购明细</h3><p>系统按单价 × 数量自动汇总总价。</p></div>
          <el-button type="primary" plain :icon="Plus" @click="addPurchaseItem">添加明细</el-button>
        </div>
        <div v-for="(item, index) in purchaseForm.items" :key="index" class="purchase-item-row">
          <el-form-item :label="`物品 ${index + 1}`" required><el-input v-model="item.itemName" placeholder="物品名称" /></el-form-item>
          <el-form-item label="规格"><el-input v-model="item.specification" placeholder="型号/规格" /></el-form-item>
          <el-form-item label="厂商"><el-input v-model="item.manufacturer" placeholder="生产厂商" /></el-form-item>
          <el-form-item label="单价" required><el-input-number v-model="item.unitPrice" :min="0" :precision="2" class="full-width" /></el-form-item>
          <el-form-item label="数量" required><el-input-number v-model="item.quantity" :min="1" :precision="0" class="full-width" /></el-form-item>
          <div class="purchase-item-total">
            <span>{{ formatMoney(Number(item.unitPrice || 0) * Number(item.quantity || 0)) }}</span>
            <el-button circle text type="danger" :icon="Delete" :disabled="purchaseForm.items.length === 1" @click="removePurchaseItem(index)" />
          </div>
        </div>
        <div class="purchase-total-bar">
          <span>申购总金额</span><strong>{{ formatMoney(purchaseTotal) }}</strong>
        </div>
        <el-alert v-if="purchaseTotal > 50000" type="warning" :closable="false" show-icon title="金额超过 5 万元，保存后必须在详情中上传“院务委员会审议材料”才能提交。" />
      </el-form>
      <template #footer><el-button @click="purchaseDialogVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="savePurchase">保存申购单</el-button></template>
    </el-dialog>

    <el-dialog v-model="purchaseDetailVisible" title="申购详情" width="920px">
      <div v-if="selectedPurchase" class="detail-grid">
        <el-descriptions border :column="2">
          <el-descriptions-item label="申购编号">{{ selectedPurchase.applicationNumber }}</el-descriptions-item>
          <el-descriptions-item label="状态"><el-tag :type="getPurchaseStatusType(selectedPurchase.status)">{{ getPurchaseStatusLabel(selectedPurchase.status) }}</el-tag></el-descriptions-item>
          <el-descriptions-item label="申请人">{{ selectedPurchase.applicantName }}</el-descriptions-item>
          <el-descriptions-item label="部门">{{ selectedPurchase.departmentName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="电话">{{ selectedPurchase.applicantPhone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="预算编号">{{ selectedPurchase.budgetNumber || '-' }}</el-descriptions-item>
          <el-descriptions-item label="采购方式">{{ selectedPurchase.purchaseMethod }}</el-descriptions-item>
          <el-descriptions-item label="是否免税">{{ selectedPurchase.taxExempt ? '是' : '否' }}</el-descriptions-item>
          <el-descriptions-item label="使用地点">{{ selectedPurchase.useLocation || '-' }}</el-descriptions-item>
          <el-descriptions-item label="资产验收单">{{ selectedPurchase.assetAcceptanceNumber || '尚未关联' }}</el-descriptions-item>
          <el-descriptions-item label="购置理由" :span="2">{{ selectedPurchase.purchaseReason }}</el-descriptions-item>
        </el-descriptions>
        <section class="detail-section">
          <div class="section-header"><div><h2>采购明细</h2><p>共 {{ selectedPurchase.items.length }} 项，合计 {{ formatMoney(selectedPurchase.amount) }}</p></div></div>
          <el-table :data="selectedPurchase.items" border>
            <el-table-column prop="itemName" label="物品名称" min-width="150" />
            <el-table-column prop="specification" label="规格" width="130" />
            <el-table-column prop="manufacturer" label="厂商" width="140" />
            <el-table-column label="单价" width="110"><template #default="{ row }">{{ formatMoney(row.unitPrice) }}</template></el-table-column>
            <el-table-column prop="quantity" label="数量" width="80" />
            <el-table-column label="总价" width="120"><template #default="{ row }"><strong>{{ formatMoney(row.totalPrice) }}</strong></template></el-table-column>
          </el-table>
        </section>
        <section class="detail-section">
          <div class="section-header">
            <div><h2>申购附件</h2><p>超过 5 万元必须上传院务委员会审议材料。</p></div>
            <div v-if="canEditPurchase(selectedPurchase)" class="attachment-actions">
              <el-select v-model="purchaseAttachmentType" class="attachment-type-select">
                <el-option v-for="type in purchaseAttachmentTypes" :key="type.value" :label="type.label" :value="type.value" />
              </el-select>
              <el-upload :show-file-list="false" :http-request="uploadPurchaseAttachment" accept="image/*,.pdf,.doc,.docx,.xls,.xlsx">
                <el-button type="primary" :icon="UploadFilled">上传附件</el-button>
              </el-upload>
            </div>
          </div>
          <el-table :data="selectedPurchase.attachments" border empty-text="暂无附件">
            <el-table-column label="类型" width="170"><template #default="{ row }">{{ purchaseAttachmentTypeLabels[row.attachmentType] || row.attachmentType }}</template></el-table-column>
            <el-table-column prop="fileName" label="文件名" min-width="220" />
            <el-table-column label="大小" width="110"><template #default="{ row }">{{ Math.round((row.fileSize || 0) / 1024) }} KB</template></el-table-column>
            <el-table-column label="操作" width="90"><template #default="{ row }"><el-button link type="primary" @click="downloadAttachment('PURCHASE', row)">查看</el-button></template></el-table-column>
          </el-table>
        </section>
        <section class="detail-section">
          <div class="section-header"><div><h2>审批记录</h2><p>财务审核 → 部门负责人 → 执行院长。</p></div></div>
          <el-table :data="selectedPurchase.approvalRecords" border empty-text="暂无审批记录">
            <el-table-column prop="approvalNode" label="节点" width="130" />
            <el-table-column prop="approverName" label="审批人" width="120" />
            <el-table-column label="结果" width="90"><template #default="{ row }"><el-tag :type="row.action === 'APPROVE' ? 'success' : 'danger'">{{ row.action === 'APPROVE' ? '通过' : '驳回' }}</el-tag></template></el-table-column>
            <el-table-column prop="comment" label="审批意见" min-width="200" />
            <el-table-column label="时间" width="170"><template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template></el-table-column>
          </el-table>
        </section>
      </div>
    </el-dialog>

    <el-dialog v-model="purchaseApprovalVisible" :title="purchaseApprovalForm.action === 'APPROVE' ? '通过申购审批' : '驳回申购申请'" width="520px">
      <el-form label-width="90px">
        <el-form-item label="申购编号"><el-input :model-value="selectedPurchase?.applicationNumber" disabled /></el-form-item>
        <el-form-item label="申购金额"><el-input :model-value="formatMoney(selectedPurchase?.amount)" disabled /></el-form-item>
        <el-form-item label="审批意见" :required="purchaseApprovalForm.action === 'REJECT'">
          <el-input v-model="purchaseApprovalForm.comment" type="textarea" :rows="4" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="purchaseApprovalVisible = false">取消</el-button><el-button :type="purchaseApprovalForm.action === 'APPROVE' ? 'success' : 'danger'" :loading="saving" @click="approvePurchase">确认</el-button></template>
    </el-dialog>

    <el-dialog v-model="advanceDialogVisible" :title="advanceDialogMode === 'create' ? '新增资金申请' : '编辑资金申请'" width="760px" destroy-on-close>
      <el-form label-width="112px">
        <div class="advance-form-grid">
          <el-form-item label="申请类型" required><el-segmented v-model="advanceForm.type" :options="advanceTypes" /></el-form-item>
          <el-form-item label="申请金额" required><el-input-number v-model="advanceForm.amount" :min="0.01" :precision="2" class="full-width" /></el-form-item>
          <el-form-item label="支付方式" required><el-select v-model="advanceForm.paymentMethod" class="full-width"><el-option label="银行转账" value="银行转账" /><el-option label="现金" value="现金" /><el-option label="其他" value="其他" /></el-select></el-form-item>
          <el-form-item label="收款人" required><el-input v-model="advanceForm.payeeName" maxlength="160" /></el-form-item>
          <el-form-item label="收款账号" required><el-input v-model="advanceForm.bankAccount" maxlength="80" /></el-form-item>
          <el-form-item label="开户行" required><el-input v-model="advanceForm.bankName" maxlength="160" /></el-form-item>
          <el-form-item v-if="advanceForm.type === 'TEMPORARY_LOAN'" label="预计还款日期" required><el-date-picker v-model="advanceForm.expectedRepaymentDate" type="date" value-format="YYYY-MM-DD" class="full-width" /></el-form-item>
          <template v-else>
            <el-form-item label="合作方" required><el-input v-model="advanceForm.partnerName" maxlength="200" /></el-form-item>
            <el-form-item label="预计结算日期" required><el-date-picker v-model="advanceForm.expectedSettlementDate" type="date" value-format="YYYY-MM-DD" class="full-width" /></el-form-item>
          </template>
          <el-form-item label="申请理由" class="wide" required><el-input v-model="advanceForm.reason" type="textarea" :rows="4" maxlength="1000" show-word-limit /></el-form-item>
        </div>
      </el-form>
      <template #footer><el-button @click="advanceDialogVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="saveAdvance">保存申请</el-button></template>
    </el-dialog>

    <el-dialog v-model="advanceDetailVisible" title="暂借款 / 预付款详情" width="900px">
      <div v-if="selectedAdvance" class="detail-grid">
        <el-descriptions border :column="2">
          <el-descriptions-item label="申请编号">{{ selectedAdvance.applicationNumber }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ advanceTypeLabels[selectedAdvance.type] }}</el-descriptions-item>
          <el-descriptions-item label="申请人">{{ selectedAdvance.applicantName }} / {{ selectedAdvance.departmentName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="金额">{{ formatMoney(selectedAdvance.amount) }}</el-descriptions-item>
          <el-descriptions-item label="审批状态"><el-tag :type="getAdvanceStatusType(selectedAdvance.status)">{{ getAdvanceStatusLabel(selectedAdvance.status) }}</el-tag></el-descriptions-item>
          <el-descriptions-item label="冲账状态"><el-tag :type="getSettlementType(selectedAdvance.settlementStatus)">{{ getSettlementLabel(selectedAdvance.settlementStatus) }}</el-tag></el-descriptions-item>
          <el-descriptions-item label="收款人">{{ selectedAdvance.payeeName }}</el-descriptions-item>
          <el-descriptions-item label="支付方式">{{ selectedAdvance.paymentMethod }}</el-descriptions-item>
          <el-descriptions-item label="账号">{{ selectedAdvance.bankAccount }}</el-descriptions-item>
          <el-descriptions-item label="开户行">{{ selectedAdvance.bankName }}</el-descriptions-item>
          <el-descriptions-item v-if="selectedAdvance.type === 'TEMPORARY_LOAN'" label="预计还款/冲账">{{ selectedAdvance.expectedRepaymentDate }}</el-descriptions-item>
          <template v-else>
            <el-descriptions-item label="合作方">{{ selectedAdvance.partnerName }}</el-descriptions-item>
            <el-descriptions-item label="预计结算">{{ selectedAdvance.expectedSettlementDate }}</el-descriptions-item>
          </template>
          <el-descriptions-item label="已冲账">{{ formatMoney(selectedAdvance.offsetAmount) }}</el-descriptions-item>
          <el-descriptions-item label="剩余待冲">{{ formatMoney(selectedAdvance.remainingAmount) }}</el-descriptions-item>
          <el-descriptions-item label="申请理由" :span="2">{{ selectedAdvance.reason }}</el-descriptions-item>
        </el-descriptions>
        <section class="detail-section">
          <div class="section-header">
            <div><h2>附件资料</h2><p>出纳上传银行回执，财务可上传还款或冲账凭证。</p></div>
            <div v-if="canEditAdvance(selectedAdvance) || canPayAdvance(selectedAdvance) || canOffsetAdvance(selectedAdvance)" class="attachment-actions">
              <el-select v-model="advanceAttachmentType" class="attachment-type-select">
                <el-option
                  v-for="item in (canPayAdvance(selectedAdvance)
                    ? advanceAttachmentTypes.filter(option => option.value === 'BANK_RECEIPT')
                    : canOffsetAdvance(selectedAdvance)
                      ? advanceAttachmentTypes.filter(option => option.value === 'OFFSET_VOUCHER')
                      : advanceAttachmentTypes.filter(option => !['BANK_RECEIPT', 'OFFSET_VOUCHER'].includes(option.value)))"
                  :key="item.value" :label="item.label" :value="item.value"
                />
              </el-select>
              <el-upload :show-file-list="false" :http-request="uploadAdvanceAttachment"><el-button type="primary" :icon="UploadFilled">上传附件</el-button></el-upload>
            </div>
          </div>
          <el-table :data="selectedAdvance.attachments" border empty-text="暂无附件">
            <el-table-column label="类型" width="150"><template #default="{ row }">{{ advanceAttachmentTypeLabels[row.attachmentType] }}</template></el-table-column>
            <el-table-column prop="fileName" label="文件名" min-width="220" />
            <el-table-column label="操作" width="90"><template #default="{ row }"><el-button link type="primary" @click="downloadAttachment('ADVANCE', row)">查看</el-button></template></el-table-column>
          </el-table>
        </section>
        <section class="detail-section">
          <div class="section-header"><div><h2>还款 / 冲账记录</h2><p>每笔冲账独立留痕，系统自动计算剩余金额。</p></div></div>
          <el-table :data="selectedAdvance.offsetRecords" border empty-text="暂无冲账记录">
            <el-table-column label="金额" width="130"><template #default="{ row }">{{ formatMoney(row.amount) }}</template></el-table-column>
            <el-table-column prop="operatorName" label="登记人" width="120" />
            <el-table-column prop="comment" label="说明" min-width="180" />
            <el-table-column label="时间" width="170"><template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template></el-table-column>
          </el-table>
        </section>
        <section class="detail-section">
          <div class="section-header"><div><h2>审批时间轴</h2><p>部门、财务、执行院长、出纳和财务复核全流程。</p></div></div>
          <el-timeline>
            <el-timeline-item v-for="node in selectedAdvance.timeline" :key="node.nodeType" :timestamp="formatDateTime(node.time)" :color="getTimelineColor(node.status)" placement="top">
              <div class="timeline-card">
                <div class="timeline-title-row"><strong>{{ node.title }}</strong><el-tag :type="getTimelineStatusType(node.status)" size="small">{{ getTimelineStatusLabel(node.status) }}</el-tag></div>
                <div class="timeline-meta"><span>操作人：{{ node.operatorName || '待处理' }}</span><span v-if="node.operatorRole">角色：{{ roleLabels[node.operatorRole] }}</span></div>
                <p>{{ node.comment || '-' }}</p>
              </div>
            </el-timeline-item>
          </el-timeline>
        </section>
      </div>
    </el-dialog>

    <el-dialog v-model="advanceApprovalVisible" :title="advanceApprovalForm.action === 'APPROVE' ? '资金审批通过' : '驳回资金申请'" width="520px">
      <el-form label-width="90px"><el-form-item label="申请编号"><el-input :model-value="selectedAdvance?.applicationNumber" disabled /></el-form-item><el-form-item label="审批意见" required><el-input v-model="advanceApprovalForm.comment" type="textarea" :rows="4" maxlength="500" /></el-form-item></el-form>
      <template #footer><el-button @click="advanceApprovalVisible = false">取消</el-button><el-button :type="advanceApprovalForm.action === 'APPROVE' ? 'success' : 'danger'" :loading="saving" @click="submitAdvanceApproval">确认</el-button></template>
    </el-dialog>

    <el-dialog v-model="advancePaymentVisible" title="出纳付款" width="520px">
      <el-alert type="info" :closable="false" show-icon title="请先在详情中上传银行回执。" />
      <el-form label-width="100px" class="payment-form"><el-form-item label="付款日期" required><el-date-picker v-model="advancePaymentForm.paymentDate" type="date" value-format="YYYY-MM-DD" class="full-width" /></el-form-item><el-form-item label="付款金额" required><el-input-number v-model="advancePaymentForm.paymentAmount" :min="0.01" :precision="2" class="full-width" /></el-form-item><el-form-item label="付款凭证号" required><el-input v-model="advancePaymentForm.voucherNumber" /></el-form-item><el-form-item label="备注"><el-input v-model="advancePaymentForm.comment" type="textarea" :rows="3" /></el-form-item></el-form>
      <template #footer><el-button @click="advancePaymentVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="submitAdvancePayment">确认付款</el-button></template>
    </el-dialog>

    <el-dialog v-model="advanceOffsetVisible" title="登记还款 / 冲账" width="500px">
      <el-form label-width="105px"><el-form-item label="剩余待冲"><el-input :model-value="formatMoney(selectedAdvance?.remainingAmount)" disabled /></el-form-item><el-form-item label="本次冲账金额" required><el-input-number v-model="advanceOffsetForm.amount" :min="0.01" :max="Number(selectedAdvance?.remainingAmount || 0)" :precision="2" class="full-width" /></el-form-item><el-form-item label="冲账说明"><el-input v-model="advanceOffsetForm.comment" type="textarea" :rows="3" maxlength="500" /></el-form-item></el-form>
      <template #footer><el-button @click="advanceOffsetVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="submitAdvanceOffset">保存冲账记录</el-button></template>
    </el-dialog>

    <el-dialog v-model="incomeDialogVisible" :title="incomeDialogMode === 'create' ? '新增收入' : '编辑收入'" width="820px" destroy-on-close>
      <el-form ref="incomeFormRef" :model="incomeForm" :rules="incomeRules" label-width="110px">
        <div class="reimbursement-form-grid">
          <el-form-item label="收款日期" prop="receiptDate"><el-date-picker v-model="incomeForm.receiptDate" type="date" value-format="YYYY-MM-DD" class="full-width" /></el-form-item>
          <el-form-item label="凭证号"><el-input v-model="incomeForm.voucherNumber" maxlength="80" /></el-form-item>
          <el-form-item label="缴款单位/个人" prop="payerName" class="wide"><el-input v-model="incomeForm.payerName" maxlength="200" /></el-form-item>
          <el-form-item label="收入分类" prop="incomeCategory"><el-select v-model="incomeForm.incomeCategory" class="full-width" placeholder="请选择"><el-option v-for="item in incomeCategories" :key="item" :label="item" :value="item" /></el-select></el-form-item>
          <el-form-item label="金额" prop="amount"><el-input-number v-model="incomeForm.amount" :min="0.01" :precision="2" :step="100" class="full-width" /></el-form-item>
          <el-form-item label="资金来源"><el-input v-model="incomeForm.fundingSource" maxlength="160" /></el-form-item>
          <el-form-item label="到账账户"><el-input v-model="incomeForm.arrivalAccount" maxlength="160" /></el-form-item>
          <el-form-item label="开票情况"><el-select v-model="incomeForm.invoiceStatus" clearable class="full-width"><el-option v-for="item in invoiceStatusOptions" :key="item" :label="item" :value="item" /></el-select></el-form-item>
          <el-form-item label="部门"><el-select v-model="incomeForm.departmentId" clearable class="full-width"><el-option v-for="dept in departments" :key="dept.id" :label="dept.name" :value="dept.id" /></el-select></el-form-item>
          <el-form-item label="备注" class="wide"><el-input v-model="incomeForm.remark" type="textarea" :rows="3" maxlength="1000" show-word-limit /></el-form-item>
        </div>
      </el-form>
      <template #footer><el-button @click="incomeDialogVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="saveIncome">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="incomeDetailVisible" title="收入详情" width="820px">
      <div v-if="selectedIncome" class="detail-grid">
        <el-descriptions border :column="2">
          <el-descriptions-item label="收入编号">{{ selectedIncome.incomeNumber }}</el-descriptions-item>
          <el-descriptions-item label="收款日期">{{ selectedIncome.receiptDate }}</el-descriptions-item>
          <el-descriptions-item label="缴款单位/个人">{{ selectedIncome.payerName }}</el-descriptions-item>
          <el-descriptions-item label="收入分类">{{ selectedIncome.incomeCategory }}</el-descriptions-item>
          <el-descriptions-item label="金额">{{ formatMoney(selectedIncome.amount) }}</el-descriptions-item>
          <el-descriptions-item label="部门">{{ selectedIncome.departmentName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="资金来源">{{ selectedIncome.fundingSource || '-' }}</el-descriptions-item>
          <el-descriptions-item label="到账账户">{{ selectedIncome.arrivalAccount || '-' }}</el-descriptions-item>
          <el-descriptions-item label="开票情况">{{ selectedIncome.invoiceStatus || '-' }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ selectedIncome.remark || '-' }}</el-descriptions-item>
        </el-descriptions>
        <section class="detail-section">
          <div class="section-header">
            <div><h2>发票附件</h2><p>收入记录可上传发票或收款附件。</p></div>
            <el-upload v-if="canManageIncome" :show-file-list="false" :http-request="uploadIncomeAttachment" accept="image/*,.pdf,.doc,.docx,.xls,.xlsx">
              <el-button type="primary" :icon="UploadFilled">上传附件</el-button>
            </el-upload>
          </div>
          <el-table :data="incomeAttachments" border empty-text="暂无附件">
            <el-table-column prop="fileName" label="文件名" min-width="220" />
            <el-table-column prop="fileType" label="类型" width="120" />
            <el-table-column label="大小" width="100"><template #default="{ row }">{{ Math.round((row.fileSize || 0) / 1024) }} KB</template></el-table-column>
            <el-table-column label="操作" width="90"><template #default="{ row }"><el-button link type="primary" @click="downloadAttachment('INCOME', row)">查看</el-button></template></el-table-column>
          </el-table>
        </section>
      </div>
    </el-dialog>

    <el-dialog v-model="laborDialogVisible" :title="laborDialogMode === 'create' ? '新增劳务单' : '编辑劳务单'" width="1120px" destroy-on-close>
      <el-form label-width="90px">
        <div class="labor-form-grid">
          <el-form-item label="费用类别" required>
            <el-select v-model="laborForm.category" class="full-width">
              <el-option v-for="item in laborCategories" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="预算编号"><el-input v-model="laborForm.budgetNumber" maxlength="80" /></el-form-item>
          <el-form-item label="发放事项" class="wide" required><el-input v-model="laborForm.title" maxlength="200" /></el-form-item>
          <el-form-item label="情况说明" class="wide"><el-input v-model="laborForm.description" type="textarea" :rows="2" maxlength="1000" show-word-limit /></el-form-item>
        </div>
        <div class="purchase-items-title">
          <div><h3>领款人明细</h3><p>身份证号与银行卡号仅在有权限的详情中显示，列表和导出均自动脱敏。</p></div>
          <el-button type="primary" plain :icon="Plus" @click="addLaborRecipient">添加领款人</el-button>
        </div>
        <div v-for="(item, index) in laborForm.recipients" :key="index" class="labor-recipient-card">
          <div class="recipient-card-header">
            <strong>领款人 {{ index + 1 }}</strong>
            <el-button circle text type="danger" :icon="Delete" :disabled="laborForm.recipients.length === 1" @click="removeLaborRecipient(index)" />
          </div>
          <div class="labor-recipient-grid">
            <el-form-item label="姓名" required><el-input v-model="item.name" maxlength="80" /></el-form-item>
            <el-form-item label="电话"><el-input v-model="item.phone" maxlength="40" /></el-form-item>
            <el-form-item label="身份证号" required><el-input v-model="item.idCard" maxlength="40" /></el-form-item>
            <el-form-item label="单位"><el-input v-model="item.organization" maxlength="160" /></el-form-item>
            <el-form-item label="职务"><el-input v-model="item.position" maxlength="100" /></el-form-item>
            <el-form-item label="实发金额" required><el-input-number v-model="item.netAmount" :min="0" :precision="2" class="full-width" /></el-form-item>
            <el-form-item label="银行卡号" required><el-input v-model="item.bankAccount" maxlength="80" /></el-form-item>
            <el-form-item label="开户行" required><el-input v-model="item.bankName" maxlength="160" /></el-form-item>
            <el-form-item label="劳务内容" class="wide" required><el-input v-model="item.serviceContent" maxlength="500" /></el-form-item>
          </div>
        </div>
        <div class="purchase-total-bar"><span>实发总额（由系统自动计算）</span><strong>{{ formatMoney(laborTotal) }}</strong></div>
      </el-form>
      <template #footer><el-button @click="laborDialogVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="saveLabor">保存劳务单</el-button></template>
    </el-dialog>

    <el-dialog v-model="laborDetailVisible" title="劳务酬金发放详情" width="980px">
      <div v-if="selectedLabor" class="detail-grid">
        <el-descriptions border :column="2">
          <el-descriptions-item label="劳务编号">{{ selectedLabor.applicationNumber }}</el-descriptions-item>
          <el-descriptions-item label="状态"><el-tag :type="getLaborStatusType(selectedLabor.status)">{{ getLaborStatusLabel(selectedLabor.status) }}</el-tag></el-descriptions-item>
          <el-descriptions-item label="类别">{{ laborCategoryLabels[selectedLabor.category] }}</el-descriptions-item>
          <el-descriptions-item label="申请人">{{ selectedLabor.applicantName }} / {{ selectedLabor.departmentName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="发放事项" :span="2">{{ selectedLabor.title }}</el-descriptions-item>
          <el-descriptions-item label="总额">{{ formatMoney(selectedLabor.totalAmount) }}</el-descriptions-item>
          <el-descriptions-item label="金额大写">{{ selectedLabor.amountInWords }}</el-descriptions-item>
          <el-descriptions-item label="预算编号">{{ selectedLabor.budgetNumber || '-' }}</el-descriptions-item>
          <el-descriptions-item label="付款凭证">{{ selectedLabor.paymentVoucherNumber || '-' }}</el-descriptions-item>
          <el-descriptions-item label="说明" :span="2">{{ selectedLabor.description || '-' }}</el-descriptions-item>
        </el-descriptions>
        <section class="detail-section">
          <div class="section-header"><div><h2>领款人明细</h2><p>详情仅向申请人及流程相关人员展示完整敏感信息。</p></div></div>
          <el-table :data="selectedLabor.recipients" border>
            <el-table-column prop="name" label="姓名" width="90" />
            <el-table-column prop="phone" label="电话" width="125" />
            <el-table-column prop="idCard" label="身份证号" width="180" />
            <el-table-column prop="organization" label="单位/职务" min-width="145"><template #default="{ row }">{{ row.organization || '-' }} / {{ row.position || '-' }}</template></el-table-column>
            <el-table-column prop="serviceContent" label="劳务内容" min-width="160" show-overflow-tooltip />
            <el-table-column label="实发金额" width="115"><template #default="{ row }">{{ formatMoney(row.netAmount) }}</template></el-table-column>
            <el-table-column prop="bankAccount" label="银行卡号" width="190" />
            <el-table-column prop="bankName" label="开户行" min-width="150" />
          </el-table>
        </section>
        <section class="detail-section">
          <div class="section-header">
            <div><h2>附件</h2><p>出纳付款时必须上传银行回执。</p></div>
            <div v-if="canEditLabor(selectedLabor) || canPayLabor(selectedLabor)" class="attachment-actions">
              <el-select v-model="laborAttachmentType" class="attachment-type-select">
                <el-option
                  v-for="item in (canPayLabor(selectedLabor) ? laborAttachmentTypes.filter(option => option.value === 'BANK_RECEIPT') : laborAttachmentTypes)"
                  :key="item.value" :label="item.label" :value="item.value"
                />
              </el-select>
              <el-upload :show-file-list="false" :http-request="uploadLaborAttachment">
                <el-button type="primary" :icon="UploadFilled">上传附件</el-button>
              </el-upload>
            </div>
          </div>
          <el-table :data="selectedLabor.attachments" border empty-text="暂无附件">
            <el-table-column label="类型" width="140"><template #default="{ row }">{{ laborAttachmentTypeLabels[row.attachmentType] }}</template></el-table-column>
            <el-table-column prop="fileName" label="文件名" min-width="200" />
            <el-table-column label="操作" width="90"><template #default="{ row }"><el-button link type="primary" @click="downloadAttachment('LABOR', row)">查看</el-button></template></el-table-column>
          </el-table>
        </section>
        <section class="detail-section">
          <div class="section-header"><div><h2>审批时间轴</h2><p>完整展示从申请到财务复核完成的全过程。</p></div></div>
          <el-timeline>
            <el-timeline-item v-for="node in selectedLabor.timeline" :key="node.nodeType" :timestamp="formatDateTime(node.time)" :color="getTimelineColor(node.status)" placement="top">
              <div class="timeline-card">
                <div class="timeline-title-row"><strong>{{ node.title }}</strong><el-tag :type="getTimelineStatusType(node.status)" size="small">{{ getTimelineStatusLabel(node.status) }}</el-tag></div>
                <div class="timeline-meta"><span>操作人：{{ node.operatorName || '待处理' }}</span><span v-if="node.operatorRole">角色：{{ roleLabels[node.operatorRole] }}</span></div>
                <p>{{ node.comment || '-' }}</p>
              </div>
            </el-timeline-item>
          </el-timeline>
        </section>
      </div>
    </el-dialog>

    <el-dialog v-model="laborApprovalVisible" :title="laborApprovalForm.action === 'APPROVE' ? '劳务审批通过' : '驳回劳务申请'" width="520px">
      <el-form label-width="90px">
        <el-form-item label="劳务单"><el-input :model-value="selectedLabor?.applicationNumber" disabled /></el-form-item>
        <el-form-item label="审批意见" required><el-input v-model="laborApprovalForm.comment" type="textarea" :rows="4" maxlength="500" show-word-limit /></el-form-item>
      </el-form>
      <template #footer><el-button @click="laborApprovalVisible = false">取消</el-button><el-button :type="laborApprovalForm.action === 'APPROVE' ? 'success' : 'danger'" :loading="saving" @click="submitLaborApproval">确认</el-button></template>
    </el-dialog>

    <el-dialog v-model="laborPaymentVisible" title="劳务出纳付款" width="540px">
      <el-alert type="info" :closable="false" show-icon title="请先打开详情上传银行回执，再登记付款信息。" />
      <el-form label-width="100px" class="payment-form">
        <el-form-item label="劳务单"><el-input :model-value="selectedLabor?.applicationNumber" disabled /></el-form-item>
        <el-form-item label="付款日期" required><el-date-picker v-model="laborPaymentForm.paymentDate" type="date" value-format="YYYY-MM-DD" class="full-width" /></el-form-item>
        <el-form-item label="付款金额" required><el-input-number v-model="laborPaymentForm.paymentAmount" :min="0.01" :precision="2" class="full-width" /></el-form-item>
        <el-form-item label="付款凭证号" required><el-input v-model="laborPaymentForm.voucherNumber" maxlength="80" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="laborPaymentForm.comment" type="textarea" :rows="3" maxlength="500" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="laborPaymentVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="submitLaborPayment">确认付款</el-button></template>
    </el-dialog>

    <el-dialog v-model="acceptanceDialogVisible" title="办理资产验收入库" width="620px">
      <el-alert type="info" :closable="false" show-icon title="仅显示已完成审批且尚未验收的申购单；提交后将按申购明细自动生成资产台账。" />
      <el-form label-width="100px" class="asset-action-form">
        <el-form-item label="关联申购单" required>
          <el-select v-model="acceptanceForm.purchaseApplicationId" filterable class="full-width" placeholder="请选择已审批申购单">
            <el-option
              v-for="purchase in eligibleAssetPurchases"
              :key="purchase.id"
              :label="`${purchase.applicationNumber} · ${purchase.purchaseReason} · ${formatMoney(purchase.amount)}`"
              :value="purchase.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="接收时间" required>
          <el-date-picker v-model="acceptanceForm.receivedAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" class="full-width" />
        </el-form-item>
        <el-form-item label="存放地点" required><el-input v-model="acceptanceForm.storageLocation" maxlength="200" placeholder="如：A 楼 302 设备室" /></el-form-item>
        <el-form-item label="验收备注"><el-input v-model="acceptanceForm.remark" type="textarea" :rows="3" maxlength="500" show-word-limit /></el-form-item>
      </el-form>
      <template #footer><el-button @click="acceptanceDialogVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="submitAssetAcceptance">确认入库</el-button></template>
    </el-dialog>

    <el-dialog v-model="claimDialogVisible" title="领用资产" width="520px">
      <el-form label-width="92px">
        <el-form-item label="资产编号"><el-input :model-value="selectedAsset?.assetNumber" disabled /></el-form-item>
        <el-form-item label="物品名称"><el-input :model-value="selectedAsset?.itemName" disabled /></el-form-item>
        <el-form-item label="使用地点" required><el-input v-model="claimForm.useLocation" maxlength="200" placeholder="填写资产实际使用地点" /></el-form-item>
        <el-form-item label="领用说明"><el-input v-model="claimForm.remark" type="textarea" :rows="3" maxlength="500" show-word-limit /></el-form-item>
      </el-form>
      <template #footer><el-button @click="claimDialogVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="submitAssetClaim">确认领用</el-button></template>
    </el-dialog>

    <el-dialog v-model="assetDetailVisible" title="资产台账详情" width="820px">
      <div v-if="selectedAsset" class="detail-grid">
        <el-descriptions border :column="2">
          <el-descriptions-item label="资产编号">{{ selectedAsset.assetNumber }}</el-descriptions-item>
          <el-descriptions-item label="资产状态"><el-tag :type="getAssetStatusType(selectedAsset.status)">{{ getAssetStatusLabel(selectedAsset.status) }}</el-tag></el-descriptions-item>
          <el-descriptions-item label="物品名称">{{ selectedAsset.itemName }}</el-descriptions-item>
          <el-descriptions-item label="规格">{{ selectedAsset.specification || '-' }}</el-descriptions-item>
          <el-descriptions-item label="厂商">{{ selectedAsset.manufacturer || '-' }}</el-descriptions-item>
          <el-descriptions-item label="数量">{{ selectedAsset.quantity }}</el-descriptions-item>
          <el-descriptions-item label="资产总价">{{ formatMoney(selectedAsset.totalPrice) }}</el-descriptions-item>
          <el-descriptions-item label="接收时间">{{ formatDateTime(selectedAsset.receivedAt) }}</el-descriptions-item>
          <el-descriptions-item label="当前保管人">{{ selectedAsset.custodianName || '办公室库存' }}</el-descriptions-item>
          <el-descriptions-item label="当前位置">{{ selectedAsset.location }}</el-descriptions-item>
          <el-descriptions-item label="关联申购单">{{ selectedAsset.purchaseApplicationNumber }}</el-descriptions-item>
          <el-descriptions-item label="验收单号">{{ selectedAsset.acceptanceNumber }}</el-descriptions-item>
        </el-descriptions>
        <section class="detail-section">
          <div class="section-header"><div><h2>资产流转历史</h2><p>记录验收入库、领用以及后续位置和状态变化。</p></div></div>
          <el-timeline>
            <el-timeline-item v-for="record in selectedAsset.history" :key="record.id" :timestamp="formatDateTime(record.createdAt)" placement="top">
              <div class="timeline-card">
                <div class="timeline-title-row">
                  <strong>{{ assetHistoryActionLabels[record.action] || record.action }}</strong>
                  <el-tag :type="getAssetStatusType(record.assetStatus)" size="small">{{ getAssetStatusLabel(record.assetStatus) }}</el-tag>
                </div>
                <div class="timeline-meta">
                  <span>操作人：{{ record.operatorName }}</span>
                  <span v-if="record.custodianName">保管人：{{ record.custodianName }}</span>
                  <span v-if="record.receiptNumber">领用单号：{{ record.receiptNumber }}</span>
                  <span>地点：{{ record.location || '-' }}</span>
                </div>
                <p>{{ record.remark || '-' }}</p>
              </div>
            </el-timeline-item>
          </el-timeline>
        </section>
      </div>
    </el-dialog>

    <el-dialog v-model="detailDialogVisible" title="报销详情" width="820px">
      <div v-if="detailData" class="detail-grid">
        <el-descriptions border :column="2">
          <el-descriptions-item label="审批编号">{{ detailData.reimbursement.approvalNumber || '-' }}</el-descriptions-item>
          <el-descriptions-item label="标题">{{ detailData.reimbursement.title }}</el-descriptions-item>
          <el-descriptions-item label="状态"><el-tag :type="getStatusType(detailData.reimbursement.status)">{{ getStatusLabel(detailData.reimbursement.status) }}</el-tag></el-descriptions-item>
          <el-descriptions-item label="申请人">{{ detailData.reimbursement.applicantName }}</el-descriptions-item>
          <el-descriptions-item label="申请人电话">{{ detailData.reimbursement.applicantPhone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="部门">{{ detailData.reimbursement.departmentName }}</el-descriptions-item>
          <el-descriptions-item label="预算编号">{{ detailData.reimbursement.budgetNumber || '-' }}</el-descriptions-item>
          <el-descriptions-item label="费用类型">{{ detailData.reimbursement.expenseType }}</el-descriptions-item>
          <el-descriptions-item label="报销金额">{{ formatMoney(detailData.reimbursement.amount) }}</el-descriptions-item>
          <el-descriptions-item label="付款总金额">{{ formatMoney(detailData.reimbursement.paymentTotal ?? detailData.reimbursement.amount) }}</el-descriptions-item>
          <el-descriptions-item label="付款日期">{{ detailData.reimbursement.paymentDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="付款凭证号">{{ detailData.reimbursement.paymentVoucherNumber || '-' }}</el-descriptions-item>
          <el-descriptions-item label="收款人">{{ detailData.reimbursement.payeeName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="开户行">{{ detailData.reimbursement.bankName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="银行账号">{{ detailData.reimbursement.bankAccount || '-' }}</el-descriptions-item>
          <el-descriptions-item label="关联申购单">{{ detailData.reimbursement.relatedPurchaseNumber || '-' }}</el-descriptions-item>
          <el-descriptions-item label="报销事由" :span="2">{{ detailData.reimbursement.reimbursementReason || '-' }}</el-descriptions-item>
          <el-descriptions-item label="补充说明" :span="2">{{ detailData.reimbursement.description || '-' }}</el-descriptions-item>
          <el-descriptions-item v-if="Number(detailData.reimbursement.amount) > 50000" label="大额报销说明" :span="2">
            {{ detailData.reimbursement.highValueExplanation || '未填写' }}
          </el-descriptions-item>
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
            <div><h2>凭证附件</h2><p>附件可分类归档；只有“发票”类型会进入 OCR 识别。</p></div>
            <div v-if="canUploadRow(detailData.reimbursement)" class="attachment-actions">
              <el-select v-model="attachmentType" class="attachment-type-select">
                <el-option
                  v-for="item in (currentUser.role === 'CASHIER' ? attachmentTypeOptions.filter(option => option.value === 'BANK_RECEIPT') : attachmentTypeOptions)"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
              <el-upload :show-file-list="false" :http-request="uploadAttachment" accept="image/*,.pdf,.doc,.docx">
                <el-button type="primary" :icon="UploadFilled">上传附件</el-button>
              </el-upload>
            </div>
          </div>
          <el-table :data="detailData.attachments" border empty-text="暂无附件">
            <el-table-column label="附件类型" width="130"><template #default="{ row }">{{ attachmentTypeLabels[row.attachmentType] || '其他附件' }}</template></el-table-column>
            <el-table-column prop="fileName" label="文件名" min-width="180" show-overflow-tooltip />
            <el-table-column label="大小" width="110"><template #default="{ row }">{{ Math.round((row.fileSize || 0) / 1024) }} KB</template></el-table-column>
            <el-table-column label="操作" width="180">
              <template #default="{ row }"><el-button link type="primary" @click="downloadAttachment('REIMBURSEMENT', row)">查看</el-button></template>
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

    <el-dialog v-model="paymentDialogVisible" title="登记出纳付款" width="520px">
      <el-alert type="info" :closable="false" show-icon title="请先在报销详情中上传“银行回执”，再登记付款信息。" />
      <el-form label-width="96px" class="payment-form">
        <el-form-item label="报销单"><el-input :model-value="selectedApprovalRow?.title" disabled /></el-form-item>
        <el-form-item label="付款日期" required><el-date-picker v-model="paymentForm.paymentDate" type="date" value-format="YYYY-MM-DD" class="full-width" /></el-form-item>
        <el-form-item label="付款金额" required><el-input-number v-model="paymentForm.paymentAmount" :min="0.01" :precision="2" class="full-width" /></el-form-item>
        <el-form-item label="付款凭证号" required><el-input v-model="paymentForm.voucherNumber" maxlength="80" placeholder="请输入银行流水号或付款凭证号" /></el-form-item>
        <el-form-item label="付款备注"><el-input v-model="paymentForm.comment" type="textarea" :rows="3" maxlength="500" show-word-limit /></el-form-item>
      </el-form>
      <template #footer><el-button @click="paymentDialogVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="submitPayment">确认付款</el-button></template>
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
