/*
 * Copyright 2017-2019 (c) Alexander "ILakeful" Shevchenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

import org.gradle.api.JavaVersion

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val kotlinVersion = extra["kotlinVersion"] as String

buildscript {
    val kotlinVersion by extra { "1.3.41" }

    repositories {
        mavenCentral()
    }

    dependencies {
        classpath(kotlin("gradle-plugin", version = kotlinVersion))
        classpath("com.github.jengelman.gradle.plugins:shadow:5.1.0")
    }
}

plugins {
    java
    idea
    application
    kotlin("jvm") version "1.3.41"
}

apply(plugin = "com.github.johnrengelman.shadow")

application {
    mainClassName = "io.ilakeful.lakebot.LakeBotKt"
    applicationName = "lakebot"
    group = "io.ilakeful.lakebot"
    version = Version(1, 0, stability = Version.Stability.BETA, unstable = 13).toString()
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

repositories {
    jcenter()

    maven {
        name = "jitpack"
        url = uri("https://jitpack.io")
    }

    maven {
        url = uri("https://dl.bintray.com/kotlin/kotlin-eap")
    }
}

dependencies {
    implementation(group = "net.dv8tion", name = "JDA", version = "4.0.0_39") { exclude(module = "opus-java") }
    implementation(group = "org.slf4j", name = "slf4j-simple", version = "1.8.0-beta4")
    implementation(group = "org.json", name = "json", version = "20180813")
    implementation(group = "com.github.kenglxn.QRGen", name = "javase", version = "2.5.0")
    implementation(group = "com.github.ilakeful", name = "lakeweather", version = "0.1.6")
    implementation(group = "com.sedmelluq", name = "lavaplayer", version = "1.3.19")
    implementation(group = "com.google.guava", name = "guava", version = "28.0-jre")
    implementation(group = "khttp", name = "khttp", version = "1.0.0")
    implementation(group = "com.github.markozajc", name = "akiwrapper", version = "1.4.2")
    implementation(group = "org.mongodb", name = "mongo-java-driver", version = "3.7.0-rc0")
    implementation(group = "com.google.apis", name = "google-api-services-youtube", version = "v3-rev212-1.25.0")
    implementation(group = "org.jsoup", name = "jsoup", version = "1.12.1")
    implementation(group = "commons-validator", name = "commons-validator", version = "1.6")
    implementation(group = "org.reflections", name = "reflections", version = "0.9.11")
    implementation(kotlin("stdlib"))
    implementation(kotlin("stdlib-jdk8", version = kotlinVersion))
    implementation(kotlin("stdlib-jdk7", version = kotlinVersion))
    implementation(kotlin("reflect"))
    implementation(group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version = "1.3.0-RC")
    implementation(kotlin("script-runtime"))
    implementation(kotlin("compiler-embeddable"))
    implementation(kotlin("script-util"))
    runtime(kotlin("scripting-compiler-embeddable"))
    testImplementation(group = "junit", name = "junit", version = "4.13-beta-3")
    testImplementation(kotlin("test-junit"))
}

val build: DefaultTask by tasks
val clean: Delete by tasks
val jar: Jar by tasks
val shadowJar: ShadowJar by tasks

build.apply {
    dependsOn(clean)
    dependsOn(shadowJar)

    jar.mustRunAfter(clean)
}
tasks.withType<ShadowJar> {
    archiveBaseName.set("lakebot")
    archiveClassifier.set("")
}
tasks.withType<Wrapper> {
    gradleVersion = "5.5.1"
    distributionUrl = "https://services.gradle.org/distributions/gradle-$gradleVersion-all.zip"
}
task("stage") {
    dependsOn(build, clean)
}
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

data class Version(
        val major: Int,
        val minor: Int,
        val patch: Int = 0,
        val stability: Stability = Stability.STABLE,
        val unstable: Int? = null
) {
    override fun toString() = arrayOf(
            major,
            minor,
            if (patch == 0) null else patch
    ).filterNotNull().joinToString(separator = ".") + stability.let {
        val suffix = it.suffix
        if (suffix !== null && (unstable !== null && unstable != 0)) {
            "-$suffix$unstable"
        } else ""
    }
    enum class Stability(val suffix: String?) {
        STABLE(null), BETA("BETA"), ALPHA("ALPHA")
    }
}