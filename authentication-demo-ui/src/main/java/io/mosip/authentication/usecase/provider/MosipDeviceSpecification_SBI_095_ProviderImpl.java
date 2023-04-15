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
import io.mosip.authentication.usecase.constants.AuthUIConstants;
import io.mosip.authentication.usecase.dto.DeviceException;
import io.mosip.authentication.usecase.dto.DeviceInfo;
import io.mosip.authentication.usecase.dto.DigitalId;
import io.mosip.authentication.usecase.dto.MdmBioDevice;
import io.mosip.authentication.usecase.dto.MdmDeviceInfo;
import io.mosip.authentication.usecase.dto.MdmDeviceInfoResponse;
import io.mosip.authentication.usecase.helper.MosipDeviceSpecificationHelper;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;

@Service
public class MosipDeviceSpecification_SBI_095_ProviderImpl implements MosipDeviceSpecificationProvider {

	private static final Logger LOGGER = AppConfig.getLogger(MosipDeviceSpecification_SBI_095_ProviderImpl.class);
	private static final String SPEC_VERSION = "0.9.5";
	private static final String loggerClassName = "MosipDeviceSpecification_SBI_095_ProviderImpl";
	
	@Autowired
	private MosipDeviceSpecificationHelper mosipDeviceSpecificationHelper;
	
	@Value("${mdm.trust.domain.digitalId:DEVICE}")
	private String digitalIdTrustDomain;
	
	@Autowired
	private ProviderHelper providerHelper;
	
	@Override
	public String getSpecVersion() {
		return SPEC_VERSION;
	}

	@Override
	public List<MdmBioDevice> getMdmDevices(String deviceInfoResponse, int port) {
		LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
				"received device info response on port : " + port);

		List<MdmBioDevice> mdmBioDevices = new LinkedList<>();

		List<MdmDeviceInfoResponse> deviceInfoResponses;
		try {

			LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID, "parsing device info response to 095 dto");
			deviceInfoResponses = (mosipDeviceSpecificationHelper.getMapper().readValue(deviceInfoResponse,
					new TypeReference<List<MdmDeviceInfoResponse>>() {
					}));

			for (MdmDeviceInfoResponse mdmDeviceInfoResponse : deviceInfoResponses) {
				if (mdmDeviceInfoResponse.getDeviceInfo() != null && !mdmDeviceInfoResponse.getDeviceInfo().isEmpty()) {
					DeviceInfo deviceInfo = mosipDeviceSpecificationHelper
							.getDeviceInfoDecoded(mdmDeviceInfoResponse.getDeviceInfo(), this.getClass());
					MdmBioDevice bioDevice = getBioDevice((MdmDeviceInfo)deviceInfo);
					if (bioDevice != null) {
						bioDevice.setPort(port);
						mdmBioDevices.add(bioDevice);
					}
				}
			}
		} catch (Exception exception) {
			LOGGER.error(APPLICATION_NAME, APPLICATION_ID, "Exception while parsing deviceinfo response(095 spec)",
					ExceptionUtils.getStackTrace(exception));
		}
		return mdmBioDevices;
	}

	private MdmBioDevice getBioDevice(MdmDeviceInfo deviceInfo)
			throws IOException, DeviceException {

		MdmBioDevice bioDevice = null;

		if (deviceInfo != null) {

			DigitalId digitalId = getDigitalId(deviceInfo.getDigitalId());

			bioDevice = new MdmBioDevice();
			bioDevice.setDeviceId(deviceInfo.getDeviceId());
			bioDevice.setFirmWare(deviceInfo.getFirmware());
			bioDevice.setCertification(deviceInfo.getCertification());
			bioDevice.setSerialVersion(deviceInfo.getServiceVersion());
			bioDevice.setSpecVersion(providerHelper.getLatestSpecVersion(deviceInfo.getSpecVersion()));
			bioDevice.setPurpose(deviceInfo.getPurpose());
			bioDevice.setDeviceCode(deviceInfo.getDeviceCode());

			bioDevice.setDeviceSubType(digitalId.getDeviceSubType());
			bioDevice.setDeviceType(digitalId.getType());
			bioDevice.setTimestamp(digitalId.getDateTime());
			bioDevice.setDeviceProviderName(digitalId.getDeviceProvider());
			bioDevice.setDeviceProviderId(digitalId.getDeviceProviderId());
			bioDevice.setDeviceModel(digitalId.getModel());
			bioDevice.setDeviceMake(digitalId.getMake());
			bioDevice.setSerialNumber(digitalId.getSerialNo());
			bioDevice.setCallbackId(deviceInfo.getCallbackId());
		}

		LOGGER.info(AuthUIConstants.MOSIP_BIO_DEVICE_MANAGER, APPLICATION_NAME, APPLICATION_ID, "Adding Device to Registry : ");
		return bioDevice;
	}

	private DigitalId getDigitalId(String digitalId) throws IOException, DeviceException {
		mosipDeviceSpecificationHelper.validateJWTResponse(digitalId, digitalIdTrustDomain);
		return mosipDeviceSpecificationHelper.getMapper().readValue(
				new String(CryptoUtil.decodeURLSafeBase64(mosipDeviceSpecificationHelper.getPayLoad(digitalId))),
				DigitalId.class);

	}
}
