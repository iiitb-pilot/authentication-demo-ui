package io.mosip.authentication.usecase.helper;

import static io.mosip.authentication.usecase.constants.AuthUIConstants.APPLICATION_ID;
import static io.mosip.authentication.usecase.constants.AuthUIConstants.APPLICATION_NAME;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.authentication.usecase.config.AppConfig;
import io.mosip.authentication.usecase.dto.MdmBioDevice;
import io.mosip.authentication.usecase.provider.MosipDeviceSpecificationProvider;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.HMACUtils2;
import io.mosip.kernel.core.util.StringUtils;

@Service
@Configurable
public class DeviceHelper {

	private static final Logger LOGGER = AppConfig.getLogger(DeviceHelper.class);
	private static final String loggerClassName = "DeviceHelper";
	
	public static final String DEVICE_INFO_ENDPOINT = "info";
	
	@Autowired
	private List<MosipDeviceSpecificationProvider> deviceSpecificationProviders;
	
	private ObjectMapper mapper = new ObjectMapper();
	
	@Value("${mdm.connection.timeout:2000}")
	private String mdmConnectionTimeout;
	
	@Value("${mdm.start.port:4501}")
	private String mdmStartPort;
	
	@Value("${mdm.end.port:4510}")
	private String mdmEndPort;
	
	public ObjectMapper getMapper() {
		return mapper;
	}
	
	private String getRunningurl() {
		return "http://127.0.0.1";
	}
	
	private String buildUrl(int port, String endPoint) {
		return String.format("%s:%s/%s", getRunningurl(), port, endPoint);
	}
	
	private static Map<String, Map<String, MdmBioDevice>> availableDeviceInfoMap = new LinkedHashMap<>();
	
	
	public Map<String, Map<String, MdmBioDevice>> getDeviceList() {
		int startPort = Integer.parseInt((String)mdmStartPort);
		int endPort = Integer.parseInt((String)mdmEndPort);
		if (startPort >= 4500 && endPort <= 4600) {
			for (int port = startPort; port <= endPort; port++) {
				getDeviceInfo(port);
			}
		}
		return availableDeviceInfoMap;
	}
	
	public Map<String, Map<String, MdmBioDevice>> getDeviceList(Integer availablePort) {
		getDeviceInfo(availablePort);
		return availableDeviceInfoMap;
	}

	private void getDeviceInfo(Integer availablePort) {
		String url = buildUrl(availablePort,
				DEVICE_INFO_ENDPOINT);
		try {
			String deviceInfoResponse = getDeviceInfoResponse(url);
			if (StringUtils.isNotEmpty(deviceInfoResponse)) {
				System.out.println(deviceInfoResponse);
				for (MosipDeviceSpecificationProvider deviceSpecificationProvider : deviceSpecificationProviders) {
					LOGGER.debug("Decoding device info response with provider : {}", deviceSpecificationProvider);
					List<MdmBioDevice> mdmBioDevices = deviceSpecificationProvider.getMdmDevices(deviceInfoResponse,
							availablePort);
					System.out.println(mdmBioDevices);
					for (MdmBioDevice bioDevice : mdmBioDevices) {
						String deviceType = bioDevice.getDeviceType();
						String deviceSubType = bioDevice.getDeviceSubType();
						if (bioDevice != null && deviceType != null && deviceSubType != null) {
							addToDeviceInfoMap(getKey(deviceType, deviceSubType), bioDevice);
						}
					}
				}
			}
		} catch (Exception ex) {
			LOGGER.error(ex.getMessage());
		}
	}

	private String getDeviceInfoResponse(String url) throws Exception {
		Integer timeout = Integer.parseInt((String)mdmConnectionTimeout);
		RequestConfig requestConfig = RequestConfig.custom()
				.setConnectTimeout(timeout * 1000)
				.setSocketTimeout(timeout * 1000)
				.setConnectionRequestTimeout(timeout * 1000)
				.build();
		HttpUriRequest request = RequestBuilder.create("MOSIPDINFO")
				.setUri(url)
				.setConfig(requestConfig)
				.build();
		
		CloseableHttpResponse clientResponse = null;
		String response = null;

		try (CloseableHttpClient client = HttpClients.createDefault()) {
			clientResponse = client.execute(request);
			response = EntityUtils.toString(clientResponse.getEntity());
		} catch (IOException exception) {
			throw new Exception(exception.getMessage());
		}
		return response;
	}
	
	private void addToDeviceInfoMap(String key, MdmBioDevice bioDevice) {
		if (key.contains("single")) {
			String deviceMake = bioDevice.getDeviceMake();
			String deviceModel = bioDevice.getDeviceModel();
			String subKey = String.format("%s_%s", deviceMake, deviceModel);
			if (availableDeviceInfoMap.containsKey(key)) {
				availableDeviceInfoMap.get(key).put(subKey, bioDevice);
			} else {
				Map<String, MdmBioDevice> bioDeviceMap = new LinkedHashMap<String, MdmBioDevice>();
				bioDeviceMap.put(subKey, bioDevice);
				availableDeviceInfoMap.put(key, bioDeviceMap);
			}
			LOGGER.debug(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
					"Added for device into cache : " + subKey);
		}
	}
	
	private String getKey(String type, String subType) {
		return String.format("%s_%s", type.toLowerCase(), subType.toLowerCase());
	}
	
	private static int getSubstrinInt(String idvid, int substrigLen, int radix) {
		String hexSubstring = getSubstring(idvid, substrigLen);
		return Integer.parseInt(hexSubstring, radix);
	}

	private static String getSubstring(String string, int substrigLen) {
		int length = string.length();
		return length > substrigLen ? string.substring(length - substrigLen) : string;
	}
	
	public static void main(String[] args) {
//		DeviceHelper helper = new DeviceHelper();
//		helper.getDeviceInfo(4503);
//		String uin = "758028348105";
//		String salt = "N8N7WYQmVAcqUF4iawKHZw==";
//		String uin = "2014369807593412";
//		String salt = "FMo0NlN6kZbgM03WYrFSPw==";
//		try {
//			String idPlainHash = HMACUtils2.digestAsPlainText(uin.getBytes());
//			int hexToDecimal = getSubstrinInt(idPlainHash, 3, 16);
//			String decimalStr = String.valueOf(hexToDecimal);
//			System.out.println(getSubstrinInt(decimalStr, 3, 10));
//			System.out.println(HMACUtils2.digestAsPlainTextWithSalt(uin.getBytes(), salt.getBytes()));
//		} catch (NoSuchAlgorithmException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}
}
