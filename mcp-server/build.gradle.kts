java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    // Use only local JAR files to avoid SSL issues
    implementation(files("../libs/aggregate-api.jar"))
    implementation(files("../libs/aggregate-api-libs.jar"))
    
    // Jackson JARs should be manually added to libs/ directory
    // Expected files:
    // - jackson-core-2.14.1.jar (or compatible version)
    // - jackson-databind-2.14.1.jar
    // - jackson-annotations-2.14.1.jar
    fileTree(mapOf("dir" to "../libs", "include" to listOf("jackson-*.jar"))).forEach { file ->
        implementation(files(file))
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    // Force Java 8 bytecode compatibility
    options.compilerArgs.addAll(listOf("-source", "1.8", "-target", "1.8"))
}

tasks.withType<Jar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    archiveBaseName.set("aggregate-mcp-server")
    archiveVersion.set("1.0.0")
    
    manifest {
        attributes(
            "Main-Class" to "com.tibbo.aggregate.mcp.Main",
            "Implementation-Title" to "AggreGate MCP Server",
            "Implementation-Version" to "1.0.0"
        )
    }
    
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }) {
        exclude("META-INF/*.SF")
        exclude("META-INF/*.DSA")
        exclude("META-INF/*.RSA")
        exclude("META-INF/*.MF")
    }
    from("build/classes/java/main")
    from("build/resources/main")
}
