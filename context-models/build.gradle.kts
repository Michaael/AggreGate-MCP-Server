import provider.*
import signing.*

val mavenJar by configurations.creating
val jarName = project.name.substringAfter("-")

dependencies {
    api(project(":aggregate-extensions"))
    api(project(":linkserver-core"))
    testImplementation(project(":linkserver-core").dependencyProject.sourceSets.test.get().output)
    api(project(":context-queries"))

    mavenJar("$groupContext.queries:queries:$aggregateVersion")
    mavenJar("$groupContext.event-filters:event-filters:$aggregateVersion")
    mavenJar("$groupCore.aggregate-extensions:aggregate-extensions:$aggregateVersion")
}

tasks.withType<Jar> {
    from("build/classes/java/main")
    from("build/classes/java/test")
}

tasks.register("signContextModelsJar", SignJar::class) {
    jarPath = "../linkserver-core/plugins/context/$jarName.jar"
}

publishing {
    publications {
        register("mavenJava", MavenPublication::class) {
            groupId = "$groupContext.${jarName}"
            artifactId = jarName
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
            artifact("../linkserver-core/plugins/context/$jarName.jar")
        }
    }
}