pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("org.springframework.boot") version "4.2.0"
        id("io.quarkus") version "3.40.1.2"
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "bank-payement"
include("services:payment-service", "services:account-service", "services:biller-service")
