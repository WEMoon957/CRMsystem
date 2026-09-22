# =========================================================
# 前端镜像：多阶段构建（Node 构建 → nginx 托管静态资源并反代后端）
# 构建：docker build -t saas-crm-frontend ./frontend
# =========================================================

# ---------- 构建阶段 ----------
FROM node:22-alpine AS build
WORKDIR /build

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# ---------- 运行阶段 ----------
FROM nginx:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /build/dist /usr/share/nginx/html

EXPOSE 80
HEALTHCHECK --interval=30s --timeout=3s \
    CMD wget -q -O /dev/null http://127.0.0.1/ || exit 1
