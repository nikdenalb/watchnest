import org.apache.tools.ant.taskdefs.condition.Os

plugins {
    base
}

group = rootProject.property("group").toString()
version = providers.fileContents(layout.projectDirectory.file("package.json"))
    .asText
    .map { text ->
        Regex(""""version"\s*:\s*"([^"]+)"""").find(text)?.groupValues?.get(1) ?: "0.0.0"
    }
    .get()

val npmCommand = if (Os.isFamily(Os.FAMILY_WINDOWS)) "npm.cmd" else "npm"

tasks.register<Exec>("npmInstall") {
    group = "npm"
    description = "Install CMS npm dependencies"
    workingDir = projectDir
    commandLine(npmCommand, "install")
    inputs.files("package.json", "package-lock.json")
    outputs.dir("node_modules")
}

tasks.register<Exec>("npmBuild") {
    group = "npm"
    description = "Typecheck and build the CMS Vite production bundle"
    dependsOn("npmInstall")
    workingDir = projectDir
    commandLine(npmCommand, "run", "build")
    inputs.files(
        "package.json",
        "package-lock.json",
        "tsconfig.json",
        "tsconfig.app.json",
        "tsconfig.node.json",
        "vite.config.ts",
        "index.html",
    )
    inputs.dir("src")
    outputs.dir("dist")
}

tasks.register<Exec>("npmDev") {
    group = "application"
    description = "Start CMS Vite dev server only"
    dependsOn("npmInstall")
    workingDir = projectDir
    commandLine(npmCommand, "run", "dev")
    standardInput = System.`in`
}

tasks.register<Exec>("npmTest") {
    group = "verification"
    description = "Run CMS unit tests"
    dependsOn("npmInstall")
    workingDir = projectDir
    commandLine(npmCommand, "run", "test")
    inputs.files(
        "package.json",
        "package-lock.json",
        "tsconfig.json",
        "tsconfig.app.json",
        "tsconfig.node.json",
        "vite.config.ts",
    )
    inputs.dir("src")
}

tasks.named("assemble") {
    dependsOn("npmBuild")
}

tasks.named("check") {
    dependsOn("npmTest", "npmBuild")
}
