import groovy.lang.MissingPropertyException

plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "net.foster.testing"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        name = "papermc-repo"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        name = "sonatype"
        url = uri("https://oss.sonatype.org/content/groups/public/")
    }
    maven {
        name = "Aikar"
        url = uri("https://repo.aikar.co/content/groups/aikar/")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.18.2-R0.1-SNAPSHOT")

    implementation("co.aikar:acf-paper:0.5.1-SNAPSHOT") // ACF (Command framework)
}

val targetJavaVersion = 17
java {
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
    if (JavaVersion.current() < javaVersion) {
        toolchain.languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
    }
}

tasks {
    withType<JavaCompile>().configureEach {
        if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible()) {
            options.release.set(targetJavaVersion)
        }
    }

    shadowJar {
        archiveFileName.set("${project.name}-${project.version}.jar")

        relocate("co.airkar.commands", "net.advancius.acf")
        relocate("co.airkar.locales", "net.advancius.locales")
        try {
            if(project.findProperty("destination") != null) {
                destinationDirectory.set(file(project.findProperty("destination").toString()))
            }
        } catch(_: MissingPropertyException) {}
    }

    processResources {
        inputs.property("version", project.version)
        //inputs.property("description", project.description)
        filteringCharset = "UTF-8"
        filesMatching("paper-plugin.yml") {
            expand("version" to project.version)
            //expand("description" to project.description)
        }
    }
}
