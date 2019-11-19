package mil.dds.anet.integrationtest.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import java.net.MalformedURLException;
import mil.dds.anet.auth.SamlClient;
import mil.dds.anet.config.AnetConfiguration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.pac4j.saml.client.SAML2Client;
import org.pac4j.saml.crypto.KeyStoreCredentialProvider;
import org.springframework.core.io.UrlResource;

public class SamlClientTest {

  private static final AnetConfiguration config =
      Mockito.mock(AnetConfiguration.class, Mockito.RETURNS_MOCKS);

  @BeforeClass
  public static void init() {
    setupConfig();
  }

  private static void setupConfig() {
    final String path = "pac4jConfig.clientsProperties.saml.";
    when(config.getDictionaryEntry(path + "keystorePassword")).thenReturn("123456");
    when(config.getDictionaryEntry(path + "privateKeyPassword")).thenReturn("123456");
    when(config.getDictionaryEntry(path + "maximumAuthenticationLifetime")).thenReturn(3600);
    when(config.getDictionaryEntry(path + "serviceProviderEntityId")).thenReturn("http://localhost:8080/simplesaml/saml2/idp/metadata.php");
    when(config.getDictionaryEntry(path + "callbackUrl")).thenReturn("http://localhost:8080/simplesaml");
  }

  @Test
  public void testx() throws MalformedURLException {
    Resource keyStore = new FileSystemResource("src/integration-test/resources/auth/samlKeystore.jks");
    SAML2Client client = new SamlClient(config, keyStore).getClient();

    client.getConfiguration().init();
    client.getConfiguration().setIdentityProviderMetadataResource(new UrlResource("http://localhost:8080/simplesaml/saml2/idp/metadata.php"));
    client.init();

    client.getIdentityProviderMetadataResolver().resolve();
    final String id = client.getIdentityProviderMetadataResolver().getEntityId();
    assertThat(id).isNotNull();


    final KeyStoreCredentialProvider p = new KeyStoreCredentialProvider(client.getConfiguration());
    assertThat(p.getKeyInfoGenerator()).isNotNull();
    assertThat(p.getCredentialResolver()).isNotNull();
    assertThat(p.getKeyInfo()).isNotNull();
    assertThat(p.getKeyInfoCredentialResolver()).isNotNull();
    assertThat(p.getCredential()).isNotNull();
    assertThat(p.getCredential().getPublicKey()).isNotNull();
    assertThat(p.getCredential().getEntityId()).isEqualTo("anet");

  }
}
