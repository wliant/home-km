FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
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
EXPOSE 80
