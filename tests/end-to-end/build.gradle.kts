plugins {
    java
}

dependencies {
    testImplementation(platform("org.springframework.boot:spring-boot-dependencies:4.2.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("tools.jackson.core:jackson-databind")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
