plugins {
    application
}

repositories {
    mavenCentral()
}

dependencies {
    // The root project is the SDK itself.
    implementation(project(":"))
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 8
    options.encoding = "UTF-8"
}

application {
    mainClass.set("com.tealium.example.ExampleApp")
}
