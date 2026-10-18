import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { Button } from '@/components/ui/Button';

export default function NotFoundPage() {
  const { t } = useTranslation();
  return (
    <div className="grid min-h-screen place-items-center bg-table-900 p-6 text-center">
      <div className="space-y-4">
        <h1 className="font-serif text-5xl text-army-white">404</h1>
        <p className="text-lg text-army-white">{t('not_found.title')}</p>
        <p className="text-sm text-table-200">{t('not_found.description')}</p>
        <Link to="/">
          <Button size="lg">{t('not_found.go_home')}</Button>
        </Link>
      </div>
    </div>
  );
}
