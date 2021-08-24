package com.kakaobank.ifkakao.eventloopdemo;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
public class EventLoopDemoController {
    @GetMapping(value = "/sleep", produces = MediaType.TEXT_PLAIN_VALUE)
    public Mono<String> sleep() {
        return Mono.fromSupplier(() -> blockingFunction(10_000L))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private String blockingFunction(long sleepMs) {
        try {
            Thread.sleep(sleepMs);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return "OK";
    }

    @GetMapping(value = "/ok", produces = MediaType.TEXT_PLAIN_VALUE)
    public String health() {
        return "OK";
    }
}
