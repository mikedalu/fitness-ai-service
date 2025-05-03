package com.fitness.aiservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class GemniService
{
    @Value("${gemni.api.url}")
    private String gemniApiUrl;

    @Value("${gemni.api.key}")
    private String gemniApiKey;

    private WebClient webClient;
    public GemniService(WebClient.Builder webClientBuilder){
        this.webClient = webClientBuilder.build();
    }

    public String getAnser(String question){

//  This sampl payload  is onstructed below
//        {
//            "contents": [{
//            "parts":[{"text": "Explain how AI works"}]
//        }]
//        }
        Map<String, Object> requestBody = Map.of(
                "contents", new Object[] {
                        Map.of("parts", new Object[]{
                                Map.of("text", question)
                        })
                }
        );

        String response = webClient.post()
                .uri(gemniApiUrl + gemniApiKey)
                .header("Content-Type","application/json")
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return response;
    }
}
