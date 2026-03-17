import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { StaticResultList } from "@/components/search/StaticResultList";
import type { StaticSummaryResponse } from "@/types/api";

const statics: StaticSummaryResponse[] = [
  {
    id: "1",
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
  {
    id: "2",
    name: "Omega Destroyers",
    contentScope: ["ULTIMATE"],
    language: "ja",
    status: "RECRUITING",
    maxMembers: 8,
    currentMembers: 7,
    dataCenter: "Aether",
    server: "Gilgamesh",
    openVacancies: 1,
    createdAt: "2026-03-05T10:00:00Z",
  },
];

describe("StaticResultList", () => {
  it("renders a list of static cards", () => {
    render(<StaticResultList statics={statics} />);
    expect(screen.getByText("Heavens' Gate")).toBeInTheDocument();
    expect(screen.getByText("Omega Destroyers")).toBeInTheDocument();
  });

  it("shows empty state when list is empty", () => {
    render(<StaticResultList statics={[]} />);
    expect(screen.getByText(/nenhuma static|no statics/i)).toBeInTheDocument();
  });

  it("shows data center for each static", () => {
    render(<StaticResultList statics={statics} />);
    expect(screen.getByText(/Primal/)).toBeInTheDocument();
    expect(screen.getByText(/Aether/)).toBeInTheDocument();
  });

  it("shows open vacancies count for each static", () => {
    render(<StaticResultList statics={statics} />);
    expect(screen.getByText(/3/)).toBeInTheDocument();
    expect(screen.getByText(/1/)).toBeInTheDocument();
  });

  it("shows content scope for each static", () => {
    render(<StaticResultList statics={statics} />);
    expect(screen.getByText(/SAVAGE/i)).toBeInTheDocument();
    expect(screen.getByText(/ULTIMATE/i)).toBeInTheDocument();
  });

  it("renders a loading skeleton when isLoading is true", () => {
    const { container } = render(
      <StaticResultList statics={[]} isLoading={true} />,
    );
    expect(container.querySelector("[data-testid='static-skeleton']")).not.toBeNull();
  });
});
