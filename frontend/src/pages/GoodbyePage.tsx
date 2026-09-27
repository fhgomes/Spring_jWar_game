import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { Button } from '@/components/ui/Button';

export default function GoodbyePage() {
  const { t } = useTranslation();
  return (
    <div className="grid min-h-screen place-items-center bg-table-900 p-6 text-center">
      <div className="space-y-4">
        <h1 className="font-serif text-3xl text-army-white">{t('auth.goodbye')}</h1>
        <Link to="/login">
          <Button>{t('auth.sign_in')}</Button>
        </Link>
      </div>
    </div>
  );
}
