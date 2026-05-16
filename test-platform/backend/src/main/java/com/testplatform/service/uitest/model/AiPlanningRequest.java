package com.testplatform.service.uitest.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class AiPlanningRequest {

    private String scene;
    private String instruction;
    private String screenshotBase64;
    private String imagePath;
    private List<PageElementCandidate> candidateElements = new ArrayList<>();
}
