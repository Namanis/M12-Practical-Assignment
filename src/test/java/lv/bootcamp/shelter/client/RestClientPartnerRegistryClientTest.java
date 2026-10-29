package lv.bootcamp.shelter.client;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RestClientPartnerRegistryClientTest {

    private MockWebServer mockWebServer;
    private RestClientPartnerRegistryClient restClientPartnerRegistryClient;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        restClientPartnerRegistryClient = new RestClientPartnerRegistryClient(RestClient.builder(), mockWebServer.url("/").toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void lookup_shouldParseSuccessfullyResponse() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {"registered": true, "partnerShelterName": "Riga Shelter"}
                        """));

        PartnerRegistryResult result = restClientPartnerRegistryClient.lookup("chip-1");

        assertThat(result.registered()).isTrue();
        assertThat(result.partnerShelterName()).isEqualTo("Riga Shelter");

        RecordedRequest request = mockWebServer.takeRequest();

        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath()).isEqualTo("/registry/chip-1");
    }

    @Test
    void lookup_shouldThrowWhenPartnerCanNotLookup(){
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(404));

        assertThatThrownBy(() -> restClientPartnerRegistryClient.lookup("chip-1"))
                .isInstanceOf(HttpClientErrorException.class)
                .hasMessageContaining("404");
    }

    @Test
    void lookup_shouldThrowWhenPartnerFails() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500));

        assertThatThrownBy(() -> restClientPartnerRegistryClient.lookup("chip-1"))
                .isInstanceOf(HttpServerErrorException.class)
                .hasMessageContaining("500");
    }


}