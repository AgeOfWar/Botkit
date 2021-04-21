plugins {
    kotlin("jvm") version "1.4.30"
    kotlin("plugin.serialization") version "1.4.30"
    application
    `maven-publish`
}

group = "com.github.ageofwar"
version = "0.3"

application {
    mainClass.set("com.github.ageofwar.botkit.MainKt")
}

repositories {
    mavenCentral()
    jcenter()
    maven(url = "https://jitpack.io")
}

dependencies {
    api("com.github.AgeOfWar:KTelegram:0.4")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.3")
    api("com.github.AgeOfWar:JavaStringTemplate:1.0")
}

tasks {
    named<Jar>("jar") {
        manifest {
            attributes("Main-Class" to "com.github.ageofwar.botkit.MainKt")
        }
        includeEmptyDirs = false
        enabled = false
        archiveClassifier.set("ignored")
    }
    
    register<Jar>("fatJar") {
        archiveClassifier.set("fat")
        from(configurations.compileClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
        with(getByName("jar") as CopySpec)
    }
    
    register<Jar>("libJar") {
        archiveClassifier.set("lib")
        include("**/botkit/plugin/**")
        with(getByName("jar") as CopySpec)
    }
    
    register<Jar>("libFatJar") {
        archiveClassifier.set("lib-fat")
        exclude("**/botkit/*.class")
        exclude("**/botkit/files/**")
        exclude("kotlin/**")
        from(configurations.compileClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
        with(getByName("jar") as CopySpec)
    }
    
    named("build") {
        dependsOn("fatJar", "libJar", "libFatJar")
    }
    
    named<JavaExec>("run") {
        standardInput = System.`in`
        if (project.hasProperty("args")) {
            args = project.properties["args"].toString().split(Regex("\\s"))
        }
    }
    
    compileKotlin {
        kotlinOptions.jvmTarget = "11"
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["kotlin"])
            artifact(tasks["libJar"]) {
                classifier = ""
            }
            artifact(tasks["kotlinSourcesJar"])
        }
    }
}