/**
 * 简化的字段锁定管理器
 * 专门解决多选/单选按钮锁定不稳定问题
 *
 * 设计原则：
 * 1. 只在实际编辑时锁定
 * 2. 明确的释放时机
 * 3. 避免复杂的定时器和多重释放机制
 * 4. 简单可靠的状态管理
 */

import { reactive } from 'vue'

interface CollaborationUser {
  id: string
  name: string
  color?: string
}

interface SimpleFieldLock {
  isLocked: boolean
  lockedBy?: CollaborationUser
  lockedAt: number
}

// WebSocket 同步回调类型
type SyncCallback = (reportId: number, fieldName: string, eventType: 'FIELD_LOCK' | 'FIELD_UNLOCK') => void

class SimpleFieldLockManager {
  private fieldLocks = reactive<Record<string, Record<string, SimpleFieldLock>>>({})
  private syncCallback?: SyncCallback
  private reportId: number = 0

  /**
   * 设置 WebSocket 同步回调
   */
  setSyncCallback(callback: SyncCallback, reportId: number) {
    this.syncCallback = callback
    this.reportId = reportId
  }

  /**
   * 锁定字段（只在实际编辑时调用）
   */
  lockField(roomId: string, fieldName: string, user: CollaborationUser): boolean {
    if (!this.fieldLocks[roomId]) {
      this.fieldLocks[roomId] = {}
    }

    const currentLock = this.fieldLocks[roomId][fieldName]

    // 如果已被其他用户锁定，返回失败
    if (currentLock?.isLocked && currentLock.lockedBy?.id !== user.id) {
      console.log(`🔒 [SimpleFieldLock] 字段 ${fieldName} 已被 ${currentLock.lockedBy?.name} 锁定`)
      return false
    }

    // 锁定字段
    this.fieldLocks[roomId][fieldName] = {
      isLocked: true,
      lockedBy: user,
      lockedAt: Date.now()
    }

    // 触发 WebSocket 同步
    if (this.syncCallback && this.reportId > 0) {
      this.syncCallback(this.reportId, fieldName, 'FIELD_LOCK')
    }

    console.log(`🔒 [SimpleFieldLock] 锁定字段: ${fieldName} (用户: ${user.name})`)
    return true
  }

  /**
   * 释放字段
   */
  unlockField(roomId: string, fieldName: string, userId: string): boolean {
    if (!this.fieldLocks[roomId]?.[fieldName]) {
      return true
    }

    const lock = this.fieldLocks[roomId][fieldName]

    // 只有锁定者才能释放
    if (lock.isLocked && lock.lockedBy?.id !== userId) {
      console.log(`❌ [SimpleFieldLock] 无权释放字段 ${fieldName}，锁定者: ${lock.lockedBy?.name}`)
      return false
    }

    // 释放锁定
    this.fieldLocks[roomId][fieldName] = {
      isLocked: false,
      lockedBy: undefined,
      lockedAt: 0
    }

    // 触发 WebSocket 同步
    if (this.syncCallback && this.reportId > 0) {
      this.syncCallback(this.reportId, fieldName, 'FIELD_UNLOCK')
    }

    console.log(`🔓 [SimpleFieldLock] 释放字段: ${fieldName}`)
    return true
  }

  /**
   * 批量释放用户的所有锁定字段（切换字段时调用）
   */
  unlockUserFields(roomId: string, userId: string, excludeField?: string): string[] {
    if (!this.fieldLocks[roomId]) {
      return []
    }

    const unlockedFields: string[] = []

    Object.keys(this.fieldLocks[roomId]).forEach(fieldName => {
      if (fieldName === excludeField) return

      const lock = this.fieldLocks[roomId][fieldName]
      if (lock?.isLocked && lock.lockedBy?.id === userId) {
        this.fieldLocks[roomId][fieldName] = {
          isLocked: false,
          lockedBy: undefined,
          lockedAt: 0
        }

        // 触发 WebSocket 同步
        if (this.syncCallback && this.reportId > 0) {
          this.syncCallback(this.reportId, fieldName, 'FIELD_UNLOCK')
        }

        unlockedFields.push(fieldName)
      }
    })

    if (unlockedFields.length > 0) {
      console.log(`🔓 [SimpleFieldLock] 批量释放用户字段: ${unlockedFields.join(', ')} (用户ID: ${userId})`)
    }

    return unlockedFields
  }

  /**
   * 获取字段锁定状态
   */
  getFieldLock(roomId: string, fieldName: string): SimpleFieldLock {
    return this.fieldLocks[roomId]?.[fieldName] || {
      isLocked: false,
      lockedBy: undefined,
      lockedAt: 0
    }
  }

  /**
   * 检查字段是否被其他用户锁定
   */
  isFieldLockedByOther(roomId: string, fieldName: string, currentUserId: string): boolean {
    const lock = this.getFieldLock(roomId, fieldName)
    return lock.isLocked && lock.lockedBy?.id !== currentUserId
  }

  /**
   * 检查字段是否被当前用户锁定
   */
  isFieldLockedByUser(roomId: string, fieldName: string, userId: string): boolean {
    const lock = this.getFieldLock(roomId, fieldName)
    return lock.isLocked && lock.lockedBy?.id === userId
  }

  /**
   * 清理超时锁定（可选的安全机制，超时5分钟自动释放）
   */
  cleanupTimeoutLocks(roomId: string, timeoutMs: number = 5 * 60 * 1000): string[] {
    if (!this.fieldLocks[roomId]) {
      return []
    }

    const now = Date.now()
    const timeoutFields: string[] = []

    Object.keys(this.fieldLocks[roomId]).forEach(fieldName => {
      const lock = this.fieldLocks[roomId][fieldName]
      if (lock?.isLocked && (now - lock.lockedAt) > timeoutMs) {
        this.fieldLocks[roomId][fieldName] = {
          isLocked: false,
          lockedBy: undefined,
          lockedAt: 0
        }
        timeoutFields.push(fieldName)
      }
    })

    if (timeoutFields.length > 0) {
      console.log(`⏰ [SimpleFieldLock] 清理超时锁定: ${timeoutFields.join(', ')}`)
    }

    return timeoutFields
  }

  /**
   * 处理远程WebSocket锁定事件（其他用户的锁定操作）
   */
  handleRemoteLockEvent(roomId: string, fieldName: string, eventType: 'FIELD_LOCK' | 'FIELD_UNLOCK', user?: CollaborationUser): void {
    if (!this.fieldLocks[roomId]) {
      this.fieldLocks[roomId] = {}
    }

    if (eventType === 'FIELD_LOCK' && user) {
      // 远程锁定事件
      this.fieldLocks[roomId][fieldName] = {
        isLocked: true,
        lockedBy: user,
        lockedAt: Date.now()
      }
      console.log(`🌐 [SimpleFieldLock] 远程锁定字段: ${fieldName} (用户: ${user.name})`)
    } else if (eventType === 'FIELD_UNLOCK') {
      // 远程解锁事件
      this.fieldLocks[roomId][fieldName] = {
        isLocked: false,
        lockedBy: undefined,
        lockedAt: 0
      }
      console.log(`🌐 [SimpleFieldLock] 远程释放字段: ${fieldName}`)
    }
  }

  /**
   * 获取所有锁定状态（调试用）
   */
  getAllLocks(roomId: string): Record<string, SimpleFieldLock> {
    return this.fieldLocks[roomId] || {}
  }
}

// 全局实例
export const simpleFieldLockManager = new SimpleFieldLockManager()

// 类型导出
export type { CollaborationUser, SimpleFieldLock }