plugins {
    java
    id("io.quarkus")
}

dependencies {
    implementation(enforcedPlatform("io.quarkus.platform:quarkus-bom:3.40.1.2"))
    implementation("io.quarkus:quarkus-rest-jackson")
    implementation("io.quarkus:quarkus-hibernate-validator")
    implementation("io.quarkus:quarkus-mongodb-client")
    implementation("io.quarkus:quarkus-smallrye-health")
    testImplementation("io.quarkus:quarkus-junit")
    testImplementation(platform("org.testcontainers:testcontainers-bom:2.0.5"))
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-mongodb")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    exclude("**/MongoTransactionIT.class")
}

tasks.register<Test>("mongoTransactionTest") {
    description = "Checks a transaction against the Compose replica set using the Quarkus MongoDB client."
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    include("**/MongoTransactionIT.class")
    outputs.upToDateWhen { false }
}
