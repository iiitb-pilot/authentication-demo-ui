package io.mosip.authentication.usecase.controller;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Random;
import java.util.ResourceBundle;

import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.jose4j.json.internal.json_simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import io.mosip.authentication.usecase.dto.Item;
import io.mosip.biometrics.util.ConvertRequestDto;
import io.mosip.biometrics.util.face.FaceDecoder;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.StringUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

@Component
public class ItemInfoController implements Initializable {

	@Autowired
	private Environment env;
	
	@FXML
	private Label listItemDesc;
	
    @FXML
    private TableView<Item> itemTable;

    @FXML
    private TableColumn<Item, String> noCol;
    
    @FXML
    private TableColumn<Item, ImageView> itemCol;

    @FXML
    private TableColumn<Item, String> itemDescCol;
    
    @FXML
    private TableColumn<Item, String> quatityCol;
    
    @FXML
    private TableColumn<Item, Double> priceCol;

    @FXML
    private TableColumn<Item, Double> totalCol;
    
    @FXML
    private Label lblName;
    
    @FXML
    private Label lblGender;
    
    @FXML
    private Label lblDOB;
    
    @FXML
    private Label lblAddress;
    
    @FXML
    private ImageView imgPhoto;
    
    HashMap<Integer, ObservableList<Item>> itemListMap = new HashMap<Integer, ObservableList<Item>>();
    
    ImageView rice = new ImageView(new Image(this.getClass().getResourceAsStream("/images/rice.jpg")));
    ImageView wheat = new ImageView(new Image(this.getClass().getResourceAsStream("/images/wheat.jpg")));
    ImageView oil = new ImageView(new Image(this.getClass().getResourceAsStream("/images/oil.jpg")));
    ImageView sugar = new ImageView(new Image(this.getClass().getResourceAsStream("/images/sugar.png")));
    ImageView bread = new ImageView(new Image(this.getClass().getResourceAsStream("/images/bread.jpg")));

    ObservableList<Item> list1 = FXCollections.observableArrayList(
    		new Item("1", rice,"Rice","2 Kg",25.00, 50.00),
    		new Item("2", wheat,"Wheat","1 Kg",20.00, 25.00),
    		new Item("3", oil,"Oil","1 Litre",30.00, 30.00),
    		new Item("4", sugar,"Sugar","2 Kg",15.00, 30.00),
    		new Item("5", bread,"Bread","2 Pkts",15.00, 30.00)
    );
    
    ObservableList<Item> list2 = FXCollections.observableArrayList(
    		new Item("1", rice,"Rice","2 Kg",25.00, 50.00),
    		new Item("2", oil,"Oil","1 Litre",30.00, 30.00),
    		new Item("3", sugar,"Sugar","2 Kg",15.00, 30.00),
    		new Item("4", bread,"Bread","2 Pkts",15.00, 30.00)
    );

    ObservableList<Item> list3 = FXCollections.observableArrayList(
    		new Item("1", oil,"Oil","1 Litre",30.00, 30.00),
    		new Item("2", sugar,"Sugar","2 Kg",15.00, 30.00),
    		new Item("3", bread,"Bread","2 Pkts",15.00, 30.00)
    );
    
    ObservableList<Item> list4 = FXCollections.observableArrayList(
    		new Item("1", rice,"Rice","2 Kg",25.00, 50.00),
    		new Item("2", bread,"Bread","2 Pkts",15.00, 30.00)
    );
    
    ObservableList<Item> list5 = FXCollections.observableArrayList(
    		new Item("1", bread,"Bread","2 Pkts",15.00, 30.00)
    );
    
    double HEIGHT = 40;
    double WIDTH = 80;
    
    public void setEKYCinfo(String ekycInfo) throws Exception {
		try {
			JSONParser parser = new JSONParser();
			JSONObject jsonObject = (JSONObject) parser.parse(ekycInfo);
			String nameField = env.getProperty("ekyc.mapfield.name");
			lblName.setText((String) jsonObject.get(nameField));
			
			String genderField = env.getProperty("ekyc.mapfield.gender");
			lblGender.setText((String) jsonObject.get(genderField));
			
			String dobField = env.getProperty("ekyc.mapfield.dob");
			lblDOB.setText((String) jsonObject.get(dobField));
			
			String addressFields = env.getProperty("ekyc.mapfield.address");
			String[] addressArray = addressFields.split(",");
			String address = "";
			for (String keyName : addressArray) {
				if (StringUtils.isNotEmpty((String)jsonObject.get(keyName))) {
					address = (address.length() > 0) ? address + ", " + (String)jsonObject.get(keyName) : address + (String)jsonObject.get(keyName);
				}
			}
			lblAddress.setText(address);
			
			String photoFields = env.getProperty("ekyc.mapfield.photo");
			String imageValue = (String) jsonObject.get(photoFields);
			ConvertRequestDto requestDto = new ConvertRequestDto();
			requestDto.setModality("Face");
			requestDto.setVersion("ISO19794_5_2011");
			requestDto.setInputBytes(CryptoUtil.decodePlainBase64(imageValue));

			byte [] imageData = FaceDecoder.convertFaceISOToImageBytes (requestDto);
			InputStream io = new ByteArrayInputStream(imageData);
			Image image = new Image(io);
			imgPhoto.setImage(image);
		} catch (ParseException e) {
			e.printStackTrace();
		}
    }
    
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		Calendar cal = Calendar.getInstance();
		String monthAndYear = new SimpleDateFormat("MMMM").format(cal.getTime()) + " " + new SimpleDateFormat("YYYY").format(cal.getTime());
		listItemDesc.setText("Eligible List of Items - " + monthAndYear);
		itemListMap.put(1, list1);
		itemListMap.put(2, list2);
		itemListMap.put(3, list3);
		itemListMap.put(4, list4);
		itemListMap.put(5, list5);
		noCol.setCellValueFactory(new PropertyValueFactory<Item, String>("sno"));
		itemCol.setCellValueFactory(new PropertyValueFactory<Item, ImageView>("itemImage"));
		itemDescCol.setCellValueFactory(new PropertyValueFactory<Item, String>("itemDesc"));
		quatityCol.setCellValueFactory(new PropertyValueFactory<Item, String>("quantity"));
		priceCol.setCellValueFactory(new PropertyValueFactory<Item, Double>("price"));
		totalCol.setCellValueFactory(new PropertyValueFactory<Item, Double>("totalPrice"));
		
		rice.setFitHeight(HEIGHT);
		rice.setFitWidth(WIDTH);
		wheat.setFitHeight(HEIGHT);
		wheat.setFitWidth(WIDTH);
		oil.setFitHeight(HEIGHT);
		oil.setFitWidth(WIDTH);
		sugar.setFitHeight(HEIGHT);
		sugar.setFitWidth(WIDTH);
		bread.setFitHeight(HEIGHT);
		bread.setFitWidth(WIDTH);
		
		Random rn = new Random();
		Integer rnValue = Integer.valueOf(rn.nextInt(5) + 1);
		ObservableList<Item> list  = itemListMap.get(rnValue);
		itemTable.setItems(list);
	}
}
