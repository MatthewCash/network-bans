import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

repositories {
    maven(url = "https://repo.papermc.io/repository/maven-public/") {
        name = "papermc"
    }
    mavenCentral()
}

dependencies {
    implementation("com.velocitypowered:velocity-api:3.0.1")
    annotationProcessor("com.velocitypowered:velocity-api:3.0.1")
    implementation("net.kyori:adventure-text-minimessage:4.11.0")
    implementation("org.java-websocket:Java-WebSocket:1.5.3")
    implementation("javax.json:javax.json-api:1.1")
    implementation("com.electronwill.night-config:toml:3.6.0")
    implementation("org.apache.httpcomponents:httpclient:4.5.13")
    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("mysql:mysql-connector-java:8.0.30")
}

tasks {
    withType<ShadowJar>() {
        dependencies {
            include(dependency("com.zaxxer:HikariCP"))
            include(dependency("mysql:mysql-connector-java"))
        }
        minimize {
            exclude(dependency("mysql:mysql-connector-java"))
        }
    }
    
    build {
        dependsOn(shadowJar)
    }
}

group = "com.matthewcash.network"
version = "2.0.0"
description = "Network Bans"
java.sourceCompatibility = JavaVersion.VERSION_17
