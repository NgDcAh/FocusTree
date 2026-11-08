import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    application
}

group = "com.focustree"
version = "1.0.0"

kotlin {
    jvmToolchain(17)
    compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
}

application {
    mainClass.set("com.focustree.server.ServerApplicationKt")
}

val postgresIntegrationTest by sourceSets.creating {
    compileClasspath += sourceSets.main.get().output + configurations.testRuntimeClasspath.get()
    runtimeClasspath += output + compileClasspath
}

configurations[postgresIntegrationTest.implementationConfigurationName]
    .extendsFrom(configurations.testImplementation.get())
configurations[postgresIntegrationTest.runtimeOnlyConfigurationName]
    .extendsFrom(configurations.testRuntimeOnly.get())

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgresql)
    implementation(libs.postgresql)
    implementation(libs.hikari)

    testImplementation(libs.junit)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlinx.serialization.json)

    add(postgresIntegrationTest.implementationConfigurationName, sourceSets.main.get().output)
    add(postgresIntegrationTest.implementationConfigurationName, libs.junit)
    add(postgresIntegrationTest.implementationConfigurationName, libs.testcontainers.postgresql)
}

tasks.register<Test>("postgresIntegrationTest") {
    description = "Runs PostgreSQL integration tests against a disposable Testcontainers database."
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    testClassesDirs = postgresIntegrationTest.output.classesDirs
    classpath = postgresIntegrationTest.runtimeClasspath
    shouldRunAfter(tasks.test)
}

tasks.check {
    dependsOn("postgresIntegrationTest")
}
