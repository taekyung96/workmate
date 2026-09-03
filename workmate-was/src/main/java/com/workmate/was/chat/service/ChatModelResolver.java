package com.workmate.was.chat.service;

import com.workmate.was.common.service.CommonCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 요청 모델 코드를 검증하고 그 모델이 속한 제공자까지 함께 해석한다 (F5-05, F9-04).
 *
 * <p><b>왜 따로 뺐나.</b> 채팅과 도우미가 같은 판단을 해야 하기 때문이다. 도우미가 제공자를
 * {@code null} 로 고정하고 있었는데, 기본 모델이 Gemini 일 때는 우연히 맞았을 뿐이다 —
 * 기본 모델이 Groq 계열로 바뀌는 순간 Groq 모델명을 Gemini 에 보내게 되어 깨진다.
 * 모델과 제공자는 항상 같이 결정돼야 해서 한 곳에 모았다.</p>
 */
@Component
@RequiredArgsConstructor
public class ChatModelResolver {

    /** 모델 화이트리스트를 담은 공통코드 그룹 */
    private static final String MODEL_GROUP = "AI_MODEL";

    private final CommonCodeService commonCodeService;

    /**
     * 사용자가 모델을 고르지 않았을 때 쓰는 기본 모델.
     *
     * <p>제공자별 설정 경로가 아니라 <b>제공자 중립</b> 환경변수를 읽는다 — 이 값이 어느 제공자의
     * 모델이든 {@code attr1} 조회로 제공자가 따라오기 때문이다. 멀티모달(OCR·음성)이 쓰는
     * Gemini 클라이언트의 기본 모델은 이 값과 분리돼 있다({@code LLM_MULTIMODAL_MODEL}) —
     * 한 변수로 묶어 두면 채팅 기본 모델을 Groq 으로 바꾸는 순간 영수증·회의록이 함께 깨진다.</p>
     */
    @Value("${LLM_CHAT_MODEL:qwen/qwen3.8-27b}")
    private String defaultModel;

    /**
     * 이 요청에 쓸 모델과 제공자를 정한다.
     *
     * @param requestedModel 클라이언트가 고른 모델 코드. 비어 있으면 기본 모델을 쓴다
     * @return 모델명과 제공자
     * @throws IllegalArgumentException 허용 목록(AI_MODEL 공통코드) 밖의 모델 코드
     */
    public ModelChoice resolve(String requestedModel) {
        String model = requestedModel;
        if (model == null || model.isBlank()) {
            model = defaultModel;
        } else if (!commonCodeService.isValidCode(MODEL_GROUP, model)) {
            throw new IllegalArgumentException("허용되지 않은 모델입니다.");
        }
        // 제공자는 공통코드(attr1)가 단일 출처다. 값이 없으면 registry 가 기본 제공자로 떨어뜨린다 —
        // attr1 을 안 채운 모델 하나 때문에 채팅 전체가 죽는 것보다 낫다
        String provider = commonCodeService.findAttr1(MODEL_GROUP, model).orElse(null);
        return new ModelChoice(model, provider);
    }

    /**
     * 이 요청에 쓸 모델과 그 모델이 속한 제공자.
     *
     * @param model    모델명 (AI_MODEL 화이트리스트 통과값)
     * @param provider LLM 제공자 (common_code.attr1). 모르면 null
     */
    public record ModelChoice(String model, String provider) {
    }
}
