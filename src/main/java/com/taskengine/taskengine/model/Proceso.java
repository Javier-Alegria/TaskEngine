package com.taskengine.taskengine.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

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
    private final ObjectProperty<LocalTime> fin = new SimpleObjectProperty<>();
    private final ObjectProperty<Long> tiempoMs = new SimpleObjectProperty<>();
    private final ObjectProperty<Integer> codigoSalida = new SimpleObjectProperty<>();
    private final ObjectProperty<Estado> estado = new SimpleObjectProperty<>(Estado.EJECUTANDO);

    public Proceso(int id, long pid, String tarea, String parametros) {
        this.id = id;
        this.pid = pid;
        this.tarea = tarea;
        this.parametros = parametros;
    }

    /** Marca el proceso como terminado. La fila se conserva, solo cambian sus datos. */
    public void finalizar(int codigo) {
        LocalTime ahora = LocalTime.now().truncatedTo(ChronoUnit.MILLIS);
        fin.set(ahora);
        tiempoMs.set(Duration.between(inicio, ahora).toMillis());
        codigoSalida.set(codigo);
        estado.set(codigo == 0 ? Estado.FINALIZADO : Estado.ERROR);
    }

    public int getId() { return id; }
    public long getPid() { return pid; }
    public String getTarea() { return tarea; }
    public String getParametros() { return parametros; }
    public LocalTime getInicio() { return inicio; }

    public ObjectProperty<LocalTime> finProperty() { return fin; }
    public ObjectProperty<Long> tiempoMsProperty() { return tiempoMs; }
    public ObjectProperty<Integer> codigoSalidaProperty() { return codigoSalida; }
    public ObjectProperty<Estado> estadoProperty() { return estado; }
}
