package com.taskengine.taskengine.textanalyzer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Cada objeto Lanzador representa UNA ejecución de un proceso externo (una fila del monitor).
 *
 * Responsabilidades:
 *  - Montar el comando y configurar las redirecciones del ProcessBuilder.
 *  - Lanzar el proceso con start() y guardar el Process (PID, destroy(), código de salida).
 *  - Leer stdout / stderr cuando se capturan en DataForge y guardarlos como texto.
 *
 * NO toca la interfaz: no conoce TextArea, TableView ni nada de JavaFX.
 * El MainController es quien lee los datos de aquí y los muestra.
 */
public class Lanzador {

    public enum DestinoSalida { CAPTURAR, FICHERO, DISCARD }

    /** Cómo recibe el texto TextAnalyzer. */
    public enum OrigenEntrada {
        DESDE_DATAFORGE,    // stdin: DataForge escribe el texto con getOutputStream()  -> TextAnalyzer <palabra>
        FICHERO_ARGUMENTO   // modo fichero: la ruta va como argumento                  -> TextAnalyzer <ruta> <palabra>
    }

    public enum Estado {
        EN_ESPERA("En espera"),     // creado pero aún sin start() (modo secuencial)
        EJECUTANDO("Ejecutando"),
        FINALIZADO("Finalizado"),   // código 0
        ERROR("Error"),             // código distinto de 0 o fallo al lanzar
        CANCELADO("Cancelado");     // detenido por el usuario con destroy()

        private final String texto;
        Estado(String texto) { this.texto = texto; }
        @Override public String toString() { return texto; }
    }

    // ---------- Datos de configuración (se conocen al crear la ejecución) ----------
    private final int id;
    private final String tarea;
    private final String parametros;          // texto que se muestra en la columna "Parámetros"
    private final List<String> comando;
    private final OrigenEntrada origenEntrada;
    private final String textoStdin;          // solo se usa en DESDE_DATAFORGE

    private DestinoSalida destinoStdout = DestinoSalida.CAPTURAR;
    private DestinoSalida destinoStderr = DestinoSalida.CAPTURAR;
    private File ficheroStdout;
    private File ficheroStderr;

    // ---------- Datos que cambian durante / al final de la ejecución ----------
    private Process proceso;
    private Estado estado = Estado.EN_ESPERA;
    private LocalTime inicio;
    private LocalTime fin;
    private Long tiempoMs;
    private Integer codigoSalida;
    private boolean canceladoPorUsuario = false;

    // Se guardan bytes y se convierten a texto al pedirlos, para no romper caracteres como 'á'
    // si llegan partidos entre dos lecturas.
    private final ByteArrayOutputStream bufferStdout = new ByteArrayOutputStream();
    private final ByteArrayOutputStream bufferStderr = new ByteArrayOutputStream();
    private String errorAlLanzar;            // mensaje si start() lanzó IOException


    private Lanzador(int id, String tarea, String parametros, List<String> comando,
                     OrigenEntrada origenEntrada, String textoStdin) {
        this.id = id;
        this.tarea = tarea;
        this.parametros = parametros;
        this.comando = comando;
        this.origenEntrada = origenEntrada;
        this.textoStdin = textoStdin;
    }

    /**
     * Crea una ejecución de Text Analyzer.
     * @param ruta       solo se usa en FICHERO_ARGUMENTO
     * @param textoStdin solo se usa en DESDE_DATAFORGE
     */
    public static Lanzador textAnalyzer(int id, OrigenEntrada entrada, String ruta, String textoStdin, String palabra) {
        List<String> comando = comandoJava(TextAnalyzer.class);

        String parametros;
        if (entrada == OrigenEntrada.FICHERO_ARGUMENTO) {
            comando.add(ruta);                        // Modo fichero: TextAnalyzer <ruta> <palabra>
            parametros = new File(ruta).getName() + " | palabra: " + palabra;
        } else {
            parametros = "stdin | palabra: " + palabra;  // Modo stdin: TextAnalyzer <palabra>
        }
        comando.add(palabra);

        return new Lanzador(id, "Text Analyzer", parametros, comando, entrada, textoStdin);
    }

    // TODO: public static Lanzador primeBenchmark(int id, long n) { ... } cuando esté la clase PrimeBenchmark

