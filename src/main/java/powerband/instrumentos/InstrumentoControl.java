package powerband.instrumentos;

import com.studiohartman.jamepad.ControllerManager;
import com.studiohartman.jamepad.ControllerState;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import powerband.dto.EventoMusical;
import powerband.excepciones.DispositivoNoDisponibleException;
import powerband.excepciones.PowerBandException;
import powerband.model.ComandoSynth;
import powerband.util.IConstants;

public final class InstrumentoControl extends Instrumento {
    private final FuenteControl fuenteControl;
    private final Map<Integer, float[]> amplitudesActivas;
    private volatile boolean ejecutando;
    private Thread hilo;

    public InstrumentoControl(ComandoSynth pComandoSynth) {
        this(pComandoSynth, new FuenteJamepad());
    }

    InstrumentoControl(ComandoSynth pComandoSynth, FuenteControl pFuenteControl) {
        super(pComandoSynth);
        this.fuenteControl = Objects.requireNonNull(pFuenteControl);
        this.amplitudesActivas = new HashMap<>();
    }

    @Override
    public synchronized void iniciar() throws DispositivoNoDisponibleException {
        if (ejecutando) {
            return;
        }
        try {
            fuenteControl.iniciar();
        } catch (DispositivoNoDisponibleException pExcepcion) {
            fuenteControl.cerrar();
            throw pExcepcion;
        }
        ejecutando = true;
        hilo = new Thread(this::leerControl, IConstants.NOMBRE_HILO_CONTROL);
        hilo.start();
    }

