package io.mosip.authentication.usecase.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class MdmSbiDeviceInfoWrapper extends DeviceInfo {

	public MdmSbiDeviceInfo deviceInfo;
	public Error error;
	
}
