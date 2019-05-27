package spring.service.typeofsample;

import java.util.List;

import spring.service.common.BaseObjectService;
import us.mn.state.health.lims.typeofsample.valueholder.TypeOfSampleTest;

public interface TypeOfSampleTestService extends BaseObjectService<TypeOfSampleTest> {
	void getData(TypeOfSampleTest typeOfSampleTest);

	void deleteData(String[] typeOfSampleTestIds, String currentUserId);

	boolean insertData(TypeOfSampleTest typeOfSample);

	List<TypeOfSampleTest> getTypeOfSampleTestsForTest(String testId);

	List getPageOfTypeOfSampleTests(int startingRecNo);

	List getNextTypeOfSampleTestRecord(String id);

	List getAllTypeOfSampleTests();

	Integer getTotalTypeOfSampleTestCount();

	TypeOfSampleTest getTypeOfSampleTestForTest(String testId);

	List getPreviousTypeOfSampleRecord(String id);

	List<TypeOfSampleTest> getTypeOfSampleTestsForSampleType(String sampleTypeId);
}
