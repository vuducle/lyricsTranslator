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
import { forgotPassword } from '@/lib/api';
import { useTranslation } from '@/context/LanguageContext';
import { useToast } from '@/hooks/useToast';

const schema = z.object({
  email: z.string().email({ message: 'Invalid email address' }),
});

type FormData = z.infer<typeof schema>;

export default function ForgotPassword() {
  const [isSent, setIsSent] = useState(false);
  const { t } = useTranslation();
  const { showToast } = useToast();
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormData>({
    resolver: zodResolver(schema),
  });

  const onSubmit = async (data: FormData) => {
    try {
      await forgotPassword(data.email);
      setIsSent(true);
      showToast(t('forgotPassword.successMessage'), 'success');
    } catch (error: unknown) {
      showToast(t('forgotPassword.error.unexpected'), 'error');
    }
  };

  return (
    <div
      className="flex flex-col items-center justify-center min-h-[calc(100vh-4rem)] p-4 bg-background dark:bg-background"
      style={{ backgroundImage: 'url(/background-pattern.svg)' }}
    >
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle>
            <h1 className="text-2xl text-primary text-center md:text-left">
              {t('forgotPassword.title')}
            </h1>
          </CardTitle>
        </CardHeader>
        <CardContent>
          {isSent ? (
            <p>{t('forgotPassword.successMessage')}</p>
          ) : (
            <form
              onSubmit={handleSubmit(onSubmit)}
              className="space-y-4"
            >
              <div>
                <Label htmlFor="email" className="mb-2">
                  {t('login.emailLabel')}
                </Label>
                <Input
                  id="email"
                  type="email"
                  {...register('email')}
                  placeholder={t('login.emailPlaceholder')}
                />
                {errors.email && (
                  <p className="text-red-500 text-sm mt-1">
                    {errors.email.message}
                  </p>
                )}
              </div>
              <Button type="submit" disabled={isSubmitting}>
                {isSubmitting
                  ? t('forgotPassword.submitButton')
                  : t('forgotPassword.submitButton')}
              </Button>
            </form>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
