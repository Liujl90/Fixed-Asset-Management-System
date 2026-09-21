package com.fixedasset.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 配置。
 *
 * <p>真实部署必须通过环境变量覆盖默认密钥，默认值仅用于本地演示。</p>
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, long expirationMinutes) {
}
