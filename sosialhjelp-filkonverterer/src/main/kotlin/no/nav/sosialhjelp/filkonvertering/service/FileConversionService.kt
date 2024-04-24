package no.nav.sosialhjelp.filkonvertering.service

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.sosialhjelp.filkonvertering.client.GotenbergClient
import no.nav.sosialhjelp.filkonvertering.exception.FileConversionException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus

private const val TAG_TIKA_MIME_TYPE = "tika_mime_type"
private const val TAG_CLIENT_MIME_TYPE = "client_mime_type"
private const val TAG_FILE_EXTENSION = "file_extension"
private const val TAG_ERROR_CLASS = "error_class"

interface FileConversionService {
    suspend fun convertFileToPdf(files: List<VedleggKonverteringOpplasting>): Map<VedleggKonverteringOpplasting, Result<ByteArray>>
}

class FileConversionServiceImpl(
    private val gotenbergClient: GotenbergClient,
    private val meterRegistry: MeterRegistry,
    private val metricPrefix: String?,
) : FileConversionService {
    private val log = LoggerFactory.getLogger(this::class.java)

    private val pdfConversionSuccess = metricPrefix?.let { Counter.builder("${metricPrefix}_pdf_conversion_success") }
    private val pdfConversionFailure = metricPrefix?.let { Counter.builder("${metricPrefix}_pdf_conversion_failure") }

    override suspend fun convertFileToPdf(
        files: List<VedleggKonverteringOpplasting>,
    ): Map<VedleggKonverteringOpplasting, Result<ByteArray>> =
        coroutineScope {
            log.info("Konverterer {} vedlegg til PDF", files.size)

            files.associateWith { file ->
                async(Dispatchers.IO) {
                    runCatching {
                        gotenbergClient.convertToPdf(file.unconvertedName, file.bytes)
                    }.onSuccess { pdfBytes ->
                        if (pdfBytes.isEmpty()) {
                            throw FileConversionException(HttpStatus.BAD_REQUEST.value(), "Konvertert fil [$file] er tom.", "")
                        }

                        pdfConversionSuccess
                            ?.tag(TAG_TIKA_MIME_TYPE, file.mimeType)
                            ?.tag(TAG_CLIENT_MIME_TYPE, file.file.contentType ?: "undefined")
                            ?.tag(TAG_FILE_EXTENSION, file.extension)
                            ?.register(meterRegistry)
                            ?.increment()
                    }.onFailure { e ->
                        log.warn("Feil ved konvertering av fil [$file]", e)
                        pdfConversionFailure
                            ?.tag(TAG_TIKA_MIME_TYPE, file.mimeType)
                            ?.tag(TAG_CLIENT_MIME_TYPE, file.file.contentType ?: "undefined")
                            ?.tag(TAG_FILE_EXTENSION, file.extension)
                            ?.tag(TAG_ERROR_CLASS, "${e::class}")
                            ?.register(meterRegistry)
                            ?.increment()
                    }
                }
            }.mapValues { it.value.await() }
        }
}
