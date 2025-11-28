import ForgotPassword from '@/features/auth/ForgotPassword';
import { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Forgot Password',
};

export default function ForgotPasswordPage() {
  return <ForgotPassword />;
}
