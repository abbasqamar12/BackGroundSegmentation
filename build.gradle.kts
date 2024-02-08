// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()  // Google's Maven repository
        mavenCentral()
    }

}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven {
            setUrl("https://sdk.developer.deepar.ai/maven-android-repository/releases/")
        }
    }
}
plugins {
    id("com.android.application") version "8.1.2" apply false
}