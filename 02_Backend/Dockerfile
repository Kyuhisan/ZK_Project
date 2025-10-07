FROM eclipse-temurin:21-jdk-jammy

RUN apt-get update && \
    apt-get install -y \
        ca-certificates \
        fonts-liberation \
        libappindicator3-1 \
        libasound2 \
        libatk-bridge2.0-0 \
        libatk1.0-0 \
        libcups2 \
        libdbus-1-3 \
        libdrm2 \
        libgdk-pixbuf2.0-0 \
        libgtk-3-0 \
        libnspr4 \
        libnss3 \
        libx11-6 \
        libx11-xcb1 \
        libxcb1 \
        libxcomposite1 \
        libxcursor1 \
        libxdamage1 \
        libxext6 \
        libxfixes3 \
        libxi6 \
        libxrandr2 \
        libxrender1 \
        libxss1 \
        libxtst6 \
        libgbm1 \
        xdg-utils \
        xvfb \
        --no-install-recommends && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

ENV SE_FORCE_BROWSER_DOWNLOAD=true
ENV SE_AVOID_STATS=true
ENV SE_CACHE_PATH=/app/.selenium-cache
ENV DISPLAY=:99

WORKDIR /app

COPY target/*.jar app.jar
COPY output/keywords/ output/keywords/
COPY mongo-init/ mongo-init/

RUN chmod +x mongo-init/init.sh

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]