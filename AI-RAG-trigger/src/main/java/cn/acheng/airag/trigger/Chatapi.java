package cn.acheng.airag.trigger;

import cn.acheng.airag.api.IAiService;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v1")
public class Chatapi implements IAiService {

    @Resource
    OllamaChatModel ollamaChatModel;

    @GetMapping("/str")
    @Override
    public ChatResponse aiapitest(@RequestParam("parm") String parm) {
        Prompt prompt = new Prompt(parm);
        return ollamaChatModel.call(prompt);
    }

    @GetMapping("/stream")
    @Override
    public Flux<ChatResponse> aiapistreamtest(@RequestParam("parm") String parm) {
        Prompt prompt = new Prompt(parm);
        return ollamaChatModel.stream(prompt);
    }
}
