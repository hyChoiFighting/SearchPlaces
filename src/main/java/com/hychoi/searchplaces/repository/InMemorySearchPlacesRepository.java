package com.hychoi.searchplaces.repository;

import com.hychoi.searchplaces.domain.SearchPlaces;
import org.springframework.stereotype.Repository;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
public class InMemorySearchPlacesRepository implements SearchPlacesRepository {

    private static ConcurrentMap<String, Integer> countMap = new ConcurrentHashMap<>();

    @Override
    public void addCountByKeyword(SearchPlaces searchPlaces) {
        String keyword = searchPlaces.getKeywords();
        int count = countMap.get(keyword) == null ? 0 : countMap.get(keyword); // 기존에 없는 키워드 일시 0으로 시작
        countMap.put(keyword, count + 1);
    }

    @Override
    public List<Map.Entry<String, Integer>> getSearchKeywordList() {
        List<Map.Entry<String,Integer>> entryList = new LinkedList<>(countMap.entrySet());
        entryList.sort(((o1, o2) -> countMap.get(o2.getKey()) - countMap.get(o1.getKey()))); // 내림 차순으로 정렬
        return entryList;
    }
}