    /**
     * Comando base para ejecutar una clase Java del proyecto como proceso independiente:
     *   <java de esta misma JVM> -cp target/classes -D...encoding=UTF-8 <clase>
     * - Se usa el java de la JVM actual (java.home) para que el hijo tenga la misma versión.
     * - Se fuerza UTF-8 en stdout/stderr del hijo para que las tildes lleguen bien a la GUI.
     */
    private static List<String> comandoJava(Class<?> clase) {
        List<String> comando = new ArrayList<>();
        comando.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        comando.add("-Dstdout.encoding=UTF-8");
        comando.add("-Dstderr.encoding=UTF-8");
        comando.add("-cp");
        comando.add(Path.of("target", "classes").toString());
        comando.add(clase.getName());
        return comando;
    }

    /** Guarda qué hacer con stdout y stderr (los ficheros solo se usan en modo FICHERO). */
    public void setRedirecciones(DestinoSalida stdout, File ficheroStdout, DestinoSalida stderr, File ficheroStderr) {
        this.destinoStdout = stdout;
        this.ficheroStdout = ficheroStdout;
        this.destinoStderr = stderr;
        this.ficheroStderr = ficheroStderr;
    }

    /**
     * Comprueba lo que hay que comprobar ANTES de lanzar.
     * El fichero de entrada NO se comprueba aquí: en modo fichero lo valida TextAnalyzer,
     * que escribe en stderr y devuelve un código distinto de 0 (prueba de error controlado del PDF).
     * @return null si todo está bien, o el mensaje de error
     */
    public String validar() {
        if (destinoStdout == DestinoSalida.FICHERO) {
            String e = comprobarFicheroSalida(ficheroStdout, "stdout");
            if (e != null) return e;
        }
        if (destinoStderr == DestinoSalida.FICHERO) {
            String e = comprobarFicheroSalida(ficheroStderr, "stderr");
            if (e != null) return e;
        }
        return null;
    }

