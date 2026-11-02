# Order Desk desktop release build. Compose Desktop adds its own default rules; these are the app's.

# The entry point and the memory probe started by system property.
-keep class io.github.kormcrete.orderdesk.desktop.MainKt { *; }
-keep class io.github.kormcrete.orderdesk.desktop.MemoryProbe* { *; }

# Generated Compose resources (fonts) are looked up by name.
-keep class io.github.kormcrete.lagoon.compose.resources.** { *; }

# Optional platform pieces that are not on every classpath.
-dontwarn kotlinx.coroutines.debug.**
-dontwarn org.jetbrains.skiko.**
