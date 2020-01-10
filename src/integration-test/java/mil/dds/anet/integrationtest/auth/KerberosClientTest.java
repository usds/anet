package mil.dds.anet.integrationtest.auth;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.security.auth.kerberos.KerberosKey;
import com.kerb4j.client.SpnegoClient;
import com.kerb4j.server.marshall.Kerb4JException;
import com.kerb4j.server.marshall.pac.Pac;
import com.kerb4j.server.marshall.pac.PacLogonInfo;
import com.kerb4j.server.marshall.pac.PacSid;
import com.kerb4j.server.marshall.spnego.SpnegoInitToken;
import com.kerb4j.server.marshall.spnego.SpnegoKerberosMechToken;
import org.apache.kerby.kerberos.kerb.KrbException;
import org.junit.Test;
import org.pac4j.core.context.HttpConstants;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.kerberos.client.direct.DirectKerberosClient;
import org.pac4j.kerberos.credentials.KerberosCredentials;
import org.pac4j.kerberos.credentials.authenticator.KerberosAuthenticator;
import org.pac4j.kerberos.credentials.authenticator.SunJaasKerberosTicketValidator;
import org.springframework.core.io.FileSystemResource;
import mil.dds.anet.integrationtest.auth.util.MockWebContext;

public class KerberosClientTest {

  private final static byte[] KERBEROS_TICKET =
      Base64.getEncoder().encode("Test Kerberos".getBytes(StandardCharsets.UTF_8));

  @Test
  public void test() throws IOException {
    final SunJaasKerberosTicketValidator validator = new SunJaasKerberosTicketValidator();
    // HTTP/fully-qualified-domain-name@DOMAIN
    validator.setServicePrincipal("user8@LOCALHOST");
    // the keytab file must contain the keys for the service principal, and should be protected
    validator.setKeyTabLocation(
        new FileSystemResource("/home/jose/Downloads/tmp2/http.localhost.keytab"));
    // validator.setHoldOnToGSSContext(true);
    validator.setDebug(true);

    byte[] ticket = Files.readAllBytes(Paths.get("/tmp/krb5cc_0"));
    // ticket = new String(ticket).getBytes(StandardCharsets.UTF_8);
    ticket = Base64.getEncoder().encode(ticket);

    final String username = validator.validateTicket(ticket).username();

    final DirectKerberosClient client =
        new DirectKerberosClient(new KerberosAuthenticator(validator));

    assertThat(username).isEqualTo("");

    final KerberosCredentials kc = new KerberosCredentials(KERBEROS_TICKET);
    assertThat(kc.getUserProfile()).isNotNull();
    assertThat(client.getUserProfile(kc, null)).isNotNull();

    final MockWebContext context = MockWebContext.create();

    context.addRequestHeader(HttpConstants.AUTHORIZATION_HEADER,
        "Negotiate " + new String(KERBEROS_TICKET, StandardCharsets.UTF_8));
    final KerberosCredentials credentials = client.getCredentials(context);
    assertThat(new String(Base64.getDecoder().decode(KERBEROS_TICKET), StandardCharsets.UTF_8))
        .isEqualTo(new String(credentials.getKerberosTicket(), StandardCharsets.UTF_8));

    final CommonProfile profile = (CommonProfile) client.getUserProfile(credentials, context);
    assertThat(profile.getId()).isEqualTo("jose/admin@LOCALHOST");
  }

  @Test
  public void testKerb4j() throws Kerb4JException, KrbException, IOException {
    final SpnegoClient spnegoClient =
        SpnegoClient.loginWithKeyTab("user8", "/home/jose/Downloads/tmp2/http.localhost.keytab");
    final KerberosKey[] a = spnegoClient.getKerberosKeys();

    final byte[] ticket = Files.readAllBytes(Paths.get("/tmp/krb5cc_1001"));
    // ticket = new String(ticket).getBytes(StandardCharsets.UTF_8);
    // ticket = Base64.getEncoder().encode(ticket);

    final SpnegoInitToken spnegoInitToken = new SpnegoInitToken(ticket);
    final SpnegoKerberosMechToken spnegoKerberosMechToken =
        spnegoInitToken.getSpnegoKerberosMechToken();
    final Pac pac = spnegoKerberosMechToken.getPac(spnegoClient.getKerberosKeys());
    final PacLogonInfo logonInfo = pac.getLogonInfo();
    final List<String> roles = Stream.of(logonInfo.getGroupSids())
        .map(PacSid::toHumanReadableString).collect(Collectors.toList());
  }

}
