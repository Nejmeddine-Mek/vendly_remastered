package org.example.vendly_remastered.controls;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.example.vendly_remastered.models.entity.Product;
import org.example.vendly_remastered.repository.IProductRepository;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class NoBarCodeSelectionController implements Initializable {
    @FXML private VBox contentContainer;
    @FXML private TextField searchBar;


    List<Product> allUncodedProducts;
    private final Map<String, ListView<Product>> categoryListViews = new HashMap<>();

    private IProductRepository productRepository;

    public void setProductRepository(IProductRepository productRepository){ this.productRepository = productRepository; }
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

    }

    public void initializeLists(){
        loadUncodedProducts();
    }

    private void loadUncodedProducts(){
        this.allUncodedProducts = productRepository.getUncodedProducts();
    }
}
