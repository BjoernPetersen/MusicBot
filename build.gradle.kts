import com.diffplug.spotless.LineEnding
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.diffplug.spotless") version "5.12.1"
    id("io.gitlab.arturbosch.detekt") version "1.14.1"
    jacoco

    id("com.github.ben-manes.versions") version "0.38.0"

    kotlin("jvm") version "1.4.32"
    `java-library`

    id("org.jetbrains.dokka") version "1.4.30"
    idea

    signing
    `maven-publish`
}

group = "com.github.bjoernpetersen"
version = "0.26.0-SNAPSHOT"

fun isSnapshot() = version.toString().endsWith("SNAPSHOT")

repositories {
    mavenCentral()
    maven(url = "https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven") {
        content {
            includeModule("org.jetbrains.kotlinx", "kotlinx-html-jvm")
        }
    }
}

idea {
    module {
        isDownloadJavadoc = true
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
    withSourcesJar()
}

spotless {
    kotlin {
        ktlint(libs.versions.ktlint.get())
        lineEndings = LineEnding.UNIX
        endWithNewline()
    }
    kotlinGradle {
        ktlint(libs.versions.ktlint.get())
        lineEndings = LineEnding.UNIX
        endWithNewline()
    }
    format("markdown") {
        target("**/*.md")
        lineEndings = LineEnding.UNIX
        endWithNewline()
    }
}

detekt {
    config = files("$rootDir/buildConfig/detekt.yml")
    buildUponDefaultConfig = true
}

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

tasks {
    create<Jar>("javadocJar") {
        dependsOn("dokkaJavadoc")
        archiveClassifier.set("javadoc")
        from("$buildDir/dokka/javadoc")
    }

    @Suppress("UnstableApiUsage")
    "processResources"(ProcessResources::class) {
        filesMatching("**/version.properties") {
            filter {
                it.replace("%APP_VERSION%", version.toString())
            }
        }
    }

    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "1.8"
            freeCompilerArgs = listOf(
                "-Xuse-experimental=kotlin.Experimental",
                "-Xopt-in=kotlin.RequiresOptIn",
                "-Xnew-inference"
            )
        }
    }

    withType<Test> {
        useJUnitPlatform()
    }

    jacocoTestReport {
        reports {
            html.isEnabled = true
            xml.isEnabled = true
            csv.isEnabled = false
        }
    }

    check {
        finalizedBy("jacocoTestReport")
    }

    withType<Jar> {
        from(project.projectDir) {
            include("LICENSE")
        }
    }

    withType<GenerateModuleMetadata> {
        enabled = !isSnapshot()
    }

    dependencyUpdates {
        rejectVersionIf {
            val version = candidate.version
            isUnstable(version, currentVersion) || isWrongPlatform(version, currentVersion)
        }
    }
}

dependencies {
    api(kotlin("stdlib-jdk8"))
    api(libs.coroutines.core)
    implementation(libs.coroutines.jdk8)
    api(kotlin("reflect"))

    api(libs.logging.slf4j.api)
    api(libs.logging.kotlin)

    api(libs.guava)
    api(
        group = "com.google.inject",
        name = "guice",
        version = libs.versions.guice.get(),
        classifier = "no_aop",
    )

    implementation(libs.jbcrypt)

    implementation(libs.jwt)

    // Ktor for any HTTP stuff
    api(libs.bundles.ktor)

    api(libs.semver)

    testRuntimeOnly(libs.logging.slf4j.simple)
    testRuntimeOnly(libs.sqlite)
    testRuntimeOnly(libs.junit.engine)
    testImplementation(libs.junit.api)
    testImplementation(libs.junit.guice)
    testImplementation(libs.mockk)
    testImplementation(libs.assertj)
    testImplementation(libs.equalsverifier)
}

publishing {
    publications {
        create("Maven", MavenPublication::class) {
            from(components["java"])
            artifact(tasks.getByName("javadocJar"))

            pom {
                name.set("MusicBot")
                description
                    .set("Core library of MusicBot, which plays music from various providers.")
                url.set("https://github.com/BjoernPetersen/MusicBot")

                licenses {
                    license {
                        name.set("MIT")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                scm {
                    connection.set("scm:git:https://github.com/BjoernPetersen/MusicBot.git")
                    developerConnection.set("scm:git:git@github.com:BjoernPetersen/MusicBot.git")
                    url.set("https://github.com/BjoernPetersen/MusicBot")
                }

                developers {
                    developer {
                        id.set("BjoernPetersen")
                        name.set("Björn Petersen")
                        email.set("pheasn@gmail.com")
                        url.set("https://github.com/BjoernPetersen")
                    }
                }
            }
        }
        repositories {
            maven {
                val releasesRepoUrl = "https://oss.sonatype.org/service/local/staging/deploy/maven2"
                val snapshotsRepoUrl = "https://oss.sonatype.org/content/repositories/snapshots"
                url = uri(
                    if (isSnapshot()) snapshotsRepoUrl
                    else releasesRepoUrl
                )
                credentials {
                    username = project.properties["ossrh.username"]?.toString()
                    password = project.properties["ossrh.password"]?.toString()
                }
            }
        }
    }
}

signing {
    sign(publishing.publications.getByName("Maven"))
}

fun isUnstable(version: String, currentVersion: String): Boolean {
    val lowerVersion = version.toLowerCase()
    val lowerCurrentVersion = currentVersion.toLowerCase()
    return listOf(
        "alpha",
        "beta",
        "rc",
        "m",
        "eap"
    ).any { it in lowerVersion && it !in lowerCurrentVersion }
}

fun isWrongPlatform(version: String, currentVersion: String): Boolean {
    return "android" in currentVersion && "android" !in version
}
