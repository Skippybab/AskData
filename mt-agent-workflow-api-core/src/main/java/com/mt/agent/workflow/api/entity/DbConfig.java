package com.mt.agent.workflow.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mt.agent.workflow.api.util.PasswordCipherService;
import com.mt.agent.workflow.api.util.CryptoKeyProvider;
import lombok.extern.slf4j.Slf4j;
import java.util.Date;

@Data
@Slf4j
@JsonIgnoreProperties(ignoreUnknown = true)
@TableName("db_config")
public class DbConfig {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField(exist = false)
    private Long tenantId;

    private Long userId;

    private String name;

    private String dbType;

    private String host;

    private Integer port;

    private String databaseName;

    private String username;

    private String passwordCipher;

    private Integer status; // 状态（1启用 0禁用）

    @TableField(value = "created_at")
    private Date createdAt;

    @TableField(value = "updated_at")
    private Date updatedAt;

    @TableField(exist = false)
    private String rawPassword; // 仅用于创建/更新时临时承载，非持久化
    
    /**
     * 获取解密后的密码（明文）
     * @return 解密后的密码
     */
    @JsonIgnore
    public String getPasswordPlain() {
        if (passwordCipher == null || passwordCipher.isEmpty()) {
            return "";
        }
        try {
            byte[] masterKey = CryptoKeyProvider.getMasterKey();
            return PasswordCipherService.decryptToStringFromString(masterKey, passwordCipher);
        } catch (Exception e) {
            log.error("解密数据库密码失败: dbConfigId={}", id, e);
            throw new RuntimeException("密码解密失败", e);
        }
    }
}


