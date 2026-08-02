plugins {
    `java-library`
}

group = rootProject.property("group").toString()
version = property("identityVersion").toString()

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}
