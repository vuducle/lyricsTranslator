'use client';

import { Button } from '@/components/ui/button';
import { LanguageSwitcher } from '@/components/ui/LanguageSwitcher';
import { ThemeToggleButton } from '@/components/ui/ThemeToggleButton';
import { useTranslation } from '@/context/LanguageContext';
import { useAppDispatch, useAppSelector } from '@/store';
import { clearUser, selectUser } from '@/store/slices/userSlice';
import { useRouter } from 'next/navigation';
import { useEffect } from 'react';

export default function Home() {
  const user = useAppSelector(selectUser);
  const dispatch = useAppDispatch();
  const router = useRouter();
  const { t } = useTranslation();

  useEffect(() => {
    if (!user.isLoggedIn) {
      router.push('/login');
    }
  }, [user.isLoggedIn, router]);

  const handleLogout = () => {
    dispatch(clearUser());
    router.push('/login');
  };

  if (!user.isLoggedIn) {
    return null;
  }

  return (
    <div className="flex flex-col items-center justify-center min-h-[calc(100vh-4rem)] p-4 bg-background dark:bg-zinc-800">
      <header className="flex w-full items-center justify-end p-4 gap-2"></header>
      <main className="flex grow w-full max-w-3xl flex-col items-center justify-center bg-white dark:bg-zinc-800">
        <div className="flex flex-col items-center gap-6 text-center sm:items-start sm:text-left">
          <h1 className="max-w-xs text-3xl font-semibold leading-10 tracking-tight text-primary">
            {t('home.welcome').replace('{name}', user.name ?? '')}
          </h1>
          <Button
            variant="default"
            className="bg-primary text-primary-foreground hover:bg-primary/90"
            onClick={handleLogout}
          >
            {t('home.logout')}
          </Button>
        </div>
      </main>
    </div>
  );
}
