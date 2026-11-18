package powerband.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import powerband.dto.EventoMusical;
import powerband.helpers.TransporteOscFalso;
import powerband.service.ClienteSuperCollider;
import powerband.util.IConstants;

class ComandoPianoTest {
    @Test
    void generaEventosDeDisparoMayorYMenor() throws Exception {
        List<EventoMusical> eventos = new ArrayList<>();
        try (ClienteSuperCollider cliente = new ClienteSuperCollider(
            IConstants.HOST_SUPERCOLLIDER,
            IConstants.PUERTO_SUPERCOLLIDER,
            new TransporteOscFalso(),
            new AtomicLong()::get
        )) {
            cliente.agregarObservador(eventos::add);
            ComandoPiano piano = new ComandoPiano(cliente);

            piano.disparar(48, 1500, true);
            piano.disparar(57, 1500, false);

            assertEquals(2, eventos.size());
            assertEquals(IConstants.TERCERA_MAYOR_SEMITONOS, eventos.get(0).getTercera());
            assertEquals(IConstants.TERCERA_MENOR_SEMITONOS, eventos.get(1).getTercera());
            assertEquals(EventoMusical.Accion.DISPARO, eventos.get(0).getAccion());
            assertEquals(IConstants.SYNTH_PIANO, eventos.get(0).getTipoSynth());
            assertEquals(48, eventos.get(0).getNota());
            assertEquals(1500, eventos.get(0).getDuracionMs());
            assertNull(eventos.get(0).getRatio1());
            assertNull(eventos.get(0).getAmplitud1());
        }
    }
}
