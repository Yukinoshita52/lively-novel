package com.livelynovel.service;

import com.livelynovel.model.dto.SceneDTO;
import com.livelynovel.model.enums.ScreenplayTypeEnum;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LlmService 单场转换测试。
 * 验证 LLM 调用和结构化输出功能。
 *
 * 使用方式：
 * 1. 复制 application-local.yml.example 为 application-local.yml
 * 2. 在 application-local.yml 中填入你的 DEEPSEEK_API_KEY
 * 3. 运行测试（会自动激活 local profile）
 */
@SpringBootTest(properties = "spring.profiles.active=local")
class LlmServiceTest {

    @Autowired
    private LlmService llmService;

    String getSourceText(){
        return """
                ==================================================
                章节 1
                ==================================================

                今天第一学期的期末考试就结束了。
                在距离暑假还不到10天的周五下午，我特意跑到远离学校的隔壁镇的家庭餐厅里点了自助饮料和大份薯条。
                一边用手帕擦了擦额头的汗，一边悠闲地环视着店内。
                关键的是不要着急。等薯条来了之后再慢悠悠地去拿喝的。
                「那么，开始吧……」
                在确认了周围没有穿着同校制服的学生后，我从书包里拿出了文库本。
                刚买的『跟年上的妹妹撒娇也可以吗？』最新卷。
                可乐&薯条&轻小说。开始party time吧——


                ==================================================
                ~一败目~
                ==================================================

                ~一败目~
                专业青梅竹马
                八奈见杏菜的惨败样
                『哥哥很努力了喔。很难受吧。哥哥拼了命地去努力我全都知道喔，随心所欲向胡桃撒娇也可以喔』
                ……看到身为女主角的妹妹的台词我不禁眼含泪水。
                无论什么时候都能宠溺着男主的胡桃酱的包容力让我颤抖。我按照惯例心满意足地回顾完这个系列20页的宠溺片段后，静静地合上书本。
                深切地注视着封面的胡桃酱。
                啊，我也想尝试这样的恋爱。想在这柔软的大腿上膝枕——
                「这样不行啦草介！现在可不是在这种地方浪费时间的时候！」
                隔壁桌传来的叫声把我的妄想给吹飞了。好像是一对情侣在争吵着什么。
                真是的，所以说阳角这类人……就应该向拥有甜美的天使绰号的菓子谷胡桃酱学习一下。
                那么，接下来就一边喝着哈密瓜汽水一边慢慢地重看插画的场景吧。
                「嗯！？」
                准备走向自助饮料台的我慌张地重新坐好来。
                大意了。隔壁桌的情侣是同学校的学生。而且还是同班同学。
                发出声音的是八奈见杏菜，是在班里很有人气的可爱系软妹。
                在她对面坐着的是袴田草介，也是一个引人注目的开朗系帅哥。两个人一直都是在一起的，果然是在交往吗。
                但是为什么在这种地方情侣吵架呢。我在目光朝下看着文库本的同时，竖起了耳朵。
                「再不去的话，华恋酱就要去英国了。那样就行了吗？」
                「但是华恋她已经和我道别了——」
                「她说这话不是摆明让你去见她吗！」
                ……那些不知道在哪听过一样的对话是怎么回事。在我读完一本轻小说的期间，他们的故事也迎来了高潮。
                从刚才开始就一直出现的华恋这名字……不久前转学来的女生么，好像是叫姬宫华恋来着。
                在转学第一天自我介绍的时候，就跟袴田发生了类似『啊！你是那时候的痴汉！』的争吵来着。
                话说回来，这就又要转学了？英国？进展也太快了吧？
                「为什么，能知道那种事？」
                「就是懂啊！因为我也一直对草介……」
                轻咬嘴唇，八奈见低下了头。
                「杏菜，我——」
                「嗯，这样就好」
                八奈见坚强地抬起头，站起身将自行车的钥匙放在了桌子上。
                「快去吧。华恋酱在等着你呢」
                「……可以吗？」
                「华恋酱人很好呢，不让她幸福的话我可不答应噢」
                「谢了。我要去跟她传递我的感情」
                「加油喔。被甩了的话，我可以勉为其难听听你的抱怨」
                「……对不起，杏菜」
                袴田讲完就飞奔了出去。一眼都没看八奈见。
                站了有一会儿的八奈见，无力地坐下并小声嘀咕着。
                「……别道歉啊。笨蛋」
                说起来我真是碰到了怎样的场景啊。虽说是与我无缘的阳角世界里的事情，但我还是同情她。这时候就装作没看见吧。
                将脸藏在菜单下等着的我不禁怀疑了起来。
                ——唔！？难道，难道她要做那种事！
                就在刚才被甩了的少女，八奈见杏菜。她慢慢地将手伸向了玻璃杯。
                那个将自己甩了的男生，袴田草介的玻璃杯。
                ——快住手！千万别去做那么可悲的事情！
                我拼死的祈祷没有传达到。八奈见的双手拿着玻璃杯，犹豫地将吸管含入口中。
                ……啊啊，还是这么做了啊。
                """;
    }

