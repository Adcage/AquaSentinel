import type { AxiosResponse, InternalAxiosRequestConfig } from 'axios'

export interface ApiResponse<T = unknown> {
  success: boolean
  data: T
  message?: string
}

export interface ApiError {
  message: string
  status?: number
  response?: {
    status: number
    data: unknown
  }
  request?: unknown
}

export type RequestInterceptors<T = unknown> = {
  onFulfilled?: (config: InternalAxiosRequestConfig<T>) => InternalAxiosRequestConfig<T> | Promise<InternalAxiosRequestConfig<T>>
  onRejected?: (error: unknown) => unknown
}

export type ResponseInterceptors<T = AxiosResponse> = {
  onFulfilled?: (response: T) => T | Promise<T>
  onRejected?: (error: unknown) => unknown
}