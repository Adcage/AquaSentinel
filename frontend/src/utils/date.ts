import dayjs, { Dayjs, ConfigType } from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import duration from 'dayjs/plugin/duration'
import isBetween from 'dayjs/plugin/isBetween'
import isSameOrBefore from 'dayjs/plugin/isSameOrBefore'
import isSameOrAfter from 'dayjs/plugin/isSameOrAfter'
import weekday from 'dayjs/plugin/weekday'
import 'dayjs/locale/zh-cn'

// 扩展 dayjs 插件
dayjs.extend(relativeTime)
dayjs.extend(duration)
dayjs.extend(isBetween)
dayjs.extend(isSameOrBefore)
dayjs.extend(isSameOrAfter)
dayjs.extend(weekday)

// 设置中文语言
dayjs.locale('zh-cn')

/**
 * 日期工具类
 * 基于 dayjs 封装的日期处理工具
 */
class DateUtil {
  /**
   * 格式化日期
   * @param date 日期
   * @param format 格式化字符串，默认 'YYYY-MM-DD HH:mm:ss'
   * @returns 格式化后的日期字符串
   */
  static format(date?: ConfigType, format = 'YYYY-MM-DD HH:mm:ss'): string {
    return dayjs(date).format(format)
  }

  /**
   * 格式化为日期（不含时间）
   * @param date 日期
   * @returns 格式化后的日期字符串 (YYYY-MM-DD)
   */
  static formatDate(date?: ConfigType): string {
    return dayjs(date).format('YYYY-MM-DD')
  }

  /**
   * 格式化为时间（不含日期）
   * @param date 日期
   * @returns 格式化后的时间字符串 (HH:mm:ss)
   */
  static formatTime(date?: ConfigType): string {
    return dayjs(date).format('HH:mm:ss')
  }

  /**
   * 格式化为日期时间
   * @param date 日期
   * @returns 格式化后的日期时间字符串 (YYYY-MM-DD HH:mm:ss)
   */
  static formatDateTime(date?: ConfigType): string {
    return dayjs(date).format('YYYY-MM-DD HH:mm:ss')
  }

  /**
   * 格式化为中文日期
   * @param date 日期
   * @returns 格式化后的中文日期字符串 (YYYY年MM月DD日)
   */
  static formatChinese(date?: ConfigType): string {
    return dayjs(date).format('YYYY年MM月DD日')
  }

  /**
   * 格式化为中文日期时间
   * @param date 日期
   * @returns 格式化后的中文日期时间字符串 (YYYY年MM月DD日 HH:mm:ss)
   */
  static formatChineseDateTime(date?: ConfigType): string {
    return dayjs(date).format('YYYY年MM月DD日 HH:mm:ss')
  }

  /**
   * 获取相对时间（如：3天前、2小时后）
   * @param date 日期
   * @param baseDate 基准日期，默认为当前时间
   * @returns 相对时间字符串
   */
  static fromNow(date: ConfigType, baseDate?: ConfigType): string {
    if (baseDate) {
      return dayjs(date).from(dayjs(baseDate))
    }
    return dayjs(date).fromNow()
  }

  /**
   * 获取距离现在的相对时间（如：3天前）
   * @param date 日期
   * @returns 相对时间字符串
   */
  static timeAgo(date: ConfigType): string {
    return dayjs(date).fromNow()
  }

  /**
   * 获取到指定时间的相对时间（如：还有3天）
   * @param date 日期
   * @returns 相对时间字符串
   */
  static toNow(date: ConfigType): string {
    return dayjs(date).toNow()
  }

  /**
   * 解析日期字符串
   * @param dateString 日期字符串
   * @param format 格式化字符串
   * @returns Dayjs 对象
   */
  static parse(dateString: string, format?: string): Dayjs {
    return format ? dayjs(dateString, format) : dayjs(dateString)
  }

  /**
   * 判断是否是有效日期
   * @param date 日期
   * @returns 是否有效
   */
  static isValid(date: ConfigType): boolean {
    return dayjs(date).isValid()
  }

  /**
   * 判断是否是今天
   * @param date 日期
   * @returns 是否是今天
   */
  static isToday(date: ConfigType): boolean {
    return dayjs(date).isSame(dayjs(), 'day')
  }

  /**
   * 判断是否是昨天
   * @param date 日期
   * @returns 是否是昨天
   */
  static isYesterday(date: ConfigType): boolean {
    return dayjs(date).isSame(dayjs().subtract(1, 'day'), 'day')
  }

  /**
   * 判断是否是明天
   * @param date 日期
   * @returns 是否是明天
   */
  static isTomorrow(date: ConfigType): boolean {
    return dayjs(date).isSame(dayjs().add(1, 'day'), 'day')
  }

  /**
   * 判断是否是本周
   * @param date 日期
   * @returns 是否是本周
   */
  static isThisWeek(date: ConfigType): boolean {
    return dayjs(date).isSame(dayjs(), 'week')
  }

  /**
   * 判断是否是本月
   * @param date 日期
   * @returns 是否是本月
   */
  static isThisMonth(date: ConfigType): boolean {
    return dayjs(date).isSame(dayjs(), 'month')
  }

