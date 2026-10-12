package com.myTripProject.PlanYourTrip.exception;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
@Getter
@Setter
@NoArgsConstructor
public class ErrorResponse {
    private LocalDateTime timeStamp;
    private int status;
    private String message;
    private String path;
    private String error;
    public ErrorResponse(LocalDateTime timeStamp, int status, String error, String message, String path) {
        this.timeStamp = timeStamp;
        this.error = error;
        this.path = path;
        this.status = status;
        this.message = message;
    }
}
