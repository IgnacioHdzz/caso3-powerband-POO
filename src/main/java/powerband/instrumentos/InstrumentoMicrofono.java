package powerband.instrumentos;

import java.util.Objects;
import java.util.function.LongSupplier;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import powerband.dto.EventoMusical;
import powerband.excepciones.DispositivoNoDisponibleException;
import powerband.excepciones.PowerBandException;
import powerband.model.ComandoSynth;
import powerband.util.IConstants;

public final class InstrumentoMicrofono extends Instrumento {
    private final CreadorFuenteAudio creadorFuente;
    private final LongSupplier relojMonotonico;
    private volatile boolean ejecutando;
    private FuenteAudio fuenteAudio;
    private Thread hilo;
    private boolean armado = true;
    private long ultimoDisparoNanos = Long.MIN_VALUE;

    public InstrumentoMicrofono(ComandoSynth pComandoSynth) {
        this(pComandoSynth, FuenteAudioSistema::new, System::nanoTime);
    }

    InstrumentoMicrofono(
        ComandoSynth pComandoSynth,
        CreadorFuenteAudio pCreadorFuente,
        LongSupplier pRelojMonotonico
    ) {
        super(pComandoSynth);
        this.creadorFuente = Objects.requireNonNull(pCreadorFuente);
        this.relojMonotonico = Objects.requireNonNull(pRelojMonotonico);
    }

    @Override
    public synchronized void iniciar() throws DispositivoNoDisponibleException {
        if (ejecutando) {
            return;
        }
        try {
            fuenteAudio = creadorFuente.crear();
            fuenteAudio.abrir();
        } catch (DispositivoNoDisponibleException pExcepcion) {
            cerrarFuente();
            throw pExcepcion;
        }
        armado = true;
        ultimoDisparoNanos = Long.MIN_VALUE;
        ejecutando = true;
        FuenteAudio fuente = fuenteAudio;
        hilo = new Thread(() -> leerMicrofono(fuente), IConstants.NOMBRE_HILO_MICROFONO);
        hilo.start();
    }

    @Override
    public void detener() throws PowerBandException {
        Thread hiloDetener;
        synchronized (this) {
            ejecutando = false;
            cerrarFuente();
            hiloDetener = hilo;
            if (hiloDetener != null) {
                hiloDetener.interrupt();
            }
        }
        if (hiloDetener != null && hiloDetener != Thread.currentThread()) {
            try {
                hiloDetener.join();
            } catch (InterruptedException pExcepcion) {
                Thread.currentThread().interrupt();
                throw new PowerBandException(IConstants.ERROR_DETENER_INSTRUMENTO, pExcepcion);
            }
        }
    }

    void procesarBloque(byte[] pBuffer, int pCantidadBytes) throws PowerBandException {
        short[] muestras = decodificarMuestras(pBuffer, pCantidadBytes);
        if (muestras.length == IConstants.CANTIDAD_BYTES_VACIA) {
            return;
        }
        double rms = calcularRms(muestras);
        if (rms < IConstants.UMBRAL_RMS_MICROFONO) {
            armado = true;
            return;
        }
        if (!armado) {
            return;
        }
        armado = false;

        long ahoraNanos = relojMonotonico.getAsLong();
        long enfriamientoNanos = IConstants.ENFRIAMIENTO_MICROFONO_MS
            * IConstants.NANOSEGUNDOS_POR_MILISEGUNDO;
        if (ultimoDisparoNanos != Long.MIN_VALUE
            && ahoraNanos - ultimoDisparoNanos < enfriamientoNanos) {
            return;
        }

        double frecuencia = estimarFrecuencia(muestras);
        Integer nota = seleccionarNota(frecuencia);
        if (nota == null) {
            return;
        }
        obtenerComandoSynth().ejecutar(
            EventoMusical.Accion.DISPARO,
            nota,
            IConstants.DURACION_DISPARO_BAJO_MS,
            null,
            null,
            null
        );
        ultimoDisparoNanos = ahoraNanos;
    }

    static short[] decodificarMuestras(byte[] pBuffer, int pCantidadBytes) {
        if (pBuffer == null || pCantidadBytes < IConstants.CANTIDAD_BYTES_VACIA
            || pCantidadBytes > pBuffer.length
            || pCantidadBytes % IConstants.BYTES_FRAME_MICROFONO != 0) {
            throw new IllegalArgumentException(IConstants.ERROR_BLOQUE_PCM_INVALIDO);
        }
        short[] muestras = new short[pCantidadBytes / IConstants.BYTES_MUESTRA_MICROFONO];
        for (int indice = IConstants.INDICE_ELEMENTO_INICIAL; indice < muestras.length; indice++) {
            int posicion = indice * IConstants.BYTES_MUESTRA_MICROFONO;
            int byteMenosSignificativo = Byte.toUnsignedInt(pBuffer[posicion]);
            int byteMasSignificativo = pBuffer[posicion + IConstants.DESPLAZAMIENTO_BYTE_ALTO_PCM];
            muestras[indice] = (short) ((byteMasSignificativo << Byte.SIZE) | byteMenosSignificativo);
        }
        return muestras;
    }

