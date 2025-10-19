package com.xai.upi.bank.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.NoHandlerFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    // Handle generic exceptions (e.g., unexpected errors)
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGenericException(Exception ex, Model model, jakarta.servlet.http.HttpServletRequest request) {
        model.addAttribute("errorCode", "500");
        model.addAttribute("errorMessage", "An unexpected error occurred: " + ex.getMessage());
        model.addAttribute("errorDetails", "Please try again later or contact support.");
        model.addAttribute("requestUri", request.getRequestURI());
        return "error";
    }

    // Handle 404 - Resource Not Found
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(NoHandlerFoundException ex, Model model, jakarta.servlet.http.HttpServletRequest request) {
        model.addAttribute("errorCode", "404");
        model.addAttribute("errorMessage", "Page not found: " + ex.getRequestURL());
        model.addAttribute("errorDetails", "The requested resource could not be located.");
        model.addAttribute("requestUri", request.getRequestURI());
        return "error";
    }

    // Handle Access Denied (403)
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleAccessDenied(AccessDeniedException ex, Model model, jakarta.servlet.http.HttpServletRequest request) {
        model.addAttribute("errorCode", "403");
        model.addAttribute("errorMessage", "Access Denied");
        model.addAttribute("errorDetails", "You do not have permission to access this resource.");
        model.addAttribute("requestUri", request.getRequestURI());
        return "error";
    }

    // Handle Bad Request (400) - e.g., invalid input
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleBadRequest(IllegalArgumentException ex, Model model, jakarta.servlet.http.HttpServletRequest request) {
        model.addAttribute("errorCode", "400");
        model.addAttribute("errorMessage", "Bad Request: " + ex.getMessage());
        model.addAttribute("errorDetails", "Please check your input and try again.");
        model.addAttribute("requestUri", request.getRequestURI());
        return "error";
    }
}