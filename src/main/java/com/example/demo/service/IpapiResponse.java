package com.example.demo.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IpapiResponse {
    public Double latitude;
    public Double longitude;
    public String city;
    public String region;
    public String country_name;
}
