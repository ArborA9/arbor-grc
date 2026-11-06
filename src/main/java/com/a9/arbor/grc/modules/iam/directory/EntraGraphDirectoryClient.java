package com.a9.arbor.grc.modules.iam.directory;

import com.a9.arbor.grc.modules.iam.web.dto.EmployeeDirectoryEntry;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * App-only Microsoft Graph calls using the tenant's existing SSO client id/secret.
 * Timeouts are short. Failures throw {@link GraphDirectoryException}; callers fail open.
 * Not used by the OIDC login path.
 */
@Component
public class EntraGraphDirectoryClient {

    private static final Logger log = LoggerFactory.getLogger(EntraGraphDirectoryClient.class);
    private static final String GRAPH = "https://graph.microsoft.com/v1.0";
    private static final String SCOPE = "https://graph.microsoft.com/.default";
    private static final int TOP = 20;

    private final RestClient restClient;
    private final ConcurrentHashMap<String, CachedToken> tokens = new ConcurrentHashMap<>();

    public EntraGraphDirectoryClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(8));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    EntraGraphDirectoryClient(RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * Distinct non-blank {@code user.department} values. Entra has no department directory object.
     * Stops after 20 pages so a huge tenant cannot hang the request.
     */
    public List<String> listDepartments(String entraDirectoryId, String clientId, String clientSecret) {
        String token = accessToken(entraDirectoryId, clientId, clientSecret);
        java.util.LinkedHashSet<String> names = new java.util.LinkedHashSet<>();
        String url = GRAPH + "/users?$select=department&$top=999&$filter="
                + enc("accountEnabled eq true");
        for (int page = 0; page < 20 && url != null; page++) {
            JsonNode body = getJson(token, url);
            for (JsonNode user : body.path("value")) {
                String department = textOrNull(user, "department");
                if (department != null) {
                    names.add(department);
                }
            }
            JsonNode next = body.get("@odata.nextLink");
            url = next == null || next.isNull() || next.asText().isBlank() ? null : next.asText();
        }
        return List.copyOf(names);
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText().trim();
        return text.isEmpty() ? null : text;
    }

    /**
     * Whether this Entra account is enabled. Empty when Graph has no matching user
     * (mail and UPN both miss). Callers treat empty and transport errors as "unknown".
     */
    public Optional<Boolean> accountEnabled(String entraDirectoryId, String clientId,
                                            String clientSecret, String email) {
        String token = accessToken(entraDirectoryId, clientId, clientSecret);
        try {
            JsonNode one = getJson(token, GRAPH + "/users/" + encPath(email) + "?$select=accountEnabled");
            if (one != null && one.has("accountEnabled")) {
                return Optional.of(one.path("accountEnabled").asBoolean(true));
            }
        } catch (GraphDirectoryException e) {
            if (!isNotFound(e)) {
                throw e;
            }
        }
        String escaped = email.replace("'", "''");
        String filter = "mail eq '" + escaped + "' or userPrincipalName eq '" + escaped + "'";
        JsonNode page = getJson(token, GRAPH + "/users?$select=accountEnabled&$top=1&$filter=" + enc(filter));
        JsonNode values = page.path("value");
        if (!values.isArray() || values.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(values.get(0).path("accountEnabled").asBoolean(true));
    }

    public List<EmployeeDirectoryEntry> search(String entraDirectoryId, String clientId,
                                               String clientSecret, String query) {
        String token = accessToken(entraDirectoryId, clientId, clientSecret);
        try {
            JsonNode page = getUsers(token, query, true);
            return GraphUserMapper.fromUsersArray(page.path("value"));
        } catch (GraphDirectoryException e) {
            if (isAuthFailure(e)) {
                throw e;
            }
            log.debug("Graph user search with $expand=manager failed ({}), retrying without expand",
                    e.getMessage());
            JsonNode page = getUsers(token, query, false);
            return fillManagers(token, GraphUserMapper.fromUsersArray(page.path("value")));
        }
    }

    private JsonNode getUsers(String token, String query, boolean expandManager) {
        // Graph $search treats "." and "@" as token breaks, so "jane.doe@" returns nobody.
        // Prefix filter matches mail, UPN, and display name as the invite field is typed.
        if (query.indexOf('@') >= 0 || query.indexOf('.') >= 0) {
            return getUsersByPrefix(token, query, expandManager);
        }
        String kql = "displayName:" + query
                + " OR mail:" + query
                + " OR userPrincipalName:" + query
                + " OR jobTitle:" + query
                + " OR department:" + query;
        StringBuilder url = new StringBuilder(GRAPH).append("/users")
                .append("?$search=").append(enc("\"" + kql + "\""))
                .append("&$count=true")
                .append("&$top=").append(TOP)
                .append("&$filter=").append(enc("accountEnabled eq true"))
                .append("&$select=id,displayName,jobTitle,department,mail,userPrincipalName");
        appendManagerExpand(url, expandManager);
        return getJson(token, url.toString());
    }

    private JsonNode getUsersByPrefix(String token, String query, boolean expandManager) {
        String literal = query.replace("'", "''");
        String filter = "accountEnabled eq true and ("
                + "startswith(mail,'" + literal + "') or "
                + "startswith(userPrincipalName,'" + literal + "') or "
                + "startswith(displayName,'" + literal + "'))";
        StringBuilder url = new StringBuilder(GRAPH).append("/users")
                .append("?$count=true")
                .append("&$top=").append(TOP)
                .append("&$filter=").append(enc(filter))
                .append("&$select=id,displayName,jobTitle,department,mail,userPrincipalName");
        appendManagerExpand(url, expandManager);
        return getJson(token, url.toString());
    }

    private static void appendManagerExpand(StringBuilder url, boolean expandManager) {
        if (expandManager) {
            url.append("&$expand=").append(enc("manager($select=displayName,mail,userPrincipalName,jobTitle,department)"));
        }
    }

    private List<EmployeeDirectoryEntry> fillManagers(String token, List<EmployeeDirectoryEntry> rows) {
        List<EmployeeDirectoryEntry> out = new ArrayList<>(rows.size());
        for (EmployeeDirectoryEntry row : rows) {
            if (row.directoryId() == null || row.managerName() != null || row.managerEmail() != null) {
                out.add(row);
                continue;
            }
            try {
                JsonNode manager = getJson(token, GRAPH + "/users/" + encPath(row.directoryId())
                        + "/manager?$select=displayName,mail,userPrincipalName,jobTitle,department");
                out.add(GraphUserMapper.withManager(row, manager));
            } catch (GraphDirectoryException e) {
                out.add(row);
            }
        }
        return out;
    }

    private String accessToken(String entraDirectoryId, String clientId, String clientSecret) {
        String key = entraDirectoryId + "|" + clientId;
        CachedToken cached = tokens.get(key);
        if (cached != null && Instant.now().isBefore(cached.expiresAt())) {
            return cached.token();
        }
        String tokenUrl = "https://login.microsoftonline.com/" + entraDirectoryId + "/oauth2/v2.0/token";
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("scope", SCOPE);
        form.add("grant_type", "client_credentials");
        try {
            GraphTokenResponse body = restClient.post()
                    .uri(tokenUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        throw new GraphDirectoryException("Graph token HTTP " + res.getStatusCode().value());
                    })
                    .body(GraphTokenResponse.class);
            if (body == null || body.accessToken() == null || body.accessToken().isBlank()) {
                throw new GraphDirectoryException("Graph token response had no access_token");
            }
            long ttl = body.expiresIn() > 120 ? body.expiresIn() - 60 : Math.max(body.expiresIn() - 10, 30);
            tokens.put(key, new CachedToken(body.accessToken(), Instant.now().plusSeconds(ttl)));
            return body.accessToken();
        } catch (GraphDirectoryException e) {
            throw e;
        } catch (RestClientException e) {
            throw new GraphDirectoryException("Graph token request failed", e);
        }
    }

    private JsonNode getJson(String token, String url) {
        try {
            return restClient.get()
                    .uri(url)
                    .header("Authorization", "Bearer " + token)
                    .header("ConsistencyLevel", "eventual")
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        throw new GraphDirectoryException("Graph HTTP " + res.getStatusCode().value());
                    })
                    .body(JsonNode.class);
        } catch (GraphDirectoryException e) {
            throw e;
        } catch (RestClientException e) {
            throw new GraphDirectoryException("Graph request failed", e);
        }
    }

    private static boolean isNotFound(GraphDirectoryException e) {
        String msg = e.getMessage();
        return msg != null && msg.contains("HTTP 404");
    }

    private static boolean isAuthFailure(GraphDirectoryException e) {
        String msg = e.getMessage();
        return msg != null && (msg.contains("HTTP 401") || msg.contains("HTTP 403"));
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /** Object id is a GUID; encode anyway so a bad id cannot change the path. */
    private static String encPath(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public record GraphTokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") long expiresIn
    ) {
    }

    private record CachedToken(String token, Instant expiresAt) {
    }
}
