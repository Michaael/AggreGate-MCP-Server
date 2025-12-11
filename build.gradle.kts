plugins {
    java
    // id("org.jetbrains.gradle.plugin.idea-ext") version "0.7" // Commented out to avoid SSL issues
}

tasks {
    named<Wrapper>("wrapper") {
        gradleVersion = "6.9"
        distributionType = Wrapper.DistributionType.ALL
    }
}

allprojects {
    apply(plugin = "java")
    apply(plugin = "java-library")
    repositories {
        mavenCentral()
        maven(url = "https://store.aggregate.digital/repository/maven-public")
    }
    configurations.all {
        resolutionStrategy {
            setForcedModules("commons-codec:commons-codec:1.6")
        }
        exclude(group = "xerces", module = "xercesImpl")
        exclude(group = "xerces", module = "xmlParserAPIs")
    }
    tasks.withType<JavaCompile> {
        options.isFork = true
        options.isIncremental = true
        options.isWarnings = false
    }

    if (name.startsWith("demo-") || name == "aggregate-api") {
        dependencies {
            testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
            testImplementation("org.junit.jupiter:junit-jupiter-params:5.9.2")
            testImplementation("org.junit.platform:junit-platform-suite:1.9.2")
            testImplementation("junit", "junit", "4.13.2")
            testImplementation("org.awaitility", "awaitility", "4.2.0")
            // Commented out due to SSL issues with buildSrc dependencies
            // testImplementation(XStreamLibs.hamcrestAll)
            // testImplementation(MockitoLibs.mockitoCore)
            testRuntimeOnly("org.junit.vintage:junit-vintage-engine")  // to let JUnit 5 engine run old JUnit tests
        }
    }
}

project(":modules:context-demo-web-app") {
    tasks.withType<JavaCompile> {
        onlyIf {
            project.hasProperty("web")
        }
    }
}



