plugins {
    `java-library`
    `maven-publish`
}

group = "com.tealium"
version = "1.4.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.jr:jackson-jr-objects:2.9.0")

    testImplementation("junit:junit:4.13.1")
    testImplementation("org.mockito:mockito-core:5.12.0")
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 8
    options.encoding = "UTF-8"
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
