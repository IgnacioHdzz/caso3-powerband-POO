package powerband.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import powerband.dto.EventoMusical;
import powerband.helpers.LectorOsc;
import powerband.helpers.TransporteOscFalso;
import powerband.service.ClienteSuperCollider;
import powerband.util.IConstants;

class ComandoBajoTest {
    @Test
    void generaEventoDeDisparoConNotaYDuracion() throws Exception {
        TransporteOscFalso transporte = new TransporteOscFalso();
        List<EventoMusical> eventos = new ArrayList<>();
        try (ClienteSuperCollider cliente = new ClienteSuperCollider(
            IConstants.HOST_SUPERCOLLIDER,
            IConstants.PUERTO_SUPERCOLLIDER,
            transporte,
            new AtomicLong()::get
        )) {
            cliente.agregarObservador(eventos::add);
            new ComandoBajo(cliente).disparar(36, 450);

            assertEquals(
                List.of(
                    IConstants.SYNTH_BAJO,
                    IConstants.OSC_PRIMER_ID_NODO,
                    IConstants.OSC_ADD_TO_HEAD,
                    IConstants.OSC_GRUPO_DEFAULT,
                    IConstants.CONTROL_NOTA,
                    36,
                    IConstants.CONTROL_DURACION_MS,
                    450
                ),
                LectorOsc.leerArgumentos(transporte.obtenerMensajes().get(0))
            );
            assertEquals(1, eventos.size());
            assertEquals(EventoMusical.Accion.DISPARO, eventos.get(0).getAccion());
            assertEquals(IConstants.SYNTH_BAJO, eventos.get(0).getTipoSynth());
            assertEquals(36, eventos.get(0).getNota());
            assertEquals(450, eventos.get(0).getDuracionMs());
            assertNull(eventos.get(0).getTercera());
            assertNull(eventos.get(0).getRatio1());
        }
    }
}
