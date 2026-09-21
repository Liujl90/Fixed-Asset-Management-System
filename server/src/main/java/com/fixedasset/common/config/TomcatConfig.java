package com.fixedasset.common.config;

import org.apache.coyote.http11.Http11Nio2Protocol;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Tomcat 运行配置。
 *
 * <p>Windows/JDK 组合下 NIO 默认连接器可能受 loopback 限制，NIO2 使用 IOCP，
 * 更适合当前本地演示环境，同时不影响 Linux 和容器部署。</p>
 */
@Configuration
public class TomcatConfig {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatProtocolCustomizer() {
        return factory -> factory.setProtocol(Http11Nio2Protocol.class.getName());
    }
}
