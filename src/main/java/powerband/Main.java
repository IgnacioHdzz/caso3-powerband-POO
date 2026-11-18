package powerband;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import powerband.excepciones.DispositivoNoDisponibleException;
import powerband.excepciones.PowerBandException;
import powerband.instrumentos.Instrumento;
import powerband.instrumentos.InstrumentoControl;
import powerband.instrumentos.InstrumentoMicrofono;
import powerband.model.ComandoBajo;
import powerband.model.ComandoOrgano;
import powerband.model.ComandoPiano;
import powerband.service.ClienteSuperCollider;
import powerband.util.IConstants;

public final class Main {
    private Main() {
    }

    public static void main(String[] pArgs) throws PowerBandException, InterruptedException {
        OpcionesEjecucion opciones = seleccionarOpciones(pArgs);

        try (ClienteSuperCollider cliente = new ClienteSuperCollider(opciones.anfitrion(), opciones.puerto())) {
            if (opciones.modo() == ModoEjecucion.DEMO) {
                ejecutarDemo(cliente);
            } else {
                ejecutarInstrumentos(cliente);
            }
        }
    }

    static OpcionesEjecucion seleccionarOpciones(String[] pArgs) {
        if (pArgs == null) {
            throw new IllegalArgumentException(IConstants.ERROR_ARGUMENTOS_MAIN);
        }
        ModoEjecucion modo = ModoEjecucion.DEMO;
        int inicioParametros = IConstants.INDICE_ARGUMENTO_INICIAL;
        if (pArgs.length > IConstants.CANTIDAD_ARGUMENTOS_NINGUNO
            && pArgs[IConstants.INDICE_ARGUMENTO_INICIAL].startsWith(IConstants.PREFIJO_ARGUMENTO_MODO)) {
            if (IConstants.ARGUMENTO_MODO_DEMO.equals(pArgs[IConstants.INDICE_ARGUMENTO_INICIAL])) {
                modo = ModoEjecucion.DEMO;
            } else if (IConstants.ARGUMENTO_MODO_INSTRUMENTOS.equals(pArgs[IConstants.INDICE_ARGUMENTO_INICIAL])) {
                modo = ModoEjecucion.INSTRUMENTOS;
            } else {
                throw new IllegalArgumentException(IConstants.ERROR_MODO_MAIN);
            }
            inicioParametros = 1;
        }

        int cantidadParametros = pArgs.length - inicioParametros;
        if (cantidadParametros < IConstants.CANTIDAD_ARGUMENTOS_NINGUNO
            || cantidadParametros > IConstants.CANTIDAD_ARGUMENTOS_DESTINO_OSC) {
            throw new IllegalArgumentException(IConstants.ERROR_ARGUMENTOS_MAIN);
        }
        String anfitrion = cantidadParametros > IConstants.CANTIDAD_ARGUMENTOS_NINGUNO
            ? pArgs[inicioParametros]
            : IConstants.HOST_SUPERCOLLIDER;
        int puerto = IConstants.PUERTO_SUPERCOLLIDER;
        if (cantidadParametros > IConstants.CANTIDAD_ARGUMENTOS_HOST) {
            try {
                puerto = Integer.parseInt(pArgs[inicioParametros + IConstants.INDICE_ARGUMENTO_PUERTO]);
            } catch (NumberFormatException pExcepcion) {
                throw new IllegalArgumentException(IConstants.ERROR_ARGUMENTOS_MAIN, pExcepcion);
            }
        }
        return new OpcionesEjecucion(modo, anfitrion, puerto);
    }

