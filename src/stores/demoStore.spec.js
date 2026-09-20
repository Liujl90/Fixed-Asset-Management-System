import { beforeEach, describe, expect, it } from 'vitest'
import {
  approveLoan,
  confirmReturn,
  createTransfer,
  deleteAsset,
  deleteDepartment,
  demoState,
  getAsset,
  login,
  rejectLoan,
  requestReturn,
  resetDemo,
  submitLoanRequest,
} from './demoStore'

describe('demoStore asset lifecycle', () => {
  beforeEach(() => {
    resetDemo()
  })

  it('supports the complete loan and return flow while keeping asset status consistent', () => {
    login('employee', '123456')
    const request = submitLoanRequest({
      assetId: 'asset-3',
      applicantId: 'emp-2',
      departmentId: 'dept-4',
      remark: '测试领用流程',
    })

    expect(request.status).toBe('pending')
    expect(getAsset('asset-3').status).toBe('idle')

    login('admin', '123456')
    approveLoan(request.id)

    expect(request.status).toBe('active')
    expect(getAsset('asset-3').status).toBe('in_use')
    expect(getAsset('asset-3').departmentId).toBe('dept-4')
    expect(getAsset('asset-3').ownerId).toBe('emp-2')

    requestReturn(request.id)
    expect(request.status).toBe('return_pending')

    confirmReturn(request.id)
    expect(request.status).toBe('returned')
    expect(getAsset('asset-3').status).toBe('idle')
    expect(getAsset('asset-3').ownerId).toBeNull()
  })

  it('keeps rejected requests from changing asset ownership', () => {
    login('admin', '123456')
    const request = submitLoanRequest({
      assetId: 'asset-6',
      applicantId: 'emp-1',
      departmentId: 'dept-2',
      remark: '测试驳回',
    })

    rejectLoan(request.id, '当前资产保留为部门备用')

    expect(request.status).toBe('rejected')
    expect(getAsset('asset-6').status).toBe('idle')
    expect(getAsset('asset-6').ownerId).toBeNull()
  })

  it('updates department and owner after a completed transfer', () => {
    login('admin', '123456')
    const record = createTransfer({
      assetId: 'asset-1',
      toDepartmentId: 'dept-3',
      toOwnerId: 'emp-3',
      reason: '支持生产项目开发',
    })

    expect(record.status).toBe('completed')
    expect(getAsset('asset-1').departmentId).toBe('dept-3')
    expect(getAsset('asset-1').ownerId).toBe('emp-3')
    expect(demoState.transferRecords[0].id).toBe(record.id)
  })

  it('prevents deleting departments that still own employees or assets', () => {
    expect(() => deleteDepartment('dept-2')).toThrow('仍被员工或资产引用')
  })

  it('prevents deleting assets that already have lifecycle records', () => {
    expect(() => deleteAsset('asset-1')).toThrow('已有在用关系或历史业务记录')
  })
})
