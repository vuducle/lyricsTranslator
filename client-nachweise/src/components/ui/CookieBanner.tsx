'use client';

import React, { useState, useEffect, use } from 'react';
import { Button } from './button';
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from './card';
import { useTranslation } from '@/context/LanguageContext';

export function CookieBanner() {
  const { t } = useTranslation();
  const [isVisible, setIsVisible] = useState<boolean>(false);
  const cookieConsent: string | null =
    'CookieConsentDenisKunzLiebtJava';

  useEffect(() => {
    const consent = localStorage.getItem(cookieConsent);
    if (!consent) {
      setIsVisible(true);
    }
  }, []);

  const handleAccept = () => {
    localStorage.setItem(cookieConsent, 'true');
    setIsVisible(false);
  };

  if (!isVisible) {
    return null;
  }

  return (
    <div className="fixed bottom-0 left-0 right-0 z-50 p-4">
      <Card className="mx-auto max-w-lg shadow-lg">
        <CardHeader>
          <CardTitle>{t('cookieBanner.title')}</CardTitle>
          <CardDescription>
            {t('cookieBanner.description')}
          </CardDescription>
        </CardHeader>
        <CardFooter className="flex justify-end">
          <Button onClick={handleAccept}>
            {t('cookieBanner.acceptButton')}
          </Button>
        </CardFooter>
      </Card>
    </div>
  );
}
