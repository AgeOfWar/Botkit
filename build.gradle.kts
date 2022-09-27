plugins {
    kotlin("jvm") version "1.7.10"
    kotlin("plugin.serialization") version "1.7.10"
    application
    `maven-publish`
}

group = "com.github.ageofwar"
version = "2.8.3"

application {
    mainClass.set("com.github.ageofwar.botkit.MainKt")
}

repositories {
    mavenCentral()
    jcenter()
    maven(url = "https://jitpack.io")
}

dependencies {
    api("com.github.AgeOfWar:KTelegram:1.7.7")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.freemarker:freemarker:2.3.31")
    implementation("org.slf4j:slf4j-nop:2.0.1")
}

tasks {
    register<Jar>("fatJar") {
        manifest {
            attributes("Main-Class" to "com.github.ageofwar.botkit.MainKt")
        }
        includeEmptyDirs = false
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        archiveClassifier.set("fat")
        from(configurations.compileClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
        from(sourceSets.main.get().output)
    }
    
    register<Jar>("libFatJar") {
        includeEmptyDirs = false
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        archiveClassifier.set("lib")
        exclude("**/botkit/*.class")
        exclude("**/botkit/files/**")
        exclude("config/**")
        exclude("kotlin/**")
        exclude("freemarker/**")
        exclude("org/slf4j/**")
        from(configurations.compileClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
        with(getByName("jar") as CopySpec)
    }
    
    named<Jar>("jar") {
        exclude("**/botkit/*.class")
        exclude("**/botkit/files/**")
        exclude("config/**")
        includeEmptyDirs = false
    }
    
    named("build") {
        dependsOn("fatJar", "libFatJar", "kotlinSourcesJar")
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