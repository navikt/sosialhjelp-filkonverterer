package no.nav.sosialhjelp.filkonvertering.config

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.micrometer.core.instrument.MeterRegistry
import no.nav.sosialhjelp.filkonvertering.client.GotenbergClient
import no.nav.sosialhjelp.filkonvertering.client.GotenbergClientImpl
import no.nav.sosialhjelp.filkonvertering.properties.FilkonverteringProperties
import no.nav.sosialhjelp.filkonvertering.service.FileConversionService
import no.nav.sosialhjelp.filkonvertering.service.FileConversionServiceImpl
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import

@SpringBootConfiguration
@AutoConfigureAfter(MetricsAutoConfiguration::class)
@EnableConfigurationProperties(FilkonverteringProperties::class)
@Import(CompositeMeterRegistryAutoConfiguration::class)
class FilkonverteringAutoConfiguration(
    private val filkonverteringProperties: FilkonverteringProperties,
    private val meterRegistry: MeterRegistry,
) {
    @Bean
    @ConditionalOnProperty("filkonvertering.gotenbergUrl")
    @ConditionalOnMissingBean
    fun gotenbergClient(): GotenbergClient {
        return GotenbergClientImpl(HttpClient(CIO), filkonverteringProperties.gotenbergUrl)
    }

    @Bean
    @ConditionalOnMissingBean
    fun fileConversionService(gotenbergClient: GotenbergClient): FileConversionService {
        return FileConversionServiceImpl(gotenbergClient, meterRegistry, filkonverteringProperties.metricPrefix)
    }
}