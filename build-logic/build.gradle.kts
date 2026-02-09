plugins {
    `kotlin-dsl`
}

dependencies {
    compileOnly(libs.plugins.android.application.toDep())
    compileOnly(libs.plugins.android.library.toDep())
    compileOnly(libs.plugins.kotlin.compose.toDep())
    compileOnly(libs.plugins.kotlin.serialization.toDep())
    compileOnly(libs.plugins.ksp.toDep())
    compileOnly(libs.plugins.hilt.toDep())
    compileOnly(libs.plugins.kotlin.jvm.toDep())
    compileOnly(libs.plugins.detekt.toDep())
}

fun Provider<PluginDependency>.toDep() = map {
    "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}"
}

gradlePlugin {
    plugins {
        register("kotlinLibrary") {
            id = "dynamicforms.kotlin.library"
            implementationClass = "KotlinLibraryConventionPlugin"
        }
        register("androidLibrary") {
            id = "dynamicforms.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidApplication") {
            id = "dynamicforms.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidFeature") {
            id = "dynamicforms.android.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("compose") {
            id = "dynamicforms.compose"
            implementationClass = "ComposeConventionPlugin"
        }
        register("hilt") {
            id = "dynamicforms.hilt"
            implementationClass = "HiltConventionPlugin"
        }
        register("detekt") {
            id = "dynamicforms.detekt"
            implementationClass = "DetektConventionPlugin"
        }
    }
}
