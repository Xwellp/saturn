plugins {
    id("fabric-loom") version "1.9-SNAPSHOT"
    kotlin("jvm")
}

base {
    archivesName = properties["archives_base_name"] as String
    version = properties["mod_version"] as String
    group = properties["maven_group"] as String
}

repositories {
    mavenCentral()
    maven { url = uri("https://maven.google.com") }
    maven {
        name = "meteor-maven"
        url = uri("https://maven.meteordev.org/releases")
    }
    maven {
        name = "meteor-maven-snapshots"
        url = uri("https://maven.meteordev.org/snapshots")
    }

    maven {
        name = "Jitpack"
        url = uri("https://jitpack.io")
    }
    maven {
        name = "TerraformersMC"
        url = uri("https://maven.terraformersmc.com/")
    }
    maven {
        name = "ViaVersion"
        url = uri("https://repo.viaversion.com/")
    }
    maven {
        name = "modrinth"
        url = uri("https://api.modrinth.com/maven")
    }
    maven {
        name = "OpenCollab Snapshots"
        url = uri("https://repo.opencollab.dev/maven-snapshots/")
    }
    maven {
        name = "Lenni0451"
        url = uri("https://maven.lenni0451.net/everything")
    }

}

dependencies {
    // Fabric
    minecraft("com.mojang:minecraft:${properties["minecraft_version"] as String}")
    mappings("net.fabricmc:yarn:${properties["yarn_mappings"] as String}:v2")
    modImplementation("net.fabricmc:fabric-loader:${properties["loader_version"] as String}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:0.102.0+1.21")
    modImplementation("maven.modrinth:sodium:${properties["sodium_version"] as String}")
    modImplementation("maven.modrinth:lithium:${properties["lithium_version"] as String}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("com.google.j2objc:j2objc-annotations:1.3")
    implementation("io.netty:netty-handler-proxy:4.1.97.Final")
    implementation("org.ahocorasick:ahocorasick:0.6.3")
    implementation("com.vdurmont:semver4j:3.1.0")
    implementation("net.fabricmc:tiny-mappings-parser:0.3.0+build.17")
    implementation("org.graalvm.polyglot:polyglot:24.0.2")
    implementation("org.graalvm.polyglot:js-community:24.0.2")
    implementation("org.jetbrains.kotlin.jvm:org.jetbrains.kotlin.jvm.gradle.plugin:2.1.0")
    implementation("org.graalvm.polyglot:tools-community:24.0.2")
    implementation("com.github.CCBlueX:DiscordIPC:4.0.0")
    implementation("com.github.CCBlueX:mc-authlib:${properties["mc_authlib_version"] as String}")
    implementation("com.github.CCBlueX:mcef:${properties["mcef_version"] as String}")
    implementation("org.apache.commons:commons-compress:1.27.1")
    implementation("com.github.CCBlueX:netty-httpserver:2.1.1")
    implementation("com.github.CCBlueX:DiscordIPC:4.0.0")


    // Meteor
    modImplementation("meteordevelopment:meteor-client:0.5.8")


    implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.14")


    // Baritone (https://github.com/MeteorDevelopment/baritone)
    modCompileOnly("meteordevelopment:baritone:${properties["baritone_version"] as String}-SNAPSHOT")
    implementation(kotlin("stdlib-jdk8"))
}

loom {
    accessWidenerPath = file("src/main/resources/saturn.accesswidener")
}

tasks {
    processResources {
        val propertyMap = mapOf(
                "version" to project.version,
                "mc_version" to project.property("minecraft_version"),
        )

        filesMatching("fabric.mod.json") {
            expand(propertyMap)
        }
    }

    jar {
        val licenseSuffix = project.base.archivesName.get()
        from("LICENSE") {
            rename { "${it}_${licenseSuffix}" }
        }
    }

    java {
    }

    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release = 21
    }

    sourceSets {
        main {
            java.srcDirs("src/main/java", "src/main/kotlin", "LiquidBounce/src/main/java", "LiquidBounce/src/main/kotlin")
        }
    }


}