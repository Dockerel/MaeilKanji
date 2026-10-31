package io.kotest.provided

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.extensions.Extension
import io.kotest.extensions.spring.SpringAutowireConstructorExtension

// Spring 슬라이스 테스트에서 MockMvc 등 빈을 Spec 생성자로 주입받기 위한 설정
object ProjectConfig : AbstractProjectConfig() {
    override fun extensions(): List<Extension> = listOf(SpringAutowireConstructorExtension)
}
