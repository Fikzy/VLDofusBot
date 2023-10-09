plugins {
    kotlin("jvm") version "1.9.0"
}

repositories {
    mavenLocal()
    maven {
        url = uri("https://repo.maven.apache.org/maven2/")
    }
}

group = "fr.lewon"
version = "1.4.0"
description = "$group:${project.name}"
java.sourceCompatibility = JavaVersion.VERSION_11