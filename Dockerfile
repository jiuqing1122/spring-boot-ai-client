# 构建阶段
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# 国内网络：使用阿里云 Maven 镜像，避免 Central 下载超时
COPY docker/maven-settings.xml /root/.m2/settings.xml

# 先拷 pom 预下载依赖，源码变动时不用重下
COPY pom.xml .
RUN mvn -B -q -s /root/.m2/settings.xml -DskipTests dependency:go-offline || true

COPY src ./src
RUN mvn -B -q -s /root/.m2/settings.xml -DskipTests package

# 运行阶段
FROM eclipse-temurin:17-jre-jammy
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=build /build/target/spring-boot-ai-client-*.jar app.jar

ENV TZ=Asia/Shanghai
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]