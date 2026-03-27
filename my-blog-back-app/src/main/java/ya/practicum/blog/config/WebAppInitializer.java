package ya.practicum.blog.config;

import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.ServletRegistration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

public class WebAppInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {

    /** One servlet context (no root): controllers must be visible to {@code DispatcherServlet}. */
    @Override
    protected Class<?>[] getRootConfigClasses() {
        return null;
    }

    @Override
    protected Class<?>[] getServletConfigClasses() {
        return new Class<?>[] {DatabaseConfig.class, AppConfig.class, WebMvcConfig.class};
    }

    @Override
    @NonNull
    protected String[] getServletMappings() {
        return new String[] {"/"};
    }

    @Override
    protected void customizeRegistration(@NonNull ServletRegistration.Dynamic registration) {
        registration.setMultipartConfig(new MultipartConfigElement(
                null,
                5L * 1024 * 1024,
                6L * 1024 * 1024,
                0
        ));
    }
}
