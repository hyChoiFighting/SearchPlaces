package com.hychoi.searchplaces.externalapi;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class KakaoTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void reqSearchPlacesByKeyword() {

        URI url = UriComponentsBuilder
                .fromUriString("https://dapi.kakao.com")
                .path("/v2/local/search/keyword.json")
                .queryParam("query","카카오프렌즈")
                .encode()
                .build()
                .toUri();

        RequestEntity<Void> req = RequestEntity
                .get(url)
                .header("Authorization","KakaoAK 0592e67da4772c0be8542b941edecbd8")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build();

        ResponseEntity<String> result = restTemplate.exchange(req, String.class);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        System.out.println(result.getBody());

    }

}
