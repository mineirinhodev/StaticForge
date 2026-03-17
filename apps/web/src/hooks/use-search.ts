import { useQuery } from "@tanstack/react-query";
import { parseAsInteger, parseAsString, parseAsBoolean, useQueryStates } from "nuqs";
import api from "@/lib/api";
import type {
  StaticSummaryResponse,
  CharacterSummaryResponse,
  PagedResponse,
} from "@/types/api";

// ---------------------------------------------------------------------------
// useSearchStatics
// ---------------------------------------------------------------------------

const staticsFilterParsers = {
  dataCenter: parseAsString,
  role: parseAsString,
  language: parseAsString,
  contentScope: parseAsString,
  query: parseAsString,
  page: parseAsInteger.withDefault(0),
};

export function useSearchStatics() {
  const [filters, setFilters] = useQueryStates(staticsFilterParsers);

  const query = useQuery<PagedResponse<StaticSummaryResponse>>({
    queryKey: ["search", "statics", filters],
    queryFn: () =>
      api
        .get<PagedResponse<StaticSummaryResponse>>("/search/statics", {
          params: filters,
        })
        .then((r) => r.data),
  });

  return { ...query, filters, setFilters };
}

// ---------------------------------------------------------------------------
// useSearchPlayers
// ---------------------------------------------------------------------------

const playersFilterParsers = {
  dataCenter: parseAsString,
  role: parseAsString,
  jobName: parseAsString,
  lookingForStatic: parseAsBoolean,
  page: parseAsInteger.withDefault(0),
};

export function useSearchPlayers() {
  const [filters, setFilters] = useQueryStates(playersFilterParsers);

  const query = useQuery<PagedResponse<CharacterSummaryResponse>>({
    queryKey: ["search", "players", filters],
    queryFn: () =>
      api
        .get<PagedResponse<CharacterSummaryResponse>>("/search/players", {
          params: filters,
        })
        .then((r) => r.data),
  });

  return { ...query, filters, setFilters };
}
