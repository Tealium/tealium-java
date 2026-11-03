plugins {
    application
}

repositories {
    mavenCentral()
}

dependencies {
    // Resolved against the included build ".." via automatic dependency substitution,
    // since that project's group ("com.tealium") and name ("java") match.
    implementation("com.tealium:java:1.4.0")
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 8
    options.encoding = "UTF-8"
}

application {
    mainClass.set("com.tealium.example.ExampleApp")
}
