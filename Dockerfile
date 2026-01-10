# ===== Build stage =====
FROM gradle:8.13-jdk21 AS build

# /workspace にソースを展開
WORKDIR /workspace
COPY . .

# primus_project をルートとして扱う
WORKDIR /workspace

# primus-proxy モジュールの fat JAR をビルド
RUN gradle :primus-proxy:shadowJar --no-daemon

# ===== Run stage =====
FROM eclipse-temurin:21-jre

WORKDIR /srv

# 生成された shadowJar をコピー
COPY --from=build /workspace/primus-proxy/build/libs/primus-proxy-all.jar /srv/app.jar

EXPOSE 8080
ENV PORT=8080

ENTRYPOINT ["java","-jar","/srv/app.jar"]
