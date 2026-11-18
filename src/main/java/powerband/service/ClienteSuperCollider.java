package powerband.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import powerband.dto.EventoMusical;
import powerband.excepciones.ConexionSynthException;
import powerband.util.IConstants;

public class ClienteSuperCollider implements AutoCloseable {
    private final DatagramSocket socket;
    private final InetAddress direccion;
    private final int puerto;
    private final TransporteOsc transporte;
    private final LongSupplier relojMonotonico;
    private final List<IObservadorComandos> observadores;
    private final Map<Integer, Integer> nodosOrganos;
    private int siguienteIdNodo;
    private boolean cerrado;

    public ClienteSuperCollider() throws ConexionSynthException {
        this(IConstants.HOST_SUPERCOLLIDER, IConstants.PUERTO_SUPERCOLLIDER);
    }

    public ClienteSuperCollider(String pHost, int pPuerto) throws ConexionSynthException {
        this(pHost, pPuerto, null, System::nanoTime);
    }

    public ClienteSuperCollider(
        String pHost,
        int pPuerto,
        TransporteOsc pTransporte,
        LongSupplier pRelojMonotonico
    ) throws ConexionSynthException {
        if (pPuerto < IConstants.PUERTO_MINIMO || pPuerto > IConstants.PUERTO_MAXIMO) {
            throw new ConexionSynthException(IConstants.ERROR_PUERTO_INVALIDO);
        }
        Objects.requireNonNull(pHost);
        Objects.requireNonNull(pRelojMonotonico);

        try {
            this.direccion = InetAddress.getByName(pHost);
            this.socket = new DatagramSocket();
        } catch (IOException pExcepcion) {
            throw new ConexionSynthException(IConstants.ERROR_INICIALIZAR_CLIENTE, pExcepcion);
        }

        this.puerto = pPuerto;
        this.relojMonotonico = pRelojMonotonico;
        this.transporte = pTransporte == null
            ? pDatos -> socket.send(new DatagramPacket(pDatos, pDatos.length, direccion, puerto))
            : pTransporte;
        this.observadores = new CopyOnWriteArrayList<>();
        this.nodosOrganos = new HashMap<>();
        this.siguienteIdNodo = IConstants.OSC_PRIMER_ID_NODO;
    }

    public void agregarObservador(IObservadorComandos pObservador) {
        observadores.add(Objects.requireNonNull(pObservador));
    }

    public void quitarObservador(IObservadorComandos pObservador) {
        observadores.remove(pObservador);
    }

    public EventoMusical crearEvento(
        String pTipoSynth,
        EventoMusical.Accion pAccion,
        int pNota,
        Integer pDuracionMs,
        Integer pTercera,
        Float pRatio1,
        Float pRatio2,
        Float pRatio3,
        Float pAmplitud1,
        Float pAmplitud2,
        Float pAmplitud3
    ) {
        long instanteMs = TimeUnit.NANOSECONDS.toMillis(relojMonotonico.getAsLong());
        return new EventoMusical(
            instanteMs,
            pTipoSynth,
            pAccion,
            pNota,
            pDuracionMs,
            pTercera,
            pRatio1,
            pRatio2,
            pRatio3,
            pAmplitud1,
            pAmplitud2,
            pAmplitud3
        );
    }

    public synchronized int crearNodo(String pNombreSynth, EventoMusical pEvento, Object... pControles)
        throws ConexionSynthException {
        verificarAbierto();
        int idNodo = siguienteIdNodo++;
        Object[] argumentos = new Object[IConstants.CANTIDAD_ARGUMENTOS_S_NEW_BASE + pControles.length];
        argumentos[0] = pNombreSynth;
        argumentos[1] = idNodo;
        argumentos[2] = IConstants.OSC_ADD_TO_HEAD;
        argumentos[3] = IConstants.OSC_GRUPO_DEFAULT;
        System.arraycopy(pControles, 0, argumentos, IConstants.CANTIDAD_ARGUMENTOS_S_NEW_BASE, pControles.length);
        enviarMensaje(IConstants.OSC_S_NEW, pEvento, argumentos);
        return idNodo;
    }

    public synchronized void iniciarOrgano(
        int pNota,
        float pFrecuencia,
        float[] pRatios,
        float[] pAmplitudes,
        EventoMusical pEventoNoteOn
    ) throws ConexionSynthException {
        verificarAbierto();
        if (pRatios == null || pAmplitudes == null
            || pRatios.length != IConstants.CANTIDAD_PARCIALES
            || pAmplitudes.length != IConstants.CANTIDAD_PARCIALES) {
            throw new IllegalArgumentException(IConstants.ERROR_TRES_PARCIALES);
        }

        apagarOrganoInterno(pNota);
        int idNodo = crearNodo(
            IConstants.SYNTH_ORGANO,
            pEventoNoteOn,
            IConstants.CONTROL_FRECUENCIA, pFrecuencia,
            IConstants.CONTROL_RATIO_1, pRatios[0],
            IConstants.CONTROL_RATIO_2, pRatios[1],
            IConstants.CONTROL_RATIO_3, pRatios[2],
            IConstants.CONTROL_AMPLITUD_1, pAmplitudes[0],
            IConstants.CONTROL_AMPLITUD_2, pAmplitudes[1],
            IConstants.CONTROL_AMPLITUD_3, pAmplitudes[2]
        );
        nodosOrganos.put(pNota, idNodo);
    }

    public synchronized void apagarOrgano(int pNota) throws ConexionSynthException {
        verificarAbierto();
        apagarOrganoInterno(pNota);
    }

