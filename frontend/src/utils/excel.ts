import * as XLSX from 'xlsx'
import { saveAs } from 'file-saver'

export interface ExcelSheet {
  name: string
  data: Record<string, unknown>[]
}

export interface ExcelExportOptions {
  sheetName?: string
  header?: string[]
  columnWidths?: Record<string, number>
  autoWidth?: boolean
}

export interface ExcelImportOptions {
  sheetIndex?: number
  sheetName?: string | null
  header?: boolean
  transform?: (data: Record<string, unknown>[]) => Record<string, unknown>[]
}

export interface ExcelImportResult {
  success: boolean
  data: Record<string, unknown>[]
  sheetNames: string[]
}

export interface ValidationResult {
  valid: boolean
  message: string
}

class ExcelUtil {
  static exportExcel(data: Record<string, unknown>[], fileName = 'export', options: ExcelExportOptions = {}): { success: boolean; message: string; error?: unknown } {
    const {
      sheetName = 'Sheet1',
      header = null,
      columnWidths = null,
      autoWidth = true
    } = options

    try {
      const wb = XLSX.utils.book_new()

      let ws: XLSX.WorkSheet
      if (header && Array.isArray(header)) {
        const wsData = [header, ...data.map(item => Object.values(item))]
        ws = XLSX.utils.aoa_to_sheet(wsData)
      } else {
        ws = XLSX.utils.json_to_sheet(data)
      }

      if (columnWidths) {
        ws['!cols'] = Object.entries(columnWidths).map(([, width]) => ({
          wch: width
        }))
      } else if (autoWidth) {
        const colWidths = this._calculateColumnWidths(data, header)
        ws['!cols'] = colWidths
      }

      XLSX.utils.book_append_sheet(wb, ws, sheetName)

      const wbout = XLSX.write(wb, { bookType: 'xlsx', type: 'array' })
      const blob = new Blob([wbout], { type: 'application/octet-stream' })

      saveAs(blob, `${fileName}.xlsx`)

      return { success: true, message: '导出成功' }
    } catch (error) {
      console.error('Excel 导出失败:', error)
      return { success: false, message: '导出失败', error }
    }
  }

  static exportMultiSheet(sheets: ExcelSheet[], fileName = 'export'): { success: boolean; message: string; error?: unknown } {
    try {
      const wb = XLSX.utils.book_new()

      sheets.forEach(sheet => {
        const ws = XLSX.utils.json_to_sheet(sheet.data)
        XLSX.utils.book_append_sheet(wb, ws, sheet.name)
      })

      const wbout = XLSX.write(wb, { bookType: 'xlsx', type: 'array' })
      const blob = new Blob([wbout], { type: 'application/octet-stream' })
      saveAs(blob, `${fileName}.xlsx`)

      return { success: true, message: '导出成功' }
    } catch (error) {
      console.error('多表导出失败:', error)
      return { success: false, message: '导出失败', error }
    }
  }

  static importExcel(file: File, options: ExcelImportOptions = {}): Promise<ExcelImportResult> {
    const {
      sheetIndex = 0,
      sheetName = null,
      header = true,
      transform = null
    } = options

    return new Promise((resolve, reject) => {
      const reader = new FileReader()

      reader.onload = (e) => {
        try {
          const data = new Uint8Array(e.target?.result as ArrayBuffer)
          const workbook = XLSX.read(data, { type: 'array' })

          let worksheet: XLSX.WorkSheet | undefined
          if (sheetName) {
            worksheet = workbook.Sheets[sheetName]
          } else {
            const sheetNameStr = workbook.SheetNames[sheetIndex]
            worksheet = workbook.Sheets[sheetNameStr]
          }

          if (!worksheet) {
            reject(new Error('工作表不存在'))
            return
          }

          let jsonData = XLSX.utils.sheet_to_json(worksheet, {
            header: header ? undefined : 1,
            defval: ''
          }) as Record<string, unknown>[]

          if (transform && typeof transform === 'function') {
            jsonData = transform(jsonData)
          }

          resolve({
            success: true,
            data: jsonData,
            sheetNames: workbook.SheetNames
          })
        } catch (error) {
          console.error('Excel 解析失败:', error)
          reject({
            success: false,
            message: '解析失败',
            error
          })
        }
      }

      reader.onerror = (error) => {
        reject({
          success: false,
          message: '文件读取失败',
          error
        })
      }

      reader.readAsArrayBuffer(file)
    })
  }

