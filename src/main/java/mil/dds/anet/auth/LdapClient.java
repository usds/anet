package mil.dds.anet.auth;

import java.time.Duration;
import mil.dds.anet.config.AnetConfiguration;
import org.ldaptive.ConnectionConfig;
import org.ldaptive.ConnectionFactory;
import org.ldaptive.DefaultConnectionFactory;
import org.ldaptive.auth.Authenticator;
import org.ldaptive.auth.FormatDnResolver;
import org.ldaptive.auth.PooledBindAuthenticationHandler;
import org.ldaptive.pool.*;

public final class LdapClient {

  private final ConnectionFactory connectionFactory = new DefaultConnectionFactory();
  private final Authenticator authenticator = new Authenticator();

  // Config
  private final String url;
  private final String dnFormat;
  private final boolean useStartTls;
  private final boolean useSsl;
  private final int connectTimeout;
  private final int responseTimeout;

  public LdapClient(final AnetConfiguration config) {
    url = (String) config.getDictionaryEntry("pac4jConfig.clientsProperties.ldap.url");
    dnFormat = (String) config.getDictionaryEntry("pac4jConfig.clientsProperties.ldap.dnFormat");
    useStartTls =
        (Boolean) config.getDictionaryEntry("pac4jConfig.clientsProperties.ldap.useStartTls");
    useSsl = (Boolean) config.getDictionaryEntry("pac4jConfig.clientsProperties.ldap.useSsl");
    connectTimeout =
        (Integer) config.getDictionaryEntry("pac4jConfig.clientsProperties.ldap.connectTimeout");
    responseTimeout =
        (Integer) config.getDictionaryEntry("pac4jConfig.clientsProperties.ldap.responseTimeout");

    setup();
  }

  private void setup() {
    final FormatDnResolver dnResolver = new FormatDnResolver();
    dnResolver.setFormat(dnFormat);

    final ConnectionConfig connectionConfig = new ConnectionConfig();
    connectionConfig.setConnectTimeout(Duration.ofMillis(connectTimeout));
    connectionConfig.setResponseTimeout(Duration.ofMillis(responseTimeout));
    connectionConfig.setLdapUrl(url);
    connectionConfig.setUseStartTLS(useStartTls);
    connectionConfig.setUseSSL(useSsl);

    ((DefaultConnectionFactory) connectionFactory).setConnectionConfig(connectionConfig);

    final PoolConfig poolConfig = new PoolConfig();
    poolConfig.setMinPoolSize(1);
    poolConfig.setMaxPoolSize(2);
    poolConfig.setValidateOnCheckOut(true);
    poolConfig.setValidateOnCheckIn(true);
    poolConfig.setValidatePeriodically(false);

    final SearchValidator searchValidator = new SearchValidator();

    final IdlePruneStrategy pruneStrategy = new IdlePruneStrategy();

    final BlockingConnectionPool connectionPool = new BlockingConnectionPool();
    connectionPool.setPoolConfig(poolConfig);
    connectionPool.setBlockWaitTime(Duration.ofSeconds(1));
    connectionPool.setValidator(searchValidator);
    connectionPool.setPruneStrategy(pruneStrategy);
    connectionPool.setConnectionFactory((DefaultConnectionFactory) connectionFactory);
    connectionPool.initialize();

    final PooledConnectionFactory pooledConnectionFactory = new PooledConnectionFactory();
    pooledConnectionFactory.setConnectionPool(connectionPool);

    final PooledBindAuthenticationHandler handler = new PooledBindAuthenticationHandler();
    handler.setConnectionFactory(pooledConnectionFactory);

    authenticator.setDnResolver(dnResolver);
    authenticator.setAuthenticationHandler(handler);
  }

  public ConnectionFactory getConnectionFactory() {
    return connectionFactory;
  }

  public Authenticator getAuthenticator() {
    return authenticator;
  }
}
