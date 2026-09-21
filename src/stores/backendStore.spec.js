import { beforeEach, describe, expect, it } from 'vitest'
import {
  canAccess,
  demoState,
  getAssetStatusMeta,
  getLoanStatusMeta,
} from './backendStore'

describe('backendStore permission compatibility', () => {
  beforeEach(() => {
    demoState.currentUser = null
  })

  it('allows administrators to access every menu', () => {
    demoState.currentUser = {
      id: 1,
      username: 'admin',
      roleCode: 'ADMIN',
      permissions: [],
    }

    expect(canAccess('dashboard')).toBe(true)
    expect(canAccess('system')).toBe(true)
    expect(canAccess('assets')).toBe(true)
  })

  it('maps asset manager permissions to the correct menus', () => {
    demoState.currentUser = {
      id: 2,
      username: 'manager',
      roleCode: 'ASSET_MANAGER',
      permissions: ['asset:read', 'asset:write', 'loan:read', 'loan:manage'],
    }

    expect(canAccess('assets')).toBe(true)
    expect(canAccess('loans')).toBe(true)
    expect(canAccess('system')).toBe(false)
  })

  it('maps employee permissions to the personal workspace', () => {
    demoState.currentUser = {
      id: 3,
      username: 'employee',
      roleCode: 'EMPLOYEE',
      permissions: ['asset:read', 'loan:read', 'loan:return', 'profile:update'],
    }

    expect(canAccess('my-assets')).toBe(true)
    expect(canAccess('profile')).toBe(true)
    expect(canAccess('transfers')).toBe(false)
  })

  it('normalizes server status codes for display components', () => {
    expect(getAssetStatusMeta('IN_USE').label).toBe('在用')
    expect(getLoanStatusMeta('RETURN_PENDING').label).toBe('待归还确认')
  })
})
