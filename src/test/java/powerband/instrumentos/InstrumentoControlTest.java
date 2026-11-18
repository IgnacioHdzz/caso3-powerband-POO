package powerband.instrumentos;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import powerband.dto.EventoMusical;
import powerband.excepciones.DispositivoNoDisponibleException;
import powerband.helpers.TransporteOscFalso;
import powerband.service.ClienteSuperCollider;
import powerband.util.IConstants;

class InstrumentoControlTest {
    @Test
    void mapeaBotonesYDetectaTransicionesPresionadoYSoltado() throws Exception {
        int[] notas = {
            IConstants.NOTA_CONTROL_CRUZ,
            IConstants.NOTA_CONTROL_CIRCULO,
            IConstants.NOTA_CONTROL_CUADRADO,
            IConstants.NOTA_CONTROL_TRIANGULO,
            IConstants.NOTA_CONTROL_L1,
            IConstants.NOTA_CONTROL_R1,
            IConstants.NOTA_CONTROL_ARRIBA,
            IConstants.NOTA_CONTROL_DERECHA,
            IConstants.NOTA_CONTROL_ABAJO,
            IConstants.NOTA_CONTROL_IZQUIERDA
        };
        try (ClienteSuperCollider cliente = crearCliente()) {
            ComandoSynthFalso comando = new ComandoSynthFalso(cliente);
            InstrumentoControl instrumento = new InstrumentoControl(comando, new FuenteControlFalsa());

            for (int indice = 0; indice < InstrumentoControl.BotonControl.values().length; indice++) {
                InstrumentoControl.BotonControl boton = InstrumentoControl.BotonControl.values()[indice];
                instrumento.procesarEstado(estado(EnumSet.of(boton), 0.0f, 0.0f, 0.0f));
                instrumento.procesarEstado(estado(EnumSet.of(boton), 0.0f, 0.0f, 0.0f));
                instrumento.procesarEstado(estado(Set.of(), 0.0f, 0.0f, 0.0f));

                int posicion = indice * IConstants.CANTIDAD_ACCIONES_POR_TRANSICION_CONTROL;
                assertEquals(EventoMusical.Accion.NOTE_ON, comando.obtenerLlamadas().get(posicion).accion());
                assertEquals(notas[indice], comando.obtenerLlamadas().get(posicion).nota());
                assertArrayEquals(
                    new float[] {
                        IConstants.AMPLITUD_DEFAULT_1,
                        IConstants.AMPLITUD_DEFAULT_2,
                        IConstants.AMPLITUD_DEFAULT_3
                    },
                    comando.obtenerLlamadas().get(posicion).amplitudes(),
                    IConstants.TOLERANCIA_FLOAT_PRUEBA
                );
                assertEquals(EventoMusical.Accion.NOTE_OFF, comando.obtenerLlamadas().get(posicion + 1).accion());
            }
            assertEquals(
                notas.length * IConstants.CANTIDAD_ACCIONES_POR_TRANSICION_CONTROL,
                comando.obtenerLlamadas().size()
            );
        }
    }

    @Test
    void gestionaPresionesSimultaneasDeFormaIndependiente() throws Exception {
        try (ClienteSuperCollider cliente = crearCliente()) {
            ComandoSynthFalso comando = new ComandoSynthFalso(cliente);
            InstrumentoControl instrumento = new InstrumentoControl(comando, new FuenteControlFalsa());
            Set<InstrumentoControl.BotonControl> botones = EnumSet.of(
                InstrumentoControl.BotonControl.CRUZ,
                InstrumentoControl.BotonControl.CIRCULO
            );

            instrumento.procesarEstado(estado(botones, 0.0f, 0.0f, 0.0f));
            instrumento.procesarEstado(estado(Set.of(), 0.0f, 0.0f, 0.0f));

            assertEquals(IConstants.CANTIDAD_LLAMADAS_PRESIONES_SIMULTANEAS, comando.obtenerLlamadas().size());
            assertEquals(EventoMusical.Accion.NOTE_ON, comando.obtenerLlamadas().get(0).accion());
            assertEquals(EventoMusical.Accion.NOTE_ON, comando.obtenerLlamadas().get(1).accion());
            assertEquals(EventoMusical.Accion.NOTE_OFF, comando.obtenerLlamadas().get(2).accion());
            assertEquals(EventoMusical.Accion.NOTE_OFF, comando.obtenerLlamadas().get(3).accion());
        }
    }

