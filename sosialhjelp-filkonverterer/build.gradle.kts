version = "1.0.0-SNAPSHOT"

plugins {
    alias(libs.plugins.jvm)
    alias(libs.plugins.kotlin.plugin.spring)
    alias(libs.plugins.spotless)

    // Apply the java-library plugin for API and implementation separation.
    `java-library`
    `maven-publish`
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(libs.spring.boot.starter.webflux)
    implementation(libs.spring.boot.autoconfigure)
    implementation(libs.spring.boot.starter.actuator)

    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.apache.tika)

    implementation(libs.simpleclient)
    implementation(libs.micrometer.registry.prometheus)

    annotationProcessor(libs.spring.boot.configuration.processor)

    testImplementation(libs.kotlinx.coroutines.test)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            pom {
                name = "Sosialhjelp filkonvertering"
                description = "Et spring-boot bibliotek for å konvertere filer til PDF via gotenberg"
                url = "https://github.com/navikt/sosialhjelp-filkonverterer"
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        organization.set("NAV (Arbeids- og velferdsdirektoratet) - The Norwegian Labour and Welfare Administration")
                        organizationUrl.set("https://www.nav.no")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/navikt/${rootProject.name}.git")
                    developerConnection.set("scm:git:ssh://github.com/navikt/${rootProject.name}.git")
                    url.set("https://github.com/navikt/${rootProject.name}")
                }
            }
            groupId = "no.nav.sosialhjelp"
            artifactId = rootProject.name
            version = project.version.toString()

            from(components["java"])
        }
    }
}

// spotless {
//    kotlin {
//        ktlint(libs.versions.ktlint.get())
//    }
//
//    kotlinGradle {
//        ktlint(libs.versions.ktlint.get())
//    }
// }

testing {
    suites {
        // Configure the built-in test suite
        val test by getting(JvmTestSuite::class) {
            // Use Kotlin Test test framework
            useKotlinTest("1.9.22")
        }
    }
}

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

val installPreCommitHook =
    tasks.register("installPreCommitHook", Copy::class) {
        group = "Setup"
        description = "Copy pre-commit git hook into repository"
        from(File(rootProject.rootDir, "scripts/pre-commit"))
        into(File(rootProject.rootDir, ".git/hooks"))
        fileMode = 0b111101101
        dirMode = 0b1010001010
    }

tasks.build.get().dependsOn(installPreCommitHook)
