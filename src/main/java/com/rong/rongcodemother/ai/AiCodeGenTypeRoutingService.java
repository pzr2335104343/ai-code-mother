package com.rong.rongcodemother.ai;

import com.rong.rongcodemother.model.enums.CodeGenTypeEnum;
import dev.langchain4j.service.SystemMessage;

/**
 * AI代码生成类型智能路由服务
 * 使用结构化输出直接返回枚举类型
 *
 * @author rong
 */
public interface AiCodeGenTypeRoutingService {

    /**
     * 根据用户需求智能选择代码生成类型
     *
     * @param userPrompt 用户输入的需求描述
     * @return 推荐的代码生成类型
     */
    @SystemMessage(fromResource = "prompt/codegen-routing-system-prompt.txt")
    CodeGenTypeEnum routeCodeGenType(String userPrompt);

    /**
     * 根据网站提示词生成网站名称
     *
     * @param userPrompt 用户输入的需求描述
     * @return 网站名称
     */
    @SystemMessage(fromResource = "prompt/codegen-create-name-system-prompt.txt")
    String generateWebsiteName(String userPrompt);
}
