package com.hychoi.searchplaces.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.net.URI;
import java.time.Duration;

@Service
public class Naver implements ExternalSearchApiService{

    @Value("${http-external.naver.url}")
    String uri;

    @Value("${http-external.naver.clientId}")
    String clientId;

    @Value("${http-external.naver.clientSecret}")
    String clientSecret;

    @Override
    public Mono<String> callByKeyword(String keyword)  {

        URI url = UriComponentsBuilder
                .fromUriString(uri)
                .path("/v1/search/local.json")
                .queryParam("query", keyword)
                .queryParam("start", 1)
                .queryParam("display", 5)
                .encode()
                .build()
                .toUri();

        return WebClient.create().get()
                .uri(url)
                .header("X-Naver-Client-Id", clientId)
                .header("X-Naver-Client-Secret", clientSecret)
                .retrieve()
                .onStatus(HttpStatusCode::is5xxServerError, clientResponse -> Mono.error(RuntimeException::new))
                .bodyToMono(String.class)
                .subscribeOn(Schedulers.boundedElastic())
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                    .filter(throwable -> throwable instanceof RuntimeException)
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                            throw new RuntimeException("외부 서버가 불안정하니 잠시 후 다시 시도해주세요.");
                }));
    }
}
