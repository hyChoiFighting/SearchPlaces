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
public class Kakao implements ExternalSearchApiService{

    @Value("${http-external.kakao.url}")
    String uri;

    @Value("${http-external.kakao.authorization}")
    String authorization;

    @Value("${http-external.kakao.contentType}")
    String contentType;

    @Override
    public Mono<String> callByKeyword(String keyword) {
        URI url = UriComponentsBuilder
                .fromUriString(uri)
                .path("/v2/local/search/keyword.json")
                .queryParam("query", keyword)
                .queryParam("size",10)
                .encode()
                .build()
                .toUri();

        return WebClient.create().get()
                .uri(url)
                .header("Authorization", authorization)
                .header("Content-Type", contentType)
                .retrieve()
                .onStatus(HttpStatusCode::is5xxServerError, clientResponse -> Mono.error(RuntimeException::new)) // HttpCode 가 500 일 때 RuntimeException 발생 시킵니다.
                .bodyToMono(String.class)
                .subscribeOn(Schedulers.boundedElastic())
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))   // 1번째 2초, 2번째 4초, 3번째 8초 재시도 합니다.
                    .filter(throwable -> throwable instanceof RuntimeException)  // 위에서 정의한 RuntimeException 발생 시 재시도 합니다.
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {  // 모든 재시도에서 실패하면 에러로 처리합니다.
                            throw new RuntimeException("외부 서버가 불안정하니 잠시 후 다시 시도해주세요.");
                }));
    }
}
