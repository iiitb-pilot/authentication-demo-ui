package io.mosip.authentication.usecase.provider;

import static io.mosip.authentication.usecase.constants.AuthUIConstants.APPLICATION_ID;
import static io.mosip.authentication.usecase.constants.AuthUIConstants.APPLICATION_NAME;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.mosip.authentication.usecase.config.AppConfig;
import io.mosip.kernel.core.logger.spi.Logger;

@Component
public class ProviderHelper {

	private static final Logger LOGGER = AppConfig.getLogger(ProviderHelper.class);
	private static final String loggerClassName = "ProviderHelper";
	
	@Autowired
	private List<MosipDeviceSpecificationProvider> deviceSpecificationProviders;
	
	public String getLatestSpecVersion(String[] specVersion) {
		String latestSpecVersion = null;
		if (specVersion != null && specVersion.length > 0) {
			latestSpecVersion = specVersion[0];
			for (int index = 1; index < specVersion.length; index++) {
				latestSpecVersion = getLatestVersion(latestSpecVersion, specVersion[index]);
			}

			if (getMdsProvider(deviceSpecificationProviders, latestSpecVersion) == null) {
				List<String> specVersions = Arrays.asList(specVersion);
				specVersions.remove(latestSpecVersion);
				if (!specVersions.isEmpty()) {
					latestSpecVersion = getLatestSpecVersion(specVersions.toArray(new String[0]));
				}
			}
		}
		return latestSpecVersion;
	}
	
	private String getLatestVersion(String version1, String version2) {
		if (version1.equalsIgnoreCase(version2)) {
			return version1;
		}
		int version1Num = 0, version2Num = 0;
		for (int index = 0, limit = 0; (index < version1.length() || limit < version2.length());) {
			while (index < version1.length() && version1.charAt(index) != '.') {
				version1Num = version1Num * 10 + (version1.charAt(index) - '0');
				index++;
			}
			while (limit < version2.length() && version2.charAt(limit) != '.') {
				version2Num = version2Num * 10 + (version2.charAt(limit) - '0');
				limit++;
			}
			if (version1Num > version2Num)
				return version1;
			if (version2Num > version1Num)
				return version2;

			version1Num = version2Num = 0;
			index++;
			limit++;
		}
		return version1;
	}
	
	private MosipDeviceSpecificationProvider getMdsProvider(
			List<MosipDeviceSpecificationProvider> deviceSpecificationProviders, String specVersion) {

		LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
				"Finding MosipDeviceSpecificationProvider for spec version : " + specVersion + " in providers : "
						+ deviceSpecificationProviders);

		MosipDeviceSpecificationProvider deviceSpecificationProvider = null;

		if (deviceSpecificationProviders != null) {
			// Get Implemented provider
			for (MosipDeviceSpecificationProvider provider : deviceSpecificationProviders) {
				if (provider.getSpecVersion().equals(specVersion)) {
					deviceSpecificationProvider = provider;
					break;
				}
			}
		}
		return deviceSpecificationProvider;
	}
	
}
