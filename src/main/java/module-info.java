module com.leelo {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires org.xerial.sqlitejdbc;

    opens com.leelo;
    opens com.leelo.controller to javafx.fxml;
    opens com.leelo.model to javafx.base;
}