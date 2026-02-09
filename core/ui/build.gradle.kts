plugins {
    id("dynamicforms.android.library")
    id("dynamicforms.compose")
    id("dynamicforms.detekt")
}

android {
    namespace = "com.lfr.dynamicforms.core.ui"
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.retrofit)
}
