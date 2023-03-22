package com.hychoi.searchplaces.service;

import org.json.simple.JSONObject;
import org.springframework.stereotype.Service;

@Service
public interface SearchPlacesService {
    JSONObject searchPlacesByKeyword(String keyword);

    JSONObject getMostTenKeywords();

}
