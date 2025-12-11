val playgroundGroup = "Playground"

dependencies {
    implementation(project(":aggregate-api"))
    testImplementation(project(":aggregate-api").dependencyProject.sourceSets.test.get().output)
}

tasks.withType<Jar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    destinationDirectory.set(File("."))
    from("build/classes/java/main")
    from("build/resources/main")
}

task("GetServerVersion", JavaExec::class) {
    main = "examples.api.GetServerVersion"
    group = playgroundGroup
    classpath = sourceSets["main"].runtimeClasspath
    workingDir = parent!!.projectDir
}

task("ExecuteAction", JavaExec::class) {
    main = "examples.api.ExecuteAction"
    group = playgroundGroup
    classpath = sourceSets["main"].runtimeClasspath
    workingDir = parent!!.projectDir
}

task("ManageDevices", JavaExec::class) {
    main = "examples.api.ManageDevices"
    group = playgroundGroup
    classpath = sourceSets["main"].runtimeClasspath
    workingDir = parent!!.projectDir
}

task("ManageUsers", JavaExec::class) {
    main = "examples.api.ManageUsers"
    group = playgroundGroup
    classpath = sourceSets["main"].runtimeClasspath
    workingDir = parent!!.projectDir
}
