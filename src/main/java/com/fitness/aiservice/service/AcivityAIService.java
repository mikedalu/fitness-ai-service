package com.fitness.aiservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitness.aiservice.dto.Recommendation;
import com.fitness.aiservice.model.Activity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AcivityAIService {
    private final GemniService gemniService;
    private void processApiResponse (Activity activity, String apiResponse){
        try{
             ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(apiResponse);
            JsonNode textNode = rootNode.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text");

            String jsonContent = textNode.asText()
                    .replaceAll("```json\\n", "")
                    .replaceAll("\\n```", "").trim();
            log.info("PARSED RESPONSE form AI: {} ",jsonContent);
        } catch (Exception e){
            e.printStackTrace();
        }
    }
    public String  genereateRecommendation (Activity activity){
        String prompt = createPromptForActivity(activity);
        String aiResponse = gemniService.getAnwser(prompt);
        log.info("AI Response for ${} ", aiResponse);
        processApiResponse(activity, aiResponse);
        return aiResponse;
    }



    private String createPromptForActivity(Activity activity) {
        String prompt = String.format("""
                Analyze this fitness activity and provided detailed recommendations in the following JSON format
                {   "analysis": {
                        "overall": "Overall analysis here",
                        "pace": "Pace analysis here",
                        "heartRate": "Heart rate analysis here",
                        "caloriesBurned" "Calories analysis here"
                
                    },
                    "improvements":[
                        "area": "Area name",
                        "recommendation": "Detailed recommendation"
                    ],
                    "suggestions": [
                        {"workeout": "Workout name",
                        "description": "Detailed workout description"
                    ],
                    "Safety": [
                    "Safety point 1",
                    "Safety point 2"
                    ]
                }
                
                Analyze this activity
                Activity Type: %s
                Duration: %d minutes
                Calories Burned: %d
                Additional Metrics: %s
                
                Provide detailed analysis focusing on performance, improvements, next workout suggestions and safety guidlines
                ensure the response follows the EXACT JSON format show above.
                """,
                activity.getType(),
                activity.getDuration(),
                activity.getCaloriesBurned(),
                activity.getAdditionalMetrics()
             );

        return prompt;
    }
}
