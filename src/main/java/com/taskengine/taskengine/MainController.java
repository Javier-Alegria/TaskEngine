package com.taskengine.taskengine;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

// Imports de clase Proceso
import com.taskengine.taskengine.model.Proceso;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
//

// Imports del contador de tiempo
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.util.Duration;

public class MainController {
    private static final String TEXT_ANALYZER = "Text Analyzer";
    private final DateTimeFormatter formatoTiempo = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private final ObservableList<Proceso> procesos = FXCollections.observableArrayList();

    // Variables de control de procesos
    private int procesosEjecutados = 0;
    private int procesosFinalizados = 0;
    private int procesosEnEjecucion = 0;
    private int procesosConError = 0;
    private int siguienteId = 1;

    // Contador del tiempo total
    private long inicioTiempoTotal;
    private Timeline contadorTiempoTotal;


    // Panel 1
    @FXML private ComboBox<String> cmbTarea;
    @FXML private Label lblDescripcion;

    // Panel 2
    @FXML private VBox configTextAnalyzer;
    @FXML private VBox configPrimeBenchmark;

    // Panel 3
    @FXML private ToggleButton tglParalelo;
    @FXML private Spinner<Integer> spnProcesos;

    // Panel 4
    @FXML private ComboBox<String> cmbStdout;
    @FXML private ComboBox<String> cmbStderr;
    @FXML private ComboBox<String> cmbStdin;
    @FXML private TextField txtArchivoStdout;
    @FXML private TextField txtArchivoStderr;
    @FXML private TextField txtArchivoStdin;

    // Panel 5
    @FXML private TableView<Proceso> tblProcesos;
    @FXML private TableColumn<Proceso, Integer> colId;
    @FXML private TableColumn<Proceso, Long> colPid;
    @FXML private TableColumn<Proceso, String> colTarea;
    @FXML private TableColumn<Proceso, String> colParametros;
    @FXML private TableColumn<Proceso, LocalTime> colInicio;
    @FXML private TableColumn<Proceso, LocalTime> colFin;
    @FXML private TableColumn<Proceso, Long> colTiempo;
    @FXML private TableColumn<Proceso, Integer> colCodigoSalida;
    @FXML private TableColumn<Proceso, Proceso.Estado> colEstado;

    // Panel 6
    @FXML private TextArea txtStdout;
    @FXML private TextArea txtStderr;
    @FXML private Button btnLimpiarStdout;
    @FXML private Button btnLimpiarStderr;

    // Panel 8
    @FXML private Label lblEjecutados;
    @FXML private Label lblFinalizados;
    @FXML private Label lblEnEjecucion;
    @FXML private Label lblConError;
    @FXML private Label lblTiempoTotal;
    @FXML private ProgressBar pgbLote;
    @FXML private Label lblPorcentaje;

    // Barra de tareas/footer
    @FXML private Label lblUsuario;
    @FXML private Label lblModo;
    @FXML private Label lblProcesos;
    @FXML private Label lblInicio;

    /* TODO:
        == REDIRECCIÓN DE ARCHIVOS (PANEL 4) ==
            == STDOUT ==
            == Si se selecciona Leer desde la aplicación ==
                - Al seleccionar una fila del monitor, se debe poder consultar la salida de ese proceso en el panel de stdout (Panel 6)
            == Si se selecciona Fichero ==
                - El profesor debe poder abrir el fichero y comprobar su contenido. (No me queda muy claro a que se refiere o como hacerlo exactamente)
            == Si se selecciona Discard ==
                - En el panel de STDOUT debe salir -> "stdout → DISCARD: salida descartada"


       == PANEL 3 MODO DE EJECUCIÓN
            - Si se selecciona secuencial se espera a que termine un proceso para empezar el siguiente con waitFor
            - Si se selecciona paralelo ...
     */


    @FXML
    private void initialize() {
        configurarTabla();
        setHoraInicio();
        actualizarArchivos();

        cmbTarea.valueProperty().addListener((obs, anterior, nueva) -> actualizarTarea(nueva));
        actualizarTarea(cmbTarea.getValue());

        //PROCESOS DE PRUEBA (NO SE CREAN ASÍ ES SOLO PARA PROBAR QUE LA TABLA FUNCIONA)
        // A PARTIR DE AQUI SE BORRARÁ PARA CUANDO TENGAMOS EL MÉTDODO TEXT ANALYZER FINALIZADO

        // Espera 3 segundos sin bloquear la interfaz
        PauseTransition espera1 = new PauseTransition(Duration.seconds(3));

        Proceso procesoPrueba = crearProcesoPrueba(
                "Text Analyzer",
                "ejemplo_texto.txt | palabra: programación"
        );

        // Espera 3 segundos sin bloquear la interfaz
        PauseTransition espera = new PauseTransition(Duration.seconds(3));

        espera.setOnFinished(evento -> {
            terminarProceso(procesoPrueba, 0);
        });

        espera.play();
    }

