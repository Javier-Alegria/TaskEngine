package com.taskengine.taskengine.textanalyzer;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class Lanzador {
    enum DestinoSalida { CAPTURAR, FICHERO, DISCARD }
    enum OrigenEntrada { DESDE_DATAFORGE, FICHERO_ARGUMENTO}

    static void main(String[] args) {

        /*-----------------------
                PRUEBAS
        -----------------------
         */

        //Ejemplo que representa lo que vendra desde GUI
        OrigenEntrada oe = OrigenEntrada.FICHERO_ARGUMENTO; //Ejemplo que representa lo que vendra desde GUI

        DestinoSalida ds = DestinoSalida.FICHERO;

        File ficheroSalida = new File("src\\datos\\fichero1.txt");     // Solo se usa en Modo DestinoSalida Fichero
        File ficheroError = new File("src\\datos\\ficheroError.txt");  // Solo se usa en Modo DestinoSalida Fichero
        File ficheroEntrada = new File("src\\datos\\fichero.txt");

        List<String> comando = new ArrayList<>();
        comando.add("java");
        comando.add("-cp");
        comando.add("target\\classes");
        comando.add("com.taskengine.taskengine.textanalyzer.TextAnalyzer");

        if (oe == OrigenEntrada.DESDE_DATAFORGE){
            comando.add("src\\datos\\fichero.txt");
        }

        comando.add("tal");

        //Validar fichero de entrada en MODO 'STDIN_FICHERO'
        String validacionEntrada = "";
        if (oe == OrigenEntrada.FICHERO_ARGUMENTO) {
            validacionEntrada = comprobarFicheroEntrada(ficheroEntrada);
        }

        //Validar ficheros de salida en MODO DestinoSalida FICHERO
        String validacionSalida = "";
        if (ds == DestinoSalida.FICHERO) {
            validacionSalida = validarFicherosSalida(ficheroSalida, ficheroError);
        }


        ProcessBuilder pb = new ProcessBuilder(comando);
        //Los parametros de este metodo vendran desde GUI
        configurarRedirecciones(pb, DestinoSalida.CAPTURAR, null, DestinoSalida.CAPTURAR, null, oe, ficheroEntrada );


        try {
            int retorno;
            System.out.println(validacionEntrada);
            System.out.println(validacionSalida);

            Process p = pb.start();
            p.getOutputStream().close();

            BufferedReader bf = new BufferedReader(new InputStreamReader(p.getInputStream() ) );

            String linea;
            String err = "";
            String resultado = "";
            while ((linea = bf.readLine()) != null){
                resultado += linea;
            }

            bf = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            while ((linea = bf.readLine()) != null){
                err += linea;
            }
            bf.close();

            retorno = p.waitFor();
            System.out.println(retorno);
            System.out.println(resultado);
            System.out.println(err);

        } catch (IOException e) {
            System.out.println(e.getMessage());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        /*
        ----------------------
           PRUEBAS hasta aqui
        -----------------------
         */

    }


    /**
     * Metodo que se usara para configurar el comportamiento de las redirecciones del ProcessBuilder segun el usuario eliga desde la GUI
     * @param pb EL PROCESO QUE SE CONFIGURARAN SUS REDIRECCIONES
     * @param stdout
     * @param ficheroStdout
     * @param stderr
     * @param ficheroStderr
     * @param stdin
     * @param ficheroStdin
     */
    static void configurarRedirecciones(ProcessBuilder pb, DestinoSalida stdout, File ficheroStdout, DestinoSalida stderr, File ficheroStderr, OrigenEntrada stdin, File ficheroStdin){
        // stdout
        switch (stdout) {
            case CAPTURAR -> pb.redirectOutput(ProcessBuilder.Redirect.PIPE);
            case FICHERO  -> pb.redirectOutput(ficheroStdout);
            case DISCARD  -> pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        }

        // stderr
        switch (stderr) {
            case CAPTURAR -> pb.redirectError(ProcessBuilder.Redirect.PIPE);
            case FICHERO  -> pb.redirectError(ficheroStderr);
            case DISCARD  -> pb.redirectError(ProcessBuilder.Redirect.DISCARD);
        }

        // stdin
        switch (stdin) {
            case DESDE_DATAFORGE -> pb.redirectInput(ProcessBuilder.Redirect.PIPE);
            case FICHERO_ARGUMENTO   -> pb.redirectInput(ficheroStdin);
        }
    }




    /**
     * Comprueba que el fichero elegido como origen de stdin (modo STDIN_FICHERO)
     * existe y se puede leer, ANTES de lanzar el proceso.
     * Si no se valida, pb.start() lanzaria IOException al intentar abrirlo
     * @param f fichero seleccionado en la GUI
     * @return null si es válido, o un mensaje con el error
     */
    private static String comprobarFicheroEntrada(File f) {
        if (f == null) return "No se ha elegido fichero para stdin";
        if (!f.exists()) return "El fichero no existe: " + f;
        if (!f.isFile()) return "La ruta no es un fichero: " + f.getAbsolutePath();
        if (!f.canRead()) return "No se puede leer el fichero: " + f.getAbsolutePath();

        return null;
    }

    /**
     * Metodo para validad los ficheros que se pasan en modo fichero y solo en modo fichero
     * Comprueba el fichero donde se escribira la salida, los errores y el de entrada
     * @return
     */
    private static String validarFicherosSalida(File fOut, File fErr)
    {
        String e = comprobarFicheroSalida(fOut, "stdout");
        if (e != null) return e;

        e = comprobarFicheroSalida(fErr, "stderr");
        return e;
    }

    /**
     *
     * @param f
     * @param canal
     * @return
     */
    static String comprobarFicheroSalida(File f, String canal) {
        if (f == null) return "No se ha elegido fichero para " + canal;

        File carpeta = f.getAbsoluteFile().getParentFile();

        if (carpeta == null || !carpeta.isDirectory()) return "La carpeta de destino de " + canal + " no existe";
        if (!f.canWrite()) return "No se puede escribir en " + f.getAbsolutePath();

        return null;
    }
}
