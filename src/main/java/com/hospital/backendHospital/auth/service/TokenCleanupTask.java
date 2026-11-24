package com.hospital.backendHospital.auth.service;

import com.hospital.backendHospital.auth.repository.TokenRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** B7: los tokens revocados o vencidos ya no sirven para nada; se borran una vez al dia (03:00). */
@Component
@RequiredArgsConstructor
public class TokenCleanupTask {

    private static final Logger log = LoggerFactory.getLogger(TokenCleanupTask.class);

    private final TokenRepository tokenRepository;

    @Scheduled(cron = "${app.tokens.cleanup-cron:0 0 3 * * *}")
    @Transactional
    public void deleteUnusableTokens() {
        int deleted = tokenRepository.deleteUnusable(LocalDateTime.now());
        if (deleted > 0) {
            log.info("Tokens revocados o vencidos eliminados: {}", deleted);
        }
    }
}
