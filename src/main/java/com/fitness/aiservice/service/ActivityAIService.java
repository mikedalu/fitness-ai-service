package com.fitness.aiservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitness.aiservice.model.Recommendation;
import com.fitness.aiservice.model.Activity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ActivityAIService {
    private final GemniService gemniService;
    private Recommendation processApiResponse (Activity activity, String apiResponse){
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
//            log.info("PARSED RESPONSE form AI: {} ",jsonContent);
            JsonNode analysisJson = mapper.readTree(jsonContent);
            JsonNode analysisNode = analysisJson.path("analysis");
            StringBuilder fullAnalysis = new StringBuilder();
            addAnalysisSection(fullAnalysis,analysisNode, "overall", "Overall:");
            addAnalysisSection(fullAnalysis,analysisNode, "pace", "Pace:");
            addAnalysisSection(fullAnalysis,analysisNode, "heartRate", "Heart Rate:");
            addAnalysisSection(fullAnalysis,analysisNode, "caloriesBurned", "Calories Burned:");

            //Improvements
            List<String> improvements = extractImprovements(analysisJson.path("improvements"));

            //Suggestions
            List<String> suggestion = extractSuggestions(analysisJson.path("suggestions"));
            
            //Safety
            List<String> safety = extractSafety(analysisJson.path("Safety"));

            return Recommendation.builder().
            activityId(activity.getId()).
                    userId(activity.getUserId()).
                    activityType(activity.getType()).
                    recommendation(fullAnalysis.toString().trim()).
                    improvements(improvements).
                    suggestions(suggestion).
                    safety(safety).
                    createdAt(LocalDateTime.now()).build();
        } catch (Exception e){
            e.printStackTrace();

            return Recommendation.builder().
                    activityId(activity.getId()).
                    userId(activity.getUserId()).
                    activityType(activity.getType()).
                    recommendation("Unable to generate detailed analysis").
                    improvements(Collections.singletonList("Continue with your current workout")).
                    suggestions(Collections.singletonList("Consider consulting a trainer")).
                    safety(Arrays.asList("Always warm up before exercise",
                            "Stay Hydrated",
                            "Listen to your body")).
                    createdAt(LocalDateTime.now()).build();
        }

    }

    private List<String> extractSafety(JsonNode safetyNode) {
        List<String> safety = new ArrayList<>();
        if(safetyNode.isArray()){
            safetyNode.forEach(safetyItem->{
                safety.add(safetyItem.asText());
            });
            return safety.isEmpty() ?
                    Collections.singletonList("No specific safety provided"):
                    safety;
        }
        return safety;
    }

    private List<String> extractSuggestions(JsonNode suggestionNode) {
        List<String> suggestions = new ArrayList<>();
        if(suggestionNode.isArray()){
            suggestionNode.forEach(suggestion ->{
                String workout = suggestion.path("workout").asText();
                String description = suggestion.path("description").asText();
                suggestions.add(String.format("%s: %s", workout, description));

            });
            return suggestions.isEmpty() ?
                    Collections.singletonList("No specific suggestion provided"):
                    suggestions;
        }
        return suggestions;
    }

    private    List<String>  extractImprovements(JsonNode improvementNode ){
        List<String> improvements = new ArrayList<>();
        if(improvementNode.isArray()){
            improvementNode.forEach(improvement -> {
                String area = improvement.path("area").asText();
                String detail = improvement.path("recommendation").asText();
                improvements.add(String.format("%s: %s", area, detail));
            });
            return improvements.isEmpty() ?
                    Collections.singletonList("No specific Improvement provided") :
                    improvements;
        }
        return improvements;
    }
    private void addAnalysisSection(StringBuilder fullAnalysis, JsonNode analysisNode, String key, String prefix) {
        if(!analysisNode.path(key).isMissingNode()){
            fullAnalysis.append(prefix)
                    .append(analysisNode.path(key).asText())
                    .append("\n\n");

        }
    }

    public Recommendation  generateRecommendation (Activity activity){
        String prompt = createPromptForActivity(activity);
        String aiResponse = gemniService.getAnwser(prompt);
//        log.info("AI Response for ${} ", aiResponse);
        return processApiResponse(activity, aiResponse);

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
