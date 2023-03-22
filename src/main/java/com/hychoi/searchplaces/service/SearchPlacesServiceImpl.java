package com.hychoi.searchplaces.service;

import com.hychoi.searchplaces.domain.SearchPlaces;
import com.hychoi.searchplaces.repository.SearchPlacesRepository;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SearchPlacesServiceImpl implements SearchPlacesService {

    final ExternalSearchApiService kakao;
    final ExternalSearchApiService naver;

    final SearchPlacesRepository searchPlacesRepository;

    public SearchPlacesServiceImpl(ExternalSearchApiService kakao, ExternalSearchApiService naver, SearchPlacesRepository searchPlacesRepository) {
        this.kakao = kakao;
        this.naver = naver;
        this.searchPlacesRepository = searchPlacesRepository;
    }

    @Override
    public JSONObject searchPlacesByKeyword(String keyword) {

        Tuple2<String, String> tuple2 = Mono.zip(kakao.callByKeyword(keyword), naver.callByKeyword(keyword)).block();
        // kakao, naver api를 비동기로 요청하고 두 작업이 모두 완료되면 blocking 합니다.


        JSONParser jsonParser = new JSONParser();
        JSONObject kakaoObject = null;
        JSONObject naverObject = null;

        try {
            kakaoObject = (JSONObject) jsonParser.parse((String) tuple2.get(0));
            naverObject = (JSONObject) jsonParser.parse((String) tuple2.get(1));
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        JSONArray jsonArray = (JSONArray) kakaoObject.get("documents");
        List<String> kakaoPlaceList = new ArrayList<>();
        for (int i=0; i < jsonArray.size(); i++) {
            JSONObject obj = (JSONObject) jsonArray.get(i);
            kakaoPlaceList.add(obj.get("place_name").toString());
        }
        List<String> naverPlaceList = new ArrayList<>();
        jsonArray = (JSONArray) naverObject.get("items");
        for (int i=0; i < jsonArray.size(); i++) {
            JSONObject obj = (JSONObject) jsonArray.get(i);
            String title = obj.get("title").toString();
            naverPlaceList.add(title.replaceAll("<(/)?([a-zA-Z]*)(\\s[a-zA-Z]*=[^>]*)?(\\s)*(/)?>", ""));
            // HTML 태그를 제거합니다.
        }

        List<String> subL1 = new ArrayList<>();
        List<String> subL2 = new ArrayList<>();



        /*
            카카오 결과가 5개 이상이고 네이버 결과가 5개 이하면 카카오 결과에서 네이버 결과에서 부족한 갯수만큼 더 남깁니다.
            카카오 결과가 5개 이하면 카카오 결과만큼 자릅니다.
            네이버 결과도 똑같이 처리하여 한쪽에서 5개가 안되면 반대 쪽에서 채웁니다.
         */
        if (kakaoPlaceList.size() > 5) {
            if (naverPlaceList.size() < 5) {
                subL1 = kakaoPlaceList.subList(0,5 + (5 - naverPlaceList.size()));
            } else {
                subL1 = kakaoPlaceList.subList(0,5);
            }
        } else {
            subL1 = kakaoPlaceList.subList(0,kakaoPlaceList.size());
        }

         if (naverPlaceList.size() > 5) {
            if (kakaoPlaceList.size() < 5) {
                subL2 = naverPlaceList.subList(0,5 + (5 - kakaoPlaceList.size()));
            } else {
                subL2 = naverPlaceList.subList(0,5);
            }
        } else {
            subL2 = naverPlaceList.subList(0,naverPlaceList.size());
        }

        List<String> subL3 = new ArrayList<>();  // 공백 제거 후 비교를 위해 네이버 결과에서 공백제거 할 리스트
        for (String s : subL2) {
            subL3.add(s.replaceAll(" ",""));
        }


        /*
            카카오 목록 중 네이버 목록에 포함되는 값이 있으면 최종 리스트에 담고 각 결과에선 제거합니다.
        */

        List<String> result = new ArrayList<>();
        List<String> removeList = new ArrayList<>();
        for (int i=0; i < subL1.size(); i++) {   //  카카오 리스트로 반복
            if (subL3.indexOf(subL1.get(i).replaceAll(" ","")) > -1 ) {
                // 공백 제거한 카카오 요소가 공백 제거된 네이버 요소에 포함되면
                result.add(subL1.get(i));  // 최종 리스트에 추가
                if (subL2.indexOf(subL1.get(i)) > -1) {  // 서로 공백 제거 안된 값으로 포함됬으면
                    subL2.remove(subL2.indexOf(subL1.get(i)));  // 네이버 리스트에서 먼저 제거
                } else {  // 간혈적으로 카카오 결과는 공백이있고 네이버 결과는 공백이 없는 경우에 포함 안됨
                    subL2.remove(subL2.indexOf(subL1.get(i).replaceAll(" ","")));
                    // 카카오 결과 공백 제거 후 요소 제거
                }
                removeList.add(subL1.get(i));  // 카카오 리스트에서 삭제할 인덱스 저장
            }
        }

        for (int j=0; j < removeList.size(); j++) {
            subL1.remove(subL1.indexOf(removeList.get(j)));  // 카카오 리스트에서 이미 필터링된 요소 제거
        }

        result.addAll(subL1);
        result.addAll(subL2);
        // 나머지 리스트들 카카오, 네이버 순으로 추가

        JSONArray resultJsonArray = new JSONArray();
        for (String placeName : result) {

            JSONObject placeObject = new JSONObject();
            placeObject.put("name" , placeName);
            resultJsonArray.add(placeObject);

        }
        JSONObject resultJsonObject = new JSONObject();
        resultJsonObject.put("placeList", resultJsonArray);

        SearchPlaces searchPlaces = new SearchPlaces(keyword);
        searchPlacesRepository.addCountByKeyword(searchPlaces);

        return resultJsonObject;
    }

    @Override
    public JSONObject getMostTenKeywords() {

        List<Map.Entry<String,Integer>> entryList = searchPlacesRepository.getSearchKeywordList();
        JSONArray jsonArray = new JSONArray();
        int limitCount = entryList.size() > 10 ? 10 : entryList.size();  // 10개 이하 일땐 현재 리스트 사이즈 저장

        for (int i = 0; i < limitCount; i++) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("place",entryList.get(i).getKey());
            jsonObject.put("searchCount",entryList.get(i).getValue());
            jsonArray.add(jsonObject);
        }

        JSONObject resultJSONObject = new JSONObject();
        resultJSONObject.put("keywordList", jsonArray);

        return resultJSONObject;
    }
}
