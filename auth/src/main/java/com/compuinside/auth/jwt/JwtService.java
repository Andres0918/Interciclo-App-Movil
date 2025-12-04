package com.compuinside.auth.jwt;

import com.compuinside.auth.controller.AuthResponse;
import com.compuinside.auth.exception.CompuInsideCustomException;
import com.compuinside.auth.repository.TokenRepository;
import com.compuinside.auth.user.Account;
import com.compuinside.auth.user.AccountRepository;
import com.compuinside.auth.user.User;
import com.compuinside.auth.user.UsersRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

@Service
@Slf4j
@RequiredArgsConstructor
public class JwtService {

    @Value("${jwt.secret}")
    private String SECRET_KEY;
    private final UsersRepository usersRepository;
    @Value("${jwt.expiration}")
    private long jwtExpiration;
    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;
    private final TokenRepository tokenRepository;

    public String getToken(UserDetails user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "access");
        return getToken(claims, user, jwtExpiration);
    }

    public String getRefreshToken(UserDetails user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        return getToken(claims, user, refreshExpiration);
    }

    private String getToken(Map<String, Object> extraClaims, UserDetails userDet, long expirationTime) {
        User user = usersRepository.findByUsername(userDet.getUsername()).orElseThrow(() -> new RuntimeException("User not found"));
        Account account = user.getAccount();
        System.out.println(account);
        extraClaims.put("role", user.getAuthorities().iterator().next().getAuthority());
        extraClaims.put("module", user.getServiceClient());
        extraClaims.put("userId", user.getId());
        extraClaims.put("accountId", account.getId());
        extraClaims.put("userPlan", account.getUserPlan());
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(user.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private Key getKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET_KEY));
    }

    public String getUsernameFromToken(String token) {
        return getClaim(token, Claims::getSubject);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = getUsernameFromToken(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    protected Claims getAllClaims (String token){
        return Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public <T> T getClaim (String token, Function<Claims, T> claimsResolver){
        final Claims claims = getAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public Date getExpirationDateFromToken(String token) {
        return getClaim(token, Claims::getExpiration);
    }

    private boolean isTokenExpired(String token) {
        return getExpirationDateFromToken(token).before(new Date());
    }

    public boolean isTokenValidWBD(String token, UserDetails userDetails) {
        try {
            Claims claims = getAllClaims(token);

            /*String type = claims.get("type", String.class);
            if (!"refresh".equals(type)) {
                log.error("Token inválido: se esperaba un access token, llegó {}", type);
                return false;
            }*/

            final String username = claims.getSubject();
            boolean firmaValida = username.equals(userDetails.getUsername()) && !isTokenExpired(token);

            return firmaValida && tokenRepository.findByToken(token)
                    .filter(t -> !t.isExpired() && !t.isRevoked())
                    .isPresent();
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Token inválido: {}", e.getMessage());
            return false;
        }
    }



}
