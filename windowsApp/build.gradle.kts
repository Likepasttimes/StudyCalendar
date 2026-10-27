plugins {
    id("org.jetbrains.kotlin.jvm")
    application
}

group = "com.learningcalendar.desktop"
version = "1.4.1"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    implementation("org.json:json:20240303")
    testImplementation(kotlin("test"))
}

application {
    mainClass.set("com.learningcalendar.desktop.MainKt")
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

tasks.register<Jar>("windowsFatJar") {
    group = "distribution"
    description = "Builds the self-contained Windows desktop application JAR."
    archiveFileName.set("LearningCalendarDesktop.jar")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes["Main-Class"] = "com.learningcalendar.desktop.MainKt"
    }
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().map { dependency ->
            if (dependency.isDirectory) dependency else zipTree(dependency)
        }
    }) {
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    }
}

tasks.named("build") {
    dependsOn("windowsFatJar")
}
