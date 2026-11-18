package powerband.util;

public final class IConstants {
    private IConstants() {
    }

    public static final long SERIAL_VERSION_UID = 1L;

    public static final String HOST_SUPERCOLLIDER = "127.0.0.1";
    public static final int PUERTO_SUPERCOLLIDER = 57110;
    public static final int PUERTO_MINIMO = 1;
    public static final int PUERTO_MAXIMO = 65535;

    public static final String OSC_S_NEW = "/s_new";
    public static final String OSC_N_SET = "/n_set";
    public static final String OSC_TAGS_PREFIX = ",";
    public static final char OSC_TAG_ENTERO = 'i';
    public static final char OSC_TAG_FLOAT = 'f';
    public static final char OSC_TAG_STRING = 's';
    public static final int OSC_BYTES_POR_ENTERO = Integer.BYTES;
    public static final int CANTIDAD_ARGUMENTOS_S_NEW_BASE = 4;
    public static final int CANTIDAD_ARGUMENTOS_N_SET_LIBERACION = 3;
    public static final int CANTIDAD_ARGUMENTOS_N_SET_ACTUALIZACION_AMPLITUDES = 7;
    public static final int RADIX_HEXADECIMAL = 16;
    public static final int OSC_PRIMER_ID_NODO = 10000;
    public static final int OSC_ADD_TO_HEAD = 0;
    public static final int OSC_GRUPO_DEFAULT = 1;
    public static final int BYTE_NULO = 0;
    public static final int VALOR_GATE_APAGADO = 0;

    public static final String SYNTH_PIANO = "classicPiano";
    public static final String SYNTH_BAJO = "stringBass";
    public static final String SYNTH_ORGANO = "organ70s";

    public static final String CONTROL_NOTA = "note";
    public static final String CONTROL_DURACION_MS = "durMs";
    public static final String CONTROL_TERCERA = "third";
    public static final String CONTROL_FRECUENCIA = "freq";
    public static final String CONTROL_GATE = "gate";
    public static final String CONTROL_RATIO_1 = "r1";
    public static final String CONTROL_RATIO_2 = "r2";
    public static final String CONTROL_RATIO_3 = "r3";
    public static final String CONTROL_AMPLITUD_1 = "a1";
    public static final String CONTROL_AMPLITUD_2 = "a2";
    public static final String CONTROL_AMPLITUD_3 = "a3";

    public static final String ARGUMENTO_MODO_DEMO = "--demo";
    public static final String ARGUMENTO_MODO_INSTRUMENTOS = "--instrumentos";
    public static final String PREFIJO_ARGUMENTO_MODO = "--";
    public static final String ARGUMENTO_MODO_DESCONOCIDO = "--desconocido";
    public static final String NOMBRE_HILO_CONTROL = "LectorControl";
    public static final String NOMBRE_HILO_MICROFONO = "LectorMicrofono";
    public static final String NOMBRE_HILO_CIERRE_INSTRUMENTOS = "CierreInstrumentos";
    public static final int INDICE_ARGUMENTO_INICIAL = 0;
    public static final int INDICE_ARGUMENTO_PUERTO = 1;
    public static final int CANTIDAD_ARGUMENTOS_NINGUNO = 0;
    public static final int CANTIDAD_ARGUMENTOS_HOST = 1;
    public static final int CANTIDAD_ARGUMENTOS_DESTINO_OSC = 2;
    public static final int CANTIDAD_EVENTOS_CIERRE_INSTRUMENTOS = 1;
    public static final int CANTIDAD_ACCIONES_POR_TRANSICION_CONTROL = 2;
    public static final int CANTIDAD_LLAMADAS_PRESIONES_SIMULTANEAS = 4;
    public static final int CANTIDAD_INTENTOS_INICIALIZACION = 1;
    public static final int INDICE_CONTROL_PRIMARIO = 0;
    public static final int INDICE_ELEMENTO_INICIAL = 0;
    public static final int DESPLAZAMIENTO_BYTE_ALTO_PCM = 1;
    public static final int CANTIDAD_BYTES_VACIA = 0;
    public static final int RESULTADO_LECTURA_FIN = -1;
    public static final int SIGNO_PCM_SILENCIO = 0;
    public static final float CONTROL_RANGO_EJE = 1.0f;
    public static final float CONTROL_EJE_NEUTRO = 0.0f;
    public static final double VALOR_RMS_SILENCIO = 0.0;
    public static final double FRECUENCIA_MINIMA_VALIDA_HZ = 0.0;
    public static final boolean PCM_FIRMADO = true;
    public static final boolean PCM_BIG_ENDIAN = false;
    public static final int TIMEOUT_PRUEBA_MS = 5000;
    public static final float TOLERANCIA_FLOAT_PRUEBA = 0.0001f;
    public static final String HOST_PRUEBA = "localhost";
    public static final int PUERTO_PRUEBA = 57111;
    public static final String PUERTO_PRUEBA_TEXTO = "57111";
    public static final String PUERTO_INVALIDO_TEXTO = "puerto";

