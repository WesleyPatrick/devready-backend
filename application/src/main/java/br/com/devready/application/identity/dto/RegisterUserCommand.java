package br.com.devready.application.identity.dto;

public record RegisterUserCommand(String email, String password, String displayName) {}
