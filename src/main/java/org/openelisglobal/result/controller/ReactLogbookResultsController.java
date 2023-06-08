package org.openelisglobal.result.controller;

import java.lang.reflect.InvocationTargetException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpEntity;

import org.json.simple.JSONObject;
import org.json.simple.parser.ContainerFactory;
import org.json.simple.parser.JSONParser;

import org.apache.commons.validator.GenericValidator;
import org.hibernate.StaleObjectStateException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.formfields.FormFields;
import org.openelisglobal.common.formfields.FormFields.Field;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.services.DisplayListService.ListType;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.ResultSaveService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.beanAdapters.ResultSaveBeanAdapter;
import org.openelisglobal.common.services.registration.ResultUpdateRegister;
import org.openelisglobal.common.services.registration.interfaces.IResultUpdate;
import org.openelisglobal.common.services.serviceBeans.ResultSaveBean;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.dataexchange.fhir.exception.FhirPersistanceException;
import org.openelisglobal.dataexchange.fhir.exception.FhirTransformationException;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.inventory.action.InventoryUtility;
import org.openelisglobal.inventory.form.InventoryKitItem;
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.note.service.NoteServiceImpl.NoteType;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.referral.action.beanitems.ReferralItem;
import org.openelisglobal.referral.service.ReferralTypeService;
import org.openelisglobal.referral.valueholder.Referral;
import org.openelisglobal.referral.valueholder.ReferralResult;
import org.openelisglobal.referral.valueholder.ReferralSet;
import org.openelisglobal.referral.valueholder.ReferralStatus;
import org.openelisglobal.referral.valueholder.ReferralType;
import org.openelisglobal.result.action.util.ResultSet;
import org.openelisglobal.result.action.util.ResultUtil;
import org.openelisglobal.result.action.util.ResultsLoadUtility;
import org.openelisglobal.result.action.util.ResultsPaging;
import org.openelisglobal.result.action.util.ResultsUpdateDataSet;
import org.openelisglobal.result.form.AccessionResultsForm;
import org.openelisglobal.result.form.LogbookResultsForm;
import org.openelisglobal.result.form.LogbookResultsForm.LogbookResults;
import org.openelisglobal.result.service.LogbookResultsPersistService;
import org.openelisglobal.result.service.ResultInventoryService;
import org.openelisglobal.result.service.ResultSignatureService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.result.valueholder.ResultInventory;
import org.openelisglobal.result.valueholder.ResultSignature;
import org.openelisglobal.resultlimit.service.ResultLimitService;
import org.openelisglobal.resultlimits.valueholder.ResultLimit;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.statusofsample.util.StatusRules;
import org.openelisglobal.systemuser.service.UserService;
import org.openelisglobal.test.beanItems.TestResultItem;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.testreflex.action.bean.ReflexRule;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.*;

@Controller
@RequestMapping(value = "/rest/")
public class ReactLogbookResultsController extends LogbookResultsBaseController {

    private final String[] ALLOWED_FIELDS = new String[] { "accessionNumber", "collectionDate", "recievedDate",
            "selectedTest", "selectedAnalysisStatus", "selectedSampleStatus", "testSectionId", "methodId", "type",
            "currentPageID", "testResult*.accessionNumber", "testResult*.isModified", "testResult*.analysisId",
            "testResult*.resultId", "testResult*.testId", "testResult*.technicianSignatureId", "testResult*.testKitId",
            "testResult*.resultLimitId", "testResult*.resultType", "testResult*.valid", "testResult*.referralId",
            "testResult*.referralCanceled", "testResult*.considerRejectReason", "testResult*.hasQualifiedResult",
            "testResult*.shadowResultValue", "testResult*.reflexJSONResult", "testResult*.testDate",
            "testResult*.analysisMethod", "testResult*.testMethod", "testResult*.testKitInventoryId",
            "testResult*.forceTechApproval", "testResult*.lowerNormalRange", "testResult*.upperNormalRange",
            "testResult*.significantDigits", "testResult*.resultValue", "testResult*.qualifiedResultValue",
            "testResult*.multiSelectResultValues", "testResult*.testMethod", "testResult*.multiSelectResultValues",
            "testResult*.qualifiedResultValue", "testResult*.qualifiedResultValue", "testResult*.shadowReferredOut",
            "testResult*.referredOut", "testResult*.referralReasonId", "testResult*.technician",
            "testResult*.shadowRejected", "testResult*.rejected", "testResult*.rejectReasonId", "testResult*.note",
            "paging.currentPage", //
            "testResult*.refer", "testResult*.referralItem.referralReasonId",
            "testResult*.referralItem.referredInstituteId", "testResult*.referralItem.referredTestId",
            "testResult*.referralItem.referredSendDate" };

    @Autowired
    private DictionaryService dictionaryService;
    @Autowired
    private ResultSignatureService resultSigService;
    @Autowired
    private ResultInventoryService resultInventoryService;
    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private ResultLimitService resultLimitService;
    @Autowired
    private TestSectionService testSectionService;
    @Autowired
    private LogbookResultsPersistService logbookPersistService;
    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private NoteService noteService;
    @Autowired
    private FhirTransformService fhirTransformService;
    @Autowired
    private UserService userService;
    @Autowired
    private RoleService roleService;
    @Autowired
    private SampleService sampleService;

    private final String RESULT_SUBJECT = "Result Note";
    private final String REFERRAL_CONFORMATION_ID;
    private static final String REFLEX_ACCESSIONS = "reflex_accessions";

