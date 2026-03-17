"use client";

import type { StaticSummaryResponse } from "@/types/api";

interface StaticResultListProps {
  statics: StaticSummaryResponse[];
  isLoading?: boolean;
}

function StaticSkeleton() {
  return (
    <div
      data-testid="static-skeleton"
      className="animate-pulse flex flex-col gap-3"
    >
      {Array.from({ length: 5 }).map((_, i) => (
        <div key={i} className="h-20 bg-gray-200 rounded-lg" />
      ))}
    </div>
  );
}

function StaticCard({ static: s }: { static: StaticSummaryResponse }) {
  return (
    <article className="border rounded-lg p-4 flex flex-col gap-2">
      <h3 className="font-semibold text-lg">{s.name}</h3>
      <div className="flex flex-wrap gap-2 text-sm text-gray-600">
        <span>{s.dataCenter}</span>
        {s.server && <span>· {s.server}</span>}
        <span>· {s.currentMembers}/{s.maxMembers} membros</span>
        <span>· {s.openVacancies} vaga(s)</span>
      </div>
      <div className="flex flex-wrap gap-1">
        {s.contentScope.map((scope) => (
          <span
            key={scope}
            className="px-2 py-0.5 bg-blue-100 text-blue-800 rounded text-xs uppercase"
          >
            {scope}
          </span>
        ))}
      </div>
    </article>
  );
}

export function StaticResultList({
  statics,
  isLoading = false,
}: StaticResultListProps) {
  if (isLoading) return <StaticSkeleton />;

  if (statics.length === 0) {
    return (
      <p className="text-center text-gray-500 py-8">
        Nenhuma static encontrada.
      </p>
    );
  }

  return (
    <ul className="flex flex-col gap-3" aria-label="Lista de statics">
      {statics.map((s) => (
        <li key={s.id}>
          <StaticCard static={s} />
        </li>
      ))}
    </ul>
  );
}
