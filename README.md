# TaskEngine ⚙️ — Multi-Process Orchestration & Analytics

**TaskEngine** es una aplicación Java con interfaz gráfica desarrollada para la gestión, ejecución y monitorización visual de procesos del sistema operativo. Permite orquestar tareas concurrentes, gestionar flujos de entrada y salida (`stdin`, `stdout`, `stderr`), configurar pipelines entre procesos y analizar comparativas de rendimiento en ejecuciones secuenciales y paralelas.

---

## 💡 Características Principales

- **Gestión de Procesos Externos:** Creación y control de procesos mediante `ProcessBuilder` y `Process`.
- **Monitor de Procesos en Tiempo Real:** Seguimiento detallado en tabla con identificador, PID, modo de ejecución, tiempo de inicio/fin, duración, código de salida y estado.
- **Redirección Completa de Flujos:**
  - **`stdout` / `stderr`:** Captura en la propia aplicación, exportación directa a fichero de texto o descarte (`DISCARD`).
  - **`stdin`:** Inyección de datos desde la interfaz de la aplicación o lectura desde fichero.
- **Pipeline de Procesos (`Pipe`):** Encadenamiento directo de salidas y entradas utilizando `ProcessBuilder.startPipeline()` (ej. *Prime Benchmark* → *Text Analyzer*).
- **Ejecución Secuencial y Paralela:** Lanzamiento masivo de instancias sin uso de hilos (exclusivamente con procesos externos) para medir límites de concurrencia del sistema.
- **Detención Controlada:** Cancelación activa de procesos en ejecución mediante `Process.destroy()`.

---

## 🛠️ Programas Externos Incluidos

TaskEngine incluye programas independientes en Java que son lanzados y controlados como procesos independientes:

1. **Text Analyzer:**
   - **Modo Fichero / Stdin:** Analiza textos recibidos como parámetro o mediante `stdin`.
   - **Funciones:** Conteo de líneas, palabras, caracteres, detección de la palabra más larga y frecuencia de una palabra específica.
2. **Prime Benchmark:**
   - Cálculo intensivo de números primos en un rango $[2, N]$.
   - Utilizado para pruebas de carga y comparativas entre ejecución secuencial y paralela.
3. **Pipeline (Benchmark → Analyzer):**
   - Conexión vía *Pipe* donde la salida estándar de *Prime Benchmark* se transmite como entrada estándar a *Text Analyzer*.

---

## 📊 Experimento de Rendimiento

La aplicación está diseñada para medir la escalabilidad de la CPU comparando tiempos de respuesta entre ejecución **Secuencial** y **Paralela** con batches de 1, 5, 10, 20, 50 y 100 procesos.

---

## 👥 Integrantes del Grupo

- **Javier Alegría** - *Desarrollador*
- **Israel González** - *Desarrollador*
- **Alberto Madrid** - *Desarrollador*

*Proyecto para la asignatura de Programación de Servicios y Procesos (PSP - RA1) | 2.º DAM*