    private ReactLogbookResultsController(ReferralTypeService referralTypeService) {
        ReferralType referralType = referralTypeService.getReferralTypeByName("Confirmation");
        if (referralType != null) {
            REFERRAL_CONFORMATION_ID = referralType.getId();
        } else {
            REFERRAL_CONFORMATION_ID = null;
        }
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

//    @RequestMapping(value = "/ReactLogbookResults", method = RequestMethod.GET)
//    public ModelAndView showReactLogbookResults(HttpServletRequest request,
//            @Validated(LogbookResults.class) @ModelAttribute("form") LogbookResultsForm form, BindingResult result)
//            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

    @GetMapping(value = "ReactLogbookResultsByRange", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public LogbookResultsForm showReactLogbookResults(@RequestParam String labNumber, @RequestParam boolean doRange,
            @RequestParam boolean finished,
            @Validated(LogbookResults.class) @ModelAttribute("form") LogbookResultsForm form, BindingResult result)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        System.out.println("Get:ReactLogbookResultsController:ReactLogbookResultsByRange:labNumber:doRange:finished"
                + labNumber + ":" + doRange + ":" + finished);

        LogbookResultsForm newForm = new LogbookResultsForm();
        if (!(result.hasFieldErrors("type") || result.hasFieldErrors("testSectionId")
                || result.hasFieldErrors("methodId") || result.hasFieldErrors("accessionNumber"))) {
            newForm.setType(form.getType());
            newForm.setTestSectionId(form.getTestSectionId());

            String currentDate = getCurrentDate();
            newForm.setCurrentDate(currentDate);
            newForm.setAccessionNumber(labNumber);
            newForm.setReferralReasons(
                    DisplayListService.getInstance().getList(DisplayListService.ListType.REFERRAL_REASONS));
            newForm.setRejectReasons(DisplayListService.getInstance()
                    .getNumberedListWithLeadingBlank(DisplayListService.ListType.REJECTION_REASONS));

            // load testSections for drop down
            String resultsRoleId = roleService.getRoleByName(Constants.ROLE_RESULTS).getId();
            List<IdValuePair> testSections = userService.getUserTestSections(getSysUserId(request), resultsRoleId);
            newForm.setTestSections(testSections);
            newForm.setTestSectionsByName(DisplayListService.getInstance().getList(ListType.TEST_SECTION_BY_NAME));
            newForm.setMethods(DisplayListService.getInstance().getList(ListType.METHODS));
        }
        newForm.setDisplayTestSections(true);
        newForm.setSearchByRange(false);

        return getLogbookResults(request, newForm, doRange, finished);
    }

//    @RequestMapping(value = "/ReactRangeResults", method = RequestMethod.GET)
//    public ModelAndView showReactLogbookResultsByRange(HttpServletRequest request,
//            @Validated(LogbookResults.class) @ModelAttribute("form") LogbookResultsForm form, BindingResult result)
//            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

    @GetMapping(value = "ReactRangeResults", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public LogbookResultsForm showReactLogbookResultsByRange(@RequestParam String labNumber,
            @RequestParam boolean doRange, @RequestParam boolean finished,
            @Validated(LogbookResults.class) @ModelAttribute("form") LogbookResultsForm form, BindingResult result)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        System.out.println("Get:ReactLogbookResultsController showReactLogbookResultsByRange:labNumber:" + labNumber);

        LogbookResultsForm newForm = new LogbookResultsForm();
        if (!(result.hasFieldErrors("type") || result.hasFieldErrors("accessionNumber"))) {
            newForm.setType(form.getType());
            newForm.setAccessionNumber(form.getAccessionNumber());

            String currentDate = getCurrentDate();
            newForm.setCurrentDate(currentDate);
            newForm.setReferralReasons(
                    DisplayListService.getInstance().getList(DisplayListService.ListType.REFERRAL_REASONS));
            newForm.setRejectReasons(DisplayListService.getInstance()
                    .getNumberedListWithLeadingBlank(DisplayListService.ListType.REJECTION_REASONS));
            newForm.setMethods(DisplayListService.getInstance().getList(ListType.METHODS));

            // load testSections for drop down
        }
        newForm.setDisplayTestSections(false);
        newForm.setSearchByRange(true);
        // System.out.println("Post:ReactLogbookResultsController:call
        // getLogbookResults");
        return getLogbookResults(request, newForm, doRange, finished);
    }

    private LogbookResultsForm getLogbookResults(HttpServletRequest request, LogbookResultsForm form, boolean doRange,
            boolean finished) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        // boolean useTechnicianName = ConfigurationProperties.getInstance()
        // .isPropertyValueEqual(Property.resultTechnicianName, "true");
        // boolean alwaysValidate = ConfigurationProperties.getInstance()
        // .isPropertyValueEqual(Property.ALWAYS_VALIDATE_RESULTS, "true");
        // boolean supportReferrals =
        // FormFields.getInstance().useField(Field.ResultsReferral);
        // String statusRuleSet =
        // ConfigurationProperties.getInstance().getPropertyValueUpperCase(Property.StatusRules);

        request.getSession().setAttribute(SAVE_DISABLED, TRUE);

        List<TestResultItem> tests;
        List<TestResultItem> filteredTests = new ArrayList<>();

        ResultsPaging paging = new ResultsPaging();
        List<InventoryKitItem> inventoryList = new ArrayList<>();
        ResultsLoadUtility resultsLoadUtility = SpringContext.getBean(ResultsLoadUtility.class);
        resultsLoadUtility.setSysUser(getSysUserId(request));

        String requestedPage = request.getParameter("page");

        if (GenericValidator.isBlankOrNull(requestedPage)) {
            requestedPage = "1";
            new StatusRules().setAllowableStatusForLoadingResults(resultsLoadUtility);

            if (!GenericValidator.isBlankOrNull(form.getTestSectionId())) {
                tests = resultsLoadUtility.getUnfinishedTestResultItemsInTestSection(form.getTestSectionId());
                filteredTests = userService.filterResultsByLabUnitRoles(getSysUserId(request), tests,
                        Constants.ROLE_RESULTS);
                int count = resultsLoadUtility.getTotalCountAnalysisByTestSectionAndStatus(form.getTestSectionId());
                request.setAttribute("analysisCount", count);
                request.setAttribute("pageSize", filteredTests.size());

                TestSection ts = null;
                if (!GenericValidator.isBlankOrNull(form.getTestSectionId())) {
                    ts = testSectionService.get(form.getTestSectionId());
                }
                setRequestType(ts == null ? MessageUtil.getMessage("workplan.unit.types") : ts.getLocalizedName());

                if (ts != null) {
                    // this does not look right what happens after a new page!!!
                    boolean isHaitiClinical = ConfigurationProperties.getInstance()
                            .isPropertyValueEqual(Property.configurationName, "Haiti Clinical");
                    if (resultsLoadUtility.inventoryNeeded()
                            || (isHaitiClinical && ("VCT").equals(ts.getTestSectionName()))) {
                        InventoryUtility inventoryUtility = SpringContext.getBean(InventoryUtility.class);
                        inventoryList = inventoryUtility.getExistingActiveInventory();

                        form.setDisplayTestKit(true);
                    }
                }
                form.setSearchFinished(true);
            } else if (!GenericValidator.isBlankOrNull(form.getAccessionNumber())) {
                tests = resultsLoadUtility.getUnfinishedTestResultItemsByAccession(form.getAccessionNumber(), doRange, finished);
                filteredTests = userService.filterResultsByLabUnitRoles(getSysUserId(request), tests,
                        Constants.ROLE_RESULTS);
                int count = resultsLoadUtility.getTotalCountAnalysisByAccessionAndStatus(form.getAccessionNumber());

                request.setAttribute("analysisCount", count);
                request.setAttribute("pageSize", filteredTests.size());
                form.setSearchFinished(true);
            } else {
                tests = new ArrayList<>();
            }

            if (ConfigurationProperties.getInstance().isPropertyValueEqual(Property.PATIENT_DATA_ON_RESULTS_BY_ROLE,
                    "true") && !userHasPermissionForModule(request, "PatientResults")) {
                for (TestResultItem resultItem : filteredTests) {
                    resultItem.setPatientInfo("---");
                }
            }

            paging.setDatabaseResults(request, form, filteredTests);

        } else {
            int requestedPageNumber = Integer.parseInt(requestedPage);
            paging.page(request, form, requestedPageNumber);
        }
        form.setDisplayTestKit(false);
        List<String> hivKits = new ArrayList<>();
        List<String> syphilisKits = new ArrayList<>();

        for (InventoryKitItem item : inventoryList) {
            if (item.getType().equals("HIV")) {
                hivKits.add(item.getInventoryLocationId());
            } else {
                syphilisKits.add(item.getInventoryLocationId());
            }
        }
        form.setHivKits(hivKits);
        form.setSyphilisKits(syphilisKits);
        form.setInventoryItems(inventoryList);
        form.setReferralOrganizations(DisplayListService.getInstance().getList(ListType.REFERRAL_ORGANIZATIONS));

        addFlashMsgsToRequest(request);

        for (TestResultItem resultItem : filteredTests) {
            resultItem.setMethods(DisplayListService.getInstance().getList(ListType.METHODS));
            resultItem.setReferralOrganizations(
                    DisplayListService.getInstance().getList(ListType.REFERRAL_ORGANIZATIONS));
            resultItem.setReferralReasons(
                    DisplayListService.getInstance().getList(DisplayListService.ListType.REFERRAL_REASONS));
        }

//        ObjectMapper mapper = new ObjectMapper();
//        String jsonForm = "";
//        try {
//            jsonForm = mapper.writeValueAsString(form);
//        } catch (JsonProcessingException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }

        // System.out.println("ReactLogbookResultsController:jsonForm:" + jsonForm);
        // return findForward(FWD_SUCCESS, form);
        return (form);
    }

