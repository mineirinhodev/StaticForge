import nextConfig from "@ffxiv-planner/eslint-config/next.js";

/** @type {import("eslint").Linter.Config[]} */
export default [
  ...nextConfig,
  {
    ignores: ["next-env.d.ts"],
  },
];
