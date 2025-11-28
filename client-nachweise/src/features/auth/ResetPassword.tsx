'use client';

import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import { resetPassword } from '@/lib/api';
import { useSearchParams, useRouter } from 'next/navigation';
import { useTranslation } from '@/context/LanguageContext';
import { useToast } from '@/hooks/useToast';

const ResetPasswordSchema = (t: (key: string) => string) =>
  z
    .object({
      password: z
        .string()
        .min(8, t('resetPassword.error.passwordTooShort')),
      confirmPassword: z.string(),
    })
    .refine((data) => data.password === data.confirmPassword, {
      message: t('resetPassword.error.mismatch'),
      path: ['confirmPassword'],
    });

type FormData = z.infer<ReturnType<typeof ResetPasswordSchema>>;

export default function ResetPassword() {
  const searchParams = useSearchParams();
  const token = searchParams.get('token');
  const { t } = useTranslation();
  const { showToast } = useToast();
  const router = useRouter();

  const [isReset, setIsReset] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormData>({
    resolver: zodResolver(ResetPasswordSchema(t)),
  });

  const onSubmit = async (data: FormData) => {
    if (!token) {
      showToast(t('resetPassword.error.invalidToken'), 'error');
      return;
    }
    try {
      await resetPassword(token, data.password);
      setIsReset(true);
      showToast(t('resetPassword.successMessage'), 'success');
    } catch (error: unknown) {
      showToast(t('resetPassword.error.unexpected'), 'error');
    }
  };

  return (
    <div
      className="flex flex-col items-center justify-center min-h-[calc(100vh-4rem)] p-4 bg-background dark:bg-background"
      style={{ backgroundImage: 'url(/background-pattern.svg)' }}
    >
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle>{t('resetPassword.title')}</CardTitle>
        </CardHeader>
        <CardContent>
          {isReset ? (
            <>
              <p>{t('resetPassword.successMessage')}</p>
              <div className="mt-4">
                <Button onClick={() => router.push('/login')}>
                  Login
                </Button>
              </div>
            </>
          ) : (
            <form
              onSubmit={handleSubmit(onSubmit)}
              className="space-y-4"
            >
              <div>
                <Label htmlFor="password">
                  {t('resetPassword.newPasswordLabel')}
                </Label>
                <Input
                  id="password"
                  type="password"
                  {...register('password')}
                  placeholder={t(
                    'resetPassword.newPasswordPlaceholder'
                  )}
                />
                {errors.password && (
                  <p className="text-red-500 text-sm mt-1">
                    {errors.password.message}
                  </p>
                )}
              </div>
              <div>
                <Label htmlFor="confirmPassword">
                  {t('resetPassword.confirmPasswordLabel')}
                </Label>
                <Input
                  id="confirmPassword"
                  type="password"
                  {...register('confirmPassword')}
                  placeholder={t(
                    'resetPassword.confirmPasswordPlaceholder'
                  )}
                />
                {errors.confirmPassword && (
                  <p className="text-red-500 text-sm mt-1">
                    {errors.confirmPassword.message}
                  </p>
                )}
              </div>
              <Button type="submit" disabled={isSubmitting || !token}>
                {isSubmitting
                  ? t('resetPassword.submitButton')
                  : t('resetPassword.submitButton')}
              </Button>
            </form>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
