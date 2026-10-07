plugins {
    java
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.oicana"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    manifest { attributes("Enable-Native-Access" to "ALL-UNNAMED") }
}

tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.1")
    implementation("com.oicana:oicana:0.9.0")
    // Since this is an example project, we add all native implementations.
    // In your project, only add what you need.
    runtimeOnly("com.oicana:oicana-linux-x86_64:0.9.0")
    runtimeOnly("com.oicana:oicana-linux-aarch64:0.9.0")
    runtimeOnly("com.oicana:oicana-macos-x86_64:0.9.0")
    runtimeOnly("com.oicana:oicana-macos-aarch64:0.9.0")
    runtimeOnly("com.oicana:oicana-windows-x86_64:0.9.0")
}
