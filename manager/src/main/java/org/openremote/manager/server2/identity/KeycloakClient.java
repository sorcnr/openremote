package org.openremote.manager.server2.identity;

import org.jboss.resteasy.annotations.Form;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.keycloak.representations.AccessTokenResponse;
import org.openremote.container.Container;
import org.openremote.manager.server.observable.RetryWithDelay;
import rx.Observable;

import javax.ws.rs.*;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.util.logging.Logger;

import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static javax.ws.rs.core.Response.Status.Family.*;

public class KeycloakClient implements AutoCloseable {

    private static final Logger LOG = Logger.getLogger(KeycloakClient.class.getName());

    public static final String ADMIN_CLI_CLIENT = "admin-cli";
    public static final String CONTEXT_PATH = "auth";

    public static class AuthForm {

        @FormParam("client_id")
        public String clientId;

        @FormParam("username")
        public String username;

        @FormParam("password")
        public String password;

        @FormParam("grant_type")
        public String grantType;

        public AuthForm() {
        }

        public AuthForm(String clientId, String username, String password, String grantType) {
            this.clientId = clientId;
            this.username = username;
            this.password = password;
            this.grantType = grantType;
        }
    }

    interface Keycloak {

        @GET
        @Path("/")
        @Produces(TEXT_HTML)
        Response getWelcomePage();

        @POST
        @Path("realms/{realm}/protocol/openid-connect/token")
        @Consumes(APPLICATION_FORM_URLENCODED)
        @Produces(APPLICATION_JSON)
        AccessTokenResponse getAccessToken(@PathParam("realm") String realm, @Form AuthForm authForm);

    }

    final protected UriBuilder serverUri;
    final protected Client client;

    public KeycloakClient(UriBuilder serverUri, ResteasyClientBuilder clientBuilder) {
        this.serverUri = serverUri.replacePath(CONTEXT_PATH);
        this.client = clientBuilder.build();

        // TODO Not a great way to block startup while we wait for other services (Hystrix?)
        Observable.create(subscriber -> {
            Response response = null;
            try {
                response = getKeycloak().getWelcomePage();
                if (response != null &&
                    (response.getStatusInfo().getFamily() == SUCCESSFUL || response.getStatusInfo().getFamily() == REDIRECTION)) {
                    subscriber.onCompleted();
                } else {
                    if (response != null) {
                        subscriber.onError(new WebApplicationException(response.getStatus()));
                    } else {
                        subscriber.onError(new WebApplicationException("No response"));
                    }
                }
            } catch (Exception ex) {
                subscriber.onError(ex);
            } finally {
                if (response != null)
                    response.close();
            }
        }).retryWhen(
            new RetryWithDelay("Connecting to Keycloak server " + serverUri.build(), 10, 3000)
        ).toBlocking().singleOrDefault(null);

    }

