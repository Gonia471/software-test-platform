FROM maven:3.9.9-eclipse-temurin-17 AS builder

WORKDIR /build
COPY test-platform/backend/pom.xml ./pom.xml
COPY test-platform/backend/src ./src
RUN mvn -DskipTests clean package

FROM eclipse-temurin:17-jre-jammy

# UI 自动化：安装 Google Chrome（Render 容器内仅有 JRE，无浏览器则 Selenium 无法建连）
RUN apt-get update \
    && apt-get install -y --no-install-recommends wget ca-certificates \
    && wget -q -O /tmp/chrome.deb https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb \
    && apt-get install -y --no-install-recommends /tmp/chrome.deb \
    && rm /tmp/chrome.deb \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=builder /build/target/*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
