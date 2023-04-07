import org.apache.tools.ant.filters.ReplaceTokens
import java.util.*

plugins {
    java
    idea
    `java-library`
    `maven-publish`
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("org.cadixdev.licenser") version "0.6.1"
    id("net.kyori.indra.git") version "2.1.1"
    id("net.researchgate.release") version "3.0.2"
}

group = "com.discordsrv"
val minecraftVersion = project.properties["minecraftVersion"]!!
val targetJavaVersion = 1.8

java {
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion

    disableAutoTargetJvm() // required because paper-api uses Java 17 (w/ gradle metadata)
}

license {
    include("**/*.java")
    header(project.file("LICENSE.head"))
}

release {
    git {
        requireBranch.set("master")
    }
}

tasks {
    java {
        withJavadocJar()
        withSourcesJar()
    }
    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
    javadoc {
        options.encoding = "UTF-8"
        options {
            (this as CoreJavadocOptions).addStringOption("Xdoclint:none", "-quiet")
        }
    }

    processResources {
        outputs.upToDateWhen { false }
        filter<ReplaceTokens>(mapOf(
            "tokens" to mapOf("version" to project.version.toString()),
            "beginToken" to "\${",
            "endToken" to "}"
        ))
    }

    test {
        useJUnitPlatform()
    }

    register("commitVersion") {
        doLast {
            val v = "v$version"
            println("Commit: $v")
            val git = indraGit.git()!!
            git.add().addFilepattern("gradle.properties").call()
            git.commit()
                .setAuthor("Scarsz", "truescarsz@gmail.com")
                .setMessage(v).call()
        }
    }

    // Set snapshot version for all jar tasks
    withType<Jar> {
        val commit = if (indraGit.isPresent) indraGit.commit()?.name() ?: "" else ""
        val version = (project.version.toString()) + if (archiveVersion.get().endsWith("-SNAPSHOT")) (if (commit.length >= 7) "-" + commit.substring(0, 7) else "") else ""
        archiveVersion.set(version)
    }

    jar {
        finalizedBy("updateLicenses", "shadowJar")
        archiveFileName.set(project.name + "-" + archiveVersion.get() + "-original.jar")

        manifest.attributes(mapOf<String, String>(
            "Build-Date" to (Date().toString()),
            "Git-Revision" to (if (indraGit.isPresent) (indraGit.commit()?.name() ?: "") else ""),
            "Git-Branch" to (if (indraGit.isPresent) indraGit.branchName() ?: "" else ""),
            "Build-Number" to (System.getenv("GITHUB_RUN_NUMBER") ?: ""),
            "Build-Origin" to (if (System.getenv("RUNNER_NAME") != null) "GitHub Actions: " + System.getenv("RUNNER_NAME") else (System.getProperty("user.name") ?: "Unknown"))
        ))
    }

    shadowJar {
        archiveFileName.set(project.name + "-" + archiveVersion.get() + ".jar")

        // Classifier for publishing
        archiveClassifier.set("shaded")

        mustRunAfter("build")
        minimize {
            exclude(dependency("github.scarsz:configuralize:.*"))
            exclude(dependency("me.scarsz:jdaappender:.*"))
            exclude(dependency("com.fasterxml.jackson.core:jackson-databind:.*"))
        }

        relocate("net.dv8tion.jda", "github.scarsz.discordsrv.dependencies.jda")
        relocate("com.iwebpp.crypto", "github.scarsz.discordsrv.dependencies.iwebpp.crypto")
        relocate("com.vdurmont.emoji", "github.scarsz.discordsrv.dependencies.emoji")
        relocate("com.neovisionaries.ws", "github.scarsz.discordsrv.dependencies.ws")
        relocate("net.kyori", "github.scarsz.discordsrv.dependencies.kyori")
        relocate("dev.vankka.mcdiscordreserializer", "github.scarsz.discordsrv.dependencies.mcdiscordreserializer")
        relocate("dev.vankka.simpleast", "github.scarsz.discordsrv.dependencies.simpleast")
        relocate("org.bstats", "github.scarsz.discordsrv.dependencies.bstats")
        relocate("org.apache.commons", "github.scarsz.discordsrv.dependencies.commons")
        relocate("org.json.simple", "github.scarsz.discordsrv.dependencies.json.simple")
        relocate("org.json", "github.scarsz.discordsrv.dependencies.json")
        relocate("org.minidns", "github.scarsz.discordsrv.dependencies.minidns")
        relocate("okhttp3", "github.scarsz.discordsrv.dependencies.okhttp3")
        relocate("okio", "github.scarsz.discordsrv.dependencies.okio")
        relocate("gnu.trove", "github.scarsz.discordsrv.dependencies.trove")
        relocate("com.fasterxml.jackson", "github.scarsz.discordsrv.dependencies.jackson")
        relocate("com.google.common", "github.scarsz.discordsrv.dependencies.google.common")
        relocate("com.google.errorprone", "github.scarsz.discordsrv.dependencies.google.errorprone")
        relocate("com.google.gson", "github.scarsz.discordsrv.dependencies.google.gson")
        relocate("com.google.j2objc", "github.scarsz.discordsrv.dependencies.google.j2objc")
        relocate("com.github.kevinsawicki", "github.scarsz.discordsrv.dependencies.kevinsawicki")
        relocate("com.github.zafarkhaja", "github.scarsz.discordsrv.dependencies.zafarkhaja")
        relocate("alexh", "github.scarsz.discordsrv.dependencies.alexh")

        // Merge META-INF/services files where needed
        mergeServiceFiles()

        // Exclude signatures, maven/ and proguard/ from META-INF
        exclude("META-INF/*.SF")
        exclude("META-INF/*.DSA")
        exclude("META-INF/*.RSA")
        exclude("META-INF/maven/**")
        exclude("META-INF/proguard/**")
    }
}

