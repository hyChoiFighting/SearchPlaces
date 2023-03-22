package com.hychoi.searchplaces.repository;

import com.hychoi.searchplaces.domain.SearchPlaces;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface SearchPlacesRepository {

    void addCountByKeyword(SearchPlaces searchPlaces);

    List<Map.Entry<String,Integer>> getSearchKeywordList();

}
