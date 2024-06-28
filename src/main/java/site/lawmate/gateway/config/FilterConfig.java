//package site.lawmate.gateway.config;
//
//import lombok.AllArgsConstructor;
//import org.springframework.cloud.gateway.route.RouteLocator;
//import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
//import org.springframework.context.annotation.Bean;
//import org.springframework.stereotype.Component;
//import site.lawmate.gateway.filter.AuthorizationHeaderFilter;
//
//@Component
//@AllArgsConstructor
//public class FilterConfig {
//
//    @Bean
//    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder, AuthorizationHeaderFilter headerFilter) {
//        return builder.routes()
//                .route(r -> r.path("/user-service/**")
//                        .filters(f -> f.addRequestHeader("user-request", "user-request-header")
//                                .addResponseHeader("user-response", "user-response-header"))
//                                //.filter(headerFilter.apply(new AuthorizationHeaderFilter.Config())))
//                        .uri("http://localhost:8080"))
//                .route(r -> r.path("/lawyer-service/**")
//                        .filters(f -> f.addRequestHeader("lawyer-request", "lawyer-request-header")
//                                .addResponseHeader("lawyer-response", "lawyer-response-header"))
//                                //.filter(headerFilter.apply(new AuthorizationHeaderFilter.Config())))
//                        .uri("http://localhost:8081"))
//                .route(r -> r.path("/admin-service/**")
//                        .filters(f -> f.addRequestHeader("admin-request", "admin-request-header")
//                                .addResponseHeader("admin-response", "admin-response-header"))
//                                //.filter(headerFilter.apply(new AuthorizationHeaderFilter.Config())))
//                        .uri("http://localhost:8082"))
//                .route(r -> r.path("/chat-serivce/**")
//                        .filters(f -> f.addRequestHeader("chat-request", "chat-request-header")
//                                .addResponseHeader("chat-response", "chat-response-header"))
//                                //.filter(headerFilter.apply(new AuthorizationHeaderFilter.Config())))
//                        .uri("http://localhost:8083"))
//                .route(r -> r.path("/manage-service/**")
//                        .filters(f -> f.addRequestHeader("manage-request", "manage-request-header")
//                                .addResponseHeader("manage-response", "manage-response-header"))
//                                //.filter(headerFilter.apply(new AuthorizationHeaderFilter.Config())))
//                        .uri("http://localhost:8084"))
//                .build();
//    }
//
//}