publishing {
    repositories {
        maven {
            val repository = "https://nexus.scarsz.me/content/repositories/"
            val releasesRepoUrl = repository + "releases"
            val snapshotsRepoUrl = repository + "snapshots"
            url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)

            credentials {
                username = System.getenv("REPO_USERNAME") ?: "ci"
                password = (System.getenv("REPO_PASSWORD") ?: project.property("repoPassword")).toString()
            }
        }
    }
    publications {
        create<MavenPublication>("maven") {
            // Publish the shaded jar as the main jar, sources & javadoc and an empty pom (no dependencies)
            artifact(tasks["shadowJar"]) {
                classifier = null
            }
            artifact(tasks["sourcesJar"])
            artifact(tasks["javadocJar"])
            artifactId = "discordsrv"
        }
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://papermc.io/repo/repository/maven-public/")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots")
    maven("https://nexus.scarsz.me/content/groups/public/")
}

dependencies {
    // Paper API
    compileOnly("io.papermc.paper:paper-api:${minecraftVersion}-R0.1-SNAPSHOT") {
        exclude("commons-lang") // Exclude lang in favor of our own lang3
    }
    
    // JDA
    api("net.dv8tion:JDA:4.4.0_353") {
        exclude(module = "opus-java") // we don't use voice features
    }
    
    // Config
    api("github.scarsz:configuralize:1.3.2") {
        // already provided by bukkit
        exclude(module = "json-simple")
        exclude(module = "snakeyaml")
    }
    
    // Logging
    implementation("me.scarsz:jdaappender:1.0.3")
    implementation("org.slf4j:slf4j-jdk14:1.7.36")
    implementation("org.slf4j:jcl-over-slf4j:1.7.36")
    // MC <  1.12 = 2.0-beta9
    // MC >= 1.12 = 2.1
    // Log4J is NOT included in DiscordSRV and is only used for compilation.
    // This means that DiscordSRV is NOT vulnerable to CVE-2021-44228
    compileOnly("org.apache.logging.log4j:log4j-core:2.0-beta9")

    // adventure, adventure-platform, MCDiscordReserializer
    val adventureVersion = "4.10.1"
    api("net.kyori:adventure-api:${adventureVersion}")
    api("net.kyori:adventure-text-minimessage:${adventureVersion}")
    api("net.kyori:adventure-text-serializer-legacy:${adventureVersion}")
    api("net.kyori:adventure-text-serializer-plain:${adventureVersion}")
    api("net.kyori:adventure-text-serializer-gson:${adventureVersion}")
    implementation("net.kyori:adventure-platform-bukkit:4.1.2")
    api("dev.vankka:MCDiscordReserializer:4.2.2")

    // Annotations
    compileOnlyApi("org.jetbrains:annotations:23.0.0")
    
    // Lombok
    compileOnly("org.projectlombok:lombok:1.18.24")
    annotationProcessor("org.projectlombok:lombok:1.18.24")
    
    // Apache Commons, guava
    implementation("commons-io:commons-io:2.11.0")
    implementation("commons-collections:commons-collections:3.2.2")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("commons-codec:commons-codec:1.15")
    implementation("com.google.guava:guava:31.1-jre")

    // DynamicProxy
    runtimeOnly("dev.vankka:dynamicproxy:1.0.0:runtime")
    compileOnly("dev.vankka:dynamicproxy:1.0.0")
    annotationProcessor("dev.vankka:dynamicproxy:1.0.0")
    
    // MySQL
    compileOnly("mysql:mysql-connector-java:8.0.28") // NEWER than CraftBukkit's
    
    // Misc libraries
    api("com.vdurmont:emoji-java:5.1.1")
    implementation("org.bstats:bstats-bukkit:2.2.1")
    implementation("com.github.zafarkhaja:java-semver:0.9.0")
    implementation("com.github.kevinsawicki:http-request:6.0")
    implementation("org.minidns:minidns-hla:1.0.4")
    implementation("com.hrakaroo:glob:0.9.0")
    implementation("org.springframework:spring-expression:5.3.16") {
        // exclude Spring's org.apache.commons.logging adapter in favor of
        // jcl-over-slf4j for better compatability with old log4j versions
        exclude("spring-jcl")
    }

    ///
    /// Plugin Hooks
    ///
    
    // chat hooks
    compileOnly("ru.mrbrikster:chatty-api:2.18.2")
    compileOnly("br.com.finalcraft:fancychat:1.0.2")
    compileOnly("com.dthielke.herochat:Herochat:5.6.5")
    compileOnly("br.com.devpaulo:legendchat:1.1.5")
    compileOnly("com.github.ucchyocean.lc:LunaChat:3.0.16")
    compileOnly("com.nickuc.chat:nchat-api:5.6")
    compileOnly("com.palmergames.bukkit:TownyChat:0.45")
    compileOnly("mineverse.aust1n46:venturechat:2.20.1")
    compileOnly("com.comphenix.protocol:ProtocolLib:4.5.0")
    
    // vanish hooks
    compileOnly("de.myzelyam:SuperVanish:6.2.0")
    
    // permissions hooks
    compileOnly("net.luckperms:api:5.4")
    
    // world hooks
    compileOnly("com.onarandombox.MultiverseCore:Multiverse-Core:2.4")
    
    // misc hooks
    compileOnly("org.dynmap:dynmap-api:2.0")
    compileOnly("com.gmail.nossr50:mcmmo:1.5.07")
    compileOnly("net.milkbowl.vault:VaultAPI:1.7")
    compileOnly("me.clip:placeholderapi:2.10.7")
    
    // debug hooks
    compileOnly("ch.njol:skript:2.5")

    // JUnit
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.0")
    testImplementation("io.papermc.paper:paper-api:${minecraftVersion}-R0.1-SNAPSHOT")
}

var generatedPaths: FileCollection = sourceSets.main.get().output.generatedSourcesDirs
idea {
    module {
        generatedPaths.forEach {
            generatedSourceDirs.plus(it)
        }
    }
}
