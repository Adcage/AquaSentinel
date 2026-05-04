import { buildDeviceApiUrl } from '@/utils/ptzDirectControl'

export interface PtzDeviceResponse {
  action?: string
  raw?: string
  ok?: boolean
  pan?: number
  tilt?: number
  value3?: number
  panMinUs?: number
  panMaxUs?: number
  panCenterUs?: number
  tiltMinUs?: number
  tiltMaxUs?: number
  tiltCenterUs?: number
}

export interface CalibrationSnapshot {
  panMinUs: number
  panMaxUs: number
  panCenterUs: number
  tiltMinUs: number
  tiltMaxUs: number
  tiltCenterUs: number
}

const normalizeCalibrationSnapshot = (payload?: Record<string, unknown>): CalibrationSnapshot => ({
  panMinUs: Number(payload?.panMinUs ?? -1),
  panMaxUs: Number(payload?.panMaxUs ?? -1),
  panCenterUs: Number(payload?.panCenterUs ?? -1),
  tiltMinUs: Number(payload?.tiltMinUs ?? -1),
  tiltMaxUs: Number(payload?.tiltMaxUs ?? -1),
  tiltCenterUs: Number(payload?.tiltCenterUs ?? -1),
})

const requestDevice = async (deviceIp: string, path: string, errorMessage: string) => {
  const url = buildDeviceApiUrl(deviceIp, path)
  if (!url) {
    throw new Error('设备 IP 不能为空')
  }
  const response = await fetch(url)
  if (!response.ok) {
    throw new Error(`${errorMessage}（HTTP ${response.status}）`)
  }
  return (await response.json()) as PtzDeviceResponse
}

export const controlNudge = async (
  deviceIp: string,
  direction: 'LEFT' | 'RIGHT' | 'UP' | 'DOWN',
  step = 5,
) => {
  return requestDevice(deviceIp, `/api/ptz/nudge?dir=${direction}&step=${step}`, '控制失败')
}

export const controlHome = async (deviceIp: string) => {
  return requestDevice(deviceIp, '/api/ptz/home', '回中失败')
}

export const queryPtzStatus = async (deviceIp: string) => {
  return requestDevice(deviceIp, '/api/ptz/status', '状态查询失败')
}

export const startCalibration = async (deviceIp: string) => {
  return requestDevice(deviceIp, '/api/ptz/calib/start', '进入校准失败')
}

export const queryCalibrationData = async (deviceIp: string) => {
  const result = await requestDevice(deviceIp, '/api/ptz/calib/data', '读取校准参数失败')
  return {
    result,
    snapshot: normalizeCalibrationSnapshot(result),
  }
}

export const saveCalibration = async (deviceIp: string) => {
  return requestDevice(deviceIp, '/api/ptz/calib/save', '保存校准失败')
}

export const exitCalibration = async (deviceIp: string) => {
  return requestDevice(deviceIp, '/api/ptz/calib/exit', '退出校准失败')
}

export const setCalibrationPanPulse = async (deviceIp: string, pulse: number) => {
  return requestDevice(deviceIp, `/api/ptz/calib/pan?pulse=${pulse}`, '设置 PAN 脉宽失败')
}

export const setCalibrationTiltPulse = async (deviceIp: string, pulse: number) => {
  return requestDevice(deviceIp, `/api/ptz/calib/tilt?pulse=${pulse}`, '设置 TILT 脉宽失败')
}
