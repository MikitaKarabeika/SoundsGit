// S3_NOTIF Aktywne powiadamianie użytkownika o wybranym zdarzeniu dotyczącym jego konta


package com.ape.friction.api.mok.rest;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.ape.friction.api.filters.userPrincipal.UserPrincipal;
import com.ape.friction.api.interceptors.MethodCallLogged;
import com.ape.friction.api.mok.services.SseNotificationService;


@RequiredArgsConstructor
@RestController
@RequestMapping("/notifications")
@MethodCallLogged
public class NotificationController {

    private final SseNotificationService sseNotificationService;

    @PreAuthorize("hasAuthority('GET_MY_NOTIFICATIONS')")
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal UserPrincipal principal) {

        return sseNotificationService.subscribe(principal.getId());
    }
}
