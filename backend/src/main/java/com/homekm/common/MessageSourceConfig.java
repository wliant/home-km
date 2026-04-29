package com.homekm.common;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.List;
import java.util.Locale;

/**
 * Wires Spring's {@link MessageSource} against {@code messages_*.properties}
 * for backend error message localisation. Locale per request is derived from
 * the {@code Accept-Language} header — the frontend sends the user's chosen
 * locale (Settings → Language) so the same code that drives UI translations
 * also drives backend strings.
 *
 * Supported locales mirror the frontend catalogue (en/es/de). Unknown
 * locales fall back to English.
 */
@Configuration
public class MessageSourceConfig {

    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
        source.setBasename("classpath:messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        source.setUseCodeAsDefaultMessage(true);  // missing key → return code
        source.setCacheSeconds(60);
        return source;
    }

    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setSupportedLocales(List.of(Locale.ENGLISH, new Locale("es"), Locale.GERMAN));
        resolver.setDefaultLocale(Locale.ENGLISH);
        return resolver;
    }
}
