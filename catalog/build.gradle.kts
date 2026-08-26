plugins {
    `java-library`
}

group = rootProject.property("group").toString()
version = property("catalogVersion").toString()

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}
