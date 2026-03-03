package com.dewple.app_api_auth.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

	public static final String BEARER_AUTH = "bearerAuth";

	@Bean
	public OpenAPI apiAuthOpenAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("App API Auth Service")
						.description("인증 및 API 서버 Swagger 문서\n\n" +
								"이 서버는 사용자 인증 및 Rest API를 제공합니다.")
						.version("v1.0"))
				.servers(List.of(
						new Server().url("http://localhost:8080").description("로컬 개발 서버"),
						new Server().url("http://43.203.217.86:8080").description("개발 서버"),
						new Server().url("https://api.dewple.com").description("운영 서버")
				))
				.components(new Components()
						.addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
								.type(SecurityScheme.Type.HTTP)
								.scheme("bearer")
								.bearerFormat("JWT")
								.description("JWT access token을 입력하세요.")
						)
				);
	}

	@Bean
	public GroupedOpenApi apiAuthGroup() {
		return GroupedOpenApi.builder()
				.group("app-api-auth")
				.pathsToMatch("/**")
				.build();
	}
}
