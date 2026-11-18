package powerband.instrumentos;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import powerband.dto.EventoMusical;
import powerband.excepciones.PowerBandException;
import powerband.model.ComandoSynth;
import powerband.service.ClienteSuperCollider;

final class ComandoSynthFalso extends ComandoSynth {
    private final List<Llamada> llamadas;

    ComandoSynthFalso(ClienteSuperCollider pClienteSuperCollider) {
        super(pClienteSuperCollider);
        llamadas = new CopyOnWriteArrayList<>();
    }

    @Override
    public void ejecutar(
        EventoMusical.Accion pAccion,
        int pNota,
        Integer pDuracionMs,
        Integer pTercera,
        float[] pRatios,
        float[] pAmplitudes
    ) throws PowerBandException {
        llamadas.add(new Llamada(
            pAccion,
            pNota,
            pDuracionMs,
            pTercera,
            pRatios == null ? null : pRatios.clone(),
            pAmplitudes == null ? null : pAmplitudes.clone()
        ));
    }

    List<Llamada> obtenerLlamadas() {
        return List.copyOf(llamadas);
    }

    record Llamada(
        EventoMusical.Accion accion,
        int nota,
        Integer duracionMs,
        Integer tercera,
        float[] ratios,
        float[] amplitudes
    ) {
    }
}
