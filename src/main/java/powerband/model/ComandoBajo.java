package powerband.model;

import powerband.dto.EventoMusical;
import powerband.excepciones.PowerBandException;
import powerband.service.ClienteSuperCollider;
import powerband.util.IConstants;

public final class ComandoBajo extends ComandoSynth {
    public ComandoBajo(ClienteSuperCollider pClienteSuperCollider) {
        super(pClienteSuperCollider);
    }

    public void disparar(int pNota, int pDuracionMs) throws PowerBandException {
        ejecutar(EventoMusical.Accion.DISPARO, pNota, pDuracionMs, null, null, null);
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
        if (pAccion != EventoMusical.Accion.DISPARO || pDuracionMs == null) {
            throw new IllegalArgumentException(IConstants.ERROR_PARAMETROS_INCOMPLETOS);
        }

        ClienteSuperCollider cliente = obtenerClienteSuperCollider();
        EventoMusical evento = cliente.crearEvento(
            IConstants.SYNTH_BAJO,
            EventoMusical.Accion.DISPARO,
            pNota,
            pDuracionMs,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );
        cliente.crearNodo(
            IConstants.SYNTH_BAJO,
            evento,
            IConstants.CONTROL_NOTA, pNota,
            IConstants.CONTROL_DURACION_MS, pDuracionMs
        );
    }
}
