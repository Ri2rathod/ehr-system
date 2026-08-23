'use client';

import React from 'react';
import { Lock, Mail, ChevronRight, Activity, ShieldCheck, Loader2 } from 'lucide-react';
import Link from 'next/link';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation } from '@tanstack/react-query';
import { useRouter } from 'next/navigation';
import { loginSchema, LoginFormValues } from '../schemas/login.schema';
import { authApi } from '../api/auth.api';
import { useAuthStore } from '../store/auth.store';
import { Alert, AlertDescription } from '@/components/ui/alert';

export default function LoginPage() {
  const router = useRouter();
  const setUser = useAuthStore((state) => state.setUser);

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
  });

  const loginMutation = useMutation({
    mutationFn: authApi.login,
    onSuccess: (data) => {
      // The backend sets the httpOnly cookies automatically.
      // We explicitly map the user data for state hydration.
      setUser({
        userId: data.userId,
        userUuid: data.userUuid,
        email: data.email,
        username: data.username,
        firstName: data.firstName,
        lastName: data.lastName,
        displayName: data.displayName,
        roles: data.roles,
        lastLoginAt: data.lastLoginAt,
      });
      const redirectAfterLogin =
        typeof window !== 'undefined' ? sessionStorage.getItem('redirectAfterLogin') : null;
      if (redirectAfterLogin) {
        sessionStorage.removeItem('redirectAfterLogin');
        router.push(redirectAfterLogin);
        return;
      }
      router.push('/dashboard');
    },
    onError: (error: any) => {
      console.log(error);
      const serverMessage =
        error.response?.data?.message || 'Login failed. Please check your clinical credentials.';
      setError('root', {
        type: 'server',
        message: serverMessage,
      });
      setError('email', {
        type: 'server',
        message: '',
      });
      setError('password', {
        type: 'server',
        message: '',
      });
    },
  });

  const onSubmit = (values: LoginFormValues) => {
    loginMutation.mutate(values);
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-surface p-4">
      <div className="flex w-full max-w-5xl overflow-hidden rounded-xl border border-outline-variant bg-surface-container-lowest shadow-xl">
        {/* Left Side - Hero/Info */}
        <div className="hidden w-1/2 bg-primary p-12 text-on-primary lg:flex lg:flex-col lg:justify-between">
          <div>
            <div className="mb-8 flex items-center gap-2">
              <Activity className="h-8 w-8" />
              <span className="text-2xl font-bold tracking-tight">ClinicalOS</span>
            </div>
            <h1 className="display-lg mb-6 text-4xl font-bold leading-tight">
              Precision in Practice.
            </h1>
            <p className="body-base mb-8 text-lg opacity-90">
              Securely access longitudinal patient records, real-time diagnostic imaging, and integrated pharmacy modules. Designed to minimize cognitive load and ensure data safety first.
            </p>
          </div>
          <div className="flex items-center gap-4 rounded-lg bg-white/10 p-4">
            <ShieldCheck className="h-10 w-10 text-white" />
            <div className="text-sm">
              <p className="font-bold">MFA Required</p>
              <p className="opacity-80">Secondary verification is mandatory for all clinical staff.</p>
            </div>
          </div>
        </div>

        {/* Right Side - Login Form */}
        <div className="flex w-full flex-col justify-center p-8 lg:w-1/2 lg:p-16">
          <div className="mb-10 lg:hidden">
            <div className="mb-4 flex items-center gap-2 text-primary">
              <Activity className="h-6 w-6" />
              <span className="text-xl font-bold tracking-tight">ClinicalOS</span>
            </div>
          </div>

          <div className="mb-8">
            <h2 className="headline-md mb-2 text-2xl font-bold text-on-surface">Provider Sign In</h2>
            <p className="body-base text-on-surface-variant">
              Enter your clinical credentials to access the secure workspace.
            </p>
          </div>

          <form className="space-y-6" onSubmit={handleSubmit(onSubmit)}>
            {errors.root?.message && (
              <Alert variant="destructive">
                <AlertDescription>{errors.root.message}</AlertDescription>
              </Alert>
            )}

            <div className="space-y-2">
              <label className="label-caps block text-xs font-bold uppercase tracking-wider text-on-surface-variant">
                Email Address
              </label>
              <div className="relative">
                <Mail className="absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-outline" />
                <input
                  {...register('email')}
                  type="email"
                  placeholder="name@hospital.org"
                  className={`w-full rounded-md border bg-surface-container-low py-3 pl-10 pr-4 outline-none transition-all focus:border-primary focus:ring-2 focus:ring-primary/20 ${
                    errors.email ? 'border-error' : 'border-outline-variant'
                  }`}
                />
              </div>
              {errors.email?.message && (
                <p className="text-xs font-bold text-error mt-1">{errors.email.message}</p>
              )}
            </div>

            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <label className="label-caps block text-xs font-bold uppercase tracking-wider text-on-surface-variant">
                  Password
                </label>
                <Link href="#" className="text-xs font-bold text-primary hover:underline">
                  Forgot Password?
                </Link>
              </div>
              <div className="relative">
                <Lock className="absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-outline" />
                <input
                  {...register('password')}
                  type="password"
                  placeholder="••••••••"
                  className={`w-full rounded-md border bg-surface-container-low py-3 pl-10 pr-4 outline-none transition-all focus:border-primary focus:ring-2 focus:ring-primary/20 ${
                    errors.password ? 'border-error' : 'border-outline-variant'
                  }`}
                />
              </div>
              {errors.password?.message && (
                <p className="text-xs font-bold text-error mt-1">{errors.password.message}</p>
              )}
            </div>

            <button
              type="submit"
              disabled={loginMutation.isPending}
              className="flex w-full items-center justify-center gap-2 rounded-md bg-primary py-3 font-bold text-on-primary transition-all hover:bg-primary/90 disabled:opacity-50"
            >
              {loginMutation.isPending ? (
                <>
                  <Loader2 className="h-5 w-5 animate-spin" />
                  Authenticating...
                </>
              ) : (
                <>
                  Sign In to Workspace
                  <ChevronRight className="h-5 w-5" />
                </>
              )}
            </button>
          </form>

          <div className="mt-8 text-center text-sm">
            <p className="text-on-surface-variant">
              New to ClinicalOS?{' '}
              <Link href="/register" className="font-bold text-primary hover:underline">
                Create clinical profile
              </Link>
            </p>
          </div>

          <div className="mt-12 flex flex-wrap justify-center gap-x-6 gap-y-2 text-xs font-medium text-outline">
            <Link href="#" className="hover:text-on-surface-variant">Privacy Policy</Link>
            <Link href="#" className="hover:text-on-surface-variant">Terms of Service</Link>
            <Link href="#" className="hover:text-on-surface-variant">IT Helpdesk</Link>
          </div>
        </div>
      </div>

    </div>
  );
}
