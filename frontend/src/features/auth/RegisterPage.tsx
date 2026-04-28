import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useNavigate, Link, useSearchParams } from 'react-router-dom'
import { useEffect, useState } from 'react'
import { authApi } from '../../api/authApi'
import { useAuthStore } from '../../lib/authStore'
import type { InvitationResponse } from '../../types/auth'

const schema = z.object({
  email: z.string().min(1, 'Email is required').email('Invalid email'),
  displayName: z.string().min(1, 'Name is required').max(100),
  password: z
    .string()
    .min(8, 'At least 8 characters')
    .regex(/[A-Z]/, 'Must include an uppercase letter')
    .regex(/[a-z]/, 'Must include a lowercase letter')
    .regex(/\d/, 'Must include a digit'),
  inviteToken: z.string().optional(),
})

type FormData = z.infer<typeof schema>

export default function RegisterPage() {
  const navigate = useNavigate()
  const setAuth = useAuthStore((s) => s.setAuth)
  const [searchParams] = useSearchParams()
  const inviteFromUrl = searchParams.get('invite') ?? ''
  const [apiError, setApiError] = useState<string | null>(null)
  const [invite, setInvite] = useState<InvitationResponse | null>(null)
  const [inviteError, setInviteError] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: { inviteToken: inviteFromUrl },
  })

  useEffect(() => {
    if (!inviteFromUrl) return
    authApi
      .verifyInvitation(inviteFromUrl)
      .then((inv) => {
        setInvite(inv)
        setValue('email', inv.email)
      })
      .catch((err: unknown) => {
        const code = (err as { response?: { data?: { code?: string } } })?.response?.data?.code
        setInviteError(
          code === 'INVITATION_USED'
            ? 'This invitation has already been used.'
            : code === 'INVITATION_EXPIRED'
            ? 'This invitation has expired. Ask an admin for a new link.'
            : 'Invalid invitation token.',
        )
      })
  }, [inviteFromUrl, setValue])

  async function onSubmit(data: FormData) {
    setApiError(null)
    try {
      const res = await authApi.register({
        email: data.email,
        password: data.password,
        displayName: data.displayName,
        inviteToken: data.inviteToken || undefined,
      })
      setAuth(res.token, res.refreshToken, res.user, res.expiresAt)
      navigate('/', { replace: true })
    } catch (err: unknown) {
      const code = (err as { response?: { data?: { code?: string } } })?.response?.data?.code
      setApiError(
        code === 'EMAIL_ALREADY_EXISTS'
          ? 'An account with this email already exists.'
          : code === 'INVITATION_REQUIRED'
          ? 'Registration is invite-only. Please paste an invitation token.'
          : code === 'INVITATION_EMAIL_MISMATCH'
          ? 'This invitation was issued for a different email.'
          : 'Registration failed. Please try again.',
      )
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 dark:bg-gray-900 px-4">
      <div className="w-full max-w-sm bg-white dark:bg-gray-800 rounded-2xl shadow-sm border border-gray-100 dark:border-gray-700 p-8">
        <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100 mb-1">Create account</h1>
        <p className="text-sm text-gray-500 dark:text-gray-400 mb-6">
          {import.meta.env.VITE_APP_NAME} — invitation required (first account becomes admin)
        </p>

        {invite && !inviteError && (
          <div className="mb-4 rounded-lg bg-green-50 dark:bg-green-900/30 border border-green-200 dark:border-green-700 px-4 py-3 text-sm text-green-700 dark:text-green-300">
            Invitation valid for {invite.email}.
          </div>
        )}

        {(apiError || inviteError) && (
          <div className="mb-4 rounded-lg bg-red-50 dark:bg-red-900/30 border border-red-200 dark:border-red-700 px-4 py-3 text-sm text-red-700 dark:text-red-300">
            {apiError ?? inviteError}
          </div>
        )}

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              Invitation token <span className="text-gray-400">(optional for first user)</span>
            </label>
            <input
              {...register('inviteToken')}
              type="text"
              className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-3 py-2 text-sm text-gray-900 dark:text-gray-100 focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Email</label>
            <input
              {...register('email')}
              type="email"
              autoComplete="email"
              readOnly={!!invite}
              className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-3 py-2 text-sm text-gray-900 dark:text-gray-100 focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
            {errors.email && (
              <p className="mt-1 text-xs text-red-600 dark:text-red-400">{errors.email.message}</p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Display name</label>
            <input
              {...register('displayName')}
              type="text"
              autoComplete="name"
              className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-3 py-2 text-sm text-gray-900 dark:text-gray-100 focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
            {errors.displayName && (
              <p className="mt-1 text-xs text-red-600 dark:text-red-400">{errors.displayName.message}</p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Password</label>
            <input
              {...register('password')}
              type="password"
              autoComplete="new-password"
              className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-3 py-2 text-sm text-gray-900 dark:text-gray-100 focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
            {errors.password && (
              <p className="mt-1 text-xs text-red-600 dark:text-red-400">{errors.password.message}</p>
            )}
          </div>

          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full rounded-lg bg-primary-600 px-4 py-2 text-sm font-semibold text-white hover:bg-primary-700 disabled:opacity-50 transition-colors"
          >
            {isSubmitting ? 'Creating account…' : 'Create account'}
          </button>
        </form>

        <p className="mt-6 text-center text-sm text-gray-500 dark:text-gray-400">
          Already have an account?{' '}
          <Link to="/login" className="text-primary-600 hover:underline font-medium">
            Sign in
          </Link>
        </p>
      </div>
    </div>
  )
}
