plugins {
    id("dynamicforms.kotlin.library")
    id("dynamicforms.detekt")
}

dependencies {
    implementation(libs.kotlinx.coroutines.test)
    implementation(libs.junit)
}
