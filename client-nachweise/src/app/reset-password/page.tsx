import ResetPassword from '@/features/auth/ResetPassword';
import { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Reset Password',
};

export default function ResetPasswordPage() {
  return <ResetPassword />;
}
