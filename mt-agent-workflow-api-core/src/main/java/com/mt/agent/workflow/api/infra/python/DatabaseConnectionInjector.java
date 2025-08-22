package com.mt.agent.workflow.api.infra.python;

import com.mt.agent.workflow.api.entity.DbConfig;
import com.mt.agent.workflow.api.util.PasswordCipherService;
import com.mt.agent.workflow.api.util.CryptoKeyProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Injects database connection configuration into the Python execution environment.
 */
@Slf4j
@Component
public class DatabaseConnectionInjector {

    /**
     * Creates a temporary properties file containing database connection details.
     *
     * @param dbConfig The database configuration entity.
     * @param tempDir  The temporary directory to create the file in.
     * @return The path to the created configuration file.
     * @throws IOException If an I/O error occurs during file creation.
     */
    public Path createDbConfigProperties(DbConfig dbConfig, Path tempDir) throws IOException {
        Path configPath = tempDir.resolve("db_config.properties");

        // 使用正确的密码解密方法
        String decryptedPassword;
        try {
            byte[] masterKey = CryptoKeyProvider.getMasterKey();
            decryptedPassword = PasswordCipherService.decryptToStringFromString(masterKey, dbConfig.getPasswordCipher());
        } catch (Exception e) {
            log.error("Failed to decrypt database password for config id: {}", dbConfig.getId(), e);
            throw new IOException("Password decryption failed", e);
        }

        // 生成符合Python configparser格式的properties文件
        StringBuilder content = new StringBuilder();
        content.append("[DEFAULT]\n");
        content.append("DB_TYPE=").append(dbConfig.getDbType()).append("\n");
        content.append("DB_HOST=").append(dbConfig.getHost()).append("\n");
        content.append("DB_PORT=").append(dbConfig.getPort()).append("\n");
        content.append("DB_NAME=").append(dbConfig.getDatabaseName()).append("\n");
        content.append("DB_USER=").append(dbConfig.getUsername()).append("\n");
        content.append("DB_PASSWORD=").append(decryptedPassword).append("\n");

        String configContent = content.toString();
        Files.writeString(configPath, configContent);

        log.info("Created database config file at: {}", configPath);
        log.info("Database config content (password hidden): {}", 
                configContent.replace(decryptedPassword, "***"));
        
        return configPath;
    }
}
