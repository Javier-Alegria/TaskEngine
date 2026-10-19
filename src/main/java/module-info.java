module com.taskengine {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    opens com.taskengine.app to javafx.fxml;
    exports com.taskengine.app;

    // Si necesitasemos exportar los otros paquetes en el futuro para que se comuniquen:
    // exports com.taskengine.analyzer;
    // exports com.taskengine.benchmark;
}