"use client";

import { useSearchStatics } from "@/hooks/use-search";
import { SearchFilters } from "@/components/search/SearchFilters";
import { StaticResultList } from "@/components/search/StaticResultList";
import { Pagination } from "@/components/search/Pagination";

export default function StaticSearchPage() {
  const { data, isLoading, filters, setFilters } = useSearchStatics();

  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="text-2xl font-bold mb-6">Buscar Statics</h1>

      <div className="flex flex-col lg:flex-row gap-6">
        {/* Filters sidebar */}
        <aside className="lg:w-64 shrink-0">
          <SearchFilters
            variant="statics"
            filters={{
              dataCenter: filters.dataCenter,
              role: filters.role,
              language: filters.language,
              contentScope: filters.contentScope,
              query: filters.query,
            }}
            onFilterChange={(patch) => setFilters({ ...patch, page: 0 })}
          />
        </aside>

        {/* Results */}
        <main className="flex-1 flex flex-col gap-4">
          {data && !isLoading && (
            <p className="text-sm text-gray-600">
              {data.totalElements} resultado{data.totalElements !== 1 ? "s" : ""}
            </p>
          )}

          <StaticResultList
            statics={data?.content ?? []}
            isLoading={isLoading}
          />

          {data && data.totalPages > 1 && (
            <Pagination
              page={filters.page}
              totalPages={data.totalPages}
              hasNext={data.hasNext}
              onPageChange={(p) => setFilters({ page: p })}
            />
          )}
        </main>
      </div>
    </div>
  );
}