    private String getCurrentDate() {
        Date today = Calendar.getInstance().getTime();
        return DateUtil.formatDateAsText(today);

    }

    @PostMapping(value = "ReactLogbookResultsUpdateTest", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public void showReactLogbookResultsUpdateTest(HttpServletRequest request,
            @Validated(LogbookResultsForm.LogbookResults.class) @RequestBody LogbookResultsForm Form,
            BindingResult result) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        // System.out.println("ReactLogbookResultsController:Form:" + Form.toString());

//        JSONParser parser = new JSONParser();
//        JSONObject jsonObject = new JSONObject();
//        try {
//            jsonObject = (JSONObject) parser.parse("{\"formName\":\"LogbookResultsForm\",\"formAction\":null,\"formMethod\":\"POST\",\"cancelAction\":\"Home\",\"submitOnCancel\":false,\"cancelMethod\":\"POST\",\"paging\":{\"totalPages\":\"1\",\"currentPage\":\"1\",\"searchTermToPage\":[]},\"accessionNumber\":null,\"singlePatient\":false,\"currentDate\":\"03/17/2023\",\"displayTestMethod\":true,\"displayTestKit\":false,\"testResult\":[],\"referralItems\":null,\"inventoryItems\":[],\"hivKits\":[],\"syphilisKits\":[],\"type\":\"\",\"referralReasons\":[{\"id\":\"1\",\"value\":\"Test not performed\"},{\"id\":\"2\",\"value\":\"Confirmation requested\"},{\"id\":\"3\",\"value\":\"Further testing required\"},{\"id\":\"4\",\"value\":\"Reagent expired\"},{\"id\":\"5\",\"value\":\"Reagents unavailable\"},{\"id\":\"6\",\"value\":\"Equipment failure\"},{\"id\":\"7\",\"value\":\"Verification of EQA\"},{\"id\":\"8\",\"value\":\"Specimen sent for serotyping\"},{\"id\":\"9\",\"value\":\"EQA by Repeat Testing\"},{\"id\":\"10\",\"value\":\"Other\"}],\"rejectReasons\":[{\"id\":\"0\",\"value\":\"\"},{\"id\":\"1140\",\"value\":\"1. Measurement not available due to technical problem. Please submit another sample.\"},{\"id\":\"1141\",\"value\":\"2. Sample / request form not / misidentified . Please submit another sample.\"},{\"id\":\"1142\",\"value\":\"3. Sample / request form incompatible . Please submit another sample.\"},{\"id\":\"1143\",\"value\":\"4. Free sample request form or vice versa. Please submit another sample.\"},{\"id\":\"1144\",\"value\":\"5. Sample transported / stored incorrectly. Please submit another sample.\"},{\"id\":\"1145\",\"value\":\"6. Identification of improper sample. Please submit another sample.\"},{\"id\":\"1146\",\"value\":\"7. Sample was collected in the wrong tube. Please submit another sample.\"},{\"id\":\"1147\",\"value\":\"8. Incorrect quantity of the sample. Please submit another sample.\"},{\"id\":\"1148\",\"value\":\"9. The sample received is not suitable for test requested . Please submit the appropriate sample.\"},{\"id\":\"1149\",\"value\":\"10. The sample received is coagulated . Please submit another sample.\"},{\"id\":\"1150\",\"value\":\"11. The received sample was refrigerated . Please submit another sample.\"},{\"id\":\"1151\",\"value\":\"12. Tube received arrived broken or spilled . Please submit another sample.\"},{\"id\":\"1152\",\"value\":\"13. The received sample is hemolyzed . Please submit another sample.\"},{\"id\":\"1160\",\"value\":\"14. The sample received is jaundiced. Please submit another sample.\"},{\"id\":\"1161\",\"value\":\"15. The sample received is lipemic. Please submit another sample.\"},{\"id\":\"1153\",\"value\":\"16. The sample is contaminated. Please submit another sample.\"},{\"id\":\"1154\",\"value\":\"17. The sample received was insufficient volume . Please submit another sample.\"},{\"id\":\"1155\",\"value\":\"18. Following a mistake in sample intake, a not requested test has been scheduled but not executed.\"},{\"id\":\"1162\",\"value\":\"19. Other.\"}],\"testSections\":null,\"testSectionsByName\":null,\"referralOrganizations\":[{\"id\":\"3210\",\"value\":\"CEDRES\"},{\"id\":\"3131\",\"value\":\"CENTRAL HEALTH LABORATORY\"},{\"id\":\"3219\",\"value\":\"CIRBA\"},{\"id\":\"2878\",\"value\":\"PROJECT RETROCI\"},{\"id\":\"3224\",\"value\":\"SSR AIRPORT LAB\"},{\"id\":\"3614\",\"value\":\"Test referral lab\"},{\"id\":\"2769\",\"value\":\"Training Reflab\"}],\"methods\":[{\"id\":\"39\",\"value\":\"EIA\"},{\"id\":\"40\",\"value\":\"PCR\"},{\"id\":\"41\",\"value\":\"STAIN\"},{\"id\":\"42\",\"value\":\"CULTURE\"},{\"id\":\"43\",\"value\":\"PROBE\"},{\"id\":\"44\",\"value\":\"BIOCHEMICAL\"},{\"id\":\"45\",\"value\":\"Diane Test\"},{\"id\":\"46\",\"value\":\"HPLC\"},{\"id\":\"47\",\"value\":\"DNA SEQUENCING\"},{\"id\":\"48\",\"value\":\"AUTO\"},{\"id\":\"49\",\"value\":\"MANUAL\"},{\"id\":\"50\",\"value\":\"HIV_TEST_KIT\"},{\"id\":\"51\",\"value\":\"SYPHILIS_TEST_KIT\"}],\"methodsByName\":null,\"displayMethods\":true,\"methodId\":null,\"testSectionId\":null,\"displayTestSections\":false,\n"
//                    + "\"searchByRange\":true,\"searchFinished\":false}");
//        } catch (Exception e) {
//            
//            System.out.println("Post:ReactLogbookResultsController:ERROR:" + e.toString());
//        }

        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        String jsonString = new String();
//        //mapper.registerModule(new ParameterNamesModule(Mode.PROPERTIES));
//       
//        LogbookResultsForm form = new LogbookResultsForm();
//
        try {
            // form = mapper.readValue(Form.toJSONString(), LogbookResultsForm.class);
            jsonString = mapper.writeValueAsString(Form);
        } catch (Exception e) {
            System.out.println("Post:ReactLogbookResultsController:ERROR:" + e.toString());
        }
//        //System.out.println("Post:ReactLogbookResultsController:form:" + form.toString());
        System.out.println("Post:ReactLogbookResultsController:SUCCESS" + jsonString);

        // return("");
    }

//    @RequestMapping(value = { "/ReactLogbookResults", "/PatientResults", "/AccessionResults",
//            "/StatusResults" }, method = RequestMethod.POST)
//    public LogbookResultsForm showReactLogbookResultsUpdate(HttpServletRequest request,
//            @ModelAttribute("form") @Validated(LogbookResultsForm.LogbookResults.class) LogbookResultsForm form,
//            BindingResult result, RedirectAttributes redirectAttributes)
//            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

