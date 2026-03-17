import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { Pagination } from "@/components/search/Pagination";

describe("Pagination", () => {
  it("renders previous and next buttons", () => {
    render(
      <Pagination
        page={1}
        totalPages={5}
        hasNext={true}
        onPageChange={vi.fn()}
      />,
    );
    expect(
      screen.getByRole("button", { name: /anterior|previous/i }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: /próximo|próxima|next/i }),
    ).toBeInTheDocument();
  });

  it("disables previous button on first page", () => {
    render(
      <Pagination
        page={0}
        totalPages={5}
        hasNext={true}
        onPageChange={vi.fn()}
      />,
    );
    expect(
      screen.getByRole("button", { name: /anterior|previous/i }),
    ).toBeDisabled();
  });

  it("disables next button when hasNext is false", () => {
    render(
      <Pagination
        page={4}
        totalPages={5}
        hasNext={false}
        onPageChange={vi.fn()}
      />,
    );
    expect(
      screen.getByRole("button", { name: /próximo|próxima|next/i }),
    ).toBeDisabled();
  });

  it("calls onPageChange with page - 1 when previous is clicked", async () => {
    const onPageChange = vi.fn();
    render(
      <Pagination
        page={2}
        totalPages={5}
        hasNext={true}
        onPageChange={onPageChange}
      />,
    );
    await userEvent.click(
      screen.getByRole("button", { name: /anterior|previous/i }),
    );
    expect(onPageChange).toHaveBeenCalledWith(1);
  });

  it("calls onPageChange with page + 1 when next is clicked", async () => {
    const onPageChange = vi.fn();
    render(
      <Pagination
        page={2}
        totalPages={5}
        hasNext={true}
        onPageChange={onPageChange}
      />,
    );
    await userEvent.click(
      screen.getByRole("button", { name: /próximo|próxima|next/i }),
    );
    expect(onPageChange).toHaveBeenCalledWith(3);
  });

  it("shows current page info", () => {
    render(
      <Pagination
        page={1}
        totalPages={5}
        hasNext={true}
        onPageChange={vi.fn()}
      />,
    );
    // should show something like "2 / 5" or "Página 2 de 5"
    expect(screen.getByText(/2/)).toBeInTheDocument();
    expect(screen.getByText(/5/)).toBeInTheDocument();
  });

  it("does not render when totalPages is 0", () => {
    const { container } = render(
      <Pagination
        page={0}
        totalPages={0}
        hasNext={false}
        onPageChange={vi.fn()}
      />,
    );
    expect(container.firstChild).toBeNull();
  });
});
