package com.danielagapov.spawn.RepositoryTests;

import com.danielagapov.spawn.analytics.api.dto.*;
import com.danielagapov.spawn.analytics.internal.domain.ReportedContent;
import com.danielagapov.spawn.analytics.internal.repositories.IReportedContentRepository;
import com.danielagapov.spawn.analytics.internal.services.ReportContentService;
import com.danielagapov.spawn.activity.api.IActivityService;
import com.danielagapov.spawn.chat.internal.services.IChatMessageService;
import com.danielagapov.spawn.shared.config.JacksonConfig;
import com.danielagapov.spawn.shared.exceptions.Logger.Logger;
import com.danielagapov.spawn.shared.util.*;
import com.danielagapov.spawn.user.api.ReportController;
import com.danielagapov.spawn.user.internal.domain.User;
import com.danielagapov.spawn.user.internal.repositories.IUserRepository;
import com.danielagapov.spawn.user.internal.services.IUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DataJpaTest(showSql=false)
@AutoConfigureTestDatabase(replace=AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Execution(ExecutionMode.SAME_THREAD)
@Transactional(propagation=Propagation.NOT_SUPPORTED)
@Import({ReportContentService.class,ReportController.class,JacksonConfig.class})
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ReportResponseSerializationTests {
    @Autowired IUserRepository users;
    @Autowired IReportedContentRepository reports;
    @Autowired ReportController controller;
    @Autowired ObjectMapper json;
    @MockBean IUserService userLookup;
    @MockBean IActivityService activities;
    @MockBean IChatMessageService chats;
    @MockBean Logger logger;
    MockMvc mvc;
    User reporter,owner;

    @BeforeEach void setup() {
        reporter=user("local_reporter");owner=user("local_owner");
        when(userLookup.getUserEntityById(reporter.getId())).thenReturn(reporter);
        when(userLookup.getUserEntityById(owner.getId())).thenReturn(owner);
        mvc=MockMvcBuilders.standaloneSetup(controller).setMessageConverters(new MappingJackson2HttpMessageConverter(json)).build();
    }

    @Test void postReturnsCreatedAndOneCommittedReport() throws Exception {
        CreateReportedContentDTO request=new CreateReportedContentDTO(reporter.getId(),owner.getId(),EntityType.User,ReportType.HARASSMENT,"Local QA only; no misconduct");
        org.springframework.mock.web.MockHttpServletResponse response;
        try {
            response=mvc.perform(post("/api/v1/reports").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(request)))
                    .andExpect(status().isCreated()).andReturn().getResponse();
        } catch(Exception serializationFailure) {
            assertEquals(1,reports.count(),"The failed HTTP response still committed the report");
            System.out.println("QA_REPORT_RESPONSE beforeConversionFailed=true committedReports="+reports.count());
            throw serializationFailure;
        }
        assertEquals(1,reports.count());
        var body=json.readTree(response.getContentAsString());
        assertEquals(reporter.getId().toString(),body.path("reporter").path("id").asText());
        assertEquals(owner.getId().toString(),body.path("reportedUser").path("id").asText());
        assertEquals("PENDING",body.path("resolution").asText());
        assertEquals(owner.getId(),reports.findAll().get(0).getContentId());
        assertPublicUser(body.path("reporter"));assertPublicUser(body.path("reportedUser"));
    }

    @Test void statusUpdateReturnsSerializableResponse() throws Exception {
        ReportedContent report=report();
        var response=mvc.perform(put("/api/v1/reports/{id}",report.getId()).param("resolution","WARN"))
                .andExpect(status().isOk()).andReturn().getResponse();
        assertEquals("WARN",json.readTree(response.getContentAsString()).path("resolution").asText());
        assertEquals(ResolutionStatus.WARN,reports.findById(report.getId()).orElseThrow().getResolution());
        assertEquals(1,reports.count());
    }

    @Test void nullableReporterAndListRemainSerializable() throws Exception {
        ReportedContent report=report();report.setReporter(null);reports.saveAndFlush(report);
        String encoded=json.writeValueAsString(ReportedContentDTO.fromEntityList(List.of(report)));
        var result=json.readTree(encoded).get(0);
        assertTrue(result.path("reporter").isNull());
        assertEquals(owner.getId().toString(),result.path("reportedUser").path("id").asText());
        assertPublicUser(result.path("reportedUser"));
    }

    @Test void dtoKeepsPublicUserFieldsAndReportRoundTrip() throws Exception {
        ReportedContent saved=report();ReportedContentDTO dto=ReportedContentDTO.fromEntity(saved);
        var data=json.readTree(json.writeValueAsString(dto));
        assertEquals(reporter.getUsername(),data.path("reporter").path("username").asText());
        assertEquals(reporter.getEmail(),data.path("reporter").path("email").asText());
        assertEquals(reporter.getProfilePictureUrlString(),data.path("reporter").path("profilePicture").asText());
        assertPublicUser(data.path("reporter"));
        ReportedContent restored=dto.toEntity();
        assertEquals(saved.getId(),restored.getId());assertEquals(reporter.getId(),restored.getReporter().getId());
        assertEquals(owner.getId(),restored.getContentOwner().getId());assertEquals(saved.getReportType(),restored.getReportType());
        dto.setReporter(null);assertNull(dto.toEntity().getReporter());
    }

    private void assertPublicUser(com.fasterxml.jackson.databind.JsonNode user) {
        for(String field:List.of("password","phoneNumber","status","notificationPreferences","optionalUsername","optionalEmail","optionalPassword"))
            assertFalse(user.has(field),field);
    }
    private ReportedContent report() {
        ReportedContent report=new ReportedContent();report.setReporter(reporter);report.setContentOwner(owner);
        report.setContentId(owner.getId());report.setContentType(EntityType.User);report.setTimeReported(OffsetDateTime.parse("2026-11-23T12:00:00Z"));
        report.setResolution(ResolutionStatus.PENDING);report.setReportType(ReportType.HARASSMENT);report.setDescription("Local QA only; no misconduct");return reports.saveAndFlush(report);
    }
    private User user(String label) {
        User user=new User();user.setUsername(label);user.setName(label);user.setEmail(label+"@example.com");user.setStatus(UserStatus.ACTIVE);
        user.setPassword("synthetic-do-not-serialize");user.setPhoneNumber(label+"-phone");user.setProfilePictureUrlString("https://example.com/"+label+".png");return users.saveAndFlush(user);
    }
}
