'use client';

import { Button } from '@/components/ui/button';
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { LanguageSwitcher } from '@/components/ui/LanguageSwitcher';
import { useTranslation } from '@/context/LanguageContext';
import { useAppDispatch } from '@/store';
import { setUser } from '@/store/slices/userSlice';
import api from '@/lib/api';
import { useRouter } from 'next/navigation';
import { useState } from 'react';
import axios from 'axios';
import Image from 'next/image';
import Link from 'next/link';

export default function LoginForm() {
  const [email, setEmail] = useState<string>('');
  const [password, setPassword] = useState<string>('');
  const [error, setError] = useState<string>('');
  const dispatch = useAppDispatch();
  const router = useRouter();
  const { t } = useTranslation();

  const handleLogin = async () => {
    try {
      const response = await api.post('/api/auth/login', {
        email,
        password,
      });
      const { accessToken, refreshToken, id, name } = response.data;
      dispatch(
        setUser({
          token: accessToken,
          refreshToken,
          id,
          email,
          name,
          isLoggedIn: true,
        })
      );
      router.push('/');
    } catch (err) {
      if (axios.isAxiosError(err) && err.response) {
        if (err.response.status === 423) {
          setError(t('login.error.locked'));
        } else {
          setError(t('login.error.invalid'));
        }
      } else {
        setError(t('login.error.unexpected'));
      }
    }
  };

  return (
    <div
      className="flex flex-col items-center justify-center min-h-[calc(100vh-4rem)] p-4 bg-background dark:bg-background"
      style={{ backgroundImage: 'url(/background-pattern.svg)' }}
    >
      <div className="absolute top-4 right-4">
        <LanguageSwitcher />
      </div>
      <div className="grid grid-cols-1 md:grid-cols-2 gap-8 max-w-4xl w-full">
        <div className="flex flex-col justify-center items-center md:items-start p-8">
          <Image
            src="/logo.png"
            alt="NachweisWelt Logo"
            width={200}
            height={50}
          />
          <h1 className="text-3xl md:text-4xl font-bold mt-4 text-foreground text-center md:text-left">
            {t('loginPage.mainTitle')}
          </h1>
          <p className="text-lg mt-2 text-muted-foreground text-center md:text-left">
            {t('loginPage.subtitle')}
          </p>
        </div>
        <div className="flex items-center justify-center">
          <Card className="w-full max-w-sm border border-border bg-card/20 backdrop-blur-md shadow-lg">
            <CardHeader>
              <CardTitle className="text-2xl text-primary text-center md:text-left">
                {t('login.title')}
              </CardTitle>
              <CardDescription className="text-center md:text-left">
                {t('login.description')}
              </CardDescription>
            </CardHeader>
            <CardContent className="grid gap-4">
              <div className="grid gap-2">
                <Label htmlFor="email">{t('login.emailLabel')}</Label>
                <Input
                  id="email"
                  type="email"
                  placeholder={t('login.emailPlaceholder')}
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                />
              </div>
              <div className="grid gap-2">
                <Label htmlFor="password">
                  {t('login.passwordLabel')}
                </Label>
                <Input
                  id="password"
                  type="password"
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                />
              </div>
              <div className="text-sm">
                <Link
                  href="/forgot-password"
                  className="font-medium text-primary hover:underline"
                  passHref
                >
                  {t('login.forgotPassword')}
                </Link>
              </div>
              {error && (
                <p className="text-destructive text-sm">{error}</p>
              )}
            </CardContent>
            <CardFooter>
              <Button className="w-full" onClick={handleLogin}>
                {t('login.submitButton')}
              </Button>
            </CardFooter>
          </Card>
        </div>
      </div>
    </div>
  );
}
