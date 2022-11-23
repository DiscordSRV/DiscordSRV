import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.20"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "com.discordsrv"
version = "1.26.1-SNAPSHOT"
val minecraftVersion = "1.16.5"
val targetJavaVersion = 1.8

java {
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
}

tasks {
    processResources {
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            expand("version" to version)
        }
    }

    jar {
        finalizedBy("shadowJar")
    }
    shadowJar {
        mustRunAfter("build")

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
        relocate("okhttp3", "github.scarsz.discordsrv.dependencies.jda")
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

fun configureKotlin(kotlinCompile: Action<KotlinCompile>) {
    tasks.withType<KotlinCompile>().configureEach(kotlinCompile)
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://papermc.io/repo/repository/maven-public/")
    maven("https://oss.sonatype.org/content/repositories/central")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://nexus.scarsz.me/content/groups/public/")
}

dependencies {
    compileOnly("com.destroystokyo.paper:paper-api:${minecraftVersion}-R0.1-SNAPSHOT") {
        exclude("commons-lang") // we have our own
    }
    implementation("net.dv8tion:JDA:4.4.0_352.fix-2") {
        exclude("opus-java") // we don't use voice features
    }
    implementation("github.scarsz:configuralize:1.3.2")
    implementation("org.bstats:bstats-bukkit:2.2.1")

    // logging
    api("me.scarsz:jdaappender:1.0.3")
    implementation("org.slf4j:slf4j-jdk14:1.7.36")
    implementation("org.slf4j:jcl-over-slf4j:1.7.36")
    // MC <  1.12 = 2.0-beta9
    // MC >= 1.12 = 2.1
    // Log4J is NOT included in DiscordSRV and is only used for compilation.
    // This means that DiscordSRV is NOT vulnerable to CVE-2021-44228
    compileOnly("org.apache.logging.log4j:log4j-core:2.0-beta9")

    // adventure API
    val adventureVersion = "4.10.1"
    val adventurePlatformVersion = "4.1.2"
    implementation("net.kyori:adventure-api:${adventureVersion}")
    implementation("net.kyori:adventure-text-serializer-legacy:${adventureVersion}")
    implementation("net.kyori:adventure-text-serializer-gson:${adventureVersion}")
    implementation("net.kyori:adventure-text-serializer-plain:${adventureVersion}")
    implementation("net.kyori:adventure-text-minimessage:${adventureVersion}")
    implementation("net.kyori:adventure-platform-bukkit:${adventurePlatformVersion}")

    // chat hooks
    compileOnly("ru.mrbrikster:chatty-api:2.18.2")
    compileOnly("br.com.finalcraft:fancychat:1.0.2")
    compileOnly("com.dthielke.herochat:Herochat:5.6.5")
    compileOnly("br.com.devpaulo:legendchat:1.1.5")
    compileOnly("com.github.ucchyocean.lc:LunaChat:3.0.16")
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
    compileOnly("ch.njol:skript:2.5")
    compileOnly("net.milkbowl.vault:VaultAPI:1.7")
    compileOnly("com.gmail.nossr50:mcmmo:1.5.07")
    compileOnly("me.clip:placeholderapi:2.10.7")

    compileOnly("org.jetbrains:annotations:23.0.0")
    compileOnly("org.projectlombok:lombok:1.18.24")
    annotationProcessor("org.projectlombok:lombok:1.18.24")
    compileOnly("commons-io:commons-io:2.11.0")
    compileOnly("commons-collections:commons-collections:3.2.2")
    compileOnly("org.apache.commons:commons-lang3:3.12.0")
    compileOnly("commons-codec:commons-codec:1.15")
    compileOnly("com.google.guava:guava:31.1-jre")
    compileOnly("mysql:mysql-connector-java:8.0.28") // NEWER than CraftBukkit's
    implementation("com.vdurmont:emoji-java:5.1.1")
    implementation("com.github.zafarkhaja:java-semver:0.9.0")
    implementation("com.github.kevinsawicki:http-request:6.0")
    implementation("org.minidns:minidns-hla:1.0.4")
    implementation("com.hrakaroo:glob:0.9.0")
    implementation("dev.vankka:MCDiscordReserializer:4.2.2")
    implementation("org.springframework:spring-expression:5.3.16") {
        // exclude Spring's org.apache.commons.logging adapter in favor of
        // jcl-over-slf4j for better compatability with old log4j versions
        exclude("spring-jcl")
    }
}
