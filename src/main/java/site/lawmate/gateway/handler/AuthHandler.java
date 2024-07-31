package site.lawmate.gateway.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerResponse;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import site.lawmate.gateway.domain.dto.LoginDTO;
import site.lawmate.gateway.domain.model.PrincipalUserDetails;
import site.lawmate.gateway.domain.vo.ExceptionStatus;
import site.lawmate.gateway.exception.GatewayException;
import site.lawmate.gateway.provider.JwtTokenProvider;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthHandler {
    private final WebClient webClient;
    private final JwtTokenProvider jwtTokenProvider;

    public Mono<ServerResponse> localLogin(LoginDTO loginDTO) {
        return login(loginDTO, "lb://user-service/auth/login/local");
    }

    public Mono<ServerResponse> adminLogin(LoginDTO loginDTO) {
        return login(loginDTO, "lb://admin-service/auth/login");
    }

    public Mono<ServerResponse> lawyerLogin(LoginDTO loginDTO) {
        return login(loginDTO, "lb://lawyer-service/auth/login");
    }


    public Mono<ServerResponse> refresh(String refreshToken) {
        return Mono.just(refreshToken)
                .flatMap(bearerToken -> Mono.just(jwtTokenProvider.removeBearer(bearerToken)))
                .filter(jwtToken -> jwtTokenProvider.isTokenValid(jwtToken, true))
                .switchIfEmpty(Mono.error(new GatewayException(ExceptionStatus.UNAUTHORIZED, "Invalid Refresh Token")))
                .flatMap(jwtToken ->
                        Mono.just(jwtTokenProvider.extractPrincipalUserDetails(jwtToken))
                                .filterWhen(user -> jwtTokenProvider.isTokenInRedis(user.getUsername(), jwtToken))
                                .switchIfEmpty(Mono.error(new GatewayException(ExceptionStatus.UNAUTHORIZED, "Token not found in Redis")))
                                .flatMap(i -> jwtTokenProvider.generateToken(i, false))
                )
                .flatMap(accessToken ->
                        ServerResponse.ok()
                                .cookie(
                                        ResponseCookie.from("accessToken")
                                                .value(accessToken)
                                                .maxAge(jwtTokenProvider.getAccessTokenExpired())
                                                .path("/")
                                                .sameSite("None")
                                                .secure(true)
                                                .httpOnly(true)
                                                .build()
                                )
                                .build()
                )
                .onErrorResume(GatewayException.class, e -> ServerResponse.status(e.getStatus().getStatus().value()).bodyValue(e.getMessage()));
    }

    public Mono<ServerResponse> logout(String refreshToken) {
        return Mono.just(refreshToken)
                .flatMap(bearerToken -> Mono.just(jwtTokenProvider.removeBearer(bearerToken)))
                .filter(jwtToken -> jwtTokenProvider.isTokenValid(jwtToken, true))
                .switchIfEmpty(Mono.error(new GatewayException(ExceptionStatus.UNAUTHORIZED, "Invalid Refresh Token")))
                .flatMap(jwtToken ->
                        Mono.just(jwtTokenProvider.extractPrincipalUserDetails(jwtToken))
                                .filterWhen(user -> jwtTokenProvider.isTokenInRedis(user.getUsername(), jwtToken))
                                .filterWhen(user -> jwtTokenProvider.removeTokenInRedis(user.getUsername()))
                                .switchIfEmpty(Mono.error(new GatewayException(ExceptionStatus.UNAUTHORIZED, "Token not found in Redis")))
                )
                .flatMap(i -> ServerResponse.ok().build())
                .onErrorResume(GatewayException.class, e -> ServerResponse.status(e.getStatus().getStatus().value()).bodyValue(e.getMessage()));
    }
    private Mono<ServerResponse> login(LoginDTO loginDTO, String uri) {
        return webClient.post()
                .uri(uri)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(loginDTO)
                .retrieve()
                .bodyToMono(PrincipalUserDetails.class)
                .flatMap(this::generateTokensAndCreateResponse)
                .onErrorMap(Exception.class, e -> new GatewayException(ExceptionStatus.UNAUTHORIZED, "Invalid User"))
                .switchIfEmpty(Mono.error(new GatewayException(ExceptionStatus.UNAUTHORIZED, "Invalid User")))
                .onErrorResume(GatewayException.class, e -> ServerResponse.status(e.getStatus().getStatus().value()).bodyValue(e.getMessage()));
    }
    private Mono<ServerResponse> generateTokensAndCreateResponse(PrincipalUserDetails userDetails) {
        return jwtTokenProvider.generateToken(userDetails, false)
                .zipWith(jwtTokenProvider.generateToken(userDetails, true))
                .flatMap(tokens -> {
                    String accessToken = tokens.getT1();
                    String refreshToken = tokens.getT2();
                log.info("accessToken: {}", accessToken);
                log.info("refreshToken: {}", refreshToken);
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .cookie(createCookie("accessToken", accessToken, jwtTokenProvider.getAccessTokenExpired()))
                            .cookie(createCookie("refreshToken", refreshToken, jwtTokenProvider.getRefreshTokenExpired()))
                            .bodyValue(Boolean.TRUE);
                });
    }

    private ResponseCookie createCookie(String name, String value, long maxAge) {
        return ResponseCookie.from(name, value)
                .maxAge(maxAge)
                .path("/")
                .sameSite("None")
                .secure(true)
                .httpOnly(true)
                .build();
    }

}
