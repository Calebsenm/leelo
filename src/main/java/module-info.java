module com.leelo {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires org.xerial.sqlitejdbc;

    requires org.apache.pdfbox;
    requires java.desktop;
    requires org.jsoup;            

    opens com.leelo;
    opens com.leelo.controller to javafx.fxml;
    opens com.leelo.model to javafx.base;
}