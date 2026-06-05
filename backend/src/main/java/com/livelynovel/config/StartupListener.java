package com.livelynovel.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * 启动成功监听器：应用就绪后打印项目地址、健康检查、API 文档地址。
 */
@Component
public class StartupListener implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(StartupListener.class);

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        String port = event.getApplicationContext().getEnvironment()
                .getProperty("server.port", "8080");
        String baseUrl = "http://localhost:" + port;

        log.info("""

                ======================================================
                Lively Novel 后端启动成功
                ======================================================
                  项目地址:     {}
                  健康检查:     {}/api/health
                  Knife4j文档:  {}/doc.html
                ======================================================
                """, baseUrl, baseUrl, baseUrl);
    }
}