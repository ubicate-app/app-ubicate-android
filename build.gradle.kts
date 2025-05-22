// Este es el archivo build.gradle a nivel de proyecto
buildscript {

    repositories {
        google() // Repositorio de Google
        mavenCentral() // Repositorio central de Maven
    }
    dependencies {
        // Añadir el complemento de Secrets Gradle Plugin
        classpath("com.google.android.libraries.mapsplatform.secrets-gradle-plugin:secrets-gradle-plugin:2.0.1")

    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false

}
