package com.galactique.gestionecole.learning.api.dto;

import java.util.List;

import com.galactique.gestionecole.learning.domain.LearningContentPage;

public record LearningContentPageResponse(List<LearningContentResponse> contents,
	long totalElements, int page, int size) {
	public LearningContentPageResponse {
		contents = List.copyOf(contents);
	}

	public static LearningContentPageResponse from(LearningContentPage page) {
		return new LearningContentPageResponse(page.contents().stream()
			.map(LearningContentResponse::from).toList(), page.totalElements(), page.page(), page.size());
	}
}
