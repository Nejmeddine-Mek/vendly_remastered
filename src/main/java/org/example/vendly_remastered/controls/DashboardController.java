package org.example.vendly_remastered.controls;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import org.example.vendly_remastered.models.dto.ClientDashboardItem;
import org.example.vendly_remastered.models.dto.ProductDashboardItem;
import org.example.vendly_remastered.models.dto.RevenueDashboardItem;
import org.example.vendly_remastered.models.dto.SupplierDashboardItem;
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
    @FXML private TableView<ClientDashboardItem> tblTopClients;
    @FXML private TableColumn<ClientDashboardItem, String> colClientName;
    @FXML private TableColumn<ClientDashboardItem, Integer> colClientOrders;
    @FXML private TableColumn<ClientDashboardItem, Double> colClientTotal;

    // --- Top Suppliers Table & Columns ---
    @FXML private TableView<SupplierDashboardItem> tblTopSuppliers;
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
        setupPresetComboBox();
        setupChartCleanups();

        if (btnApplyFilter != null) {
            btnApplyFilter.setOnAction(e -> applyCustomDateFilter());
        }
    }

    public void initializeContent() {
        // Default startup load
        refreshDashboard(null, null);
    }

    private void setupPresetComboBox() {
        if (cmbDatePreset == null) return;

        cmbDatePreset.setItems(FXCollections.observableArrayList(
                "All Time", "Today", "Last 10 Days", "Last 30 Days", "This Month"
        ));
        cmbDatePreset.getSelectionModel().select("Last 10 Days");

        cmbDatePreset.setOnAction(e -> handlePresetSelection(cmbDatePreset.getValue()));
    }

    private void handlePresetSelection(String preset) {
        if (preset == null) return;

        LocalDate today = LocalDate.now();
        LocalDate startDate = null;
        LocalDate endDate = today;

        switch (preset) {
            case "Today" -> startDate = today;
            case "Last 10 Days" -> startDate = today.minusDays(9);
            case "Last 30 Days" -> startDate = today.minusDays(29);
            case "This Month" -> startDate = today.withDayOfMonth(1);
            case "All Time" -> endDate = null;
        }

        if (dpStartDate != null) dpStartDate.setValue(startDate);
        if (dpEndDate != null) dpEndDate.setValue(endDate);

        refreshDashboard(startDate, endDate);
    }

    private void applyCustomDateFilter() {
        LocalDate start = (dpStartDate != null) ? dpStartDate.getValue() : null;
        LocalDate end = (dpEndDate != null) ? dpEndDate.getValue() : null;

        if (cmbDatePreset != null) {
            cmbDatePreset.getSelectionModel().clearSelection();
        }

        refreshDashboard(start, end);
    }

    private void refreshDashboard(LocalDate startDate, LocalDate endDate) {
        loadClients(startDate, endDate);
        loadSuppliers(startDate, endDate);
        loadRevenue(startDate, endDate);
        loadProducts(startDate, endDate);
        loadKpiMetrics(startDate, endDate);
    }

    private void setupChartCleanups() {
        if (topProductsChart != null) {
            topProductsChart.setLegendVisible(false);
            topProductsChart.setAnimated(false);
        }
        if (salesLineChart != null) {
            salesLineChart.setAnimated(false);
        }
    }

    // --- Table Setup Methods ---

    private void setupClientsTable() {
        colClientName.setCellValueFactory(cell ->
                new ReadOnlyObjectWrapper<>(cell.getValue().clientName()));

        colClientOrders.setCellValueFactory(cell ->
                new ReadOnlyObjectWrapper<>(cell.getValue().ordersCount()));

        colClientTotal.setCellValueFactory(cell ->
                new ReadOnlyObjectWrapper<>(cell.getValue().totalSpent()));

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

        tblTopClients.setPlaceholder(new Label("No client records found for this period."));
        tblTopClients.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    private void setupSuppliersTable() {
        colSupplierName.setCellValueFactory(cell ->
                new ReadOnlyObjectWrapper<>(cell.getValue().supplierName()));

        colSupplierPurchases.setCellValueFactory(cell ->
                new ReadOnlyObjectWrapper<>(cell.getValue().purchasesCount()));

        colSupplierTotal.setCellValueFactory(cell ->
                new ReadOnlyObjectWrapper<>(cell.getValue().totalSpent()));

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

        tblTopSuppliers.setPlaceholder(new Label("No supplier records found for this period."));
        tblTopSuppliers.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    // --- Data Binding Methods ---

    private void setTopClientsData(List<ClientDashboardItem> clients) {
        tblTopClients.setItems(FXCollections.observableArrayList(clients));
    }

    private void setTopSuppliersData(List<SupplierDashboardItem> suppliers) {
        tblTopSuppliers.setItems(FXCollections.observableArrayList(suppliers));
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

        salesLineChart.getData().add(revenueSeries);

        for (int i = 0; i < revenueData.size(); i++) {
            RevenueDashboardItem item = revenueData.get(i);
            XYChart.Data<String, Number> dataPoint = revenueSeries.getData().get(i);

            if (dataPoint.getNode() != null) {
                String tooltipText = String.format(
                        "Date: %s\nRevenue: %s DA\nTransactions: %d",
                        item.date(),
                        currencyFormatter.format(item.totalRevenue()),
                        (int) item.totalPurchases()
                );

                Tooltip tooltip = new Tooltip(tooltipText);
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

        topProductsChart.getData().add(series);

        for (int i = 0; i < topProducts.size(); i++) {
            ProductDashboardItem item = topProducts.get(i);
            XYChart.Data<String, Number> dataPoint = series.getData().get(i);

            if (dataPoint.getNode() != null) {
                String tooltipText = String.format(
                        "Product: %s\nUnits Sold: %d\nTotal Revenue: %s DA",
                        item.productName(),
                        item.unitsSold(),
                        currencyFormatter.format(item.totalRevenue())
                );

                Tooltip tooltip = new Tooltip(tooltipText);
                tooltip.setShowDelay(javafx.util.Duration.millis(100));
                Tooltip.install(dataPoint.getNode(), tooltip);

                dataPoint.getNode().setOnMouseEntered(e -> dataPoint.getNode().setStyle("-fx-cursor: hand; -fx-opacity: 0.85;"));
                dataPoint.getNode().setOnMouseExited(e -> dataPoint.getNode().setStyle("-fx-cursor: default; -fx-opacity: 1.0;"));
            }
        }
    }

    // --- DAO Loader Implementations ---

    private void loadRevenue(LocalDate beginningDate, LocalDate endDate) {
        List<RevenueDashboardItem> revenueData = (beginningDate == null && endDate == null)
                ? dashBoardRepository.getRevenue()
                : dashBoardRepository.getRevenue(beginningDate, endDate);

        setRevenueData(revenueData);
    }

    private void loadSuppliers(LocalDate beginningDate, LocalDate endDate) {
        List<SupplierDashboardItem> topSuppliers = (beginningDate == null && endDate == null)
                ? dashBoardRepository.getTopSuppliers()
                : dashBoardRepository.getTopSuppliers(beginningDate, endDate);

        setTopSuppliersData(topSuppliers);
    }

    private void loadClients(LocalDate beginningDate, LocalDate endDate) {
        List<ClientDashboardItem> topClients = (beginningDate == null && endDate == null)
                ? dashBoardRepository.getTopClients()
                : dashBoardRepository.getTopClients(beginningDate, endDate);

        setTopClientsData(topClients);
    }

    private void loadProducts(LocalDate beginningDate, LocalDate endDate) {
        List<ProductDashboardItem> topProducts = (beginningDate == null && endDate == null)
                ? dashBoardRepository.getTopProducts()
                : dashBoardRepository.getTopProducts(beginningDate, endDate);

        setProductsData(topProducts);
    }

    // --- KPI Card Summaries ---

    private void loadKpiMetrics(LocalDate beginningDate, LocalDate endDate) {
        // Fetch revenue dataset for aggregate sum
        List<RevenueDashboardItem> revenueList = (beginningDate == null && endDate == null)
                ? dashBoardRepository.getRevenue()
                : dashBoardRepository.getRevenue(beginningDate, endDate);

        double totalRevenue = revenueList.stream()
                .mapToDouble(RevenueDashboardItem::totalRevenue)
                .sum();

        long totalTransactions = (long) revenueList.stream()
                .mapToDouble(RevenueDashboardItem::totalPurchases)
                .sum();

        if (lblTotalRevenue != null) {
            lblTotalRevenue.setText(String.format("%s DA", currencyFormatter.format(totalRevenue)));
        }

        if (lblTotalPurchases != null) {
            lblTotalPurchases.setText(String.format("%d", totalTransactions));
        }

        // Inventory Alerts (Call repository count methods if available in IDashBoardRepository)
        if (lblLowStockCount != null) {
            int lowStock = dashBoardRepository.getLowStockCount();
            lblLowStockCount.setText(String.valueOf(lowStock));
        }

        if (lblExpiringCount != null) {
            int expiring = dashBoardRepository.getExpiringProductsCount();
            lblExpiringCount.setText(String.valueOf(expiring));
        }
    }
}