package maeilkanji.maeilkanji.presentation.controller.view

import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ModelAttribute
import java.time.Clock
import java.time.Year
import java.time.ZoneId

/**
 * 모든 뷰 페이지에 공통 모델 값을 넣는다. (footer 저작권 연도 등)
 */
@ControllerAdvice(basePackageClasses = [ViewModelAdvice::class])
class ViewModelAdvice(
    private val clock: Clock = Clock.system(ZoneId.of("Asia/Seoul")),
) {

    @ModelAttribute("currentYear")
    fun currentYear(): Int = Year.now(clock).value

}
