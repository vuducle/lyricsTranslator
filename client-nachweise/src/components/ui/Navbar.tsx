import {
  Home,
  Plus,
  Book,
  User as UserIcon,
  Menu,
  X,
  Briefcase,
  GraduationCap,
} from 'lucide-react';
import Link from 'next/link';
import { useAppDispatch } from '@/store';
import { clearUser, User } from '@/store/slices/userSlice';
import { Button } from './button';
import { ThemeToggleButton } from './ThemeToggleButton';
import { Logo } from './Logo';
import { LanguageSwitcher } from './LanguageSwitcher';
import { useState } from 'react';

export function Navbar({ user }: { user: User }) {
  const [isOpen, setIsOpen] = useState(false);
  const isAuthenticated = user.isLoggedIn;
  const dispatch = useAppDispatch();

  const handleLogout = () => {
    dispatch(clearUser());
  };

  const navLinks = [
    { href: '/', label: 'Home', icon: Home },
    {
      href: '/erstellen',
      label: 'Erstellen',
      icon: Plus,
      userOnly: true,
    },
    {
      href: '/user-erstellen',
      label: 'User Erstellen',
      icon: Plus,
      adminOnly: true,
    },
    {
      href: '/nachweise-anschauen',
      label: 'Nachweise anschauen',
      icon: Book,
    },
    { href: '/profil', label: 'Profil', icon: UserIcon },
  ];

  const isAdmin = user.roles.includes('ROLE_ADMIN');
  const isUser = user.roles.includes('ROLE_USER');
  const RoleIcon = isAdmin ? Briefcase : GraduationCap;

  return (
    <nav className="fixed top-0 left-0 right-0 z-50 bg-card dark:bg-card border-b-chart-1 shadow-md">
      <div className="container mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          <div className="flex items-center">
            <Logo />
          </div>
          <div className="hidden md:block">
            <div className="ml-10 flex items-baseline space-x-4">
              {navLinks.map((link) => {
                if (link.adminOnly && !isAdmin) {
                  return null;
                }
                if (link.userOnly && !isUser) {
                  return null;
                }
                return (
                  <Link
                    key={link.href}
                    href={link.href}
                    className="text-foreground hover:bg-accent px-3 py-2 rounded-md text-sm font-medium flex items-center"
                  >
                    <link.icon className="mr-2 h-5 w-5" />
                    {link.label}
                  </Link>
                );
              })}
            </div>
          </div>
          <div className="hidden md:block">
            <div className="flex items-center space-x-4">
              <RoleIcon className="h-6 w-6 text-foreground" />
              <LanguageSwitcher />
              <ThemeToggleButton />
              <Button onClick={handleLogout}>Logout</Button>
            </div>
          </div>
          <div className="-mr-2 flex md:hidden">
            <button
              onClick={() => setIsOpen(!isOpen)}
              className="inline-flex items-center justify-center p-2 rounded-md text-foreground hover:bg-accent focus:outline-none"
            >
              {isOpen ? <X size={24} /> : <Menu size={24} />}
            </button>
          </div>
        </div>
      </div>
      {isOpen && (
        <div className="md:hidden">
          <div className="px-2 pt-2 pb-3 space-y-1 sm:px-3">
            {navLinks.map((link) => {
              if (link.adminOnly && !isAdmin) {
                return null;
              }
              if (link.userOnly && !isUser) {
                return null;
              }
              return (
                <Link
                  key={link.href}
                  href={link.href}
                  className="text-foreground hover:bg-accent block px-3 py-2 rounded-md text-base font-medium flex items-center"
                >
                  <link.icon className="mr-2 h-5 w-5" />
                  {link.label}
                </Link>
              );
            })}
          </div>
          <div className="pt-4 pb-3 border-t border-gray-700">
            <div className="flex items-center px-5 space-x-4">
              <RoleIcon className="h-6 w-6 text-foreground" />
              <LanguageSwitcher />
              <ThemeToggleButton />
              {isAuthenticated ? (
                <Button onClick={handleLogout} className="w-full">
                  Logout
                </Button>
              ) : (
                <Button asChild className="w-full">
                  <Link href="/login">Login</Link>
                </Button>
              )}
            </div>
          </div>
        </div>
      )}
    </nav>
  );
}
