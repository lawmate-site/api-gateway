package site.lawmate.gateway.service.provider;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
@Service
@RequiredArgsConstructor
public class JwtTokenProvider{
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    private SecretKey SECRET_KEY;

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.issuer}")
    private String issuer;

    @Getter
    @Value("${jwt.expired.access}")
    private Long accessTokenExpired;

    @Getter
    @Value("${jwt.expired.refresh}")
    private Long refreshTokenExpired;

    @PostConstruct
    protected void init() {
        SECRET_KEY = Keys.hmacShaKeyFor(Base64.getUrlEncoder().encode(secretKey.getBytes()));
    }

    public String extractEmail(String jwt){
        return extractClaim(jwt, Claims::getSubject);
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String jwt){
        return extractClaim(jwt, i -> i.get("roles", List.class));
    }

    public String generateToken(UserDetails userDetails, boolean isRefreshToken){
        return Stream.of(generateToken(Map.of(), userDetails, isRefreshToken))
                .peek(i -> Stream.of(i)
                        .filter(j -> isRefreshToken)
                        // .peek(j -> tokenRedisRepository.save(TokenModel.builder().token(j).build()))
                        .findAny()
                        .get())
                .findAny()
                .get();
    }

    private String generateToken(Map<String, Object> extraClaims, UserDetails userDetails, boolean isRefreshToken){
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuer(issuer)
                .claim("roles", userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList())
                .claim("type", isRefreshToken ? "refresh" : "access")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(isRefreshToken ? refreshTokenExpired : accessTokenExpired)))
                .signWith(SECRET_KEY, Jwts.SIG.HS256)
                .compact();
    }

    private <T> T extractClaim(String jwt, Function<Claims, T> claimsResolver){
        return claimsResolver.apply(extractAllClaims(jwt));
    }

    private Claims extractAllClaims(String jwt){
        try {
            return Jwts.parser()
                    .verifyWith(SECRET_KEY)
                    .build()
                    .parseSignedClaims(jwt)
                    .getPayload();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public Boolean isTokenValid(String token) {
        return !isTokenExpired(token);
    }

    private Boolean isTokenExpired(String token){
        return extractClaim(token, Claims::getExpiration).before(Date.from(Instant.now()));
    }
}
