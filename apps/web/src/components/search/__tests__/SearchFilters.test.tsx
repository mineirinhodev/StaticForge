import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { SearchFilters } from "@/components/search/SearchFilters";

describe("SearchFilters (statics variant)", () => {
  const defaultProps = {
    variant: "statics" as const,
    filters: { dataCenter: null, role: null, language: null, contentScope: null, query: null },
    onFilterChange: vi.fn(),
  };

  it("renders a search query input", () => {
    render(<SearchFilters {...defaultProps} />);
    expect(screen.getByRole("searchbox")).toBeInTheDocument();
  });

  it("renders a data center filter", () => {
    render(<SearchFilters {...defaultProps} />);
    expect(screen.getByLabelText(/data center/i)).toBeInTheDocument();
  });

  it("renders a role filter", () => {
    render(<SearchFilters {...defaultProps} />);
    expect(screen.getByLabelText(/role|função/i)).toBeInTheDocument();
  });

  it("renders a content scope filter when variant is statics", () => {
    render(<SearchFilters {...defaultProps} />);
    expect(screen.getByLabelText(/conteúdo|content/i)).toBeInTheDocument();
  });

  it("calls onFilterChange when query input changes", async () => {
    const onFilterChange = vi.fn();
    render(
      <SearchFilters {...defaultProps} onFilterChange={onFilterChange} />,
    );
    await userEvent.type(screen.getByRole("searchbox"), "heaven");
    expect(onFilterChange).toHaveBeenCalled();
  });

  it("renders a clear filters button", () => {
    render(
      <SearchFilters
        {...defaultProps}
        filters={{ ...defaultProps.filters, dataCenter: "Primal" }}
      />,
    );
    expect(screen.getByRole("button", { name: /limpar|clear/i })).toBeInTheDocument();
  });
});

describe("SearchFilters (players variant)", () => {
  const defaultProps = {
    variant: "players" as const,
    filters: { dataCenter: null, role: null, jobName: null, lookingForStatic: null },
    onFilterChange: vi.fn(),
  };

  it("renders a job name filter when variant is players", () => {
    render(<SearchFilters {...defaultProps} />);
    expect(screen.getByLabelText(/job/i)).toBeInTheDocument();
  });

  it("does not render contentScope filter when variant is players", () => {
    render(<SearchFilters {...defaultProps} />);
    expect(
      screen.queryByLabelText(/conteúdo|content/i),
    ).not.toBeInTheDocument();
  });
});
