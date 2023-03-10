package io.mosip.authentication.usecase;

import java.io.IOException;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

import io.mosip.authentication.demo.helper.CryptoUtility;
import io.mosip.authentication.demo.util.ApplicationResourceContext;
import io.mosip.kernel.crypto.jce.core.CryptoCore;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

@SpringBootApplication
@Import({CryptoCore.class, CryptoUtility.class})
@ComponentScan(basePackages = {"io.mosip.authentication.demo.helper",
		"io.mosip.authentication.demo.service.util",
		"io.mosip.authentication.usecase"})
public class IdaUsecaseStarter extends Application {

	private static ConfigurableApplicationContext applicationContext;
	FXMLLoader loader = new FXMLLoader();
	AnchorPane root;

	public static void main(String[] args) {
		Application.launch(IdaUsecaseStarter.class, args);
	}

	public static ConfigurableApplicationContext getApplicationContext() {
		return applicationContext;
	}
	
	@Override
	public void init() throws Exception {
		SpringApplicationBuilder builder = new SpringApplicationBuilder(IdaUsecaseStarter.class);
		applicationContext = builder.run(getParameters().getRaw().toArray(new String[0]));

		loader.setControllerFactory(applicationContext::getBean);
		ApplicationResourceContext.getInstance().setApplicationLanguage(applicationContext.getEnvironment().getProperty("mosip.primary-language"));
		ApplicationResourceContext.getInstance().setApplicationSupportedLanguage(applicationContext.getEnvironment().getProperty("mosip.supported-languages"));

		loader.setResources(ApplicationResourceContext.getInstance().getLabelBundle());
		root = loader.load(this.getClass().getClassLoader().getResourceAsStream("fxml/idausecase.fxml"));
	}

	@Override
	public void start(Stage stage) throws IOException {
		// Create the Scene
		Scene scene = new Scene(root);
		// Set the Scene to the Stage
		stage.setScene(scene);
		// Set the Title to the Stage
		stage.setTitle("Ministry of Consumer Affairs, Food and Public Distribution");
		// Display the Stage
		stage.show();
	}	
	
    @Override
    public void stop() {
    	applicationContext.close();
        System.exit(0);
    }
	
}
