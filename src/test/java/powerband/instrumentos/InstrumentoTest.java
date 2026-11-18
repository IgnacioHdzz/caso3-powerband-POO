package powerband.instrumentos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import powerband.excepciones.DispositivoNoDisponibleException;
import powerband.helpers.TransporteOscFalso;
import powerband.model.ComandoSynth;
import powerband.service.ClienteSuperCollider;
import powerband.util.IConstants;

class InstrumentoTest {
    @Test
    void abstraccionBridgeConservaElComandoRecibido() throws Exception {
        try (ClienteSuperCollider cliente = crearCliente()) {
            ComandoSynthFalso comando = new ComandoSynthFalso(cliente);
            InstrumentoFalso instrumento = new InstrumentoFalso(comando);
            assertSame(comando, instrumento.obtenerComando());
        }
    }

    @Test
    void informaDispositivosAusentesSinHardwareReal() throws Exception {
        try (ClienteSuperCollider cliente = crearCliente()) {
            ComandoSynthFalso comando = new ComandoSynthFalso(cliente);
            InstrumentoControl control = new InstrumentoControl(comando, new FuenteControlAusente());
            InstrumentoMicrofono microfono = new InstrumentoMicrofono(
                comando,
                () -> {
                    throw new DispositivoNoDisponibleException(IConstants.ERROR_MICROFONO_NO_DISPONIBLE);
                },
                System::nanoTime
            );

            assertThrows(DispositivoNoDisponibleException.class, control::iniciar);
            assertThrows(DispositivoNoDisponibleException.class, microfono::iniciar);
        }
    }

    @Test
    void detenerMicrofonoCierraLaFuenteYTerminaElHilo() throws Exception {
        try (ClienteSuperCollider cliente = crearCliente()) {
            ComandoSynthFalso comando = new ComandoSynthFalso(cliente);
            FuenteAudioBloqueante fuente = new FuenteAudioBloqueante();
            InstrumentoMicrofono microfono = new InstrumentoMicrofono(
                comando,
                () -> fuente,
                System::nanoTime
            );

            microfono.iniciar();
            assertTrue(fuente.lecturaIniciada.await(
                IConstants.TIMEOUT_PRUEBA_MS,
                TimeUnit.MILLISECONDS
            ));
            microfono.detener();

            assertEquals(IConstants.CANTIDAD_EVENTOS_CIERRE_INSTRUMENTOS, fuente.cierres);
        }
    }

    private static ClienteSuperCollider crearCliente() throws Exception {
        return new ClienteSuperCollider(
            IConstants.HOST_SUPERCOLLIDER,
            IConstants.PUERTO_SUPERCOLLIDER,
            new TransporteOscFalso(),
            System::nanoTime
        );
    }

    private static final class InstrumentoFalso extends Instrumento {
        private InstrumentoFalso(ComandoSynthFalso pComando) {
            super(pComando);
        }

        @Override
        public void iniciar() {
        }

        @Override
        public void detener() {
        }

        private ComandoSynth obtenerComando() {
            return obtenerComandoSynth();
        }
    }

    private static final class FuenteControlAusente implements InstrumentoControl.FuenteControl {
        @Override
        public void iniciar() throws DispositivoNoDisponibleException {
            throw new DispositivoNoDisponibleException(IConstants.ERROR_CONTROL_NO_DISPONIBLE);
        }

        @Override
        public InstrumentoControl.EstadoControl leerEstado() {
            return new InstrumentoControl.EstadoControl(Set.of(), 0.0f, 0.0f, 0.0f);
        }

        @Override
        public void cerrar() {
        }
    }

    private static final class FuenteAudioBloqueante implements InstrumentoMicrofono.FuenteAudio {
        private final CountDownLatch lecturaIniciada = new CountDownLatch(
            IConstants.CANTIDAD_EVENTOS_CIERRE_INSTRUMENTOS
        );
        private final CountDownLatch cerrar = new CountDownLatch(
            IConstants.CANTIDAD_EVENTOS_CIERRE_INSTRUMENTOS
        );
        private int cierres;

        @Override
        public void abrir() {
        }

        @Override
        public int leer(byte[] pBuffer, int pDesplazamiento, int pLongitud)
            throws DispositivoNoDisponibleException {
            lecturaIniciada.countDown();
            try {
                cerrar.await();
                return 0;
            } catch (InterruptedException pExcepcion) {
                Thread.currentThread().interrupt();
                throw new DispositivoNoDisponibleException(IConstants.ERROR_LEER_MICROFONO, pExcepcion);
            }
        }

        @Override
        public void cerrar() {
            cierres++;
            cerrar.countDown();
        }
    }
}
