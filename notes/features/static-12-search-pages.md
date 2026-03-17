# STATIC-12: Search Pages (Frontend)

**Branch:** `feature/static-12-search-pages`
**Issues:** #45, #143 (TDD sub-issue)
**Milestone:** Milestone 3: Static Management

## Scope

Two CSR pages with filters and paginated results:
- `/[locale]/(main)/search/statics` — search statics with filters
- `/[locale]/(main)/search/players` — search players with filters

## API Endpoints

| Method | Path | Auth | Response |
|--------|------|------|----------|
| GET | `/api/v1/search/statics` | Yes | `Page<StaticSummaryResponse>` |
| GET | `/api/v1/search/players` | Yes | `Page<CharacterSummaryResponse>` |

### Filter params — statics
`dataCenter`, `server`, `roleNeeded`, `language`, `contentScope`, `status`, `query`, `page`, `size`, `sort`

### Filter params — players
`dataCenter`, `server`, `role`, `jobName`, `language`, `parseTier`, `lookingForStatic`, `query`, `page`, `size`, `sort`

## Frontend Routes
```
src/app/[locale]/(main)/search/
  statics/page.tsx
  players/page.tsx
```

## Components to Build
- `SearchFilters` — filter panel (DataCenter, Role, Language, ContentScope)
- `StaticResultList` — list of StaticCard items
- `PlayerResultList` — list of PlayerCard items
- `Pagination` — page navigator

## Hooks
- `useSearchStatics()` — nuqs URL state + TanStack Query
- `useSearchPlayers()` — nuqs URL state + TanStack Query

## Zod Schemas
- `searchStaticsSchema` — filter validation
- `searchPlayersSchema` — filter validation

## nuqs Fields (URL state)

### useSearchStatics
- `dataCenter` (string)
- `role` (string, maps to roleNeeded)
- `language` (string)
- `contentScope` (string)
- `query` (string)
- `page` (integer, default 0)

### useSearchPlayers
- `dataCenter` (string)
- `role` (string)
- `jobName` (string)
- `lookingForStatic` (boolean)
- `page` (integer, default 0)

## Dependencies to Install
- `nuqs` — URL state management
- `@tanstack/react-query` — server state/cache
- `axios` — HTTP client
- `zod` — schema validation
- `vitest`, `@vitest/coverage-v8` — test runner
- `@testing-library/react`, `@testing-library/user-event` — component testing
- `jsdom` — DOM environment for tests
- `@testing-library/jest-dom` — custom matchers
- `msw` — API mocking for tests
- ShadCN/UI components: Button, Input, Select, Badge, Card, Skeleton

## TDD Progress

### Red → Green → Refactor cycles

- [ ] 1. Zod schemas for search filters
- [ ] 2. `useSearchStatics` hook — builds correct query params
- [ ] 3. `useSearchPlayers` hook — builds correct query params
- [ ] 4. `Pagination` component
- [ ] 5. `SearchFilters` component (statics)
- [ ] 6. `SearchFilters` component (players)
- [ ] 7. `StaticResultList` component
- [ ] 8. `PlayerResultList` component
- [ ] 9. `search/statics/page.tsx` integration
- [ ] 10. `search/players/page.tsx` integration

## Response Shapes

### StaticSummaryResponse
```ts
{
  id: string;
  name: string;
  contentScope: string[];
  language: string;
  status: string;
  maxMembers: number;
  currentMembers: number;
  dataCenter: string;
  server: string;
  openVacancies: number;
  createdAt: string;
}
```

### CharacterSummaryResponse
```ts
{
  id: string;
  name: string;
  server: string;
  dataCenter: string;
  isVerified: boolean;
  avatarUrl: string;
  mainJobName: string;
}
```

### Paginated response shape
```ts
{
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
}
```