    @PostMapping(value = "ReactLogbookResultsUpdate", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public LogbookResultsForm showReactLogbookResultsUpdate(HttpServletRequest request,
            @Validated(LogbookResultsForm.LogbookResults.class) @RequestBody LogbookResultsForm form,
            BindingResult result) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        System.out.println("Post:ReactLogbookResultsController:SUCCESS");

        boolean useTechnicianName = ConfigurationProperties.getInstance()
                .isPropertyValueEqual(Property.resultTechnicianName, "true");
        boolean alwaysValidate = ConfigurationProperties.getInstance()
                .isPropertyValueEqual(Property.ALWAYS_VALIDATE_RESULTS, "true");
        boolean supportReferrals = FormFields.getInstance().useField(Field.ResultsReferral);
        String statusRuleSet = ConfigurationProperties.getInstance().getPropertyValueUpperCase(Property.StatusRules);

        if ("true".equals(request.getParameter("pageResults"))) {
            return getLogbookResults(request, form, true, true);
        }

        if (result.hasErrors()) {
            saveErrors(result);
//            return findForward(FWD_FAIL_INSERT, form);
        }

//  gnr: shows current session records, can be current, stale/empty vs. other user
//        ie: empty when another user saved and hasn't reloaded.

        List<Result> checkPagedResults = (List<Result>) request.getSession()
                .getAttribute(IActionConstants.RESULTS_SESSION_CACHE);
        List<Result> checkResults = (List<Result>) checkPagedResults.get(0);
        if (checkResults.size() == 0) {
            LogEvent.logDebug(this.getClass().getName(), "LogbookResults()", "Attempted save of stale page.");

            List<TestResultItem> resultList = form.getTestResult();
            for (TestResultItem item : resultList) {
                item.setFailedValidation(true);
                item.setNote("Result has been saved by another user.");
            }

            ResultsUpdateDataSet actionDataSet = new ResultsUpdateDataSet(getSysUserId(request));
            actionDataSet.filterModifiedItems(form.getTestResult());

            Errors errors = actionDataSet.validateModifiedItems();

            if (true) {
                saveErrors(errors);
//                return findForward(FWD_VALIDATION_ERROR, form);
            }
        }

        List<IResultUpdate> updaters = ResultUpdateRegister.getRegisteredUpdaters();

        // gnr
        // if (true) return(form);

        ResultsPaging paging = new ResultsPaging();
        paging.updatePagedResults(request, form);
        List<TestResultItem> tests = paging.getResults(request);

        ResultsUpdateDataSet actionDataSet = new ResultsUpdateDataSet(getSysUserId(request));
        actionDataSet.filterModifiedItems(tests);

        Errors errors = actionDataSet.validateModifiedItems();

        if (errors.hasErrors()) {
            saveErrors(errors);
//            return findForward(FWD_VALIDATION_ERROR, form);
        }

        createResultsFromItems(actionDataSet, supportReferrals, alwaysValidate, useTechnicianName, statusRuleSet);
        createAnalysisOnlyUpdates(actionDataSet);

        try {
            List<Analysis> reflexAnalysises = logbookPersistService.persistDataSet(actionDataSet, updaters,
                    getSysUserId(request));
//            redirectAttributes.addFlashAttribute(REFLEX_ACCESSIONS, reflexAnalysises.stream()
//                    .map(e -> analysisService.getOrderAccessionNumber(e)).collect(Collectors.toList()));
            try {
                fhirTransformService.transformPersistResultsEntryFhirObjects(actionDataSet);
            } catch (FhirTransformationException | FhirPersistanceException e) {
                LogEvent.logError(e);
            }
        } catch (LIMSRuntimeException e) {
            String errorMsg;
            if (e.getException() instanceof StaleObjectStateException) {
                errorMsg = "errors.OptimisticLockException";
            } else {
                LogEvent.logDebug(e);
                errorMsg = "errors.UpdateException";
            }

            errors.reject(errorMsg, errorMsg);
            saveErrors(errors);
//            return findForward(FWD_FAIL_INSERT, form);

        }

        for (IResultUpdate updater : updaters) {
            try {
                updater.postTransactionalCommitUpdate(actionDataSet);
            } catch (Exception e) {
                LogEvent.logError(this.getClass().getName(), "showLogbookResultsUpdate",
                        "error doing a post transactional commit");
                LogEvent.logError(e);
            }
        }

//        redirectAttributes.addFlashAttribute(FWD_SUCCESS, true);
        if (GenericValidator.isBlankOrNull(form.getType())) {
//            return findForward(FWD_SUCCESS_INSERT, form);
        } else {
            Map<String, String> params = new HashMap<>();
            params.put("type", form.getType());
//            return getForwardWithParameters(findForward(FWD_SUCCESS_INSERT, form), params);
        }
        // added return
        return (form);
    }