  /**
   * 判断是否是本年
   * @param date 日期
   * @returns 是否是本年
   */
  static isThisYear(date: ConfigType): boolean {
    return dayjs(date).isSame(dayjs(), 'year')
  }

  /**
   * 判断日期是否在指定范围内
   * @param date 日期
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @param inclusivity 包含性 '[]' 包含两端, '()' 不包含两端, '[)' 包含开始不包含结束, '(]' 不包含开始包含结束
   * @returns 是否在范围内
   */
  static isBetween(
    date: ConfigType,
    startDate: ConfigType,
    endDate: ConfigType,
    inclusivity: '[]' | '()' | '[)' | '(]' = '[]'
  ): boolean {
    return dayjs(date).isBetween(startDate, endDate, null, inclusivity)
  }

  /**
   * 判断日期是否在指定日期之前
   * @param date 日期
   * @param compareDate 比较日期
   * @returns 是否在之前
   */
  static isBefore(date: ConfigType, compareDate: ConfigType): boolean {
    return dayjs(date).isBefore(compareDate)
  }

  /**
   * 判断日期是否在指定日期之后
   * @param date 日期
   * @param compareDate 比较日期
   * @returns 是否在之后
   */
  static isAfter(date: ConfigType, compareDate: ConfigType): boolean {
    return dayjs(date).isAfter(compareDate)
  }

  /**
   * 判断日期是否在指定日期之前或相同
   * @param date 日期
   * @param compareDate 比较日期
   * @returns 是否在之前或相同
   */
  static isSameOrBefore(date: ConfigType, compareDate: ConfigType): boolean {
    return dayjs(date).isSameOrBefore(compareDate)
  }

  /**
   * 判断日期是否在指定日期之后或相同
   * @param date 日期
   * @param compareDate 比较日期
   * @returns 是否在之后或相同
   */
  static isSameOrAfter(date: ConfigType, compareDate: ConfigType): boolean {
    return dayjs(date).isSameOrAfter(compareDate)
  }

  /**
   * 获取两个日期之间的差值
   * @param date1 日期1
   * @param date2 日期2
   * @param unit 单位 ('year' | 'month' | 'week' | 'day' | 'hour' | 'minute' | 'second' | 'millisecond')
   * @returns 差值
   */
  static diff(
    date1: ConfigType,
    date2: ConfigType,
    unit: 'year' | 'month' | 'week' | 'day' | 'hour' | 'minute' | 'second' | 'millisecond' = 'day'
  ): number {
    return dayjs(date1).diff(dayjs(date2), unit)
  }

  /**
   * 添加时间
   * @param date 日期
   * @param amount 数量
   * @param unit 单位
   * @returns 新的日期
   */
  static add(
    date: ConfigType,
    amount: number,
    unit: 'year' | 'month' | 'week' | 'day' | 'hour' | 'minute' | 'second' | 'millisecond'
  ): Dayjs {
    return dayjs(date).add(amount, unit)
  }

  /**
   * 减少时间
   * @param date 日期
   * @param amount 数量
   * @param unit 单位
   * @returns 新的日期
   */
  static subtract(
    date: ConfigType,
    amount: number,
    unit: 'year' | 'month' | 'week' | 'day' | 'hour' | 'minute' | 'second' | 'millisecond'
  ): Dayjs {
    return dayjs(date).subtract(amount, unit)
  }

  /**
   * 获取日期的开始时间
   * @param date 日期
   * @param unit 单位
   * @returns 开始时间
   */
  static startOf(
    date: ConfigType,
    unit: 'year' | 'month' | 'week' | 'day' | 'hour' | 'minute' | 'second'
  ): Dayjs {
    return dayjs(date).startOf(unit)
  }

  /**
   * 获取日期的结束时间
   * @param date 日期
   * @param unit 单位
   * @returns 结束时间
   */
  static endOf(
    date: ConfigType,
    unit: 'year' | 'month' | 'week' | 'day' | 'hour' | 'minute' | 'second'
  ): Dayjs {
    return dayjs(date).endOf(unit)
  }

  /**
   * 获取今天的开始时间（00:00:00）
   * @returns 今天的开始时间
   */
  static startOfToday(): Dayjs {
    return dayjs().startOf('day')
  }

  /**
   * 获取今天的结束时间（23:59:59）
   * @returns 今天的结束时间
   */
  static endOfToday(): Dayjs {
    return dayjs().endOf('day')
  }

  /**
   * 获取本周的开始时间
   * @returns 本周的开始时间
   */
  static startOfWeek(): Dayjs {
    return dayjs().startOf('week')
  }

  /**
   * 获取本周的结束时间
   * @returns 本周的结束时间
   */
  static endOfWeek(): Dayjs {
    return dayjs().endOf('week')
  }

  /**
   * 获取本月的开始时间
   * @returns 本月的开始时间
   */
  static startOfMonth(): Dayjs {
    return dayjs().startOf('month')
  }

  /**
   * 获取本月的结束时间
   * @returns 本月的结束时间
   */
  static endOfMonth(): Dayjs {
    return dayjs().endOf('month')
  }

