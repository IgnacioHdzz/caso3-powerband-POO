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

    public static final int TERCERA_MAYOR_SEMITONOS = 4;
    public static final int TERCERA_MENOR_SEMITONOS = 3;
    public static final int CANTIDAD_PARCIALES = 3;
    public static final double FRECUENCIA_LA4_HZ = 440.0;
    public static final double NOTA_MIDI_LA4 = 69.0;
    public static final double SEMITONOS_POR_OCTAVA = 12.0;
    public static final long NANOSEGUNDOS_POR_MILISEGUNDO = 1_000_000L;

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
}
