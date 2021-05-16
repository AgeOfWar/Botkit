plugins {
    kotlin("jvm") version "1.5.0"
    kotlin("plugin.serialization") version "1.5.0"
    application
    `maven-publish`
}

group = "com.github.ageofwar"
version = "1.0"

application {
    mainClass.set("com.github.ageofwar.botkit.MainKt")
}

repositories {
    mavenCentral()
    jcenter()
    maven(url = "https://jitpack.io")
}

dependencies {
    api("com.github.AgeOfWar:KTelegram:1.0")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.3")
    implementation("org.freemarker:freemarker:2.3.31")
}

tasks {
    register<Jar>("fatJar") {
        manifest {
            attributes("Main-Class" to "com.github.ageofwar.botkit.MainKt")
        }
        includeEmptyDirs = false
        archiveClassifier.set("fat")
        from(configurations.compileClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
        from(sourceSets.main.get().output)
    }
    
    register<Jar>("libFatJar") {
        includeEmptyDirs = false
        archiveClassifier.set("lib")
        exclude("**/botkit/*.class")
        exclude("**/botkit/files/**")
        exclude("config/**")
        exclude("kotlin/**")
        exclude("freemarker/**")
        from(configurations.compileClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
        with(getByName("jar") as CopySpec)
    }
    
    named<Jar>("jar") {
        exclude("**/botkit/*.class")
        exclude("**/botkit/files/**")
        exclude("config/**")
        exclude("freemarker/**")
        includeEmptyDirs = false
    }
    
    named("build") {
        dependsOn("fatJar", "libFatJar")
    }
    
    named<JavaExec>("run") {
        standardInput = System.`in`
        if (project.hasProperty("args")) {
            args = project.properties["args"].toString().split(Regex("\\s"))
        }
    }
    
    withType<GenerateModuleMetadata> {
        enabled = false
    }
    
    compileKotlin {
        kotlinOptions.jvmTarget = "11"
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["kotlin"])
            artifact(tasks["kotlinSourcesJar"])
        }
    }
}