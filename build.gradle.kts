import io.papermc.paperweight.tasks.ReobfJarTask

plugins {
    kotlin("jvm") version "1.9.23"
    id("io.papermc.paperweight.userdev") version "1.7.2"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.kwonpop.companylife"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    paperweight.paperDevBundle("1.21.10-R0.1-SNAPSHOT")

    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.flywaydb:flyway-core:10.15.0")
    implementation("org.xerial:sqlite-jdbc:3.45.3.0")
    implementation("mysql:mysql-connector-j:8.4.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.1")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.17.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.1")
    implementation("org.spongepowered:configurate-yaml:4.1.2")
    implementation("net.kyori:adventure-text-minimessage:4.16.0")
    implementation("net.kyori:adventure-platform-bukkit:4.3.2")

    compileOnly("com.github.MilkBowl:VaultAPI:1.7")
    compileOnly("net.luckperms:api:5.4")
    compileOnly("me.clip:placeholderapi:2.11.5")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
}

tasks {
    test {
        useJUnitPlatform()
    }

    shadowJar {
        archiveClassifier.set("")
        minimize()
        relocate("kotlin", "com.kwonpop.companylife.libs.kotlin")
        relocate("kotlinx", "com.kwonpop.companylife.libs.kotlinx")
        relocate("org.flywaydb", "com.kwonpop.companylife.libs.flyway")
        relocate("com.zaxxer.hikari", "com.kwonpop.companylife.libs.hikari")
        relocate("com.fasterxml.jackson", "com.kwonpop.companylife.libs.jackson")
    }

    withType<ReobfJarTask>().configureEach {
        dependsOn(shadowJar)
        inputJar.set(shadowJar.flatMap { it.archiveFile })
    }
}
