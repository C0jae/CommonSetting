package com.example.commonsetting.global.infra.mail.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MailDecoration{
    KEYPAIR_CREATE("mail/keyPairCreate.html", "email.title.sshkey.create"),
    PLATFORM_DELETE("mail/platFormClose.html","email.title.platform.close"),
    PLATFORM_CREATE("mail/platFormCreate.html","email.title.platform.create"),
    IPC_PLATFORM_CREATE("mail/ipcPlatFormCreate.html","email.title.ipcPlatform.create");

    private final String templateHtml; // Thymeleaf path
    private final String titleMessageCode; // define messages.properties

}
