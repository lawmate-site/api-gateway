package site.lawmate.gateway.service.impl;

import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.userinfo.DefaultReactiveOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.ReactiveOAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import site.lawmate.gateway.domain.model.OAuth2UserInfo;
import site.lawmate.gateway.domain.model.PrincipalUserDetails;
import site.lawmate.gateway.domain.model.UserModel;
import site.lawmate.gateway.domain.vo.Registration;
import site.lawmate.gateway.domain.vo.Role;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PrincipalOauthUserService implements ReactiveOAuth2UserService<OAuth2UserRequest, OAuth2User> {
    private final WebClient webClient;

    @Override
    public Mono<OAuth2User> loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        return new DefaultReactiveOAuth2UserService()
                .loadUser(userRequest)
                .log()
                .flatMap(user -> Mono.just(user.getAttributes()))
                .flatMap(attributes ->
                        Mono.just(userRequest.getClientRegistration().getClientName())
                                .log()
                                .flatMap(clientId -> Mono.just(Registration.getRegistration(clientId)))
                                .flatMap(registration ->
                                        Mono.just(OAuth2UserInfo.of(registration, attributes))
                                                .flatMap(oAuth2UserInfo ->
                                                        Mono.just(
                                                                        UserModel.builder()
                                                                                .id(oAuth2UserInfo.id())
                                                                                .email(oAuth2UserInfo.email())
                                                                                .name(oAuth2UserInfo.name())
                                                                                .profile(oAuth2UserInfo.profile())
                                                                                .roles(List.of(Role.ROLE_USER))
                                                                                .registration(registration)
                                                                                .build()
                                                                )
                                                                .filterWhen(i ->
                                                                        webClient.post()
                                                                                .uri("lb://user-service/auth/oauth2/" + i.getRegistration().name().toLowerCase())
                                                                                .accept(MediaType.APPLICATION_JSON)
                                                                                .bodyValue(i)
                                                                                .retrieve()
                                                                                .bodyToMono(Boolean.class)
                                                                )
                                                                .flatMap(user -> Mono.just(new PrincipalUserDetails(user, attributes)))
                                                )
                                )
                );
    }
}
