package com.hychoi.searchplaces;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.*;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiTest {
    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("APITest")
    void reqSearchPlacesByKeyword() {

        //given
        URI url = UriComponentsBuilder
                .fromUriString("https://dapi.kakao.com")
                .path("/v2/local/search/keyword.json")
                .queryParam("query", "카카오프렌즈")
                .encode()
                .build()
                .toUri();

        RequestEntity<Void> req = RequestEntity
                .get(url)
                .header("Authorization", "KakaoAK 0592e67da4772c0be8542b941edecbd8")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build();

        //when
        ResponseEntity<String> result = restTemplate.exchange(req, String.class);

        // then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        System.out.println(result.getBody());

        URI naverUrl = UriComponentsBuilder
                .fromUriString("https://openapi.naver.com")
                .path("/v1/search/local.json")
                .queryParam("query", "라인프렌즈")
                .encode()
                .build()
                .toUri();

        RequestEntity<Void> naverReq = RequestEntity
                .get(naverUrl)
                .header("X-Naver-Client-Id", "Oo_1cTz3PrLVi8TtZ0Vv")
                .header("X-Naver-Client-Secret", "m9fBNlN7dV")
                .build();

        ResponseEntity<String> naverResult = restTemplate.exchange(naverReq, String.class);

        assertThat(naverResult.getStatusCode()).isEqualTo(HttpStatus.OK);
        System.out.println(naverResult.getBody());


    }


    @Test
    void asyncProcessing() throws ExecutionException, InterruptedException, ParseException {

        List<String> targetList = new ArrayList<>();
        targetList.add("kakao");
        targetList.add("naver");

        ExecutorService executor = Executors.newFixedThreadPool(Math.min(targetList.size(), 100), runnable -> {  // 요청할 API 만큼 스레드 풀 확보
            Thread t = new Thread(runnable);
            t.setDaemon(true);
            return t;
        });

        System.out.println(String.format("요청시간 : : %20dms", System.currentTimeMillis()));

        //targetList 에 맵으로 회사 별 필요 값들 세팅해보자
        List<CompletableFuture<ResponseEntity<String>>> apiFutureList = targetList.stream()
                .map(company -> CompletableFuture.supplyAsync(() -> {

                    System.out.println(String.format(company + "요청시간 : : %20dms",System.currentTimeMillis()));
                    URI url = UriComponentsBuilder
                            .fromUriString("https://dapi.kakao.com")
                            .path("/v2/local/search/keyword.json")
                            .queryParam("query", "카카오프렌즈")
                            .encode()
                            .build()
                            .toUri();

                    RequestEntity<Void> req = RequestEntity
                            .get(url)
                            .header("Authorization", "KakaoAK 0592e67da4772c0be8542b941edecbd8")
                            .header("Content-Type", "application/x-www-form-urlencoded")
                            .build();
                    ResponseEntity<String> result = restTemplate.exchange(req, String.class); // resttemplate 의 스레드 세이프 여부 스레드들이 같은 자원을 공유할텐데 http 요청이 동시에 발생할때 문제될지 ..?
                    // 요청마다 new Connection 을 활용한다고 함. 적합성에 대한 고민


                    try {
                        Thread.sleep("kakao".equals(company) ? 3000 : 6000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    System.out.println(company + " = " + Thread.currentThread().getName() + String.format("%20dms",System.currentTimeMillis()));

                    return result;

        }, executor)).toList();

        CompletableFuture<List<ResponseEntity<String>>> resultList = CompletableFuture.allOf(apiFutureList.toArray(new CompletableFuture[apiFutureList.size()]))
        .thenApply(v -> apiFutureList.stream().
                map(CompletableFuture::join).
                toList());
   //     List<String> resultList = apiFutureList.stream().map(CompletableFuture::join).collect(Collectors.toList());


        //값 받아서 이제 데이터 정제

        for (ResponseEntity<String> s : resultList.get()) {

            System.out.println(s.getStatusCode());
            System.out.println(s.getHeaders());
            System.out.println(s.getBody());

            JSONParser jsonParser = new JSONParser();
            JSONObject jsonObject = (JSONObject) jsonParser.parse(s.getBody());

            JSONArray jsonArray = (JSONArray) jsonObject.get("documents");


            jsonObject = (JSONObject) jsonArray.get(0);
            System.out.println(jsonObject.get("place_name"));

        }

        System.out.println("main Thread : " + Thread.currentThread().getName() + String.format("  종료 : : %20dms",System.currentTimeMillis()));
    }


}