  static downloadTemplate(headers: string[], fileName = 'template', sampleData: (string | number)[][] = []): { success: boolean; message: string; error?: unknown } {
    try {
      const data = [headers, ...sampleData]
      const ws = XLSX.utils.aoa_to_sheet(data)
      const wb = XLSX.utils.book_new()
      XLSX.utils.book_append_sheet(wb, ws, 'Template')

      const wbout = XLSX.write(wb, { bookType: 'xlsx', type: 'array' })
      const blob = new Blob([wbout], { type: 'application/octet-stream' })
      saveAs(blob, `${fileName}.xlsx`)

      return { success: true, message: '模板下载成功' }
    } catch (error) {
      console.error('模板下载失败:', error)
      return { success: false, message: '下载失败', error }
    }
  }

  static exportCSV(data: Record<string, unknown>[], fileName = 'export', options: { header?: string[] | null; delimiter?: string } = {}): { success: boolean; message: string; error?: unknown } {
    const { header = null, delimiter = ',' } = options

    try {
      let csvContent = ''

      if (header) {
        csvContent += header.join(delimiter) + '\n'
      } else if (data.length > 0) {
        csvContent += Object.keys(data[0]).join(delimiter) + '\n'
      }

      data.forEach(row => {
        const values = Object.values(row).map(val => {
          if (typeof val === 'string' && (val.includes(delimiter) || val.includes('"'))) {
            return `"${val.replace(/"/g, '""')}"`
          }
          return val
        })
        csvContent += values.join(delimiter) + '\n'
      })

      const blob = new Blob(['\ufeff' + csvContent], {
        type: 'text/csv;charset=utf-8;'
      })
      saveAs(blob, `${fileName}.csv`)

      return { success: true, message: '导出成功' }
    } catch (error) {
      console.error('CSV 导出失败:', error)
      return { success: false, message: '导出失败', error }
    }
  }

  static _calculateColumnWidths(data: Record<string, unknown>[], header: string[] | null): { wch: number }[] {
    if (!data || data.length === 0) return []

    const keys = header || Object.keys(data[0])
    return keys.map((key, index) => {
      let maxWidth = String(key).length

      data.forEach(row => {
        const value = header ? Object.values(row)[index] : row[key]
        const valueLength = String(value ?? '').length
        if (valueLength > maxWidth) {
          maxWidth = valueLength
        }
      })

      return { wch: Math.min(Math.max(maxWidth + 2, 10), 50) }
    })
  }

  static validateFile(file: File | null, options: { maxSize?: number; allowedExtensions?: string[] } = {}): ValidationResult {
    const {
      maxSize = 10 * 1024 * 1024,
      allowedExtensions = ['.xlsx', '.xls', '.csv']
    } = options

    if (!file) {
      return { valid: false, message: '请选择文件' }
    }

    if (file.size > maxSize) {
      return {
        valid: false,
        message: `文件大小不能超过 ${maxSize / 1024 / 1024}MB`
      }
    }

    const fileName = file.name.toLowerCase()
    const isValidExtension = allowedExtensions.some(ext =>
      fileName.endsWith(ext)
    )

    if (!isValidExtension) {
      return {
        valid: false,
        message: `只支持 ${allowedExtensions.join(', ')} 格式的文件`
      }
    }

    return { valid: true, message: '验证通过' }
  }
}

export default ExcelUtil