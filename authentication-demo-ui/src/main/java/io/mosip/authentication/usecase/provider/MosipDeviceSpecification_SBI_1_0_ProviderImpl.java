package io.mosip.authentication.usecase.provider;

import static io.mosip.authentication.usecase.constants.AuthUIConstants.APPLICATION_ID;
import static io.mosip.authentication.usecase.constants.AuthUIConstants.APPLICATION_NAME;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;

import io.mosip.authentication.usecase.config.AppConfig;
import io.mosip.authentication.usecase.dto.DeviceException;
import io.mosip.authentication.usecase.dto.DeviceInfo;
import io.mosip.authentication.usecase.dto.MdmBioDevice;
import io.mosip.authentication.usecase.dto.MdmDeviceInfoResponse;
import io.mosip.authentication.usecase.dto.MdmSbiDeviceInfoWrapper;
import io.mosip.authentication.usecase.dto.SbiDigitalId;
import io.mosip.authentication.usecase.helper.MosipDeviceSpecificationHelper;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;

@Service
public class MosipDeviceSpecification_SBI_1_0_ProviderImpl {
//public class MosipDeviceSpecification_SBI_1_0_ProviderImpl implements MosipDeviceSpecificationProvider {

	private static final Logger LOGGER = AppConfig.getLogger(MosipDeviceSpecification_SBI_1_0_ProviderImpl.class);
	private static final String SPEC_VERSION = "1.0";
	private static final String loggerClassName = "MosipDeviceSpecification_SBI_1_0_ProviderImpl";
	
	@Value("${mdm.trust.domain.digitalId:FTM}")
	private String digitalIdTrustDomain = "FTM";
	
	@Autowired
	private MosipDeviceSpecificationHelper mosipDeviceSpecificationHelper;
	
	@Autowired
	private ProviderHelper providerHelper;
	
//	@Override
	public String getSpecVersion() {
		return SPEC_VERSION;
	}
	
//	@Override
	public List<MdmBioDevice> getMdmDevices(String deviceInfoResponse, int port) {
		LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
				"received device info response on port : " + port);
		List<MdmBioDevice> mdmBioDevices = new LinkedList<>();
		List<MdmDeviceInfoResponse> deviceInfoResponses;

		try {
			LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID, "parsing device info response to SBI 1_0 dto");
			deviceInfoResponses = (mosipDeviceSpecificationHelper.getMapper().readValue(deviceInfoResponse,
					new TypeReference<List<MdmDeviceInfoResponse>>() {
					}));

			for (MdmDeviceInfoResponse mdmDeviceInfoResponse : deviceInfoResponses) {
				if (mdmDeviceInfoResponse.getDeviceInfo() != null && !mdmDeviceInfoResponse.getDeviceInfo().isEmpty()) {
					DeviceInfo deviceInfo = mosipDeviceSpecificationHelper
							.getDeviceInfoDecoded(mdmDeviceInfoResponse.getDeviceInfo(), this.getClass());
					MdmBioDevice bioDevice = getBioDevice((MdmSbiDeviceInfoWrapper)deviceInfo);
					if (bioDevice != null) {
						bioDevice.setPort(port);
						mdmBioDevices.add(bioDevice);
					}
				}
			}
		} catch (Exception exception) {
			LOGGER.error(APPLICATION_NAME, APPLICATION_ID, "Exception while parsing deviceinfo response(SBI 1_0 spec)",
					ExceptionUtils.getStackTrace(exception));
		}
		return mdmBioDevices;
	}

	private MdmBioDevice getBioDevice(MdmSbiDeviceInfoWrapper deviceSbiInfo)
			throws IOException, DeviceException {
		MdmBioDevice bioDevice = null;
		if (deviceSbiInfo.deviceInfo != null) {
			SbiDigitalId sbiDigitalId = getSbiDigitalId(deviceSbiInfo.deviceInfo.getDigitalId());
			bioDevice = new MdmBioDevice();
			bioDevice.setFirmWare(deviceSbiInfo.deviceInfo.getFirmware());
			bioDevice.setCertification(deviceSbiInfo.deviceInfo.getCertification());
			bioDevice.setSerialVersion(deviceSbiInfo.deviceInfo.getServiceVersion());
			bioDevice.setSpecVersion(providerHelper.getLatestSpecVersion(deviceSbiInfo.deviceInfo.getSpecVersion()));
			bioDevice.setPurpose(deviceSbiInfo.deviceInfo.getPurpose());

			bioDevice.setDeviceSubType(sbiDigitalId.getDeviceSubType());
			bioDevice.setDeviceType(sbiDigitalId.getType());
			bioDevice.setTimestamp(sbiDigitalId.getDateTime());
			bioDevice.setDeviceProviderName(sbiDigitalId.getDeviceProvider());
			bioDevice.setDeviceProviderId(sbiDigitalId.getDeviceProviderId());
			bioDevice.setDeviceModel(sbiDigitalId.getModel());
			bioDevice.setDeviceMake(sbiDigitalId.getMake());

			bioDevice.setSerialNumber(sbiDigitalId.getSerialNo());
			bioDevice.setCallbackId(deviceSbiInfo.deviceInfo.getCallbackId());
		}
		return bioDevice;
	}
	
	private SbiDigitalId getSbiDigitalId(String digitalId) throws IOException, DeviceException {
		mosipDeviceSpecificationHelper.validateJWTResponse(digitalId, digitalIdTrustDomain);
		return mosipDeviceSpecificationHelper.getMapper().readValue(
				new String(CryptoUtil.decodeURLSafeBase64(mosipDeviceSpecificationHelper.getPayLoad(digitalId))),
				SbiDigitalId.class);
	}
	
}
