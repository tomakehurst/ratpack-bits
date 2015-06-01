import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.http.Fault
import ratpack.exec.Execution
import ratpack.exec.Promise
import ratpack.handling.Context
import ratpack.handling.InjectionHandler
import ratpack.http.client.HttpClient
import ratpack.test.embed.EmbeddedApp
import ratpack.test.exec.ExecHarness
import spock.lang.Specification
import spock.lang.Timeout

import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future

import static com.github.tomakehurst.wiremock.client.WireMock.*
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig

class HttpClientSpec extends Specification {

    WireMockServer wm

    void setup() {
        wm = new WireMockServer(wireMockConfig().dynamicPort())
        wm.start()
    }

    @Timeout(5)
    void 'demonstrate http client hanging when connection is prematurely dropped'() {
        given:
            wm.stubFor(get(urlEqualTo('/test')).willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE)))

        expect:
            EmbeddedApp.fromHandler(new InjectionHandler() {
                void handle(Context context, HttpClient httpClient) {
                    def mockServiceUrl = "http://localhost:${wm.port()}/test"
                    httpClient.get(URI.create(mockServiceUrl)) { req ->
                        req.readTimeout(Duration.ofSeconds(2))
                    }
                    .onError {
                        context.response.status(500).send "Fail!"
                    }
                    .then {
                        context.response.status(200).send "You'll never see this"
                    }
                }
            }).test { testHttpClient ->
                assert testHttpClient.getText() == "You'll never see this"
            }

    }

}