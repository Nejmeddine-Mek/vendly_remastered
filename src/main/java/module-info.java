module org.example.vendly_remastered {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires java.sql;
    requires jbcrypt;

    opens org.example.vendly_remastered to javafx.fxml;
    opens org.example.vendly_remastered.controls to javafx.fxml;
    exports org.example.vendly_remastered;
    exports org.example.vendly_remastered.constants;
    opens org.example.vendly_remastered.constants to javafx.fxml;
    exports org.example.vendly_remastered.utils;
    opens org.example.vendly_remastered.utils to javafx.fxml;
    opens org.example.vendly_remastered.models.entity to javafx.base;
}