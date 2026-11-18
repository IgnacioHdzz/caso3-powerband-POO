package powerband;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;
import powerband.excepciones.DispositivoNoDisponibleException;
import powerband.instrumentos.Instrumento;
import powerband.model.ComandoPiano;
import powerband.service.ClienteSuperCollider;
import powerband.helpers.TransporteOscFalso;
import powerband.util.IConstants;

class MainTest {
    @Test
    void seleccionaDemoPorDefectoYConArgumentosPosicionales() {
        Main.OpcionesEjecucion predeterminada = Main.seleccionarOpciones(new String[0]);
        assertEquals(Main.ModoEjecucion.DEMO, predeterminada.modo());
        assertEquals(IConstants.HOST_SUPERCOLLIDER, predeterminada.anfitrion());
        assertEquals(IConstants.PUERTO_SUPERCOLLIDER, predeterminada.puerto());

        Main.OpcionesEjecucion host = Main.seleccionarOpciones(new String[] { IConstants.HOST_PRUEBA });
        assertEquals(Main.ModoEjecucion.DEMO, host.modo());
        assertEquals(IConstants.HOST_PRUEBA, host.anfitrion());
        assertEquals(IConstants.PUERTO_SUPERCOLLIDER, host.puerto());

        Main.OpcionesEjecucion destino = Main.seleccionarOpciones(
            new String[] { IConstants.HOST_PRUEBA, IConstants.PUERTO_PRUEBA_TEXTO }
        );
        assertEquals(IConstants.PUERTO_PRUEBA, destino.puerto());
    }

    @Test
    void seleccionaModosExplicitosYValidaArgumentos() {
        Main.OpcionesEjecucion demo = Main.seleccionarOpciones(
            new String[] { IConstants.ARGUMENTO_MODO_DEMO }
        );
        Main.OpcionesEjecucion instrumentos = Main.seleccionarOpciones(
            new String[] {
                IConstants.ARGUMENTO_MODO_INSTRUMENTOS,
                IConstants.HOST_PRUEBA,
                IConstants.PUERTO_PRUEBA_TEXTO
            }
        );
        assertEquals(Main.ModoEjecucion.DEMO, demo.modo());
        assertEquals(Main.ModoEjecucion.INSTRUMENTOS, instrumentos.modo());
        assertEquals(IConstants.HOST_PRUEBA, instrumentos.anfitrion());
        assertEquals(IConstants.PUERTO_PRUEBA, instrumentos.puerto());
        assertThrows(
            IllegalArgumentException.class,
            () -> Main.seleccionarOpciones(new String[] { IConstants.ARGUMENTO_MODO_DESCONOCIDO })
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> Main.seleccionarOpciones(new String[] {
                IConstants.ARGUMENTO_MODO_DEMO,
                IConstants.HOST_PRUEBA,
                IConstants.PUERTO_PRUEBA_TEXTO,
                IConstants.ARGUMENTO_MODO_INSTRUMENTOS
            })
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> Main.seleccionarOpciones(new String[] { IConstants.HOST_PRUEBA, IConstants.PUERTO_INVALIDO_TEXTO })
        );
    }

    @Test
    void iniciaCadaInstrumentoAunqueElAnteriorNoEsteDisponible() throws Exception {
        try (ClienteSuperCollider cliente = new ClienteSuperCollider(
            IConstants.HOST_SUPERCOLLIDER,
            IConstants.PUERTO_SUPERCOLLIDER,
            new TransporteOscFalso(),
            System::nanoTime
        )) {
            InstrumentoFalso ausente = new InstrumentoFalso(new ComandoPiano(cliente), true);
            InstrumentoFalso disponible = new InstrumentoFalso(new ComandoPiano(cliente), false);

            List<Instrumento> iniciados = Main.iniciarInstrumentosIndependientemente(
                List.of(ausente, disponible)
            );

            assertEquals(List.of(disponible), iniciados);
            assertEquals(IConstants.CANTIDAD_INTENTOS_INICIALIZACION, ausente.intentos);
            assertEquals(IConstants.CANTIDAD_INTENTOS_INICIALIZACION, disponible.intentos);
        }
    }

    private static final class InstrumentoFalso extends Instrumento {
        private final boolean noDisponible;
        private int intentos;

        private InstrumentoFalso(ComandoPiano pComando, boolean pNoDisponible) {
            super(pComando);
            noDisponible = pNoDisponible;
        }

        @Override
        public void iniciar() throws DispositivoNoDisponibleException {
            intentos++;
            if (noDisponible) {
                throw new DispositivoNoDisponibleException(IConstants.ERROR_CONTROL_NO_DISPONIBLE);
            }
        }

        @Override
        public void detener() {
        }
    }
}
