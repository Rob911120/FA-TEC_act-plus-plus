# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep ARCore classes
-keep class com.google.ar.** { *; }

# Keep SceneView classes
-keep class io.github.sceneview.** { *; }

# Keep DXF generator classes
-keep class com.fatec.armeasure.utils.** { *; }
