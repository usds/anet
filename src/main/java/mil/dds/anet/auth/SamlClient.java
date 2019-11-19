package mil.dds.anet.auth;

import java.net.MalformedURLException;
import mil.dds.anet.config.AnetConfiguration;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.pac4j.saml.client.SAML2Client;
import org.pac4j.saml.config.SAML2Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

public final class SamlClient {

  private final SAML2Client samlClient;
  private final Resource keyStore;
  private final AnetConfiguration config;
  private final String samlConfigPath = "pac4jConfig.clientsProperties.saml.";

  public SamlClient(final AnetConfiguration config, final Resource keyStore) throws MalformedURLException {
    this.config = config;
    this.keyStore = keyStore;
    samlClient = getClient();
  }

  public SAML2Client getClient() throws MalformedURLException {

    final String keystorePassword = (String) config.getDictionaryEntry(samlConfigPath + "keystorePassword");
    final String privateKeyPassword = (String) config.getDictionaryEntry(samlConfigPath + "privateKeyPassword");
    final String serviceProviderEntityId = (String) config.getDictionaryEntry(samlConfigPath + "serviceProviderEntityId");
    final Integer maximumAuthenticationLifetime = (Integer) config.getDictionaryEntry(samlConfigPath + "maximumAuthenticationLifetime");
    final String callbackUrl = (String) config.getDictionaryEntry(samlConfigPath + "callbackUrl");

    final SAML2Configuration cfg =
        new SAML2Configuration(keyStore, keystorePassword, privateKeyPassword,
            new UrlResource(serviceProviderEntityId));

    cfg.setForceAuth(true);
    cfg.setPassive(true);

    cfg.setAuthnRequestBindingType(SAMLConstants.SAML2_REDIRECT_BINDING_URI);
    cfg.setSpLogoutResponseBindingType(SAMLConstants.SAML2_POST_BINDING_URI);

    cfg.setMaximumAuthenticationLifetime(maximumAuthenticationLifetime);
    cfg.setServiceProviderEntityId("urn:mace:saml:pac4j.org");

    final SAML2Client saml2Client = new SAML2Client(cfg);
    saml2Client.setCallbackUrl(callbackUrl);
    return saml2Client;
  }

  public SAML2Client getSamlClient() {
    return samlClient;
  }
}
