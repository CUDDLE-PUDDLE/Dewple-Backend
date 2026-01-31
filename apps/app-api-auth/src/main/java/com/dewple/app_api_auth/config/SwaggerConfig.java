package com.dewple.app_api_auth.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

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
						new Server().url("https://api-dev.dewple.com").description("개발 서버"),
						new Server().url("https://api.dewple.com").description("운영 서버")
				));
	}

	@Bean
	public GroupedOpenApi apiAuthGroup() {
		return GroupedOpenApi.builder()
				.group("app-api-auth")
				.pathsToMatch("/**")
				.build();
	}
}
