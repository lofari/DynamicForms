plugins {
    id("dynamicforms.kotlin.library")
    id("dynamicforms.detekt")
}

dependencies {
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
}
