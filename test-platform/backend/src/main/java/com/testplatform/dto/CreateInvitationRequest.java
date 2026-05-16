package com.testplatform.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateInvitationRequest {
    private String invitedPhone;
    private Integer validDays = 7;
}