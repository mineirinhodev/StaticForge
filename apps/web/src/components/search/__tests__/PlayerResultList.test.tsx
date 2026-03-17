import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { PlayerResultList } from "@/components/search/PlayerResultList";
import type { CharacterSummaryResponse } from "@/types/api";

const players: CharacterSummaryResponse[] = [
  {
    id: "1",
    name: "Luna Starfall",
    server: "Lamia",
    dataCenter: "Primal",
    isVerified: true,
    avatarUrl: "https://example.com/avatar1.jpg",
    mainJobName: "WHM",
  },
  {
    id: "2",
    name: "Aerith Gainsborough",
    server: "Gilgamesh",
    dataCenter: "Aether",
    isVerified: true,
    avatarUrl: "https://example.com/avatar2.jpg",
    mainJobName: "PLD",
  },
];

describe("PlayerResultList", () => {
  it("renders a list of player cards", () => {
    render(<PlayerResultList players={players} />);
    expect(screen.getByText("Luna Starfall")).toBeInTheDocument();
    expect(screen.getByText("Aerith Gainsborough")).toBeInTheDocument();
  });

  it("shows empty state when list is empty", () => {
    render(<PlayerResultList players={[]} />);
    expect(
      screen.getByText(/nenhum jogador|no players/i),
    ).toBeInTheDocument();
  });

  it("shows main job name for each player", () => {
    render(<PlayerResultList players={players} />);
    expect(screen.getByText(/WHM/)).toBeInTheDocument();
    expect(screen.getByText(/PLD/)).toBeInTheDocument();
  });

  it("shows data center for each player", () => {
    render(<PlayerResultList players={players} />);
    expect(screen.getByText(/Primal/)).toBeInTheDocument();
    expect(screen.getByText(/Aether/)).toBeInTheDocument();
  });

  it("renders a loading skeleton when isLoading is true", () => {
    const { container } = render(
      <PlayerResultList players={[]} isLoading={true} />,
    );
    expect(container.querySelector("[data-testid='player-skeleton']")).not.toBeNull();
  });
});
