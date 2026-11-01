package maeilkanji.maeilkanji.presentation.config

import maeilkanji.maeilkanji.business.service.AdminService
import maeilkanji.maeilkanji.presentation.auth.AdminAuthInterceptor
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebMvcConfig(
    private val adminService: AdminService,
) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(AdminAuthInterceptor(adminService))
            .addPathPatterns(
                "/admin/**",
                "/api/v1/admin/**",
                "/api/v1/tests/**",
            )
            .excludePathPatterns(
                "/admin/login",
                "/api/v1/admin/send-login-code",
                "/api/v1/admin/verify-login",
                "/api/v1/admin/logout",
            )
    }
}
