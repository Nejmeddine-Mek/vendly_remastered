package org.example.vendly_remastered.controls;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.example.vendly_remastered.models.dto.ClientDashboardItem;
import org.example.vendly_remastered.models.dto.ProductDashboardItem;
import org.example.vendly_remastered.models.dto.RevenueDashboardItem;
import org.example.vendly_remastered.models.dto.SupplierDashboardItem;
import org.example.vendly_remastered.models.entity.Supplier;
import org.example.vendly_remastered.repository.IDashBoardRepository;

import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

    // --- Header & Filter Controls ---
    @FXML private ComboBox<String> cmbDatePreset;
    @FXML private DatePicker dpStartDate;
    @FXML private DatePicker dpEndDate;
    @FXML private Button btnApplyFilter;

    // --- KPI Card Labels ---
    @FXML private Label lblTotalRevenue;
    @FXML private Label lblTotalPurchases;
    @FXML private Label lblLowStockCount;
    @FXML private Label lblExpiringCount;

    // --- Analytics Charts ---
    @FXML private LineChart<String, Number> salesLineChart;
    @FXML private BarChart<String, Number> topProductsChart;

    // --- Top Clients Table & Columns ---
    @FXML private TableView<ClientDashboardItem> tblTopClients; // Replace '?' with your Client DTO
    @FXML private TableColumn<ClientDashboardItem, String> colClientName;
    @FXML private TableColumn<ClientDashboardItem, Integer> colClientOrders;
    @FXML private TableColumn<ClientDashboardItem, Double> colClientTotal;

    // --- Top Suppliers Table & Columns ---
    @FXML private TableView<SupplierDashboardItem> tblTopSuppliers; // Replace '?' with your Supplier DTO
    @FXML private TableColumn<SupplierDashboardItem, String> colSupplierName;
    @FXML private TableColumn<SupplierDashboardItem, Integer> colSupplierPurchases;
    @FXML private TableColumn<SupplierDashboardItem, Double> colSupplierTotal;

    private final NumberFormat currencyFormatter = NumberFormat.getNumberInstance(Locale.US);

    private IDashBoardRepository dashBoardRepository;

    public void setDashBoardRepository(IDashBoardRepository dashBoardRepository) {
        this.dashBoardRepository = dashBoardRepository;
    }

    // --- Initialization & Event Handlers ---

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupClientsTable();
        setupSuppliersTable();
    }

    public void initializeContent(){
        loadClients();
        loadSuppliers();
        loadRevenue();
        loadProducts();
    }

    private void setupClientsTable() {
        // Value Factories (Record Property Matching)
        colClientName.setCellValueFactory(cell ->
                new ReadOnlyObjectWrapper<>(cell.getValue().clientName()));

        colClientOrders.setCellValueFactory(cell ->
                new ReadOnlyObjectWrapper<>(cell.getValue().ordersCount()));

        colClientTotal.setCellValueFactory(cell ->
                new ReadOnlyObjectWrapper<>(cell.getValue().totalSpent()));
        // Format Total Spent column as "X,XXX.XX DA"
        colClientTotal.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText(null);
                } else {
                    setText(String.format("%s DA", currencyFormatter.format(value)));
                }
            }
        });

        // UX Cleanups: Placeholder text & column resizing behavior
        tblTopClients.setPlaceholder(new Label("No client records found for this period."));
        tblTopClients.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }
    private void setupSuppliersTable() {
        // Value Factories
        colSupplierName.setCellValueFactory(cell ->
            new ReadOnlyObjectWrapper<>(cell.getValue().supplierName())
        );
        colSupplierPurchases.setCellValueFactory(cell ->
            new ReadOnlyObjectWrapper<>(cell.getValue().purchasesCount())
        );
        colSupplierTotal.setCellValueFactory(cell ->
            new ReadOnlyObjectWrapper<>(cell.getValue().totalSpent())
        );

        // Format Total Spent column as "X,XXX.XX DA"
        colSupplierTotal.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText(null);
                } else {
                    setText(String.format("%s DA", currencyFormatter.format(value)));
                }
            }
        });
        // UX Cleanups
        tblTopSuppliers.setPlaceholder(new Label("No supplier records found for this period."));
        tblTopSuppliers.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    private void setTopClientsData(List<ClientDashboardItem> clients) {
        ObservableList<ClientDashboardItem> data = FXCollections.observableArrayList(clients);
        tblTopClients.setItems(data);
    }
    private void setTopSuppliersData(List<SupplierDashboardItem> suppliers) {
        ObservableList<SupplierDashboardItem> data = FXCollections.observableArrayList(suppliers);
        tblTopSuppliers.setItems(data);
    }
    private void setRevenueData(List<RevenueDashboardItem> revenueData) {
        if (salesLineChart == null) return;

        salesLineChart.getData().clear();

        XYChart.Series<String, Number> revenueSeries = new XYChart.Series<>();
        revenueSeries.setName("Revenue");

        for (RevenueDashboardItem item : revenueData) {
            if (item.date() != null) {
                String dateLabel = item.date().toString();
                XYChart.Data<String, Number> dataPoint = new XYChart.Data<>(dateLabel, item.totalRevenue());

                revenueSeries.getData().add(dataPoint);
            }
        }

        // Add series to chart first so the visual nodes get created
        salesLineChart.getData().add(revenueSeries);

        // Attach tooltips to each node
        for (int i = 0; i < revenueData.size(); i++) {
            RevenueDashboardItem item = revenueData.get(i);
            XYChart.Data<String, Number> dataPoint = revenueSeries.getData().get(i);

            if (dataPoint.getNode() != null) {
                String tooltipText = String.format(
                        "Date: %s\nRevenue: $%.2f\nSales Count: %d",
                        item.date(),
                        item.totalRevenue(),
                        (int) item.totalPurchases()
                );

                Tooltip tooltip = new Tooltip(tooltipText);
                // Optional: reduce delay so it pops up fast on hover
                tooltip.setShowDelay(javafx.util.Duration.millis(100));

                Tooltip.install(dataPoint.getNode(), tooltip);
            }
        }
    }
    private void setProductsData(List<ProductDashboardItem> topProducts) {
        if (topProductsChart == null) return;

        topProductsChart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Units Sold");

        for (ProductDashboardItem item : topProducts) {
            XYChart.Data<String, Number> dataPoint = new XYChart.Data<>(item.productName(), item.unitsSold());
            series.getData().add(dataPoint);
        }

        // Add series to chart so visual nodes are generated
        topProductsChart.getData().add(series);

        // Attach tooltips to each dataPoint node
        for (int i = 0; i < topProducts.size(); i++) {
            ProductDashboardItem item = topProducts.get(i);
            XYChart.Data<String, Number> dataPoint = series.getData().get(i);

            if (dataPoint.getNode() != null) {
                String tooltipText = String.format(
                        "Product: %s\nUnits Sold: %d\nTotal Revenue: $%.2f",
                        item.productName(),
                        item.unitsSold(),
                        item.totalRevenue()
                );

                Tooltip tooltip = new Tooltip(tooltipText);
                tooltip.setShowDelay(javafx.util.Duration.millis(100));
                Tooltip.install(dataPoint.getNode(), tooltip);

                // Subtle hover cursor effect
                dataPoint.getNode().setOnMouseEntered(e -> dataPoint.getNode().setStyle("-fx-cursor: hand; -fx-opacity: 0.85;"));
                dataPoint.getNode().setOnMouseExited(e -> dataPoint.getNode().setStyle("-fx-cursor: default; -fx-opacity: 1.0;"));
            }
        }
    }
    private void loadRevenue(){
        List<RevenueDashboardItem> tenDaysRevenue = dashBoardRepository.getRevenue();
        setRevenueData(tenDaysRevenue);
    }
    private void loadRevenue(LocalDate beginningDate, LocalDate endDate){}

    private void loadSuppliers(){
        List<SupplierDashboardItem> topSuppliers = dashBoardRepository.getTopSuppliers();
        setTopSuppliersData(topSuppliers);
    }
    private void loadSuppliers(LocalDate beginningDate, LocalDate endDate){}

    private void loadClients(){
        List<ClientDashboardItem> topClients = dashBoardRepository.getTopClients();
        setTopClientsData(topClients);
    }
    private void loadClients(LocalDate beginningDate, LocalDate endDate){}

    private void loadProducts(){
        List<ProductDashboardItem> topProducts = dashBoardRepository.getTopProducts();
        setProductsData(topProducts);
    }
    private void loadProducts(LocalDate beginningDate, LocalDate endDate){}

}