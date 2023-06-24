package com.cydeo.lab08rest.client;

import com.cydeo.lab08rest.dto.CurrencyApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(url = "http://apilayer.net/api",name = "currencyApiClient")
public interface CurrencyApiClient {
//http://apilayer.net/api --> baseUrl
// /live --> endpoint
// ?
// access_key=483b1ef51d63928d3aeb7079d3bd5557
// &currencies=EUR,TRY
// &source=USD
// &format=1
    @GetMapping("/live")
    Map<String,Object> getCurrencyRates(@RequestParam("access_key") String accessKey,
                                        @RequestParam("currencies") String currencies,
                                        @RequestParam("source") String source,
                                        @RequestParam("format") int format);
}
