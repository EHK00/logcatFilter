import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "1.9.21"
    id("org.jetbrains.compose") version "1.5.11"
}

group = "com.logcatfilter"
version = "1.0.0"

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.7.3")
    
    // Testing
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}

compose.desktop {
    application {
        mainClass = "MainKt"
        
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "LogcatFilter"
            packageVersion = "1.0.0"
            description = "Android ADB Logcat Viewer & Filter"
            vendor = "LogcatFilter"
            
            // macOS 설정
            macOS {
                bundleID = "com.logcatfilter.app"
                // 아이콘 파일이 있으면 설정 (없으면 기본 아이콘 사용)
                // iconFile.set(project.file("src/main/resources/icon.icns"))
            }
            
            // Windows 설정
            windows {
                menuGroup = "LogcatFilter"
                perUserInstall = true
                // 아이콘 파일이 있으면 설정
                // iconFile.set(project.file("src/main/resources/icon.ico"))
            }
            
            // Linux 설정
            linux {
                // iconFile.set(project.file("src/main/resources/icon.png"))
            }
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}
