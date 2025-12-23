package utils

import kotlinx.browser.document
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag
import org.w3c.dom.url.URL
import org.w3c.dom.HTMLAnchorElement
import kotlin.js.*

fun downloadPdf(bytes: ByteArray, filename: String) {
    val buffer = bytes.asDynamic().buffer

    val blob = Blob(arrayOf(buffer), BlobPropertyBag(type = "application/pdf"))
    val url = URL.createObjectURL(blob)

    val a = document.createElement("a") as HTMLAnchorElement
    a.href = url
    a.download = filename
    document.body?.appendChild(a)
    a.click()
    document.body?.removeChild(a)

    URL.revokeObjectURL(url)
}