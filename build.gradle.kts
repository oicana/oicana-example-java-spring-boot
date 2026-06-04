plugins {
    java
    id("org.springframework.boot") version "3.4.3"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.oicana"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<JavaExec> {
    jvmArgs("--enable-native-access=com.oicana")
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.6")
    implementation("com.oicana:oicana:0.2.0")
    // Since this is an example project, we add all native implementations.
    // In your project, only add what you need.
    runtimeOnly("com.oicana:oicana-linux-x86_64:0.2.0")
    runtimeOnly("com.oicana:oicana-linux-aarch64:0.2.0")
    runtimeOnly("com.oicana:oicana-macos-x86_64:0.2.0")
    runtimeOnly("com.oicana:oicana-macos-aarch64:0.2.0")
    runtimeOnly("com.oicana:oicana-windows-x86_64:0.2.0")
}
