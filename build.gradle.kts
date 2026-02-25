plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.11.0"
    id("idea")
}

group = "com.en_circle.slt"
version = "0.5.5"

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdea("2025.3.2")
        // Spellchecker was removed from bundled plugins in 2025.3
    }
    implementation("org.awaitility:awaitility:4.2.0")
    implementation("org.watertemplate:watertemplate-engine:1.2.2")
    implementation("org.rauschig:jarchivelib:1.2.0")
    implementation("org.jsoup:jsoup:1.16.1")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testImplementation("junit:junit:4.13.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")
}

sourceSets {
    main {
        java {
            srcDirs("src/main/gen")
        }
    }
}

tasks {
    val sltZip by registering(Zip::class) {
        from("src/main/lisp")
        archiveFileName.set("slt.zip")
        destinationDirectory.set(File("build/resources/main"))
        include("**/*")

        doFirst {
            println("Zipping SLT to " + destinationDirectory.get())
        }
        eachFile{
            println("Zipping $this")
        }
        doLast {
            println("Done Zipping SLT")
        }

        outputs.upToDateWhen {
            false
        }
    }
    sltZip.get().mustRunAfter(processResources)

    instrumentedJar {
        dependsOn(sltZip)
    }
    jar {
        dependsOn(sltZip)
    }

    // Fix for Gradle 8+ task dependency validation
    named("compileTestJava") {
        dependsOn(sltZip)
    }

    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }

    patchPluginXml {
        sinceBuild.set("253")
        untilBuild.set("253.*")
    }

    signPlugin {
        try {
            certificateChain.set(File("./signcerts/chain.crt").readText(Charsets.UTF_8))
            privateKey.set(File("./signcerts/private.pem").readText(Charsets.UTF_8))
            password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
        } catch (_: Exception) {

        }
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }

    // Disable searchable options index build (not needed for this plugin)
    buildSearchableOptions {
        enabled = false
    }
}

