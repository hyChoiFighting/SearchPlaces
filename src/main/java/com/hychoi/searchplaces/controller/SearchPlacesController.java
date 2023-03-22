package com.hychoi.searchplaces.controller;


import com.hychoi.searchplaces.service.SearchPlacesService;
import org.json.simple.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SearchPlacesController {

    private final SearchPlacesService searchPlacesService;

    public SearchPlacesController(SearchPlacesService searchPlacesService) {
        this.searchPlacesService = searchPlacesService;
    }

    /*
        키워드로 장소를 서치합니다.
        @Request String keyword
        @Response JSONObject placeList
     */
    @GetMapping("/v1/api/search-places/by-keyword")
    public ResponseEntity<JSONObject> searchPlacesByKeyword(@RequestParam String keyword) {

        return new ResponseEntity<>(searchPlacesService.searchPlacesByKeyword(keyword), HttpStatus.OK);
    }

    /*
        키워드 별 누적 검색 목록을 위에서 부터 10개 조회합니다.
        @Request
        @Response JSONObject keywordList
     */
    @GetMapping("/v1/api/search-places/keyword-list")
    public ResponseEntity<JSONObject> getSearchPlacesKeywordList() {

        return new ResponseEntity<>(searchPlacesService.getMostTenKeywords(), HttpStatus.OK);
    }

}
