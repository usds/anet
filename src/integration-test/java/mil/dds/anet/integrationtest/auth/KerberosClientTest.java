package mil.dds.anet.integrationtest.auth;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
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
    SunJaasKerberosTicketValidator validator = new SunJaasKerberosTicketValidator();
    // HTTP/fully-qualified-domain-name@DOMAIN
    validator.setServicePrincipal("jose/admin@LOCALHOST");
    // the keytab file must contain the keys for the service principal, and should be protected
    validator.setKeyTabLocation(new FileSystemResource("/tmp/krb5cc_1001"));
    // validator.setDebug(true);

    final DirectKerberosClient client = new DirectKerberosClient(new KerberosAuthenticator(validator));
    final MockWebContext context = MockWebContext.create();

    context.addRequestHeader(HttpConstants.AUTHORIZATION_HEADER, "Negotiate " + new String(KERBEROS_TICKET, StandardCharsets.UTF_8));
    final KerberosCredentials credentials = client.getCredentials(context);
    assertThat(new String(Base64.getDecoder().decode(KERBEROS_TICKET), StandardCharsets.UTF_8)).isEqualTo(
        new String(credentials.getKerberosTicket(), StandardCharsets.UTF_8));

    final CommonProfile profile = (CommonProfile) client.getUserProfile(credentials, context);
    assertThat(profile.getId()).isEqualTo("jose/admin@LOCALHOST");
  }
}
