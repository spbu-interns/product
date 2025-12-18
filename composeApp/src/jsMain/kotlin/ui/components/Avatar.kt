package ui.components

import io.kvision.html.Div

private const val AVATAR_BASE_URL = "http://127.0.0.1:8001"

private fun escapeUrl(url: String): String = url
    .replace("\"", "%22")
    .replace("'", "%27")

private fun resolveAvatarUrl(rawUrl: String?): String? {
    val url = rawUrl?.trim().orEmpty()
    if (url.isBlank()) return null
    if (url.startsWith("http://") || url.startsWith("https://")) return url

    val base = AVATAR_BASE_URL.trimEnd('/')

    if (url.startsWith("/")) {
        return base + url
    }

    val filename = url.substringAfterLast('/')
    val match = Regex("""^user_(\d+)_""").find(filename)
    if (match != null) {
        val userId = match.groupValues[1]
        return "$base/users/$userId/avatar"
    }

    return "$base/avatars/$url"
}

fun Div.updateAvatar(imageUrl: String?, initials: String) {
    removeAll()

    val finalUrl = resolveAvatarUrl(imageUrl)
    if (finalUrl == null) {
        removeCssClass("has-image")
        removeStyle("background-image")
        +initials
        return
    }

    addCssClass("has-image")
    val safeUrl = escapeUrl(finalUrl)
    setStyle("background-image", "url('$safeUrl')")
    setStyle("background-size", "cover")
    setStyle("background-position", "center")
    setStyle("background-repeat", "no-repeat")
}
