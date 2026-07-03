import assert from 'node:assert/strict'
import { canAccessMenu, defaultMenuForRole, routeAccess } from '../src/composables/usePermissions.js'
import { routeCatalog } from '../src/views/routeCatalog.js'

const expected = {
  EMPLOYEE: {
    defaultMenu: 'myApplications',
    menus: ['myApplications', 'reimbursement', 'purchases', 'assets', 'labor', 'advances', 'profile'],
  },
  DEPARTMENT_MANAGER: {
    defaultMenu: 'dashboard',
    menus: ['dashboard', 'myApplications', 'myTodos', 'doneItems', 'reimbursement', 'purchases', 'assets', 'labor', 'advances', 'approval', 'ledger', 'profile'],
  },
  FINANCE: {
    defaultMenu: 'dashboard',
    menus: ['dashboard', 'myApplications', 'myTodos', 'doneItems', 'reimbursement', 'purchases', 'assets', 'labor', 'advances', 'approval', 'paymentTasks', 'incomes', 'ledger', 'budget', 'report', 'profile'],
  },
  OFFICE: {
    defaultMenu: 'myApplications',
    menus: ['myApplications', 'purchases', 'assets', 'profile'],
  },
  EXECUTIVE: {
    defaultMenu: 'dashboard',
    menus: ['dashboard', 'myApplications', 'myTodos', 'doneItems', 'reimbursement', 'purchases', 'assets', 'labor', 'advances', 'approval', 'ledger', 'report', 'profile'],
  },
  COMMITTEE: {
    defaultMenu: 'reimbursement',
    menus: ['myApplications', 'reimbursement', 'purchases', 'assets', 'labor', 'advances', 'profile'],
  },
  CASHIER: {
    defaultMenu: 'myTodos',
    menus: ['myApplications', 'myTodos', 'doneItems', 'reimbursement', 'labor', 'advances', 'profile'],
  },
  ADMIN: {
    defaultMenu: 'dashboard',
    menus: routeCatalog.map(([menu]) => menu),
  },
}

const catalogMenus = routeCatalog.map(([menu]) => menu)
assert.deepEqual(Object.keys(routeAccess).sort(), [...catalogMenus].sort(), 'Every route must have an access rule')

for (const [role, config] of Object.entries(expected)) {
  assert.equal(defaultMenuForRole(role), config.defaultMenu, `${role} default menu`)
  const actualMenus = catalogMenus.filter((menu) => canAccessMenu(role, menu))
  assert.deepEqual(actualMenus, config.menus, `${role} visible menus`)
  assert.ok(actualMenus.includes(config.defaultMenu), `${role} must access its default menu`)
}

console.log(`Role access verified for ${Object.keys(expected).length} demo roles and ${catalogMenus.length} routes.`)
