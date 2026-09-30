# =========================================================
# 后端镜像：多阶段构建（Maven 构建 → 精简 JRE 运行）
# 构建：docker build -t saas-crm-backend ./backend
# =========================================================

# ---------- 构建阶段 ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# 先拷贝 pom 并预拉依赖，利用 Docker 层缓存
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q package -DskipTests

# ---------- 运行阶段 ----------
FROM eclipse-temurin:21-jre-jammy

# curl 用于容器健康检查
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd -r -u 10001 -m app

WORKDIR /app
COPY --from=build /build/target/*.jar app.jar

# 上传目录（运行时以 -v 挂载卷持久化，对应 UPLOAD_DIR=/data/uploads）
RUN mkdir -p /data/uploads && chown -R app:app /app /data

USER app
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD curl -fsS http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
