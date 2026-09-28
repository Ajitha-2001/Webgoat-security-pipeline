/*
 * SPDX-FileCopyrightText: Copyright © 2014 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.lessons.idor;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AssignmentEndpoint;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.container.session.UserSessionData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

public class IDOREditOtherProfile implements AssignmentEndpoint {

  @Autowired private UserSessionData userSessionData;

  @Autowired private LessonDataSource dataSource;

  @PutMapping(
      path = "/IDOR/profile/{userId}",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  public AttackResult completed(
      @PathVariable String userId,
      @RequestBody String userSubmittedProfile)
      throws IOException {

    // Get the authenticated user's ID from the server-side session.
    String authUserId =
        (String) userSessionData.getValue("idor-authenticated-user-id");

    /*
     * SECURITY FIX:
     * Block the request if:
     * 1. There is no authenticated user, OR
     * 2. The authenticated user's ID is different from the
     *    user ID requested in the URL.
     */
    if (authUserId == null || !authUserId.equals(userId)) {
      return failed(this)
          .feedback("Unauthorized profile access")
          .build();
    }

    ObjectMapper objectMapper = new ObjectMapper();

    Map<String, Object> userSubmittedProfileMap =
        objectMapper.readValue(userSubmittedProfile, HashMap.class);

    /*
     * IMPORTANT:
     * Load the profile using the authenticated user's ID,
     * not an arbitrary user ID supplied by the client.
     */
    UserProfile currentUserProfile =
        new UserProfile(dataSource, authUserId);

    if (userSubmittedProfileMap.containsKey("color")) {
      currentUserProfile.setColor(
          (String) userSubmittedProfileMap.get("color"));
    }

    if (userSubmittedProfileMap.containsKey("size")) {
      currentUserProfile.setSize(
          (String) userSubmittedProfileMap.get("size"));
    }

    if (userSubmittedProfileMap.containsKey("name")) {
      currentUserProfile.setName(
          (String) userSubmittedProfileMap.get("name"));
    }

    if (userSubmittedProfileMap.containsKey("role")) {
      currentUserProfile.setRole(
          (Integer) userSubmittedProfileMap.get("role"));
    }

    currentUserProfile.updateProfile();

    return success(this)
        .feedback("Profile updated successfully")
        .output(currentUserProfile.toString())
        .build();
  }
}