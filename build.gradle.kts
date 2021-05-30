plugins {
    java
}

group = "xyz.acrylicstyle"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://repo2.acrylicstyle.xyz") }
}

dependencies {
    implementation("xyz.acrylicstyle:api:0.8.1")
    compileOnly("org.spigotmc:spigot-api:1.12.2-R0.1-SNAPSHOT")
}

tasks {
    withType<JavaCompile>().configureEach {
        options.encoding = "utf-8"
    }

    withType<ProcessResources> {
        filteringCharset = "UTF-8"
        from(sourceSets.main.get().resources.srcDirs) {
            include("**")
            val tokenReplacementMap = mapOf("version" to project.version)
            filter<org.apache.tools.ant.filters.ReplaceTokens>("tokens" to tokenReplacementMap)
        }

        from(projectDir) { include("LICENSE") }
    }

    withType<Jar> {
        from(configurations.getByName("implementation").apply { isCanBeResolved = true }.map { if (it.isDirectory) it else zipTree(it) })
    }
}
