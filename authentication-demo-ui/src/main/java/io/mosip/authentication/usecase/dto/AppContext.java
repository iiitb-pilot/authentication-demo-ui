package io.mosip.authentication.usecase.dto;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.layout.AnchorPane;

public class AppContext {

	private StringProperty userName = new SimpleStringProperty("");
	private StringProperty password = new SimpleStringProperty("");
	private BooleanProperty isBioAuth = new SimpleBooleanProperty();
	private BooleanProperty isOTPAuth = new SimpleBooleanProperty();
	private BooleanProperty isIdExist = new SimpleBooleanProperty();
	private StringProperty individualId = new SimpleStringProperty("");
	private StringProperty individualIdType = new SimpleStringProperty("");
	private StringProperty otpValue = new SimpleStringProperty("");
	private StringProperty captureValue = new SimpleStringProperty("");
	private AnchorPane topPane;
	
    public StringProperty userNameProperty() {
        return userName;
    }
    
	public final String getUserName() {
		return userName.get();
	}
	
	public final void setUserName(String userName) {
		this.userName.set(userName);
	}
	
    public StringProperty passwordProperty() {
        return password;
    }
	
	public final String getPassword() {
		return password.get();
	}
	
	public final void setPassword(String password) {
		this.password.set(password);
	}
	
	public void setTopPane(AnchorPane pane) {
		topPane = pane;
	}
	
	public AnchorPane getTopPane() {
		return topPane;
	}
	
    public BooleanProperty isBioAuthProperty() {
        return isBioAuth;
    }
    
	public void setIsBioAuth(boolean bioAuth) {
		isBioAuth.set(bioAuth);
	}
	
	public boolean getIsBioAuth() {
		return isBioAuth.get();
	}
	
    public BooleanProperty isOTPAuthProperty() {
        return isOTPAuth;
    }
    
	public void setIsOTPAuth(boolean otpAuth) {
		isOTPAuth.set(otpAuth);
	}
	
	public boolean getIsOTPAuth() {
		return isOTPAuth.get();
	}
	
    public BooleanProperty isIdExistProperty() {
        return isIdExist;
    }
    
	public void setIsIdExist(boolean idExist) {
		isIdExist.set(idExist);
	}
	
	public boolean getIsIdExist() {
		return isIdExist.get();
	}
	
    public StringProperty individualIdProperty() {
        return individualId;
    }
    
	public void setIndividualId(String id) {
		individualId.set(id);
	}
	
	public String getIndividualId() {
		return individualId.get();
	}
	
    public StringProperty individualIdTypeProperty() {
        return individualIdType;
    }
    
	public void setIndividualIdType(String type) {
		individualIdType.set(type);
	}
	
	public String getIndividualIdType() {
		return individualIdType.get();
	}
	
    public StringProperty otpValueProperty() {
        return otpValue;
    }
    
	public void setOtpValue(String value) {
		otpValue.set(value);
	}
	
	public String getOtpValue() {
		return otpValue.get();
	}
	
    public StringProperty captureValueProperty() {
        return captureValue;
    }
    
	public void setCaptureValue(String value) {
		captureValue.set(value);
	}
	
	public String getCaptureValue() {
		return captureValue.get();
	}
	
}
