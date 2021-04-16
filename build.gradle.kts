plugins {
    kotlin("jvm") version "1.4.30"
    kotlin("plugin.serialization") version "1.4.30"
    application
    `maven-publish`
}

group = "com.github.ageofwar"
version = "0.1"

application {
    mainClass.set("com.github.ageofwar.botkit.MainKt")
}

repositories {
    mavenCentral()
    jcenter()
    maven(url = "https://jitpack.io")
}

dependencies {
    api("com.github.AgeOfWar:KTelegram:0.3")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.3")
    implementation("com.github.AgeOfWar:JavaStringTemplate:1.0")
}

tasks {
    named<Jar>("jar") {
        manifest {
            attributes("Main-Class" to "com.github.ageofwar.botkit.MainKt")
        }
        
        from(configurations.compileClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    }
    
    register<Jar>("libJar") {
        includeEmptyDirs = false
        archiveClassifier.set("lib")
        exclude("**/botkit/*.class")
        exclude("**/botkit/files/**")
        exclude("kotlin/**")
        exclude("**/javastringtemplate/**")
        with(getByName("jar") as CopySpec)
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
        create<MavenPublication>("maven") {
            artifact(tasks["libJar"]) {
                this.classifier = ""
            }
            artifact(tasks["kotlinSourcesJar"])
        }
    }
}