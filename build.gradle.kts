import com.diffplug.spotless.LineEnding
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.diffplug.spotless") version Plugin.SPOTLESS
    id("io.gitlab.arturbosch.detekt") version Plugin.DETEKT
    jacoco

    id("com.github.ben-manes.versions") version Plugin.VERSIONS

    kotlin("jvm") version Plugin.KOTLIN
    `java-library`

    id("org.jetbrains.dokka") version Plugin.DOKKA
    idea

    signing
    `maven-publish`
}

group = "com.github.bjoernpetersen"
version = "0.26.0-SNAPSHOT"

fun isSnapshot() = version.toString().endsWith("SNAPSHOT")

repositories {
    jcenter()
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
        ktlint(Plugin.KTLINT)
        lineEndings = LineEnding.UNIX
        endWithNewline()
    }
    kotlinGradle {
        ktlint(Plugin.KTLINT)
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
    toolVersion = Plugin.DETEKT
    config = files("$rootDir/buildConfig/detekt.yml")
    buildUponDefaultConfig = true
}

jacoco {
    toolVersion = Plugin.JACOCO
}

tasks {
    create<Jar>("javadocJar") {
        dependsOn("dokkaJavadoc")
        archiveClassifier.set("javadoc")
        from("$buildDir/dokka/javadoc")
    }

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
    api(
        group = "org.jetbrains.kotlinx",
        name = "kotlinx-coroutines-core",
        version = Lib.KOTLIN_COROUTINES
    )
    implementation(
        group = "org.jetbrains.kotlinx",
        name = "kotlinx-coroutines-jdk8",
        version = Lib.KOTLIN_COROUTINES
    )
    api(kotlin("reflect"))
    api(
        group = "io.github.microutils",
        name = "kotlin-logging",
        version = Lib.KOTLIN_LOGGING
    )

    api(group = "org.slf4j", name = "slf4j-api", version = Lib.SLF4J)
    api(group = "com.google.guava", name = "guava", version = Lib.GUAVA)
    api(group = "com.google.inject", name = "guice", version = Lib.GUICE, classifier = "no_aop")

    implementation(group = "org.mindrot", name = "jbcrypt", version = Lib.JBCRYPT)

    implementation(
        group = "com.auth0",
        name = "java-jwt",
        version = Lib.JJWT
    )

    api(
        group = "com.github.zafarkhaja",
        name = "java-semver",
        version = Lib.JAVA_SEMVER
    )

    testRuntimeOnly(group = "org.slf4j", name = "slf4j-simple", version = Lib.SLF4J)
    testRuntimeOnly(group = "org.xerial", name = "sqlite-jdbc", version = Lib.SQLITE)
    testRuntimeOnly(
        group = "org.junit.jupiter",
        name = "junit-jupiter-engine",
        version = Lib.JUNIT
    )
    testImplementation(
        group = "org.junit.jupiter",
        name = "junit-jupiter-api",
        version = Lib.JUNIT
    )
    testImplementation(
        group = "name.falgout.jeffrey.testing.junit5",
        name = "guice-extension",
        version = Lib.JUNIT_GUICE
    )
    testImplementation(group = "io.mockk", name = "mockk", version = Lib.MOCK_K)
    testImplementation(group = "org.assertj", name = "assertj-core", version = Lib.ASSERT_J)
    testImplementation(
        group = "nl.jqno.equalsverifier",
        name = "equalsverifier",
        version = Lib.EQUALSVERIFIER
    )
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
                // change to point to your repo, e.g. http://my.org/repo
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
