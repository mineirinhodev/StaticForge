# Nginx reverse proxy for FFXIV Static Planner (hardened)
FROM nginx:1.27-alpine

# Remove default configs and HTML to reduce attack surface
RUN rm -rf /etc/nginx/conf.d/default.conf \
           /usr/share/nginx/html/*

COPY nginx.conf /etc/nginx/nginx.conf

# Use non-root user (nginx user already exists in the image).
# Listens on unprivileged port 8080 — docker-compose maps 80:8080.
RUN chown -R nginx:nginx /var/cache/nginx /var/log/nginx /tmp \
    && touch /tmp/nginx.pid \
    && chown nginx:nginx /tmp/nginx.pid

USER nginx

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD wget -qO /dev/null http://localhost:8080/ || exit 1
