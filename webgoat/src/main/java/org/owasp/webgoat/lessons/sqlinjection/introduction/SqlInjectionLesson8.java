/*
 * SPDX-FileCopyrightText: Copyright © 2016 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.lessons.sqlinjection.introduction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AssignmentEndpoint;
import org.owasp.webgoat.container.assignments.AssignmentHints;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

import static org.owasp.webgoat.container.assignments.AttackResultBuilder.failed;
import static org.owasp.webgoat.container.assignments.AttackResultBuilder.success;

@RestController
@AssignmentHints(
    value = {
      "SqlStringInjectionHint.8.1",
      "SqlStringInjectionHint.8.2",
      "SqlStringInjectionHint.8.3",
      "SqlStringInjectionHint.8.4",
      "SqlStringInjectionHint.8.5"
    })
public class SqlInjectionLesson8 implements AssignmentEndpoint {

  private final LessonDataSource dataSource;

  public SqlInjectionLesson8(LessonDataSource dataSource) {
    this.dataSource = dataSource;
  }

  @PostMapping("/SqlInjection/attack8")
  @ResponseBody
  public AttackResult completed(
      @RequestParam String name,
      @RequestParam String auth_tan) {

    return injectableQueryConfidentiality(name, auth_tan);
  }

  protected AttackResult injectableQueryConfidentiality(
      String name,
      String auth_tan) {

    StringBuilder output = new StringBuilder();

    // SECURITY FIX:
    // Use placeholders instead of concatenating user-controlled input.
    String query =
        "SELECT * FROM employees WHERE last_name = ? AND auth_tan = ?";

    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement =
            connection.prepareStatement(
                query,
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY)) {

      // SECURITY FIX:
      // User input is treated as data instead of executable SQL.
      statement.setString(1, name);
      statement.setString(2, auth_tan);

      // Record the attempted lookup without constructing executable SQL.
      log(connection, name, auth_tan);

      try (ResultSet results = statement.executeQuery()) {

        if (results.first()) {

          // Move back before the first row because generateTable()
          // iterates through the complete ResultSet.
          results.beforeFirst();

          output.append(generateTable(results));

          results.last();

          if (results.getRow() > 1) {
            return success(this)
                .feedback("sql-injection.8.success")
                .output(output.toString())
                .build();
          }

          return failed(this)
              .feedback("sql-injection.8.one")
              .output(output.toString())
              .build();

        } else {
          return failed(this)
              .feedback("sql-injection.8.no.results")
              .build();
        }
      }

    } catch (SQLException e) {
      return failed(this)
          .output(
              "<br><span class='feedback-negative'>"
                  + HtmlUtils.htmlEscape(e.getMessage() == null ? "" : e.getMessage())
                  + "</span>")
          .build();

    } catch (Exception e) {
      return failed(this)
          .output(
              "<br><span class='feedback-negative'>"
                  + HtmlUtils.htmlEscape(e.getMessage() == null ? "" : e.getMessage())
                  + "</span>")
          .build();
    }
  }

  public static String generateTable(ResultSet results)
      throws SQLException {

    ResultSetMetaData resultsMetaData = results.getMetaData();
    int numColumns = resultsMetaData.getColumnCount();

    results.beforeFirst();

    StringBuilder table = new StringBuilder();
    table.append("<table>");

    if (results.next()) {

      table.append("<tr>");

      for (int i = 1; i <= numColumns; i++) {
        table.append("<th>")
            .append(
                HtmlUtils.htmlEscape(
                    resultsMetaData.getColumnName(i)))
            .append("</th>");
      }

      table.append("</tr>");

      results.beforeFirst();

      while (results.next()) {

        table.append("<tr>");

        for (int i = 1; i <= numColumns; i++) {

          String value = results.getString(i);

          table.append("<td>")
              .append(
                  HtmlUtils.htmlEscape(
                      value == null ? "" : value))
              .append("</td>");
        }

        table.append("</tr>");
      }

    } else {
      table.append(
          "Query Successful; however no data was returned from this query.");
    }

    table.append("</table>");

    return table.toString();
  }

  public static void log(
      Connection connection,
      String name,
      String authTan) {

    Calendar cal = Calendar.getInstance();

    SimpleDateFormat sdf =
        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    String time = sdf.format(cal.getTime());

    // Log the attempted operation without creating executable SQL.
    String action =
        "Employee lookup: last_name="
            + name
            + ", auth_tan="
            + authTan;

    String logQuery =
        "INSERT INTO access_log (time, action) VALUES (?, ?)";

    try (PreparedStatement statement =
        connection.prepareStatement(logQuery)) {

      statement.setString(1, time);
      statement.setString(2, action);

      statement.executeUpdate();

    } catch (SQLException e) {
      System.err.println(
          "Unable to write access log: " + e.getMessage());
    }
  }
}