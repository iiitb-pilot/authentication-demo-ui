/**
 * 
 */
package io.mosip.authentication.usecase.controller;

import java.io.FileNotFoundException;
import java.net.URL;

import io.mosip.authentication.usecase.IdaUsecaseStarter;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.Pane;

/**
 * @author HP
 *
 */
public class FxmlManager {

	@SuppressWarnings("static-access")
	public Pane getPage(String fileName) {
		try {
			URL fileUrl = IdaUsecaseStarter.class.getResource("/fxml/" + fileName);
			if (fileUrl == null) {
				throw new FileNotFoundException("FXML file cannot be found");
			}
			return new FXMLLoader().load(fileUrl);
		} catch (Exception ex) {
			System.out.println("No page " + fileName + " please check FxmlLoader");
		}
		return null;
	}
	

}
