package mil.dds.anet.test.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.kerb4j.client.SpnegoClient;
import com.kerb4j.server.marshall.Kerb4JException;
import com.kerb4j.server.marshall.pac.Pac;
import com.kerb4j.server.marshall.pac.PacLogonInfo;
import com.kerb4j.server.marshall.pac.PacSid;
import com.kerb4j.server.marshall.spnego.SpnegoInitToken;
import com.kerb4j.server.marshall.spnego.SpnegoKerberosMechToken;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.security.auth.kerberos.KerberosKey;
import org.apache.kerby.kerberos.kerb.KrbException;
import org.junit.Test;

public class KerberosClientTest {

  /*
  private final static byte[] KERBEROS_TICKET =
      Base64.getEncoder().encode("Test Kerberos".getBytes(StandardCharsets.UTF_8));
  */

  @Test
  public void testKerb4j() throws Kerb4JException, KrbException, IOException {
    final SpnegoClient spnegoClient =
        SpnegoClient.loginWithKeyTab("user8", "/home/jose/Downloads/tmp2/http.localhost.keytab");
    final KerberosKey[] a = spnegoClient.getKerberosKeys();

    assertThat(a.length).isGreaterThan(0);

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
