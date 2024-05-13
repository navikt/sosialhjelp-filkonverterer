package no.nav.sosialhjelp.filkonvertering.config

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
import org.springframework.web.reactive.function.client.WebClient

@SpringBootConfiguration
@AutoConfigureAfter(MetricsAutoConfiguration::class)
@EnableConfigurationProperties(FilkonverteringProperties::class)
@Import(CompositeMeterRegistryAutoConfiguration::class)
class FilkonverteringAutoConfiguration(
    private val filkonverteringProperties: FilkonverteringProperties,
    private val meterRegistry: MeterRegistry,
) {
    @Bean
    @ConditionalOnMissingBean
    fun webClientBuilder(): WebClient.Builder {
        return WebClient.builder().codecs {
            it.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)
        }
    }

    @Bean
    @ConditionalOnProperty("filkonvertering.gotenbergUrl")
    @ConditionalOnMissingBean
    fun gotenbergClient(webClientBuilder: WebClient.Builder): GotenbergClient {
        return GotenbergClientImpl(webClientBuilder, filkonverteringProperties.gotenbergUrl)
    }

    @Bean
    @ConditionalOnMissingBean
    fun fileConversionService(gotenbergClient: GotenbergClient): FileConversionService {
        return FileConversionServiceImpl(gotenbergClient, meterRegistry, filkonverteringProperties.metricPrefix)
    }
}
