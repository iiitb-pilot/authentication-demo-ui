package io.mosip.authentication.usecase.controller;

import java.io.IOException;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.Map;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.bouncycastle.operator.OperatorCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.authentication.demo.service.util.KeyMgrUtil;
import io.mosip.authentication.usecase.IdaUsecaseStarter;
import io.mosip.authentication.usecase.dto.AppContext;
import io.mosip.authentication.usecase.helper.IdentityCaptureHelper;
import io.mosip.kernel.core.util.StringUtils;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

@Component
public class IdaUsecaseController {

	@Autowired
	private Environment env;

	/** The sign refid. */
	@Value("${mosip.sign.refid:SIGN}")
	private String signRefid;

	@Value("${mosip.primary-language}")
	private String applicationLanguage;

	@Value("${mosip.auth.ekyc.label}")
	private String[] ekycLabel;
	
	ObjectMapper mapper = new ObjectMapper();
	
	@Autowired
	private KeyMgrUtil keyMgrUtil;
	
	@Autowired
	private IdentityCaptureHelper helper;
	
	@FXML
	private BorderPane mainPane;
	
	@FXML
	private AnchorPane centerPane;
	
	@FXML
	private AnchorPane topPane;
	
	@FXML
	private AnchorPane bottomPane;
	
	@FXML
	private Button btnVerify;
	
	@FXML
	private Button btnReset;
	
	@FXML
	private Button btnlogin;
	
	@FXML
	private Button btnLoginReset;
	
	@FXML
	private Button btnLogout;
	
	@FXML
	private HBox identityPaneButtonBox;
	
	@FXML
	private HBox itemInfoPaneButtonBox;
	
	@FXML
	private HBox loginButtonBox;
	
	private LoginController loginController;
	private IdentityCaptureController identityCaptureController;
	private ItemInfoController itemInfoController;
	
	private AppContext appContext;
	private BooleanProperty hasUserNameValue = new SimpleBooleanProperty(false);
	private BooleanProperty hasPasswordValue = new SimpleBooleanProperty(false);
	
	@PostConstruct
	public void postConstruct() throws NoSuchAlgorithmException, UnrecoverableEntryException, KeyStoreException,
			CertificateException, OperatorCreationException, IOException {
		appContext = new AppContext();
		initializeKeysAndCerts();
	}

	@FXML
	private void initialize() {
		appContext.setTopPane(topPane);
		initLoginPage();
		openLoginPage();
	}
	
	@FXML
	public void onLoginReset() {
		loginController.reset();
	}
	
	@FXML
	public void onLogin() {
		if (appContext.getUserName().equals(appContext.getPassword())) {
			helper.showNotification("Login", "Login Successful", topPane, "SUCCESS");
			openIdentityCapture();
		} else {
			helper.showNotification("Login", "Username or Password is invalid", topPane, "FAILURE");
		}
	}
	
	@FXML
	public void onLogout() {
		loginButtonBox.setVisible(true);
		initLoginPage();
		openLoginPage();
	}
	
