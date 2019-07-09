package spring.service.patientidentitytype;

import java.util.List;

import spring.service.common.BaseObjectService;
import us.mn.state.health.lims.patientidentitytype.valueholder.PatientIdentityType;

public interface PatientIdentityTypeService extends BaseObjectService<PatientIdentityType, String> {
	PatientIdentityType getNamedIdentityType(String name);

	List<PatientIdentityType> getAllPatientIdenityTypes();
}