    private void createAnalysisOnlyUpdates(ResultsUpdateDataSet actionDataSet) {
        for (TestResultItem testResultItem : actionDataSet.getAnalysisOnlyChangeResults()) {

            Analysis analysis = analysisService.get(testResultItem.getAnalysisId());
            analysis.setSysUserId(getSysUserId(request));
            analysis.setCompletedDate(DateUtil.convertStringDateToSqlDate(testResultItem.getTestDate()));
            if (testResultItem.getAnalysisMethod() != null) {
                analysis.setAnalysisType(testResultItem.getAnalysisMethod());
            }
            actionDataSet.getModifiedAnalysis().add(analysis);
        }
    }

    private void createResultsFromItems(ResultsUpdateDataSet actionDataSet, boolean supportReferrals,
            boolean alwaysValidate, boolean useTechnicianName, String statusRuleSet) {

        for (TestResultItem testResultItem : actionDataSet.getModifiedItems()) {

            Analysis analysis = analysisService.get(testResultItem.getAnalysisId());
            analysis.setStatusId(getStatusForTestResult(testResultItem, alwaysValidate));
            analysis.setSysUserId(getSysUserId(request));
            actionDataSet.getModifiedAnalysis().add(analysis);

            actionDataSet.addToNoteList(noteService.createSavableNote(analysis, NoteType.INTERNAL,
                    testResultItem.getNote(), RESULT_SUBJECT, getSysUserId(request)));

            if (testResultItem.isShadowRejected()) {
                String rejectedReasonId = testResultItem.getRejectReasonId();
                for (IdValuePair rejectReason : DisplayListService.getInstance().getList(ListType.REJECTION_REASONS)) {
                    if (rejectedReasonId.equals(rejectReason.getId())) {
                        actionDataSet.addToNoteList(noteService.createSavableNote(analysis, NoteType.REJECTION_REASON,
                                rejectReason.getValue(), RESULT_SUBJECT, getSysUserId(request)));
                        break;
                    }
                }
            }

            ResultSaveBean bean = ResultSaveBeanAdapter.fromTestResultItem(testResultItem);
            ResultSaveService resultSaveService = new ResultSaveService(analysis, getSysUserId(request));
            // deletable Results will be written to, not read
            List<Result> results = resultSaveService.createResultsFromTestResultItem(bean,
                    actionDataSet.getDeletableResults());

            analysis.setCorrectedSincePatientReport(
                    resultSaveService.isUpdatedResult() && analysisService.patientReportHasBeenDone(analysis));

            if (analysisService.hasBeenCorrectedSinceLastPatientReport(analysis)) {
                actionDataSet.addToNoteList(noteService.createSavableNote(analysis, NoteType.EXTERNAL,
                        MessageUtil.getMessage("note.corrected.result"), RESULT_SUBJECT, getSysUserId(request)));
            }

            // If there is more than one result then each user selected reflex gets mapped
            // to that result
            for (Result result : results) {
                addResult(result, testResultItem, analysis, results.size() > 1, actionDataSet, useTechnicianName);

                if (analysisShouldBeUpdated(testResultItem, result, supportReferrals)) {
                    updateAnalysis(testResultItem, testResultItem.getTestDate(), analysis, statusRuleSet);
                }
            }
            if (supportReferrals && testResultItem.isRefer()) {
                handleReferrals(testResultItem, testResultItem.getReferralItem(), results, analysis, actionDataSet);
            }
        }
    }

