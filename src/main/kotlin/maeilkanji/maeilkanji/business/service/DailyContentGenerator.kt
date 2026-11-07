package maeilkanji.maeilkanji.business.service

import maeilkanji.maeilkanji.business.domain.KanjiLevel
import maeilkanji.maeilkanji.business.service.dto.DailyContentDto

/**
 * 학습 문제(DailyContent) 한 개를 새로 만든다. 구현체는 infra 계층(LLM 호출)에 있다.
 */
interface DailyContentGenerator {

    /** 설정(API 키 등)이 갖춰져 있어 생성을 할 수 있는지 */
    fun isEnabled(): Boolean

    /**
     * @param usedPairs 이미 쓴 한자 쌍. 예: "上/下". 새 문제는 이 쌍과 겹치면 안 된다.
     * @param previousError 직전 시도가 검증에 실패한 이유. 재시도할 때 모델에게 알려 준다.
     */
    fun generate(level: KanjiLevel, usedPairs: List<String>, previousError: String?): DailyContentDto
}
