package com.trading.controller;

import com.trading.security.CurrentUserProvider;
import com.trading.service.backup.BackupService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@RestController
@RequestMapping("/api/system")
public class BackupController {

    private static final DateTimeFormatter FILE_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final BackupService backupService;
    private final CurrentUserProvider currentUserProvider;

    public BackupController(BackupService backupService, CurrentUserProvider currentUserProvider) {
        this.backupService = backupService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/sql-backup")
    public void downloadSqlBackup(HttpServletResponse response) throws IOException {
        UUID userId = currentUserProvider.getCurrentUserId();
        String timestamp = OffsetDateTime.now(ZoneOffset.UTC).format(FILE_TIMESTAMP_FORMATTER);
        String filename = "trading-sql-backup-" + timestamp + ".sql";

        response.setContentType("application/sql");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        try (BufferedWriter writer = new BufferedWriter(
            new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8)
        )) {
            backupService.writeUserBackupSql(userId, writer);
            writer.flush();
        }
    }
}
