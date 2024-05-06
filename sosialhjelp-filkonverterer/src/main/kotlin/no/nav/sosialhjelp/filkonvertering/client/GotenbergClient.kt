package no.nav.sosialhjelp.filkonvertering.client

import no.nav.sosialhjelp.filkonvertering.exception.FileConversionException
import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.io.ByteArrayInputStream
import java.io.File

private const val LIBRE_OFFICE_ROUTE = "/forms/libreoffice/convert"

interface GotenbergClient {
    suspend fun convertToPdf(
        filename: String,
        contentType: String,
        body: ByteArray,
    ): ByteArray
}

private const val GOTENBERG_TRACE_HEADER = "gotenberg-trace"

class GotenbergClientImpl(
    private val webClientBuilder: WebClient.Builder,
    private val gotenbergUrl: String,
) : GotenbergClient {
    private val log = LoggerFactory.getLogger(this::class.java)

    private var trace = "[NA]"

    override suspend fun convertToPdf(
        filename: String,
        contentType: String,
        bytes: ByteArray,
    ): ByteArray {
        val multipartBody =
            MultipartBodyBuilder().run {
                part("files", ByteArrayMultipartFile(filename, bytes, contentType).resource)
                build()
            }

        log.debug("Kaller gotenberg på url ${gotenbergUrl + LIBRE_OFFICE_ROUTE}")
        return buildWebClient().post()
            .uri(LIBRE_OFFICE_ROUTE)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$filename\"")
            .body(BodyInserters.fromMultipartData(multipartBody))
            .exchangeToMono { evaluateClientResponse(it) }
            .block() ?: throw IllegalStateException("[$trace] Innhold i konvertert fil \"$filename\" er null.")
    }

    private fun evaluateClientResponse(response: ClientResponse): Mono<ByteArray> {
        trace = response.headers().header(GOTENBERG_TRACE_HEADER).first()
        log.info("[$trace] Konverterer fil")

        return if (response.statusCode().is2xxSuccessful) {
            response.bodyToMono(ByteArray::class.java)
        } else {
            response.bodyToMono(String::class.java)
                .flatMap { body -> Mono.error(FileConversionException(response.statusCode().value(), body, trace)) }
        }
    }

    private fun buildWebClient(): WebClient {
        return webClientBuilder
            .baseUrl(gotenbergUrl)
            .defaultHeaders {
                it.contentType = MediaType.MULTIPART_FORM_DATA
                it.accept = listOf(MediaType.APPLICATION_PDF, MediaType.TEXT_PLAIN)
            }.build()
    }
}

private class ByteArrayMultipartFile(
    private val filnavn: String,
    private val bytes: ByteArray,
    private val contentType: String,
) : MultipartFile {
    override fun getInputStream() = ByteArrayInputStream(bytes)

    override fun getName() = "file"

    override fun getOriginalFilename() = filnavn

    override fun getContentType() = contentType

    override fun isEmpty(): Boolean = bytes.isEmpty()

    override fun getSize() = bytes.size.toLong()

    override fun getBytes() = bytes

    override fun transferTo(dest: File) {
        FileUtils.writeByteArrayToFile(dest, bytes)
    }
}
