package com.betting;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

// import static org.junit.Assert.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
public class AppIntegrationTest {

    public static final int SERVER_PORT = 8001;
    private String baseUrl;
    

    @Container
    public static GenericContainer<?> appContainer = new GenericContainer<>(
    DockerImageName.parse("localhost:5000/stakes-service:1.0-SNAPSHOT"))
    .withExposedPorts(SERVER_PORT)
    // .waitingFor(Wait.forHttp("/123/session"))
    .withCreateContainerCmdModifier(cmd -> cmd.getHostConfig()
    .withCpuCount(2L)
    .withMemory(1024L * 1024 * 1024)) ;// 1GB内存
    // .withCommand("java", "-Xmx512m", "-Xms256m", "-jar", "app.jar");
    // public static GenericContainer<?> appContainer = new GenericContainer<>(
    //         new JibImage(
    //                 "stakes-service:1.0-SNAPSHOT",
    //                 jibContainerBuilder -> {
    //                     return jibContainerBuilder.setEntrypoint("echo", "Hello World");
    //                 }))
    //         .withStartupCheckStrategy(new OneShotStartupCheckStrategy().withTimeout(Duration.ofSeconds(3)));

    @BeforeEach
    void setUp() {
        baseUrl = "http://" + appContainer.getHost() + ":" + appContainer.getMappedPort(SERVER_PORT);
        // baseUrl = "http://localhost:8001";
    }

    private  HttpResponse<String> getOneSession4Customer(int customerId) throws IOException, InterruptedException{

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/" + customerId + "/session"))
                .build();

        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private  HttpResponse<String> postCustomerStake(int betofferid, String sessionKey, int stake) throws IOException, InterruptedException{

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/" + betofferid + "/stake?sessionkey=" + sessionKey))
                .POST(HttpRequest.BodyPublishers.ofString( String.valueOf(stake) ))
                .build();

        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    ///for get <betofferid>/highstakes
    private  HttpResponse<String> getHighStakes4Betoffer(int betofferid) throws IOException, InterruptedException{

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/" + betofferid + "/highstakes"))
                .build();

        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void shouldGetOneSession() throws IOException, InterruptedException {

        int customerId = ThreadLocalRandom.current().nextInt(1000,1000000);

        HttpResponse<String> response = this.getOneSession4Customer(customerId);

        System.out.println("Status Code: " + response.statusCode());
        System.out.println("Response Body: " + response.body());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isInstanceOf(String.class);
    }
    @Test
    void shouldPostStake() throws IOException, InterruptedException {

        int betofferid = ThreadLocalRandom.current().nextInt(1000,1000000);
        int stake = ThreadLocalRandom.current().nextInt(1000,1000000);
        int customerId = ThreadLocalRandom.current().nextInt(1000,1000000);

        HttpResponse<String> sessionResponse = this.getOneSession4Customer(customerId);

        String sessionKey = sessionResponse.body();

        HttpResponse<String> response = this.postCustomerStake(betofferid, sessionKey, stake);


        System.out.println("Status Code: " + response.statusCode());

        assertThat(response.statusCode()).isEqualTo(200);
    }

    @Test
    void shouldGetHighStakes() throws IOException, InterruptedException {

        int betofferid = ThreadLocalRandom.current().nextInt(1000,1000000);

        HttpResponse<String> response = this.getHighStakes4Betoffer(betofferid);

        System.out.println("Status Code: " + response.statusCode());
        System.out.println("Response Body: " + response.body());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isInstanceOf(String.class);
    }

    @RepeatedTest(10)
    void shouldReturnSortedHighStakes() throws Exception {
        final int NUM_CUSTOMER = 30;
        final int NUM_STAKES = 30;
        final int MAX_STAKE = 10000;
        int betofferid = ThreadLocalRandom.current().nextInt(1000,1000000);
        
        // Generate test data with 30 random stakes
        int[] stakes = new int[NUM_STAKES];
        for (int i = 0; i < NUM_STAKES; i++) {
            stakes[i] = ThreadLocalRandom.current().nextInt(100, MAX_STAKE);
        }
        // Set explicit max value
        stakes[ThreadLocalRandom.current().nextInt(NUM_STAKES)] = MAX_STAKE;

        // Generate test data with 30 random users
        int[] customers = new int[NUM_CUSTOMER];
        for (int i = 0; i < NUM_CUSTOMER; i++) {
            customers[i] = ThreadLocalRandom.current().nextInt(100, 500);
        }

        // Submit stakes from different users
        for (int customerId : customers) {
            
            for (int stake : stakes) {
                HttpResponse<String> sessionResponse = getOneSession4Customer(customerId);
                assertThat(sessionResponse.statusCode()).isEqualTo(200);
                
                String sessionKey = sessionResponse.body();
                
                HttpResponse<String> postResponse = postCustomerStake(betofferid, sessionKey, stake);
                assertThat(postResponse.statusCode()).isEqualTo(200);
            }
        }

        // Get high stakes results
        HttpResponse<String> response = getHighStakes4Betoffer(betofferid);
        assertThat(response.statusCode()).isEqualTo(200);
        
        // Parse and validate CSV response
        String csvBody = response.body();
        assertThat(csvBody).isNotEmpty();
        
        String[] entries = csvBody.split(",");
        assertThat(entries).hasSize(20); // Exact 20 entries
        
        int prevStake = Integer.MAX_VALUE;
        boolean maxFound = false;
        
        for (String entry : entries) {
            // Validate entry format
            assertThat(entry).matches("\\d+=\\d+");
            
            String[] parts = entry.split("=");
            int stake = Integer.parseInt(parts[1]);
            
            // Validate stake values
            assertThat(stake).isBetween(100, MAX_STAKE);
            
            if (stake == MAX_STAKE) {
                maxFound = true;
            }
            
            // Validate descending order
            assertThat(stake).isLessThanOrEqualTo(prevStake);
            prevStake = stake;
        }
        
        assertThat(maxFound).isTrue();
    }
}
