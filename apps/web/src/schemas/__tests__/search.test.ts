import { describe, it, expect } from "vitest";
import { searchStaticsSchema, searchPlayersSchema } from "@/schemas/search";

describe("searchStaticsSchema", () => {
  it("accepts an empty object (all filters optional)", () => {
    const result = searchStaticsSchema.safeParse({});
    expect(result.success).toBe(true);
  });

  it("accepts valid statics filter values", () => {
    const result = searchStaticsSchema.safeParse({
      dataCenter: "Primal",
      role: "HEALER",
      language: "en",
      contentScope: "SAVAGE",
      query: "heaven",
      page: 2,
    });
    expect(result.success).toBe(true);
  });

  it("rejects an invalid role value", () => {
    const result = searchStaticsSchema.safeParse({ role: "MAGE" });
    expect(result.success).toBe(false);
  });

  it("rejects an invalid contentScope value", () => {
    const result = searchStaticsSchema.safeParse({ contentScope: "FLEXRAID" });
    expect(result.success).toBe(false);
  });

  it("rejects a negative page number", () => {
    const result = searchStaticsSchema.safeParse({ page: -1 });
    expect(result.success).toBe(false);
  });

  it("defaults page to 0 when not provided", () => {
    const result = searchStaticsSchema.safeParse({});
    expect(result.success).toBe(true);
    expect(result.data?.page).toBe(0);
  });
});

describe("searchPlayersSchema", () => {
  it("accepts an empty object (all filters optional)", () => {
    const result = searchPlayersSchema.safeParse({});
    expect(result.success).toBe(true);
  });

  it("accepts valid players filter values", () => {
    const result = searchPlayersSchema.safeParse({
      dataCenter: "Aether",
      role: "TANK",
      jobName: "PLD",
      lookingForStatic: true,
      page: 0,
    });
    expect(result.success).toBe(true);
  });

  it("rejects an invalid role value", () => {
    const result = searchPlayersSchema.safeParse({ role: "SUPPORT" });
    expect(result.success).toBe(false);
  });

  it("rejects a negative page number", () => {
    const result = searchPlayersSchema.safeParse({ page: -1 });
    expect(result.success).toBe(false);
  });

  it("defaults page to 0 when not provided", () => {
    const result = searchPlayersSchema.safeParse({});
    expect(result.success).toBe(true);
    expect(result.data?.page).toBe(0);
  });

  it("coerces lookingForStatic to boolean", () => {
    const result = searchPlayersSchema.safeParse({ lookingForStatic: false });
    expect(result.success).toBe(true);
    expect(result.data?.lookingForStatic).toBe(false);
  });
});
