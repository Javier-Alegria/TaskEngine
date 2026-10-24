package com.taskengine.taskengine.textanalyzer;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class Lanzador {
    enum DestinoSalida { CAPTURAR, FICHERO, DISCARD }
    enum OrigenEntrada { DESDE_DATAFORGE, DESDE_FICHERO }

    static void main(String[] args) {

        List<String> comando = new ArrayList<>();
        comando.add("java");
        comando.add("-cp");
        comando.add("target\\classes");
        comando.add("com.taskengine.taskengine.textanalyzer.TextAnalyzer");
        comando.add("src\\datos\\fichero.txt");
        comando.add("tal");

        System.out.println("PETA AQUI QUIZAS?¿");
        ProcessBuilder pb = new ProcessBuilder(comando);

        /*
        ----------------------
                PRUEBAS
        -----------------------
         */
        File ficheroSalida = new File("src\\datos\\fichero12.txt");
        File ficheroError = new File("src\\datos\\ficheroEError.txt");
        File ficheroEntrada = new File("src\\datos\\fichero.txt");


        //Los parametros de este metodo vendran desde GUI
        configurarRedirecciones(pb, DestinoSalida.CAPTURAR, null, DestinoSalida.CAPTURAR, null, OrigenEntrada.DESDE_FICHERO, ficheroEntrada );

        /*
        ----------------------
                PRUEBAS
        -----------------------
         */
        try {
            int retorno;
            System.out.println("LLEGA HASTA AQUI??");
            Process p = pb.start();
            System.out.println("LLEGA HASTA AQUI??");

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
            case DESDE_FICHERO   -> pb.redirectInput(ficheroStdin);
        }
    }


    /**
     * Metodo para validad los ficheros que se pasan en modo fichero y solo en modo fichero
     * Comprueba el fichero donde se escribira la salida, los errores y el de entrada
     * @return
     */
    private static String validarFicheroEntrada(File fOut, File fErr )
    {
        String e = comprobarFicheroSalida(fOut, "stdout");
        if (e != null) return e;

        e = comprobarFicheroSalida(fErr, "stderr");
        if (e != null) return e;

        return null;
    }

    /**
     * Metodo que usa validarAntesDeLanzar para comprobar ficheros
     * @param f
     * @param canal
     * @return
     */
    private static String comprobarFicheroSalida(File f, String canal) {
        if (f == null) return "No se ha elegido fichero para " + canal;
        if (f.exists() && !f.canWrite()) return "No se puede escribir en " + f.getAbsolutePath();
        return null;
    }


}
