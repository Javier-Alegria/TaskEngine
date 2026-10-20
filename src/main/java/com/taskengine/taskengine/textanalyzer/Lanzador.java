package com.taskengine.taskengine.textanalyzer;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Lanzador {
    enum DestinoSalida { CAPTURAR, FICHERO, DISCARD }
    enum OrigenEntrada { DESDE_DATAFORGE, DESDE_FICHERO }

    static void main(String[] args) {
        boolean modoFichero = true;

        List<String> comando = new ArrayList<String>();
        comando.add("java");
        comando.add("-cp");
        comando.add("rutaClases");
        comando.add("TextAnalyzer");


        if (modoFichero ){
            comando.add("RutaFichero");
        }
        comando.add("Palabra");

        ProcessBuilder pb = new ProcessBuilder(comando);
        //configurarRedirecciones(pb, destinoOut, fOut, destinoErr, fErr, origenIn, fIn);
        try {
            Process p = pb.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Metodo que se usara para configurar el comportamiento de las redirecciones del ProcessBuilder segun el usuario eliga desde la GUI
     * @param pb
     * @param stdout
     * @param ficheroStdout
     * @param stderr
     * @param ficheroStderr
     * @param stdin
     * @param ficheroStdin
     */
    void configurarRedirecciones(ProcessBuilder pb, DestinoSalida stdout, File ficheroStdout, DestinoSalida stderr, File ficheroStderr, OrigenEntrada stdin, File ficheroStdin){
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