    public static final int TERCERA_MAYOR_SEMITONOS = 4;
    public static final int TERCERA_MENOR_SEMITONOS = 3;
    public static final int CANTIDAD_PARCIALES = 3;
    public static final int NOTA_CONTROL_CRUZ = 60;
    public static final int NOTA_CONTROL_CIRCULO = 62;
    public static final int NOTA_CONTROL_CUADRADO = 64;
    public static final int NOTA_CONTROL_TRIANGULO = 65;
    public static final int NOTA_CONTROL_L1 = 67;
    public static final int NOTA_CONTROL_R1 = 69;
    public static final int NOTA_CONTROL_ARRIBA = 71;
    public static final int NOTA_CONTROL_DERECHA = 72;
    public static final int NOTA_CONTROL_ABAJO = 74;
    public static final int NOTA_CONTROL_IZQUIERDA = 76;
    public static final float CONTROL_ZONA_MUERTA = 0.10f;
    public static final float CONTROL_PASO_AMPLITUD = 0.01f;
    public static final long CONTROL_INTERVALO_LECTURA_MS = 20L;
    public static final int FRECUENCIA_MUESTREO_MICROFONO_HZ = 44100;
    public static final int BITS_MUESTRA_MICROFONO = Short.SIZE;
    public static final int CANALES_MICROFONO = 1;
    public static final int BYTES_MUESTRA_MICROFONO = Short.BYTES;
    public static final int MUESTRAS_BLOQUE_MICROFONO = 4096;
    public static final double DIVISOR_NORMALIZACION_PCM = 32768.0;
    public static final double CRUCES_POR_CICLO = 2.0;
    public static final double BASE_LOGARITMO_DOS = 2.0;
    public static final double FRACCION_MIDI = 0.5;
    public static final int CANTIDAD_MUESTRAS_MINIMAS_TONO = 2;
    public static final int CANTIDAD_CRUCES_MINIMOS_TONO = 2;
    public static final double FACTOR_FASE_COMPLETA = 2.0;
    public static final int INDICE_BYTE_ALTO_PCM = 1;
    public static final double TOLERANCIA_EMPATE_NOTA_MIDI = 0.0000000001;
    public static final int PCM_BYTE_MUESTRA_POSITIVA_BAJA = 0;
    public static final int PCM_BYTE_MUESTRA_POSITIVA_ALTA = 64;
    public static final int PCM_BYTE_MUESTRA_NEGATIVA_BAJA = 0;
    public static final int PCM_BYTE_MUESTRA_NEGATIVA_ALTA = 192;
    public static final short MUESTRA_PCM_PRUEBA_POSITIVA = 16384;
    public static final short MUESTRA_PCM_PRUEBA_NEGATIVA = -16384;
    public static final double RMS_PCM_PRUEBA = 0.5;
    public static final double FRECUENCIA_INVALIDA_BAJA_HZ = 1.0;
    public static final double FRECUENCIA_INVALIDA_ALTA_HZ = 10000.0;
    public static final double TOLERANCIA_FRECUENCIA_PRUEBA_HZ = 15.0;
    public static final double AMPLITUD_SENAL_PRUEBA = 0.5;
    public static final float UMBRAL_RMS_MICROFONO = 0.10f;
    public static final long ENFRIAMIENTO_MICROFONO_MS = 150L;
    public static final int DURACION_DISPARO_BAJO_MS = 450;
    public static final int NOTA_BAJO_ESCALA_01 = 36;
    public static final int NOTA_BAJO_ESCALA_02 = 38;
    public static final int NOTA_BAJO_ESCALA_03 = 40;
    public static final int NOTA_BAJO_ESCALA_04 = 41;
    public static final int NOTA_BAJO_ESCALA_05 = 43;
    public static final int NOTA_BAJO_ESCALA_06 = 45;
    public static final int NOTA_BAJO_ESCALA_07 = 47;
    public static final int NOTA_BAJO_ESCALA_08 = 48;
    public static final int NOTA_BAJO_ESCALA_09 = 50;
    public static final int NOTA_BAJO_ESCALA_10 = 52;
    public static final int NOTA_BAJO_ESCALA_11 = 53;
    public static final int NOTA_BAJO_ESCALA_12 = 55;
    public static final int NOTA_BAJO_ESCALA_13 = 57;
    public static final int NOTA_BAJO_ESCALA_14 = 59;
    public static final int NOTA_BAJO_ESCALA_15 = 60;
    public static final int CANTIDAD_NOTAS_ESCALA_BAJO = 15;
    public static final long MILISEGUNDOS_POR_SEGUNDO = 1000L;
    public static final int BYTES_FRAME_MICROFONO = BYTES_MUESTRA_MICROFONO * CANALES_MICROFONO;
    public static final float AMPLITUD_MINIMA_ORGANO = 0.0f;
    public static final float AMPLITUD_MAXIMA_ORGANO = 1.0f;
    public static final double FRECUENCIA_LA4_HZ = 440.0;
    public static final double NOTA_MIDI_LA4 = 69.0;
    public static final double SEMITONOS_POR_OCTAVA = 12.0;
    public static final long NANOSEGUNDOS_POR_MILISEGUNDO = 1_000_000L;
    public static final double FRECUENCIA_NOTA_BAJO_ESCALA_15_HZ =
        FRECUENCIA_LA4_HZ * Math.pow(
            BASE_LOGARITMO_DOS,
            (NOTA_BAJO_ESCALA_15 - NOTA_MIDI_LA4) / SEMITONOS_POR_OCTAVA
        );

