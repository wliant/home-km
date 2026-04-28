FROM node:24-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
ARG VITE_APP_NAME="Home KM"
ARG VITE_APP_THEME_COLOR="#6366f1"
ENV VITE_APP_NAME=$VITE_APP_NAME
ENV VITE_APP_THEME_COLOR=$VITE_APP_THEME_COLOR
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
ARG MAX_FILE_UPLOAD_MB=100
ENV MAX_FILE_UPLOAD_MB=$MAX_FILE_UPLOAD_MB
# MINIO_PUBLIC_ORIGIN is the browser-visible MinIO origin (e.g. http://minio.lan:9000)
# substituted into the CSP header. Empty default = same-origin only.
ENV MINIO_PUBLIC_ORIGIN=""
EXPOSE 80