    public Observable<AccessTokenResponse> authenticateDirectly(String realm, String clientId, String username, String password) {
        return Observable.create(subscriber -> {
            try {
                AccessTokenResponse response = getKeycloak().getAccessToken(
                    realm, new AuthForm(clientId, username, password, "password")
                );
                if (response != null) {
                    subscriber.onNext(response);
                    subscriber.onCompleted();
                } else {
                    subscriber.onError(new WebApplicationException("No response"));
                }
            } catch (Exception ex) {
                subscriber.onError(ex);
            }
        });
    }

/*

    public void handleProxyRequest(HttpServerRequest serverRequest) {
        String requestUri = UrlUtil.url(serverRequest.path()).withQuery(serverRequest.query()).toString();
        HttpClientRequest proxyRequest =
            proxyClient.request(
                serverRequest.method(),
                requestUri,
                keycloakResponse -> {
                    serverRequest.response().setChunked(true);
                    serverRequest.response().setStatusCode(keycloakResponse.statusCode());
                    serverRequest.response().headers().setAll(keycloakResponse.headers());
                    keycloakResponse.handler(data -> serverRequest.response().write(data));
                    keycloakResponse.endHandler((v) -> serverRequest.response().end());
                }
            );
        proxyRequest.setChunked(true);
        proxyRequest.headers().setAll(serverRequest.headers());
        serverRequest.handler(proxyRequest::write);
        serverRequest.endHandler((v) -> proxyRequest.end());
    }



    public Observable<RealmRepresentation> getRealm(String realm, String accessToken) {
        return client.get(
            UrlUtil.getPath(CONTEXT_PATH, "admin", "realms", realm),
            RealmRepresentation.class,
            request -> {
                addBearerAuthorization(request, accessToken);
                request.putHeader(ACCEPT, APPLICATION_JSON_VALUE);
                request.end();
            }
        ).flatMap(response -> Observable.just(response.getBody()));
    }

    public Observable<Integer> putRealm(String realm, String accessToken, RealmRepresentation realmRepresentation) {
        return client.put(
            UrlUtil.getPath(CONTEXT_PATH, "admin", "realms", realm),
            request -> {
                addBearerAuthorization(request, accessToken);
                request.putHeader(ACCEPT, APPLICATION_JSON_VALUE);
                request.putHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE);
                request.end(realmRepresentation);
            }
        ).flatMap(response -> Observable.just(response.statusCode()));
    }

    public Observable<String> registerClientApplication(String realm, ClientRepresentation clientRepresentation) {
        return client.post(
            UrlUtil.getPath(CONTEXT_PATH, "admin", "realms", realm, "clients"),
            request -> {
                if (clientRepresentation.getRegistrationAccessToken() == null) {
                    throw new IllegalStateException("Missing registration access token on client representation");
                }
                addBearerAuthorization(request, clientRepresentation.getRegistrationAccessToken());
                request.putHeader(ACCEPT, APPLICATION_JSON_VALUE);
                request.putHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE);
                request.end(clientRepresentation);
            }
        ).flatMap(response -> Observable.just(response.headers().get(LOCATION)));
    }

    public Observable<ClientRepresentation> getClientApplications(String realm, String accessToken) {
        return client.get(
            UrlUtil.getPath(CONTEXT_PATH, "admin", "realms", realm, "clients"),
            ClientRepresentation[].class,
            request -> {
                addBearerAuthorization(request, accessToken);
                request.putHeader(ACCEPT, APPLICATION_JSON_VALUE);
                request.end();
            }
        ).flatMap(response -> Observable.from(response.getBody()));
    }

    public Observable<ClientRepresentation> getClientApplicationByLocation(String realm, String accessToken, String location) {
        return client.get(
            location,
            ClientRepresentation.class,
            request -> {
                addBearerAuthorization(request, accessToken);
                request.putHeader(ACCEPT, APPLICATION_JSON_VALUE);
                request.end();
            }
        ).flatMap(response -> Observable.just(response.getBody()));
    }

    public Observable<ClientRepresentation> getClientApplication(String realm, String accessToken, String clientId) {
        return client.get(
            UrlUtil.getPath(CONTEXT_PATH, "admin", "realms", realm, "clients", clientId),
            ClientRepresentation.class,
            request -> {
                addBearerAuthorization(request, accessToken);
                request.putHeader(ACCEPT, APPLICATION_JSON_VALUE);
                request.end();
            }
        ).flatMap(response -> Observable.just(response.getBody()));
    }

    public Observable<Integer> deleteClientApplication(String realm, String accessToken, String clientId) {
        return client.delete(
            UrlUtil.getPath(CONTEXT_PATH, "admin", "realms", realm, "clients", clientId),
            request -> {
                addBearerAuthorization(request, accessToken);
                request.end();
            }
        ).flatMap(response -> Observable.just(response.statusCode()));
    }

    public Observable<String> createRoleForClientApplication(String realm, String accessToken, String clientObjectId, RoleRepresentation roleRepresentation) {
        return client.post(
            UrlUtil.getPath(CONTEXT_PATH, "admin", "realms", realm, "clients", clientObjectId, "roles"),
            request -> {
                addBearerAuthorization(request, accessToken);
                request.putHeader(ACCEPT, APPLICATION_JSON_VALUE);
                request.putHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE);
                request.end(roleRepresentation);
            }
        ).flatMap(response -> Observable.just(response.headers().get(LOCATION)));
    }

    public Observable<RoleRepresentation> getRoleOfClientApplication(String realm, String accessToken, String clientObjectId, String role) {
        return client.get(
            UrlUtil.getPath(CONTEXT_PATH, "admin", "realms", realm, "clients", clientObjectId, "roles", role),
            RoleRepresentation.class,
            request -> {
                addBearerAuthorization(request, accessToken);
                request.putHeader(ACCEPT, APPLICATION_JSON_VALUE);
                request.end();
            }
        ).flatMap(response -> Observable.just(response.getBody()));
    }

    public Observable<RoleRepresentation> getRoleOfClientApplicationByLocation(String realm, String accessToken, String location) {
        return client.get(
            location,
            RoleRepresentation.class,
            request -> {
                addBearerAuthorization(request, accessToken);
                request.putHeader(ACCEPT, APPLICATION_JSON_VALUE);
                request.end();
            }
        ).flatMap(response -> Observable.just(response.getBody()));
    }

    public Observable<Integer> addCompositesToRoleForClientApplication(String realm, String accessToken, String clientObjectId, String role, RoleRepresentation[] roleRepresentations) {
        return client.post(
            UrlUtil.getPath(CONTEXT_PATH, "admin", "realms", realm, "clients", clientObjectId, "roles", role, "composites"),
            request -> {
                addBearerAuthorization(request, accessToken);
                request.putHeader(ACCEPT, APPLICATION_JSON_VALUE);
                request.putHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE);
                request.end(roleRepresentations);
            }
        ).flatMap(response -> Observable.just(response.statusCode()));
    }

    public Observable<ClientInstall> getClientInstall(String realm, String clientId) {
        return getClientInstall(realm, clientId, null);
    }

    public Observable<ClientInstall> getClientInstall(String realm, String clientId, String clientSecret) {
        return client.get(
            UrlUtil.getPath(CONTEXT_PATH, "realms", realm, "clients-registrations", "install", clientId),
            ClientInstall.class,
            request -> {
                if (clientSecret != null) {
                    addClientAuthorization(request, clientId, clientSecret);
                }
                request.putHeader(ACCEPT, APPLICATION_JSON_VALUE);
                request.end();
            }
        ).flatMap(response -> {
            ClientInstall clientInstall = response.getBody();
            try {
                clientInstall.setPublicKey(PemUtils.decodePublicKey(clientInstall.getPublicKeyPEM()));
            } catch (Exception ex) {
                throw new RuntimeException("Error decoding public key PEM for realm: " + clientInstall.getRealm(), ex);
            }
            return Observable.just(clientInstall);
        });
    }

    public Observable<UserRepresentation> getUsers(String realm, String accessToken) {
        return client.get(
            UrlUtil.getPath(CONTEXT_PATH, "admin", "realms", realm, "users"),
            UserRepresentation[].class,
            request -> {
                addBearerAuthorization(request, accessToken);
                request.putHeader(ACCEPT, APPLICATION_JSON_VALUE);
                request.end();
            }
        ).flatMap(response -> Observable.from(response.getBody()));
    }

    public Observable<Integer> deleteUser(String realm, String accessToken, String userId) {
        return client.delete(
            UrlUtil.getPath(CONTEXT_PATH, "admin", "realms", realm, "users", userId),
            request -> {
                addBearerAuthorization(request, accessToken);
                request.end();
            }
        ).flatMap(response -> Observable.just(response.statusCode()));
    }

    public Observable<String> createUser(String realm, String accessToken, UserRepresentation userRepresentation) {
        return client.post(
            UrlUtil.getPath(CONTEXT_PATH, "admin", "realms", realm, "users"),
            request -> {
                addBearerAuthorization(request, accessToken);
                request.putHeader(ACCEPT, APPLICATION_JSON_VALUE);
                request.putHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE);
                request.end(userRepresentation);
            }
        ).flatMap(response -> Observable.just(response.headers().get(LOCATION)));
    }

    public Observable<UserRepresentation> getUserByLocation(String realm, String accessToken, String location) {
        return client.get(
            location,
            UserRepresentation.class,
            request -> {
                addBearerAuthorization(request, accessToken);
                request.putHeader(ACCEPT, APPLICATION_JSON_VALUE);
                request.end();
            }
        ).flatMap(response -> Observable.just(response.getBody()));
    }

    public Observable<Integer> resetPassword(String realm, String accessToken, String userId, CredentialRepresentation credentialRepresentation) {
        return client.put(
            UrlUtil.getPath(CONTEXT_PATH, "admin", "realms", realm, "users", userId, "reset-password"),
            request -> {
                addBearerAuthorization(request, accessToken);
                request.putHeader(ACCEPT, APPLICATION_JSON_VALUE);
                request.putHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE);
                request.end(credentialRepresentation);
            }
        ).flatMap(response -> Observable.just(response.statusCode()));
    }

    public Observable<Integer> addUserClientRoleMapping(String realm, String accessToken, String userId, String clientObjectId, RoleRepresentation[] roleRepresentations) {
        return client.post(
            UrlUtil.getPath(CONTEXT_PATH, "admin", "realms", realm, "users", userId, "role-mappings", "clients", clientObjectId),
            request -> {
                addBearerAuthorization(request, accessToken);
                request.putHeader(ACCEPT, APPLICATION_JSON_VALUE);
                request.putHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE);
                request.end(roleRepresentations);
            }
        ).flatMap(response -> Observable.just(response.statusCode()));
    }

    protected void addBearerAuthorization(RestClientRequest request, String accessToken) {
        String authorization = "Bearer " + accessToken;
        request.putHeader(HttpHeaders.AUTHORIZATION, authorization);
    }

    protected void addClientAuthorization(RestClientRequest request, String clientId, String clientSecret) {
        try {
            String authorization = "Basic " + Base64.getEncoder().encodeToString(
                (clientId + ":" + clientSecret).getBytes("utf-8")
            );
            request.putHeader(HttpHeaders.AUTHORIZATION, authorization);
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }
    */

    @Override
    public void close() {
        if (client != null)
            client.close();
    }

    protected Keycloak getKeycloak() {
        return ((ResteasyWebTarget) client.target(serverUri)).proxy(Keycloak.class);
    }

}