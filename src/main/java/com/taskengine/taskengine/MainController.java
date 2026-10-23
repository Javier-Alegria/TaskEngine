package com.taskengine.taskengine;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

// Imports de clase Proceso
import com.taskengine.taskengine.model.Proceso;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
//

public class MainController {
    private static final String TEXT_ANALYZER = "Text Analyzer";
    private final DateTimeFormatter formatoTiempo = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    // Panel 1
    @FXML
    private ComboBox<String> cmbTarea;
    @FXML
    private Label lblDescripcion;

    // Panel 2
    @FXML
    private VBox configTextAnalyzer;
    @FXML
    private VBox configPrimeBenchmark;

    // Panel 5
    @FXML
    private TableView<Proceso> tblProcesos;
    @FXML
    private TableColumn<Proceso, Integer> colId;
    @FXML
    private TableColumn<Proceso, Long> colPid;
    @FXML
    private TableColumn<Proceso, String> colTarea;
    @FXML
    private TableColumn<Proceso, String> colParametros;
    @FXML
    private TableColumn<Proceso, LocalTime> colInicio;
    @FXML
    private TableColumn<Proceso, LocalTime> colFin;
    @FXML
    private TableColumn<Proceso, Long> colTiempo;
    @FXML
    private TableColumn<Proceso, Integer> colCodigoSalida;
    @FXML
    private TableColumn<Proceso, Proceso.Estado> colEstado;

    private final ObservableList<Proceso> procesos = FXCollections.observableArrayList();

    // Panel 6
    @FXML private TextArea txtStdout;
    @FXML private TextArea txtStderr;
    @FXML private Button btnLimpiarStdout;
    @FXML private Button btnLimpiarStderr;

    // Barra de tareas
    @FXML private Label lblUsuario;
    @FXML private Label lblModo;
    @FXML private Label lblProcesos;
    @FXML private Label lblInicio;


    @FXML
    private void initialize() {
        configurarTabla();
        setHoraInicio();

        cmbTarea.valueProperty().addListener((obs, anterior, nueva) -> actualizarTarea(nueva));
        actualizarTarea(cmbTarea.getValue());
    }

    private void actualizarTarea(String tarea) {
        boolean esTextAnalyzer = TEXT_ANALYZER.equals(tarea);

        mostrar(configTextAnalyzer, esTextAnalyzer);
        mostrar(configPrimeBenchmark, !esTextAnalyzer);

        if (esTextAnalyzer) {
            lblDescripcion.setText("Analiza un fichero de texto y devuelve estadísticas por la salida estándar.");
        } else {
            lblDescripcion.setText("Calcula los números primos hasta N y mide el tiempo de ejecución.");
        }
    }

    private void configurarTabla() {
        tblProcesos.setItems(procesos);

        colId.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getId()));
        colPid.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getPid()));
        colTarea.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getTarea()));
        colParametros.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getParametros()));
        colInicio.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getInicio()));

        colFin.setCellValueFactory(c -> c.getValue().finProperty());
        colTiempo.setCellValueFactory(c -> c.getValue().tiempoMsProperty());
        colCodigoSalida.setCellValueFactory(c -> c.getValue().codigoSalidaProperty());
        colEstado.setCellValueFactory(c -> c.getValue().estadoProperty());
    }

    /**
     * Actualiza el texto de 'modo' en el Footer según el botón pulsado
     * @param actionEvent acción evento
     */
    public void actualizarModoFooter(ActionEvent actionEvent) {
        ToggleButton botonPulsado = (ToggleButton) actionEvent.getSource();
        lblModo.setText("Modo: " + botonPulsado.getText());
    }

    /**
     * Establece la hora del Footer a la hora en la que se inicia el sistema
     */
    private void setHoraInicio() {
        lblInicio.setText("Inicio: " + LocalTime.now().format(formatoTiempo));
    }

    /**
     * borra el texto de la salida por la aplicación
     * @param actionEvent acción evento
     */
    public void limpiarTexto(ActionEvent actionEvent) {
        Button botonPulsado = (Button) actionEvent.getSource();

        if(botonPulsado == btnLimpiarStdout) {
            txtStdout.clear();
            System.out.println("Texto Limpiado: STdout");
        } else if (botonPulsado == btnLimpiarStderr) {
            txtStderr.clear();
            System.out.println("Texto Limpiado: STderr");
        }
    }

    private void mostrar(VBox bloque, boolean visible) {
        bloque.setVisible(visible);
        bloque.setManaged(visible);
    }


}
