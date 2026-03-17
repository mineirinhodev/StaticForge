"use client";

import { ROLES, CONTENT_SCOPES } from "@/schemas/search";

type StaticsFilters = {
  dataCenter: string | null;
  role: string | null;
  language: string | null;
  contentScope: string | null;
  query: string | null;
};

type PlayersFilters = {
  dataCenter: string | null;
  role: string | null;
  jobName: string | null;
  lookingForStatic: boolean | null;
};

type SearchFiltersProps =
  | {
      variant: "statics";
      filters: StaticsFilters;
      onFilterChange: (patch: Partial<StaticsFilters>) => void;
    }
  | {
      variant: "players";
      filters: PlayersFilters;
      onFilterChange: (patch: Partial<PlayersFilters>) => void;
    };

const DATA_CENTERS = [
  "Aether",
  "Crystal",
  "Dynamis",
  "Primal",
  "Chaos",
  "Light",
  "Materia",
  "Meteor",
];

const LANGUAGES = [
  { value: "en", label: "English" },
  { value: "ja", label: "日本語" },
  { value: "de", label: "Deutsch" },
  { value: "fr", label: "Français" },
  { value: "pt", label: "Português" },
];

export function SearchFilters(props: SearchFiltersProps) {
  const { variant, filters, onFilterChange } = props;

  const hasActiveFilters = Object.values(filters).some(
    (v) => v !== null && v !== undefined && v !== "",
  );

  function handleClear() {
    const cleared = Object.fromEntries(
      Object.keys(filters).map((k) => [k, null]),
    );
    (onFilterChange as (patch: Record<string, null>) => void)(cleared);
  }

  return (
    <aside className="flex flex-col gap-4 p-4 border rounded-lg">
      {/* Query search */}
      <div className="flex flex-col gap-1">
        <label htmlFor="search-query" className="text-sm font-medium">
          Busca
        </label>
        <input
          id="search-query"
          type="search"
          role="searchbox"
          placeholder="Nome..."
          value={(filters as StaticsFilters).query ?? ""}
          onChange={(e) =>
            (onFilterChange as (p: { query: string }) => void)({
              query: e.target.value,
            })
          }
          className="border rounded px-3 py-2 text-sm"
        />
      </div>

      {/* Data Center */}
      <div className="flex flex-col gap-1">
        <label htmlFor="filter-datacenter" className="text-sm font-medium">
          Data Center
        </label>
        <select
          id="filter-datacenter"
          value={filters.dataCenter ?? ""}
          onChange={(e) =>
            (onFilterChange as (p: { dataCenter: string | null }) => void)({
              dataCenter: e.target.value || null,
            })
          }
          className="border rounded px-3 py-2 text-sm"
        >
          <option value="">Todos</option>
          {DATA_CENTERS.map((dc) => (
            <option key={dc} value={dc}>
              {dc}
            </option>
          ))}
        </select>
      </div>

      {/* Role */}
      <div className="flex flex-col gap-1">
        <label htmlFor="filter-role" className="text-sm font-medium">
          Função (Role)
        </label>
        <select
          id="filter-role"
          value={filters.role ?? ""}
          onChange={(e) =>
            (onFilterChange as (p: { role: string | null }) => void)({
              role: e.target.value || null,
            })
          }
          className="border rounded px-3 py-2 text-sm"
        >
          <option value="">Todas</option>
          {ROLES.map((r) => (
            <option key={r} value={r}>
              {r}
            </option>
          ))}
        </select>
      </div>

      {/* Content Scope — only for statics */}
      {variant === "statics" && (
        <div className="flex flex-col gap-1">
          <label htmlFor="filter-content" className="text-sm font-medium">
            Conteúdo
          </label>
          <select
            id="filter-content"
            value={(filters as StaticsFilters).contentScope ?? ""}
            onChange={(e) =>
              (
                onFilterChange as (p: {
                  contentScope: string | null;
                }) => void
              )({ contentScope: e.target.value || null })
            }
            className="border rounded px-3 py-2 text-sm"
          >
            <option value="">Todos</option>
            {CONTENT_SCOPES.map((cs) => (
              <option key={cs} value={cs}>
                {cs}
              </option>
            ))}
          </select>
        </div>
      )}

      {/* Language — only for statics */}
      {variant === "statics" && (
        <div className="flex flex-col gap-1">
          <label htmlFor="filter-language" className="text-sm font-medium">
            Idioma
          </label>
          <select
            id="filter-language"
            value={(filters as StaticsFilters).language ?? ""}
            onChange={(e) =>
              (onFilterChange as (p: { language: string | null }) => void)({
                language: e.target.value || null,
              })
            }
            className="border rounded px-3 py-2 text-sm"
          >
            <option value="">Todos</option>
            {LANGUAGES.map((l) => (
              <option key={l.value} value={l.value}>
                {l.label}
              </option>
            ))}
          </select>
        </div>
      )}

      {/* Job Name — only for players */}
      {variant === "players" && (
        <div className="flex flex-col gap-1">
          <label htmlFor="filter-job" className="text-sm font-medium">
            Job
          </label>
          <input
            id="filter-job"
            type="text"
            placeholder="ex: WHM, PLD..."
            value={(filters as PlayersFilters).jobName ?? ""}
            onChange={(e) =>
              (onFilterChange as (p: { jobName: string | null }) => void)({
                jobName: e.target.value || null,
              })
            }
            className="border rounded px-3 py-2 text-sm"
          />
        </div>
      )}

      {/* Clear button */}
      {hasActiveFilters && (
        <button
          onClick={handleClear}
          className="text-sm text-red-500 underline text-left"
          aria-label="Limpar filtros"
        >
          Limpar filtros
        </button>
      )}
    </aside>
  );
}
