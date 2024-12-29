package com.example.commonsetting.global.infra.mail.dto;

import java.util.List;
import java.util.Map;

public record MailDto(List<String> toAddressList, String titleMessageCode, String service, Content content) {
    public record Content(String templateHtml, Map<String, Object> variables) {}

}