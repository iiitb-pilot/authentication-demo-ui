package io.mosip.authentication.usecase.helper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.MGF1ParameterSpec;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource.PSpecified;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.bouncycastle.operator.OperatorCreationException;
import org.controlsfx.control.Notifications;
import org.jose4j.lang.JoseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.mosip.authentication.demo.dto.AuthRequestDTO;
import io.mosip.authentication.demo.dto.AuthTypeDTO;
import io.mosip.authentication.demo.dto.EncryptionRequestDto;
import io.mosip.authentication.demo.dto.EncryptionResponseDto;
import io.mosip.authentication.demo.dto.OtpRequestDTO;
import io.mosip.authentication.demo.dto.RequestDTO;
import io.mosip.authentication.demo.helper.CryptoUtility;
import io.mosip.authentication.demo.service.util.KeyMgrUtil;
import io.mosip.authentication.demo.service.util.SignatureUtil;
import io.mosip.authentication.usecase.dto.AppContext;
import io.mosip.authentication.usecase.dto.AuthRequest;
import io.mosip.authentication.usecase.dto.AuthRequestBioInfo;
import io.mosip.authentication.usecase.dto.MdmBioDevice;
import io.mosip.kernel.core.http.RequestWrapper;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.util.HMACUtils2;
import javafx.geometry.Pos;
import javafx.scene.layout.AnchorPane;
import javafx.util.Duration;

@Service
@Configurable
public class IdentityCaptureHelper {

	private static final String DEFAULT_SUBID = "0";
	private static final String SSL = "SSL";
	ObjectMapper mapper = new ObjectMapper();

	@Autowired
	private Environment env;
	
	@Autowired
	private SignatureUtil signatureUtil;
	
	@Autowired
	private KeyMgrUtil keyMgrUtil;
	
	@Autowired
	private CryptoUtility cryptoUtil;
	
	@Value("${mosip.auth.ekyc.label}")
	private String[] ekycLabel;
	
	private ObjectMapper objectMapper = new ObjectMapper();
	
	private String getTransactionID() {
		return "1234567890";
	}
	
	private String getUTCCurrentDateTimeISOString() {
		return DateUtils.formatToISOString(DateUtils.getUTCCurrentDateTime());
	}
	
	private RestTemplate createTemplate() throws KeyManagementException, NoSuchAlgorithmException {
		turnOffSslChecking();
		RestTemplate restTemplate = new RestTemplate();
		ClientHttpRequestInterceptor interceptor = new ClientHttpRequestInterceptor() {
			@Override
			public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
					throws IOException {
				String authToken = generateAuthToken();
				if (authToken != null && !authToken.isEmpty()) {
					request.getHeaders().set("Cookie", "Authorization=" + authToken);
					request.getHeaders().set("Authorization", "Authorization=" + authToken);
				}
				return execution.execute(request, body);
			}
		};
		restTemplate.setInterceptors(Collections.singletonList(interceptor));
		return restTemplate;
	}
	
	private String generateAuthToken() {
		ObjectNode requestBody = mapper.createObjectNode();
		requestBody.put("clientId", env.getProperty("clientId"));
		requestBody.put("secretKey", env.getProperty("secretKey"));
		requestBody.put("appId", env.getProperty("appId"));
		RequestWrapper<ObjectNode> request = new RequestWrapper<>();
		request.setRequesttime(DateUtils.getUTCCurrentDateTime());
		request.setRequest(requestBody);
		ClientResponse response = WebClient.create(env.getProperty("ida.authmanager.url")).post().syncBody(request)
				.exchange().block();
		List<ResponseCookie> list = response.cookies().get("Authorization");
		if (list != null && !list.isEmpty()) {
			ResponseCookie responseCookie = list.get(0);
			return responseCookie.getValue();
		}
		return "";
	}
	
	private void turnOffSslChecking() throws KeyManagementException, java.security.NoSuchAlgorithmException {
		// Install the all-trusting trust manager
		final SSLContext sc = SSLContext.getInstance(SSL);
		sc.init(null, UNQUESTIONING_TRUST_MANAGER, null);
		HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
	}
	
