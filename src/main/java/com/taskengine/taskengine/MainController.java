package com.taskengine.taskengine;

import com.taskengine.taskengine.textanalyzer.Lanzador;
import com.taskengine.taskengine.textanalyzer.Lanzador.DestinoSalida;
import com.taskengine.taskengine.textanalyzer.Lanzador.Estado;
import com.taskengine.taskengine.textanalyzer.Lanzador.OrigenEntrada;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class MainController {
    private static final String TEXT_ANALYZER = "Text Analyzer";

    // Textos de los ComboBox del panel 4 (deben coincidir con main-view.fxml)
    private static final String OPCION_CAPTURAR = "Capturar en la aplicación";
    private static final String OPCION_FICHERO = "Guardar en fichero";
    private static final String OPCION_DESCARTAR = "Descartar";
    private static final String STDIN_APLICACION = "Desde la aplicación";   // -> OrigenEntrada.DESDE_DATAFORGE
    private static final String STDIN_FICHERO = "Leer desde fichero";       // -> OrigenEntrada.FICHERO_ARGUMENTO

    private final DateTimeFormatter formatoTiempo = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    /** Una fila del monitor = un Lanzador = un proceso lanzado. */
    private final ObservableList<Lanzador> procesos = FXCollections.observableArrayList();

    // Lote en curso (lo que se lanzó con el último "Iniciar ejecución")
    private final List<Lanzador> loteActual = new ArrayList<>();
    private final Deque<Lanzador> colaSecuencial = new ArrayDeque<>();

    // Variables de control de procesos
    private int procesosEjecutados = 0;
    private int procesosFinalizados = 0;
    private int procesosEnEjecucion = 0;
    private int procesosConError = 0;
    private int siguienteId = 1;

    // Contador del tiempo total del lote
    private long inicioTiempoTotal;
    private Timeline contadorTiempoTotal;

    // Comprobación periódica de los procesos (sin hilos: se ejecuta en el hilo de JavaFX)
    private Timeline monitor;

    // Panel 1
    @FXML private ComboBox<String> cmbTarea;
    @FXML private Label lblDescripcion;

    // Panel 2
    @FXML private VBox configTextAnalyzer;
    @FXML private VBox configPrimeBenchmark;
    @FXML private VBox cajaFichero;
    @FXML private VBox cajaTextoStdin;
    @FXML private TextField txtFichero;
    @FXML private TextField txtPalabra;
    @FXML private TextArea txtTextoStdin;
    @FXML private TextField txtLimiteN;

    // Panel 3
    @FXML private ToggleButton tglParalelo;
    @FXML private Spinner<Integer> spnProcesos;
    @FXML private Button btnIniciar;

    // Panel 4
    @FXML private ComboBox<String> cmbStdout;
    @FXML private ComboBox<String> cmbStderr;
    @FXML private ComboBox<String> cmbStdin;
    @FXML private TextField txtArchivoStdout;
    @FXML private TextField txtArchivoStderr;
    @FXML private TitledPane tpRedirecciones;

    // Panel 5
    @FXML private TableView<Lanzador> tblProcesos;
    @FXML private TableColumn<Lanzador, Integer> colId;
    @FXML private TableColumn<Lanzador, Long> colPid;
    @FXML private TableColumn<Lanzador, String> colTarea;
    @FXML private TableColumn<Lanzador, String> colParametros;
    @FXML private TableColumn<Lanzador, LocalTime> colInicio;
    @FXML private TableColumn<Lanzador, LocalTime> colFin;
    @FXML private TableColumn<Lanzador, Long> colTiempo;
    @FXML private TableColumn<Lanzador, Integer> colCodigoSalida;
    @FXML private TableColumn<Lanzador, Estado> colEstado;

    // Panel 6 y 7
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


    @FXML
    private void initialize() {
        configurarTabla();
        setHoraInicio();
        actualizarArchivos();
        actualizarEstadisticas();

        cmbTarea.valueProperty().addListener((obs, anterior, nueva) -> actualizarTarea(nueva));
        actualizarTarea(cmbTarea.getValue());

        // Cada 100 ms se revisan los procesos en ejecución: se lee su salida disponible
        // y se detecta si han terminado. Así la ventana no se congela y se puede usar destroy().
        monitor = new Timeline(new KeyFrame(Duration.millis(100), e -> revisarProcesos()));
        monitor.setCycleCount(Timeline.INDEFINITE);
    }

    // =====================================================================
    //                         LANZAR EJECUCIONES
    // =====================================================================

    /** Botón "Iniciar ejecución" (panel 3). */
    @FXML
    private void iniciarEjecucion() {
        if (hayLoteEnCurso()) {
            avisar("Ya hay una ejecución en curso. Espera a que termine o detén los procesos.");
            return;
        }

        if (!TEXT_ANALYZER.equals(cmbTarea.getValue())) {
            avisar("Prime Benchmark todavía no está implementado.");   // TODO: Lanzador.primeBenchmark(...)
            return;
        }

        // ---------- 1. Leer la configuración de la GUI ----------
        String palabra = txtPalabra.getText().trim();
        if (palabra.isEmpty()) {
            avisar("Escribe la palabra a buscar.");
            return;
        }

        OrigenEntrada entrada = STDIN_FICHERO.equals(cmbStdin.getValue()) ? OrigenEntrada.FICHERO_ARGUMENTO : OrigenEntrada.DESDE_DATAFORGE;

        String ruta = txtFichero.getText().trim();
        if (entrada == OrigenEntrada.FICHERO_ARGUMENTO && ruta.isEmpty()) {
            avisar("Indica el fichero de entrada (o elige stdin \"" + STDIN_APLICACION + "\").");
            return;
        }
        // No se comprueba si el fichero existe. Eso lo hace TextAnalyzer (stderr + código 2).

        DestinoSalida destinoOut = destinoDe(cmbStdout.getValue());
        DestinoSalida destinoErr = destinoDe(cmbStderr.getValue());
        File ficheroOut = ficheroDe(txtArchivoStdout);
        File ficheroErr = ficheroDe(txtArchivoStderr);

        spnProcesos.increment(0);               // confirma el número si se ha escrito a mano
        int n = spnProcesos.getValue();
        boolean paralelo = tglParalelo.isSelected();

        // ---------- 2. Crear una ejecución (fila) por cada proceso ----------
        loteActual.clear();
        colaSecuencial.clear();
        for (int i = 0; i < n; i++) {
            Lanzador l = Lanzador.textAnalyzer(siguienteId, entrada, ruta, txtTextoStdin.getText(), palabra);
            l.setRedirecciones(destinoOut, ficheroOut, destinoErr, ficheroErr);

            if (i == 0) {   // todas tienen la misma configuración: basta con validar la primera
                String error = l.validar();
                if (error != null) {
                    avisar(error);
                    return;
                }
            }
            siguienteId++;
            loteActual.add(l);
        }
        procesos.addAll(loteActual);
        tblProcesos.getSelectionModel().select(loteActual.get(0));

        // ---------- 3. Lanzar según el modo ----------
        iniciarContadorTiempoTotal();
        if (paralelo) {
            for (Lanzador l : loteActual) {
                lanzar(l);                      // todos a la vez
            }
        } else {
            colaSecuencial.addAll(loteActual);  // de uno en uno: el siguiente cuando acabe el anterior
            lanzarSiguienteSecuencial();
        }

        monitor.play();
        comprobarFinDeLote();
        actualizarEstadisticas();
        tblProcesos.refresh();
    }

    /** Lanza una ejecución y actualiza los contadores. @return true si el proceso se ha creado */
    private boolean lanzar(Lanzador l) {
        try {
            l.iniciar();
            procesosEjecutados++;
            procesosEnEjecucion++;
            return true;
        } catch (IOException e) {
            // start() ha fallado: no hay proceso, ni PID, ni código de salida. La fila queda en ERROR.
            procesosConError++;
            mostrarSalidas(l);
            return false;
        }
    }

    /** Modo secuencial: lanza el siguiente de la cola que siga en espera. */
    private void lanzarSiguienteSecuencial() {
        while (!colaSecuencial.isEmpty()) {
            Lanzador siguiente = colaSecuencial.poll();
            if (siguiente.getEstado() == Estado.EN_ESPERA && lanzar(siguiente)) {
                tblProcesos.getSelectionModel().select(siguiente);
                return;
            }
            // Si estaba cancelado o no se pudo lanzar, se pasa al siguiente
        }
    }

    // =====================================================================
    //                 SEGUIMIENTO DE LOS PROCESOS (Timeline)
    // =====================================================================

    /** Lo ejecuta el Timeline cada 100 ms. */
    private void revisarProcesos() {
        boolean algunoTerminado = false;

        for (Lanzador l : loteActual) {
            if (l.actualizar()) {              // true = acaba de terminar
                procesoTerminado(l);
                algunoTerminado = true;
            }
        }

        // Mostrar la salida en vivo de la fila seleccionada
        Lanzador seleccionado = tblProcesos.getSelectionModel().getSelectedItem();
        if (seleccionado != null && (seleccionado.getEstado() == Estado.EJECUTANDO || algunoTerminado)) {
            mostrarSalidas(seleccionado);
        }

        if (algunoTerminado) {
            tblProcesos.refresh();
            actualizarEstadisticas();
            comprobarFinDeLote();
        }
    }

    private void procesoTerminado(Lanzador l) {
        procesosEnEjecucion--;
        switch (l.getEstado()) {
            case FINALIZADO -> procesosFinalizados++;
            case ERROR -> procesosConError++;
            default -> { }   // CANCELADO: no cuenta ni como finalizado ni como error
        }

        // Modo secuencial: al terminar uno se lanza el siguiente (en paralelo la cola está vacía)
        if (!colaSecuencial.isEmpty()) {
            lanzarSiguienteSecuencial();
        }
    }

    /** Si todos los procesos del lote han terminado, para el Timeline y el contador. */
    private void comprobarFinDeLote() {
        long terminados = loteActual.stream().filter(Lanzador::estaTerminado).count();
        double progreso = loteActual.isEmpty() ? 0 : (double) terminados / loteActual.size();
        pgbLote.setProgress(progreso);
        lblPorcentaje.setText(Math.round(progreso * 100) + "%");

        if (!hayLoteEnCurso()) {
            monitor.stop();
            if (contadorTiempoTotal != null) contadorTiempoTotal.stop();
        }
    }

    private boolean hayLoteEnCurso() {
        return loteActual.stream().anyMatch(l -> !l.estaTerminado());
    }

    // =====================================================================
    //                      DETENER (Process.destroy())
    // =====================================================================

    /** Botón "Detener ejecución" (panel 5): detiene el proceso de la fila seleccionada. */
    @FXML
    private void detenerEjecucion() {
        Lanzador l = tblProcesos.getSelectionModel().getSelectedItem();
        if (l == null) {
            avisar("Selecciona en el monitor el proceso que quieres detener.");
            return;
        }
        if (l.estaTerminado()) {
            avisar("El proceso " + l.getId() + " ya ha terminado.");
            return;
        }

        l.detener();
        // Si estaba EJECUTANDO, el Timeline detectará en la próxima vuelta que ha terminado
        // y lo marcará como CANCELADO. Si estaba EN_ESPERA, ya está cancelado.
        tblProcesos.refresh();
        comprobarFinDeLote();
    }

    // =====================================================================
    //                    MOSTRAR SALIDAS (paneles 6 y 7)
    // =====================================================================

    /**
     * Muestra stdout y stderr de una ejecución. Solo se muestra texto si se eligió "Capturar";
     * si se eligió fichero o descartar, se muestra un aviso (el PDF pide no inventar contenido).
     */
    private void mostrarSalidas(Lanzador l) {
        if (l == null) {
            txtStdout.clear();
            txtStderr.clear();
            return;
        }

        txtStdout.setText(textoCanal("stdout", l.getDestinoStdout(), l.getFicheroStdout(), l.getStdout()));

        String err = textoCanal("stderr", l.getDestinoStderr(), l.getFicheroStderr(), l.getStderr());
        if (l.getErrorAlLanzar() != null) {
            err = l.getErrorAlLanzar() + "\n" + err;
        }
        txtStderr.setText(err);

        // Bajar el scroll al final para ver lo último
        txtStdout.positionCaret(txtStdout.getLength());
        txtStderr.positionCaret(txtStderr.getLength());
    }

    private String textoCanal(String canal, DestinoSalida destino, File fichero, String capturado) {
        return switch (destino) {
            case CAPTURAR -> capturado;
            case FICHERO -> canal + " → FICHERO: salida guardada en\n" + fichero.getAbsolutePath();
            case DISCARD -> canal + " → DISCARD: salida descartada";
        };
    }

    /** Muestra un mensaje de la propia aplicación (no de un proceso) en el panel stderr. */
    private void avisar(String mensaje) {
        txtStderr.setText("[DataForge] " + mensaje);
    }

    // =====================================================================
    //                       CONFIGURACIÓN DE LA GUI
    // =====================================================================

    /** Botón "Examinar..." del fichero de entrada. */
    @FXML
    private void examinarFichero() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Fichero de entrada");
        File datos = new File("src/datos");
        if (datos.isDirectory()) fc.setInitialDirectory(datos);

        File f = fc.showOpenDialog(txtFichero.getScene().getWindow());
        if (f != null) txtFichero.setText(f.getAbsolutePath());
    }

    /**
     * Al cambiar un ComboBox de redirección. Si se elige "Guardar en fichero" para stdout/stderr,
     * se abre un selector de archivos y se muestra la ruta completa.
     */
    @FXML
    private void cambiarRedireccion(ActionEvent evento) {
        Object origen = evento.getSource();
        if (origen == cmbStdout && OPCION_FICHERO.equals(cmbStdout.getValue())) {
            elegirFicheroSalida(txtArchivoStdout, "stdout");
        } else if (origen == cmbStderr && OPCION_FICHERO.equals(cmbStderr.getValue())) {
            elegirFicheroSalida(txtArchivoStderr, "stderr");
        }
        actualizarArchivos();
    }

    private void elegirFicheroSalida(TextField destino, String canal) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Fichero de destino para " + canal);
        fc.setInitialFileName(canal + ".txt");
        File datos = new File("src/datos");
        if (datos.isDirectory()) fc.setInitialDirectory(datos);

        File f = fc.showSaveDialog(destino.getScene().getWindow());
        if (f != null) {
            destino.setText(f.getAbsolutePath());
            tpRedirecciones.setExpanded(true);
        }
    }

    /** Muestra u oculta los campos según las opciones de redirección elegidas. */
    private void actualizarArchivos() {
        boolean verStdout = OPCION_FICHERO.equals(cmbStdout.getValue());
        boolean verStderr = OPCION_FICHERO.equals(cmbStderr.getValue());
        boolean modoFichero = STDIN_FICHERO.equals(cmbStdin.getValue());

        mostrar(txtArchivoStdout, verStdout);
        mostrar(txtArchivoStderr, verStderr);

        // Panel 2: en modo fichero se pide la ruta; en modo stdin, el texto a enviar
        mostrar(cajaFichero, modoFichero);
        mostrar(cajaTextoStdin, !modoFichero);
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

    private DestinoSalida destinoDe(String opcion) {
        if (OPCION_FICHERO.equals(opcion)) return DestinoSalida.FICHERO;
        if (OPCION_DESCARTAR.equals(opcion)) return DestinoSalida.DISCARD;
        return DestinoSalida.CAPTURAR;
    }

    private File ficheroDe(TextField campo) {
        String texto = campo.getText().trim();
        return texto.isEmpty() ? null : new File(texto);
    }

    private void actualizarEstadisticas() {
        lblEjecutados.setText(String.valueOf(procesosEjecutados));
        lblFinalizados.setText(String.valueOf(procesosFinalizados));
        lblEnEjecucion.setText(String.valueOf(procesosEnEjecucion));
        lblConError.setText(String.valueOf(procesosConError));
        // Para el footer
        lblProcesos.setText("Procesos: " + procesosEjecutados);
    }

    // TODO:COMPROBAR CON LA PROFE PROBLEMA:
    //                      NO HEMOS DADO EL TimeLine PERO ES NECESARIO PARA EL CONTADOR
    //                      (y también para revisar los procesos sin congelar la ventana)
    private void iniciarContadorTiempoTotal() {
        if (contadorTiempoTotal != null) {
            contadorTiempoTotal.stop();
        }

        inicioTiempoTotal = System.nanoTime();

        contadorTiempoTotal = new Timeline(
                new KeyFrame(Duration.millis(100), evento -> {
                    long tiempoMs = (System.nanoTime() - inicioTiempoTotal) / 1_000_000;

                    long horas = tiempoMs / 3_600_000;
                    long minutos = (tiempoMs % 3_600_000) / 60_000;
                    long segundos = (tiempoMs % 60_000) / 1_000;
                    long milisegundos = tiempoMs % 1_000;

                    lblTiempoTotal.setText(String.format("%02d:%02d:%02d.%03d",
                            horas, minutos, segundos, milisegundos));
                })
        );

        contadorTiempoTotal.setCycleCount(Timeline.INDEFINITE);
        contadorTiempoTotal.play();
    }

    private void configurarTabla() {
        tblProcesos.setItems(procesos);

        colId.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getId()));
        colPid.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getPid()));
        colTarea.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getTarea()));
        colParametros.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getParametros()));
        colInicio.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getInicio()));
        colFin.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getFin()));
        colTiempo.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getTiempoMs()));
        colCodigoSalida.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getCodigoSalida()));
        colEstado.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getEstado()));

        // Al seleccionar una fila se muestran su stdout y su stderr
        tblProcesos.getSelectionModel().selectedItemProperty()
                .addListener((obs, anterior, seleccionado) -> mostrarSalidas(seleccionado));
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
     * Borra el texto de la salida por la aplicación
     * @param actionEvent acción evento
     */
    public void limpiarTexto(ActionEvent actionEvent) {
        Button botonPulsado = (Button) actionEvent.getSource();

        if (botonPulsado == btnLimpiarStdout) {
            txtStdout.clear();
        } else if (botonPulsado == btnLimpiarStderr) {
            txtStderr.clear();
        }
    }

    private void mostrar(javafx.scene.Node nodo, boolean visible) {
        nodo.setVisible(visible);
        nodo.setManaged(visible);
    }
}