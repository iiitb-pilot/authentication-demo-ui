package io.mosip.authentication.usecase.dto;

import lombok.Data;

@Data
public class MdmDeviceInfoResponse {

	private Error error;
	private String deviceInfo;
}
