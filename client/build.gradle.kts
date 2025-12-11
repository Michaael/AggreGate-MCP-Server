import provider.*
import publication.*
import signing.*

val pluginGroupId = "$groupCore.${project.name}"



dependencies {
    api(project(":gui-builder"))
    api(project(":aggregate-components"))
    api(project(":aggregate-extensions"))
    api(project(":process-control-ide"))
    testImplementation(project(":aggregate-commons").dependencyProject.sourceSets.test.get().output)
    testImplementation(project(":widget-runtime").dependencyProject.sourceSets.test.get().output)

    api(files(JideLibs.jideDiff))

    api(files("../libs/jdk-tools/jnlp.jar"))
}

tasks.named("jar", Jar::class) {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from("build/classes/java/main")
    from("build/classes/java/test") {
        exclude("Test*")
        exclude("com/tibbo/platform/tests/**")
    }
    from("build/resources/main")
    destinationDirectory.set(File("../linkserver-core/jar"))

    manifest {
        attributes["Main-Class"] = "com.tibbo.aggregate.client.Client"
        attributes["Implementation-Title"] = "AggreGate Client"
        attributes["Implementation-Vendor"] = "Tibbo Systems Inc."
        attributes["Implementation-Version"] = aggregateVersion
        if (project.hasProperty("buildNumberFromTrigger")) {
            println("buildNumberFromTrigger = " + project.findProperty("buildNumberFromTrigger"))
            attributes["Build-Number"] = project.property("buildNumberFromTrigger") as String
        }
        else{
            attributes["Build-Number"] = System.getenv("BUILD_NUMBER") ?: "SNAPSHOT"

        }
        println("Build-Number = " + attributes["Build-Number"])
    }
}

tasks.register("signClientJar", SignJar::class) {
    jarPath = "../linkserver-core/jar/client.jar"
}

fun packageMacro(): Zip {
    return tasks.create("macro", Zip::class) {
        archiveFileName.set("macro.zip")
        destinationDirectory.set(file("."))
        from("macro")
    }
}

tasks.create("cleanArchives", Delete::class) {
    delete(file("client/macro.zip"))
}

publishing {
    publications {
        register("mavenJava", MavenPublication::class) {
            groupId = groupClient
            artifactId = "client"
            version = aggregateVersion
            pom.withXml {
                val dependenciesNode = asNode().appendNode("dependencies")

                PomUtils.addDependency(dependenciesNode, "$groupCore.aggregate-components", "aggregate-components", aggregateVersion)
                PomUtils.addDependency(dependenciesNode, "$groupCore.aggregate-extensions", "aggregate-extensions", aggregateVersion)
                PomUtils.addDependency(dependenciesNode, "$groupCore.gui-builder", "gui-builder", aggregateVersion)
                PomUtils.addDependency(dependenciesNode, pluginGroupId, "client-libs", aggregateVersion)
                PomUtils.addDependency(dependenciesNode, pluginGroupId, "macro", aggregateVersion, "zip")
                PomUtils.addDependency(dependenciesNode, pluginGroupId, "mp3spi1.9.5", aggregateVersion)
                PomUtils.addDependency(dependenciesNode, pluginGroupId, "logging-client", aggregateVersion, "xml")
            }
            artifact("../linkserver-core/jar/client.jar")
        }

        register("mavenJavaLibs", MavenPublication::class) {
            groupId = groupClient
            artifactId = "client-libs"
            version = aggregateVersion
            artifact("../linkserver-core/jar/client-libs.jar")
        }

        register("mavenLogging", MavenPublication::class) {
            groupId = groupClient
            artifactId = "logging-client"
            version = aggregateVersion
            artifact(file("logging-client.xml"))
        }

        register("mavenLib", MavenPublication::class) {
            groupId = groupClient
            artifactId = "mp3spi1.9.5"
            version = aggregateVersion
            artifact(file("../libs/mp3-spi-1.9.5/mp3spi1.9.5.jar"))
        }

        register("mavenPostInstallScript", MavenPublication::class) {
            groupId = groupClient
            artifactId = "post-install-script"
            version = aggregateVersion
            artifact("post-install-script.java") {
                artifactId = "post-install-script"
            }
        }
        register("mavenMacro", MavenPublication::class) {
            groupId = groupClient
            artifactId = "macro"
            version = aggregateVersion
            artifact(packageMacro())
        }
    }
}

tasks.named("cleanArchives").get().shouldRunAfter("publish")