    @Override
    public void detener() throws PowerBandException {
        Thread hiloDetener;
        synchronized (this) {
            ejecutando = false;
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

    synchronized void procesarEstado(EstadoControl pEstado) throws PowerBandException {
        float[] amplitudes = calcularAmplitudes(
            pEstado.ejeIzquierdoX(),
            pEstado.ejeIzquierdoY(),
            pEstado.ejeDerechoY()
        );
        for (BotonControl boton : BotonControl.values()) {
            procesarBoton(
                pEstado.botonesPresionados().contains(boton),
                obtenerNota(boton),
                amplitudes
            );
        }
        actualizarNotasActivas(amplitudes);
    }

    static float[] calcularAmplitudes(float pEjeIzquierdoX, float pEjeIzquierdoY, float pEjeDerechoY) {
        return new float[] {
            calcularAmplitud(pEjeIzquierdoX, IConstants.AMPLITUD_DEFAULT_1),
            calcularAmplitud(pEjeIzquierdoY, IConstants.AMPLITUD_DEFAULT_2),
            calcularAmplitud(pEjeDerechoY, IConstants.AMPLITUD_DEFAULT_3)
        };
    }

    private static float calcularAmplitud(float pEje, float pBase) {
        float eje = Float.isFinite(pEje)
            ? Math.max(-IConstants.CONTROL_RANGO_EJE, Math.min(IConstants.CONTROL_RANGO_EJE, pEje))
            : IConstants.CONTROL_EJE_NEUTRO;
        float zonaMuerta = IConstants.CONTROL_ZONA_MUERTA;
        if (Math.abs(eje) <= zonaMuerta) {
            return pBase;
        }
        if (eje < -zonaMuerta) {
            return pBase * (IConstants.CONTROL_RANGO_EJE
                - ((-eje - zonaMuerta) / (IConstants.CONTROL_RANGO_EJE - zonaMuerta)));
        }
        return pBase + (IConstants.CONTROL_RANGO_EJE - pBase)
            * ((eje - zonaMuerta) / (IConstants.CONTROL_RANGO_EJE - zonaMuerta));
    }

    private static int obtenerNota(BotonControl pBoton) {
        return switch (pBoton) {
            case CRUZ -> IConstants.NOTA_CONTROL_CRUZ;
            case CIRCULO -> IConstants.NOTA_CONTROL_CIRCULO;
            case CUADRADO -> IConstants.NOTA_CONTROL_CUADRADO;
            case TRIANGULO -> IConstants.NOTA_CONTROL_TRIANGULO;
            case L1 -> IConstants.NOTA_CONTROL_L1;
            case R1 -> IConstants.NOTA_CONTROL_R1;
            case ARRIBA -> IConstants.NOTA_CONTROL_ARRIBA;
            case DERECHA -> IConstants.NOTA_CONTROL_DERECHA;
            case ABAJO -> IConstants.NOTA_CONTROL_ABAJO;
            case IZQUIERDA -> IConstants.NOTA_CONTROL_IZQUIERDA;
        };
    }

    private void procesarBoton(boolean pPresionado, int pNota, float[] pAmplitudes) throws PowerBandException {
        if (pPresionado && !amplitudesActivas.containsKey(pNota)) {
            obtenerComandoSynth().ejecutar(
                EventoMusical.Accion.NOTE_ON,
                pNota,
                null,
                null,
                obtenerRatios(),
                pAmplitudes
            );
            amplitudesActivas.put(pNota, pAmplitudes.clone());
        } else if (!pPresionado && amplitudesActivas.containsKey(pNota)) {
            obtenerComandoSynth().ejecutar(
                EventoMusical.Accion.NOTE_OFF,
                pNota,
                null,
                null,
                null,
                null
            );
            amplitudesActivas.remove(pNota);
        }
    }

    private void actualizarNotasActivas(float[] pAmplitudes) throws PowerBandException {
        for (Map.Entry<Integer, float[]> entrada : amplitudesActivas.entrySet()) {
            if (cambiaronAmplitudes(entrada.getValue(), pAmplitudes)) {
                obtenerComandoSynth().ejecutar(
                    EventoMusical.Accion.ACTUALIZAR_PARAMETROS,
                    entrada.getKey(),
                    null,
                    null,
                    null,
                    pAmplitudes
                );
                entrada.setValue(pAmplitudes.clone());
            }
        }
    }

    private static boolean cambiaronAmplitudes(float[] pAnteriores, float[] pActuales) {
        for (int indice = 0; indice < IConstants.CANTIDAD_PARCIALES; indice++) {
            if (Math.abs(pAnteriores[indice] - pActuales[indice]) >= IConstants.CONTROL_PASO_AMPLITUD) {
                return true;
            }
        }
        return false;
    }

    private static float[] obtenerRatios() {
        return new float[] {
            IConstants.RATIO_DEFAULT_1,
            IConstants.RATIO_DEFAULT_2,
            IConstants.RATIO_DEFAULT_3
        };
    }

    private void leerControl() {
        try {
            while (ejecutando) {
                procesarEstado(fuenteControl.leerEstado());
                Thread.sleep(IConstants.CONTROL_INTERVALO_LECTURA_MS);
            }
        } catch (PowerBandException pExcepcion) {
            if (ejecutando) {
                informarError(pExcepcion);
            }
        } catch (InterruptedException pExcepcion) {
            Thread.currentThread().interrupt();
        } finally {
            ejecutando = false;
            liberarNotasActivas();
            fuenteControl.cerrar();
            synchronized (this) {
                hilo = null;
            }
        }
    }

    private synchronized void liberarNotasActivas() {
        for (int nota : Set.copyOf(amplitudesActivas.keySet())) {
            try {
                obtenerComandoSynth().ejecutar(
                    EventoMusical.Accion.NOTE_OFF,
                    nota,
                    null,
                    null,
                    null,
                    null
                );
            } catch (PowerBandException pExcepcion) {
                informarError(pExcepcion);
            } finally {
                amplitudesActivas.remove(nota);
            }
        }
    }

    private static void informarError(PowerBandException pExcepcion) {
        System.err.println(IConstants.ERROR_INTERNO_INSTRUMENTO + pExcepcion.getMessage());
    }

    enum BotonControl {
        CRUZ,
        CIRCULO,
        CUADRADO,
        TRIANGULO,
        L1,
        R1,
        ARRIBA,
        DERECHA,
        ABAJO,
        IZQUIERDA
    }

    record EstadoControl(Set<BotonControl> botonesPresionados, float ejeIzquierdoX, float ejeIzquierdoY,
                         float ejeDerechoY) {
        EstadoControl {
            botonesPresionados = Set.copyOf(botonesPresionados);
        }
    }

    interface FuenteControl {
        void iniciar() throws DispositivoNoDisponibleException;

        EstadoControl leerEstado() throws DispositivoNoDisponibleException;

        void cerrar();
    }

    private static final class FuenteJamepad implements FuenteControl {
        private final ControllerManager administrador;
        private boolean inicializado;

        private FuenteJamepad() {
            administrador = new ControllerManager();
        }

        @Override
        public void iniciar() throws DispositivoNoDisponibleException {
            try {
                administrador.initSDLGamepad();
                inicializado = true;
                if (administrador.getNumControllers() <= IConstants.CANTIDAD_ARGUMENTOS_NINGUNO
                    || !administrador.getState(IConstants.INDICE_CONTROL_PRIMARIO).isConnected) {
                    cerrar();
                    throw new DispositivoNoDisponibleException(IConstants.ERROR_CONTROL_NO_DISPONIBLE);
                }
            } catch (DispositivoNoDisponibleException pExcepcion) {
                throw pExcepcion;
            } catch (RuntimeException pExcepcion) {
                cerrar();
                throw new DispositivoNoDisponibleException(IConstants.ERROR_INICIALIZAR_CONTROL, pExcepcion);
            }
        }

        @Override
        public EstadoControl leerEstado() throws DispositivoNoDisponibleException {
            ControllerState estado;
            try {
                estado = administrador.getState(IConstants.INDICE_CONTROL_PRIMARIO);
            } catch (RuntimeException pExcepcion) {
                throw new DispositivoNoDisponibleException(IConstants.ERROR_CONTROL_DESCONECTADO, pExcepcion);
            }
            if (estado == null || !estado.isConnected) {
                throw new DispositivoNoDisponibleException(IConstants.ERROR_CONTROL_DESCONECTADO);
            }
            Set<BotonControl> botones = EnumSet.noneOf(BotonControl.class);
            agregarSiPresionado(botones, BotonControl.CRUZ, estado.a);
            agregarSiPresionado(botones, BotonControl.CIRCULO, estado.b);
            agregarSiPresionado(botones, BotonControl.CUADRADO, estado.x);
            agregarSiPresionado(botones, BotonControl.TRIANGULO, estado.y);
            agregarSiPresionado(botones, BotonControl.L1, estado.lb);
            agregarSiPresionado(botones, BotonControl.R1, estado.rb);
            agregarSiPresionado(botones, BotonControl.ARRIBA, estado.dpadUp);
            agregarSiPresionado(botones, BotonControl.DERECHA, estado.dpadRight);
            agregarSiPresionado(botones, BotonControl.ABAJO, estado.dpadDown);
            agregarSiPresionado(botones, BotonControl.IZQUIERDA, estado.dpadLeft);
            return new EstadoControl(botones, estado.leftStickX, estado.leftStickY, estado.rightStickY);
        }

        @Override
        public void cerrar() {
            if (inicializado) {
                administrador.quitSDLGamepad();
                inicializado = false;
            }
        }

        private static void agregarSiPresionado(
            Set<BotonControl> pBotones,
            BotonControl pBoton,
            boolean pPresionado
        ) {
            if (pPresionado) {
                pBotones.add(pBoton);
            }
        }
    }
}
