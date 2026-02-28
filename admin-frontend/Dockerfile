# Etapa 1: Construcción (Build)
FROM node:22-alpine AS builder
WORKDIR /app

# Copiar dependencias e instalar
COPY package*.json ./
RUN npm install

# Copiar el resto del código y compilar
COPY . .
# Si usas variables de entorno en el build, descomenta la siguiente línea y pásala en el docker-compose
# ARG GEMINI_API_KEY
# ENV GEMINI_API_KEY=$GEMINI_API_KEY
RUN npm run build

# Etapa 2: Servidor (Nginx)
FROM nginx:alpine

# Copiar la configuración personalizada de Nginx
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copiar los archivos estáticos compilados desde la etapa 1
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]