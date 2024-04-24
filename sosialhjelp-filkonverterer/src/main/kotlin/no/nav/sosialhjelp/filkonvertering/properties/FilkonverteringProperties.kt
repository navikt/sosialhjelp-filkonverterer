package no.nav.sosialhjelp.filkonvertering.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "filkonvertering")
data class FilkonverteringProperties(val gotenbergUrl: String, val metricPrefix: String? = null)
