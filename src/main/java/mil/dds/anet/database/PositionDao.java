package mil.dds.anet.database;

import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.skife.jdbi.v2.GeneratedKeys;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Query;

import mil.dds.anet.beans.Organization;
import mil.dds.anet.beans.Person;
import mil.dds.anet.beans.Position;
import mil.dds.anet.database.mappers.PersonMapper;
import mil.dds.anet.database.mappers.PositionMapper;
import mil.dds.anet.utils.DaoUtils;

public class PositionDao implements IAnetDao<Position> {

	Handle dbHandle;
	
	public PositionDao(Handle h) { 
		this.dbHandle = h;
	}
	
	public List<Position> getAll(int pageNum, int pageSize) {
		Query<Position> query = dbHandle.createQuery("SELECT * from positions ORDER BY createdAt ASC LIMIT :limit OFFSET :offset")
			.bind("limit", pageSize)
			.bind("offset", pageSize * pageNum)
			.map(new PositionMapper());
		return query.list();
	}
	
	public Position insert(Position p) { 
		p.setCreatedAt(DateTime.now());
		p.setUpdatedAt(DateTime.now());
		GeneratedKeys<Map<String,Object>> keys = dbHandle.createStatement(
				"INSERT INTO positions (name, code, organizationId, createdAt, updatedAt) " +
				"VALUES (:name, :code, :organizationId, :createdAt, :updatedAt)")
			.bindFromProperties(p)
			.bind("organizationId", DaoUtils.getId(p.getOrganization()))
			.executeAndReturnGeneratedKeys();
		p.setId((Integer) (keys.first().get("last_insert_rowid()")));
		
		//TODO: this should be in a transaction.
		dbHandle.createStatement("INSERT INTO peoplePositions (positionId, personId, createdAt) VALUES (:positionId, :personId, :createdAt)")
			.bind("positionId", p.getId())
			.bind("personId", (Integer) null)
			.bind("createdAt", p.getCreatedAt())
			.execute();
		
		return p;
	}
	
	public Position getById(int id) { 
		Query<Position> query = dbHandle.createQuery("SELECT * FROM positions WHERE id = :id")
			.bind("id", id)
			.map(new PositionMapper());
		List<Position> positions = query.list();
		if (positions.size() == 0) { return null; } 
		return positions.get(0);
	}
	
	/*
	 * @return: number of rows updated. 
	 */
	public int update(Position p) { 
		p.setUpdatedAt(DateTime.now());
		return dbHandle.createStatement("UPDATE positions SET name = :name, code = :code, organizationId = :organizationId, updatedAt = :updatedAt WHERE id = :id")
			.bindFromProperties(p)
			.bind("organizationId", DaoUtils.getId(p.getOrganization()))
			.execute();
	}
	
	public void setPersonInPosition(Person p, Position b) {
		//TODO: this should be in a transaction. 
		DateTime now = DateTime.now();
		//If this person is in a position already, we need to remove them. 
		List<Map<String,Object>> positions = dbHandle.createQuery("SELECT positionId FROM peoplePositions where personId = :personId ORDER BY createdAt DESC LIMIT 1")
			.bind("personId", p.getId())
			.list();
		if (positions.size() > 0) { 
			Integer positionId = (Integer) positions.get(0).get("positionId");
			if (positionId != null) { 
				dbHandle.createStatement("INSERT INTO peoplePositions (positionId, personId, createdAt) VALUES (:positionId, null, :createdAt)")
					.bind("positionId", positionId)
					.bind("createdAt", now)
					.execute();
			}
		}

		//Whomever was previously in this position, need to insert a record of them being removed. 
		List<Map<String,Object>> persons = dbHandle.createQuery("SELECT personId from peoplePositions WHERE positionId = :positionId ORDER BY createdAt DESC LIMIT 1")
				.bind("positionId", b.getId())
				.list();
		if (persons.size() > 0) {
			Integer personId = (Integer) persons.get(0).get("personId");
			if (personId != null) { 
				dbHandle.createStatement("INSERT INTO peoplePositions (positionId, personId, createdAt) VALUES (null, :personId, :createdAt)")
					.bind("personId", personId)
					.bind("createdAt", now)
					.execute();
			}
		}
			
		dbHandle.createStatement("INSERT INTO peoplePositions (positionId, personId, createdAt) " +
				"VALUES (:positionId, :personId, :createdAt)")
			.bind("positionId", b.getId())
			.bind("personId", p.getId())
			.bind("createdAt", now)
			.execute();
	}
	
