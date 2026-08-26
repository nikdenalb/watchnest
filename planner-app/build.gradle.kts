plugins {
    java
    id("org.springframework.boot") version "3.5.16"
    id("io.spring.dependency-management") version "1.1.7"
}

group = rootProject.property("group").toString()
version = property("plannerAppVersion").toString()

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

dependencies {
    implementation(project(":planner"))
    implementation(project(":identity"))
    implementation(project(":catalog"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.liquibase:liquibase-core")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.9")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
}

val testSourceSet = sourceSets.test.get()

tasks.named<Test>("test") {
    useJUnitPlatform {
        excludeTags("persistent-http")
    }
}

tasks.register<Test>("persistentHttpTest") {
    group = "verification"
    description = "Run HTTP tests on PostgreSQL via Testcontainers (requires Docker)"
    testClassesDirs = testSourceSet.output.classesDirs
    classpath = testSourceSet.runtimeClasspath
    shouldRunAfter(tasks.test)
    failOnNoDiscoveredTests = true
    useJUnitPlatform {
        includeTags("persistent-http")
    }
}
