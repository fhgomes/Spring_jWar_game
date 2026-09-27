# jWar Frontend

React 18 + TypeScript 5 + Vite 5 + Tailwind CSS 3 SPA for the digital
"War" board game. Builds into the Spring Boot static folder so the
same JAR serves both API and UI.

## Tech stack

- **React 18**, **TypeScript 5 (strict)**, **Vite 5**
- **Tailwind CSS 3** (dark by default; six WAR army color tokens)
- **React Router 6** (data routers + protected routes)
- **TanStack Query 5** (server state)
- **Zustand 4** (auth & toast client state)
- **Firebase Web SDK 10** (auth: email/password + Google)
- **@stomp/stompjs 7** (WebSocket realtime)
- **react-hook-form + zod** (forms & validation)
- **Headless UI**, **lucide-react**, **i18next + react-i18next** (pt-BR)

## Scripts

```bash
npm install
npm run dev         # Vite dev server on :5173, /api & /ws proxied to :8080
npm run build       # writes ../jwar-server/.../resources/static/
npm run preview     # preview the built bundle
npm run lint        # ESLint
npm run typecheck   # tsc --noEmit
npm run format      # Prettier
```

## Environment

Create `.env.local` with your Firebase project config:

```
VITE_FIREBASE_API_KEY=...
VITE_FIREBASE_AUTH_DOMAIN=...
VITE_FIREBASE_PROJECT_ID=...
VITE_FIREBASE_STORAGE_BUCKET=...
VITE_FIREBASE_MESSAGING_SENDER_ID=...
VITE_FIREBASE_APP_ID=...
```

## Structure

See `src/` — pages under `src/pages/`, design-system primitives under
`src/components/ui/`, feature modules under `src/components/{auth,rooms,game}/`.

UI copy lives in `src/locales/pt-BR.json` (Constitution Principle III —
Domain Fidelity, Brazilian Portuguese).

## Production deploy

`npm run build` writes fingerprinted assets directly into
`../jwar-server/jwarsv-sboot/src/main/resources/static/`. The Spring
backend is responsible for a SPA fallback controller that returns
`index.html` for any non-`/api`, non-`/ws`, non-`/actuator` GET that
doesn't contain a `.` (file extension).
