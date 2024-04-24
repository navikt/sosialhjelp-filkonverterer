package no.nav.sosialhjelp.filkonvertering.service

import no.nav.sosialhjelp.filkonvertering.exception.FileConversionException
import org.apache.tika.Tika
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.multipart.MultipartFile
import java.io.File

private fun detectMimeType(bytes: ByteArray?): String {
    val mimeType = Tika().detect(bytes).lowercase()
    return if (mimeType == MimeTypes.TEXT_X_MATLAB) MimeTypes.APPLICATION_PDF else mimeType
}

private object MimeTypes {
    const val APPLICATION_PDF = "application/pdf"
    const val TEXT_X_MATLAB = "text/x-matlab"
}

data class VedleggKonverteringOpplasting(val file: MultipartFile) {
    val unconvertedName = validatedFilename(file.originalFilename)
    private val splitFilename = splitFilename(unconvertedName)
    val extension = splitFilename.second
    val convertedName = "${splitFilename.first}.pdf"
    val mimeType = detectMimeType(file.bytes)

    val bytes: ByteArray = file.bytes

    override fun toString(): String = "FileConversionUpload(mime='$mimeType', extension='$extension', size=${bytes.size}b)"

    private fun validatedFilename(filename: String?): String {
        if (filename.isNullOrBlank()) {
            throw FileConversionException(HttpStatus.BAD_REQUEST.value(), "Filnavn er tomt", "")
        }
        return filename
    }

    init {
        if (this.file.isEmpty) {
            throw FileConversionException(HttpStatus.BAD_REQUEST.value(), "Fil for konvertering er tom.", "")
        }
        if (this.file.contentType != this.mimeType) {
            log.warn("Ulik MIME mellom klientdata og Tika: ${this.file.contentType} != ${this.mimeType}")
        }
    }

    private fun splitFilename(filename: String): Pair<String, String> {
        return File(filename).let {
            if (it.extension.isBlank()) {
                throw FileConversionException(HttpStatus.BAD_REQUEST.value(), "Finner ikke filtype", "")
            }
            Pair(it.nameWithoutExtension, it.extension)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
    }
}