  /**
   * 获取本年的开始时间
   * @returns 本年的开始时间
   */
  static startOfYear(): Dayjs {
    return dayjs().startOf('year')
  }

  /**
   * 获取本年的结束时间
   * @returns 本年的结束时间
   */
  static endOfYear(): Dayjs {
    return dayjs().endOf('year')
  }

  /**
   * 获取当前时间戳（毫秒）
   * @returns 时间戳
   */
  static timestamp(): number {
    return dayjs().valueOf()
  }

  /**
   * 获取当前时间戳（秒）
   * @returns 时间戳
   */
  static timestampSecond(): number {
    return dayjs().unix()
  }

  /**
   * 时间戳转日期
   * @param timestamp 时间戳（毫秒或秒）
   * @returns Dayjs 对象
   */
  static fromTimestamp(timestamp: number): Dayjs {
    // 如果是秒级时间戳（10位），转换为毫秒
    if (timestamp.toString().length === 10) {
      return dayjs.unix(timestamp)
    }
    return dayjs(timestamp)
  }

  /**
   * 获取年份
   * @param date 日期
   * @returns 年份
   */
  static getYear(date?: ConfigType): number {
    return dayjs(date).year()
  }

  /**
   * 获取月份（1-12）
   * @param date 日期
   * @returns 月份
   */
  static getMonth(date?: ConfigType): number {
    return dayjs(date).month() + 1
  }

  /**
   * 获取日期（1-31）
   * @param date 日期
   * @returns 日期
   */
  static getDate(date?: ConfigType): number {
    return dayjs(date).date()
  }

  /**
   * 获取星期几（0-6，0表示周日）
   * @param date 日期
   * @returns 星期几
   */
  static getDay(date?: ConfigType): number {
    return dayjs(date).day()
  }

  /**
   * 获取星期几（中文）
   * @param date 日期
   * @returns 星期几
   */
  static getDayName(date?: ConfigType): string {
    const day = dayjs(date).day()
    const dayNames = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
    return dayNames[day]
  }

  /**
   * 获取小时（0-23）
   * @param date 日期
   * @returns 小时
   */
  static getHour(date?: ConfigType): number {
    return dayjs(date).hour()
  }

  /**
   * 获取分钟（0-59）
   * @param date 日期
   * @returns 分钟
   */
  static getMinute(date?: ConfigType): number {
    return dayjs(date).minute()
  }

  /**
   * 获取秒（0-59）
   * @param date 日期
   * @returns 秒
   */
  static getSecond(date?: ConfigType): number {
    return dayjs(date).second()
  }

  /**
   * 获取当前月份的天数
   * @param date 日期
   * @returns 天数
   */
  static daysInMonth(date?: ConfigType): number {
    return dayjs(date).daysInMonth()
  }

  /**
   * 获取两个日期之间的所有日期
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @returns 日期数组
   */
  static getDateRange(startDate: ConfigType, endDate: ConfigType): Dayjs[] {
    const dates: Dayjs[] = []
    let current = dayjs(startDate)
    const end = dayjs(endDate)

    while (current.isBefore(end) || current.isSame(end, 'day')) {
      dates.push(current)
      current = current.add(1, 'day')
    }

    return dates
  }

  /**
   * 获取最近N天的日期范围
   * @param days 天数
   * @returns [开始日期, 结束日期]
   */
  static getRecentDays(days: number): [Dayjs, Dayjs] {
    const end = dayjs()
    const start = end.subtract(days - 1, 'day')
    return [start, end]
  }

  /**
   * 格式化时长（如：2小时30分钟）
   * @param milliseconds 毫秒数
   * @returns 格式化后的时长字符串
   */
  static formatDuration(milliseconds: number): string {
    const dur = dayjs.duration(milliseconds)
    const days = Math.floor(dur.asDays())
    const hours = dur.hours()
    const minutes = dur.minutes()
    const seconds = dur.seconds()

    const parts: string[] = []
    if (days > 0) parts.push(`${days}天`)
    if (hours > 0) parts.push(`${hours}小时`)
    if (minutes > 0) parts.push(`${minutes}分钟`)
    if (seconds > 0 || parts.length === 0) parts.push(`${seconds}秒`)

    return parts.join('')
  }

  /**
   * 智能格式化日期（根据时间远近自动选择格式）
   * @param date 日期
   * @returns 格式化后的字符串
   */
  static smartFormat(date: ConfigType): string {
    const d = dayjs(date)
    const now = dayjs()
    const diffMinutes = now.diff(d, 'minute')
    const diffHours = now.diff(d, 'hour')
    const diffDays = now.diff(d, 'day')

    if (diffMinutes < 1) {
      return '刚刚'
    } else if (diffMinutes < 60) {
      return `${diffMinutes}分钟前`
    } else if (diffHours < 24) {
      return `${diffHours}小时前`
    } else if (diffDays < 7) {
      return `${diffDays}天前`
    } else if (d.year() === now.year()) {
      return d.format('MM-DD HH:mm')
    } else {
      return d.format('YYYY-MM-DD')
    }
  }
}

export default DateUtil
