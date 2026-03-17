import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { NuqsTestingAdapter } from "nuqs/adapters/testing";
import React from "react";

/**
 * Creates a test wrapper with QueryClientProvider and NuqsTestingAdapter.
 * Use as the `wrapper` option in renderHook / render calls.
 */
export function createTestWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return function Wrapper({ children }: { children: React.ReactNode }) {
    return React.createElement(
      NuqsTestingAdapter,
      null,
      React.createElement(QueryClientProvider, { client: queryClient }, children),
    );
  };
}