    private void handleReferrals(TestResultItem testResultItem, ReferralItem referralItem, List<Result> results,
            Analysis analysis, ResultsUpdateDataSet actionDataSet) {
//        List<Referral> referrals = new ArrayList<>();
        Referral referral = new Referral();
        referral.setFhirUuid(UUID.randomUUID());
        referral.setStatus(ReferralStatus.SENT);
        referral.setSysUserId(actionDataSet.getCurrentUserId());
        referral.setReferralTypeId(REFERRAL_CONFORMATION_ID);
        referral.setRequesterName(testResultItem.getTechnician());

        referral.setRequestDate(new Timestamp(new Date().getTime()));
        referral.setSentDate(DateUtil.convertStringDateToTruncatedTimestamp(referralItem.getReferredSendDate()));
        referral.setRequesterName(referralItem.getReferrer());
        referral.setOrganization(organizationService.get(referralItem.getReferredInstituteId()));
        referral.setAnalysis(analysis);

        referral.setReferralReasonId(referralItem.getReferralReasonId());

//            referralService.insert(referral);
//        referrals.add(referral);
        ReferralResult referralResult = new ReferralResult();
        referralResult.setReferralId(referral.getId());
        referralResult.setSysUserId(actionDataSet.getCurrentUserId());
        referralResult.setTestId(referralItem.getReferredTestId());
        if (results.size() == 1) {
            referralResult.setResult(results.get(0));
        }
//            referralResult.setResult(result);
//            referralResultService.insert(referralResult);

        ReferralSet referralSet = new ReferralSet();
        referralSet.setReferral(referral);
        referralSet.getExistingReferralResults().add(referralResult);
        actionDataSet.getSavableReferralSets().add(referralSet);

        String originalResultNote = MessageUtil.getMessage("referral.original.result") + ": ";
        if (TypeOfTestResultServiceImpl.ResultType.isDictionaryVariant(testResultItem.getResultType())
                || TypeOfTestResultServiceImpl.ResultType.isMultiSelectVariant(testResultItem.getResultType())) {
            if ("0".equals(testResultItem.getResultValue())) {
                originalResultNote = originalResultNote + "";
            } else {
                Dictionary dictionary = dictionaryService.get(testResultItem.getResultValue());
                if (dictionary.getLocalizedDictionaryName() == null) {
                    originalResultNote = originalResultNote + dictionary.getDictEntry();
                } else {
                    originalResultNote = originalResultNote
                            + dictionary.getLocalizedDictionaryName().getLocalizedValue();
                }
            }
        } else {
            originalResultNote = originalResultNote + testResultItem.getResultValue();
        }

        actionDataSet.addToNoteList(noteService.createSavableNote(analysis, NoteType.INTERNAL, originalResultNote,
                RESULT_SUBJECT, this.getSysUserId(request)));

    }

