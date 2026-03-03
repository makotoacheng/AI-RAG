package cn.acheng.airag.api;

import org.springframework.ai.chat.model.ChatResponse;
import reactor.core.publisher.Flux;

public interface IAiService {
    ChatResponse aiapitest(String parm);

    Flux<ChatResponse> aiapistreamtest(String parm);
}
