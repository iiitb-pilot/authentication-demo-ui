package io.mosip.authentication.usecase.dto;

import lombok.Data;

@Data
public class AuthRequestBioInfo {
	private String type;
	private String count;
	private String[] bioSubType;
	private String requestedScore;
	private String deviceId;
	private String deviceSubId;
	private String previousHash;
}
