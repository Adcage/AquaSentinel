import jsPDF from 'jspdf'
import html2canvas from 'html2canvas'

export interface PDFExportOptions {
  orientation?: 'portrait' | 'landscape'
  format?: 'a4' | 'a3' | 'letter'
  quality?: number
  margin?: { top: number; right: number; bottom: number; left: number }
  compress?: boolean
}

export interface PDFMultiPageElement {
  element: HTMLElement | string
  title?: string
}

export interface PDFTableColumn {
  header: string
  dataKey: string
}

export interface PDFTableOptions {
  title?: string
  columns: PDFTableColumn[]
  fileName?: string
  orientation?: 'portrait' | 'landscape'
  format?: 'a4' | 'a3' | 'letter'
}

class PDFUtil {
  static async exportFromHTML(element: HTMLElement | string, fileName = 'document', options: PDFExportOptions = {}): Promise<{ success: boolean; message: string; error?: unknown }> {
    const {
      orientation = 'portrait',
      format = 'a4',
      quality = 0.95,
      margin = { top: 10, right: 10, bottom: 10, left: 10 },
      compress = true
    } = options

    try {
      const targetElement = typeof element === 'string'
        ? document.querySelector(element) as HTMLElement | null
        : element

      if (!targetElement) {
        throw new Error('未找到目标元素')
      }

      const canvas = await html2canvas(targetElement, {
        scale: 2,
        useCORS: true,
        logging: false,
        backgroundColor: '#ffffff'
      })

      const imgWidth = canvas.width
      const imgHeight = canvas.height

      const pdf = new jsPDF({
        orientation,
        unit: 'mm',
        format,
        compress
      })

      const pageWidth = pdf.internal.pageSize.getWidth()
      const pageHeight = pdf.internal.pageSize.getHeight()

      const contentWidth = pageWidth - margin.left - margin.right
      const contentHeight = pageHeight - margin.top - margin.bottom

      const ratio = Math.min(
        contentWidth / (imgWidth * 0.264583),
        contentHeight / (imgHeight * 0.264583)
      )

      const scaledWidth = (imgWidth * 0.264583) * ratio
      const scaledHeight = (imgHeight * 0.264583) * ratio

      const x = margin.left + (contentWidth - scaledWidth) / 2
      const y = margin.top

      const imgData = canvas.toDataURL('image/jpeg', quality)
      pdf.addImage(imgData, 'JPEG', x, y, scaledWidth, scaledHeight)

      pdf.save(`${fileName}.pdf`)

      return { success: true, message: 'PDF 生成成功' }
    } catch (error) {
      console.error('PDF 生成失败:', error)
      return { success: false, message: 'PDF 生成失败', error }
    }
  }

  static async exportMultiPage(elements: PDFMultiPageElement[], fileName = 'document', options: PDFExportOptions = {}): Promise<{ success: boolean; message: string; error?: unknown }> {
    const {
      orientation = 'portrait',
      format = 'a4',
      quality = 0.95,
      compress = true
    } = options

    try {
      const pdf = new jsPDF({
        orientation,
        unit: 'mm',
        format,
        compress
      })

      const pageWidth = pdf.internal.pageSize.getWidth()
      const pageHeight = pdf.internal.pageSize.getHeight()

      for (let i = 0; i < elements.length; i++) {
        const { element, title } = elements[i]

        const targetElement = typeof element === 'string'
          ? document.querySelector(element) as HTMLElement | null
          : element

        if (!targetElement) {
          console.warn(`未找到元素: ${element}`)
          continue
        }

        const canvas = await html2canvas(targetElement as HTMLElement, {
          scale: 2,
          useCORS: true,
          logging: false,
          backgroundColor: '#ffffff'
        })

        const imgData = canvas.toDataURL('image/jpeg', quality)
        const imgWidth = canvas.width * 0.264583
        const imgHeight = canvas.height * 0.264583

        const ratio = Math.min(
          (pageWidth - 20) / imgWidth,
          (pageHeight - 20) / imgHeight
        )

        const scaledWidth = imgWidth * ratio
        const scaledHeight = imgHeight * ratio

        if (i > 0) {
          pdf.addPage()
        }

        if (title) {
          pdf.setFontSize(16)
          pdf.text(title, pageWidth / 2, 10, { align: 'center' })
        }

        pdf.addImage(
          imgData,
          'JPEG',
          (pageWidth - scaledWidth) / 2,
          title ? 15 : 10,
          scaledWidth,
          scaledHeight
        )
      }

      pdf.save(`${fileName}.pdf`)

      return { success: true, message: 'PDF 生成成功' }
    } catch (error) {
      console.error('多页 PDF 生成失败:', error)
      return { success: false, message: 'PDF 生成失败', error }
    }
  }

