// Используем исходники из sdk-temp/aggregate-api
sourceSets {
    main {
        java {
            srcDir("../../sdk-temp/aggregate-api/src/main/java")
        }
        resources {
            srcDir("../../sdk-temp/aggregate-api/src/main/java")
            exclude("**/*.java")
        }
    }
    test {
        java {
            srcDir("../../sdk-temp/aggregate-api/src/test/java")
        }
    }
}

dependencies {
    // Зависимости из последнего SDK 1.3.6 (sdk-temp/buildSrc/src/main/java/Dependencies.kt)
    // Обновлено до актуальных версий из AggreGate-SDK-CE
    
    // Apache Commons
    api("commons-net:commons-net:3.3")
    api("commons-beanutils:commons-beanutils:1.9.4")
    api("commons-logging:commons-logging:1.2")
    api("org.apache.commons:commons-lang3:3.14.0")
    api("commons-io:commons-io:2.11.0")
    api("org.apache.commons:commons-math3:3.6.1")
    
    // Log4j 2.23.1 - последняя версия с исправлениями безопасности
    api("org.apache.logging.log4j:log4j-api:2.23.1")
    api("org.apache.logging.log4j:log4j-core:2.23.1")
    api("org.apache.logging.log4j:log4j-1.2-api:2.23.1")
    api("org.apache.logging.log4j:log4j-slf4j-impl:2.23.1")
    api("org.slf4j:slf4j-api:2.0.9")
    
    // Другие библиотеки
    api("net.sourceforge.javacsv:javacsv:2.1")
    api("xalan:xalan:2.7.2")
    api("com.googlecode.json-simple:json-simple:1.1.1") {
        exclude(group = "junit", module = "junit")
    }
    
    api("com.google.code.findbugs:jsr305:3.0.2")
    // Guava 32.1.3-jre - последняя версия, поддерживающая Java 8
    api("com.google.guava:guava:32.1.3-jre")
    
    api("net.sf.jpf:jpf:1.5.1")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    sourceCompatibility = JavaVersion.VERSION_1_8.toString()
    targetCompatibility = JavaVersion.VERSION_1_8.toString()
}

