package site.lawmate.gatewayservice.router;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties
public class UriConfiguration {
    private String httpbin = "http://httpbin.org:80";

}
