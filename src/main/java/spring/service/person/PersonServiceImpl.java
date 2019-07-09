package spring.service.person;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.validator.GenericValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import spring.service.address.AddressPartService;
import spring.service.address.PersonAddressService;
import spring.service.common.BaseObjectServiceImpl;
import spring.service.dictionary.DictionaryService;
import us.mn.state.health.lims.address.valueholder.AddressPart;
import us.mn.state.health.lims.address.valueholder.PersonAddress;
import us.mn.state.health.lims.person.dao.PersonDAO;
import us.mn.state.health.lims.person.valueholder.Person;

@Service
@DependsOn({ "springContext" })
public class PersonServiceImpl extends BaseObjectServiceImpl<Person, String> implements PersonService {

	private Map<String, String> addressPartIdToNameMap;

	@Autowired
	protected PersonDAO baseObjectDAO;

	@Autowired
	private DictionaryService dictionaryService;
	@Autowired
	private AddressPartService addressPartService;
	@Autowired
	private PersonAddressService personAddressService;

	@PostConstruct
	private void initializeGlobalVariables() {
		addressPartIdToNameMap = new HashMap<>();
		List<AddressPart> parts = addressPartService.getAll();

		for (AddressPart part : parts) {
			addressPartIdToNameMap.put(part.getId(), part.getPartName());
		}
	}

	public PersonServiceImpl() {
		super(Person.class);
	}

	@Override
	protected PersonDAO getBaseObjectDAO() {
		return baseObjectDAO;
	}

	@Override
	@Transactional(readOnly = true)
	public String getFirstName(Person person) {
		return person != null ? person.getFirstName() : "";
	}

	@Override
	@Transactional(readOnly = true)
	public String getLastName(Person person) {
		return person != null ? person.getLastName() : "";
	}

	@Override
	@Transactional(readOnly = true)
	public String getLastFirstName(Person person) {
		String lastName = getLastName(person);
		String firstName = getFirstName(person);
		if (!GenericValidator.isBlankOrNull(lastName) && !GenericValidator.isBlankOrNull(firstName)) {
			lastName += ", ";
		}

		lastName += firstName;

		return lastName;

	}

	@Override
	public Map<String, String> getAddressComponents(Person person) {
		String value;
		Map<String, String> addressMap = new HashMap<>();

		if (person == null) {
			return addressMap;
		}

		List<PersonAddress> addressParts = personAddressService.getAddressPartsByPersonId(person.getId());

		for (PersonAddress parts : addressParts) {
			if ("D".equals(parts.getType()) && !GenericValidator.isBlankOrNull(parts.getValue())) {
				addressMap.put(addressPartIdToNameMap.get(parts.getAddressPartId()),
						dictionaryService.getDataForId(parts.getValue()).getDictEntry());
			} else {
				value = parts.getValue();
				addressMap.put(addressPartIdToNameMap.get(parts.getAddressPartId()), value == null ? "" : value.trim());
			}
		}

		value = person.getCity();
		addressMap.put("City", value == null ? "" : value.trim());
		value = person.getCountry();
		addressMap.put("Country", value == null ? "" : value.trim());
		value = person.getState();
		addressMap.put("State", value == null ? "" : value.trim());
		value = person.getStreetAddress();
		addressMap.put("Street", value == null ? "" : value.trim());
		value = person.getZipCode();
		addressMap.put("Zip", value == null ? "" : value.trim());

		return addressMap;
	}

	@Override
	@Transactional(readOnly = true)
	public String getPhone(Person person) {
		if (person == null) {
			return "";
		}

		String phone = person.getHomePhone();

		if (GenericValidator.isBlankOrNull(phone)) {
			phone = person.getCellPhone();
		}

		if (GenericValidator.isBlankOrNull(phone)) {
			phone = person.getWorkPhone();
		}

		return phone;
	}

	@Override
	@Transactional(readOnly = true)
	public String getWorkPhone(Person person) {
		return person.getWorkPhone();
	}

	@Override
	@Transactional(readOnly = true)
	public String getCellPhone(Person person) {
		return person.getCellPhone();
	}

	@Transactional(readOnly = true)
	public String getHomePhone(Person person) {
		return person.getHomePhone();
	}

	@Override
	@Transactional(readOnly = true)
	public String getFax(Person person) {
		return person.getFax();
	}

	@Override
	@Transactional(readOnly = true)
	public String getEmail(Person person) {
		return person.getEmail();
	}

	@Override
	@Transactional(readOnly = true)
	public void getData(Person person) {
		getBaseObjectDAO().getData(person);
	}

	@Override
	@Transactional(readOnly = true)
	public List getNextPersonRecord(String id) {
		return getBaseObjectDAO().getNextPersonRecord(id);
	}

	@Override
	@Transactional(readOnly = true)
	public List getPreviousPersonRecord(String id) {
		return getBaseObjectDAO().getPreviousPersonRecord(id);
	}

	@Override
	@Transactional(readOnly = true)
	public Person getPersonByLastName(String lastName) {
		return getBaseObjectDAO().getPersonByLastName(lastName);
	}

	@Override
	@Transactional(readOnly = true)
	public List getPageOfPersons(int startingRecNo) {
		return getBaseObjectDAO().getPageOfPersons(startingRecNo);
	}

	@Override
	@Transactional(readOnly = true)
	public List getAllPersons() {
		return getBaseObjectDAO().getAllPersons();
	}

	@Override
	@Transactional(readOnly = true)
	public Person getPersonById(String personId) {
		return getBaseObjectDAO().getPersonById(personId);
	}

}