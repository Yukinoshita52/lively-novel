package com.livelynovel;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DeepSeek API 连接测试。
 * 验证 Spring AI + DeepSeek 配置是否正确。
 *
 * 使用方式：
 * 1. 复制 application-local.yml.example 为 application-local.yml
 * 2. 在 application-local.yml 中填入你的 DEEPSEEK_API_KEY
 * 3. 运行测试（会自动激活 local profile）
 */
@SpringBootTest(properties = "spring.profiles.active=local")
class DeepSeekConnectionTest {

    @Autowired
    private ChatModel chatModel;

    @Test
    void testDeepSeekConnection() {
        // 使用 ChatModel 直接调用
        Prompt prompt = new Prompt("请用一句话回复：你好");

        try {
            var response = chatModel.call(prompt);

            // 验证响应不为空
            assertNotNull(response, "DeepSeek API 响应不应为空");
            assertNotNull(response.getResult(), "响应结果不应为空");
            assertNotNull(response.getResult().getOutput(), "输出不应为空");

            String content = response.getResult().getOutput().getText();
            assertNotNull(content, "响应内容不应为空");
            assertFalse(content.isEmpty(), "响应内容不应为空字符串");

            System.out.println("DeepSeek 响应: " + content);

            // 打印 token 使用情况
            var metadata = response.getMetadata();
            if (metadata != null) {
                System.out.println("Token 使用: " + metadata);
            }
        } catch (Exception e) {
            System.err.println("DeepSeek API 调用失败: " + e.getMessage());
            e.printStackTrace();

            // 提供排查建议
            if (e.getMessage().contains("authentication") || e.getMessage().contains("401")) {
                System.err.println("\n排查建议:");
                System.err.println("1. 检查 DEEPSEEK_API_KEY 是否正确设置");
                System.err.println("2. 检查 API Key 是否有效（登录 DeepSeek 控制台确认）");
                System.err.println("3. 检查 API Key 是否有余额");
                System.err.println("4. 用 curl 测试: curl https://api.deepseek.com/chat/completions -H \"Authorization: Bearer $DEEPSEEK_API_KEY\" ...");
            }

            fail("DeepSeek API 调用失败: " + e.getMessage());
        }
    }
}