    static double calcularRms(short[] pMuestras) {
        if (pMuestras == null || pMuestras.length == IConstants.CANTIDAD_BYTES_VACIA) {
            return IConstants.VALOR_RMS_SILENCIO;
        }
        double sumaCuadrados = IConstants.VALOR_RMS_SILENCIO;
        for (short muestra : pMuestras) {
            double normalizada = muestra / IConstants.DIVISOR_NORMALIZACION_PCM;
            sumaCuadrados += normalizada * normalizada;
        }
        return Math.sqrt(sumaCuadrados / pMuestras.length);
    }

    static double estimarFrecuencia(short[] pMuestras) {
        if (pMuestras == null || pMuestras.length < IConstants.CANTIDAD_MUESTRAS_MINIMAS_TONO) {
            return Double.NaN;
        }
        int signoAnterior = IConstants.SIGNO_PCM_SILENCIO;
        int cruces = IConstants.CANTIDAD_BYTES_VACIA;
        for (short muestra : pMuestras) {
            int signoActual = Integer.compare(muestra, IConstants.SIGNO_PCM_SILENCIO);
            if (signoActual == IConstants.SIGNO_PCM_SILENCIO) {
                continue;
            }
            if (signoAnterior != IConstants.SIGNO_PCM_SILENCIO && signoActual != signoAnterior) {
                cruces++;
            }
            signoAnterior = signoActual;
        }
        if (cruces < IConstants.CANTIDAD_CRUCES_MINIMOS_TONO) {
            return Double.NaN;
        }
        return (cruces / IConstants.CRUCES_POR_CICLO)
            * IConstants.FRECUENCIA_MUESTREO_MICROFONO_HZ / pMuestras.length;
    }

    static Integer seleccionarNota(double pFrecuenciaHz) {
        if (!Double.isFinite(pFrecuenciaHz) || pFrecuenciaHz <= IConstants.FRECUENCIA_MINIMA_VALIDA_HZ) {
            return null;
        }
        int[] escala = obtenerEscalaBajo();
        double frecuenciaMinima = frecuenciaMIDI(escala[0]);
        double frecuenciaMaxima = frecuenciaMIDI(escala[escala.length - 1]);
        if (pFrecuenciaHz < frecuenciaMinima || pFrecuenciaHz > frecuenciaMaxima) {
            return null;
        }
        double notaMIDI = IConstants.NOTA_MIDI_LA4
            + IConstants.SEMITONOS_POR_OCTAVA
            * (Math.log(pFrecuenciaHz / IConstants.FRECUENCIA_LA4_HZ) / Math.log(IConstants.BASE_LOGARITMO_DOS));
        int notaMasCercana = escala[0];
        double distanciaMinima = Math.abs(notaMIDI - notaMasCercana);
        for (int indice = 1; indice < escala.length; indice++) {
            double distancia = Math.abs(notaMIDI - escala[indice]);
            if (distancia + IConstants.TOLERANCIA_EMPATE_NOTA_MIDI < distanciaMinima) {
                notaMasCercana = escala[indice];
                distanciaMinima = distancia;
            }
        }
        return notaMasCercana;
    }

    private static double frecuenciaMIDI(int pNota) {
        return IConstants.FRECUENCIA_LA4_HZ
            * Math.pow(
                IConstants.BASE_LOGARITMO_DOS,
                (pNota - IConstants.NOTA_MIDI_LA4) / IConstants.SEMITONOS_POR_OCTAVA
            );
    }

    private static int[] obtenerEscalaBajo() {
        return new int[] {
            IConstants.NOTA_BAJO_ESCALA_01,
            IConstants.NOTA_BAJO_ESCALA_02,
            IConstants.NOTA_BAJO_ESCALA_03,
            IConstants.NOTA_BAJO_ESCALA_04,
            IConstants.NOTA_BAJO_ESCALA_05,
            IConstants.NOTA_BAJO_ESCALA_06,
            IConstants.NOTA_BAJO_ESCALA_07,
            IConstants.NOTA_BAJO_ESCALA_08,
            IConstants.NOTA_BAJO_ESCALA_09,
            IConstants.NOTA_BAJO_ESCALA_10,
            IConstants.NOTA_BAJO_ESCALA_11,
            IConstants.NOTA_BAJO_ESCALA_12,
            IConstants.NOTA_BAJO_ESCALA_13,
            IConstants.NOTA_BAJO_ESCALA_14,
            IConstants.NOTA_BAJO_ESCALA_15
        };
    }

