FROM node:24-alpine AS build
WORKDIR /src

COPY package.json package-lock.json ./
RUN npm install --no-audit --no-fund

COPY . .
# Same-origin /api in the image. Nginx uses CMS_BACKEND from .env.dev / .env.prod at runtime.
ENV REACT_APP_API_BASE_URL=
ENV GENERATE_SOURCEMAP=false
RUN npm run build

FROM nginx:1.30.4-alpine
COPY nginx-main.conf /etc/nginx/nginx.conf
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /src/build /usr/share/nginx/html

RUN chmod -R a-w /usr/share/nginx/html \
    && chown -R nginx:nginx /var/cache/nginx /var/log/nginx /etc/nginx/conf.d /etc/nginx/templates

EXPOSE 8080
USER nginx
