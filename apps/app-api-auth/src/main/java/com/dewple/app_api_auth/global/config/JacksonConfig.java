package com.dewple.app_api_auth.global.config;

import com.fasterxml.jackson.datatype.jsr310.ser.OffsetDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

@Configuration
public class JacksonConfig {

    private static final TimeZone KST = TimeZone.getTimeZone("Asia/Seoul");

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> builder
                .timeZone(KST)
                .serializerByType(OffsetDateTime.class,
                        new OffsetDateTimeSerializer(
                                OffsetDateTimeSerializer.INSTANCE,
                                false,
                                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"),
                                null
                        )
                );
    }
}
