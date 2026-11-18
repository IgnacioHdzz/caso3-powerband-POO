package powerband.dto;

import java.util.Objects;

public final class EventoMusical {
    public enum Accion {
        NOTE_ON,
        NOTE_OFF,
        DISPARO,
        ACTUALIZAR_PARAMETROS
    }

    private final long instanteMs;
    private final String tipoSynth;
    private final Accion accion;
    private final int nota;
    private final Integer duracionMs;
    private final Integer tercera;
    private final Float ratio1;
    private final Float ratio2;
    private final Float ratio3;
    private final Float amplitud1;
    private final Float amplitud2;
    private final Float amplitud3;

    public EventoMusical(
        long pInstanteMs,
        String pTipoSynth,
        Accion pAccion,
        int pNota,
        Integer pDuracionMs,
        Integer pTercera,
        Float pRatio1,
        Float pRatio2,
        Float pRatio3,
        Float pAmplitud1,
        Float pAmplitud2,
        Float pAmplitud3
    ) {
        this.instanteMs = pInstanteMs;
        this.tipoSynth = Objects.requireNonNull(pTipoSynth);
        this.accion = Objects.requireNonNull(pAccion);
        this.nota = pNota;
        this.duracionMs = pDuracionMs;
        this.tercera = pTercera;
        this.ratio1 = pRatio1;
        this.ratio2 = pRatio2;
        this.ratio3 = pRatio3;
        this.amplitud1 = pAmplitud1;
        this.amplitud2 = pAmplitud2;
        this.amplitud3 = pAmplitud3;
    }

    public long getInstanteMs() {
        return instanteMs;
    }

    public String getTipoSynth() {
        return tipoSynth;
    }

    public Accion getAccion() {
        return accion;
    }

    public int getNota() {
        return nota;
    }

    public Integer getDuracionMs() {
        return duracionMs;
    }

    public Integer getTercera() {
        return tercera;
    }

    public Float getRatio1() {
        return ratio1;
    }

    public Float getRatio2() {
        return ratio2;
    }

    public Float getRatio3() {
        return ratio3;
    }

    public Float getAmplitud1() {
        return amplitud1;
    }

    public Float getAmplitud2() {
        return amplitud2;
    }

    public Float getAmplitud3() {
        return amplitud3;
    }
}
