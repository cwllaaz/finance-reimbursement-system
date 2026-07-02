export const formatMoney = (value) => `￥${Number(value || 0).toFixed(2)}`

export const formatDateTime = (value) => (
  value ? String(value).replace('T', ' ').slice(0, 19) : '-'
)

export const formatDate = (value) => (
  value ? String(value).slice(0, 10) : '-'
)