	public void removePersonFromPosition(Position b) {
		DateTime now = DateTime.now();
		dbHandle.createStatement("INSERT INTO peoplePositions (positionId, personId, createdAt) " + 
			"VALUES(null, " +
				"(SELECT personId FROM peoplePositions WHERE positionId = :positionId ORDER BY createdAt DESC LIMIT 1), " +
			":createdAt)")
			.bind("positionId", b.getId())
			.bind("createdAt", now)
			.execute();
	
		dbHandle.createStatement("INSERT INTO peoplePositions (positionId, personId, createdAt) " + 
				"VALUES (:positionId, null, :createdAt)")
			.bind("positionId", b.getId())
			.bind("createdAt", now)
			.execute();
	}
	
	public Person getPersonInPositionNow(Position b) { 
		return getPersonInPosition(b, DateTime.now());
	}
	
	public Person getPersonInPosition(Position b, DateTime dtg) { 
		Query<Person> query = dbHandle.createQuery("SELECT people.* FROM peoplePositions " +
				" LEFT JOIN people ON people.id = peoplePositions.personId " +
				"WHERE peoplePositions.positionId = :positionId " +
				"AND peoplePositions.createdAt < :dtg " + 
				"ORDER BY peoplePositions.createdAt DESC LIMIT 1")
			.bind("positionId", b.getId())
			.bind("dtg", dtg)
			.map(new PersonMapper());
		List<Person> results = query.list();
		if (results.size() == 0 ) { return null; }
		return results.get(0);
	}

	public Position getPositionForPerson(Person p) {
		List<Position> positions = dbHandle.createQuery("SELECT positions.* from peoplePositions " +
				"LEFT JOIN positions ON peoplePositions.positionId = positions.id " +
				"WHERE peoplePositions.personId = :personId " +
				"ORDER BY peoplePositions.createdAt DESC LIMIT 1")
			.bind("personId", p.getId())
			.map(new PositionMapper())
			.list();
		if (positions.size() == 0) { return null; } 
		return positions.get(0);		
	}
	
	public List<Position> getAllPositions(int pageNum, int pageSize) {
		Query<Position> query = dbHandle.createQuery("SELECT * from positions ORDER BY createdAt ASC LIMIT :limit OFFSET :offset")
				.bind("limit", pageSize)
				.bind("offset", pageSize * pageNum)
				.map(new PositionMapper());
			return query.list();
	}
	

	public List<Position> getAssociatedPositions(Position p) {
		Query<Position> query = dbHandle.createQuery("SELECT positions.* FROM positions WHERE id IN "
				+ "(SELECT positionId_a FROM positionRelationships WHERE positionId_b = :positionId AND deleted = :deleted) "
				+ "OR id IN (SELECT positionId_b FROM positionRelationships WHERE positionId_a = :positionId AND deleted = :deleted)")
			.bind("positionId", p.getId())
			.bind("deleted", false)
			.map(new PositionMapper());
		return query.list();
	}

	public void associatePosition(Position a, Position b) {
		Integer idOne = Math.min(a.getId(), b.getId());
		Integer idTwo = Math.max(a.getId(), b.getId());
		dbHandle.createStatement("INSERT INTO positionRelationships (positionId_a, positionId_b, createdAt, updatedAt, deleted) " + 
				"VALUES (:positionId_a, :positionId_b, :createdAt, :updatedAt, :deleted)")
			.bind("positionId_a", idOne)
			.bind("positionId_b", idTwo)
			.bind("createdAt", DateTime.now())
			.bind("updatedAt", DateTime.now())
			.bind("deleted", false)
			.execute();
	}

	public int deletePositionAssociation(Position a, Position b) {
		Integer idOne = Math.min(a.getId(), b.getId());
		Integer idTwo = Math.max(a.getId(), b.getId());
		return dbHandle.createStatement("UPDATE positionRelationships SET deleted = :deleted, updatedAt = :updatedAt " + 
				"WHERE positionId_a = :positionId_a AND positionId_b = :positionId_b")
			.bind("deleted", true)
			.bind("positionId_a", idOne)
			.bind("positionId_b", idTwo)
			.bind("updatedAt", DateTime.now())
			.execute();
		
	}

	public List<Position> getEmptyPositions() {
		return dbHandle.createQuery("SELECT positions.* FROM positions INNER JOIN " + 
				"(SELECT positionId, personId, MAX(createdAt) FROM peoplePositions GROUP BY positionId) emptyPositions " +
				"ON positions.id = emptyPositions.positionId WHERE emptypositions.personId is null")
			.map(new PositionMapper())
			.list();
	}

	public List<Position> getByOrganization(Organization organization) {
		return dbHandle.createQuery("SELECT * from positions WHERE organizationId = :aoId")
			.bind("aoId", organization.getId())
			.map(new PositionMapper())
			.list();
	}

}
