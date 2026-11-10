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
    testImplementation("com.squareup.okhttp3:mockwebserver3:5.5.1")
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
    // Tests that use the default ~/.tealium storage write under build/ instead of the real home directory,
    // starting from an empty directory on every run.
    val testHome = layout.buildDirectory.dir("test-home")
    doFirst { delete(testHome) }
    systemProperty("user.home", testHome.get().asFile.absolutePath)
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
            // Each credential is read from a Gradle property of the same name if set (e.g. in
            // ~/.gradle/gradle.properties), falling back to an environment variable of the same
            // name otherwise.
            credentials(AwsCredentials::class) {
                accessKey = awsCredential("AWS_ACCESS_KEY")
                secretKey = awsCredential("AWS_SECRET_KEY")
                sessionToken = awsCredential("AWS_SESSION_TOKEN")
            }
        }
    }
}

private fun awsCredential(name: String): String? =
    providers.gradleProperty(name).orElse(providers.environmentVariable(name)).orNull
