package com.trading.service.backup;

import java.io.IOException;
import java.io.Writer;
import java.util.UUID;

public interface BackupService {

    void writeUserBackupSql(UUID userId, Writer writer) throws IOException;
}
