package powerband.model;

import powerband.dto.EventoMusical;
import powerband.excepciones.PowerBandException;
import powerband.service.ClienteSuperCollider;
import powerband.util.IConstants;

public final class ComandoOrgano extends ComandoSynth {
    public ComandoOrgano(ClienteSuperCollider pClienteSuperCollider) {
        super(pClienteSuperCollider);
    }

    public void noteOn(int pNota) throws PowerBandException {
        noteOn(
            pNota,
            new float[] {
                IConstants.RATIO_DEFAULT_1,
                IConstants.RATIO_DEFAULT_2,
                IConstants.RATIO_DEFAULT_3
            },
            new float[] {
                IConstants.AMPLITUD_DEFAULT_1,
                IConstants.AMPLITUD_DEFAULT_2,
                IConstants.AMPLITUD_DEFAULT_3
            }
        );
    }

    public void noteOn(int pNota, float[] pRatios, float[] pAmplitudes) throws PowerBandException {
        ejecutar(EventoMusical.Accion.NOTE_ON, pNota, null, null, pRatios, pAmplitudes);
    }

    public void noteOff(int pNota) throws PowerBandException {
        ejecutar(EventoMusical.Accion.NOTE_OFF, pNota, null, null, null, null);
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
        ClienteSuperCollider cliente = obtenerClienteSuperCollider();
        if (pAccion == EventoMusical.Accion.NOTE_OFF) {
            cliente.apagarOrgano(pNota);
            return;
        }
        if (pAccion != EventoMusical.Accion.NOTE_ON
            || pRatios == null
            || pAmplitudes == null
            || pRatios.length != IConstants.CANTIDAD_PARCIALES
            || pAmplitudes.length != IConstants.CANTIDAD_PARCIALES) {
            throw new IllegalArgumentException(IConstants.ERROR_TRES_PARCIALES);
        }

        float[] ratios = pRatios.clone();
        float[] amplitudes = pAmplitudes.clone();
        float frecuencia = (float) (
            IConstants.FRECUENCIA_LA4_HZ
                * Math.pow(
                    2.0,
                    (pNota - IConstants.NOTA_MIDI_LA4) / IConstants.SEMITONOS_POR_OCTAVA
                )
        );
        EventoMusical evento = cliente.crearEvento(
            IConstants.SYNTH_ORGANO,
            EventoMusical.Accion.NOTE_ON,
            pNota,
            null,
            null,
            ratios[0],
            ratios[1],
            ratios[2],
            amplitudes[0],
            amplitudes[1],
            amplitudes[2]
        );
        cliente.iniciarOrgano(pNota, frecuencia, ratios, amplitudes, evento);
    }
}
