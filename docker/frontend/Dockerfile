# ---- 构建阶段：编译 uni-app H5 ----
FROM node:20-alpine AS build
WORKDIR /build
COPY package.json package-lock.json* ./
RUN npm install
COPY . .
RUN npm run build:h5

# ---- 运行阶段：Nginx 托管静态产物并反代 /api ----
FROM nginx:1.27-alpine
COPY --from=build /build/dist/build/h5 /usr/share/nginx/html
COPY docker/frontend/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
