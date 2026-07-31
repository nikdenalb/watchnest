plugins {
    `java-library`
}

group = rootProject.property("group").toString()
version = property("plannerVersion").toString()

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}