    public static final float RATIO_DEFAULT_1 = 1.0f;
    public static final float RATIO_DEFAULT_2 = 2.0f;
    public static final float RATIO_DEFAULT_3 = 3.0f;
    public static final float AMPLITUD_DEFAULT_1 = 1.0f;
    public static final float AMPLITUD_DEFAULT_2 = 0.5f;
    public static final float AMPLITUD_DEFAULT_3 = 0.25f;
    public static final float RATIO_PRUEBA_1 = 1.0f;
    public static final float RATIO_PRUEBA_2 = 2.01f;
    public static final float RATIO_PRUEBA_3 = 4.0f;
    public static final float RATIO_PRUEBA_4 = 3.0f;
    public static final float RATIO_PRUEBA_5 = 5.0f;
    public static final float RATIO_PRUEBA_6 = 2.0f;
    public static final float AMPLITUD_PRUEBA_1 = 1.0f;
    public static final float AMPLITUD_PRUEBA_2 = 0.6f;
    public static final float AMPLITUD_PRUEBA_3 = 0.3f;
    public static final float AMPLITUD_PRUEBA_4 = 0.4f;
    public static final float AMPLITUD_PRUEBA_5 = 0.2f;

    public static final int NOTA_PIANO_PRUEBA_1 = 48;
    public static final int NOTA_PIANO_PRUEBA_2 = 57;
    public static final int NOTA_PIANO_PRUEBA_3 = 53;
    public static final int DURACION_PIANO_PRUEBA_1_MS = 1500;
    public static final int DURACION_PIANO_PRUEBA_2_MS = 1500;
    public static final int DURACION_PIANO_PRUEBA_3_MS = 2000;
    public static final long ESPERA_PIANO_PRUEBA_1_MS = 1700L;
    public static final long ESPERA_PIANO_PRUEBA_2_MS = 1700L;
    public static final long ESPERA_PIANO_PRUEBA_3_MS = 2300L;