    public synchronized void actualizarAmplitudesOrgano(int pNota, float[] pAmplitudes)
        throws ConexionSynthException {
        verificarAbierto();
        if (pAmplitudes == null || pAmplitudes.length != IConstants.CANTIDAD_PARCIALES) {
            throw new IllegalArgumentException(IConstants.ERROR_AMPLITUDES_ORGANO_INVALIDAS);
        }

        float[] amplitudes = pAmplitudes.clone();
        for (float amplitud : amplitudes) {
            if (!Float.isFinite(amplitud)
                || amplitud < IConstants.AMPLITUD_MINIMA_ORGANO
                || amplitud > IConstants.AMPLITUD_MAXIMA_ORGANO) {
                throw new IllegalArgumentException(IConstants.ERROR_AMPLITUDES_ORGANO_INVALIDAS);
            }
        }

        Integer idNodo = nodosOrganos.get(pNota);
        if (idNodo == null) {
            return;
        }

        EventoMusical evento = crearEvento(
            IConstants.SYNTH_ORGANO,
            EventoMusical.Accion.ACTUALIZAR_PARAMETROS,
            pNota,
            null,
            null,
            null,
            null,
            null,
            amplitudes[0],
            amplitudes[1],
            amplitudes[2]
        );
        enviarMensaje(
            IConstants.OSC_N_SET,
            evento,
            idNodo,
            IConstants.CONTROL_AMPLITUD_1,
            amplitudes[0],
            IConstants.CONTROL_AMPLITUD_2,
            amplitudes[1],
            IConstants.CONTROL_AMPLITUD_3,
            amplitudes[2]
        );
    }

    private void apagarOrganoInterno(int pNota) throws ConexionSynthException {
        Integer idNodo = nodosOrganos.remove(pNota);
        if (idNodo != null) {
            EventoMusical evento = crearEvento(
                IConstants.SYNTH_ORGANO,
                EventoMusical.Accion.NOTE_OFF,
                pNota,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
            );
            enviarMensaje(
                IConstants.OSC_N_SET,
                evento,
                idNodo,
                IConstants.CONTROL_GATE,
                IConstants.VALOR_GATE_APAGADO
            );
        }
    }

    private void enviarMensaje(String pDireccionOsc, EventoMusical pEvento, Object... pArgumentos)
        throws ConexionSynthException {
        byte[] mensaje = codificarMensaje(pDireccionOsc, pArgumentos);
        try {
            transporte.enviar(mensaje);
        } catch (IOException pExcepcion) {
            throw new ConexionSynthException(IConstants.ERROR_ENVIAR_OSC, pExcepcion);
        }
        for (IObservadorComandos observador : observadores) {
            observador.actualizar(pEvento);
        }
    }

    private void verificarAbierto() throws ConexionSynthException {
        if (cerrado) {
            throw new ConexionSynthException(IConstants.ERROR_CLIENTE_CERRADO);
        }
    }

    static byte[] codificarMensaje(String pDireccionOsc, Object... pArgumentos) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        escribirCadenaOsc(salida, pDireccionOsc);

        StringBuilder etiquetas = new StringBuilder(IConstants.OSC_TAGS_PREFIX);
        for (Object argumento : pArgumentos) {
            if (argumento instanceof Integer) {
                etiquetas.append(IConstants.OSC_TAG_ENTERO);
            } else if (argumento instanceof Float) {
                etiquetas.append(IConstants.OSC_TAG_FLOAT);
            } else if (argumento instanceof String) {
                etiquetas.append(IConstants.OSC_TAG_STRING);
            } else {
                String tipo = argumento == null ? IConstants.NOMBRE_TIPO_NULL : argumento.getClass().getSimpleName();
                throw new IllegalArgumentException(IConstants.ERROR_TIPO_OSC_NO_ADMITIDO + tipo);
            }
        }
        escribirCadenaOsc(salida, etiquetas.toString());

        for (Object argumento : pArgumentos) {
            if (argumento instanceof Integer valor) {
                escribirEnteroOsc(salida, valor);
            } else if (argumento instanceof Float valor) {
                escribirEnteroOsc(salida, Float.floatToIntBits(valor));
            } else if (argumento instanceof String valor) {
                escribirCadenaOsc(salida, valor);
            }
        }
        return salida.toByteArray();
    }

    private static void escribirCadenaOsc(ByteArrayOutputStream pSalida, String pTexto) {
        byte[] bytes = pTexto.getBytes(StandardCharsets.UTF_8);
        pSalida.writeBytes(bytes);
        int relleno = IConstants.OSC_BYTES_POR_ENTERO - bytes.length % IConstants.OSC_BYTES_POR_ENTERO;
        for (int indice = 0; indice < relleno; indice++) {
            pSalida.write(IConstants.BYTE_NULO);
        }
    }

    private static void escribirEnteroOsc(ByteArrayOutputStream pSalida, int pValor) {
        byte[] bytes = ByteBuffer.allocate(IConstants.OSC_BYTES_POR_ENTERO)
            .order(ByteOrder.BIG_ENDIAN)
            .putInt(pValor)
            .array();
        pSalida.writeBytes(bytes);
    }

    @Override
    public synchronized void close() throws ConexionSynthException {
        if (cerrado) {
            return;
        }

        ConexionSynthException primerError = null;
        List<Integer> notasActivas = new ArrayList<>(nodosOrganos.keySet());
        try {
            for (int nota : notasActivas) {
                try {
                    apagarOrganoInterno(nota);
                } catch (ConexionSynthException pExcepcion) {
                    if (primerError == null) {
                        primerError = pExcepcion;
                    } else {
                        primerError.addSuppressed(pExcepcion);
                    }
                }
            }
        } finally {
            cerrado = true;
            socket.close();
        }
        if (primerError != null) {
            throw primerError;
        }
    }
}
