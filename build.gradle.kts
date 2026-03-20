import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
    kotlin("jvm") version "2.3.20"
    kotlin("plugin.spring") version "2.3.20"
    id("org.springframework.boot") version "4.0.3"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "nu.westlin"
version = "0.0.1-SNAPSHOT"
description = "InforTacoJmsDemo"

repositories {
    maven { url = uri("https://maven.lmv.lm.se/repo") }
    mavenLocal()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-jms") // JMS-stöd utan broker-specifik auto-config
    implementation("tibco:jakarta.jms-tibjms:10.3.0")                     // Ditt TIBCO-beroende
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("tools.jackson.module:jackson-module-kotlin")

    runtimeOnly("org.postgresql:postgresql")
    
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")

    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-artemis-test")
    testImplementation("org.springframework.boot:spring-boot-starter-jdbc-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

/**
 * Skickar vidare System.properties till BootRun-tasket så man ex. kan göra: gradle bootrun -Dspring.profiles.active=fisksoppa
 */
tasks.withType<BootRun> {
    systemProperties(System.getProperties().mapKeys { it.key as String })
}
