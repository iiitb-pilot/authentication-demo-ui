package io.mosip.authentication.usecase.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.authentication.usecase.dto.AppContext;
import io.mosip.authentication.usecase.helper.IdentityCaptureHelper;
import io.mosip.kernel.core.util.StringUtils;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

@Component
@ComponentScan(basePackages = {"io.mosip.authentication.demo.helper",
		"io.mosip.authentication.demo.service.util",
		"io.mosip.authentication.usecase"})
public class IdentityCaptureController {

	@Value("${mosip.primary-language}")
	private String applicationLanguage;

	@Value("${mosip.auth.ekyc.label}")
	private String[] ekycLabel;
	
	@Autowired
	private Environment env;
	
	@Autowired
	private IdentityCaptureHelper helper;
	
	private String previousHash;
	private StringProperty capture = new SimpleStringProperty();
	ObjectMapper mapper = new ObjectMapper();
	
	private BooleanProperty isFPselected = new SimpleBooleanProperty(false);
	private BooleanProperty isIrisSelected = new SimpleBooleanProperty(false);
	private BooleanProperty isOTPSelected = new SimpleBooleanProperty(false);
	
	ObservableList<String> idTypeChoices = FXCollections.observableArrayList("UIN", "VID");
	ObservableList<String> fingerCountChoices = FXCollections.observableArrayList("1", "2", "3", "4", "5", "6", "7",
			"8", "9", "10");
	ObservableList<String> irisCountChoices = FXCollections.observableArrayList("Left Iris", "Right Iris",
			"Both Iris");
	
	@FXML
	private AnchorPane idcapturePane;
	
	@FXML
	private TextField idValue;
	
	@FXML
	private TextField responsetextField;
	
	@FXML
	private Button requestOtp;

	@FXML
	private RadioButton rbFingerAuthType;
	
	@FXML
	private RadioButton rbIrisAuthType;	
	
	@FXML
	private RadioButton rbOTPAuthType;
	
	@FXML
	ComboBox<String> fingerCount;
	
	@FXML
	ComboBox<String> irisType;
	
	@FXML
	private ComboBox<String> idTypebox;
	
	@FXML
	private TextField otpValue;
	
	@FXML
	private AnchorPane otpAnchorPane;

	@FXML
	private AnchorPane bioAnchorPane;
	
	Stage dialog;
	private AppContext appContext;
	
    public void setContext(AppContext appContextInfo) {
    	this.appContext = appContextInfo;
    	this.appContext.individualIdTypeProperty().bind(idTypebox.valueProperty());
    	this.appContext.individualIdProperty().bind(idValue.textProperty());
    	this.appContext.isBioAuthProperty().bind(Bindings.when(isBioAuth()).then(true).otherwise(false));
    	this.appContext.isOTPAuthProperty().bind(Bindings.when(isOTPAuth()).then(true).otherwise(false));
    	this.appContext.otpValueProperty().bind(otpValue.textProperty());
    	this.appContext.captureValueProperty().bind(capture);
    }
    
	public void initialize() {
		responsetextField.setText(null);
		fingerCount.setItems(fingerCountChoices);
		fingerCount.getSelectionModel().select(0);
		irisType.setItems(irisCountChoices);
		irisType.getSelectionModel().select(0);

		idTypebox.setItems(idTypeChoices);
		idTypebox.setValue("UIN");
		otpAnchorPane.setDisable(true);
		bioAnchorPane.setDisable(true);
		responsetextField.setDisable(true);

		rbFingerAuthType.selectedProperty().addListener(rbFingerAuthTypeChangeListener());
		rbIrisAuthType.selectedProperty().addListener(rbIrisAuthTypeChangeListener());
		rbOTPAuthType.selectedProperty().addListener(rbOTPAuthTypeChangeListener());
		
		bioAnchorPane.disableProperty().bind(Bindings.when(isBioAuth()).then(false).otherwise(true));
		otpAnchorPane.disableProperty().bind(Bindings.when(isOTPAuth()).then(false).otherwise(true));
		fingerCount.disableProperty().bind(Bindings.when(isFPselected).then(false).otherwise(true));
		irisType.disableProperty().bind(Bindings.when(isIrisSelected).then(false).otherwise(true));
	}
	
	public void reset() {
		rbFingerAuthType.setSelected(false);
		rbIrisAuthType.setSelected(false);
		rbOTPAuthType.setSelected(false);
		otpValue.setText("");
		idValue.setText("");
//		appContext.isBioAuthProperty().set(false);
//		appContext.isOTPAuthProperty().set(false);
//		appContext.individualIdProperty().set("");
//		appContext.otpValueProperty().set("");
//		appContext.captureValueProperty().set("");
	}
	
