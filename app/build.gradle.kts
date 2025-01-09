plugins {
    application
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(libs.junit.jupiter)

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    implementation(libs.guava)
    implementation(libs.kafka)
    implementation(libs.responsive)
    implementation(libs.spring.boot)
    implementation(libs.spring.kafka)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    mainClass = "dev.responsive.quickstart.WordCount"
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
