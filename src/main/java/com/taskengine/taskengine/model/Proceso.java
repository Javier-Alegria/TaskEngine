package com.taskengine.taskengine.model;

import javafx.beans.value.ObservableValue;

import java.time.Duration;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

public class Proceso {
    public enum Estado {
        EJECUTANDO("Ejecutando"),
        FINALIZADO("Finalizado"),
        ERROR("Error");

        private final String texto;

        Estado(String texto) {
            this.texto = texto;
        }

        @Override
        public String toString() {
            return texto;
        }
    }

    // Datos fijos: se conocen al lanzar el proceso
    private final int id;
    private final long pid;
    private final String tarea;
    private final String parametros;
    private final LocalTime inicio = LocalTime.now().truncatedTo(ChronoUnit.MILLIS);

    // Datos que cambian cuando el proceso termina
    private LocalTime fin;
    private Long tiempoMs;
    private Integer codigoSalida;
    private Estado estado = Estado.EJECUTANDO;

    private String stdout = "";

    public Proceso(int id, long pid, String tarea, String parametros) {
        this.id = id;
        this.pid = pid;
        this.tarea = tarea;
        this.parametros = parametros;
    }

    /** Marca el proceso como terminado. La fila se conserva, solo cambian sus datos. */
    public void finalizar(int codigo) {
        LocalTime ahora = LocalTime.now().truncatedTo(ChronoUnit.MILLIS);
        fin = ahora;
        tiempoMs = Duration.between(inicio, ahora).toMillis();
        codigoSalida = codigo;
        estado = codigo == 0 ? Estado.FINALIZADO : Estado.ERROR;
    }

    public int getId() { return id; }
    public long getPid() { return pid; }
    public String getTarea() { return tarea; }
    public String getParametros() { return parametros; }
    public LocalTime getInicio() { return inicio; }

    public LocalTime getFin() { return fin; }
    public Long getTiempoMs() { return tiempoMs; }
    public Integer getCodigoSalida() { return codigoSalida; }
    public Estado getEstado() { return estado; }

    public String getStdout() { return stdout; }

    public void setStdout(String texto) { stdout = texto; }

    public void agregarStdout(String linea) { stdout += linea + "\n"; }
}
