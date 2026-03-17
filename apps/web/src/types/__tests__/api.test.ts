import { describe, it, expect } from "vitest";
import {
  type StaticSummaryResponse,
  type CharacterSummaryResponse,
  type PagedResponse,
} from "@/types/api";

describe("API types", () => {
  it("StaticSummaryResponse has expected shape", () => {
    const item: StaticSummaryResponse = {
      id: "550e8400-e29b-41d4-a716-446655440000",
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
    };
    expect(item.id).toBe("550e8400-e29b-41d4-a716-446655440000");
    expect(item.contentScope).toContain("SAVAGE");
  });

  it("CharacterSummaryResponse has expected shape", () => {
    const item: CharacterSummaryResponse = {
      id: "uuid",
      name: "Luna Starfall",
      server: "Lamia",
      dataCenter: "Primal",
      isVerified: true,
      avatarUrl: "https://example.com/avatar.jpg",
      mainJobName: "WHM",
    };
    expect(item.isVerified).toBe(true);
    expect(item.mainJobName).toBe("WHM");
  });

  it("PagedResponse wraps a content array with pagination metadata", () => {
    const page: PagedResponse<StaticSummaryResponse> = {
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
      hasNext: false,
    };
    expect(page.content).toHaveLength(0);
    expect(page.hasNext).toBe(false);
  });
});
