import { useTranslation } from 'react-i18next';
import { Card } from '@/components/ui/Card';
import { SignupForm } from '@/components/auth/SignupForm';
import { AuthHero } from '@/components/auth/AuthHero';

export default function SignupPage() {
  const { t } = useTranslation();

  return (
    <div className="grid min-h-screen grid-cols-1 bg-table-900 lg:grid-cols-2">
      <AuthHero />
      <main className="flex items-center justify-center p-6">
        <Card className="w-full max-w-md p-6">
          <header className="mb-6 text-center">
            <h1 className="font-serif text-3xl text-army-white">{t('auth.signup_title')}</h1>
          </header>
          <SignupForm />
        </Card>
      </main>
    </div>
  );
}
