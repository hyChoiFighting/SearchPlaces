package com.hychoi.searchplaces.domain;

import lombok.Data;

@Data
public class SearchPlaces {

    private String keywords;

    public SearchPlaces(String keywords) {
        this.keywords = keywords;
    }
}
