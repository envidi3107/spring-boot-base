package com.wetech.hrms;

import com.jayway.jsonpath.JsonPath;
import com.wetech.hrms.common.exception.AppException;
import com.wetech.hrms.common.response.ApiResponse;
import com.wetech.hrms.common.web.RequestIdFilter;
import com.wetech.hrms.config.security.JwtClaimNames;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kiểm tra các thành phần nền tảng: xác thực, định dạng lỗi, validation, request id.
 * Tài khoản admin/Admin@123 được tạo bởi DataInitializer (xem application-test.yaml).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, ApiIntegrationTest.SampleController.class})
class ApiIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtAuthenticationConverter jwtAuthenticationConverter;

	@Test
	void healthEndpointIsPublic() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"));
	}

	@Test
	void loginThenAccessProtectedEndpoint() throws Exception {
		String accessToken = JsonPath.read(login("admin", "Admin@123"), "$.data.accessToken");

		mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.username").value("admin"))
				.andExpect(jsonPath("$.data.roles").value(org.hamcrest.Matchers.hasItems("ADMIN", "USER")))
				.andExpect(jsonPath("$.data.password").doesNotExist());
	}

	@Test
	void loginWithWrongPasswordReturns401() throws Exception {
		postJson("/api/v1/auth/login", "{\"username\":\"admin\",\"password\":\"wrong\"}")
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
	}

	@Test
	void refreshRotatesTokenAndDetectsReuse() throws Exception {
		String firstRefresh = JsonPath.read(login("admin", "Admin@123"), "$.data.refreshToken");

		String body = refresh(firstRefresh)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.accessToken").isNotEmpty())
				.andReturn().getResponse().getContentAsString();
		String secondRefresh = JsonPath.read(body, "$.data.refreshToken");

		// Dùng lại token cũ → bị từ chối và toàn bộ phiên bị thu hồi, kể cả token mới
		refresh(firstRefresh)
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
		refresh(secondRefresh)
				.andExpect(status().isUnauthorized());
	}

	@Test
	void logoutRevokesRefreshToken() throws Exception {
		String refreshToken = JsonPath.read(login("admin", "Admin@123"), "$.data.refreshToken");

		postJson("/api/v1/auth/logout", "{\"refreshToken\":\"" + refreshToken + "\"}")
				.andExpect(status().isOk());
		refresh(refreshToken)
				.andExpect(status().isUnauthorized());
	}

	@Test
	void protectedEndpointWithoutTokenReturns401() throws Exception {
		mockMvc.perform(get("/api/v1/auth/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(header().exists(HttpHeaders.WWW_AUTHENTICATE))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void invalidTokenReturns401() throws Exception {
		mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer abc.def.ghi"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
	}

	@Test
	void invalidBodyReturnsFieldErrors() throws Exception {
		mockMvc.perform(post("/test/samples").with(user("alice"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\": \"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.errors[0].field").value("name"));
	}

	@Test
	void malformedJsonReturns400() throws Exception {
		mockMvc.perform(post("/test/samples").with(user("alice"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{not-json"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
	}

	@Test
	void appExceptionIsMappedToItsStatus() throws Exception {
		mockMvc.perform(get("/test/samples/42").with(user("alice")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
				.andExpect(jsonPath("$.message").value("Không tìm thấy sample với id: 42"));
	}

	@Test
	void methodSecurityDenialReturns403() throws Exception {
		mockMvc.perform(get("/test/admin").with(user("bob", "USER")))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("FORBIDDEN"));

		mockMvc.perform(get("/test/admin").with(user("alice", "ADMIN")))
				.andExpect(status().isOk());
	}

	@Test
	void unknownPathReturns404() throws Exception {
		mockMvc.perform(get("/api/v1/does-not-exist").with(user("alice")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
	}

	@Test
	void requestIdIsEchoedOrGenerated() throws Exception {
		mockMvc.perform(get("/actuator/health").header(RequestIdFilter.REQUEST_ID_HEADER, "abc-123"))
				.andExpect(header().string(RequestIdFilter.REQUEST_ID_HEADER, "abc-123"));

		mockMvc.perform(get("/actuator/health"))
				.andExpect(header().exists(RequestIdFilter.REQUEST_ID_HEADER));
	}

	private String login(String username, String password) throws Exception {
		return postJson("/api/v1/auth/login",
				"{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
	}

	private ResultActions refresh(String refreshToken) throws Exception {
		return postJson("/api/v1/auth/refresh", "{\"refreshToken\":\"" + refreshToken + "\"}");
	}

	private ResultActions postJson(String path, String json) throws Exception {
		return mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(json));
	}

	/**
	 * Giả lập request đã xác thực, đi qua đúng converter (claim roles → ROLE_xxx) mà ứng dụng dùng.
	 */
	private RequestPostProcessor user(String username, String... roles) {
		Jwt jwt = Jwt.withTokenValue("token")
				.header("alg", "HS512")
				.subject("00000000-0000-0000-0000-000000000001")
				.claim(JwtClaimNames.USERNAME, username)
				.claim(JwtClaimNames.ROLES, List.of(roles))
				.build();
		return authentication(jwtAuthenticationConverter.convert(jwt));
	}

	record SampleRequest(@NotBlank String name) {
	}

	@RestController
	@RequestMapping("/test")
	static class SampleController {

		@PostMapping("/samples")
		ApiResponse<SampleRequest> create(@Valid @RequestBody SampleRequest request) {
			return ApiResponse.ok(request);
		}

		@GetMapping("/samples/{id}")
		ApiResponse<Void> get(@PathVariable long id) {
			throw AppException.notFound("sample", id);
		}

		@PreAuthorize("hasRole('ADMIN')")
		@GetMapping("/admin")
		ApiResponse<Void> adminOnly() {
			return ApiResponse.ok();
		}

	}

}
