"use client";

interface PaginationProps {
  page: number;
  totalPages: number;
  hasNext: boolean;
  onPageChange: (page: number) => void;
}

export function Pagination({
  page,
  totalPages,
  hasNext,
  onPageChange,
}: PaginationProps) {
  if (totalPages === 0) return null;

  const currentDisplay = page + 1;

  return (
    <nav
      role="navigation"
      aria-label="Paginação"
      className="flex items-center justify-center gap-4"
    >
      <button
        aria-label="Anterior"
        onClick={() => onPageChange(page - 1)}
        disabled={page === 0}
        className="px-4 py-2 rounded border disabled:opacity-50 disabled:cursor-not-allowed"
      >
        Anterior
      </button>

      <span aria-current="page">
        {currentDisplay} / {totalPages}
      </span>

      <button
        aria-label="Próximo"
        onClick={() => onPageChange(page + 1)}
        disabled={!hasNext}
        className="px-4 py-2 rounded border disabled:opacity-50 disabled:cursor-not-allowed"
      >
        Próximo
      </button>
    </nav>
  );
}
