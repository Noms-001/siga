package mg.bank.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.security.PermissionAuthorizationInterceptor;

@Configuration
@RequiredArgsConstructor
public class PermissionWebConfig implements WebMvcConfigurer {

    private final PermissionAuthorizationInterceptor permissionAuthorizationInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(permissionAuthorizationInterceptor)
                .addPathPatterns("/api/**");
    }
}
