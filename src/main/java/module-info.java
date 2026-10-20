module com.taskengine.taskengine {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.taskengine.taskengine to javafx.fxml;
    exports com.taskengine.taskengine;
}