	private final TrustManager[] UNQUESTIONING_TRUST_MANAGER = new TrustManager[] { new X509TrustManager() {
		public java.security.cert.X509Certificate[] getAcceptedIssuers() {
			return null;
		}

		public void checkClientTrusted(java.security.cert.X509Certificate[] arg0, String arg1)
				throws CertificateException {
		}

		public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String arg1)
				throws CertificateException {
		}
	} };
	
	private String getSignature(String reqJson)
			throws KeyManagementException, NoSuchAlgorithmException, UnrecoverableEntryException, KeyStoreException,
			CertificateException, OperatorCreationException, JoseException, IOException {
		return sign(reqJson, false);
	}
	
	private String sign(String data, boolean isPayloadRequired)
			throws KeyManagementException, NoSuchAlgorithmException, UnrecoverableEntryException, KeyStoreException,
			CertificateException, OperatorCreationException, JoseException, IOException {
		return signatureUtil.sign(data, false, true, false, null, keyMgrUtil.getKeysDirPath(), env.getProperty("partnerId"));
	}
	
	@SuppressWarnings("rawtypes")
	public ResponseEntity<Map> sendOTPRequest(String individualId, String individualIdType) {
		OtpRequestDTO otpRequestDTO = new OtpRequestDTO();
		otpRequestDTO.setId("mosip.identity.otp");
		otpRequestDTO.setIndividualId(individualId);
		otpRequestDTO.setIndividualIdType(individualIdType);
		otpRequestDTO.setOtpChannel(Collections.singletonList("email"));
		otpRequestDTO.setRequestTime(getUTCCurrentDateTimeISOString());
		otpRequestDTO.setTransactionID(getTransactionID());
		otpRequestDTO.setVersion("1.0");

		try {
			RestTemplate restTemplate = createTemplate();
			String reqJson = mapper.writeValueAsString(otpRequestDTO);
			HttpHeaders httpHeaders = new HttpHeaders();
			httpHeaders.add("signature", getSignature(reqJson));
			httpHeaders.add("Content-type", MediaType.APPLICATION_JSON_VALUE);
			HttpEntity<String> httpEntity = new HttpEntity<>(reqJson, httpHeaders);
			String url = env.getProperty("ida.otp.url");
			System.out.println("OTP Request URL: " + url);
			System.out.println("OTP Request Body: " + reqJson);
			ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, httpEntity, Map.class);
			System.out.println(response);
			return response;
		} catch (KeyManagementException | NoSuchAlgorithmException | IOException | UnrecoverableEntryException
				| KeyStoreException | CertificateException | OperatorCreationException | JoseException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	@SuppressWarnings({ "deprecation", "unchecked", "rawtypes" })
	public ResponseEntity<Map> getAuthResponse(AppContext appContext) throws Exception {
		AuthRequestDTO authRequestDTO = new AuthRequestDTO();
		if (isPreLTS()) {
			// Set Auth Type
			AuthTypeDTO authTypeDTO = new AuthTypeDTO();
			authTypeDTO.setBio(appContext.getIsBioAuth());
			authTypeDTO.setOtp(appContext.getIsOTPAuth());
			authTypeDTO.setDemo(false);
			authRequestDTO.setRequestedAuth(authTypeDTO);
			// Set Individual Id type
			authRequestDTO.setIndividualIdType(appContext.getIndividualIdType());
		}
		// set Individual Id
		authRequestDTO.setIndividualId(appContext.getIndividualId());
		
		authRequestDTO.setEnv(env.getProperty("ida.request.captureFinger.env"));
		authRequestDTO.setDomainUri(env.getProperty("ida.request.captureFinger.domainUri"));
		RequestDTO requestDTO = new RequestDTO();
		requestDTO.setTimestamp(getUTCCurrentDateTimeISOString());
		
		if (appContext.getIsOTPAuth()) {
			requestDTO.setOtp(appContext.getOtpValue());
		}
		Map<String, Object> identityBlock = mapper.convertValue(requestDTO, Map.class);
		if (appContext.getIsBioAuth()) {
			identityBlock.put("biometrics", mapper.readValue(appContext.getCaptureValue(), Map.class).get("biometrics"));
		}

		System.out.println("******* Request before encryption ************ \n\n");
		System.out.println(mapper.writeValueAsString(identityBlock));
		EncryptionRequestDto encryptionRequestDto = new EncryptionRequestDto();
		encryptionRequestDto.setIdentityRequest(identityBlock);
		EncryptionResponseDto kernelEncrypt = null;
		try {
			kernelEncrypt = kernelEncrypt(encryptionRequestDto, false);
		} catch (Exception e) {
			e.printStackTrace();
			showNotification("Authentication", "Auth request failed", appContext.getTopPane(), "FAILURE");
			return null;
		}
		
		// Set request block
		authRequestDTO.setRequest(requestDTO);

		authRequestDTO.setTransactionID(getTransactionID());
		authRequestDTO.setRequestTime(getUTCCurrentDateTimeISOString());
		authRequestDTO.setConsentObtained(true);
		authRequestDTO.setId("mosip.identity.kyc");
		authRequestDTO.setVersion("1.0");
		authRequestDTO.setThumbprint(kernelEncrypt.getThumbprint());

		Map<String, Object> authRequestMap = mapper.convertValue(authRequestDTO, Map.class);
		authRequestMap.replace("request", kernelEncrypt.getEncryptedIdentity());
		authRequestMap.replace("requestSessionKey", kernelEncrypt.getEncryptedSessionKey());
		authRequestMap.replace("requestHMAC", kernelEncrypt.getRequestHMAC());
		RestTemplate restTemplate = createTemplate();

		String reqJson = mapper.writeValueAsString(authRequestMap);
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.add("signature", getSignature(reqJson));
		httpHeaders.add("Content-type", MediaType.APPLICATION_JSON_VALUE);
		HttpEntity<String> httpEntity = new HttpEntity<>(reqJson, httpHeaders);

		String url = env.getProperty("ida.ekyc.url");
		System.out.println("Auth URL: " + url);
		System.out.println("Auth Request : \n" + new ObjectMapper().writeValueAsString(authRequestMap));
		try {
			ResponseEntity<Map> authResponse = restTemplate.exchange(url, HttpMethod.POST, httpEntity, Map.class);
			System.out.println("Auth Response : \n" + new ObjectMapper().writeValueAsString(authResponse));
			System.out.println(authResponse.getBody());
			if (authResponse.getStatusCode().is2xxSuccessful()) {
				Map<String, Object> responseMap = (Map<String, Object>) authResponse.getBody().get("response");
				boolean status;
				if (Objects.nonNull(responseMap)) {
					Object key = "kycStatus";
					Object statusVal = responseMap.get(key);
					if (statusVal instanceof Boolean) {
						status = (Boolean) statusVal;
					} else {
						status = false;
					}
				} else {
					status = false;
				}
				
				if (status) {
					showNotification("Authentication", "Verification success", appContext.getTopPane(), "SUCCESS");
					return authResponse;
				} else {
					List errors = ((List) authResponse.getBody().get("errors"));
					if (errors != null) {
						Map<String, Object> errorsMap = (Map<String, Object>) (errors.get(0));
						showNotification("Authentication", (String) errorsMap.get("errorMessage"), appContext.getTopPane(), "FAILURE");
					} else {
						showNotification("Authentication", "Verification failed", appContext.getTopPane(), "FAILURE");
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private boolean isPreLTS() {
		return env.getProperty("isPreLTS", Boolean.class, false);
	}
	
	public AuthRequest getAuthRequest(MdmBioDevice mdmBioDevice) throws Exception {
		AuthRequest authRequest = new AuthRequest();
		authRequest.setPurpose(mdmBioDevice.getPurpose());
		authRequest.setSpecVersion(mdmBioDevice.getSpecVersion());
		authRequest.setCaptureTime(getCaptureTime());
		authRequest.setBio(new ArrayList<AuthRequestBioInfo>());
		AuthRequestBioInfo bioInfo = new AuthRequestBioInfo();
		bioInfo.setType(mdmBioDevice.getDeviceType());
		bioInfo.setDeviceId(mdmBioDevice.getDeviceId());
		authRequest.getBio().add(bioInfo);
		return authRequest;
	}
	
	public String captureFingerprint(int fingerCount, String prevHash, AppContext appContext, MdmBioDevice mdmBioDevice) throws Exception {
		
		AuthRequest authRequest = getAuthRequest(mdmBioDevice);
		authRequest.setEnv(env.getProperty("ida.request.captureFinger.env"));
		authRequest.setTimeout(env.getProperty("ida.request.captureFinger.timeout"));
		authRequest.setDomainUri(env.getProperty("ida.request.captureFinger.domainUri"));
		authRequest.setTransactionId(getTransactionID());
		List<AuthRequestBioInfo> bioInfo = authRequest.getBio();
		bioInfo.get(0).setCount(String.valueOf(fingerCount));
		bioInfo.get(0).setBioSubType(getBioSubTypeString(fingerCount, env.getProperty("ida.request.captureFinger.bioSubType")).split(","));
		bioInfo.get(0).setDeviceSubId(getFingerDeviceSubId());
		bioInfo.get(0).setPreviousHash(prevHash);
		bioInfo.get(0).setRequestedScore(env.getProperty("ida.request.captureFinger.requestedScore"));
		String requestBody = mapper.writer().withDefaultPrettyPrinter().writeValueAsString(authRequest);
		return capturebiometrics(requestBody, prevHash, appContext, mdmBioDevice);
	}
	
	public String captureIris(int irisCount, String irisSubId, String prevHash, AppContext appContext, MdmBioDevice mdmBioDevice) throws Exception {
		String irisSubtype = getIrisSubType(irisCount);
		String bioSubType = getBioSubTypeString(irisCount, irisSubtype);

		AuthRequest authRequest = getAuthRequest(mdmBioDevice);
		authRequest.setEnv(env.getProperty("ida.request.captureIris.env"));
		authRequest.setTimeout(env.getProperty("ida.request.captureIris.timeout"));
		authRequest.setDomainUri(env.getProperty("ida.request.captureIris.domainUri"));
		authRequest.setTransactionId(getTransactionID());
		List<AuthRequestBioInfo> bioInfo = authRequest.getBio();
		bioInfo.get(0).setCount(String.valueOf(irisCount));
		bioInfo.get(0).setBioSubType(bioSubType.split(","));
		bioInfo.get(0).setDeviceSubId(irisSubId);
		bioInfo.get(0).setPreviousHash(prevHash);
		bioInfo.get(0).setRequestedScore(env.getProperty("ida.request.captureIris.requestedScore"));
		String requestBody = mapper.writer().withDefaultPrettyPrinter().writeValueAsString(authRequest);
		return capturebiometrics(requestBody, prevHash, appContext, mdmBioDevice);
	}
	
	private String getFingerDeviceSubId() {
		return env.getProperty("finger.device.subid", DEFAULT_SUBID);
	}
	
	private String getIrisSubType(int count) {
		return env.getProperty("ida.request.captureIris.bioSubType");
	}
	
	private String getCaptureTime() {
		TimeZone tz = TimeZone.getTimeZone("UTC");
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"); // Quoted "Z" to indicate UTC, no timezone
																			// offset
		df.setTimeZone(tz);
		String nowAsISO = df.format(new Date());
		return nowAsISO;
	}
	
	private String capturebiometrics(String requestBody, String prevHash, AppContext appContext, MdmBioDevice mdmBioDevice) throws Exception {
		System.out.println("Capture request:\n" + requestBody);
		CloseableHttpClient client = HttpClients.createDefault();
		StringEntity requestEntity = new StringEntity(requestBody, ContentType.APPLICATION_JSON);
		HttpUriRequest request = RequestBuilder.create("CAPTURE").setUri(mdmBioDevice.getCallbackId() + "capture")
				.setEntity(requestEntity).build();
		CloseableHttpResponse response;
		StringBuilder stringBuilder = new StringBuilder();
		try {
			response = client.execute(request);

			InputStream inputStram = response.getEntity().getContent();
			BufferedReader bR = new BufferedReader(new InputStreamReader(inputStram));
			String line = null;
			while ((line = bR.readLine()) != null) {
				stringBuilder.append(line);
			}
			bR.close();
		} catch (IOException e) {
			showNotification("Authentication", "Device connectivity failed....", appContext.getTopPane(), "FAILURE");
			e.printStackTrace();
			return null;
		}
		String result = stringBuilder.toString();
		return result;
	}

	@SuppressWarnings("rawtypes")
	public String validateResult(String result, AppContext appContext, String prevHash)
			throws IOException, JsonParseException, JsonMappingException {
		String returnValue = null;
		String error = null;
		List data = (List) objectMapper.readValue(result.getBytes(), Map.class).get("biometrics");
		if (data == null) {
			showNotification("Authentication", result, appContext.getTopPane(), "SUCCESS");
		}

		for (int j = 0; j < data.size(); j++) {
			Map e = (Map) data.get(j);
			Map errorMap = (Map) e.get("error");
			error = errorMap.get("errorCode").toString();
			if (error.equals(DEFAULT_SUBID) || error.equals("100")) {
				returnValue = result;
				showNotification("Authentication", "Capture Success", appContext.getTopPane(), "SUCCESS");
				ObjectMapper objectMapper = new ObjectMapper();
				List dataList = (List) objectMapper.readValue(result.getBytes(), Map.class).get("biometrics");
				for (int i = 0; i < dataList.size(); i++) {
					Map b = (Map) dataList.get(i);
					String dataJws = (String) b.get("data");
					Map dataMap = objectMapper.readValue(CryptoUtil.decodeURLSafeBase64(dataJws.split("\\.")[1]), Map.class);
					System.out.println((i + 1) + " Bio-type: " + dataMap.get("bioType") + " Bio-sub-type: "
							+ dataMap.get("bioSubType"));
					prevHash = (String) b.get("hash");
				}
			} else {
				returnValue = null;
				showNotification("Authentication", "Capture Failed", appContext.getTopPane(), "FAILURE");
				break;
			}
		}
		return returnValue;
	}
	
	private String getBioSubTypeString(int count, String bioValue) {
		if (count == 1) {
			return bioValue;
		}
		String finalStr =  bioValue ;
		for (int i = 2; i <= count; i++) {
			finalStr = finalStr + "," + bioValue;
		}
		return finalStr;
	}
	
	@SuppressWarnings("rawtypes")
	public String decryptEKYCinfo(ResponseEntity<Map> authResponse) throws Exception {
		String partnerId = env.getProperty("partnerId");
		PrivateKeyEntry ekycKey = keyMgrUtil.getKeyEntry(keyMgrUtil.getKeysDirPath(), partnerId);
		Map ekycResponseData = (Map) authResponse.getBody().get("response");
		String identity = (String) ekycResponseData.get("identity");
		
		String sessionKey = (String) ekycResponseData.get("sessionKey");

		byte[] encSecKey;
		byte[] encKycData;
		if(sessionKey == null) {
			Map<String, String> encryptedData = this.splitEncryptedData(identity);
			encSecKey = CryptoUtil.decodeURLSafeBase64(encryptedData.get("encryptedSessionKey"));
			encKycData = CryptoUtil.decodeURLSafeBase64(encryptedData.get("encryptedData"));
		} else {
			encSecKey = CryptoUtil.decodeURLSafeBase64(sessionKey);
			encKycData = CryptoUtil.decodeURLSafeBase64(identity);
		}
		
		byte[] decSecKey = decryptSecretKey(ekycKey.getPrivateKey(), encSecKey);
	    Cipher cipher = Cipher.getInstance("AES/GCM/PKCS5Padding"); //NoPadding
	    byte[] nonce = Arrays.copyOfRange(encKycData, encKycData.length - cipher.getBlockSize(), encKycData.length);
	    byte[] encryptedKycData = Arrays.copyOf(encKycData, encKycData.length - cipher.getBlockSize());
		
		SecretKey secretKey =  new SecretKeySpec(decSecKey, 0, decSecKey.length, "AES");
		GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, nonce); 
		cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);

		String ekycResponseText = mapper.writerWithDefaultPrettyPrinter()
				.writeValueAsString(mapper.readValue(cipher.doFinal(encryptedKycData), Object.class));
		return ekycResponseText;
	}
	
	public Map<String, String> splitEncryptedData(@RequestBody String data) {
		byte[] dataBytes = CryptoUtil.decodeURLSafeBase64(data);
		byte[][] splits = splitAtFirstOccurance(dataBytes, env.getRequiredProperty("mosip.kernel.data-key-splitter").getBytes());
		return Map.of("encryptedSessionKey", CryptoUtil.encodeToURLSafeBase64(splits[0]), "encryptedData", CryptoUtil.encodeToURLSafeBase64(splits[1]));
	}
	
	private static byte[][] splitAtFirstOccurance(byte[] strBytes, byte[] sepBytes) {
		int index = findIndex(strBytes, sepBytes);
		if (index >= 0) {
			byte[] bytes1 = new byte[index];
			byte[] bytes2 = new byte[strBytes.length - (bytes1.length + sepBytes.length)];
			System.arraycopy(strBytes, 0, bytes1, 0, bytes1.length);
			System.arraycopy(strBytes, (bytes1.length + sepBytes.length), bytes2, 0, bytes2.length);
			return new byte[][] { bytes1, bytes2 };
		} else {
			return new byte[][] { strBytes, new byte[0] };
		}
	}
	
	private static int findIndex(byte arr[], byte[] subarr) {
		int len = arr.length;
		int subArrayLen = subarr.length;
		return IntStream.range(0, len).filter(currentIndex -> {
			if ((currentIndex + subArrayLen) <= len) {
				byte[] sArray = new byte[subArrayLen];
				System.arraycopy(arr, currentIndex, sArray, 0, subArrayLen);
				return Arrays.equals(sArray, subarr);
			}
			return false;
		}).findFirst() // first occurence
				.orElse(-1); // No element found
	}
	
	private byte[] decryptSecretKey(PrivateKey privKey, byte[] encKey)
			throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
			InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
		Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWITHSHA-256ANDMGF1PADDING");
		OAEPParameterSpec oaepParams = new OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256,
				PSpecified.DEFAULT);
		cipher.init(Cipher.DECRYPT_MODE, privKey, oaepParams);
		return cipher.doFinal(encKey, 0, encKey.length);
	}
	
	private EncryptionResponseDto kernelEncrypt(EncryptionRequestDto encryptionRequestDto, boolean isInternal)
			throws Exception {
		EncryptionResponseDto encryptionResponseDto = new EncryptionResponseDto();
		String identityBlock = mapper.writeValueAsString(encryptionRequestDto.getIdentityRequest());

		SecretKey secretKey = cryptoUtil.genSecKey();

		byte[] encryptedIdentityBlock = cryptoUtil.symmetricEncrypt(identityBlock.getBytes(StandardCharsets.UTF_8), secretKey);
		encryptionResponseDto.setEncryptedIdentity(Base64.encodeBase64URLSafeString(encryptedIdentityBlock));

		X509Certificate certificate = getCertificate(identityBlock, isInternal);
		PublicKey publicKey = certificate.getPublicKey();
		byte[] encryptedSessionKeyByte = cryptoUtil.asymmetricEncrypt((secretKey.getEncoded()), publicKey);
		encryptionResponseDto.setEncryptedSessionKey(Base64.encodeBase64URLSafeString(encryptedSessionKeyByte));
		byte[] byteArr = cryptoUtil.symmetricEncrypt(HMACUtils2.digestAsPlainText(identityBlock.getBytes(StandardCharsets.UTF_8)).getBytes(),
				secretKey);
		encryptionResponseDto.setRequestHMAC(Base64.encodeBase64URLSafeString(byteArr));

		String thumbprint = Hex.encodeHexString(getCertificateThumbprint(certificate));
		encryptionResponseDto.setThumbprint(thumbprint);
		return encryptionResponseDto;
	}
	
	private byte[] getCertificateThumbprint(Certificate cert) throws CertificateEncodingException {
		return DigestUtils.sha256(cert.getEncoded());
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public X509Certificate getCertificate(String data, boolean isInternal)
			throws KeyManagementException, RestClientException, NoSuchAlgorithmException, CertificateException {
		RestTemplate restTemplate = createTemplate();

		String publicKeyId = env.getProperty("ida.reference.id");
		Map<String, String> uriParams = new HashMap<>();
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(env.getProperty("ida.certificate.url"))
				.queryParam("applicationId", "IDA").queryParam("referenceId", publicKeyId);
		ResponseEntity<Map> response = restTemplate.exchange(builder.build(uriParams), HttpMethod.GET, null, Map.class);
		String certificate = (String) ((Map<String, Object>) response.getBody().get("response")).get("certificate");

		String certificateTrimmed = trimBeginEnd(certificate);
		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		X509Certificate x509cert = (X509Certificate) cf.generateCertificate(
				new ByteArrayInputStream(java.util.Base64.getDecoder().decode(certificateTrimmed)));
		return x509cert;
	}
	
	public static String trimBeginEnd(String pKey) {
		pKey = pKey.replaceAll("-*BEGIN([^-]*)-*(\r?\n)?", "");
		pKey = pKey.replaceAll("-*END([^-]*)-*(\r?\n)?", "");
		pKey = pKey.replaceAll("\\s", "");
		return pKey;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public String combineCaptures(List<String> bioCaptures) {
		List<String> captures = bioCaptures.stream().filter(obj -> obj != null)
				.filter(str -> str.contains("\"biometrics\"")).collect(Collectors.toList());

		if (captures.isEmpty()) {
			return null;
		}
		if (captures.size() == 1) {
			return captures.get(0);
		}
		LinkedHashMap<String, Object> identity = new LinkedHashMap<String, Object>();
		List<Map<String, Object>> biometricsList = captures.stream().map(obj -> {
			try {
				return objectMapper.readValue(obj, Map.class);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}).map(map -> map.get("biometrics")).filter(obj -> obj instanceof List).map(obj -> (List<Map>) obj)
				.flatMap(list -> list.stream()).filter(obj -> obj instanceof Map).map(obj -> (Map<String, Object>) obj)
				.collect(Collectors.toList());

		identity.put("biometrics", biometricsList);

		try {
			return objectMapper.writeValueAsString(identity);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public void showNotification(String title, String msg, AnchorPane topPane, String msgType) {
		String css = this.getClass().getResource("/css/application.css").toExternalForm();
//		graphic(new ImageView(new Image(this.getClass().getResourceAsStream("/images/warning.png"),50,50,true,true)))
		topPane.getScene().getStylesheets().add(css);
		Notifications notificationBuilder = Notifications.create()
				.title(title)
				.text(msg)
				.graphic(null)
				.hideAfter(Duration.seconds(3))
				.position(Pos.TOP_RIGHT)
				.owner(topPane);
		if (msgType.equals("FAILURE")) {
			notificationBuilder.showError();
		} else if (msgType.equals("SUCCESS")) {
			notificationBuilder.showInformation();
		} else if (msgType.equals("WARNING")) {
			notificationBuilder.showWarning();
		} else {
			notificationBuilder.show();
		}
	}
}
