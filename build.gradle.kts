subprojects {
    group = "bankpayment"

    plugins.withType<JavaPlugin> {
        extensions.configure<JavaPluginExtension> {
            toolchain.languageVersion.set(JavaLanguageVersion.of(27))
        }
        dependencyLocking {
            lockAllConfigurations()
        }
        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
        }
    }
}
