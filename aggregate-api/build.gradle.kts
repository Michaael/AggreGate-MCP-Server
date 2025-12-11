
import provider.aggregateVersion
import provider.groupCore
import signing.SignJar
import java.time.LocalDate

val mavenJar: Configuration by configurations.creating
val jarImplementation: Configuration by configurations.creating
val sdkGroup = "SDK"

dependencies {
    //region SDK equality
    // All dependencies from here up to "endregion" marker must be kept in sync with 
    // `aggregate-sdk/aggregate-api/build.gradle.kts` build script
    api(ApacheCommonsLibs.commonsNet)
    api(ApacheCommonsLibs.commonsBeanutils)
    api(ApacheCommonsLibs.commonsLogging)
    api(ApacheCommonsLibs.commonsLang3)
    api(ApacheCommonsLibs.commonsIo)
    api(ApacheCommonsLibs.commonsMath3)

    api(Log4JLibs.log4jApi)
    api(Log4JLibs.log4jCore)
    api(Log4JLibs.log4j12Api)
    api(Log4JLibs.log4jSlf4jImpl)
    api(Log4JLibs.slf4jApi)

    api("net.sourceforge.javacsv", "javacsv", "2.1")
    api("xalan", "xalan", "2.7.2")
    api("com.googlecode.json-simple", "json-simple", "1.1.1") {
        exclude(group = "junit", module = "junit")
    }
    
    api("com.google.code.findbugs", "jsr305", "3.0.2")       // @Nonnull and @Nullable annotations
    api(GoogleLibs.guava)

    api("net.sf.jpf", "jpf", "1.5.1")
    //endregion
    
    mavenJar("$groupCore.${project.name}:${CommonArtifacts.AGGREGATE_API_LIBS}:$aggregateVersion")
}

tasks.named("assemble") {
    dependsOn(":widget-api:assemble")
}

tasks.named<Jar>("jar") {
    from(jarImplementation.asFileTree.files.map { zipTree(it) })
    from("../widget-api/build/classes/java/main")
    from("../widget-api/build/classes/java/test") {
        exclude("Test*")
    }
    from("../widget-api/build/resources/main")
    exclude("**/*.jar")
    dependsOn(":widget-api:classes")
}

tasks.register("signAggregateApiJar", SignJar::class) {
    jarPath = "../linkserver-core/jar/aggregate-api.jar"
}

tasks.withType<Test> {
    jvmArgs = listOf("-Duser.language.format=ru", "-Duser.country.format=RU")
}

tasks.withType<Delete> {
    delete("docs")
    delete("logs")
}

tasks.create<Jar>(CommonTasks.BUILD_AGG_API_LIBS) {
    description = "Creates '${CommonArtifacts.AGGREGATE_API_LIBS_JAR}' archive with all 3rd party dependencies"
    group = "build"
    dependsOn("assemble")

    destinationDirectory.set(File("../linkserver-core/jar"))
    archiveFileName.set(CommonArtifacts.AGGREGATE_API_LIBS_JAR)

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    val aggLibFiles = configurations.runtimeClasspath.map {
        runtimeClasspath -> runtimeClasspath.asFileTree.files.map { zipTree(it) }
    }
    from(aggLibFiles)
}

tasks.register("buildAggregateSdk") {
    finalizedBy("buildJavadocForSdk")
    group = sdkGroup
}

tasks.register("buildJavadocForSdk", Javadoc::class) {
    group = sdkGroup
    source = sourceSets.main.get().allJava
    classpath = sourceSets.main.get().runtimeClasspath
    isFailOnError = false
    options.windowTitle("AggreGate Java API")
    options.encoding("UTF-8")
    options.header = "<h1>AggreGate Java API</h1>"
    title = "<i>Copyright &#169; 2001 - ${LocalDate.now().year} Tibbo Systems. All Rights Reserved.</i>"
    setDestinationDir(file("docs"))
    finalizedBy("zipAggregateSdk")
}

tasks.register("zipAggregateSdk", Zip::class) {
    description = "Create ZIP distribution with Java SDK"
    group = sdkGroup

    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    archiveFileName.set("aggregate_sdk_$aggregateVersion.zip")
    destinationDirectory.set(file("../../releases/$aggregateVersion"))

    from(".") {
        exclude("build", "out", "logs", "sdk", "docs", "build.gradle.kts", "logging.xml", "src/main/java/examples")
        into("aggregate-api")
    }
    from("src/main/java/examples/api") {
        into("demo-api/src/main/java/examples/api")
    }
    from("src/main/java/examples/driver/DemoDeviceDriver.java") {
        into("demo-driver/src/main/java/examples/driver")
    }
    from("src/main/java/examples/driver/plugin.xml") {
        into("demo-driver")
    }
    from("src/main/java/examples/plugin/DemoServerPlugin.java") {
        into("demo-plugin/src/main/java/examples/plugin")
    }
    from("src/main/java/examples/plugin/plugin.xml") {
        into("demo-plugin")
    }
    from("src/main/java/examples/agent/DemoAgent.java") {
        into("demo-agent/src/main/java/examples/agent")
    }
    from("../widget-api") {
        exclude(".gradle", "build", "out", "build.gradle.kts")
        into("widget-api")
    }
    from("../context-demo-web-app") {
        exclude("build", "demo-web-app.jar", "demo-web-app.war", "build.gradle.kts")
        into("context-demo-web-app")
    }
    from("../buildSrc") {
        exclude(".gradle",
                "build",
                "src/main/java/Constants.kt",
                "src/main/java/customization",
                "src/main/java/documentation",
                "src/main/java/ftp",
                "src/main/java/installer",
                "src/main/java/jasperstudio",
                "src/main/java/plugin",
                "src/main/java/signing",
                "src/main/resources/signjar.jks",
                "src/main/kotlin"
        )
        into("buildSrc")
    }

    from("docs") {
        into("docs")
    }
    from("../aggregate-sdk") {
        exclude("demo-driver/build", "demo-driver/demo-driver.jar", "demo-plugin/build", "demo-plugin/demo-plugin.jar")
    }
    from("logging.xml")

    from("../linkserver-core/jar/aggregate-api.jar") {
        into("libs")
    }
    from(tasks.named<Jar>(CommonTasks.BUILD_AGG_API_LIBS)) {
        into("libs")
    }

    from("../gradle") {
        into("gradle")
    }
    from("../gradlew.bat")
    from("../gradlew")
    from("../gradle.properties")
}

publishing {
    publications {
        register("mavenJava", MavenPublication::class) {
            groupId = "$groupCore.${project.name}"
            artifactId = project.name
            version = aggregateVersion
            pom.withXml {
                val dependenciesNode = asNode().appendNode("dependencies")

                mavenJar.allDependencies.forEach { dependency ->
                    val dependencyNode = dependenciesNode.appendNode("dependency")
                    dependencyNode.appendNode("groupId", dependency.group)
                    dependencyNode.appendNode("artifactId", dependency.name)
                    dependencyNode.appendNode("version", dependency.version)
                    dependencyNode.appendNode("scope", "compile")
                }
            }
            artifact("../linkserver-core/jar/${project.name}.jar")
        }
        register("mavenJavaLibs", MavenPublication::class) {
            groupId = "$groupCore.${project.name}"
            artifactId = CommonArtifacts.AGGREGATE_API_LIBS
            version = aggregateVersion
            artifact("../linkserver-core/jar/${CommonArtifacts.AGGREGATE_API_LIBS_JAR}")
        }
    }
}
