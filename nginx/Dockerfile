# Nginx reverse proxy for FFXIV Static Planner (hardened)
FROM nginx:1.27-alpine

# Remove default configs and HTML to reduce attack surface
RUN rm -rf /etc/nginx/conf.d/default.conf \
           /usr/share/nginx/html/*

COPY nginx.conf /etc/nginx/nginx.conf

# Pre-create all cache subdirectories Nginx needs at runtime,
# then hand ownership to the nginx user (UID 101).
# Listens on unprivileged port 8080 — docker-compose maps 80:8080.
RUN mkdir -p /var/cache/nginx/client_temp \
             /var/cache/nginx/proxy_temp \
             /var/cache/nginx/fastcgi_temp \
             /var/cache/nginx/uwsgi_temp \
             /var/cache/nginx/scgi_temp \
    && chown -R nginx:nginx /var/cache/nginx \
    && chmod -R 755 /var/cache/nginx \
    && chown -R nginx:nginx /var/log/nginx \
    && touch /tmp/nginx.pid \
    && chown nginx:nginx /tmp/nginx.pid

USER nginx

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD wget -qO /dev/null http://localhost:8080/ || exit 1
