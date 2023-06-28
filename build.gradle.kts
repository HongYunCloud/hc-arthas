import de.undercouch.gradle.tasks.download.Download

plugins {
    id("java")
    id("de.undercouch.download") version "5.4.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "io.github.hongyuncloud"
version = "1.0"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    implementation("org.yaml:snakeyaml:2.0")
    implementation("bot.inker.acj:runtime:1.5")
    compileOnly("org.bukkit:bukkit:1.12-R0.1-SNAPSHOT")
}

tasks.create<Download>("downloadArthas") {
    val targetFile = buildDir.resolve("tmp").resolve("arthas-packaging-3.6.9-bin.zip")
    this.onlyIf { !targetFile.exists() }
    src("https://repo1.maven.org/maven2/com/taobao/arthas/arthas-packaging/3.6.9/arthas-packaging-3.6.9-bin.zip")
    dest(targetFile)
}

tasks.processResources {
    dependsOn(tasks["downloadArthas"])
    with(copySpec {
        tasks["downloadArthas"]
            .outputs
            .files
            .flatMap(::zipTree)
            .filter { it.name == "arthas-core.jar" || it.name == "arthas-spy.jar" }
            .forEach(::from)
        rename { "$it.bin" }
    })
}

tasks.jar {
    manifest {
        attributes["Premain-Class"] = "io.github.hongyuncloud.arthas.HcArthasMain"
        attributes["Agent-Class"] = "io.github.hongyuncloud.arthas.HcArthasMain"
        attributes["Can-Redefine-Classes"] = "true"
        attributes["Can-Retransform-Classes"] = "true"
        attributes["Can-Set-Native-Method-Prefix"] = "true"
    }
}

tasks.shadowJar {
    relocate("org.yaml.snakeyaml", "io.github.hongyuncloud.arthas.snakeyaml")
    relocate("bot.inker.acj", "io.github.hongyuncloud.arthas.acj")

    exclude("META-INF/maven/**")
    exclude("META-INF/versions/**")
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
}