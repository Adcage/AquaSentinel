import ExcelUtil from './excel'
import PDFUtil from './pdf'
import DateUtil from './date'

type DateInput = Date | string | number

export interface CopyResult {
  success: boolean
  message: string
  error?: unknown
}

export interface DownloadResult {
  success: boolean
  message: string
  error?: unknown
}

export interface RetryOptions {
  retries?: number
  delay?: number
}

export const CommonUtil = {
  formatDate(date: DateInput, format = 'YYYY-MM-DD HH:mm:ss'): string {
    const d = new Date(date)
    const year = d.getFullYear()
    const month = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    const hours = String(d.getHours()).padStart(2, '0')
    const minutes = String(d.getMinutes()).padStart(2, '0')
    const seconds = String(d.getSeconds()).padStart(2, '0')

    return format
      .replace('YYYY', String(year))
      .replace('MM', month)
      .replace('DD', day)
      .replace('HH', hours)
      .replace('mm', minutes)
      .replace('ss', seconds)
  },

  formatFileSize(bytes: number, decimals = 2): string {
    if (bytes === 0) return '0 Bytes'

    const k = 1024
    const dm = decimals < 0 ? 0 : decimals
    const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))

    return parseFloat((bytes / Math.pow(k, i)).toFixed(dm)) + ' ' + sizes[i]
  },

  debounce<T extends (...args: unknown[]) => unknown>(func: T, wait = 300): (...args: Parameters<T>) => void {
    let timeout: ReturnType<typeof setTimeout> | null = null
    return function executedFunction(...args: Parameters<T>) {
      const later = () => {
        timeout = null
        func(...args)
      }
      if (timeout) clearTimeout(timeout)
      timeout = setTimeout(later, wait)
    }
  },

  throttle<T extends (...args: unknown[]) => unknown>(func: T, limit = 300): (...args: Parameters<T>) => void {
    let inThrottle = false
    return function executedFunction(...args: Parameters<T>) {
      if (!inThrottle) {
        func(...args)
        inThrottle = true
        setTimeout(() => (inThrottle = false), limit)
      }
    }
  },

  deepClone<T>(obj: T): T {
    if (obj === null || typeof obj !== 'object') return obj
    if (obj instanceof Date) return new Date(obj) as T
    if (obj instanceof Array) return obj.map(item => this.deepClone(item)) as T
    if (obj instanceof Object) {
      const clonedObj = {} as T
      for (const key in obj) {
        if (Object.prototype.hasOwnProperty.call(obj, key)) {
          clonedObj[key] = this.deepClone(obj[key])
        }
      }
      return clonedObj
    }
    return obj
  },

  generateId(prefix = 'id'): string {
    return `${prefix}_${Date.now()}_${Math.random().toString(36).substring(2, 11)}`
  },

  downloadFile(url: string, fileName: string): void {
    const link = document.createElement('a')
    link.href = url
    link.download = fileName
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
  },

  async copyToClipboard(text: string): Promise<CopyResult> {
    try {
      await navigator.clipboard.writeText(text)
      return { success: true, message: '复制成功' }
    } catch {
      const textarea = document.createElement('textarea')
      textarea.value = text
      textarea.style.position = 'fixed'
      textarea.style.opacity = '0'
      document.body.appendChild(textarea)
      textarea.select()
      try {
        document.execCommand('copy')
        document.body.removeChild(textarea)
        return { success: true, message: '复制成功' }
      } catch (err) {
        document.body.removeChild(textarea)
        return { success: false, message: '复制失败', error: err }
      }
    }
  },

  getUrlParam(name: string, url = window.location.href): string | null {
    const params = new URL(url).searchParams
    return params.get(name)
  },

  formatNumber(num: unknown, decimals = 2): string {
    return Number(num).toLocaleString('zh-CN', {
      minimumFractionDigits: decimals,
      maximumFractionDigits: decimals
    })
  },

  formatCurrency(amount: unknown, currency = '¥'): string {
    return `${currency}${this.formatNumber(amount, 2)}`
  },

  maskPhone(phone: unknown): string {
    return String(phone).replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')
  },

  maskIdCard(idCard: unknown): string {
    return String(idCard).replace(/(\d{6})\d{8}(\d{4})/, '$1********$2')
  },

  maskEmail(email: unknown): string {
    return String(email).replace(/(.{2}).*(@.*)/, '$1***$2')
  },

  validatePhone(phone: unknown): boolean {
    return /^1[3-9]\d{9}$/.test(String(phone))
  },

  validateEmail(email: unknown): boolean {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(String(email))
  },

  validateIdCard(idCard: unknown): boolean {
    return /(^\d{15}$)|(^\d{18}$)|(^\d{17}(\d|X|x)$)/.test(String(idCard))
  },

  unique<T>(arr: T[], key?: keyof T): T[] {
    if (!key) {
      return [...new Set(arr)]
    }
    const map = new Map()
    return arr.filter(item => !map.has(item[key]) && map.set(item[key], 1))
  },

  groupBy<T>(arr: T[], key: keyof T | ((item: T) => string)): Record<string, T[]> {
    return arr.reduce((result, item) => {
      const groupKey = typeof key === 'function' ? key(item) : String(item[key])
      if (!result[groupKey]) {
        result[groupKey] = []
      }
      result[groupKey].push(item)
      return result
    }, {} as Record<string, T[]>)
  },

  sleep(ms: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, ms))
  },

  async retry<T>(fn: () => Promise<T>, retries = 3, delay = 1000): Promise<T> {
    try {
      return await fn()
    } catch (error) {
      if (retries <= 0) throw error
      await this.sleep(delay)
      return this.retry(fn, retries - 1, delay)
    }
  }
}

export { ExcelUtil, PDFUtil, DateUtil }

export default {
  Excel: ExcelUtil,
  PDF: PDFUtil,
  Date: DateUtil,
  Common: CommonUtil
}