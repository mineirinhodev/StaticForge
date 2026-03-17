import { describe, it, expect, beforeEach, afterEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { setupServer } from "msw/node";
import PlayerSearchPage from "@/app/[locale]/(main)/search/players/page";
import { createTestWrapper } from "@/test/helpers";
import type { PagedResponse, CharacterSummaryResponse } from "@/types/api";

const mockData: PagedResponse<CharacterSummaryResponse> = {
  content: [
    {
      id: "1",
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
  http.get("http://localhost:8080/api/v1/search/players", () =>
    HttpResponse.json(mockData),
  ),
);

beforeEach(() => server.listen());
afterEach(() => server.resetHandlers());
afterEach(() => server.close());

describe("PlayerSearchPage", () => {
  it("renders the page heading", () => {
    render(<PlayerSearchPage />, { wrapper: createTestWrapper() });
    expect(
      screen.getByRole("heading", { name: /buscar jogadores|search players/i }),
    ).toBeInTheDocument();
  });

  it("renders the SearchFilters component with job filter", () => {
    render(<PlayerSearchPage />, { wrapper: createTestWrapper() });
    expect(screen.getByLabelText(/job/i)).toBeInTheDocument();
  });

  it("renders results after data loads", async () => {
    render(<PlayerSearchPage />, { wrapper: createTestWrapper() });
    await waitFor(() =>
      expect(screen.getByText("Luna Starfall")).toBeInTheDocument(),
    );
  });

  it("shows total results count", async () => {
    render(<PlayerSearchPage />, { wrapper: createTestWrapper() });
    await waitFor(() =>
      expect(screen.getByText(/1 resultado|1 result/i)).toBeInTheDocument(),
    );
  });
});
