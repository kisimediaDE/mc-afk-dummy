plugins {
    `java-library`
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21"
    id("com.gradleup.shadow") version "9.4.3"
}

group = "com.plugin"
val targetPaper = providers.gradleProperty("targetPaper").getOrElse("26.3")
require(targetPaper in listOf("26.2", "26.3")) { "targetPaper must be 26.2 or 26.3" }
version = if (targetPaper == "26.2") "1.1.0" else "2.1.0"
description = "AFKDummyLimited - Free timed farm dummies for Paper $targetPaper"
layout.buildDirectory.set(layout.projectDirectory.dir("build/$targetPaper"))

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

repositories {
    mavenCentral()
}

dependencies {
    paperweight.paperDevBundle(if (targetPaper == "26.2") "26.2.build.123-stable" else "26.3.build.41-alpha")
    implementation("com.google.code.gson:gson:2.13.1")

    // Test dependencies
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.4")
    testImplementation("org.mockito:mockito-core:5.18.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.18.0")
}

tasks.processResources {
    val props = mapOf("version" to project.version, "description" to (project.description ?: ""), "targetPaper" to targetPaper)
    inputs.properties(props)
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.shadowJar {
    archiveBaseName.set("AFKDummyLimited")
    archiveClassifier.set("")
    from(files("LICENSE", "NOTICE", "THIRD_PARTY_NOTICES.md")) { into("META-INF") }
    from("licenses") { into("META-INF/licenses") }
    minimize()
}

tasks.test {
    useJUnitPlatform()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

// Isolated server probe; never packaged in the distributable plugin.
val integration by sourceSets.creating {
    compileClasspath += sourceSets.main.get().output + sourceSets.main.get().compileClasspath
    runtimeClasspath += output + compileClasspath
}
tasks.named<ProcessResources>("processIntegrationResources") {
    inputs.property("targetPaper", targetPaper)
    filesMatching("plugin.yml") { expand(mapOf("targetPaper" to targetPaper)) }
}
tasks.register<Jar>("integrationJar") {
    archiveBaseName.set("AFKDummy-Probe")
    from(integration.output)
}
