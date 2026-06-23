group = property("group").toString()
version = property("rootVersion").toString()

allprojects {
    group = rootProject.property("group").toString()

    repositories {
        mavenCentral()
    }
}

subprojects {
    plugins.withId("java") {
        dependencies {
            add("testImplementation", platform("org.junit:junit-bom:6.0.0"))
            add("testImplementation", "org.junit.jupiter:junit-jupiter")
            add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")
        }

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
        }
    }
}
