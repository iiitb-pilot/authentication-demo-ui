package io.mosip.authentication.usecase.provider;

import java.util.List;

import io.mosip.authentication.usecase.dto.MdmBioDevice;

public interface MosipDeviceSpecificationProvider {

	/**
	 * Get the spec implementation class version
	 * 
	 * @return mds spec version
	 */
	public String getSpecVersion();
	
	/**
	 * @param deviceInfoResponse received from mds
	 * @return list of mdmBio Devices
	 */
	public List<MdmBioDevice> getMdmDevices(String deviceInfoResponse, int port);
	
	
}
