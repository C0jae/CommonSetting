package com.example.commonsetting.global.infra.mail.type;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConstructorBinding
@ConfigurationProperties(prefix = "mail")
public record MailProperties(String imageUrl, From from) {
    public record From(String address, String name) {
    }
}