import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

repositories {
    maven(url = "https://repo.papermc.io/repository/maven-public/") {
        name = "papermc"
    }
    mavenCentral()
}

dependencies {
    implementation("com.velocitypowered:velocity-api:3.1.0")
    implementation("net.kyori:adventure-text-minimessage:4.21.0")
    implementation("jakarta.json:jakarta.json-api:2.1.3")
    implementation("org.eclipse.parsson:parsson:1.1.7")
    implementation("com.electronwill.night-config:toml:3.8.2")
    implementation("com.zaxxer:HikariCP:6.3.0")
    implementation("com.mysql:mysql-connector-j:9.3.0")
}

tasks {
    withType<ShadowJar>() {
        mergeServiceFiles()
        dependencies {
            include(dependency("com.zaxxer:HikariCP"))
            include(dependency("com.mysql:mysql-connector-j"))
            include(dependency("jakarta.json:jakarta.json-api"))
            include(dependency("org.eclipse.parsson:parsson"))
            include(dependency("commons-logging:commons-logging"))
        }
    }

    build {
        dependsOn(shadowJar)
    }
}

group = "com.matthewcash.network"
version = "3.0.0"
description = "Network Bans"
java.sourceCompatibility = JavaVersion.VERSION_17
