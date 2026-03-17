import { describe, it, expect, beforeEach, afterEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { setupServer } from "msw/node";
import StaticSearchPage from "@/app/[locale]/(main)/search/statics/page";
import { createTestWrapper } from "@/test/helpers";
import type { PagedResponse, StaticSummaryResponse } from "@/types/api";

const mockData: PagedResponse<StaticSummaryResponse> = {
  content: [
    {
      id: "1",
      name: "Phoenix Rising",
      contentScope: ["SAVAGE"],
      language: "en",
      status: "RECRUITING",
      maxMembers: 8,
      currentMembers: 4,
      dataCenter: "Primal",
      server: "Lamia",
      openVacancies: 4,
      createdAt: "2026-03-01T00:00:00Z",
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
    HttpResponse.json(mockData),
  ),
);

beforeEach(() => server.listen());
afterEach(() => server.resetHandlers());
afterEach(() => server.close());

describe("StaticSearchPage", () => {
  it("renders the page heading", () => {
    render(<StaticSearchPage />, { wrapper: createTestWrapper() });
    expect(
      screen.getByRole("heading", { name: /buscar statics|search statics/i }),
    ).toBeInTheDocument();
  });

  it("renders the SearchFilters component", () => {
    render(<StaticSearchPage />, { wrapper: createTestWrapper() });
    expect(screen.getByRole("searchbox")).toBeInTheDocument();
  });

  it("renders results after data loads", async () => {
    render(<StaticSearchPage />, { wrapper: createTestWrapper() });
    await waitFor(() =>
      expect(screen.getByText("Phoenix Rising")).toBeInTheDocument(),
    );
  });

  it("shows total results count", async () => {
    render(<StaticSearchPage />, { wrapper: createTestWrapper() });
    await waitFor(() =>
      expect(screen.getByText(/1 resultado|1 result/i)).toBeInTheDocument(),
    );
  });
});
