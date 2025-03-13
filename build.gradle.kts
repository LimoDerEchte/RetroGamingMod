@file:Suppress("PropertyName")

plugins {
    id("fabric-loom") version "1.10-SNAPSHOT"
    id("maven-publish")
}

version = project.property("mod_version") as String
group = project.property("maven_group") as String

base {
    archivesName.set(project.property("archives_base_name") as String)
}

loom {
    splitEnvironmentSourceSets()

    mods {
        create("emumod") {
            sourceSet(sourceSets["main"])
            sourceSet(sourceSets["client"])
        }
    }
}

repositories {
    maven {
        name = "Shedaniel"
        url = uri("https://maven.shedaniel.me/" )
    }
    maven {
        name= "Terraformers"
        url = uri("https://maven.terraformersmc.com/")
    }
}

val minecraft_version: String by project
val yarn_mappings: String by project
val loader_version: String by project

val fabric_version: String by project
val modmenu_version: String by project
val cloth_config_version: String by project

dependencies {
    minecraft("com.mojang:minecraft:${minecraft_version}")
    mappings("net.fabricmc:yarn:${yarn_mappings}:v2")
    modImplementation("net.fabricmc:fabric-loader:${loader_version}")

    modImplementation("net.fabricmc.fabric-api:fabric-api:${fabric_version}")
    modImplementation("com.terraformersmc:modmenu:${modmenu_version}")
    modApi("me.shedaniel.cloth:cloth-config-fabric:${cloth_config_version}") {
        exclude("net.fabricmc.fabric-api")
    }
}

tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("minecraft_version", minecraft_version)
    inputs.property("loader_version", loader_version)
    filteringCharset = "UTF-8"

    filesMatching("fabric.mod.json") {
        expand(
            mapOf(
                "version" to project.version,
                "minecraft_version" to minecraft_version,
                "loader_version" to loader_version
            )
        )
    }
}

val targetJavaVersion = 21
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible) {
        options.release.set(targetJavaVersion)
    }
    options.compilerArgs.addAll(listOf("-h", file("src/core/bridge/headers").absolutePath))
}

java {
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    if (JavaVersion.current() < javaVersion) {
        toolchain.languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
    }
    withSourcesJar()
}

tasks.jar {
    from("LICENSE") {
        rename { "${it}_${project.property("archives_base_name")}" }
    }
    from(fileTree("run/retroGamingRequired") {
        include("**/*bridge.*")
        include("**/*retro-core*")
    }) {
        into("lib")
    }
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}
