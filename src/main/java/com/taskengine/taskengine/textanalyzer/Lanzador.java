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

        List<String> comando = new ArrayList<String>();
        comando.add("java");
        comando.add("-cp");
        comando.add("target\\classes");
        comando.add("com.taskengine.taskengine.textanalyzer.TextAnalyzer");

        comando.add("tal");

        System.out.println("PETA AQUI QUIZAS?¿");
        ProcessBuilder pb = new ProcessBuilder(comando);

        File ficheroSalida = new File("src\\datos\\fichero1.txt");
        File ficheroError = new File("src\\datos\\ficheroError.txt");
        File ficheroEntrada = new File("src\\datos\\fichero2.txt");

        configurarRedirecciones(pb, DestinoSalida.FICHERO, ficheroSalida, DestinoSalida.FICHERO, ficheroError, OrigenEntrada.DESDE_FICHERO, ficheroEntrada );

        /*
          ================= !!!! PRUEBAS !!!!! =================
         */
        try {
            int retorno;
            System.out.println("LLEGA HASTA AQUI??");
            Process p = pb.start();


            BufferedReader bf = new BufferedReader(new InputStreamReader(p.getInputStream() ) );

            String linea;
            String resultado = "";
            while ((linea = bf.readLine()) != null){
                resultado += linea;
            }

            retorno = p.waitFor();
            System.out.println(retorno);
            System.out.println(resultado);

        } catch (IOException e) {
            System.out.println(e.getMessage());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

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

}
