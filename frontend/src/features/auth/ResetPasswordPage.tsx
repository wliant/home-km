import { useState } from 'react'
import { useSearchParams, useNavigate, Link } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { apiClient } from '../../api/client'

const schema = z.object({
  newPassword: z
    .string()
    .min(8, 'At least 8 characters')
    .regex(/[A-Z]/, 'Must include an uppercase letter')
    .regex(/[a-z]/, 'Must include a lowercase letter')
    .regex(/\d/, 'Must include a digit'),
  confirmPassword: z.string(),
}).refine(d => d.newPassword === d.confirmPassword, {
  message: 'Passwords do not match',
  path: ['confirmPassword'],
})

type FormData = z.infer<typeof schema>

export default function ResetPasswordPage() {
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token')
  const navigate = useNavigate()
  const [apiError, setApiError] = useState<string | null>(null)
  const [success, setSuccess] = useState(false)

  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<FormData>({
    resolver: zodResolver(schema),
  })

  async function onSubmit(data: FormData) {
    setApiError(null)
    try {
      await apiClient.post('/auth/password-reset/confirm', {
        token,
        newPassword: data.newPassword,
      })
      setSuccess(true)
      setTimeout(() => navigate('/login', { replace: true }), 2000)
    } catch (err: unknown) {
      const code = (err as { response?: { data?: { code?: string } } })?.response?.data?.code
      setApiError(
        code === 'RESET_TOKEN_EXPIRED' ? 'This reset link has expired. Please request a new one.'
          : code === 'RESET_TOKEN_ALREADY_USED' ? 'This reset link has already been used.'
          : 'Password reset failed. Please try again.',
      )
    }
  }

  if (!token) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50 dark:bg-gray-900 px-4">
        <div className="text-center">
          <p className="text-gray-500 dark:text-gray-400 mb-4">Invalid or missing reset token.</p>
          <Link to="/forgot-password" className="text-primary-600 dark:text-primary-400 hover:underline font-medium">
            Request a new reset link
          </Link>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 dark:bg-gray-900 px-4">
      <div className="w-full max-w-sm bg-white dark:bg-gray-800 rounded-2xl shadow-sm border border-gray-100 dark:border-gray-700 p-8">
        <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100 mb-1">Set new password</h1>
        <p className="text-sm text-gray-500 dark:text-gray-400 mb-6">Choose a strong password.</p>

        {success && (
          <div className="mb-4 rounded-lg bg-green-50 dark:bg-green-900/30 border border-green-200 dark:border-green-700 px-4 py-3 text-sm text-green-700 dark:text-green-300">
            Password updated! Redirecting to login…
          </div>
        )}

        {apiError && (
          <div className="mb-4 rounded-lg bg-red-50 dark:bg-red-900/30 border border-red-200 dark:border-red-700 px-4 py-3 text-sm text-red-700 dark:text-red-300">
            {apiError}
          </div>
        )}

        {!success && (
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <div>
              <label htmlFor="reset-new-password" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">New password</label>
              <input
                id="reset-new-password"
                {...register('newPassword')}
                type="password"
                autoComplete="new-password"
                className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-3 py-2 text-sm text-gray-900 dark:text-gray-100 focus:outline-none focus:ring-2 focus:ring-primary-500"
              />
              {errors.newPassword && (
                <p className="mt-1 text-xs text-red-600 dark:text-red-400">{errors.newPassword.message}</p>
              )}
            </div>

            <div>
              <label htmlFor="reset-confirm-password" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Confirm password</label>
              <input
                id="reset-confirm-password"
                {...register('confirmPassword')}
                type="password"
                autoComplete="new-password"
                className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-3 py-2 text-sm text-gray-900 dark:text-gray-100 focus:outline-none focus:ring-2 focus:ring-primary-500"
              />
              {errors.confirmPassword && (
                <p className="mt-1 text-xs text-red-600 dark:text-red-400">{errors.confirmPassword.message}</p>
              )}
            </div>

            <button
              type="submit"
              disabled={isSubmitting}
              className="w-full rounded-lg bg-primary-600 px-4 py-2 text-sm font-semibold text-white hover:bg-primary-700 disabled:opacity-50 transition-colors"
            >
              {isSubmitting ? 'Resetting…' : 'Reset password'}
            </button>
          </form>
        )}

        <p className="mt-6 text-center text-sm text-gray-500 dark:text-gray-400">
          <Link to="/login" className="text-primary-600 dark:text-primary-400 hover:underline font-medium">
            Back to sign in
          </Link>
        </p>
      </div>
    </div>
  )
}