    /**
     * Crea el ProcessBuilder, configura las redirecciones y lanza el proceso con start().
     * Si estamos en modo stdin, envía el texto por getOutputStream() y lo cierra (EOF).
     * @throws IOException si el proceso no se puede crear. La fila queda en estado ERROR.
     */
    public void iniciar() throws IOException {
        ProcessBuilder pb = new ProcessBuilder(comando);
        configurarRedirecciones(pb);

        inicio = ahora();
        try {
            proceso = pb.start();
        } catch (IOException e) {
            // El proceso no ha llegado a existir: no hay PID ni código de salida.
            errorAlLanzar = "No se pudo lanzar el proceso: " + e.getMessage();
            estado = Estado.ERROR;
            fin = inicio;
            tiempoMs = 0L;
            throw e;
        }
        estado = Estado.EJECUTANDO;

        // stdin: se escribe el texto y se cierra para que el hijo reciba EOF.
        // En modo fichero se cierra directamente (el hijo no lee stdin).
        try (OutputStream os = proceso.getOutputStream()) {
            if (origenEntrada == OrigenEntrada.DESDE_DATAFORGE && textoStdin != null) {
                os.write(textoStdin.getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            // El hijo puede haber terminado antes de leer (p. ej. error de argumentos): no es grave,
            // su código de salida y su stderr ya dirán qué ha pasado.
        }
    }

    private void configurarRedirecciones(ProcessBuilder pb) {
        // stdout
        switch (destinoStdout) {
            case CAPTURAR -> pb.redirectOutput(ProcessBuilder.Redirect.PIPE);
            case FICHERO  -> pb.redirectOutput(ficheroStdout);
            case DISCARD  -> pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        }
        // stderr
        switch (destinoStderr) {
            case CAPTURAR -> pb.redirectError(ProcessBuilder.Redirect.PIPE);
            case FICHERO  -> pb.redirectError(ficheroStderr);
            case DISCARD  -> pb.redirectError(ProcessBuilder.Redirect.DISCARD);
        }
        // stdin: en los dos modos es PIPE (en modo fichero se cierra nada más lanzar)
        pb.redirectInput(ProcessBuilder.Redirect.PIPE);
    }

    /**
     * Comprueba el proceso SIN bloquear. El controlador la llama periódicamente.
     *  - Lee lo que haya disponible en stdout/stderr (si se capturan).
     *  - Si el proceso ha terminado, lee lo que quede, guarda código, fin, duración y estado.
     * @return true justo en la llamada en la que se detecta que el proceso ha terminado
     */
    public boolean actualizar() {
        if (estado != Estado.EJECUTANDO) return false;

        leerDisponible(proceso.getInputStream(), bufferStdout);
        leerDisponible(proceso.getErrorStream(), bufferStderr);

        if (proceso.isAlive()) return false;

        // Ha terminado: leer lo que quede en las tuberías y registrar el final
        leerResto(proceso.getInputStream(), bufferStdout);
        leerResto(proceso.getErrorStream(), bufferStderr);

        codigoSalida = proceso.exitValue();   // no bloquea: ya sabemos que ha terminado
        fin = ahora();
        tiempoMs = Duration.between(inicio, fin).toMillis();

        if (canceladoPorUsuario)      estado = Estado.CANCELADO;
        else if (codigoSalida == 0)   estado = Estado.FINALIZADO;
        else                          estado = Estado.ERROR;
        return true;
    }

    /**
     * Detiene la ejecución con Process.destroy().
     * El cambio de estado a CANCELADO se registra en la siguiente llamada a actualizar(),
     * cuando el proceso ya ha terminado de verdad.
     * Si estaba EN_ESPERA (secuencial) se cancela sin llegar a lanzarse.
     */
    public void detener() {
        if (estado == Estado.EJECUTANDO) {
            canceladoPorUsuario = true;
            proceso.destroy();
        } else if (estado == Estado.EN_ESPERA) {
            estado = Estado.CANCELADO;
        }
    }

    /** Lee solo los bytes que ya están disponibles, así read() no se queda bloqueado. */
    private static void leerDisponible(InputStream is, ByteArrayOutputStream destino) {
        try {
            int disponibles = is.available();
            if (disponibles > 0) {
                byte[] datos = new byte[disponibles];
                int leidos = is.read(datos);
                if (leidos > 0) destino.write(datos, 0, leidos);
            }
        } catch (IOException e) {
            // Tubería cerrada: no hay más que leer
        }
    }

    /** Con el proceso ya terminado, lee hasta EOF lo que quede. */
    private static void leerResto(InputStream is, ByteArrayOutputStream destino) {
        try {
            destino.write(is.readAllBytes());
        } catch (IOException e) {
            // Tubería cerrada: no hay más que leer
        }
    }

    /**
     * Comprueba un fichero de salida (stdout / stderr) antes de lanzar.
     * Si no existe no es un error: redirectOutput lo crea. Sí hace falta que exista la carpeta.
     */
    static String comprobarFicheroSalida(File f, String canal) {
        if (f == null) return "No se ha elegido fichero para " + canal;

        File carpeta = f.getAbsoluteFile().getParentFile();
        if (carpeta == null || !carpeta.isDirectory())
            return "La carpeta de destino de " + canal + " no existe";

        if (f.exists()) {
            if (!f.isFile()) return "La ruta de " + canal + " no es un fichero: " + f.getAbsolutePath();
            if (!Files.isWritable(f.toPath())) return "No se puede escribir en " + f.getAbsolutePath();
        } else if (!Files.isWritable(carpeta.toPath())) {
            return "No se puede crear el fichero en " + carpeta.getAbsolutePath();
        }
        return null;
    }

    private static LocalTime ahora() {
        return LocalTime.now().truncatedTo(ChronoUnit.MILLIS);
    }

    // ---------------- Getters (los usa el MainController para la tabla y los paneles) ----------------
    public int getId() { return id; }
    /** null mientras el proceso no se ha lanzado (o si no se pudo lanzar). */
    public Long getPid() { return proceso != null ? proceso.pid() : null; }
    public String getTarea() { return tarea; }
    public String getParametros() { return parametros; }
    public LocalTime getInicio() { return inicio; }
    public LocalTime getFin() { return fin; }
    public Long getTiempoMs() { return tiempoMs; }
    public Integer getCodigoSalida() { return codigoSalida; }
    public Estado getEstado() { return estado; }
    public boolean estaTerminado() {
        return estado == Estado.FINALIZADO || estado == Estado.ERROR || estado == Estado.CANCELADO;
    }

    public DestinoSalida getDestinoStdout() { return destinoStdout; }
    public DestinoSalida getDestinoStderr() { return destinoStderr; }
    public File getFicheroStdout() { return ficheroStdout; }
    public File getFicheroStderr() { return ficheroStderr; }

    /** Texto capturado de stdout (solo tiene contenido si stdout es CAPTURAR). */
    public String getStdout() { return bufferStdout.toString(StandardCharsets.UTF_8); }
    /** Texto capturado de stderr (solo tiene contenido si stderr es CAPTURAR). */
    public String getStderr() { return bufferStderr.toString(StandardCharsets.UTF_8); }
    /** Mensaje si start() falló (el proceso no llegó a existir); null en otro caso. */
    public String getErrorAlLanzar() { return errorAlLanzar; }
}
