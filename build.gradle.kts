import com.diffplug.spotless.LineEnding
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.diffplug.gradle.spotless") version Plugin.SPOTLESS
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
version = "0.23.0-SNAPSHOT"

repositories {
    jcenter()
}

idea {
    module {
        isDownloadJavadoc = true
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

spotless {
    kotlin {
        ktlint()
        lineEndings = LineEnding.UNIX
        endWithNewline()
    }
    kotlinGradle {
        ktlint()
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
    config = files("buildSrc/src/main/resources/detekt.yml")
    buildUponDefaultConfig = true
}

jacoco {
    toolVersion = Plugin.JACOCO
}

tasks {
    "dokka"(DokkaTask::class) {
        outputFormat = "html"
        outputDirectory = "$buildDir/kdoc"
    }

    @Suppress("UNUSED_VARIABLE")
    val dokkaJavadoc by creating(DokkaTask::class) {
        outputFormat = "javadoc"
        outputDirectory = "$buildDir/javadoc"
    }

    @Suppress("UNUSED_VARIABLE")
    val javadocJar by creating(Jar::class) {
        dependsOn("dokkaJavadoc")
        archiveClassifier.set("javadoc")
        from("$buildDir/javadoc")
    }

    @Suppress("UNUSED_VARIABLE")
    val sourcesJar by creating(Jar::class) {
        archiveClassifier.set("sources")
        from(sourceSets["main"].allSource)
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
                "-Xuse-experimental=kotlin.Experimental"
            )
        }
    }

    withType<Test> {
        useJUnitPlatform()
    }

    jacocoTestReport {
        reports {
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

    implementation(group = "org.xerial", name = "sqlite-jdbc", version = Lib.SQLITE)

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

    testImplementation(group = "org.slf4j", name = "slf4j-simple", version = Lib.SLF4J)
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
}

publishing {
    publications {
        create("Maven", MavenPublication::class) {
            from(components["java"])
            artifact(tasks.getByName("javadocJar"))
            artifact(tasks.getByName("sourcesJar"))

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
                    if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl
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
