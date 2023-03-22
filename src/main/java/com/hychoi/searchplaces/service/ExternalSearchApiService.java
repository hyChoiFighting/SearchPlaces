package com.hychoi.searchplaces.service;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public interface ExternalSearchApiService {

    Mono<String> callByKeyword(String keyword);

}
