package io.mosip.authentication.usecase.dto;

import java.util.List;

import lombok.Data;

@Data
public class AuthRequest {
	private String env;
	private String purpose;
	private String specVersion;
	private String timeout;
	private String captureTime;
	private String domainUri;
	private String transactionId;
	private String customOpts;
	private List<AuthRequestBioInfo> bio;
}
