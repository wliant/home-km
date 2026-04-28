package com.homekm.auth.dto;

import com.homekm.auth.InvitationService;

public record IssuedInvitationResponse(
        InvitationResponse invitation,
        String token
) {
    public static IssuedInvitationResponse from(InvitationService.IssuedInvitation issued) {
        return new IssuedInvitationResponse(InvitationResponse.from(issued.invitation()), issued.rawToken());
    }
}
