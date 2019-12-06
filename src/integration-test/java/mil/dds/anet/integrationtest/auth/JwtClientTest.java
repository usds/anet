package mil.dds.anet.integrationtest.auth;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.PlainHeader;
import com.nimbusds.jose.JWSObject.State;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.pac4j.core.context.J2EContext;
import org.pac4j.core.credentials.TokenCredentials;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.core.profile.definition.ProfileDefinition;
import org.pac4j.jwt.config.encryption.ECEncryptionConfiguration;
import org.pac4j.jwt.config.encryption.EncryptionConfiguration;
import org.pac4j.jwt.config.encryption.SecretEncryptionConfiguration;
import org.pac4j.jwt.config.signature.RSASignatureConfiguration;
import org.pac4j.jwt.config.signature.SecretSignatureConfiguration;
import org.pac4j.jwt.config.signature.SignatureConfiguration;
import org.pac4j.jwt.credentials.authenticator.JwtAuthenticator;
import org.pac4j.jwt.profile.JwtProfile;

/**
 * Test based on: Local instance of https://github.com/auth0-blog/nodejs-jwt-authentication-sample
 */
public class JwtClientTest {

  @Test
  public void test() throws IOException, ParseException {



    String MAC_SECRET = "12345678901234567890123456789012";
    final String token =
        "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwczovL2dvbnRvLmNvbSIsImF1ZCI6Im5vZGVqcy1qd3QtYXV0aCIsImV4cCI6MTU3NTU0NTg2MCwic2NvcGUiOiJmdWxsX2FjY2VzcyIsInN1YiI6ImxhbGFsYW5kfGdvbnRvIiwianRpIjoiend5a2haTExSbmRQRTAxNCIsImFsZyI6IkhTMjU2IiwiaWF0IjoxNTc1NTQyMjYwfQ.DepCSWXQKBnrzFo6Qpis-w0ZW5M8-AyFbVKUJsJwAGg";
    /*
     * final TokenCredentials credentials = new TokenCredentials(token); final JwtAuthenticator
     * authenticator = new JwtAuthenticator(new SecretSignatureConfiguration(MAC_SECRET), new
     * SecretEncryptionConfiguration(MAC_SECRET)); authenticator.validate(credentials, null);
     * assertThat(credentials.getUserProfile()).isNotNull();
     */

    SignedJWT signedJWT = null;
    signedJWT = SignedJWT.parse(token);
    // continue with header and claims extraction...
    JWSHeader m = signedJWT.getHeader();
    JWTClaimsSet m2 = signedJWT.getJWTClaimsSet();
    String keyID = signedJWT.getHeader().getKeyID();
    Payload c = signedJWT.getPayload();
    State k = signedJWT.getState();
    String x9 = "";

    // final String response =
    // sendServerRequest("http://localhost:3001/api/protected/random-quote", "GET");
    // final String response = sendServerRequest("http://localhost:3001/api/random-quote", "GET");
    // assertThat(response).isEqualTo("");

    /*
    final String token =
        "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwczovL2dvbnRvLmNvbSIsImF1ZCI6Im5vZGVqcy1qd3QtYXV0aCIsImV4cCI6MTU3NTU0NTg2MCwic2NvcGUiOiJmdWxsX2FjY2VzcyIsInN1YiI6ImxhbGFsYW5kfGdvbnRvIiwianRpIjoiend5a2haTExSbmRQRTAxNCIsImFsZyI6IkhTMjU2IiwiaWF0IjoxNTc1NTQyMjYwfQ.DepCSWXQKBnrzFo6Qpis-w0ZW5M8-AyFbVKUJsJwAGg";
    TokenCredentials tokenCredentials = new TokenCredentials(token);

    JwtAuthenticator jwtAuthenticator = new JwtAuthenticator();
*/
    /*
     * jwtAuthenticator.validate(tokenCredentials, null); CommonProfile p =
     * tokenCredentials.getUserProfile();
     */
/*
    CommonProfile x1 = jwtAuthenticator.validateToken(token);
    Map<String, Object> x2 = jwtAuthenticator.validateTokenAndGetClaims(token);

    List<EncryptionConfiguration> aa = jwtAuthenticator.getEncryptionConfigurations();
    List<SignatureConfiguration> bb = jwtAuthenticator.getSignatureConfigurations();
    // Date cc = jwtAuthenticator.getExpirationTime();
    ProfileDefinition<JwtProfile> profileDefinition = jwtAuthenticator.getProfileDefinition();
    List<String> a = profileDefinition.getPrimaryAttributes();
    List<String> b = profileDefinition.getSecondaryAttributes();
    String d = profileDefinition.getProfileId();
    String c = jwtAuthenticator.getRealmName();
    JwtProfile cd = profileDefinition.newProfile(new ArrayList<>());
    String x = "10";

    */

    /*
    final String token =
        "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwczovL2dvbnRvLmNvbSIsImF1ZCI6Im5vZGVqcy1qd3QtYXV0aCIsImV4cCI6MTU3NTU0NTg2MCwic2NvcGUiOiJmdWxsX2FjY2VzcyIsInN1YiI6ImxhbGFsYW5kfGdvbnRvIiwianRpIjoiend5a2haTExSbmRQRTAxNCIsImFsZyI6IkhTMjU2IiwiaWF0IjoxNTc1NTQyMjYwfQ.DepCSWXQKBnrzFo6Qpis-w0ZW5M8-AyFbVKUJsJwAGg";
    JwtAuthenticator jwtAuthenticator = new JwtAuthenticator();

    // define two signature configurations (one based on the KEY2 secret and the other one based on
    // a generated RSA key pair)
    jwtAuthenticator.addSignatureConfiguration(new SecretSignatureConfiguration(KEY2));
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
    KeyPair rsaKeyPair = keyGen.generateKeyPair();
    jwtAuthenticator.addSignatureConfiguration(new RSASignatureConfiguration(rsaKeyPair));

    // define two encryption configurations (one based on the SECRET secret and the other one based
    // on a generated elliptic curve key pair)
    jwtAuthenticator.addEncryptionConfiguration(new SecretEncryptionConfiguration(SECRET));
    KeyPairGenerator keyGen2 = KeyPairGenerator.getInstance("EC");
    KeyPair ecKeyPair = keyGen2.generateKeyPair();
    ECEncryptionConfiguration encConfig = new ECEncryptionConfiguration(ecKeyPair);
    encConfig.setAlgorithm(JWEAlgorithm.ECDH_ES_A128KW);
    encConfig.setMethod(EncryptionMethod.A192CBC_HS384);
    jwtAuthenticator.addEncryptionConfiguration(encConfig);

    jwtAuthenticator.validateToken(token);
*/

    /*
    J2EContext context = new J2EContext(request, response);
    ProfileManager manager = new ProfileManager(context);
    */
  }

  private String sendServerRequest(String request, String requestType) throws IOException {
    final URL url = new URL(request);
    final HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();

    /*
    if (!Utils.isEmptyOrNull(smtpUsername) || !Utils.isEmptyOrNull(smtpPassword)) {
      final String userpass = smtpUsername + ":" + smtpPassword;
      final String basicAuth =
          "Basic " + new String(Base64.getEncoder().encode(userpass.getBytes()));
      httpConnection.setRequestProperty("Authorization", basicAuth);
    }
    */

    httpConnection.setDoOutput(true);
    httpConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
    httpConnection.setRequestProperty("Accept-Charset", StandardCharsets.UTF_8.name());
    httpConnection.setRequestMethod(requestType);
    httpConnection.connect();
    final InputStream response = httpConnection.getInputStream();
    return IOUtils.toString(response, StandardCharsets.UTF_8.name());
  }

}