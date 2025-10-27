plugins {
    java
    id("org.springframework.boot") version "3.5.6"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.openapi.generator") version "7.10.0"
    id("io.freefair.lombok") version "9.0.0"
    id("com.diffplug.spotless") version "8.0.0"
    id("org.sonarqube") version "7.0.1.6134"
}

group = "com.barclays"
version = "0.0.1-SNAPSHOT"
description =
    "REST API for Eagle Bank - A banking system supporting user management, account operations, and transaction processing with JWT authentication"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.retry:spring-retry")
    implementation("jakarta.annotation:jakarta.annotation-api")
    implementation("org.openapitools:jackson-databind-nullable:0.2.6")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.13")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.jsonwebtoken:jjwt-api:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.13.0")

    developmentOnly("org.springframework.boot:spring-boot-devtools")
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")
    developmentOnly("com.google.googlejavaformat:google-java-format:1.30.0")
    runtimeOnly("org.postgresql:postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

spotless {
    java {
        targetExclude("**/generated/**")
        googleJavaFormat()
    }
}

// OpenAPI Generator Configuration
val generatedSourcesDir = layout.buildDirectory.dir("generated/src/main/java")

openApiGenerate {
    generatorName.set("spring")
    inputSpec.set("$rootDir/src/main/resources/static/openapi.yaml")
    outputDir.set(layout.buildDirectory.dir("generated").get().asFile.path)
    apiPackage.set("com.barclays.eagle_bank_api.api")
    modelPackage.set("com.barclays.eagle_bank_api.model")
    configOptions.set(
        mapOf(
            "interfaceOnly" to "true",
            "skipDefaultInterface" to "true",
            "useTags" to "true",
            "useJakartaEe" to "true",
            "performBeanValidation" to "true",
            "useBeanValidation" to "true"
        )
    )
}

// Add generated sources to source sets
sourceSets {
    main {
        java {
            srcDir(generatedSourcesDir)
        }
    }
}

// Ensure code is generated before compiling
tasks.compileJava {
    dependsOn(tasks.openApiGenerate)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.register("installGitHooks") {
    description = "Install git pre-commit hook (runs spotlessCheck, test, check)"
    group = "git hooks"
    doLast {
        val hooksDir = file(".git/hooks")
        hooksDir.mkdirs()
        val hook = file(".git/hooks/pre-commit")
        hook.writeText(
            """
            #!/bin/sh
            echo "Running pre-commit checks..."
            ./gradlew spotlessCheck || { echo "❌ Spotless check failed. Run './gradlew spotlessApply'"; exit 1; }
            ./gradlew check || { echo "❌ Checks failed"; exit 1; }
            echo "✅ All pre-commit checks passed!"
        """.trimIndent()
        )
        hook.setExecutable(true)
        println("✅ Git pre-commit hook installed! See docs/git-hooks.md for more info")
    }
}

tasks.named("build") { dependsOn("installGitHooks") }
