package org.openelisglobal.admin.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.transaction.Transactional;
import javax.validation.Valid;

import org.openelisglobal.admin.form.LabNumberManagementForm;
import org.openelisglobal.common.provider.validation.AccessionNumberValidatorFactory.AccessionFormat;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.service.SiteInformationServiceImpl;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LabNumberManagementController {
    
    @Autowired
    private SiteInformationService siteInformationService;


    @GetMapping("/rest/labnumbermanagement")
    public LabNumberManagementForm getValues() {
        LabNumberManagementForm form = new LabNumberManagementForm();

        form.setAlphanumPrefix(ConfigurationProperties.getInstance().getPropertyValueUpperCase(Property.ALPHANUM_ACCESSION_PREFIX));
        form.setLabNumberType(AccessionFormat.valueOf(ConfigurationProperties.getInstance().getPropertyValue(Property.AccessionFormat)));
        form.setUsePrefix("true".equals(ConfigurationProperties.getInstance().getPropertyValue(Property.USE_ALPHANUM_ACCESSION_PREFIX)));
        
        return form;
    }

    @PostMapping("/rest/labnumbermanagement")
    public LabNumberManagementForm setValues(@Valid @RequestBody LabNumberManagementForm form) {
        Map<String, String> map = new HashMap<>();

        map.put(Property.ALPHANUM_ACCESSION_PREFIX.getName(), form.getAlphanumPrefix() != null ? form.getAlphanumPrefix().toUpperCase() : "");
        map.put(Property.AccessionFormat.getName(), form.getLabNumberType().name());
        map.put(Property.USE_ALPHANUM_ACCESSION_PREFIX.getName(), form.getUsePrefix().toString());
        siteInformationService.updateSiteInformationByName(map);

        ConfigurationProperties.forceReload();
        return form;
    }
}
