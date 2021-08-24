package com.kakaobank.ifkakao.eventloopdemo;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.blockhound.BlockHound;

import java.time.Duration;

@Execution(ExecutionMode.CONCURRENT)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class EventLoopDemoTest {

    @BeforeAll
    public static void beforeAll() {
        BlockHound.install();
        System.setProperty("reactor.netty.ioWorkerCount", "4");
    }

    private final WebTestClient webClient = WebTestClient.bindToServer()
        .baseUrl("http://localhost:8080")
        .responseTimeout(Duration.ofMillis(30000))
        .build();

    @Test
    public void testSleep() {
        this.webClient.get().uri("/sleep")
            .exchange()
            .expectStatus().isOk();
    }

    @RepeatedTest(10)
    void testOk() {
        this.webClient.get().uri("/ok")
            .exchange()
            .expectStatus().isOk();
    }
}