  static exportTable(data: Record<string, unknown>[], options: PDFTableOptions): { success: boolean; message: string; error?: unknown } {
    const {
      title = '',
      columns = [],
      fileName = 'table',
      orientation = 'portrait',
      format = 'a4'
    } = options

    try {
      const pdf = new jsPDF({
        orientation,
        unit: 'mm',
        format
      })

      const pageWidth = pdf.internal.pageSize.getWidth()
      let yPosition = 20

      if (title) {
        pdf.setFontSize(18)
        pdf.text(title, pageWidth / 2, yPosition, { align: 'center' })
        yPosition += 10
      }

      pdf.setFontSize(10)
      const cellPadding = 5
      const cellHeight = 10
      const tableWidth = pageWidth - 20
      const columnWidth = tableWidth / columns.length

      pdf.setFillColor(66, 139, 202)
      pdf.setTextColor(255, 255, 255)
      columns.forEach((col, index) => {
        const x = 10 + index * columnWidth
        pdf.rect(x, yPosition, columnWidth, cellHeight, 'F')
        pdf.text(
          col.header,
          x + cellPadding,
          yPosition + cellHeight - 3
        )
      })

      yPosition += cellHeight

      pdf.setTextColor(0, 0, 0)
      data.forEach((row, rowIndex) => {
        if (yPosition > pdf.internal.pageSize.getHeight() - 20) {
          pdf.addPage()
          yPosition = 20
        }

        if (rowIndex % 2 === 0) {
          pdf.setFillColor(245, 245, 245)
          pdf.rect(10, yPosition, tableWidth, cellHeight, 'F')
        }

        columns.forEach((col, colIndex) => {
          const x = 10 + colIndex * columnWidth
          const value = String(row[col.dataKey] ?? '')
          pdf.text(
            value,
            x + cellPadding,
            yPosition + cellHeight - 3
          )
        })

        yPosition += cellHeight
      })

      pdf.save(`${fileName}.pdf`)

      return { success: true, message: 'PDF 生成成功' }
    } catch (error) {
      console.error('表格 PDF 生成失败:', error)
      return { success: false, message: 'PDF 生成失败', error }
    }
  }

  static print(element: HTMLElement | string, options: { title?: string } = {}): { success: boolean; message: string; error?: unknown } {
    const { title = document.title } = options

    try {
      const targetElement = typeof element === 'string'
        ? document.querySelector(element) as HTMLElement | null
        : element

      if (!targetElement) {
        throw new Error('未找到目标元素')
      }

      const printWindow = window.open('', '_blank')
      if (!printWindow) {
        throw new Error('无法打开打印窗口')
      }

      printWindow.document.write(`
        <!DOCTYPE html>
        <html>
          <head>
            <title>${title}</title>
            <style>
              body { margin: 0; padding: 20px; }
              @media print {
                body { margin: 0; }
                @page { margin: 1cm; }
              }
            </style>
          </head>
          <body>
            ${targetElement.innerHTML}
          </body>
        </html>
      `)

      printWindow.document.close()
      printWindow.focus()

      setTimeout(() => {
        printWindow.print()
        printWindow.close()
      }, 250)

      return { success: true, message: '打印成功' }
    } catch (error) {
      console.error('打印失败:', error)
      return { success: false, message: '打印失败', error }
    }
  }

  static async exportQRCode(_text: string, _fileName = 'qrcode', _options: { title?: string; size?: number; orientation?: 'portrait' | 'landscape' } = {}): Promise<{ success: boolean; message: string; error?: unknown }> {
    try {
      console.warn('需要安装 qrcode 库才能使用此功能')
      return { success: false, message: '功能未实现' }
    } catch (error) {
      console.error('二维码 PDF 生成失败:', error)
      return { success: false, message: 'PDF 生成失败', error }
    }
  }

  static preview(pdfBlob: Blob): { success: boolean; message: string; error?: unknown } {
    try {
      const url = URL.createObjectURL(pdfBlob)
      window.open(url, '_blank')

      setTimeout(() => URL.revokeObjectURL(url), 100)

      return { success: true, message: '预览成功' }
    } catch (error) {
      console.error('PDF 预览失败:', error)
      return { success: false, message: '预览失败', error }
    }
  }
}

export default PDFUtil