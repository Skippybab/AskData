package com.mt.agent.workflow.api.util;

import java.util.Base64;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CryptoKeyProvider {
	private static volatile byte[] masterKey;
	
	// 默认密钥（Base64编码）- 仅用于开发环境，生产环境应使用环境变量
	private static final String DEFAULT_KEY_BASE64 = "dGVzdC1kZWZhdWx0LWtleS1mb3ItZGV2ZWxvcG1lbnQ=";

	public static byte[] getMasterKey() {
		if (masterKey == null) {
			synchronized (CryptoKeyProvider.class) {
				if (masterKey == null) {
					String base64 = System.getProperty("DB_CIPHER_KEY_BASE64");
					if (base64 == null || base64.isEmpty()) base64 = System.getenv("DB_CIPHER_KEY_BASE64");
					if (base64 != null && !base64.isEmpty()) {
						masterKey = Base64.getDecoder().decode(base64);
						log.info("使用环境变量中的加密密钥");
					} else {
						// 使用固定的默认密钥，确保应用重启后能解密
						masterKey = Base64.getDecoder().decode(DEFAULT_KEY_BASE64);
						log.warn("未设置DB_CIPHER_KEY_BASE64环境变量，使用默认密钥（仅适用于开发环境）");
					}
					log.debug("加密密钥长度: {} bytes", masterKey.length);
				}
			}
		}
		return masterKey;
	}
}
