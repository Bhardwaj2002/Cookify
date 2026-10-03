package com.cookify.recommendation;

import com.cookify.common.response.PageResponse;
import com.cookify.security.jwt.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Recommendations")
@RestController
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @Operation(summary = "\"For You\": recipes matching your diet, cooks you follow and cuisines you like")
    @GetMapping("/api/users/me/recommendations")
    public PageResponse<RecommendationResponse> forYou(@AuthenticationPrincipal AuthUser user,
                                                       @RequestParam(defaultValue = "0") @Min(0) int page,
                                                       @RequestParam(defaultValue = "12") @Min(1) @Max(50) int size) {
        return recommendationService.forUser(user, page, size);
    }
}
