package com.galactique.gestionecole.learning.domain;

import java.util.List;

public record LearningContentPage(List<LearningContent> contents, long totalElements, int page, int size) {
	public LearningContentPage {
		contents = List.copyOf(contents);
	}
}
