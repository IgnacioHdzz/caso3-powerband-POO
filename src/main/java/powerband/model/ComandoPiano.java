package powerband.model;

import java.util.Objects;
import powerband.dto.EventoMusical;
import powerband.excepciones.PowerBandException;
import powerband.service.ClienteSuperCollider;
import powerband.util.IConstants;

public final class ComandoPiano extends ComandoSynth {
    public ComandoPiano(ClienteSuperCollider pClienteSuperCollider) {
        super(pClienteSuperCollider);
    }

    public void disparar(int pNota, int pDuracionMs, boolean pEsMayor) throws PowerBandException {
        int tercera = pEsMayor
            ? IConstants.TERCERA_MAYOR_SEMITONOS
            : IConstants.TERCERA_MENOR_SEMITONOS;
        ejecutar(EventoMusical.Accion.DISPARO, pNota, pDuracionMs, tercera, null, null);
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
        if (pAccion != EventoMusical.Accion.DISPARO
            || pDuracionMs == null
            || pTercera == null
            || (pTercera != IConstants.TERCERA_MAYOR_SEMITONOS
                && pTercera != IConstants.TERCERA_MENOR_SEMITONOS)) {
            throw new IllegalArgumentException(IConstants.ERROR_PARAMETROS_INCOMPLETOS);
        }

        ClienteSuperCollider cliente = obtenerClienteSuperCollider();
        EventoMusical evento = cliente.crearEvento(
            IConstants.SYNTH_PIANO,
            EventoMusical.Accion.DISPARO,
            pNota,
            pDuracionMs,
            pTercera,
            null,
            null,
            null,
            null,
            null,
            null
        );
        cliente.crearNodo(
            IConstants.SYNTH_PIANO,
            evento,
            IConstants.CONTROL_NOTA, pNota,
            IConstants.CONTROL_DURACION_MS, pDuracionMs,
            IConstants.CONTROL_TERCERA, pTercera
        );
    }
}
