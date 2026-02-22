package com.trading.dto.transaction;

public record CleanHistoryBackup(
    byte[] fileContent,
    String fileName
) {
}