    private static void ejecutarDemo(ClienteSuperCollider pCliente)
        throws PowerBandException, InterruptedException {
        ComandoPiano piano = new ComandoPiano(pCliente);
        ComandoBajo bajo = new ComandoBajo(pCliente);
        ComandoOrgano organo = new ComandoOrgano(pCliente);

        piano.disparar(IConstants.NOTA_PIANO_PRUEBA_1, IConstants.DURACION_PIANO_PRUEBA_1_MS, true);
        Thread.sleep(IConstants.ESPERA_PIANO_PRUEBA_1_MS);
        piano.disparar(IConstants.NOTA_PIANO_PRUEBA_2, IConstants.DURACION_PIANO_PRUEBA_2_MS, false);
        Thread.sleep(IConstants.ESPERA_PIANO_PRUEBA_2_MS);
        piano.disparar(IConstants.NOTA_PIANO_PRUEBA_3, IConstants.DURACION_PIANO_PRUEBA_3_MS, true);
        Thread.sleep(IConstants.ESPERA_PIANO_PRUEBA_3_MS);

        int[] notasBajo = {
            IConstants.NOTA_BAJO_PRUEBA_1,
            IConstants.NOTA_BAJO_PRUEBA_2,
            IConstants.NOTA_BAJO_PRUEBA_3,
            IConstants.NOTA_BAJO_PRUEBA_4
        };
        for (int nota : notasBajo) {
            bajo.disparar(nota, IConstants.DURACION_BAJO_PRUEBA_MS);
            Thread.sleep(IConstants.ESPERA_BAJO_PRUEBA_MS);
        }
        Thread.sleep(IConstants.ESPERA_ENTRE_BAJO_Y_ORGANO_MS);

        organo.noteOn(IConstants.NOTA_ORGANO_PRUEBA_1);
        Thread.sleep(IConstants.ESPERA_ORGANO_PRUEBA_1_MS);
        organo.noteOn(
            IConstants.NOTA_ORGANO_PRUEBA_2,
            new float[] { IConstants.RATIO_PRUEBA_1, IConstants.RATIO_PRUEBA_2, IConstants.RATIO_PRUEBA_3 },
            new float[] { IConstants.AMPLITUD_PRUEBA_1, IConstants.AMPLITUD_PRUEBA_2, IConstants.AMPLITUD_PRUEBA_3 });
        Thread.sleep(IConstants.ESPERA_ORGANO_PRUEBA_2_MS);
        organo.noteOn(
            IConstants.NOTA_ORGANO_PRUEBA_3,
            new float[] { IConstants.RATIO_PRUEBA_4, IConstants.RATIO_PRUEBA_5, IConstants.RATIO_PRUEBA_6 },
            new float[] { IConstants.AMPLITUD_PRUEBA_1, IConstants.AMPLITUD_PRUEBA_4, IConstants.AMPLITUD_PRUEBA_5 });
        Thread.sleep(IConstants.ESPERA_ORGANO_PRUEBA_3_MS);
        organo.noteOff(IConstants.NOTA_ORGANO_PRUEBA_1);
        organo.noteOff(IConstants.NOTA_ORGANO_PRUEBA_2);
        organo.noteOff(IConstants.NOTA_ORGANO_PRUEBA_3);
        Thread.sleep(IConstants.ESPERA_LIBERACION_ORGANO_MS);
    }

    private static void ejecutarInstrumentos(ClienteSuperCollider pCliente) throws InterruptedException {
        List<Instrumento> instrumentos = new ArrayList<>();
        instrumentos.add(new InstrumentoControl(new ComandoOrgano(pCliente)));
        instrumentos.add(new InstrumentoMicrofono(new ComandoBajo(pCliente)));

        List<Instrumento> iniciados = iniciarInstrumentosIndependientemente(instrumentos);
        if (iniciados.isEmpty()) {
            return;
        }

        CountDownLatch cierreSolicitado =
            new CountDownLatch(IConstants.CANTIDAD_EVENTOS_CIERRE_INSTRUMENTOS);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            detenerInstrumentos(iniciados);
            cierreSolicitado.countDown();
        }, IConstants.NOMBRE_HILO_CIERRE_INSTRUMENTOS));
        try {
            cierreSolicitado.await();
        } finally {
            detenerInstrumentos(iniciados);
        }
    }

    static List<Instrumento> iniciarInstrumentosIndependientemente(List<Instrumento> pInstrumentos) {
        List<Instrumento> iniciados = new ArrayList<>();
        for (Instrumento instrumento : pInstrumentos) {
            try {
                instrumento.iniciar();
                iniciados.add(instrumento);
            } catch (DispositivoNoDisponibleException pExcepcion) {
                System.err.println(pExcepcion.getMessage());
            }
        }
        return List.copyOf(iniciados);
    }

    private static void detenerInstrumentos(List<Instrumento> pInstrumentos) {
        for (Instrumento instrumento : pInstrumentos) {
            try {
                instrumento.detener();
            } catch (PowerBandException pExcepcion) {
                informarErrorDetencion(pExcepcion);
            }
        }
    }

    private static void informarErrorDetencion(PowerBandException pExcepcion) {
        System.err.println(IConstants.ERROR_DETENER_INSTRUMENTO + pExcepcion.getMessage());
    }

    enum ModoEjecucion {
        DEMO,
        INSTRUMENTOS
    }

    record OpcionesEjecucion(ModoEjecucion modo, String anfitrion, int puerto) {
    }
}
