import { z } from "zod";

export const ROLES = ["TANK", "HEALER", "DPS"] as const;
export const CONTENT_SCOPES = [
  "SAVAGE",
  "ULTIMATE",
  "EXTREME",
  "CRITERION",
] as const;

export const searchStaticsSchema = z.object({
  dataCenter: z.string().optional(),
  server: z.string().optional(),
  role: z.enum(ROLES).optional(),
  language: z.string().optional(),
  contentScope: z.enum(CONTENT_SCOPES).optional(),
  status: z.string().optional(),
  query: z.string().optional(),
  page: z.number().int().min(0).default(0),
  size: z.number().int().min(1).max(100).default(20),
  sort: z.string().optional(),
});

export const searchPlayersSchema = z.object({
  dataCenter: z.string().optional(),
  server: z.string().optional(),
  role: z.enum(ROLES).optional(),
  jobName: z.string().optional(),
  language: z.string().optional(),
  parseTier: z.string().optional(),
  lookingForStatic: z.boolean().optional(),
  query: z.string().optional(),
  page: z.number().int().min(0).default(0),
  size: z.number().int().min(1).max(100).default(20),
  sort: z.string().optional(),
});

export type SearchStaticsInput = z.input<typeof searchStaticsSchema>;
export type SearchStaticsFilters = z.output<typeof searchStaticsSchema>;

export type SearchPlayersInput = z.input<typeof searchPlayersSchema>;
export type SearchPlayersFilters = z.output<typeof searchPlayersSchema>;
