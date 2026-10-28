package com.taskengine.taskengine.primebenchmark;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class PrimeBenchmark {

    // Codigos de Salida
    static final int OK = 0;
    static final int ERROR_ARGUMENTOS = 1;
    static final int ERROR_NUMERO = 2;
    static final int ERROR_FICHERO = 3;
    static final int ERROR_LECTURA = 4;

    static void main(String[] args) {
        System.exit(ejecutar(args));
    }

    /**
     * Recibe el argumento N, lo valida, calcula cuántos primos hay entre 2 y N y escribe el resultado por stdout.
     * Si algo falla, escribe el motivo por stderr y devuelve un código distinto de 0.
     * @param args N
     * @return código de salida
     */
    public static int ejecutar(String[] args) {
        String texto;
        // TIPOS DE ENTRADA: (N como argumento | N en un fichero)
        if (args.length == 1) { // ----> N como argumento
            texto = args[0];
        } else if (args.length == 2 && args[0].equals("-f")) { // ----> N en un fichero
            try (BufferedReader lector = new BufferedReader(new FileReader(args[1], StandardCharsets.UTF_8))) {
                String linea = lector.readLine();
                texto = (linea == null) ? "" : linea;
            } catch (FileNotFoundException e) {
                System.err.println("No se pudo abrir el fichero: " + args[1]);
                return ERROR_FICHERO;
            } catch (IOException e) {
                System.err.println("Error de lectura: " + e.getMessage());
                return ERROR_LECTURA;
            }
        } else {
            System.err.println("Uso: PrimeBenchmark <N>             (N como argumento)");
            System.err.println("     PrimeBenchmark -f <ruta>       (N en la primera línea del fichero)");
            return ERROR_ARGUMENTOS;
        }

        int n;
        try {
            n = Integer.parseInt(texto.trim());
        } catch (NumberFormatException e) {
            System.err.println("'" + texto + "' no es un entero válido (entre 2 y 2147483647)");
            return ERROR_NUMERO;
        }

        // Comprueba si n es menor a 2
        if (n < 2) {
            System.err.println("Error: N debe ser mayor o igual que 2");
            return ERROR_NUMERO;
        }

        // Cuenta los números primos entere 2 y n
        int totalPrimos = contarPrimos(n);

        // Imprime la cantidad de primos
        System.out.println("cantidad de primos: " + totalPrimos);
        return OK;
    }

    /**
     * Cuenta la cantidad de primos que hay desde 2 hasta N
     * @param n numero límite a comprobar primos
     * @return número de primos entre 2 y N
     */
    static int contarPrimos(int n) {
        int totalPrimos = 0;
        for (int i = 2; i <= n; i++) {
            if (esPrimo(i))
                totalPrimos++;
        }
        return totalPrimos;
    }

    /**
     * Comprueba si un número en específico es primo
     * @param n Número a comprobar
     * @return True si es primo | False si no es primo
     */
    static boolean esPrimo(int n) {
        boolean esPrimo = true;
        for (int i = 2; i < n; i++) {
            if (n % i == 0)
                esPrimo = false;
        }
        return esPrimo;
    }
}
