package io.mosip.authentication.usecase.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.mosip.authentication.usecase.dto.AppContext;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

@Component
public class LoginController {

    @FXML
    private PasswordField txtPassword;		

    @FXML
    private TextField txtUsername;
    
    private AppContext appContext;

	@Value("${mosip.primary-language}")
	private String applicationLanguage;

	@Value("${mosip.auth.ekyc.label}")
	private String[] ekycLabel;
	
    public void setContext(AppContext appContextInfo) {
    	this.appContext = appContextInfo;
    	this.appContext.userNameProperty().bind(txtUsername.textProperty());
    	this.appContext.passwordProperty().bind(txtPassword.textProperty());
    }
    
    public void reset() {
    	txtUsername.setText("");
    	txtPassword.setText("");
    }

}

