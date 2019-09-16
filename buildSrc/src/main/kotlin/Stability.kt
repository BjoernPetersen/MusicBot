import java.util.Locale

private val UNSTABLE_KEYWORDS = listOf(
    "alpha",
    "beta"
)

fun isUnstable(version: String, currentVersion: String): Boolean {
    val lowerVersion = version.toLowerCase(Locale.US)
    val lowerCurrentVersion = currentVersion.toLowerCase(Locale.US)
    return UNSTABLE_KEYWORDS.any { it in lowerVersion && it !in lowerCurrentVersion }
}
