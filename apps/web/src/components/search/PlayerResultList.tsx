"use client";

import type { CharacterSummaryResponse } from "@/types/api";

interface PlayerResultListProps {
  players: CharacterSummaryResponse[];
  isLoading?: boolean;
}

function PlayerSkeleton() {
  return (
    <div
      data-testid="player-skeleton"
      className="animate-pulse flex flex-col gap-3"
    >
      {Array.from({ length: 5 }).map((_, i) => (
        <div key={i} className="h-16 bg-gray-200 rounded-lg" />
      ))}
    </div>
  );
}

function PlayerCard({ player }: { player: CharacterSummaryResponse }) {
  return (
    <article className="border rounded-lg p-4 flex items-center gap-4">
      {player.avatarUrl && (
        // eslint-disable-next-line @next/next/no-img-element
        <img
          src={player.avatarUrl}
          alt={`Avatar de ${player.name}`}
          className="w-12 h-12 rounded-full object-cover"
        />
      )}
      <div className="flex flex-col gap-1">
        <h3 className="font-semibold">{player.name}</h3>
        <div className="flex gap-2 text-sm text-gray-600">
          <span>{player.dataCenter}</span>
          {player.server && <span>· {player.server}</span>}
          {player.mainJobName && <span>· {player.mainJobName}</span>}
        </div>
      </div>
    </article>
  );
}

export function PlayerResultList({
  players,
  isLoading = false,
}: PlayerResultListProps) {
  if (isLoading) return <PlayerSkeleton />;

  if (players.length === 0) {
    return (
      <p className="text-center text-gray-500 py-8">
        Nenhum jogador encontrado.
      </p>
    );
  }

  return (
    <ul className="flex flex-col gap-3" aria-label="Lista de jogadores">
      {players.map((player) => (
        <li key={player.id}>
          <PlayerCard player={player} />
        </li>
      ))}
    </ul>
  );
}
