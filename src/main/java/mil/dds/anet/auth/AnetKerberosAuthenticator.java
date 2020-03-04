package mil.dds.anet.auth;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.kerb4j.client.SpnegoClient;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.basic.BasicCredentials;
import java.util.List;
import java.util.Optional;
import mil.dds.anet.AnetObjectEngine;
import mil.dds.anet.beans.Person;
import mil.dds.anet.beans.Person.PersonStatus;
import mil.dds.anet.beans.Person.Role;
import mil.dds.anet.config.AnetConfiguration;
import mil.dds.anet.database.PersonDao;

public class AnetKerberosAuthenticator implements Authenticator<BasicCredentials, Person> {

  private final PersonDao dao;
  private final Timer timerAuthenticate;

  // TODO: Move to configuration
  private String keytabFileLocation;
  private AUTHENTICATION_TYPE authenticationType;

  public AnetKerberosAuthenticator(final AnetObjectEngine engine,
      final MetricRegistry metricRegistry, AnetConfiguration config) {
    this.dao = engine.getPersonDao();
    this.timerAuthenticate =
        metricRegistry.timer(MetricRegistry.name(this.getClass(), "authenticate"));

    this.keytabFileLocation = config.getKerberosConfig().get("keytabLocation");
    this.authenticationType =
        AUTHENTICATION_TYPE.valueOf(config.getKerberosConfig().get("authenticationType"));
  }

  @Override
  public Optional<Person> authenticate(final BasicCredentials credentials)
      throws AuthenticationException {
    final Timer.Context context = timerAuthenticate.time();

    try {
      if (!authenticateCredentials(credentials)) {
        return Optional.empty();
      }

      final List<Person> p = dao.findByDomainUsername(credentials.getUsername());
      if (p.size() > 0) {
        return Optional.of(p.get(0));
      }

      // Special development mechanism to perform a 'first login'.
      Person newUser = new Person();
      newUser.setName(credentials.getUsername());
      newUser.setRole(Role.ADVISOR); // TODO
      newUser.setDomainUsername(credentials.getUsername()); // TODO
      newUser.setStatus(PersonStatus.NEW_USER);
      newUser = dao.insert(newUser);

      return Optional.of(newUser);

    } finally {
      context.stop();
    }
  }

  // Authenticaticate using Kerberos
  private boolean authenticateCredentials(final BasicCredentials credentials) {
    try {
      SpnegoClient spnegoClient;

      switch (authenticationType) {
        case KEYTAB:
          spnegoClient =
              SpnegoClient.loginWithKeyTab(credentials.getUsername(), keytabFileLocation);
          break;
        case TICKET:
          spnegoClient = SpnegoClient.loginWithUsernamePassword(credentials.getUsername(),
              credentials.getPassword());
          break;
        case CACHE:
          spnegoClient = SpnegoClient.loginWithUsernamePassword(credentials.getUsername(),
              credentials.getPassword(), true);
          break;
        case TICKET_CACHE:
          spnegoClient = SpnegoClient.loginWithTicketCache(credentials.getUsername());
          break;
        default:
          // TODO: Log incorrect configuration (?)
          return false;
      }

      spnegoClient.getKerberosKeys();
      return true;
    } catch (final RuntimeException e) { // <-- Ugly
      return false;
    }
  }

  public static enum AUTHENTICATION_TYPE {
    // Auhtenticate using a username relying on a local keytab file
    KEYTAB,
    // Auhtenticate using a username and password combination relying on a ticket
    TICKET,
    // Auhtenticate using a username and password combination relying on the cache
    CACHE,
    // Auhtenticate using a username relying on the cache
    TICKET_CACHE
  }

}