    protected boolean analysisShouldBeUpdated(TestResultItem testResultItem, Result result, boolean supportReferrals) {
        return result != null && !GenericValidator.isBlankOrNull(result.getValue())
                || (supportReferrals && ResultUtil.isReferred(testResultItem))
                || ResultUtil.isForcedToAcceptance(testResultItem) || testResultItem.isShadowRejected();
    }

    private void addResult(Result result, TestResultItem testResultItem, Analysis analysis,
            boolean multipleResultsForAnalysis, ResultsUpdateDataSet actionDataSet, boolean useTechnicianName) {
        boolean newResult = result.getId() == null;
        boolean newAnalysisInLoop = analysis != actionDataSet.getPreviousAnalysis();

        ResultSignature technicianResultSignature = null;

        if (useTechnicianName && newAnalysisInLoop) {
            technicianResultSignature = createTechnicianSignatureFromResultItem(testResultItem);
        }

        ResultInventory testKit = createTestKitLinkIfNeeded(testResultItem, ResultsLoadUtility.TESTKIT);

        analysis.setReferredOut(testResultItem.isReferredOut());
        analysis.setEnteredDate(DateUtil.getNowAsTimestamp());

        if (newResult) {
            analysis.setEnteredDate(DateUtil.getNowAsTimestamp());
            analysis.setRevision("1");
        } else if (newAnalysisInLoop) {
            analysis.setRevision(String.valueOf(Integer.parseInt(analysis.getRevision()) + 1));
        }

        SampleService sampleService = SpringContext.getBean(SampleService.class);
        Sample sample = sampleService.getSampleByAccessionNumber(testResultItem.getAccessionNumber());
        Patient patient = sampleService.getPatient(sample);

        Map<String, List<String>> triggersToReflexesMap = new HashMap<>();

        getSelectedReflexes(testResultItem.getReflexJSONResult(), triggersToReflexesMap);

        if (newResult) {
            actionDataSet.getNewResults().add(new ResultSet(result, technicianResultSignature, testKit, patient, sample,
                    triggersToReflexesMap, multipleResultsForAnalysis));
        } else {
            actionDataSet.getModifiedResults().add(new ResultSet(result, technicianResultSignature, testKit, patient,
                    sample, triggersToReflexesMap, multipleResultsForAnalysis));
        }

        actionDataSet.setPreviousAnalysis(analysis);
    }

    private void getSelectedReflexes(String reflexJSONResult, Map<String, List<String>> triggersToReflexesMap) {
        if (!GenericValidator.isBlankOrNull(reflexJSONResult)) {
            JSONParser parser = new JSONParser();
            try {
                JSONObject jsonResult = (JSONObject) parser.parse(reflexJSONResult.replaceAll("'", "\""));

                for (Object compoundReflexes : jsonResult.values()) {
                    if (compoundReflexes != null) {
                        String triggerIds = (String) ((JSONObject) compoundReflexes).get("triggerIds");
                        List<String> selectedReflexIds = new ArrayList<>();
                        JSONArray selectedReflexes = (JSONArray) ((JSONObject) compoundReflexes).get("selected");
                        for (Object selectedReflex : selectedReflexes) {
                            selectedReflexIds.add(((String) selectedReflex));
                        }
                        triggersToReflexesMap.put(triggerIds.trim(), selectedReflexIds);
                    }
                }
            } catch (ParseException e) {
                LogEvent.logDebug(e);
            }
        }
    }

