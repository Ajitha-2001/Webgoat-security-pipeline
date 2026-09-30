/*
 * SPDX-FileCopyrightText: Copyright © 2018 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.lessons.jwt;

import static org.owasp.webgoat.container.assignments.AttackResultBuilder.failed;
import static org.owasp.webgoat.container.assignments.AttackResultBuilder.success;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.TextCodec;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.owasp.webgoat.container.assignments.AssignmentEndpoint;
import org.owasp.webgoat.container.assignments.AssignmentHints;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AssignmentHints({"jwt-secret-hint1", "jwt-secret-hint2", "jwt-secret-hint3"})
public class JWTSecretKeyEndpoint implements AssignmentEndpoint {

  // NOTE: This array is kept only so existing integration tests referencing
  // JWTSecretKeyEndpoint.SECRETS still compile. It is NO LONGER used to
  // generate the actual signing secret (see loadSecret() below).
  public static final String[] SECRETS = {
    "victory", "business", "available", "shipping", "washington"
  };

  public static final String JWT_SECRET = TextCodec.BASE64.encode(loadSecret());

  private static String loadSecret() {
    String secret = System.getenv("JWT_SECRET_KEY");
    if (secret == null || secret.isBlank()) {
      // No secret configured (e.g. CI/tests): generate a strong random one per startup
      byte[] bytes = new byte[48];
      new SecureRandom().nextBytes(bytes);
      return Base64.getEncoder().encodeToString(bytes);
    }
    if (secret.length() < 32) {
      throw new IllegalStateException("JWT_SECRET_KEY must be at least 32 characters");
    }
    return secret;
  }

  private static final String WEBGOAT_USER = "WebGoat";
  private static final List<String> expectedClaims =
      List.of("iss", "iat", "exp", "aud", "sub", "username", "Email", "Role");

  @RequestMapping(path = "/JWT/secret/gettoken", produces = MediaType.TEXT_HTML_VALUE)
  @ResponseBody
  public String getSecretToken() {
    return Jwts.builder()
        .setIssuer("WebGoat Token Builder")
        .setAudience("webgoat.org")
        .setIssuedAt(Calendar.getInstance().getTime())
        .setExpiration(Date.from(Instant.now().plusSeconds(60)))
        .setSubject("tom@webgoat.org")
        .claim("username", "Tom")
        .claim("Email", "tom@webgoat.org")
        .claim("Role", new String[] {"Manager", "Project Administrator"})
        .signWith(SignatureAlgorithm.HS256, JWT_SECRET)
        .compact();
  }

  @PostMapping("/JWT/secret")
  @ResponseBody
  public AttackResult login(@RequestParam String token) {
    try {
      Jwt jwt = Jwts.parser().setSigningKey(JWT_SECRET).parseClaimsJws(token);
      Claims claims = (Claims) jwt.getBody();
      if (!claims.keySet().containsAll(expectedClaims)) {
        return failed(this).feedback("jwt-secret-claims-missing").build();
      } else {
        String user = (String) claims.get("username");

        if (WEBGOAT_USER.equalsIgnoreCase(user)) {
          return success(this).build();
        } else {
          return failed(this).feedback("jwt-secret-incorrect-user").feedbackArgs(user).build();
        }
      }
    } catch (Exception e) {
      return failed(this).feedback("jwt-invalid-token").output(e.getMessage()).build();
    }
  }
}