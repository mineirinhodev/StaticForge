# ========================================
# Stage 1: Install dependencies
# ========================================
FROM node:22-alpine AS deps

WORKDIR /app

# Copy root workspace files
COPY package.json package-lock.json ./

# Copy workspace package.json files (needed for npm ci to resolve workspaces)
COPY apps/web/package.json ./apps/web/
COPY packages/eslint-config/package.json ./packages/eslint-config/
COPY packages/typescript-config/package.json ./packages/typescript-config/

# Install all dependencies
RUN npm ci

# ========================================
# Stage 2: Build the application
# ========================================
FROM node:22-alpine AS build

WORKDIR /app

# Copy installed dependencies from deps stage (npm workspaces hoists everything to root node_modules)
COPY --from=deps /app/node_modules ./node_modules

# Copy shared packages (needed for build)
COPY packages/ ./packages/
COPY package.json package-lock.json turbo.json ./

# Copy web app source
COPY apps/web/ ./apps/web/

# Build the Next.js app (generates .next/standalone)
WORKDIR /app/apps/web
RUN npm run build

# ========================================
# Stage 3: Production runtime
# ========================================
FROM node:22-alpine AS runtime

WORKDIR /app

# Create non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy standalone server output
COPY --from=build /app/apps/web/.next/standalone ./
# Copy static assets
COPY --from=build /app/apps/web/.next/static ./apps/web/.next/static
# Copy public directory
COPY --from=build /app/apps/web/public ./apps/web/public

# Switch to non-root user
USER appuser

EXPOSE 3000

ENV PORT=3000
ENV HOSTNAME="0.0.0.0"

CMD ["node", "apps/web/server.js"]
