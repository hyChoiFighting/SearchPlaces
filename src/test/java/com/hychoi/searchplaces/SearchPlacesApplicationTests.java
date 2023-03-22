package com.hychoi.searchplaces;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.retry.Retry;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@SpringBootTest
class SearchPlacesApplicationTests {

    @Test
    void contextLoads() {

    }

    /*
     * WebClient의 비동기 요청 및 재시도 루프를 테스트합니다.
     * */
    @Test
    void webClientTest() throws InterruptedException {
        Mono<String> kakaoResult = null;
        Mono<String> naverResult = null;

        WebClient webClient = WebClient.create();

        URI naverUrl = UriComponentsBuilder
                .fromUriString("https://openapi.naver.com")
                .path("/v1/search/local.json")
                .queryParam("query", "맥도날드")
                .queryParam("start", 1)
                .queryParam("display", 5)
                .encode()
                .build()
                .toUri();

        naverResult = webClient.get().uri(naverUrl).header("X-Naver-Client-Id", "Oo_1cTz3PrLVi8TtZ0Vv")
                .header("X-Naver-Client-Secret", "m9fBNlN7dV").retrieve().bodyToMono(String.class).subscribeOn(Schedulers.boundedElastic()).doOnSubscribe(response -> {
//                            System.out.println(response);
//                            System.out.println(s + " = " + Thread.currentThread().getName() + String.format("%20dms",System.currentTimeMillis()));
//                            try {
//                                Thread.sleep(6000);
//                            } catch (InterruptedException e) {
//                                throw new RuntimeException(e);
//                            }
                });

        URI url = UriComponentsBuilder
                .fromUriString("https://dapi.kakao.com")
                .path("/v2/local/search/keyword.json")
                .queryParam("query", "맥도날드")
                .queryParam("size",10)
                .encode()
                .build()
                .toUri();

        webClient = WebClient.create();

        kakaoResult = webClient.get()
                .uri(url)
                .header("Authorization", "KakaoAK 0592e67da4772c0be8542b941edecbd8")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .retrieve()
                .onStatus(HttpStatusCode::is5xxServerError, clientResponse -> Mono.error(RuntimeException::new))
                //.onStatus(httpStatusCode -> httpStatusCode.isSameCodeAs(HttpStatusCode.valueOf(200)),clientResponse -> Mono.error(RuntimeException::new))
                .bodyToMono(String.class)
                .subscribeOn(Schedulers.boundedElastic())
//                        .doOnSubscribe(response -> {
//                            try {
//                                Thread.sleep(3000);
//                            } catch (InterruptedException e) {
//                                throw new RuntimeException(e);
//                            }
//                            System.out.println(response);
//                            System.out.println(s + " = " + Thread.currentThread().getName() + String.format("%20dms",System.currentTimeMillis()));
//                        })
//                        .doOnSuccess(stringResponseEntity -> {
//                            System.out.println("시발");
//                            System.out.println(stringResponseEntity);
//
//
//                        })
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .filter(throwable -> throwable instanceof RuntimeException)
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                            throw new RuntimeException("외부 서버가 불안정하니 잠시 후 다시 시도해주세요.");
                        }));

        Tuple2<String, String> tuple2 = Mono.zip(kakaoResult,naverResult).block();

        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject = null;
        JSONObject naverObj = null;

        try {
            jsonObject = (JSONObject) jsonParser.parse((String) tuple2.get(0));
            naverObj = (JSONObject) jsonParser.parse((String) tuple2.get(1));
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        JSONArray jsonArray = (JSONArray) jsonObject.get("documents");
        List<String> placeList = new ArrayList<>();
        for (int i=0; i < jsonArray.size(); i++) {
            JSONObject obj = (JSONObject) jsonArray.get(i);
            System.out.println(obj.get("place_name").toString().replaceAll(" ",""));
//            System.out.println(obj.get("x"));
//            System.out.println(obj.get("y"));
            placeList.add(obj.get("place_name").toString());

        }
        List<String> naverPlaceList = new ArrayList<>();
        jsonArray = (JSONArray) naverObj.get("items");
        for (int i=0; i < jsonArray.size(); i++) {
            JSONObject obj = (JSONObject) jsonArray.get(i);
            String title = obj.get("title").toString();

            System.out.println(title.replaceAll("<(/)?([a-zA-Z]*)(\\s[a-zA-Z]*=[^>]*)?(\\s)*(/)?>", "").replaceAll(" ",""));
//            System.out.println(obj.get("mapx"));
//            System.out.println(obj.get("mapy"));
            naverPlaceList.add(title.replaceAll("<(/)?([a-zA-Z]*)(\\s[a-zA-Z]*=[^>]*)?(\\s)*(/)?>", ""));
        }
    }


    /*
     * 카카오 검색 API, 네이버 검색 API - 를 통해 각각 최대 5개씩, 총 10개의 키워드 관련 장소를 검색합니다.
     *  (특정 서비스 검색 결과가 5개 이하면 최대한 총 10개에 맞게 적용)
     *  해당 요구사항을 테스트합니다.
     * */
    @Test
    void dataFilter() {
        List<String> l1 = new ArrayList<>(List.of(
//                "맥도날드강남2호점",
//                "맥도날드서울시청점",
//                "맥도날드청담DT점",
//                "맥도날드명동2호점",
//                "맥도날드대전유성DT점",
                "맥도날드송파잠실DT점",
                "맥도날드서울둔촌DT점",
                "맥도날드용인수지DT점"
        ));
        List<String> l2 = new ArrayList<>(List.of(
                "맥도날드서울시청점",
                //"맥도날드명동점",
                "맥도날드명동2호점",
                "맥도날드종로3가점",
//                "맥도날드서울역점",
//                "맥도날드송파잠실DT점",
//                "맥도날드서울둔촌DT점",
                "맥도날드용인수지DT점"
        ));

        List<String> subL1 = new ArrayList<>();
        List<String> subL2 = new ArrayList<>();
        if (l1.size() > 5) {
            if (l2.size() < 5) {
                subL1 = l1.subList(0,5 + (5 - l2.size()));
            } else {
                subL1 = l1.subList(0,5);
            }
        } else {
            subL1 = l1.subList(0,l1.size());
        }
        if (l2.size() > 5) {
            if (l1.size() < 5) {
                subL2 = l2.subList(0,5 + (5 - l1.size()));
            } else {
                subL2 = l2.subList(0,5);
            }

        } else {
            subL2 = l2.subList(0,l2.size());
        }

        System.out.println(subL1);
        System.out.println(subL2);
    }

    /*
     * 카카오 장소 검색 API의 결과를 기준으로 두 API 검색 결과에 동일하게 나타나는 문서(장소)가 상위에 올 수 있도록 정렬
     * 동일 업체 판단 기준은 자유롭게 결정해주세요.
     * (예, 업체명 공백 제거 비교, 태그 제거 비교, 문자열 유사도 비교, 장소 위치 비교 등)
     * 해당 요구사항을 테스트합니다. 동일 업체 판단 기준은 업체명 공백 제거 비교로 합니다.
     * */
    @Test
    void dataProcess() {
//        List<String> l1 = new ArrayList<>(List.of(
//                "맥도날드 강남2호점",
//                "맥도날드 서울시청점",
//                "맥도날드 청담DT점",
//                "맥도날드 명동2호점",
//                "맥도날드 대전유성DT점"));
//        List<String> l2 = new ArrayList<>(List.of(
//                "맥도날드 서울시청점",
//                "맥도날드 명동점",
//                "맥도날드 명동2호점",
//                "맥도날드 종로3가점",
//                "맥도날드 서울역점"));
//
//        List<String> l3 = new ArrayList<>(List.of(
//                "맥도날드서울시청점",
//                "맥도날드명동점",
//                "맥도날드명동2호점",
//                "맥도날드종로3가점",
//                "맥도날드서울역점"));
        List<String> l1 = new ArrayList<>(List.of(
                "고척스카이돔",
                "부산사직종합운동장 사직야구장",
                "인천SSG랜더스필드",
                "잠실종합운동장 잠실야구장",
                "창원NC파크"));
        List<String> l2 = new ArrayList<>(List.of(
                "장충단공원장충리틀야구장",
                "최경태 피칭스튜디오",
                "이태원스크린야구",
                "스크라이크팡",
                "잠실종합운동장잠실야구장"));

        List<String> l3 = new ArrayList<>(List.of(
                "장충단공원장충리틀야구장",
                "최경태피칭스튜디오",
                "이태원스크린야구",
                "스크라이크팡",
                "잠실종합운동장잠실야구장"));


        List<String> result = new ArrayList<>();
        List<String> removeList = new ArrayList<>();
        System.out.println("l1 : " + l1);
        System.out.println("l2 : " + l2);
        System.out.println("l3 : " + l3);
        for (int i=0; i < l1.size(); i++) {
            if (l3.indexOf(l1.get(i).replaceAll(" ","")) > -1 ) {
                System.out.println(l3.indexOf(l1.get(i).replaceAll(" ","")));
                System.out.println(l1.get(i).replaceAll(" ",""));
                result.add(l1.get(i));
                System.out.println(l2.indexOf(l1.get(i)));
                if (l2.indexOf(l1.get(i)) > -1) {
                    l2.remove(l2.indexOf(l1.get(i)));
                } else {
                    l2.remove(l2.indexOf(l1.get(i).replaceAll(" ","")));
                }

                removeList.add(l1.get(i));
            }
        }

        for (int j=0; j < removeList.size(); j++) {
            l1.remove(l1.indexOf(removeList.get(j)));
        }

        result.addAll(l1);
        result.addAll(l2);

        System.out.println("처리결과 : " + result);
        System.out.println("l1 : " + l1);
        System.out.println("l2 : " + l2);
        System.out.println("l3 : " + l3);


    }


    /*

        키워드 검색 순위와 키워드 별 누적 횟수 조회를 테스트합니다.
     */
    @Test
    void getMostTenKeywords() {
        ConcurrentMap<String, Integer> keywords = new ConcurrentHashMap<>();

        keywords.put("은행",1);
        keywords.put("맥도날드",2);
        keywords.put("이삭토스트",33);
        keywords.put("체육관",4);
        keywords.put("피자집",15);
        keywords.put("고기집",6);
        keywords.put("야구장",17);
        keywords.put("축구장",8);
        keywords.put("음식점",17);
        keywords.put("카페",21);
        keywords.put("문방구",11);
        keywords.put("노래방",9);
        keywords.put("편의점",13);
        keywords.put("동사무소",45);
        keywords.put("주유소",15);
        keywords.put("호텔",16);

        List<Map.Entry<String,Integer>> entryList = new LinkedList<>(keywords.entrySet());
        entryList.sort(((o1, o2) -> keywords.get(o2.getKey()) - keywords.get(o1.getKey())));

        JSONArray jsonArray = new JSONArray();
        for (int i = 0; i < 10; i++) {
//            System.out.println("rank : " + i+1 + " place : " + entryList.get(i).getKey() + ", searchCount : " + entryList.get(i).getValue());
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("place",entryList.get(i).getKey());
            jsonObject.put("searchCount",entryList.get(i).getValue());
            jsonArray.add(jsonObject);
        }

        System.out.println(jsonArray);
    }



}