    private String getStatusForTestResult(TestResultItem testResult, boolean alwaysValidate) {
        if (testResult.isShadowRejected() && ConfigurationProperties.getInstance()
                .isPropertyValueEqual(Property.VALIDATE_REJECTED_TESTS, "true")) {
            return SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.TechnicalRejected);
        } else if (testResult.isShadowRejected()) {
            return SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.Canceled);
        } else if (alwaysValidate || !testResult.isValid() || ResultUtil.isForcedToAcceptance(testResult)) {
            return SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.TechnicalAcceptance);
        } else if (noResults(testResult.getShadowResultValue(), testResult.getMultiSelectResultValues(),
                testResult.getResultType())) {
            return SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.NotStarted);
        } else {
            if (!GenericValidator.isBlankOrNull(testResult.getResultLimitId())) {
                ResultLimit resultLimit = resultLimitService.get(testResult.getResultLimitId());
                if (resultLimit.isAlwaysValidate()) {
                    return SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.TechnicalAcceptance);
                }
                if (TypeOfTestResultServiceImpl.ResultType.DICTIONARY.matches(testResult.getResultType())
                        && !testResult.getResultValue().equals(resultLimit.getDictionaryNormalId())) {
                    return SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.TechnicalAcceptance);
                }
            }

            return SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.Finalized);
        }
    }

    private boolean noResults(String value, String multiSelectValue, String type) {

        return (GenericValidator.isBlankOrNull(value) && GenericValidator.isBlankOrNull(multiSelectValue))
                || (TypeOfTestResultServiceImpl.ResultType.DICTIONARY.matches(type) && "0".equals(value));
    }

    private ResultInventory createTestKitLinkIfNeeded(TestResultItem testResult, String testKitName) {
        ResultInventory testKit = null;

        if ((TestResultItem.ResultDisplayType.SYPHILIS.toString() == testResult.getResultDisplayType()
                || TestResultItem.ResultDisplayType.HIV.toString() == testResult.getResultDisplayType())
                && ResultsLoadUtility.TESTKIT.equals(testKitName)) {

            testKit = createTestKit(testResult, testKitName, testResult.getTestKitId());
        }

        return testKit;
    }

    private ResultInventory createTestKit(TestResultItem testResult, String testKitName, String testKitId)
            throws LIMSRuntimeException {
        ResultInventory testKit;
        testKit = new ResultInventory();

        if (!GenericValidator.isBlankOrNull(testKitId)) {
            testKit.setId(testKitId);
            testKit = resultInventoryService.get(testKitId);
        }

        testKit.setInventoryLocationId(testResult.getTestKitInventoryId());
        testKit.setDescription(testKitName);
        testKit.setSysUserId(getSysUserId(request));
        return testKit;
    }

    private void updateAnalysis(TestResultItem testResultItem, String testDate, Analysis analysis,
            String statusRuleSet) {
        if (testResultItem.getAnalysisMethod() != null) {
            analysis.setAnalysisType(testResultItem.getAnalysisMethod());
        }
        analysis.setStartedDateForDisplay(testDate);

        // This needs to be refactored -- part of the logic is in
        // getStatusForTestResult. RetroCI over rides to whatever was set before
        if (statusRuleSet.equals(STATUS_RULES_RETROCI)) {
            if (!SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.Canceled)
                    .equals(analysis.getStatusId())) {
                analysis.setCompletedDate(DateUtil.convertStringDateToSqlDate(testDate));
                analysis.setStatusId(
                        SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.TechnicalAcceptance));
            }
        } else if (SpringContext.getBean(IStatusService.class).matches(analysis.getStatusId(), AnalysisStatus.Finalized)
                || SpringContext.getBean(IStatusService.class).matches(analysis.getStatusId(),
                        AnalysisStatus.TechnicalAcceptance)
                || (analysis.isReferredOut()
                        && !GenericValidator.isBlankOrNull(testResultItem.getShadowResultValue()))) {
            analysis.setCompletedDate(DateUtil.convertStringDateToSqlDate(testDate));
        }

    }

    private ResultSignature createTechnicianSignatureFromResultItem(TestResultItem testResult) {
        ResultSignature sig = null;

        // The technician signature may be blank if the user changed a
        // conclusion and then changed it back. It will be dirty
        // but will not need a signature
        if (!GenericValidator.isBlankOrNull(testResult.getTechnician())) {
            sig = new ResultSignature();

            if (!GenericValidator.isBlankOrNull(testResult.getTechnicianSignatureId())) {
                sig = resultSigService.get(testResult.getTechnicianSignatureId());
            }

            sig.setIsSupervisor(false);
            sig.setNonUserName(testResult.getTechnician());

            sig.setSysUserId(getSysUserId(request));
        }
        return sig;
    }

    private String findLogBookForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "resultsLogbookDefinition";
        } else if (FWD_SUCCESS_INSERT.equals(forward)) {
            return "redirect:/LogbookResults";
        } else if (FWD_VALIDATION_ERROR.equals(forward)) {
            return "resultsLogbookDefinition";
        } else if (FWD_FAIL_INSERT.equals(forward)) {
            return "resultsLogbookDefinition";
        } else {
            return "PageNotFound";
        }
    }

    private String findAccessionForward(String forward) {
        if (FWD_SUCCESS_INSERT.equals(forward)) {
            return "redirect:/AccessionResults";
        } else if (FWD_VALIDATION_ERROR.equals(forward)) {
            return "accessionResultDefinition";
        } else if (FWD_FAIL_INSERT.equals(forward)) {
            return "accessionResultDefinition";
        } else {
            return "PageNotFound";
        }
    }

    private String findPatientForward(String forward) {
        if (FWD_SUCCESS_INSERT.equals(forward)) {
            return "redirect:/PatientResults";
        } else if (FWD_VALIDATION_ERROR.equals(forward)) {
            return "patientResultDefinition";
        } else if (FWD_FAIL_INSERT.equals(forward)) {
            return "patientResultDefinition";
        } else {
            return "PageNotFound";
        }
    }

    private String findStatusForward(String forward) {
        if (FWD_SUCCESS_INSERT.equals(forward)) {
            return "redirect:/StatusResults?blank=true";
        } else if (FWD_VALIDATION_ERROR.equals(forward)) {
            return "statusResultDefinition";
        } else if (FWD_FAIL_INSERT.equals(forward)) {
            return "statusResultDefinition";
        } else {
            return "PageNotFound";
        }
    }

    private String findRangeForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "resultsLogbookDefinition";
        } else if (FWD_SUCCESS_INSERT.equals(forward)) {
            return "redirect:/RangeResults";
        } else if (FWD_VALIDATION_ERROR.equals(forward)) {
            return "resultsLogbookDefinition";
        } else if (FWD_FAIL_INSERT.equals(forward)) {
            return "resultsLogbookDefinition";
        } else {
            return "PageNotFound";
        }
    }

    @Override
    protected String findLocalForward(String forward) {
        if (request.getRequestURL().indexOf("RangeResults") >= 0) {
            return findRangeForward(forward);
        } else if (request.getRequestURL().indexOf("LogbookResults") >= 0) {
            return findLogBookForward(forward);
        } else if (request.getRequestURL().indexOf("AccessionResults") >= 0) {
            return findAccessionForward(forward);
        } else if (request.getRequestURL().indexOf("PatientResults") >= 0) {
            return findPatientForward(forward);
        } else if (request.getRequestURL().indexOf("StatusResults") >= 0) {
            return findStatusForward(forward);
        } else {
            return "PageNotFound";
        }
    }
}