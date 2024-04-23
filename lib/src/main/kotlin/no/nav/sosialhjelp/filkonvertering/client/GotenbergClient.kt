package no.nav.sosialhjelp.filkonvertering.client

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.headers
import io.ktor.http.isSuccess
import no.nav.sosialhjelp.filkonvertering.exception.FileConversionException
import org.springframework.stereotype.Component

@Component
class GotenbergClient(private val httpClient: HttpClient) {
    suspend fun convertToPdf(
        filename: String,
        bytes: ByteArray,
    ): ByteArray {
        val response =
            httpClient.post("http://localhost:3000/forms/libreoffice/convert") {
                headers {
                    append("Content-Disposition", "attachment; filename=\"$filename\"")
                }
                contentType(ContentType.MultiPart.FormData)
                accept(ContentType.Application.Pdf)
                accept(ContentType.Text.Plain)

                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append("files", bytes)
                        },
                    ),
                )
            }
        val traceHeader = response.headers["gotenberg-trace"] ?: "[N/A]"
        if (!response.status.isSuccess()) {
            throw FileConversionException(response.status.value, response.body(), traceHeader)
        }

        return response.body()
    }
}
