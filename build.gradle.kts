import com.github.spotbugs.SpotBugsTask
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version Version.KOTLIN
    id("org.jetbrains.dokka") version Version.DOKKA
    idea

    signing
    `maven-publish`
    id("com.github.spotbugs") version Version.SPOTBUGS
}

group = "com.github.bjoernpetersen"
version = "0.16.0-SNAPSHOT"

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

val processResources by tasks.getting(ProcessResources::class) {
    filesMatching("**/version.properties") {
        filter {
            it.replace("%APP_VERSION%", version.toString())
        }
    }
}

spotbugs {
    isIgnoreFailures = true
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
        classifier = "javadoc"
        from("$buildDir/javadoc")
    }

    @Suppress("UNUSED_VARIABLE")
    val sourcesJar by creating(Jar::class) {
        classifier = "sources"
        from(sourceSets["main"].allSource)
    }

    "compileKotlin"(KotlinCompile::class) {
        kotlinOptions.jvmTarget = "1.8"
    }

    "compileTestKotlin"(KotlinCompile::class) {
        kotlinOptions.jvmTarget = "1.8"
    }

    "test"(Test::class) {
        useJUnitPlatform()
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    implementation(
        group = "io.github.microutils",
        name = "kotlin-logging",
        version = Version.KOTLIN_LOGGING)
    api(group = "com.google.guava", name = "guava", version = Version.GUAVA)
    api(group = "com.google.inject", name = "guice", version = Version.GUICE)

    implementation(group = "org.xerial", name = "sqlite-jdbc", version = Version.SQLITE)

    implementation(group = "org.mindrot", name = "jbcrypt", version = Version.JBCRYPT)
    implementation(
        group = "io.jsonwebtoken",
        name = "jjwt",
        version = Version.JJWT)
    api(group = "com.github.zafarkhaja", name = "java-semver",
        version = Version.JAVA_SEMVER)
    api(group = "io.reactivex.rxjava2", name = "rxjava", version = Version.RX_JAVA)
    api(group = "io.reactivex.rxjava2", name = "rxkotlin", version = Version.RX_KOTLIN)

    compileOnly(group = "com.google.android", name = "android", version = Version.ANDROID)

    testImplementation(group = "org.slf4j", name = "slf4j-simple", version = Version.SLF4J)
    testImplementation(
        group = "org.junit.jupiter",
        name = "junit-jupiter-api",
        version = Version.JUNIT)
    testImplementation(
        group = "org.junit.jupiter",
        name = "junit-jupiter-engine",
        version = Version.JUNIT)
    testImplementation(
        group = "name.falgout.jeffrey.testing.junit5",
        name = "guice-extension",
        version = Version.JUNIT_GUICE)
    testImplementation(group = "io.mockk", name = "mockk", version = Version.MOCK_K)
    testImplementation(group = "org.assertj", name = "assertj-core", version = Version.ASSERT_J)
}

publishing {
    publications {
        create("Maven", MavenPublication::class) {
            from(components["java"])
            artifact(tasks.getByName("javadocJar"))
            artifact(tasks.getByName("sourcesJar"))

            pom {
                description.set(
                    "Core library of JMusicBot, which plays music from various providers.")
                url.set("https://github.com/BjoernPetersen/jmusicbot")

                licenses {
                    license {
                        name.set("MIT")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                scm {
                    connection.set("scm:git:https://github.com/BjoernPetersen/JMusicBot.git")
                    developerConnection.set("scm:git:git@github.com:BjoernPetersen/JMusicBot.git")
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
                url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl
                else releasesRepoUrl)
                credentials {
                    username = project.properties["ossrh.username"]?.toString()
                    password = project.properties["ossrh.password"]?.toString()
                }
            }
        }
    }
}

tasks.withType(Jar::class) {
    from(project.projectDir) {
        include("LICENSE")
    }
}

signing {
    sign(publishing.publications.getByName("Maven"))
}
