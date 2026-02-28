FROM node:20-alpine AS build
WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .

RUN npm run build


FROM node:20-alpine
WORKDIR /app

COPY --from=build /app/dist/app ./dist/app

EXPOSE 4000


CMD ["node", "dist/app/server/server.mjs"]