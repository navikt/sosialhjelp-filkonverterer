package no.nav.sosialhjelp.filkonvertering.config

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class HttpClientConfig {
    @Bean
    fun ktorClient(): HttpClient {
        return HttpClient(CIO)
    }
}
