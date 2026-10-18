import { Link } from 'react-router-dom';
import { Container } from '@/components/ui/Container';
import { UserMenu } from './UserMenu';

export function TopNav() {
  return (
    <header className="sticky top-0 z-30 border-b border-table-700 bg-table-900/95 backdrop-blur">
      <Container size="full">
        <div className="flex h-14 items-center justify-between">
          <Link to="/lobby" className="flex items-center gap-2 font-serif text-xl text-army-white">
            <span className="text-army-red">⚔</span>
            <span>jWar</span>
          </Link>
          <UserMenu />
        </div>
      </Container>
    </header>
  );
}
