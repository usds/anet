package mil.dds.anet.integrationtest.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import java.io.IOException;
import mil.dds.anet.auth.LdapClient;
import mil.dds.anet.config.AnetConfiguration;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.pac4j.core.credentials.UsernamePasswordCredentials;
import org.pac4j.core.exception.BadCredentialsException;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.ldap.profile.LdapProfile;
import org.pac4j.ldap.profile.service.LdapProfileService;

/**
 * These tests are bases on the test Docker image from
 * https://github.com/rroemhild/docker-test-openldap
 */
@Ignore
public class LdapClientTest {
  private static final AnetConfiguration config =
      Mockito.mock(AnetConfiguration.class, Mockito.RETURNS_MOCKS);

  @BeforeClass
  public static void init() throws JsonParseException, JsonMappingException, IOException {
    setupConfig();
  }

  private static LdapProfileService loadLdapProfileService(final AnetConfiguration config) {
    final LdapClient ldapClient = new LdapClient(config);
    final String principalAttributes = (String) config
        .getDictionaryEntry("pac4jConfig.clientsProperties.ldap.principalAttributes");
    final String usersDn =
        (String) config.getDictionaryEntry("pac4jConfig.clientsProperties.ldap.usersDn");

    return new LdapProfileService(ldapClient.getConnectionFactory(), ldapClient.getAuthenticator(),
        principalAttributes, usersDn);
  }

  private static void setupConfig() {
    final String ldapPath = "pac4jConfig.clientsProperties.ldap.";
    when(config.getDictionaryEntry(ldapPath + "url")).thenReturn("ldap://localhost:389");
    when(config.getDictionaryEntry(ldapPath + "useStartTls")).thenReturn(false);
    when(config.getDictionaryEntry(ldapPath + "useSsl")).thenReturn(false);
    when(config.getDictionaryEntry(ldapPath + "connectTimeout")).thenReturn(500);
    when(config.getDictionaryEntry(ldapPath + "responseTimeout")).thenReturn(1000);
    when(config.getDictionaryEntry(ldapPath + "dnFormat"))
        .thenReturn("cn=%s,ou=people,dc=planetexpress,dc=com");
    when(config.getDictionaryEntry(ldapPath + "usersDn")).thenReturn("dc=planetexpress,dc=com");
    when(config.getDictionaryEntry(ldapPath + "principalAttributes")).thenReturn(
        "cn,sn,description,displayName,employeeType,givenName,jpegPhoto,mail,ou,uid,userPassword");
  }

  @Test
  public void testLdapClientAdmin() {
    when(config.getDictionaryEntry("pac4jConfig.clientsProperties.ldap.dnFormat"))
        .thenReturn("cn=%s,dc=planetexpress,dc=com");

    LdapProfileService ldapProfileService = loadLdapProfileService(config);

    final UsernamePasswordCredentials credentials =
        new UsernamePasswordCredentials("admin", "GoodNewsEveryone");

    ldapProfileService.validate(credentials, null);

    assertThat(ldapProfileService.getUsersDn()).isEqualTo(
        (String) config.getDictionaryEntry("pac4jConfig.clientsProperties.ldap.usersDn"));
    assertThat(ldapProfileService.getAttributes()).isEqualTo((String) config
        .getDictionaryEntry("pac4jConfig.clientsProperties.ldap.principalAttributes"));

    final CommonProfile profile = credentials.getUserProfile();
    assertThat(profile).isNotNull();
    assertThat(profile instanceof LdapProfile).isTrue();

    final LdapProfile ldapProfile = (LdapProfile) profile;
    assertThat(ldapProfile.getId()).isEqualTo("admin");
    assertThat(ldapProfile.getAttributes().size()).isEqualTo(3);
    assertThat(ldapProfile.getAttribute("cn")).isEqualTo("admin");
    assertThat(ldapProfile.getAttribute("userPassword")).isNotNull();
    assertThat(ldapProfile.getAttribute("description")).isNotNull();
  }

  @Test
  public void testLdapClientUser() {

    LdapProfileService ldapProfileService = loadLdapProfileService(config);

    final UsernamePasswordCredentials credentials =
        new UsernamePasswordCredentials("Philip J. Fry", "fry");

    ldapProfileService.validate(credentials, null);

    assertThat(ldapProfileService.getUsersDn()).isEqualTo(
        (String) config.getDictionaryEntry("pac4jConfig.clientsProperties.ldap.usersDn"));
    assertThat(ldapProfileService.getAttributes()).isEqualTo((String) config
        .getDictionaryEntry("pac4jConfig.clientsProperties.ldap.principalAttributes"));

    final CommonProfile profile = credentials.getUserProfile();
    assertThat(profile).isNotNull();
    assertThat(profile instanceof LdapProfile).isTrue();

    final LdapProfile ldapProfile = (LdapProfile) profile;
    assertThat(ldapProfile.getId()).isEqualTo("Philip J. Fry");
    assertThat(ldapProfile.getAttributes().size()).isEqualTo(11);
    assertThat(ldapProfile.getAttribute("cn")).isEqualTo("Philip J. Fry");
    assertThat(ldapProfile.getAttribute("sn")).isEqualTo("Fry");
    assertThat(ldapProfile.getAttribute("uid")).isEqualTo("fry");
    assertThat(ldapProfile.getAttribute("mail")).isEqualTo("fry@planetexpress.com");
  }

  @Test(expected = BadCredentialsException.class)
  public void testLdapClientFailedLogin() {

    LdapProfileService ldapProfileService = loadLdapProfileService(config);

    final UsernamePasswordCredentials credentials =
        new UsernamePasswordCredentials("Philip J. Fry", "fry2");
    ldapProfileService.validate(credentials, null);
  }
}
