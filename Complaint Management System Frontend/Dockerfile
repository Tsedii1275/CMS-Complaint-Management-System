FROM node:24-alpine AS build
WORKDIR /src

COPY package.json package-lock.json ./
RUN npm install --no-audit --no-fund

COPY . .
ENV REACT_APP_API_BASE_URL=
ENV GENERATE_SOURCEMAP=false
RUN npm run build

FROM nginx:1.30.4-alpine
COPY --from=build /src/build /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
