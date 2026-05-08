FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

# 先复制 pom 以便缓存依赖
COPY pom.xml .
RUN mvn -DskipTests package -q || true

# 再复制源码
COPY src ./src

RUN mvn -DskipTests package -q

# 复制打包产物到统一文件名
RUN JAR_PATH="$(ls -1 target/*.jar | head -n 1)" && cp "$JAR_PATH" /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/app.jar"]

