import { describe, it, expect, beforeEach, afterEach } from "vitest";
import { renderHook, waitFor } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { setupServer } from "msw/node";
import { useSearchStatics, useSearchPlayers } from "@/hooks/use-search";
import { createTestWrapper } from "@/test/helpers";
import type {
  StaticSummaryResponse,
  CharacterSummaryResponse,
  PagedResponse,
} from "@/types/api";

// ---------------------------------------------------------------------------
// Mock API server
// ---------------------------------------------------------------------------

const mockStaticsPage: PagedResponse<StaticSummaryResponse> = {
  content: [
    {
      id: "abc",
      name: "Heavens' Gate",
      contentScope: ["SAVAGE"],
      language: "en",
      status: "RECRUITING",
      maxMembers: 8,
      currentMembers: 5,
      dataCenter: "Primal",
      server: "Lamia",
      openVacancies: 3,
      createdAt: "2026-03-04T20:00:00Z",
    },
  ],
  page: 0,
  size: 20,
  totalElements: 1,
  totalPages: 1,
  hasNext: false,
};

const mockPlayersPage: PagedResponse<CharacterSummaryResponse> = {
  content: [
    {
      id: "xyz",
      name: "Luna Starfall",
      server: "Lamia",
      dataCenter: "Primal",
      isVerified: true,
      avatarUrl: "https://example.com/avatar.jpg",
      mainJobName: "WHM",
    },
  ],
  page: 0,
  size: 20,
  totalElements: 1,
  totalPages: 1,
  hasNext: false,
};

const server = setupServer(
  http.get("http://localhost:8080/api/v1/search/statics", () =>
    HttpResponse.json(mockStaticsPage),
  ),
  http.get("http://localhost:8080/api/v1/search/players", () =>
    HttpResponse.json(mockPlayersPage),
  ),
);

beforeEach(() => server.listen());
afterEach(() => server.resetHandlers());
afterEach(() => server.close());

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

// ---------------------------------------------------------------------------
// useSearchStatics
// ---------------------------------------------------------------------------

describe("useSearchStatics", () => {
  it("fetches statics and returns paginated data", async () => {
    const { result } = renderHook(() => useSearchStatics(), {
      wrapper: createTestWrapper(),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(result.current.data?.content).toHaveLength(1);
    expect(result.current.data?.content[0].name).toBe("Heavens' Gate");
    expect(result.current.data?.totalElements).toBe(1);
  });

  it("exposes filters object derived from URL params", () => {
    const { result } = renderHook(() => useSearchStatics(), {
      wrapper: createTestWrapper(),
    });

    expect(result.current.filters).toBeDefined();
    expect(result.current.filters.page).toBe(0);
  });

  it("exposes setFilters function", () => {
    const { result } = renderHook(() => useSearchStatics(), {
      wrapper: createTestWrapper(),
    });

    expect(typeof result.current.setFilters).toBe("function");
  });
});

// ---------------------------------------------------------------------------
// useSearchPlayers
// ---------------------------------------------------------------------------

describe("useSearchPlayers", () => {
  it("fetches players and returns paginated data", async () => {
    const { result } = renderHook(() => useSearchPlayers(), {
      wrapper: createTestWrapper(),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(result.current.data?.content).toHaveLength(1);
    expect(result.current.data?.content[0].name).toBe("Luna Starfall");
    expect(result.current.data?.content[0].isVerified).toBe(true);
  });

  it("exposes filters object derived from URL params", () => {
    const { result } = renderHook(() => useSearchPlayers(), {
      wrapper: createTestWrapper(),
    });

    expect(result.current.filters).toBeDefined();
    expect(result.current.filters.page).toBe(0);
  });

  it("exposes setFilters function", () => {
    const { result } = renderHook(() => useSearchPlayers(), {
      wrapper: createTestWrapper(),
    });

    expect(typeof result.current.setFilters).toBe("function");
  });
});