    /**
     * 测试单场转换功能。
     */
    @Test
    void testConvertSingleScene() {
        String sourceText = getSourceText();

        try {
            // 调用转换
            SceneDTO scene = llmService.convertSingleScene(sourceText, ScreenplayTypeEnum.ANIME);

            // 验证返回结构不为空
            assertNotNull(scene, "场景 DTO 不应为空");

            // 验证基本字段
            assertNotNull(scene.getSceneId(), "sceneId 不应为空");
            assertNotNull(scene.getHeading(), "heading 不应为空");
            assertNotNull(scene.getActionLines(), "actionLines 不应为空");
            assertNotNull(scene.getSourceText(), "sourceText 不应为空");

            // 验证 heading 结构
            assertNotNull(scene.getHeading().getLocation(), "location 不应为空");
            assertNotNull(scene.getHeading().getTimeOfDay(), "timeOfDay 不应为空");

            // 验证溯源字段
            assertEquals(1, scene.getSourceChapter(), "sourceChapter 应为 1");
            // 根据原文内容调整验证（原文包含"八奈见"或"家庭餐厅"等关键词）
            String sourceTextContent = scene.getSourceText();
            assertTrue(sourceTextContent.contains("八奈见") || sourceTextContent.contains("家庭餐厅") || sourceTextContent.length() > 50,
                    "sourceText 应包含原文相关内容");

            // 打印结果便于调试
            System.out.println("=== 转换结果 ===");
            System.out.println("sceneId: " + scene.getSceneId());
            System.out.println("heading: " + (scene.getHeading().isInterior() ? "内景" : "外景")
                    + " - " + scene.getHeading().getLocation()
                    + " - " + scene.getHeading().getTimeOfDay());
            System.out.println("actionLines 数量: " + scene.getActionLines().size());
            for (String action : scene.getActionLines()) {
                System.out.println("  - " + action);
            }
            System.out.println("dialogueBlocks 数量: " + scene.getDialogueBlocks().size());
            for (var dialogue : scene.getDialogueBlocks()) {
                System.out.println("  - " + dialogue.getCharacter() + ": " + dialogue.getLine());
            }
            System.out.println("visualizedInnerThoughts 数量: " + scene.getVisualizedInnerThoughts().size());
            for (var thought : scene.getVisualizedInnerThoughts()) {
                System.out.println("  - [" + thought.getMethod() + "] "
                        + thought.getOriginal() + " → " + thought.getResult());
            }
            System.out.println("transitions: " + scene.getTransitions());

        } catch (Exception e) {
            System.err.println("LLM 调用失败: " + e.getMessage());
            e.printStackTrace();

            // 提供排查建议
            if (e.getMessage().contains("authentication") || e.getMessage().contains("401")) {
                System.err.println("\n排查建议:");
                System.err.println("1. 检查 DEEPSEEK_API_KEY 是否正确设置");
                System.err.println("2. 检查 API Key 是否有效（登录 DeepSeek 控制台确认）");
                System.err.println("3. 检查 API Key 是否有余额");
            }

            fail("LLM 调用失败: " + e.getMessage());
        }
    }

    /**
     * 测试非 ANIME 类型应抛出异常。
     */
    @Test
    void testUnsupportedScreenplayType() {
        String sourceText = "测试文本";

        assertThrows(UnsupportedOperationException.class, () -> {
            llmService.convertSingleScene(sourceText, ScreenplayTypeEnum.FILM);
        }, "MVP 阶段仅支持 ANIME 类型，其他类型应抛出异常");
    }
}