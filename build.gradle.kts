// Top-level build file where you can add configuration options common to all sub-projects/modules.

/**
 * Patched versions of libraries the build tools bring in with them (AGP,
 * lint, Robolectric), listed once as `buildToolFloors` in gradle.properties.
 * None of these reach the APK — releaseRuntimeClasspath carries only Guava,
 * already past its advisories — but they run on the build machine, and GitHub
 * listed them (54 alerts, 25 Sep 2026). Each entry is a floor: a tool asking
 * for an older version gets this one, a newer request is left alone. Drop an
 * entry once the tool bringing it ships at or above it (`./gradlew
 * buildEnvironment`, `./gradlew :app:dependencies`).
 */
buildscript {
  val floors = (project.findProperty("buildToolFloors") as String).split(",").map { it.trim() }
  dependencies { constraints { floors.forEach { classpath(it) } } }
}

subprojects {
  val floors = (rootProject.findProperty("buildToolFloors") as String).split(",").map { it.trim() }
  fun parts(v: String) = v.split('.', '-').map { it.toIntOrNull() ?: 0 }
  fun below(v: String, floor: String): Boolean {
    val a = parts(v)
    val b = parts(floor)
    for (i in 0 until maxOf(a.size, b.size)) {
      val x = a.getOrElse(i) { 0 }
      val y = b.getOrElse(i) { 0 }
      if (x != y) return x < y
    }
    return false
  }
  configurations.configureEach {
    resolutionStrategy.eachDependency {
      val floor = floors.firstOrNull { it.startsWith("${requested.group}:${requested.name}:") } ?: return@eachDependency
      val wanted = floor.substringAfterLast(':')
      val current = requested.version ?: return@eachDependency
      if (below(current, wanted)) {
        useVersion(wanted)
        because("patched release; see buildToolFloors in gradle.properties")
      }
    }
  }
}

plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.google.devtools.ksp) apply false
  alias(libs.plugins.roborazzi) apply false
  alias(libs.plugins.secrets) apply false
  alias(libs.plugins.google.services) apply false
  alias(libs.plugins.google.firebase.crashlytics) apply false
}
