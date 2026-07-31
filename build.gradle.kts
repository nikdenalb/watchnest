import org.apache.tools.ant.taskdefs.condition.Os

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

tasks.register<Exec>("dev") {
    group = "application"
    description = "Start planner-app and frontend via scripts/dev.*"
    workingDir = rootDir
    standardInput = System.`in`

    if (Os.isFamily(Os.FAMILY_WINDOWS)) {
        commandLine(
            "powershell.exe",
            "-NoProfile",
            "-ExecutionPolicy",
            "Bypass",
            "-File",
            "scripts/dev.ps1",
        )
    } else {
        commandLine("bash", "scripts/dev.sh")
    }
}