    public static final int NOTA_BAJO_PRUEBA_1 = 36;
    public static final int NOTA_BAJO_PRUEBA_2 = 43;
    public static final int NOTA_BAJO_PRUEBA_3 = 45;
    public static final int NOTA_BAJO_PRUEBA_4 = 41;
    public static final int DURACION_BAJO_PRUEBA_MS = 450;
    public static final long ESPERA_BAJO_PRUEBA_MS = 500L;
    public static final long ESPERA_ENTRE_BAJO_Y_ORGANO_MS = 500L;

    public static final int NOTA_ORGANO_PRUEBA_1 = 60;
    public static final int NOTA_ORGANO_PRUEBA_2 = 64;
    public static final int NOTA_ORGANO_PRUEBA_3 = 67;
    public static final long ESPERA_ORGANO_PRUEBA_1_MS = 1500L;
    public static final long ESPERA_ORGANO_PRUEBA_2_MS = 1500L;
    public static final long ESPERA_ORGANO_PRUEBA_3_MS = 2000L;
    public static final long ESPERA_LIBERACION_ORGANO_MS = 1000L;
    public static final int HILOS_PRUEBA_CONCURRENCIA = 4;
    public static final int CANTIDAD_PARTICIPANTES_PRUEBA_CONCURRENCIA = 2;
    public static final int CANTIDAD_COMANDOS_CONCURRENTES = 32;

    public static final String ERROR_PUERTO_INVALIDO = "El puerto OSC está fuera del rango permitido.";
    public static final String ERROR_INICIALIZAR_CLIENTE = "No se pudo inicializar el cliente OSC.";
    public static final String ERROR_ENVIAR_OSC = "No se pudo enviar el mensaje OSC.";
    public static final String ERROR_CLIENTE_CERRADO = "El cliente de SuperCollider está cerrado.";
    public static final String ERROR_TIPO_OSC_NO_ADMITIDO = "Tipo de argumento OSC no admitido: ";
    public static final String NOMBRE_TIPO_NULL = "null";
    public static final String ERROR_ACCION_NO_ADMITIDA = "La acción no es válida para este sintetizador.";
    public static final String ERROR_PARAMETROS_INCOMPLETOS = "Faltan parámetros requeridos para el sintetizador.";
    public static final String ERROR_TRES_PARCIALES = "El órgano requiere exactamente tres ratios y tres amplitudes.";
    public static final String ERROR_AMPLITUDES_ORGANO_INVALIDAS =
        "El órgano requiere exactamente tres amplitudes finitas entre 0 y 1.";
    public static final String ERROR_CONTROL_NO_DISPONIBLE = "No hay un control conectado.";
    public static final String ERROR_CONTROL_DESCONECTADO = "El control de juego se desconectó.";
    public static final String ERROR_INICIALIZAR_CONTROL = "No se pudo inicializar el control de juego.";
    public static final String ERROR_MICROFONO_NO_DISPONIBLE = "No hay un micrófono disponible.";
    public static final String ERROR_INICIALIZAR_MICROFONO = "No se pudo inicializar el micrófono.";
    public static final String ERROR_LEER_MICROFONO = "No se pudo leer el audio del micrófono.";
    public static final String ERROR_BLOQUE_PCM_INVALIDO = "El bloque PCM no tiene un formato de muestras válido.";
    public static final String ERROR_ARGUMENTOS_MAIN =
        "Uso: Main [host [port]] | --demo [host] [port] | --instrumentos [host] [port].";
    public static final String ERROR_MODO_MAIN = "El modo de ejecución indicado no es válido.";
    public static final String ERROR_INTERNO_INSTRUMENTO = "Error al ejecutar el instrumento: ";
    public static final String ERROR_DETENER_INSTRUMENTO = "Error al detener el instrumento: ";
}
