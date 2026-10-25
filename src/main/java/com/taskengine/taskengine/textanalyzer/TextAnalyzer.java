package com.taskengine.taskengine.textanalyzer;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class TextAnalyzer {

    // Codigos de Salida
    static final int OK = 0;
    static final int ERROR_ARGUMENTOS= 1;
    static final int ERROR_FICHERO = 2;
    static final int ERROR_LECTURA = 3;


    static void main(String[] args){
        System.exit(ejecutar(args));
    }

    /**
     * Metodo que recibe los parametros, los valida y llamara al metodo que tratara el texto segun esos parametros
     * @param args
     * @return
     */
    public static int ejecutar(String[] args){
        String palabra;
        InputStream entrada;

        if (args.length == 2){                                                         // ----> Modo Fichero
            File file = new File(args[0]);
            palabra = args[1];

            try {
                entrada = new FileInputStream(file);
            } catch (FileNotFoundException e) {
                System.err.println("No se pudo abrir el fichero: " + args[0]);
                return ERROR_FICHERO;
            }
        } else if (args.length == 1){                                                  // -----> Modo stdin
            palabra = args[0];
            entrada = System.in;

        } else {
            System.err.println("Uso: TextAnalyzer <ruta> <palabra>   (modo fichero)");
            System.err.println("     TextAnalyzer <palabra>          (modo stdin)");
            return ERROR_ARGUMENTOS;
        }

        // Try with resources para el BufferedReader que usará el metodo que tratara los datos de entrada
        try (BufferedReader lector = new BufferedReader ( new InputStreamReader(entrada, StandardCharsets.UTF_8))){
            analizarTexto(lector, palabra);
            return OK;
        } catch (IOException e) {
            System.err.println("Error de lectura: " + e.getMessage());
            return ERROR_LECTURA;
        }
    }


    /**
     * Metodo que se encarga de realizar el tratamiento del texto contando lineas, palabras, buscando la palabra,etc
     * Imprime en consola de momento para pruebas
     * @param lector
     * @param palabra
     * @throws IOException
     */
    private static void analizarTexto(BufferedReader lector, String palabra) throws IOException{
        int lineas = 0,palabras = 0, caracteres = 0, apariciones = 0;
        String masLarga = "";
        String linea;

        while ((linea = lector.readLine()) != null){
            lineas ++;
            caracteres += linea.length();

            String contenido = linea.trim();

            if (!contenido.isEmpty() ){
                for (String trozo : linea.split("\\s+") ){           // ----> parte la linea en varios trozos usando como criterio 1 o mas espacios en blanco
                    palabras ++;

                    String limpia = trozo.replaceAll("[^\\p{L}\\p{N}]", ""); // -----> Eliminar cualquier caracter que no alfanumerici o numero de array (el ^ equivale a !)

                    if (limpia.length() > masLarga.length()) masLarga = limpia;
                    if (limpia.equalsIgnoreCase(palabra)) apariciones ++;
                }
            }
        }

        System.out.println("--- Resultado del análisis ---");
        System.out.println("Líneas: " + lineas);
        System.out.println("Palabras: " + palabras);
        System.out.println("Caracteres: " + caracteres);
        System.out.println("Palabra más larga: " + masLarga);
        System.out.println("Apariciones de '" + palabra + "': " + apariciones);
    }
}