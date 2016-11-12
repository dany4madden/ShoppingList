package com.example.dany.shoppinglist;
import android.support.annotation.NonNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * Created by dany on 11/8/16.
 */

public class ShoppingList {
    String listname; // listname (is filename) without .txt
    List <String> items; // TODO: make thia s list of <Item>

    public ShoppingList() {
		listname = new String();
		items = new ArrayList<String>();
	}
	public ShoppingList(String n, List <String> i){
		this.listname = n;
		this.items = i;
	}

    //add item to items. check for duplicate
	public void addItem (Item i) {

	}
    //remove item from items
	public void removeItem (Item i) {

	}

    public class Item {
		private int quantity;
		private String name;

		public Item () {
			quantity = 1;
			name="item";
		}
		public Item (String n, int q) {
			this.name = n;
			this.quantity = q;

		}
		public String getItemName() {
			return name; 
		}
		public int getItemQuantity() {
			return quantity;
		}
		public void setItemName(String s) {
			this.name=s;
		}
		public void setItemQuantity(int i) { this.quantity = i; }
	}

}
