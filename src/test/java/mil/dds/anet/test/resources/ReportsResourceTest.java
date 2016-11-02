package mil.dds.anet.test.resources;

import static org.assertj.core.api.Assertions.assertThat;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

import org.junit.Test;

import com.google.common.collect.Lists;

import io.dropwizard.client.JerseyClientBuilder;
import mil.dds.anet.beans.AdvisorOrganization;
import mil.dds.anet.beans.ApprovalStep;
import mil.dds.anet.beans.Billet;
import mil.dds.anet.beans.Group;
import mil.dds.anet.beans.Person;
import mil.dds.anet.beans.Poam;
import mil.dds.anet.beans.Report;
import mil.dds.anet.beans.Report.ReportState;
import mil.dds.anet.beans.geo.LatLng;
import mil.dds.anet.beans.geo.Location;
import mil.dds.anet.test.beans.AdvisorOrganizationTest;

public class ReportsResourceTest extends AbstractResourceTest {

	public ReportsResourceTest() { 
		if (client == null) { 
			client = new JerseyClientBuilder(RULE.getEnvironment()).build("reports test client");
		}
	}
	
	@Test
	public void createReport() {
		//Create a report writer
		Person author = getJackJackson();
		
		//Create a principal for the report
		Person principal = getSteveSteveson();
		
		//Create an Advising Organization for the report writer
		AdvisorOrganization ao = httpQuery("/advisorOrganizations/new", author)
				.post(Entity.json(AdvisorOrganizationTest.getTestAO()), AdvisorOrganization.class);
		
		//Create leadership people in the AO who can approve this report
		Person approver1 = getRogerRogwell();
		
		//Create a billet for the author
		Billet authorBillet = new Billet();
		authorBillet.setName("A report writer");
		authorBillet.setAdvisorOrganization(ao);
		authorBillet = httpQuery("/billets/new", author).post(Entity.json(authorBillet), Billet.class);
		assertThat(authorBillet.getId()).isNotNull();
		
		//Set this author in this billet
		Response resp = httpQuery(String.format("/billets/%d/advisor", authorBillet.getId()), author).post(Entity.json(author));
		assertThat(resp.getStatus()).isEqualTo(200);
		
		//Create Approval workflow for Advising Organization
		Group approvingGroup = httpQuery("/groups/new", author)
				.post(Entity.json(Group.create("Test Group of approvers")), Group.class);
		resp = httpQuery(String.format("/groups/%d/addMember?personId=%d", approvingGroup.getId(), approver1.getId()), author)
				.get();
		assertThat(resp.getStatus()).isEqualTo(200);
		
		ApprovalStep approval = httpQuery("/approvalSteps/new", author)
				.post(Entity.json(ApprovalStep.create(null, approvingGroup.getId(), null, ao.getId())), ApprovalStep.class);
		
		//TODO: Create a POAM structure for the AO
//		fail("No way to assign a POAM to an AO");
		Poam top = httpQuery("/poams/new", author)
				.post(Entity.json(Poam.create("test-1", "Test Top Poam", "TOP")), Poam.class);
		Poam action = httpQuery("/poams/new", author)
				.post(Entity.json(Poam.create("test-1-1", "Test Poam Action", "Action", top)), Poam.class);
				
		
		//Create a Location that this Report was written at
		Location loc = httpQuery("/locations/new", author)
				.post(Entity.json(Location.create("The Boat Dock", new LatLng(1.23,4.56))), Location.class);
		
		//Write a Report
		Report r = new Report();
		r.setAuthor(author);
		r.setPrincipals(Lists.newArrayList(principal));
		r.setPoams(Lists.newArrayList(action));
		r.setLocation(loc);
		r.setIntent("A testing report to test that reporting reports");
		r.setReportText("Report Text goes here, asdfjk");
		r.setNextSteps("This is the next steps on a report");
		Report created = httpQuery("/reports/new", author)
				.post(Entity.json(r), Report.class);
		assertThat(created.getId()).isNotNull();
		assertThat(created.getState()).isEqualTo(ReportState.DRAFT);
		
		//Have the author submit the report
		resp = httpQuery(String.format("/reports/%d/submit", created.getId()), author).get();
		assertThat(resp.getStatus()).isEqualTo(200);
		
		Report returned = httpQuery(String.format("/reports/%d", created.getId()), author).get(Report.class);
		assertThat(returned.getState()).isEqualTo(ReportState.PENDING_APPROVAL);
		assertThat(returned.getApprovalStepId()).isEqualTo(approval.getId());
		
		//TODO: verify the location on this report
		//TODO: verify the principals on this report
		//TODO: verify the poams on this report
		
		//TODO: Check on Report status for who needs to approve
		
		//Approve the report
		resp = httpQuery(String.format("/reports/%d/approve", created.getId()), approver1).get();
		assertThat(resp.getStatus()).isEqualTo(200);
		
		//Check on Report status to verify it got moved forward
		returned = httpQuery(String.format("/reports/%d", created.getId()), author).get(Report.class);
		assertThat(returned.getState()).isEqualTo(ReportState.RELEASED);
		assertThat(returned.getApprovalStepId()).isNull();
		
		//Post a comment on the report because it's awesome
		
		
		//Search for this report by Author
		//Search for this report by Advisor
		//Search for this report by Location
		//Search for this report by Date
		//Search for this report by keyword
		//Search for this report by POAM (top level and bottom level)
		
	}
}
