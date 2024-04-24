package no.nav.sosialhjelp.filkonvertering.exception

data class FileConversionException(
    val httpStatus: Int,
    val msg: String,
    val trace: String,
) : RuntimeException("[$trace] Feil i filkonvertering: $httpStatus - $msg")
