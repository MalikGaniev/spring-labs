package com.cydeo.lab08rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@Getter
@Setter
public class CurrencyApiResponse {

    private Boolean success;
    private String terms;
    private String privacy;
    private Long timestamp;
    private String source;
    private Map<String,Double> quotes;
}
