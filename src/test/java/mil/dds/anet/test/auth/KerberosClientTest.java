package mil.dds.anet.test.auth;

import static org.assertj.core.api.Assertions.assertThat;
import com.kerb4j.client.SpnegoClient;
import org.junit.Test;

/**
 * Kerberos cmd instructions:
 * 
 * Generate principal: sudo kadmin.local → add_principal <username>
 * 
 * Generate keytab file: sudo kadmin.local → ktadd -k <path_to_keytab_file> <principal>
 * 
 * Add user to Kerberos: sudo kadmin.local → add_principal <username>@<domain>
 * 
 * Add user to keytab: sudo kadmin.local → xst -norandkey -k <keytab_file> <user>@<domain>
 * 
 * Generate ticket cache: sudo kinit -f <username>@<domain>
 * 
 * Generate ticket: kninit <user>
 * 
 * Generate ticket using keytab file: sudo kinit -kt http.localhost.keytab <user>@<domain>
 * 
 * Verify keytab: klist -k <path_to_keytab_file>
 * 
 * /tmp/krb5cc_0 → Ticket cache 
 * 
 * /tmp/krb5cc_1001 → Ticket
 */
public class KerberosClientTest {

  private static final String keytabFileLocation =
      KerberosClientTest.class.getResource("/auth/test_keytab").toString();

  private static final String domain = "LOCALHOST";

  private static final String existingUserId = "user1";
  private static final String existingUserPassword = "pass1";
  private static final String existingUserWrongPassword = "pass";
  private static final String nonExistingUserId = "user3";

  @Test
  public void testCredentials() {
    final SpnegoClient spnegoClient =
        SpnegoClient.loginWithKeyTab(existingUserId, keytabFileLocation);
    assertThat(spnegoClient.getSubject()).isNotNull();
    assertThat(spnegoClient.getSubject().getPrincipals().size()).isEqualTo(1);
    assertThat(spnegoClient.getKerberosKeys().length).isEqualTo(2);
    assertThat(spnegoClient.getKerberosKeys()[0].getPrincipal()).isNotNull();
  }

  // Uses keytab file
  @Test
  public void testKerb4j_keytabFile() {
    final SpnegoClient spnegoClient =
        SpnegoClient.loginWithKeyTab(existingUserId, keytabFileLocation);
    assertThat(isUserLoggedIn(spnegoClient, existingUserId + "@" + domain)).isTrue();
  }

  // Uses keytab file
  @Test(expected = RuntimeException.class)
  public void testKerb4j_keytabFile_failed() {
    final SpnegoClient spnegoClient =
        SpnegoClient.loginWithKeyTab(nonExistingUserId, keytabFileLocation);
    isUserLoggedIn(spnegoClient, nonExistingUserId + "@" + domain);
  }

  // Uses ticket
  @Test
  public void testKerb4j_usernameAndPassword() {
    final SpnegoClient spnegoClient =
        SpnegoClient.loginWithUsernamePassword(existingUserId, existingUserPassword);
    assertThat(isUserLoggedIn(spnegoClient, existingUserId + "@" + domain)).isTrue();
  }

  // Uses username + password and ticket
  @Test(expected = RuntimeException.class)
  public void testKerb4j_usernameAndPassword_failedUsername() {
    final SpnegoClient spnegoClient =
        SpnegoClient.loginWithUsernamePassword(nonExistingUserId, existingUserWrongPassword);
    isUserLoggedIn(spnegoClient, nonExistingUserId + "@" + domain);
  }

  // Uses username + password and cache
  @Test(expected = RuntimeException.class)
  public void testKerb4j_usernameAndPassword_failedUsernameTicket() {
    final SpnegoClient spnegoClient =
        SpnegoClient.loginWithUsernamePassword(nonExistingUserId, existingUserWrongPassword, true);
    isUserLoggedIn(spnegoClient, nonExistingUserId + "@" + domain);
  }

  // Uses username + password and ticket
  @Test(expected = RuntimeException.class)
  public void testKerb4j_usernameAndPassword_failedPassword() {
    final SpnegoClient spnegoClient =
        SpnegoClient.loginWithUsernamePassword(existingUserId, existingUserWrongPassword);
    isUserLoggedIn(spnegoClient, existingUserId + "@" + domain);
  }

  // Uses ticket cache
  @Test
  public void testKerb4j_ticketCache() {
    final SpnegoClient spnegoClient = SpnegoClient.loginWithTicketCache(existingUserId);
    assertThat(isUserLoggedIn(spnegoClient, existingUserId + "@" + domain)).isTrue();
  }

  // Uses ticket cache
  @Test(expected = RuntimeException.class)
  public void testKerb4j_ticketCache_failed() {
    final SpnegoClient spnegoClient = SpnegoClient.loginWithTicketCache(nonExistingUserId);
    spnegoClient.getKerberosKeys();
  }

  // Throws RuntimeException if login fails
  private static boolean isUserLoggedIn(final SpnegoClient spnegoClient, String principalName) {
    return spnegoClient.getSubject().getPrincipals().stream()
        .filter(p -> p.getName().equals(principalName)).findFirst().isPresent();
  }
}