    @Test
    void calculaAmplitudesConZonaMuertaYLimites() {
        assertArrayEquals(
            new float[] {
                IConstants.AMPLITUD_DEFAULT_1,
                IConstants.AMPLITUD_DEFAULT_2,
                IConstants.AMPLITUD_DEFAULT_3
            },
            InstrumentoControl.calcularAmplitudes(
                IConstants.CONTROL_ZONA_MUERTA,
                -IConstants.CONTROL_ZONA_MUERTA,
                0.0f
            ),
            IConstants.TOLERANCIA_FLOAT_PRUEBA
        );
        assertArrayEquals(
            new float[] {
                IConstants.AMPLITUD_MINIMA_ORGANO,
                IConstants.AMPLITUD_MAXIMA_ORGANO,
                IConstants.AMPLITUD_MAXIMA_ORGANO
            },
            InstrumentoControl.calcularAmplitudes(-1.0f, 1.0f, 1.0f),
            IConstants.TOLERANCIA_FLOAT_PRUEBA
        );
        assertArrayEquals(
            new float[] {
                IConstants.AMPLITUD_DEFAULT_1,
                IConstants.AMPLITUD_DEFAULT_2,
                IConstants.AMPLITUD_DEFAULT_3
            },
            InstrumentoControl.calcularAmplitudes(Float.NaN, 0.0f, Float.POSITIVE_INFINITY),
            IConstants.TOLERANCIA_FLOAT_PRUEBA
        );
    }

    @Test
    void actualizaAmplitudesDeNotasActivasSinRetrigger() throws Exception {
        try (ClienteSuperCollider cliente = crearCliente()) {
            ComandoSynthFalso comando = new ComandoSynthFalso(cliente);
            InstrumentoControl instrumento = new InstrumentoControl(comando, new FuenteControlFalsa());
            InstrumentoControl.EstadoControl notaPresionada = estado(
                EnumSet.of(InstrumentoControl.BotonControl.CRUZ),
                0.0f,
                0.0f,
                0.0f
            );

            instrumento.procesarEstado(notaPresionada);
            instrumento.procesarEstado(estado(
                EnumSet.of(InstrumentoControl.BotonControl.CRUZ),
                -1.0f,
                1.0f,
                1.0f
            ));

            assertEquals(2, comando.obtenerLlamadas().size());
            assertEquals(EventoMusical.Accion.NOTE_ON, comando.obtenerLlamadas().get(0).accion());
            assertEquals(EventoMusical.Accion.ACTUALIZAR_PARAMETROS, comando.obtenerLlamadas().get(1).accion());
            assertArrayEquals(
                new float[] {
                    IConstants.AMPLITUD_MINIMA_ORGANO,
                    IConstants.AMPLITUD_MAXIMA_ORGANO,
                    IConstants.AMPLITUD_MAXIMA_ORGANO
                },
                comando.obtenerLlamadas().get(1).amplitudes(),
                IConstants.TOLERANCIA_FLOAT_PRUEBA
            );

            instrumento.procesarEstado(estado(Set.of(), -1.0f, 1.0f, 1.0f));
            assertEquals(EventoMusical.Accion.NOTE_OFF, comando.obtenerLlamadas().get(2).accion());
        }
    }

    @Test
    void detenerLiberaNotasActivasYRecursosDelControl() throws Exception {
        try (ClienteSuperCollider cliente = crearCliente()) {
            ComandoSynthFalso comando = new ComandoSynthFalso(cliente);
            FuenteControlBloqueante fuente = new FuenteControlBloqueante();
            InstrumentoControl instrumento = new InstrumentoControl(comando, fuente);
            instrumento.procesarEstado(estado(
                EnumSet.of(InstrumentoControl.BotonControl.CRUZ),
                0.0f,
                0.0f,
                0.0f
            ));
            instrumento.iniciar();

            assertTrue(fuente.lecturaIniciada.await(
                IConstants.TIMEOUT_PRUEBA_MS,
                TimeUnit.MILLISECONDS
            ));
            instrumento.detener();

            assertEquals(2, comando.obtenerLlamadas().size());
            assertEquals(EventoMusical.Accion.NOTE_OFF, comando.obtenerLlamadas().get(1).accion());
            assertTrue(fuente.cerrada.get());
        }
    }

