package powerband.model;

import java.util.Objects;
import powerband.dto.EventoMusical;
import powerband.excepciones.PowerBandException;
import powerband.service.ClienteSuperCollider;

public abstract class ComandoSynth {
    private final ClienteSuperCollider clienteSuperCollider;

    protected ComandoSynth(ClienteSuperCollider pClienteSuperCollider) {
        this.clienteSuperCollider = Objects.requireNonNull(pClienteSuperCollider);
    }

    public abstract void ejecutar(
        EventoMusical.Accion pAccion,
        int pNota,
        Integer pDuracionMs,
        Integer pTercera,
        float[] pRatios,
        float[] pAmplitudes
    ) throws PowerBandException;

    protected final ClienteSuperCollider obtenerClienteSuperCollider() {
        return clienteSuperCollider;
    }
}
