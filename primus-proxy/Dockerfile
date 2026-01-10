# ===== Build stage =====
FROM gradle:8.13-jdk21 AS build

WORKDIR /workspace

# primus-proxy プロジェクト一式をコピー
COPY . .

# 単一プロジェクトとして shadowJar をビルド
RUN gradle shadowJar --no-daemon

# ===== Run stage =====
FROM eclipse-temurin:21-jre

WORKDIR /srv

# 生成された shadowJar をコピー
COPY --from=build /workspace/build/libs/primus-proxy-all.jar /srv/app.jar

EXPOSE 8080
ENV PORT=8080

ENTRYPOINT ["java","-jar","/srv/app.jar"]