    @Test
    void desconexionLiberaNotasActivas() throws Exception {
        try (ClienteSuperCollider cliente = crearCliente()) {
            ComandoSynthFalso comando = new ComandoSynthFalso(cliente);
            FuenteControlDesconectada fuente = new FuenteControlDesconectada();
            InstrumentoControl instrumento = new InstrumentoControl(comando, fuente);
            instrumento.procesarEstado(estado(
                EnumSet.of(InstrumentoControl.BotonControl.CRUZ),
                IConstants.CONTROL_EJE_NEUTRO,
                IConstants.CONTROL_EJE_NEUTRO,
                IConstants.CONTROL_EJE_NEUTRO
            ));

            instrumento.iniciar();
            assertTrue(fuente.cerrada.await(
                IConstants.TIMEOUT_PRUEBA_MS,
                TimeUnit.MILLISECONDS
            ));

            assertEquals(
                EventoMusical.Accion.NOTE_OFF,
                comando.obtenerLlamadas().get(comando.obtenerLlamadas().size() - 1).accion()
            );
        }
    }

    private static InstrumentoControl.EstadoControl estado(
        Set<InstrumentoControl.BotonControl> pBotones,
        float pEjeIzquierdoX,
        float pEjeIzquierdoY,
        float pEjeDerechoY
    ) {
        return new InstrumentoControl.EstadoControl(
            pBotones,
            pEjeIzquierdoX,
            pEjeIzquierdoY,
            pEjeDerechoY
        );
    }

    private static ClienteSuperCollider crearCliente() throws Exception {
        return new ClienteSuperCollider(
            IConstants.HOST_SUPERCOLLIDER,
            IConstants.PUERTO_SUPERCOLLIDER,
            new TransporteOscFalso(),
            System::nanoTime
        );
    }

    private static class FuenteControlFalsa implements InstrumentoControl.FuenteControl {
        @Override
        public void iniciar() {
        }

        @Override
        public InstrumentoControl.EstadoControl leerEstado() throws DispositivoNoDisponibleException {
            return estado(Set.of(), 0.0f, 0.0f, 0.0f);
        }

        @Override
        public void cerrar() {
        }
    }

    private static final class FuenteControlBloqueante extends FuenteControlFalsa {
        private final CountDownLatch lecturaIniciada = new CountDownLatch(
            IConstants.CANTIDAD_EVENTOS_CIERRE_INSTRUMENTOS
        );
        private final AtomicBoolean cerrada = new AtomicBoolean();

        @Override
        public InstrumentoControl.EstadoControl leerEstado() throws DispositivoNoDisponibleException {
            lecturaIniciada.countDown();
            try {
                new CountDownLatch(IConstants.CANTIDAD_EVENTOS_CIERRE_INSTRUMENTOS).await();
            } catch (InterruptedException pExcepcion) {
                Thread.currentThread().interrupt();
                throw new DispositivoNoDisponibleException(IConstants.ERROR_CONTROL_DESCONECTADO, pExcepcion);
            }
            return estado(Set.of(), 0.0f, 0.0f, 0.0f);
        }

        @Override
        public void cerrar() {
            cerrada.set(true);
        }
    }

    private static final class FuenteControlDesconectada extends FuenteControlFalsa {
        private final CountDownLatch cerrada = new CountDownLatch(
            IConstants.CANTIDAD_EVENTOS_CIERRE_INSTRUMENTOS
        );

        @Override
        public InstrumentoControl.EstadoControl leerEstado() throws DispositivoNoDisponibleException {
            throw new DispositivoNoDisponibleException(IConstants.ERROR_CONTROL_DESCONECTADO);
        }

        @Override
        public void cerrar() {
            cerrada.countDown();
        }
    }
}