    private void leerMicrofono(FuenteAudio pFuenteAudio) {
        byte[] buffer = new byte[
            IConstants.MUESTRAS_BLOQUE_MICROFONO * IConstants.BYTES_FRAME_MICROFONO
        ];
        try {
            while (ejecutando) {
                int cantidadBytes = 0;
                while (ejecutando && cantidadBytes < buffer.length) {
                    int leidos = pFuenteAudio.leer(buffer, cantidadBytes, buffer.length - cantidadBytes);
                    if (leidos < IConstants.CANTIDAD_BYTES_VACIA) {
                        throw new DispositivoNoDisponibleException(IConstants.ERROR_LEER_MICROFONO);
                    }
                    if (leidos > IConstants.CANTIDAD_BYTES_VACIA) {
                        cantidadBytes += leidos;
                    }
                }
                if (ejecutando && cantidadBytes == buffer.length) {
                    procesarBloque(buffer, cantidadBytes);
                }
            }
        } catch (PowerBandException pExcepcion) {
            if (ejecutando) {
                informarError(pExcepcion);
            }
        } finally {
            ejecutando = false;
            cerrarFuente();
            synchronized (this) {
                hilo = null;
            }
        }
    }

    private synchronized void cerrarFuente() {
        if (fuenteAudio != null) {
            fuenteAudio.cerrar();
            fuenteAudio = null;
        }
    }

    private static void informarError(PowerBandException pExcepcion) {
        System.err.println(IConstants.ERROR_INTERNO_INSTRUMENTO + pExcepcion.getMessage());
    }

    @FunctionalInterface
    interface CreadorFuenteAudio {
        FuenteAudio crear() throws DispositivoNoDisponibleException;
    }

    interface FuenteAudio {
        void abrir() throws DispositivoNoDisponibleException;

        int leer(byte[] pBuffer, int pDesplazamiento, int pLongitud) throws DispositivoNoDisponibleException;

        void cerrar();
    }

    private static final class FuenteAudioSistema implements FuenteAudio {
        private TargetDataLine linea;

        @Override
        public void abrir() throws DispositivoNoDisponibleException {
            AudioFormat formato = new AudioFormat(
                IConstants.FRECUENCIA_MUESTREO_MICROFONO_HZ,
                IConstants.BITS_MUESTRA_MICROFONO,
                IConstants.CANALES_MICROFONO,
                IConstants.PCM_FIRMADO,
                IConstants.PCM_BIG_ENDIAN
            );
            DataLine.Info informacion = new DataLine.Info(TargetDataLine.class, formato);
            try {
                if (!AudioSystem.isLineSupported(informacion)) {
                    throw new DispositivoNoDisponibleException(IConstants.ERROR_MICROFONO_NO_DISPONIBLE);
                }
                linea = (TargetDataLine) AudioSystem.getLine(informacion);
                linea.open(
                    formato,
                    IConstants.MUESTRAS_BLOQUE_MICROFONO * IConstants.BYTES_FRAME_MICROFONO
                );
                linea.start();
            } catch (LineUnavailableException | IllegalArgumentException | SecurityException pExcepcion) {
                cerrar();
                throw new DispositivoNoDisponibleException(IConstants.ERROR_INICIALIZAR_MICROFONO, pExcepcion);
            } catch (DispositivoNoDisponibleException pExcepcion) {
                throw pExcepcion;
            }
        }

        @Override
        public int leer(byte[] pBuffer, int pDesplazamiento, int pLongitud)
            throws DispositivoNoDisponibleException {
            try {
                if (linea == null || !linea.isOpen()) {
                    throw new DispositivoNoDisponibleException(IConstants.ERROR_MICROFONO_NO_DISPONIBLE);
                }
                return linea.read(pBuffer, pDesplazamiento, pLongitud);
            } catch (IllegalArgumentException | IllegalStateException pExcepcion) {
                throw new DispositivoNoDisponibleException(IConstants.ERROR_LEER_MICROFONO, pExcepcion);
            }
        }

        @Override
        public void cerrar() {
            if (linea != null) {
                linea.close();
                linea = null;
            }
        }
    }
}
