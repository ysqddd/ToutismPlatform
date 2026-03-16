package org.example.toutismplatform.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {
    @Value("${jwt.secret}")
    private String secret;
    
    @Value("${jwt.expiration}")
    private long expiration;
    
    // 获取签名密钥
    private SecretKey getSigningKey() {
        // 将字符串转换为字节数组，并确保长度至少为 256 位（32 字节）
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        
        // 如果密钥太短，扩展它
        if (keyBytes.length < 32) {
            byte[] extendedKey = new byte[32];
            System.arraycopy(keyBytes, 0, extendedKey, 0, keyBytes.length);
            keyBytes = extendedKey;
        }
        
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    public String generateToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }
    
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        
        return claims.getSubject();
    }
    
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (SignatureException | MalformedJwtException | ExpiredJwtException | UnsupportedJwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
