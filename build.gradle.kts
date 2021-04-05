plugins {
    kotlin("jvm") version "1.4.30"
    kotlin("plugin.serialization") version "1.4.30"
    application
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
    api("com.github.AgeOfWar:KTelegram:0.2")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.3")
    implementation("com.github.AgeOfWar:JavaStringTemplate:1.0")
}

tasks {
    withType<Jar> {
        manifest {
            attributes("Main-Class" to "com.github.ageofwar.botkit.MainKt")
        }
        
        from(configurations.compileClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
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
