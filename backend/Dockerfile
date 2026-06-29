# syntax=docker/dockerfile:1.6
# ---------- build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
# 优先拷贝 pom 让依赖层缓存命中
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline
COPY src ./src
RUN mvn -B -ntp -DskipTests package

# ---------- runtime ----------
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /src/target/zhikao-cloud-backend-1.0.0.jar app.jar

ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-Xms256m -Xmx768m -XX:+UseG1GC"

EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=4s --retries=10 --start-period=60s \
  CMD wget -qO- http://127.0.0.1:8080/api/v1/auth/login >/dev/null 2>&1 || exit 1

ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS -jar app.jar"]