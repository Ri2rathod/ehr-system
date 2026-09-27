package com.example.ehrsystem.modules.laborder.integration;

import com.example.ehrsystem.modules.laborder.entity.LabOrder;
import com.example.ehrsystem.modules.laborder.repository.LabOrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LabIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LabOrderRepository labOrderRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private String adminToken;
    private String patientToken;
    private UUID patientUuid;
    private UUID doctorUuid;
    private LocalDate monday;

    private UUID encounterAUuid;
    private UUID encounterBUuid;
    private UUID encounterCUuid;
    private UUID encounterDUuid;

    private String runCode;

    // Lab tests
    private UUID testA; // NUMERIC hemoglobin, referenced -> delete blocked
    private UUID testB; // NUMERIC glucose
    private UUID testC; // QUALITATIVE
    private UUID testD; // TEXT
    private UUID testE; // CODED
    private UUID testF; // deactivated -> cannot be ordered
    private UUID testG; // added then item-deleted -> unreferenced
    private UUID testH; // never referenced -> delete succeeds
    private String codeA;
    private String codeF;
    private String codeH;

    // Orders
    private UUID order1; // encounter A: main workflow, ends COMPLETED
    private UUID order2; // encounter B: empty draft, ends CANCELLED
    private UUID order3; // encounter B: ends COMPLETED (guards)
    private UUID order4; // encounter A: draft guards, soft deleted
    private UUID orderC; // encounter C: survives encounter completion (lab carve-out)

    private UUID itemA;
    private UUID itemB;
    private UUID itemC;
    private UUID itemD;
    private UUID itemE;
    private UUID item3B; // order3's glucose item
    private UUID itemCItem; // orderC's hemoglobin item

    // Specimens
    private UUID spec1; // RECEIVED
    private UUID spec2; // REJECTED
    private UUID spec3; // CANCELLED
    private UUID specC; // on orderC

    // Results
    private UUID result1; // hemoglobin -> FINAL -> CORRECTED
    private UUID result2; // glucose normal -> CANCELLED
    private UUID result3; // explicit CRITICAL_HIGH
    private UUID result4; // qualitative -> FINAL (cancel rejected)
    private UUID result5; // text
    private UUID result6; // coded

    private String authHeader() {
        return "Bearer " + adminToken;
    }

    private String getAdminToken() throws Exception {
        var loginPayload = Map.of("email", "admin@ehr.local", "password", "Admin@123");
        var result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginPayload)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.parse(result.getResponse().getContentAsString()).read("$.accessToken");
    }

    private String registerAndLogin(String suffix) throws Exception {
        var registerPayload = Map.of(
                "firstName", "LabUser",
                "lastName", "Test" + suffix,
                "email", "lab.user." + suffix + "@example.com",
                "password", "Secret123!",
                "username", "labuser" + suffix
        );
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerPayload)))
                .andExpect(status().isCreated());

        var loginPayload = Map.of(
                "email", "lab.user." + suffix + "@example.com",
                "password", "Secret123!");
        var result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginPayload)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.parse(result.getResponse().getContentAsString()).read("$.accessToken");
    }

    private LocalDate nextDayOfWeek(DayOfWeek dayOfWeek) {
        LocalDate date = LocalDate.now().plusDays(1);
        while (date.getDayOfWeek() != dayOfWeek) {
            date = date.plusDays(1);
        }
        return date;
    }

    private String createAppointment(LocalDateTime start, String visitType) throws Exception {
        var payload = Map.of(
                "patientUuid", patientUuid.toString(),
                "doctorUuid", doctorUuid.toString(),
                "startTime", start.toString(),
                "endTime", start.plusMinutes(30).toString(),
                "visitType", visitType,
                "reasonForVisit", "Lab integration test"
        );
        var result = mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.parse(result.getResponse().getContentAsString()).read("$.uuid");
    }

    private void transitionAppointment(String appointmentUuid, String status) throws Exception {
        mockMvc.perform(put("/api/v1/appointments/" + appointmentUuid + "/status")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", status))))
                .andExpect(status().isOk());
    }

    private String createEncounter(String appointmentUuid, String chiefComplaint) throws Exception {
        var payload = new HashMap<String, Object>();
        payload.put("appointmentUuid", appointmentUuid);
        if (chiefComplaint != null) payload.put("chiefComplaint", chiefComplaint);

        var result = mockMvc.perform(post("/api/v1/encounters")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.parse(result.getResponse().getContentAsString()).read("$.uuid");
    }

    private String createCheckedInEncounter(LocalDateTime start, String chiefComplaint) throws Exception {
        String appointmentUuid = createAppointment(start, "CONSULTATION");
        transitionAppointment(appointmentUuid, "CONFIRMED");
        transitionAppointment(appointmentUuid, "CHECKED_IN");
        return createEncounter(appointmentUuid, chiefComplaint);
    }

    private Map<String, Object> labTestPayload(String code, String name, String resultType) {
        var payload = new HashMap<String, Object>();
        payload.put("code", code);
        payload.put("codeSystem", "LOCAL");
        payload.put("name", name);
        payload.put("resultType", resultType);
        return payload;
    }

    private String createLabTest(Map<String, Object> payload) throws Exception {
        var result = mockMvc.perform(post("/api/v1/lab-tests")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.parse(result.getResponse().getContentAsString()).read("$.uuid");
    }

    private Map<String, Object> orderPayload(List<UUID> tests, String priority, String instructions) {
        var payload = new HashMap<String, Object>();
        if (priority != null) payload.put("priority", priority);
        if (instructions != null) payload.put("instructions", instructions);
        var items = new ArrayList<Map<String, Object>>();
        if (tests != null) {
            for (UUID test : tests) {
                items.add(Map.of("labTestUuid", test.toString()));
            }
        }
        payload.put("items", items);
        return payload;
    }

    private UUID createOrder(Object encounterUuid, List<UUID> tests) throws Exception {
        var result = mockMvc.perform(post("/api/v1/encounters/" + encounterUuid + "/lab-orders")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderPayload(tests, null, null))))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(
                JsonPath.parse(result.getResponse().getContentAsString()).read("$.uuid"));
    }

    private UUID addItem(Object encounterUuid, Object orderUuid, UUID test) throws Exception {
        var result = mockMvc.perform(post("/api/v1/encounters/" + encounterUuid
                        + "/lab-orders/" + orderUuid + "/items")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("labTestUuid", test.toString()))))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(
                JsonPath.parse(result.getResponse().getContentAsString()).read("$.uuid"));
    }

    private String placeOrder(Object encounterUuid, Object orderUuid) throws Exception {
        var result = mockMvc.perform(post("/api/v1/encounters/" + encounterUuid
                        + "/lab-orders/" + orderUuid + "/order")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getContentAsString();
    }

    private String startOrder(Object encounterUuid, Object orderUuid) throws Exception {
        var result = mockMvc.perform(post("/api/v1/encounters/" + encounterUuid
                        + "/lab-orders/" + orderUuid + "/start")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getContentAsString();
    }

    private String completeOrder(Object encounterUuid, Object orderUuid) throws Exception {
        var result = mockMvc.perform(post("/api/v1/encounters/" + encounterUuid
                        + "/lab-orders/" + orderUuid + "/complete")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getContentAsString();
    }

    private UUID createSpecimen(Object orderUuid, String type) throws Exception {
        var result = mockMvc.perform(post("/api/v1/lab-orders/" + orderUuid + "/specimens")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("specimenType", type))))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(
                JsonPath.parse(result.getResponse().getContentAsString()).read("$.uuid"));
    }

    private void collectSpecimen(Object orderUuid, Object specimenUuid) throws Exception {
        mockMvc.perform(post("/api/v1/lab-orders/" + orderUuid
                        + "/specimens/" + specimenUuid + "/collect")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COLLECTED"));
    }

    private void receiveSpecimen(Object orderUuid, Object specimenUuid) throws Exception {
        mockMvc.perform(post("/api/v1/lab-orders/" + orderUuid
                        + "/specimens/" + specimenUuid + "/receive")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECEIVED"));
    }

    private Map<String, Object> value(UUID test, Object numeric, String text, String code) {
        var v = new HashMap<String, Object>();
        v.put("labTestUuid", test.toString());
        if (numeric != null) v.put("valueNumeric", numeric);
        if (text != null) v.put("valueText", text);
        if (code != null) v.put("valueCode", code);
        return v;
    }

    private UUID createResult(Object orderUuid, UUID item, UUID specimen,
                                List<Map<String, Object>> values) throws Exception {
        var payload = new HashMap<String, Object>();
        payload.put("labOrderItemUuid", item.toString());
        payload.put("specimenUuid", specimen.toString());
        payload.put("values", values);
        var result = mockMvc.perform(post("/api/v1/lab-orders/" + orderUuid + "/results")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(
                JsonPath.parse(result.getResponse().getContentAsString()).read("$.uuid"));
    }

    @BeforeAll
    void setupAll() throws Exception {
        adminToken = getAdminToken();
        long suffix = System.currentTimeMillis();
        runCode = Long.toString(suffix);
        patientToken = registerAndLogin(runCode);

        var patientPayload = Map.of(
                "firstName", "LabPatient",
                "lastName", "Test",
                "gender", "MALE",
                "email", "lab.patient." + suffix + "@test.com"
        );
        var patientResult = mockMvc.perform(post("/api/v1/patients")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patientPayload)))
                .andExpect(status().isCreated())
                .andReturn();
        patientUuid = UUID.fromString(JsonPath.parse(patientResult.getResponse().getContentAsString()).read("$.uuid"));

        var doctorPayload = Map.of(
                "firstName", "LabDoctor",
                "lastName", "Test",
                "email", "lab.doctor." + suffix + "@test.com",
                "specialization", "INTERNAL_MEDICINE"
        );
        var doctorResult = mockMvc.perform(post("/api/v1/doctors")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(doctorPayload)))
                .andExpect(status().isCreated())
                .andReturn();
        doctorUuid = UUID.fromString(JsonPath.parse(doctorResult.getResponse().getContentAsString()).read("$.uuid"));

        for (DayOfWeek day : DayOfWeek.values()) {
            if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) continue;
            for (var range : new String[][]{
                    {"WORK", "09:00:00", "13:00:00"},
                    {"BREAK", "13:00:00", "14:00:00"},
                    {"WORK", "14:00:00", "18:00:00"}}) {
                var payload = Map.of(
                        "dayOfWeek", day.name(),
                        "scheduleType", range[0],
                        "startTime", range[1],
                        "endTime", range[2]
                );
                mockMvc.perform(post("/api/v1/doctors/" + doctorUuid + "/availability")
                                .header("Authorization", authHeader())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(payload)))
                        .andExpect(status().isCreated());
            }
        }

        monday = nextDayOfWeek(DayOfWeek.MONDAY);
        encounterAUuid = UUID.fromString(createCheckedInEncounter(monday.atTime(9, 0), "Lab panel review"));
        encounterBUuid = UUID.fromString(createCheckedInEncounter(monday.atTime(10, 0), "Follow-up visit"));
        encounterCUuid = UUID.fromString(createCheckedInEncounter(monday.atTime(11, 0), "Pre-op labs"));
        encounterDUuid = UUID.fromString(createCheckedInEncounter(monday.atTime(12, 0), "Routine visit"));
    }

    // =============================================================
    // Lab test catalog
    // =============================================================

    @Test
    @Order(1)
    void createLabTest_success() throws Exception {
        codeA = "HB-" + runCode;
        var payload = labTestPayload(codeA, "Hemoglobin", "NUMERIC");
        payload.put("shortName", "HGB");
        payload.put("category", "HEMATOLOGY");
        payload.put("specimenType", "BLOOD");
        payload.put("unit", "g/dL");
        payload.put("defaultReferenceLow", 12);
        payload.put("defaultReferenceHigh", 16);

        var result = mockMvc.perform(post("/api/v1/lab-tests")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(codeA))
                .andExpect(jsonPath("$.name").value("Hemoglobin"))
                .andExpect(jsonPath("$.resultType").value("NUMERIC"))
                .andExpect(jsonPath("$.unit").value("g/dL"))
                .andExpect(jsonPath("$.specimenType").value("BLOOD"))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.version").value(0))
                .andReturn();
        testA = UUID.fromString(JsonPath.parse(result.getResponse().getContentAsString()).read("$.uuid"));
    }

    @Test
    @Order(2)
    void createLabTest_duplicateCode_conflict() throws Exception {
        var payload = labTestPayload(codeA, "Hemoglobin Duplicate", "NUMERIC");

        mockMvc.perform(post("/api/v1/lab-tests")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("A lab test with this code already exists in the catalog."));
    }

    @Test
    @Order(3)
    void createLabTest_missingResultType_badRequest() throws Exception {
        var payload = Map.of("code", "NORESTYPE-" + runCode, "name", "No Result Type");

        mockMvc.perform(post("/api/v1/lab-tests")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    @Order(4)
    void createLabTest_invalidEnum_badRequest() throws Exception {
        var payload = Map.of("code", "BADTYPE-" + runCode, "name", "Bad Type",
                "resultType", "BOGUS_TYPE");

        mockMvc.perform(post("/api/v1/lab-tests")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(5)
    void createLabTest_invalidReferenceRange_badRequest() throws Exception {
        var payload = labTestPayload("RANGE-" + runCode, "Bad Range", "NUMERIC");
        payload.put("defaultReferenceLow", 10);
        payload.put("defaultReferenceHigh", 5);

        mockMvc.perform(post("/api/v1/lab-tests")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Default reference low cannot exceed default reference high"));
    }

    @Test
    @Order(6)
    void createLabTests_remaining_success() throws Exception {
        var glucose = labTestPayload("GLU-" + runCode, "Glucose", "NUMERIC");
        glucose.put("unit", "mg/dL");
        glucose.put("defaultReferenceLow", 70);
        glucose.put("defaultReferenceHigh", 99);
        glucose.put("specimenType", "BLOOD");
        testB = UUID.fromString(createLabTest(glucose));

        var crp = labTestPayload("CRPQ-" + runCode, "CRP Qualitative", "QUALITATIVE");
        crp.put("defaultReferenceText", "Negative");
        crp.put("specimenType", "BLOOD");
        testC = UUID.fromString(createLabTest(crp));

        var culture = labTestPayload("CULT-" + runCode, "Urine Culture", "TEXT");
        culture.put("specimenType", "URINE");
        testD = UUID.fromString(createLabTest(culture));

        var drugs = labTestPayload("UDS-" + runCode, "Urine Drug Screen", "CODED");
        drugs.put("specimenType", "URINE");
        testE = UUID.fromString(createLabTest(drugs));

        codeF = "TSH-" + runCode;
        testF = UUID.fromString(createLabTest(
                labTestPayload(codeF, "TSH", "NUMERIC")));

        testG = UUID.fromString(createLabTest(
                labTestPayload("FERR-" + runCode, "Ferritin", "NUMERIC")));

        codeH = "VITD-" + runCode;
        testH = UUID.fromString(createLabTest(
                labTestPayload(codeH, "Vitamin D", "NUMERIC")));

        assertThat(testA).isNotNull();
        assertThat(List.of(testB, testC, testD, testE, testF, testG, testH))
                .doesNotContain((UUID) null);
    }

    @Test
    @Order(7)
    void searchLabTests_query_success() throws Exception {
        mockMvc.perform(get("/api/v1/lab-tests?query=" + codeA)
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value(codeA))
                .andExpect(jsonPath("$.content[0].resultType").value("NUMERIC"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @Order(8)
    void searchLabTests_activeFilter_success() throws Exception {
        mockMvc.perform(get("/api/v1/lab-tests?query=GLU-" + runCode + "&active=true")
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/v1/lab-tests?query=GLU-" + runCode + "&active=false")
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @Order(9)
    void updateLabTest_success() throws Exception {
        var payload = Map.of("name", "Glucose (Fasting)", "version", 0);

        mockMvc.perform(put("/api/v1/lab-tests/" + testB)
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Glucose (Fasting)"))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    @Order(10)
    void updateLabTest_staleVersion_conflict() throws Exception {
        var payload = Map.of("name", "Stale Name", "version", 0);

        mockMvc.perform(put("/api/v1/lab-tests/" + testB)
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("This clinical record was updated by another user. Reload to continue."));
    }

    @Test
    @Order(11)
    void deactivateLabTest_success() throws Exception {
        var payload = Map.of("isActive", false, "version", 0);

        mockMvc.perform(put("/api/v1/lab-tests/" + testF)
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    @Order(12)
    void searchLabTests_inactiveFilter() throws Exception {
        mockMvc.perform(get("/api/v1/lab-tests?query=" + codeF + "&active=true")
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(get("/api/v1/lab-tests?query=" + codeF)
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].isActive").value(false));
    }

    // =============================================================
    // Lab order creation, items, retrieval
    // =============================================================

    @Test
    @Order(13)
    void createLabOrder_withTests_success() throws Exception {
        var result = mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid + "/lab-orders")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                orderPayload(List.of(testA, testB), "ROUTINE", "Fasting sample"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.priority").value("ROUTINE"))
                .andExpect(jsonPath("$.encounterUuid").value(encounterAUuid.toString()))
                .andExpect(jsonPath("$.instructions").value("Fasting sample"))
                .andExpect(jsonPath("$.itemCount").value(2))
                .andExpect(jsonPath("$.specimenCount").value(0))
                .andExpect(jsonPath("$.resultCount").value(0))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.orderedAt").doesNotExist())
                .andExpect(jsonPath("$.version").value(0))
                .andReturn();
        String body = result.getResponse().getContentAsString();
        order1 = UUID.fromString(JsonPath.parse(body).read("$.uuid"));
        String number = JsonPath.parse(body).read("$.orderNumber");
        assertThat(number).matches("LAB-\\d{4}-\\d{6}");
        itemA = UUID.fromString(JsonPath.parse(body).read("$.items[0].uuid"));
        itemB = UUID.fromString(JsonPath.parse(body).read("$.items[1].uuid"));
    }

    @Test
    @Order(14)
    void createLabOrder_duplicateTest_rejected() throws Exception {
        mockMvc.perform(post("/api/v1/encounters/" + encounterBUuid + "/lab-orders")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                orderPayload(List.of(testC, testC), null, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("This lab test is already included in the order"));
    }

    @Test
    @Order(15)
    void createLabOrder_inactiveTest_rejected() throws Exception {
        mockMvc.perform(post("/api/v1/encounters/" + encounterBUuid + "/lab-orders")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                orderPayload(List.of(testF), null, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Cannot order an inactive lab test: " + codeF));
    }

    @Test
    @Order(16)
    void createLabOrder_emptyDraft_success() throws Exception {
        order2 = createOrder(encounterBUuid, List.of());
    }

    @Test
    @Order(17)
    void addItem_success() throws Exception {
        itemC = addItem(encounterAUuid, order1, testC);
        itemD = addItem(encounterAUuid, order1, testD);
        itemE = addItem(encounterAUuid, order1, testE);
        assertThat(itemC).isNotEqualTo(itemD).isNotEqualTo(itemE);
    }

    @Test
    @Order(18)
    void addItem_duplicateTest_rejected() throws Exception {
        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/lab-orders/" + order1 + "/items")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("labTestUuid", testA.toString()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("This lab test is already included in the order"));
    }

    @Test
    @Order(19)
    void addItem_unknownTest_notFound() throws Exception {
        UUID missing = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/lab-orders/" + order1 + "/items")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("labTestUuid", missing.toString()))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Lab test not found with UUID: " + missing));
    }

    @Test
    @Order(20)
    void updateItem_instructions_success() throws Exception {
        var payload = Map.of("labTestUuid", testB.toString(),
                "instructions", "Fasting sample required", "version", 0);

        mockMvc.perform(put("/api/v1/encounters/" + encounterAUuid
                        + "/lab-orders/" + order1 + "/items/" + itemB)
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.instructions").value("Fasting sample required"))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    @Order(21)
    void updateItem_staleVersion_conflict() throws Exception {
        var payload = Map.of("labTestUuid", testB.toString(),
                "instructions", "Stale", "version", 0);

        mockMvc.perform(put("/api/v1/encounters/" + encounterAUuid
                        + "/lab-orders/" + order1 + "/items/" + itemB)
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("This clinical record was updated by another user. Reload to continue."));
    }

    @Test
    @Order(22)
    void addItem_thenDeleteItem_success() throws Exception {
        UUID tempItem = addItem(encounterAUuid, order1, testG);

        mockMvc.perform(delete("/api/v1/encounters/" + encounterAUuid
                        + "/lab-orders/" + order1 + "/items/" + tempItem)
                        .header("Authorization", authHeader()))
                .andExpect(status().isNoContent());
    }

    @Test
    @Order(23)
    void listLabOrders_summaryCounts() throws Exception {
        var result = mockMvc.perform(get("/api/v1/encounters/" + encounterAUuid + "/lab-orders")
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("DRAFT"))
                .andExpect(jsonPath("$[0].itemCount").value(5))
                .andExpect(jsonPath("$[0].specimenCount").value(0))
                .andExpect(jsonPath("$[0].specimenStatus").doesNotExist())
                .andExpect(jsonPath("$[0].resultCount").value(0))
                .andReturn();
        String number = JsonPath.parse(result.getResponse().getContentAsString())
                .read("$[0].orderNumber");
        assertThat(number).matches("LAB-\\d{4}-\\d{6}");
    }

    @Test
    @Order(24)
    void getLabOrder_detailWithItems() throws Exception {
        mockMvc.perform(get("/api/v1/encounters/" + encounterAUuid + "/lab-orders/" + order1)
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(5))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    @Order(25)
    void getLabOrder_fromAnotherEncounter_notFound() throws Exception {
        mockMvc.perform(get("/api/v1/encounters/" + encounterBUuid + "/lab-orders/" + order1)
                        .header("Authorization", authHeader()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Lab order not found with UUID: " + order1));
    }

    @Test
    @Order(26)
    void updateLabOrder_instructions_success() throws Exception {
        var payload = Map.of("instructions", "Collect before 10am", "version", 0);

        mockMvc.perform(put("/api/v1/encounters/" + encounterAUuid + "/lab-orders/" + order1)
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.instructions").value("Collect before 10am"))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    @Order(27)
    void updateLabOrder_staleVersion_conflict() throws Exception {
        var payload = Map.of("instructions", "Stale", "version", 0);

        mockMvc.perform(put("/api/v1/encounters/" + encounterAUuid + "/lab-orders/" + order1)
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("This clinical record was updated by another user. Reload to continue."));
    }

    // =============================================================
    // Lab order lifecycle
    // =============================================================

    @Test
    @Order(28)
    void createLabOrder_draftGuards_success() throws Exception {
        order4 = createOrder(encounterAUuid, List.of(testA));
    }

    @Test
    @Order(29)
    void completeDraftOrder_invalidTransition() throws Exception {
        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/lab-orders/" + order4 + "/complete")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Invalid lab order status transition from DRAFT to COMPLETED"));
    }

    @Test
    @Order(30)
    void startDraftOrder_invalidTransition() throws Exception {
        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/lab-orders/" + order4 + "/start")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Invalid lab order status transition from DRAFT to IN_PROGRESS"));
    }

    @Test
    @Order(31)
    void placeOrder_withoutTests_rejected() throws Exception {
        mockMvc.perform(post("/api/v1/encounters/" + encounterBUuid
                        + "/lab-orders/" + order2 + "/order")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Cannot order a lab order without lab tests"));
    }

    @Test
    @Order(32)
    void placeOrder_success() throws Exception {
        placeOrder(encounterAUuid, order1);

        mockMvc.perform(get("/api/v1/encounters/" + encounterAUuid + "/lab-orders/" + order1)
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ORDERED"))
                .andExpect(jsonPath("$.orderedAt").exists())
                .andExpect(jsonPath("$.items[0].status").value("ORDERED"));
    }

    @Test
    @Order(33)
    void placeOrder_idempotent() throws Exception {
        placeOrder(encounterAUuid, order1);
    }

    @Test
    @Order(34)
    void addItem_afterPlaced_rejected() throws Exception {
        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/lab-orders/" + order1 + "/items")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("labTestUuid", testG.toString()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Lab order items can only be modified while the order is a draft"));
    }

    @Test
    @Order(35)
    void updateLabOrder_priorityAfterPlaced_success() throws Exception {
        var payload = Map.of("priority", "URGENT", "version", 2);

        mockMvc.perform(put("/api/v1/encounters/" + encounterAUuid + "/lab-orders/" + order1)
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priority").value("URGENT"))
                .andExpect(jsonPath("$.version").value(3));
    }

    @Test
    @Order(36)
    void startOrder_success_cascadesItems() throws Exception {
        startOrder(encounterAUuid, order1);

        mockMvc.perform(get("/api/v1/encounters/" + encounterAUuid + "/lab-orders/" + order1)
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.items[0].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.items[4].status").value("IN_PROGRESS"));
    }

    @Test
    @Order(37)
    void createLabOrder_forCompletedGuards_success() throws Exception {
        order3 = createOrder(encounterBUuid, List.of(testB));
        var get = mockMvc.perform(get("/api/v1/encounters/" + encounterBUuid
                        + "/lab-orders/" + order3)
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andReturn();
        item3B = UUID.fromString(JsonPath.parse(get.getResponse().getContentAsString())
                .read("$.items[0].uuid"));
    }

    @Test
    @Order(38)
    void placeAndCompleteOrder3_success() throws Exception {
        placeOrder(encounterBUuid, order3);
        completeOrder(encounterBUuid, order3);

        mockMvc.perform(get("/api/v1/encounters/" + encounterBUuid + "/lab-orders/" + order3)
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.items[0].status").value("COMPLETED"));
    }

    @Test
    @Order(39)
    void completeOrder_alreadyCompleted_rejected() throws Exception {
        mockMvc.perform(post("/api/v1/encounters/" + encounterBUuid
                        + "/lab-orders/" + order3 + "/complete")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Lab order is already completed"));
    }

    @Test
    @Order(40)
    void startCompletedOrder_terminal() throws Exception {
        mockMvc.perform(post("/api/v1/encounters/" + encounterBUuid
                        + "/lab-orders/" + order3 + "/start")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Cannot change lab order status from terminal state: COMPLETED"));
    }

    @Test
    @Order(41)
    void updateCompletedOrder_rejected() throws Exception {
        var payload = Map.of("instructions", "late edit", "version", 3);

        mockMvc.perform(put("/api/v1/encounters/" + encounterBUuid + "/lab-orders/" + order3)
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Cannot modify a completed lab order"));
    }

    @Test
    @Order(42)
    void deleteCompletedOrder_rejected() throws Exception {
        mockMvc.perform(delete("/api/v1/encounters/" + encounterBUuid + "/lab-orders/" + order3)
                        .header("Authorization", authHeader()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Cannot delete a completed lab order"));
    }

    @Test
    @Order(43)
    void cancelOrder_success() throws Exception {
        mockMvc.perform(post("/api/v1/encounters/" + encounterBUuid
                        + "/lab-orders/" + order2 + "/cancel")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @Order(44)
    void cancelOrder_alreadyCancelled_rejected() throws Exception {
        mockMvc.perform(post("/api/v1/encounters/" + encounterBUuid
                        + "/lab-orders/" + order2 + "/cancel")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Lab order is already cancelled"));
    }

    @Test
    @Order(45)
    void placeCancelledOrder_terminal() throws Exception {
        mockMvc.perform(post("/api/v1/encounters/" + encounterBUuid
                        + "/lab-orders/" + order2 + "/order")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Cannot change lab order status from terminal state: CANCELLED"));
    }

    @Test
    @Order(46)
    void updateCancelledOrder_rejected() throws Exception {
        var payload = Map.of("instructions", "late edit", "version", 1);

        mockMvc.perform(put("/api/v1/encounters/" + encounterBUuid + "/lab-orders/" + order2)
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Cannot modify a cancelled lab order"));
    }

    // =============================================================
    // Specimen workflow
    // =============================================================

    @Test
    @Order(47)
    void createSpecimen_cancelledOrder_rejected() throws Exception {
        mockMvc.perform(post("/api/v1/lab-orders/" + order2 + "/specimens")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("specimenType", "BLOOD"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Cannot add specimens to a cancelled lab order"));
    }

    @Test
    @Order(48)
    void createSpecimen_draftOrder_rejected() throws Exception {
        mockMvc.perform(post("/api/v1/lab-orders/" + order4 + "/specimens")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("specimenType", "BLOOD"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Cannot add specimens to a draft lab order"));
    }

    @Test
    @Order(49)
    void createSpecimen_completedOrder_rejected() throws Exception {
        mockMvc.perform(post("/api/v1/lab-orders/" + order3 + "/specimens")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("specimenType", "BLOOD"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Cannot add specimens to a completed lab order"));
    }

    @Test
    @Order(50)
    void createSpecimen_missingType_badRequest() throws Exception {
        mockMvc.perform(post("/api/v1/lab-orders/" + order1 + "/specimens")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    @Order(51)
    void createSpecimen_success() throws Exception {
        var result = mockMvc.perform(post("/api/v1/lab-orders/" + order1 + "/specimens")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "specimenType", "BLOOD",
                                "specimenIdentifier", "Tube-001"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_COLLECTION"))
                .andExpect(jsonPath("$.specimenType").value("BLOOD"))
                .andExpect(jsonPath("$.specimenIdentifier").value("Tube-001"))
                .andExpect(jsonPath("$.orderNumber").exists())
                .andExpect(jsonPath("$.version").value(0))
                .andReturn();
        spec1 = UUID.fromString(JsonPath.parse(result.getResponse().getContentAsString()).read("$.uuid"));
    }

    @Test
    @Order(52)
    void collectSpecimen_success() throws Exception {
        mockMvc.perform(post("/api/v1/lab-orders/" + order1
                        + "/specimens/" + spec1 + "/collect")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COLLECTED"))
                .andExpect(jsonPath("$.collectedAt").exists());
    }

    @Test
    @Order(53)
    void collectSpecimen_idempotent() throws Exception {
        mockMvc.perform(post("/api/v1/lab-orders/" + order1
                        + "/specimens/" + spec1 + "/collect")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COLLECTED"));
    }

    @Test
    @Order(54)
    void receiveSpecimen_success() throws Exception {
        mockMvc.perform(post("/api/v1/lab-orders/" + order1
                        + "/specimens/" + spec1 + "/receive")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.receivedAt").exists());
    }

    @Test
    @Order(55)
    void receiveSpecimen_idempotent() throws Exception {
        mockMvc.perform(post("/api/v1/lab-orders/" + order1
                        + "/specimens/" + spec1 + "/receive")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECEIVED"));
    }

    @Test
    @Order(56)
    void rejectSpecimen_success() throws Exception {
        spec2 = createSpecimen(order1, "BLOOD");
        collectSpecimen(order1, spec2);

        mockMvc.perform(post("/api/v1/lab-orders/" + order1
                        + "/specimens/" + spec2 + "/reject")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("rejectionReason", "Hemolyzed sample"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("Hemolyzed sample"));
    }

    @Test
    @Order(57)
    void rejectSpecimen_missingReason_badRequest() throws Exception {
        spec3 = createSpecimen(order1, "BLOOD");
        collectSpecimen(order1, spec3);

        mockMvc.perform(post("/api/v1/lab-orders/" + order1
                        + "/specimens/" + spec3 + "/reject")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("rejectionReason", " "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    @Order(58)
    void receiveRejectedSpecimen_terminal() throws Exception {
        mockMvc.perform(post("/api/v1/lab-orders/" + order1
                        + "/specimens/" + spec2 + "/receive")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Cannot change specimen status from terminal state: REJECTED"));
    }

    @Test
    @Order(59)
    void cancelSpecimen_success() throws Exception {
        mockMvc.perform(post("/api/v1/lab-orders/" + order1
                        + "/specimens/" + spec3 + "/cancel")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @Order(60)
    void collectCancelledSpecimen_terminal() throws Exception {
        mockMvc.perform(post("/api/v1/lab-orders/" + order1
                        + "/specimens/" + spec3 + "/collect")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Cannot change specimen status from terminal state: CANCELLED"));
    }

    @Test
    @Order(61)
    void getSpecimen_fromAnotherOrder_notFound() throws Exception {
        mockMvc.perform(get("/api/v1/lab-orders/" + order3
                        + "/specimens/" + spec1)
                        .header("Authorization", authHeader()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Specimen not found with UUID: " + spec1));
    }

    @Test
    @Order(62)
    void listSpecimens_orderScoped() throws Exception {
        mockMvc.perform(get("/api/v1/lab-orders/" + order1 + "/specimens")
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));

        mockMvc.perform(get("/api/v1/lab-orders/" + order3 + "/specimens")
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // =============================================================
    // Lab results: guards and validation
    // =============================================================

    @Test
    @Order(63)
    void createResult_draftOrder_rejected() throws Exception {
        mockMvc.perform(post("/api/v1/lab-orders/" + order4 + "/results")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "labOrderItemUuid", itemA.toString(),
                                "specimenUuid", spec1.toString(),
                                "values", List.of(value(testA, 10, null, null))))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Cannot add results to a draft lab order"));
    }

    @Test
    @Order(64)
    void createResult_cancelledOrder_rejected() throws Exception {
        mockMvc.perform(post("/api/v1/lab-orders/" + order2 + "/results")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "labOrderItemUuid", itemA.toString(),
                                "specimenUuid", spec1.toString(),
                                "values", List.of(value(testA, 10, null, null))))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Cannot add results to a cancelled lab order"));
    }

    @Test
    @Order(65)
    void createResult_missingSpecimen_badRequest() throws Exception {
        var payload = new HashMap<String, Object>();
        payload.put("labOrderItemUuid", itemA.toString());
        payload.put("values", List.of(value(testA, 10, null, null)));

        mockMvc.perform(post("/api/v1/lab-orders/" + order1 + "/results")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    @Order(66)
    void createResult_unknownItem_notFound() throws Exception {
        UUID missing = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/lab-orders/" + order1 + "/results")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "labOrderItemUuid", missing.toString(),
                                "specimenUuid", spec1.toString(),
                                "values", List.of(value(testA, 10, null, null))))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Lab order item not found with UUID: " + missing));
    }

    @Test
    @Order(67)
    void createResult_itemOfAnotherOrder_notFound() throws Exception {
        mockMvc.perform(post("/api/v1/lab-orders/" + order3 + "/results")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "labOrderItemUuid", itemA.toString(),
                                "specimenUuid", spec1.toString(),
                                "values", List.of(value(testA, 10, null, null))))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Lab order item not found with UUID: " + itemA));
    }

    @Test
    @Order(68)
    void createResult_specimenOfAnotherOrder_notFound() throws Exception {
        mockMvc.perform(post("/api/v1/lab-orders/" + order3 + "/results")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "labOrderItemUuid", item3B.toString(),
                                "specimenUuid", spec1.toString(),
                                "values", List.of(value(testB, 85, null, null))))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Specimen not found with UUID: " + spec1));
    }

    @Test
    @Order(69)
    void createResult_unreceivedSpecimen_rejected() throws Exception {
        mockMvc.perform(post("/api/v1/lab-orders/" + order1 + "/results")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "labOrderItemUuid", itemA.toString(),
                                "specimenUuid", spec2.toString(),
                                "values", List.of(value(testA, 10, null, null))))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Specimen must be received before entering results. "
                                + "Current status: REJECTED"));
    }

    @Test
    @Order(70)
    void createResult_representationValidation_rejected() throws Exception {
        // NUMERIC test with text
        mockMvc.perform(post("/api/v1/lab-orders/" + order1 + "/results")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "labOrderItemUuid", itemA.toString(),
                                "specimenUuid", spec1.toString(),
                                "values", List.of(value(testA, 10, "low", null))))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Numeric result for Hemoglobin cannot also include text or code"));

        // NUMERIC test without a number
        mockMvc.perform(post("/api/v1/lab-orders/" + order1 + "/results")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "labOrderItemUuid", itemA.toString(),
                                "specimenUuid", spec1.toString(),
                                "values", List.of(value(testA, null, "low", null))))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Value for Hemoglobin must be numeric"));

        // QUALITATIVE test with a number
        mockMvc.perform(post("/api/v1/lab-orders/" + order1 + "/results")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "labOrderItemUuid", itemC.toString(),
                                "specimenUuid", spec1.toString(),
                                "values", List.of(value(testC, 1, null, null))))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Qualitative result for CRP Qualitative cannot be numeric"));

        // TEXT test with a number
        mockMvc.perform(post("/api/v1/lab-orders/" + order1 + "/results")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "labOrderItemUuid", itemD.toString(),
                                "specimenUuid", spec1.toString(),
                                "values", List.of(value(testD, 1, null, null))))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Text result for Urine Culture cannot also include a number or code"));

        // CODED test without a code
        mockMvc.perform(post("/api/v1/lab-orders/" + order1 + "/results")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "labOrderItemUuid", itemE.toString(),
                                "specimenUuid", spec1.toString(),
                                "values", List.of(value(testE, null, "DETECTED", null))))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Value for Urine Drug Screen must be a coded value"));
    }

    @Test
    @Order(71)
    void createResult_unknownLabTest_notFound() throws Exception {
        UUID missing = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/lab-orders/" + order1 + "/results")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "labOrderItemUuid", itemA.toString(),
                                "specimenUuid", spec1.toString(),
                                "values", List.of(value(missing, 10, null, null))))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Lab test not found with UUID: " + missing));
    }

    @Test
    @Order(72)
    void createResult_emptyValues_badRequest() throws Exception {
        var payload = new HashMap<String, Object>();
        payload.put("labOrderItemUuid", itemA.toString());
        payload.put("specimenUuid", spec1.toString());
        payload.put("values", List.of());

        mockMvc.perform(post("/api/v1/lab-orders/" + order1 + "/results")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    // =============================================================
    // Lab results: lifecycle
    // =============================================================

    @Test
    @Order(73)
    void createResult_preliminaryWithInferredFlag_success() throws Exception {
        var result = mockMvc.perform(post("/api/v1/lab-orders/" + order1 + "/results")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "labOrderItemUuid", itemA.toString(),
                                "specimenUuid", spec1.toString(),
                                "values", List.of(value(testA, 10.5, null, null))))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PRELIMINARY"))
                .andExpect(jsonPath("$.labTestCode").value(codeA))
                .andExpect(jsonPath("$.verifiedAt").doesNotExist())
                .andExpect(jsonPath("$.version").value(0))
                .andExpect(jsonPath("$.values[0].valueNumeric").value(10.5))
                .andExpect(jsonPath("$.values[0].unit").value("g/dL"))
                .andExpect(jsonPath("$.values[0].referenceLow").value(12))
                .andExpect(jsonPath("$.values[0].referenceHigh").value(16))
                .andExpect(jsonPath("$.values[0].abnormalFlag").value("LOW"))
                .andReturn();
        result1 = UUID.fromString(JsonPath.parse(result.getResponse().getContentAsString()).read("$.uuid"));
    }

    @Test
    @Order(74)
    void createResult_normalValue_success() throws Exception {
        var result = mockMvc.perform(post("/api/v1/lab-orders/" + order1 + "/results")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "labOrderItemUuid", itemB.toString(),
                                "specimenUuid", spec1.toString(),
                                "values", List.of(value(testB, 85, null, null))))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PRELIMINARY"))
                .andExpect(jsonPath("$.values[0].abnormalFlag").value("NORMAL"))
                .andExpect(jsonPath("$.values[0].referenceLow").value(70))
                .andExpect(jsonPath("$.values[0].referenceHigh").value(99))
                .andReturn();
        result2 = UUID.fromString(JsonPath.parse(result.getResponse().getContentAsString()).read("$.uuid"));
    }

    @Test
    @Order(75)
    void createResult_explicitFlag_notOverwritten() throws Exception {
        var valuePayload = value(testB, 200, null, null);
        valuePayload.put("abnormalFlag", "CRITICAL_HIGH");

        var result = mockMvc.perform(post("/api/v1/lab-orders/" + order1 + "/results")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "labOrderItemUuid", itemB.toString(),
                                "specimenUuid", spec1.toString(),
                                "values", List.of(valuePayload)))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.values[0].abnormalFlag").value("CRITICAL_HIGH"))
                .andReturn();
        result3 = UUID.fromString(JsonPath.parse(result.getResponse().getContentAsString()).read("$.uuid"));
    }

    @Test
    @Order(76)
    void createResult_qualitative_success() throws Exception {
        var valuePayload = value(testC, null, "Positive", null);
        valuePayload.put("abnormalFlag", "POSITIVE");

        var result = mockMvc.perform(post("/api/v1/lab-orders/" + order1 + "/results")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "labOrderItemUuid", itemC.toString(),
                                "specimenUuid", spec1.toString(),
                                "values", List.of(valuePayload)))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.values[0].valueText").value("Positive"))
                .andExpect(jsonPath("$.values[0].abnormalFlag").value("POSITIVE"))
                .andExpect(jsonPath("$.values[0].referenceText").value("Negative"))
                .andReturn();
        result4 = UUID.fromString(JsonPath.parse(result.getResponse().getContentAsString()).read("$.uuid"));
    }

    @Test
    @Order(77)
    void createResult_text_success() throws Exception {
        var result = mockMvc.perform(post("/api/v1/lab-orders/" + order1 + "/results")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "labOrderItemUuid", itemD.toString(),
                                "specimenUuid", spec1.toString(),
                                "values", List.of(value(testD, null, "No growth after 48h", null))))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.values[0].valueText").value("No growth after 48h"))
                .andExpect(jsonPath("$.values[0].abnormalFlag").doesNotExist())
                .andReturn();
        result5 = UUID.fromString(JsonPath.parse(result.getResponse().getContentAsString()).read("$.uuid"));
    }

    @Test
    @Order(78)
    void createResult_coded_success() throws Exception {
        var result = mockMvc.perform(post("/api/v1/lab-orders/" + order1 + "/results")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "labOrderItemUuid", itemE.toString(),
                                "specimenUuid", spec1.toString(),
                                "values", List.of(value(testE, null, null, "DETECTED"))))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.values[0].valueCode").value("DETECTED"))
                .andReturn();
        result6 = UUID.fromString(JsonPath.parse(result.getResponse().getContentAsString()).read("$.uuid"));
    }

    @Test
    @Order(79)
    void updateResult_preliminary_success() throws Exception {
        var payload = new HashMap<String, Object>();
        payload.put("comments", "Rechecked on repeat run");
        payload.put("values", List.of(value(testA, 11.0, null, null)));
        payload.put("version", 0);

        mockMvc.perform(put("/api/v1/lab-orders/" + order1 + "/results/" + result1)
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PRELIMINARY"))
                .andExpect(jsonPath("$.comments").value("Rechecked on repeat run"))
                .andExpect(jsonPath("$.values[0].valueNumeric").value(11.0))
                .andExpect(jsonPath("$.values[0].abnormalFlag").value("LOW"))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    @Order(80)
    void updateResult_staleVersion_conflict() throws Exception {
        var payload = new HashMap<String, Object>();
        payload.put("values", List.of(value(testA, 12.0, null, null)));
        payload.put("version", 0);

        mockMvc.perform(put("/api/v1/lab-orders/" + order1 + "/results/" + result1)
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("This clinical record was updated by another user. Reload to continue."));
    }

    @Test
    @Order(81)
    void finalizeResult_success() throws Exception {
        mockMvc.perform(post("/api/v1/lab-orders/" + order1
                        + "/results/" + result1 + "/finalize")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINAL"))
                .andExpect(jsonPath("$.verifiedAt").exists())
                .andExpect(jsonPath("$.version").value(2));
    }

    @Test
    @Order(82)
    void finalizeResult_alreadyFinal_rejected() throws Exception {
        mockMvc.perform(post("/api/v1/lab-orders/" + order1
                        + "/results/" + result1 + "/finalize")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Lab result is already finalized"));
    }

    @Test
    @Order(83)
    void updateFinalResult_rejected() throws Exception {
        var payload = new HashMap<String, Object>();
        payload.put("values", List.of(value(testA, 13.0, null, null)));
        payload.put("version", 2);

        mockMvc.perform(put("/api/v1/lab-orders/" + order1 + "/results/" + result1)
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Lab result is already finalized. Use correction to record changes."));
    }

    @Test
    @Order(84)
    void correctResult_missingReason_badRequest() throws Exception {
        var payload = new HashMap<String, Object>();
        payload.put("values", List.of(value(testA, 11.5, null, null)));
        payload.put("version", 2);

        mockMvc.perform(post("/api/v1/lab-orders/" + order1
                        + "/results/" + result1 + "/correct")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    @Order(85)
    void correctResult_success() throws Exception {
        var payload = new HashMap<String, Object>();
        payload.put("correctionReason", "Instrument recalibration");
        payload.put("values", List.of(value(testA, 11.5, null, null)));
        payload.put("version", 2);

        mockMvc.perform(post("/api/v1/lab-orders/" + order1
                        + "/results/" + result1 + "/correct")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CORRECTED"))
                .andExpect(jsonPath("$.correctionReason").value("Instrument recalibration"))
                .andExpect(jsonPath("$.verifiedAt").exists())
                .andExpect(jsonPath("$.values[0].valueNumeric").value(11.5))
                .andExpect(jsonPath("$.version").value(3));
    }

    @Test
    @Order(86)
    void correctResult_alreadyCorrected_rejected() throws Exception {
        var payload = new HashMap<String, Object>();
        payload.put("correctionReason", "Again");
        payload.put("values", List.of(value(testA, 11.6, null, null)));

        mockMvc.perform(post("/api/v1/lab-orders/" + order1
                        + "/results/" + result1 + "/correct")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Lab result has already been corrected"));
    }

    @Test
    @Order(87)
    void editCorrectedResult_rejected() throws Exception {
        var payload = new HashMap<String, Object>();
        payload.put("values", List.of(value(testA, 11.7, null, null)));
        payload.put("version", 3);

        mockMvc.perform(put("/api/v1/lab-orders/" + order1 + "/results/" + result1)
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Cannot edit a corrected lab result"));
    }

    @Test
    @Order(88)
    void cancelResult_success() throws Exception {
        mockMvc.perform(post("/api/v1/lab-orders/" + order1
                        + "/results/" + result2 + "/cancel")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @Order(89)
    void cancelResult_alreadyCancelled_rejected() throws Exception {
        mockMvc.perform(post("/api/v1/lab-orders/" + order1
                        + "/results/" + result2 + "/cancel")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Lab result is already cancelled"));
    }

    @Test
    @Order(90)
    void finalizeCancelledResult_terminal() throws Exception {
        mockMvc.perform(post("/api/v1/lab-orders/" + order1
                        + "/results/" + result2 + "/finalize")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Cannot change lab result status from terminal state: CANCELLED"));
    }

    @Test
    @Order(91)
    void finalizeThenCancelFinalResult_invalidTransition() throws Exception {
        mockMvc.perform(post("/api/v1/lab-orders/" + order1
                        + "/results/" + result4 + "/finalize")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINAL"));

        mockMvc.perform(post("/api/v1/lab-orders/" + order1
                        + "/results/" + result4 + "/cancel")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Invalid lab result status transition from FINAL to CANCELLED"));
    }

    @Test
    @Order(92)
    void getResult_fromAnotherOrder_notFound() throws Exception {
        mockMvc.perform(get("/api/v1/lab-orders/" + order3 + "/results/" + result1)
                        .header("Authorization", authHeader()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Lab result not found with UUID: " + result1));
    }

    @Test
    @Order(93)
    void listResults_orderScoped() throws Exception {
        mockMvc.perform(get("/api/v1/lab-orders/" + order1 + "/results")
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6));

        mockMvc.perform(get("/api/v1/lab-orders/" + order3 + "/results")
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @Order(94)
    void updateLabTest_resultTypeGuard() throws Exception {
        // Referenced by results -> change blocked
        var blocked = Map.of("resultType", "TEXT", "version", 0);
        mockMvc.perform(put("/api/v1/lab-tests/" + testC)
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blocked)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Cannot change the result type of a lab test that has recorded results"));

        // Never resulted -> change allowed
        var allowed = Map.of("resultType", "TEXT", "version", 0);
        mockMvc.perform(put("/api/v1/lab-tests/" + testG)
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(allowed)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultType").value("TEXT"));
    }

    @Test
    @Order(95)
    void listLabOrders_finalCounts() throws Exception {
        mockMvc.perform(get("/api/v1/encounters/" + encounterAUuid + "/lab-orders")
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].uuid").value(order1.toString()))
                .andExpect(jsonPath("$[0].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$[0].itemCount").value(5))
                .andExpect(jsonPath("$[0].specimenCount").value(3))
                .andExpect(jsonPath("$[0].specimenStatus").value("RECEIVED"))
                .andExpect(jsonPath("$[0].resultCount").value(6))
                .andExpect(jsonPath("$[0].finalizedResultCount").value(2));
    }

    @Test
    @Order(96)
    void completeOrder1_success() throws Exception {
        completeOrder(encounterAUuid, order1);

        mockMvc.perform(get("/api/v1/encounters/" + encounterAUuid + "/lab-orders/" + order1)
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.items[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$.items[4].status").value("COMPLETED"));
    }

    @Test
    @Order(97)
    void deleteOrder_withFinalizedResults_rejected() throws Exception {
        mockMvc.perform(delete("/api/v1/encounters/" + encounterAUuid + "/lab-orders/" + order1)
                        .header("Authorization", authHeader()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Cannot delete a lab order with finalized lab results"));
    }

    // =============================================================
    // Completed encounter: clinical edits blocked, lab processing continues
    // =============================================================

    @Test
    @Order(98)
    void createLabOrder_onEncounterC_beforeCompletion_success() throws Exception {
        orderC = createOrder(encounterCUuid, List.of(testA));
        var get = mockMvc.perform(get("/api/v1/encounters/" + encounterCUuid
                        + "/lab-orders/" + orderC)
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andReturn();
        itemCItem = UUID.fromString(JsonPath.parse(get.getResponse().getContentAsString())
                .read("$.items[0].uuid"));
    }

    @Test
    @Order(99)
    void completeEncounterC_success() throws Exception {
        mockMvc.perform(post("/api/v1/encounters/" + encounterCUuid + "/complete")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @Order(100)
    void createLabOrder_completedEncounter_rejected() throws Exception {
        mockMvc.perform(post("/api/v1/encounters/" + encounterCUuid + "/lab-orders")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                orderPayload(List.of(testB), null, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Cannot modify lab orders for a completed encounter"));
    }

    @Test
    @Order(101)
    void updateLabOrder_completedEncounter_rejected() throws Exception {
        var payload = Map.of("instructions", "nope", "version", 0);

        mockMvc.perform(put("/api/v1/encounters/" + encounterCUuid + "/lab-orders/" + orderC)
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Cannot modify lab orders for a completed encounter"));
    }

    @Test
    @Order(102)
    void addItem_completedEncounter_rejected() throws Exception {
        mockMvc.perform(post("/api/v1/encounters/" + encounterCUuid
                        + "/lab-orders/" + orderC + "/items")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("labTestUuid", testB.toString()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Cannot modify lab orders for a completed encounter"));
    }

    @Test
    @Order(103)
    void deleteOrder_completedEncounter_rejected() throws Exception {
        mockMvc.perform(delete("/api/v1/encounters/" + encounterCUuid + "/lab-orders/" + orderC)
                        .header("Authorization", authHeader()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Cannot modify lab orders for a completed encounter"));
    }

    @Test
    @Order(104)
    void placeOrder_completedEncounter_allowed() throws Exception {
        placeOrder(encounterCUuid, orderC);
    }

    @Test
    @Order(105)
    void startOrder_completedEncounter_allowed() throws Exception {
        startOrder(encounterCUuid, orderC);
    }

    @Test
    @Order(106)
    void specimenWorkflow_completedEncounter_allowed() throws Exception {
        specC = createSpecimen(orderC, "BLOOD");
        collectSpecimen(orderC, specC);
        receiveSpecimen(orderC, specC);
    }

    @Test
    @Order(107)
    void resultWorkflow_completedEncounter_allowed() throws Exception {
        UUID resultC = createResult(orderC, itemCItem, specC,
                List.of(value(testA, 14.2, null, null)));

        mockMvc.perform(post("/api/v1/lab-orders/" + orderC
                        + "/results/" + resultC + "/finalize")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINAL"));
    }

    // =============================================================
    // Soft delete and catalog guards
    // =============================================================

    @Test
    @Order(108)
    void deleteDraftOrder_success() throws Exception {
        mockMvc.perform(delete("/api/v1/encounters/" + encounterAUuid + "/lab-orders/" + order4)
                        .header("Authorization", authHeader()))
                .andExpect(status().isNoContent());
    }

    @Test
    @Order(109)
    void getDeletedOrder_notFound() throws Exception {
        mockMvc.perform(get("/api/v1/encounters/" + encounterAUuid + "/lab-orders/" + order4)
                        .header("Authorization", authHeader()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Lab order not found with UUID: " + order4));

        var result = mockMvc.perform(get("/api/v1/encounters/" + encounterAUuid + "/lab-orders")
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andReturn();
        assertThat(result.getResponse().getContentAsString()).doesNotContain(order4.toString());
    }

    @Test
    @Order(110)
    void createLabOrder_softDeletedEncounter_notFound() throws Exception {
        jdbcTemplate.update("UPDATE encounters SET deleted_at = now() WHERE uuid = ?::uuid",
                encounterDUuid.toString());

        mockMvc.perform(post("/api/v1/encounters/" + encounterDUuid + "/lab-orders")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                orderPayload(List.of(testA), null, null))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Encounter not found with UUID: " + encounterDUuid));
    }

    @Test
    @Order(111)
    void deleteLabTest_unreferenced_success() throws Exception {
        mockMvc.perform(delete("/api/v1/lab-tests/" + testH)
                        .header("Authorization", authHeader()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/lab-tests/" + testH)
                        .header("Authorization", authHeader()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Lab test not found with UUID: " + testH));

        mockMvc.perform(get("/api/v1/lab-tests?query=" + codeH)
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @Order(112)
    void deleteLabTest_referenced_rejected() throws Exception {
        mockMvc.perform(delete("/api/v1/lab-tests/" + testA)
                        .header("Authorization", authHeader()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Cannot delete a lab test that is referenced by clinical records. "
                                + "Set it inactive instead."));
    }

    @Test
    @Order(113)
    void labOrderNumbers_uniqueAndFormatted() throws Exception {
        var numbers = new ArrayList<String>();

        var a = mockMvc.perform(get("/api/v1/encounters/" + encounterAUuid + "/lab-orders")
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andReturn();
        numbers.addAll(JsonPath.parse(a.getResponse().getContentAsString())
                .read("$[*].orderNumber", java.util.List.class));

        var b = mockMvc.perform(get("/api/v1/encounters/" + encounterBUuid + "/lab-orders")
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andReturn();
        numbers.addAll(JsonPath.parse(b.getResponse().getContentAsString())
                .read("$[*].orderNumber", java.util.List.class));

        assertThat(numbers).hasSize(3);
        assertThat(numbers).allSatisfy(n -> assertThat((String) n).matches("LAB-\\d{4}-\\d{6}"))
                .doesNotHaveDuplicates();
    }

    @Test
    @Order(114)
    void labOrder_jpaOptimisticLock_conflict() {
        String uuid = order2.toString();

        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
            LabOrder order = labOrderRepository.findByUuidAndDeletedAtIsNull(order2)
                    .orElseThrow();
            jdbcTemplate.update("UPDATE lab_orders SET version = version + 5 WHERE uuid = ?::uuid", uuid);
            order.setInstructions("conflicting concurrent write");
            labOrderRepository.saveAndFlush(order);
        })).isInstanceOf(OptimisticLockingFailureException.class);
    }

    // =============================================================
    // Permissions and authentication
    // =============================================================

    @Test
    @Order(115)
    void labEndpoints_missingPermission_forbidden() throws Exception {
        // PATIENT holds LAB_ORDER_VIEW + LAB_RESULT_VIEW only.
        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid + "/lab-orders")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                orderPayload(List.of(testA), null, null))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/lab-orders/" + order1 + "/order")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/lab-orders/" + order1 + "/specimens")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("specimenType", "BLOOD"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/lab-orders/" + order1 + "/results")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "labOrderItemUuid", itemA.toString(),
                                "specimenUuid", spec1.toString(),
                                "values", List.of(value(testA, 10, null, null))))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/lab-orders/" + order1
                        + "/results/" + result1 + "/finalize")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/lab-orders/" + order1
                        + "/results/" + result1 + "/correct")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "correctionReason", "no perm",
                                "values", List.of(value(testA, 10, null, null))))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/lab-tests?query=HB")
                        .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/lab-tests")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                labTestPayload("NOPERM-" + runCode, "No Perm", "NUMERIC"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/v1/encounters/" + encounterAUuid + "/lab-orders/" + order1)
                        .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(116)
    void labEndpoints_patientCanView() throws Exception {
        mockMvc.perform(get("/api/v1/encounters/" + encounterAUuid + "/lab-orders")
                        .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/encounters/" + encounterAUuid + "/lab-orders/" + order1)
                        .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/lab-orders/" + order1 + "/results")
                        .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isOk());

        // Specimen handling requires SPECIMEN_VIEW, which PATIENT lacks.
        mockMvc.perform(get("/api/v1/lab-orders/" + order1 + "/specimens")
                        .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(117)
    void labEndpoints_requireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/encounters/" + encounterAUuid + "/lab-orders"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid + "/lab-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                orderPayload(List.of(testA), null, null))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/lab-tests?query=HB"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/lab-orders/" + order1 + "/results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "labOrderItemUuid", itemA.toString(),
                                "specimenUuid", spec1.toString(),
                                "values", List.of(value(testA, 10, null, null))))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/lab-orders/" + order1 + "/specimens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("specimenType", "BLOOD"))))
                .andExpect(status().isUnauthorized());
    }
}
