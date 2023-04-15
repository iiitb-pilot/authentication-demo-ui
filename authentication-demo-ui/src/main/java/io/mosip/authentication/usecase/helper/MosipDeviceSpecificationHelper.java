package io.mosip.authentication.usecase.helper;

import static io.mosip.authentication.usecase.constants.AuthUIConstants.APPLICATION_ID;
import static io.mosip.authentication.usecase.constants.AuthUIConstants.APPLICATION_NAME;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.authentication.usecase.config.AppConfig;
import io.mosip.authentication.usecase.constants.AuthUIConstants;
import io.mosip.authentication.usecase.dto.DeviceException;
import io.mosip.authentication.usecase.dto.DeviceInfo;
import io.mosip.authentication.usecase.dto.MDMError;
import io.mosip.authentication.usecase.dto.MdmDeviceInfo;
import io.mosip.authentication.usecase.dto.MdmSbiDeviceInfoWrapper;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.signature.service.SignatureService;

@Component
public class MosipDeviceSpecificationHelper {

	private static final Logger LOGGER = AppConfig.getLogger(MosipDeviceSpecificationHelper.class);
	private static final String loggerClassName = "MosipDeviceSpecificationHelper";
	
	private ObjectMapper mapper = new ObjectMapper();
	
	@Value("${mdm.trust.domain.deviceinfo:DEVICE}")
	private String deviceInfoTrustDomain;
	
//	@Autowired
//	private SignatureService signatureService;
	
	public String getPayLoad(String data) throws DeviceException {
		if (data == null || data.isEmpty()) {
			throw new DeviceException(MDMError.MDS_JWT_INVALID.getErrorCode(),
					MDMError.MDS_JWT_INVALID.getErrorMessage());
		}
		Pattern pattern = Pattern.compile(AuthUIConstants.BIOMETRIC_SEPERATOR);
		Matcher matcher = pattern.matcher(data);
		if (matcher.find()) {
			return matcher.group(1);
		}
		throw new DeviceException(MDMError.MDS_PAYLOAD_EMPTY.getErrorCode(),
				MDMError.MDS_PAYLOAD_EMPTY.getErrorMessage());
	}
	
	public void validateJWTResponse(final String signedData, final String domain) throws DeviceException {
//		JWTSignatureVerifyRequestDto jwtSignatureVerifyRequestDto = new JWTSignatureVerifyRequestDto();
//		jwtSignatureVerifyRequestDto.setValidateTrust(true);
//		jwtSignatureVerifyRequestDto.setDomain(domain);
//		jwtSignatureVerifyRequestDto.setJwtSignatureData(signedData);
//		
//		JWTSignatureVerifyResponseDto jwtSignatureVerifyResponseDto = signatureService.jwtVerify(jwtSignatureVerifyRequestDto);
//		if(!jwtSignatureVerifyResponseDto.isSignatureValid())
//				throw new DeviceException(MDMError.MDM_INVALID_SIGNATURE.getErrorCode(), MDMError.MDM_INVALID_SIGNATURE.getErrorMessage());
//		
//		if (jwtSignatureVerifyRequestDto.getValidateTrust() && !jwtSignatureVerifyResponseDto.getTrustValid().equals(SignatureConstant.TRUST_VALID)) {
//		      throw new DeviceException(MDMError.MDM_CERT_PATH_TRUST_FAILED.getErrorCode(), MDMError.MDM_CERT_PATH_TRUST_FAILED.getErrorMessage());
//		}
	}
	
	public ObjectMapper getMapper() {
		return mapper;
	}
	
	public DeviceInfo getDeviceInfoDecoded(String deviceInfo, Class<?> classType) {
		try {
			validateJWTResponse(deviceInfo, deviceInfoTrustDomain);
			String result = new String(CryptoUtil.decodeURLSafeBase64(getPayLoad(deviceInfo)));
			if(classType.getName().equals("io.mosip.authentication.usecase.provider.MosipDeviceSpecification_SBI_1_0_ProviderImpl")) {
				return mapper.readValue(result, MdmSbiDeviceInfoWrapper.class);
			} else {
				return mapper.readValue(result, MdmDeviceInfo.class);
			}
			
		} catch (Exception exception) {
			LOGGER.error(APPLICATION_ID, APPLICATION_NAME, "Failed to decode device info",
					ExceptionUtils.getStackTrace(exception));
		}
		return null;
	}
	

}
