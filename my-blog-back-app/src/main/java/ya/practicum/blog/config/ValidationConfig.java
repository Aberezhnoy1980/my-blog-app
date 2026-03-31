package ya.practicum.blog.config;

import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@Configuration
public class ValidationConfig {

    @Bean
    public LocalValidatorFactoryBean validatorFactoryBean() {
        LocalValidatorFactoryBean factory = new LocalValidatorFactoryBean();
        // Avoid jakarta.el on classpath (HV default interpolator needs EL for {} in messages).
        factory.setMessageInterpolator(new ParameterMessageInterpolator());
        factory.afterPropertiesSet();
        return factory;
    }
}
