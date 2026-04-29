import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useNavigate, Link } from 'react-router-dom'
import { useState } from 'react'
import { authApi } from '../../api/authApi'
import { useAuthStore } from '../../lib/authStore'

const schema = z.object({
  email: z.string().min(1, 'Email is required').email('Invalid email'),
  password: z.string().min(1, 'Password is required'),
  rememberMe: z.boolean().optional(),
})

type FormData = z.infer<typeof schema>

function defaultDeviceLabel(): string {
  if (typeof navigator === 'undefined') return 'Browser'
  const ua = navigator.userAgent ?? ''
  if (/iPhone|iPad/i.test(ua)) return 'iOS device'
  if (/Android/i.test(ua)) return 'Android device'
  if (/Mac OS X/i.test(ua)) return 'Mac'
  if (/Windows/i.test(ua)) return 'Windows PC'
  if (/Linux/i.test(ua)) return 'Linux'
  return 'Browser'
}

export default function LoginPage() {
  const navigate = useNavigate()
  const setAuth = useAuthStore((s) => s.setAuth)
  const [apiError, setApiError] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormData>({ resolver: zodResolver(schema) })

  async function onSubmit(data: FormData) {
    setApiError(null)
    try {
      const res = await authApi.login({
        email: data.email,
        password: data.password,
        rememberMe: data.rememberMe ?? false,
        deviceLabel: defaultDeviceLabel(),
      })
      setAuth(res.token, res.refreshToken, res.user, res.expiresAt)
      navigate('/', { replace: true })
    } catch (err: unknown) {
      const code = (err as { response?: { data?: { code?: string } } })?.response?.data?.code
      setApiError(
        code === 'INVALID_CREDENTIALS'
          ? 'Invalid email or password.'
          : code === 'ACCOUNT_DISABLED'
          ? 'This account has been disabled.'
          : 'Login failed. Please try again.',
      )
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 dark:bg-gray-900 px-4">
      <div className="w-full max-w-sm bg-white dark:bg-gray-800 rounded-2xl shadow-sm border border-gray-100 dark:border-gray-700 p-8">
        <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100 mb-1">
          {import.meta.env.VITE_APP_NAME}
        </h1>
        <p className="text-sm text-gray-500 dark:text-gray-400 mb-6">Sign in to your household</p>

        {apiError && (
          <div className="mb-4 rounded-lg bg-red-50 dark:bg-red-900/30 border border-red-200 dark:border-red-700 px-4 py-3 text-sm text-red-700 dark:text-red-300">
            {apiError}
          </div>
        )}

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div>
            <label htmlFor="login-email" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Email</label>
            <input
              id="login-email"
              {...register('email')}
              type="email"
              autoComplete="email"
              className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-3 py-2 text-sm text-gray-900 dark:text-gray-100 focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
            {errors.email && (
              <p className="mt-1 text-xs text-red-600 dark:text-red-400">{errors.email.message}</p>
            )}
          </div>

          <div>
            <label htmlFor="login-password" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Password</label>
            <input
              id="login-password"
              {...register('password')}
              type="password"
              autoComplete="current-password"
              className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-3 py-2 text-sm text-gray-900 dark:text-gray-100 focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
            {errors.password && (
              <p className="mt-1 text-xs text-red-600 dark:text-red-400">{errors.password.message}</p>
            )}
          </div>

          <label className="flex items-center gap-2 text-sm text-gray-700 dark:text-gray-300">
            <input
              {...register('rememberMe')}
              type="checkbox"
              className="rounded border-gray-300 text-primary-600 focus:ring-primary-500"
            />
            Keep me signed in on this device
          </label>

          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full rounded-lg bg-primary-600 px-4 py-2 text-sm font-semibold text-white hover:bg-primary-700 disabled:opacity-50 transition-colors"
          >
            {isSubmitting ? 'Signing in…' : 'Sign in'}
          </button>
        </form>

        <div className="mt-4 text-center">
          <Link to="/forgot-password" className="text-sm text-primary-600 dark:text-primary-400 hover:underline font-medium">
            Forgot password?
          </Link>
        </div>

        <p className="mt-3 text-center text-sm text-gray-500 dark:text-gray-400">
          No account?{' '}
          <Link to="/register" className="text-primary-600 dark:text-primary-400 hover:underline font-medium">
            Register
          </Link>
        </p>
      </div>
    </div>
  )
}
