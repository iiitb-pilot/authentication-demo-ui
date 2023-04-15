package io.mosip.authentication.usecase.dto;

import lombok.Data;

@Data
public class Error {

	private String errorCode;
	private String errorInfo;

}
