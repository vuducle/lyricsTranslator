'use client';

import { ThemeProvider } from './ThemeProvider';
import StoreProvider from './StoreProvider';
import { LanguageProvider } from '@/context/LanguageContext';
import { CookieBanner } from '@/components/ui/CookieBanner';
import { Footer } from '@/components/ui/Footer';

export function Providers({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <ThemeProvider
      attribute="class"
      defaultTheme="system"
      enableSystem
    >
      <StoreProvider>
        <LanguageProvider>
          <div className="flex flex-col min-h-screen">
            {children}
            <CookieBanner />
            <Footer></Footer>
          </div>
        </LanguageProvider>
      </StoreProvider>
    </ThemeProvider>
  );
}
