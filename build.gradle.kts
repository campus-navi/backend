plugins {
    java
    id("org.springframework.boot") version "4.0.6"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.campusnavi"
version = "0.0.1-SNAPSHOT"
description = "campus-navi-backend"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

extra["netty.version"] = "4.2.13.Final"

repositories {
    mavenCentral()
}

dependencies {
    //Spring
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")

    //Security & Auth
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("io.jsonwebtoken:jjwt-api:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.13.0")

    //DB  & Migration
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("io.hypersistence:hypersistence-utils-hibernate-71:3.15.2")

    //Swagger
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.2")

    //Cache
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    //Mail (SMTP - local)
    implementation("org.springframework.boot:spring-boot-starter-mail")

    //Mail (SES - dev/prod)
    implementation("software.amazon.awssdk:ses:2.31.19")

    //Storage (S3 - dev/prod)
    implementation("software.amazon.awssdk:s3:2.31.19")

    //Crawling
    implementation("org.jsoup:jsoup:1.18.1")

    //HTTP Client
    implementation("org.apache.httpcomponents.client5:httpclient5")

    //Util
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    //QueryDSL
    implementation("io.github.openfeign.querydsl:querydsl-jpa:7.1")
    annotationProcessor("io.github.openfeign.querydsl:querydsl-apt:7.1:jpa")

    annotationProcessor("jakarta.persistence:jakarta.persistence-api")
    annotationProcessor("jakarta.annotation:jakarta.annotation-api")

    //Test
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.springframework.boot:spring-boot-starter-data-redis-test")
    testImplementation("org.springframework.boot:spring-boot-starter-flyway-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testCompileOnly("org.projectlombok:lombok")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testAnnotationProcessor("org.projectlombok:lombok")
}

tasks.withType<Test> {
    useJUnitPlatform()
    systemProperty("user.timezone", "Asia/Seoul")
}
