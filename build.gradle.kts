plugins {
    `java-library`
    `maven-publish`
}

group = "com.tealium"
version = "1.4.0"

repositories {
    mavenCentral()
}

val mockitoAgent = configurations.create("mockitoAgent")

dependencies {
    implementation("com.fasterxml.jackson.jr:jackson-jr-objects:2.22.4")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.24.0")
    mockitoAgent("org.mockito:mockito-core:5.24.0") { isTransitive = false }
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 8
    options.encoding = "UTF-8"
}

val generateBuildConfig = tasks.register("generateBuildConfig") {
    val projectVersion = version.toString()
    val outputDir = layout.buildDirectory.dir("generated/sources/buildConfig/java/main")

    inputs.property("version", projectVersion)
    outputs.dir(outputDir)

    doLast {
        val packageDir = outputDir.get().dir("com/tealium").asFile
        packageDir.mkdirs()
        packageDir.resolve("BuildConfig.java").writeText(
            """
            package com.tealium;

            final class BuildConfig {
                static final String VERSION = "$projectVersion";

                private BuildConfig() {}
            }
            """.trimIndent()
        )
    }
}

sourceSets {
    main {
        java.srcDir(generateBuildConfig)
    }
}

abstract class MockitoAgentProvider : CommandLineArgumentProvider {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val agentJar: ConfigurableFileCollection

    override fun asArguments(): Iterable<String> =
        listOf("-javaagent:${agentJar.asPath}")
}

tasks.test {
    jvmArgumentProviders.add(objects.newInstance<MockitoAgentProvider>().apply {
        agentJar.from(mockitoAgent)
    })
    systemProperty("tealium.projectVersion", version.toString())
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            pom {
                name = "TealiumJava"
            }
        }
    }
    repositories {
        maven {
            name = "tealium"
            url = uri("s3://maven.tealiumiq.com/java/releases/")
            // Gradle resolves these from the properties tealiumAccessKey, tealiumSecretKey,
            // and optionally tealiumSessionToken (e.g. in ~/.gradle/gradle.properties).
            credentials(AwsCredentials::class)
        }
    }
}
