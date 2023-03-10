package io.mosip.authentication.usecase.dto;

import javafx.scene.image.ImageView;

public class Item {

	private String sno;
	private ImageView itemImage;
	private String itemDesc;
	private String quantity;
	private Double price;
	private Double totalPrice;

	
	public Item(String sno, ImageView itemImage, String itemDesc, String quantity, Double price, Double totalPrice) {
		super();
		this.sno = sno;
		this.itemImage = itemImage;
		this.itemDesc = itemDesc;
		this.quantity = quantity;
		this.price = price;
		this.totalPrice = totalPrice;
	}

	public String getSno() {
		return sno;
	}
	
	public ImageView getItemImage() {
		return itemImage;
	}

	public String getItemDesc() {
		return itemDesc;
	}
	
	public String getQuantity() {
		return quantity;
	}
	
	public Double getPrice() {
		return price;
	}
	
	public Double getTotalPrice() {
		return totalPrice;
	}
	
}