	@SuppressWarnings("rawtypes")
	@FXML
	public void onVerify() {
		try {
			if (StringUtils.isEmpty(appContext.getIndividualId())) {
				helper.showNotification("Authentication", "Id Number is required to proceed...", topPane, "WARNING");
				return;
			}
			ResponseEntity<Map> authResponse = helper.getAuthResponse(appContext);
			if (authResponse != null) {
				String ekycInfo = helper.decryptEKYCinfo(authResponse);
				openItemInfo(ekycInfo);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@FXML
	public void onReset() {
		identityCaptureController.reset();
	}
	
	@FXML
	public void onCollect() {
		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.setTitle("Confirmation");
		alert.setContentText("Have you collected amount and handover items to resident?");
		Optional<ButtonType> result = alert.showAndWait();
		if (result.get() == ButtonType.OK) {
			openIdentityCapture();
		}
	}
	
	@FXML
	public void onBack() {
		openIdentityCapture();
	}

	private void initLoginPage() {
		appContext.userNameProperty().addListener(userNameChangeListener());
		appContext.passwordProperty().addListener(passwordChangeListener());
		btnlogin.disableProperty().bind(Bindings.when(isLoginReady()).then(false).otherwise(true));
		btnLoginReset.disableProperty().bind(Bindings.when(isResetReady()).then(false).otherwise(true));
	}
	
	private ChangeListener<String> userNameChangeListener() {
		return new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				if (StringUtils.isNotEmpty(newValue)) {
					hasUserNameValue.set(true);
				} else {
					hasUserNameValue.set(false);
				}
			}
		};
	}
	
	private ChangeListener<String> passwordChangeListener() {
		return new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				if (StringUtils.isNotEmpty(newValue)) {
					hasPasswordValue.set(true);
				} else {
					hasPasswordValue.set(false);
				}
			}
		};
	}
	
	private BooleanBinding isLoginReady() {
		BooleanBinding isReady = Bindings.when(hasUserNameValue.and(hasPasswordValue)).then(true).otherwise(false);
		return isReady;
	}
	
	private BooleanBinding isResetReady() {
		BooleanBinding isReady = Bindings.when(hasUserNameValue.or(hasPasswordValue)).then(true).otherwise(false);
		return isReady;
	}
	
	@SuppressWarnings("resource")
	public void openLoginPage() {
		
		FXMLLoader loader = new FXMLLoader();
		Parent pane;
		try {
			loader.setControllerFactory(IdaUsecaseStarter.getApplicationContext()::getBean);
			pane = loader.load(this.getClass().getClassLoader().getResourceAsStream("fxml/login.fxml"));
			mainPane.setCenter(pane);
			
			identityPaneButtonBox.setVisible(false);
			itemInfoPaneButtonBox.setVisible(false);
			btnLogout.setVisible(false);
			loginController = loader.<LoginController>getController();
			loginController.setContext(appContext);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("resource")
	private void openIdentityCapture() {
		FXMLLoader loader = new FXMLLoader();
		Parent pane;
		try {
			loader.setControllerFactory(IdaUsecaseStarter.getApplicationContext()::getBean);
			pane = loader.load(this.getClass().getClassLoader().getResourceAsStream("fxml/identitycapture.fxml"));
			mainPane.setCenter(pane);
			identityPaneButtonBox.setVisible(true);
			itemInfoPaneButtonBox.setVisible(false);
			loginButtonBox.setVisible(false);
			btnLogout.setVisible(true);
	
			identityCaptureController = loader.<IdentityCaptureController>getController();
			identityCaptureController.setContext(appContext);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("resource")
	public void openItemInfo(String ekycInfo) throws Exception {
		FXMLLoader loader = new FXMLLoader();
		Parent pane;
		try {
			loader.setControllerFactory(IdaUsecaseStarter.getApplicationContext()::getBean);
			pane = loader.load(this.getClass().getClassLoader().getResourceAsStream("fxml/iteminfo.fxml"));
			mainPane.setCenter(pane);
			identityPaneButtonBox.setVisible(false);
			itemInfoPaneButtonBox.setVisible(true);
			loginButtonBox.setVisible(false);
			btnLogout.setVisible(true);
	
			itemInfoController = loader.<ItemInfoController>getController();
			itemInfoController.setEKYCinfo(ekycInfo);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void initializeKeysAndCerts() throws NoSuchAlgorithmException, UnrecoverableEntryException,
			KeyStoreException, CertificateException, IOException, OperatorCreationException {
		String partnerId = env.getProperty("partnerId");
		String organization = env.getProperty("partnerOrg", env.getProperty("partnerId"));
		String dirPath = keyMgrUtil.getKeysDirPath();
		// Check if partner private (<partnerId>.p12) key is present in keys dir
		PrivateKeyEntry keyEntry = keyMgrUtil.getKeyEntry(dirPath, partnerId);
		if (keyEntry == null) {
			System.out.println("Initializing parnter keys and certificates..");
			keyMgrUtil.getPartnerCertificates(partnerId, organization, dirPath);
			System.out.println("Completed initializing parnter keys and certificates. Location: " + dirPath);
		} else {
			System.out.println("Parnter keys and certificates already available.");
		}
	}
	

}
