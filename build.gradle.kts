plugins {
	java
	id("org.springframework.boot") version "3.5.6"
	id("io.spring.dependency-management") version "1.1.7"
	id("org.openapi.generator") version "7.10.0"
	id("io.freefair.lombok") version "9.0.0"
}

group = "com.barclays"
version = "0.0.1-SNAPSHOT"
description = "REST API for Eagle Bank - A banking system supporting user management, account operations, and transaction processing with JWT authentication"

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
	implementation("org.flywaydb:flyway-core")
	implementation("org.flywaydb:flyway-database-postgresql")
	implementation("jakarta.annotation:jakarta.annotation-api")
	implementation("org.openapitools:jackson-databind-nullable:0.2.6")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.13")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    developmentOnly("org.springframework.boot:spring-boot-devtools")
	developmentOnly("org.springframework.boot:spring-boot-docker-compose")
	runtimeOnly("org.postgresql:postgresql")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.boot:spring-boot-testcontainers")
	testImplementation("org.springframework.security:spring-security-test")
	testImplementation("org.testcontainers:junit-jupiter")
	testImplementation("org.testcontainers:postgresql")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// OpenAPI Generator Configuration
val generatedSourcesDir = layout.buildDirectory.dir("generated/src/main/java")

openApiGenerate {
	generatorName.set("spring")
	inputSpec.set("$rootDir/src/main/resources/static/openapi.yaml")
	outputDir.set(layout.buildDirectory.dir("generated").get().asFile.path)
	apiPackage.set("com.barclays.eagle_bank_api.api")
	modelPackage.set("com.barclays.eagle_bank_api.model")
	configOptions.set(mapOf(
        "interfaceOnly" to "true",
		"skipDefaultInterface" to "true",
		"useTags" to "true",
		"useJakartaEe" to "true",
		"performBeanValidation" to "true",
		"useBeanValidation" to "true"
	))
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