    @FXML
    private void actualizarArchivos() {
        boolean verStdout = "Guardar en fichero".equals(cmbStdout.getValue());
        boolean verStderr = "Guardar en fichero".equals(cmbStderr.getValue());
        boolean verStdin = "Leer desde fichero".equals(cmbStdin.getValue());

        txtArchivoStdout.setVisible(verStdout);
        txtArchivoStdout.setManaged(verStdout);

        txtArchivoStderr.setVisible(verStderr);
        txtArchivoStderr.setManaged(verStderr);

        txtArchivoStdin.setVisible(verStdin);
        txtArchivoStdin.setManaged(verStdin);
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

    private void actualizarEstadisticas() {
        lblEjecutados.setText("Ejectuados: " + procesosEjecutados);
        lblFinalizados.setText("Finalizados: " + procesosFinalizados);
        lblEnEjecucion.setText("En Ejecucion: " + procesosEnEjecucion);
        lblConError.setText("Con Error: " + procesosConError);
        // Para el footer
        lblProcesos.setText("Procesos: " + procesosEjecutados);
    }

    private Proceso crearProcesoPrueba(String tarea, String parametros) {
        Proceso proceso = new Proceso(
                siguienteId,
                1000L + siguienteId, // PID ficticio para las pruebas
                tarea,
                parametros
        );

        proceso.setStdout("Línea de prueba 1\nLínea de prueba 2");

        siguienteId++;

        procesos.add(proceso);

        procesosEjecutados++;
        procesosEnEjecucion++;

        actualizarEstadisticas();
        iniciarContadorTiempoTotal();

        return proceso;
    }

    // TODO:COMPROBAR CON LA PROFE PROBLEMA:
    //                      NO HEMOS DADO EL TimeLine PERO ES NECESARIO PARA EL CONTADOR
    private void iniciarContadorTiempoTotal() {
        if (contadorTiempoTotal != null) {
            return;
        }

        inicioTiempoTotal = System.nanoTime();

        contadorTiempoTotal = new Timeline(
                new KeyFrame(Duration.millis(100), evento -> {
                    long tiempoMs =
                            (System.nanoTime() - inicioTiempoTotal) / 1_000_000;

                    long horas = tiempoMs / 3_600_000;
                    long minutos = (tiempoMs % 3_600_000) / 60_000;
                    long segundos = (tiempoMs % 60_000) / 1_000;
                    long milisegundos = tiempoMs % 1_000;

                    lblTiempoTotal.setText(String.format(
                            "Tiempo total: %02d:%02d:%02d.%03d",
                            horas, minutos, segundos, milisegundos
                    ));
                })
        );

        contadorTiempoTotal.setCycleCount(Timeline.INDEFINITE);
        contadorTiempoTotal.play();
    }

    /**
     * Termina un proceso en ejecución
     * @param proceso que queremos terminar
     * @param codigo 0 si se ha terminado correctamente | 2 si ha habido un error
     */
    private void terminarProceso(Proceso proceso, int codigo) {

        //TODO:[IMPORTANTE] comprobar el pdf que el código de error puede ser así y no de una forma específica

        // Si el proceso que se intenta terminar no está en ejecución, se termina
        if (proceso.getEstado() != Proceso.Estado.EJECUTANDO) {
            return;
        }

        proceso.finalizar(codigo);

        procesosEnEjecucion--;

        if (codigo == 0) {
            procesosFinalizados++;
        } else {
            procesosConError++;
        }

        actualizarEstadisticas();
        tblProcesos.refresh();
    }

    /**
     * Crea un proceso, actualizando las estadísticas globales y añadiendolo a la lista de procesos lanzados por la app
     * @param pid PID real del proceso lanzado
     * @param tarea Texto que indica si ha sido lanzado por el text analyzer o el prime benchmark
     * @param parametros Parámetros que han sido pasados al proceso
     * @return //TODO
     */
    private Proceso crearProceso(long pid, String tarea, String parametros) { //TODO: Terminar proceso
        Proceso process = new Proceso(siguienteId++, pid, tarea, parametros);
        procesos.add(process);

        procesosEjecutados++;
        procesosEnEjecucion++;



        //TODO:actualizarEstadisticas();

        return process;
    }

    private void configurarTabla() {
        tblProcesos.setItems(procesos);

        colId.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue().getId()));
        colPid.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue().getPid()));
        colTarea.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue().getTarea()));
        colParametros.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue().getParametros()));
        colInicio.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue().getInicio()));

        colFin.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue().getFin()));
        colTiempo.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue().getTiempoMs()));
        colCodigoSalida.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue().getCodigoSalida()));
        colEstado.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue().getEstado()));

        tblProcesos.getSelectionModel().selectedItemProperty()
                .addListener((obs, anterior, seleccionado) -> mostrarStdout(seleccionado));
    }

    private void mostrarStdout(Proceso proceso) {
        if (proceso == null) {
            txtStdout.clear();
        } else {
            txtStdout.setText(proceso.getStdout());
        }
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