	@SuppressWarnings("rawtypes")
	@FXML
	private void onRequestOtp() {
		if (StringUtils.isEmpty(idValue.getText())) {
			helper.showNotification("OTP", "Id Number is required to proceed...", appContext.getTopPane(), "WARNING");
			return;
		}
		otpValue.setText("");
		ResponseEntity<Map> otpResponse = helper.sendOTPRequest(idValue.getText(), idTypebox.getValue());
		if (otpResponse.getStatusCode().is2xxSuccessful()) {
			List errors = ((List) otpResponse.getBody().get("errors"));
			boolean status = errors == null || errors.isEmpty();
			String responseText = status ? "OTP request successfully sent" : "OTP request failed to sent";
			if (status) {
				helper.showNotification("OTP", responseText, appContext.getTopPane(), "SUCCESS");
			} else {
				helper.showNotification("OTP", responseText, appContext.getTopPane(), "WARNING");
			}
			responsetextField.setText(responseText);
		} else {
			helper.showNotification("OTP", "OTP request failed with error", appContext.getTopPane(), "FAILURE");
		}
	}
	
	@FXML
	private void onCapture() throws Exception {
		previousHash = null;
		List<String> bioCaptures = new ArrayList<>();
		String fingerCapture;
		if (rbFingerAuthType.isSelected()) {
			fingerCapture = helper.captureFingerprint(getFingerCount(), getPreviousHash(), appContext);
			if (!fingerCapture.contains("\"biometrics\"")) {
				return;
			}
			bioCaptures.add(fingerCapture);

			Integer delay = env.getProperty("delay.after.finger.capture.millisecs", Integer.class, 0);
			if (delay > 0) {
				System.out.println("waiting for millisecs: " + delay);
				Thread.sleep(delay);
			}
		}
		String irisCapture;
		if (rbIrisAuthType.isSelected()) {
			irisCapture = helper.captureIris(getIrisCount(), getIrisDeviceSubId(), getPreviousHash(), appContext);
			if (!irisCapture.contains("\"biometrics\"")) {
				return;
			}
			bioCaptures.add(irisCapture);
			Integer delay = env.getProperty("delay.after.iris.capture.millisecs", Integer.class, 0);
			if (delay > 0) {
				System.out.println("waiting for millisecs: " + delay);
				Thread.sleep(delay);
			}
		}
		capture.set(helper.combineCaptures(bioCaptures));
	}
	
	private ChangeListener<Boolean> rbFingerAuthTypeChangeListener() {
		return new ChangeListener<Boolean>() {
		    @Override
		    public void changed(ObservableValue<? extends Boolean> obs, Boolean wasPreviouslySelected, Boolean isNowSelected) {
		        if (isNowSelected) { 
		        	isFPselected.setValue(Boolean.TRUE);
		        	otpValue.setText("");
		        } else {
		        	isFPselected.setValue(Boolean.FALSE);
		        }
		    }
		};
	}
	
	private ChangeListener<Boolean> rbIrisAuthTypeChangeListener() {
		return new ChangeListener<Boolean>() {
		    @Override
		    public void changed(ObservableValue<? extends Boolean> obs, Boolean wasPreviouslySelected, Boolean isNowSelected) {
		        if (isNowSelected) { 
		        	isIrisSelected.setValue(Boolean.TRUE);
		        	otpValue.setText("");
		        } else {
		        	isIrisSelected.setValue(Boolean.FALSE);
		        }
		    }
		};
	}
	
	private ChangeListener<Boolean> rbOTPAuthTypeChangeListener() {
		return new ChangeListener<Boolean>() {
		    @Override
		    public void changed(ObservableValue<? extends Boolean> obs, Boolean wasPreviouslySelected, Boolean isNowSelected) {
		        if (isNowSelected) { 
		        	isOTPSelected.setValue(Boolean.TRUE);
		        } else {
		        	isOTPSelected.setValue(Boolean.FALSE);
		        }
		    }
		};
	}
	
	private String getPreviousHash() {
		return previousHash == null ? "" : previousHash;
	}
	
	private BooleanBinding isBioAuth() {
		BooleanBinding isReady = Bindings.when(isFPselected.or(isIrisSelected)).then(true).otherwise(false);
		return isReady;
	}
	
	private BooleanBinding isOTPAuth() {
		BooleanBinding isReady = Bindings.when(isOTPSelected).then(true).otherwise(false);
		return isReady;
	}
	
	private int getFingerCount() {
		return fingerCount.getValue() == null ? 1 : Integer.parseInt(fingerCount.getValue());
	}
	
	private int getIrisCount() {
		return irisType.getSelectionModel().getSelectedIndex() == 2 ? 2 : 1;
	}
	
	private String getIrisDeviceSubId() {
		String irisSubId = env.getProperty("iris.device.subid");
		if (irisSubId == null) {
			if (irisType.getSelectionModel().getSelectedIndex() == 0) {
				irisSubId = String.valueOf(1);
			} else if (irisType.getSelectionModel().getSelectedIndex() == 1) {
				irisSubId = String.valueOf(2);
			} else {
				irisSubId = String.valueOf(3);
			}
		}
		return irisSubId;
	}
}
