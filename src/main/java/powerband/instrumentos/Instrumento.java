package powerband.instrumentos;

import java.util.Objects;
import powerband.excepciones.DispositivoNoDisponibleException;
import powerband.excepciones.PowerBandException;
import powerband.model.ComandoSynth;

public abstract class Instrumento {
    private final ComandoSynth comandoSynth;

    protected Instrumento(ComandoSynth pComandoSynth) {
        this.comandoSynth = Objects.requireNonNull(pComandoSynth);
    }

    protected final ComandoSynth obtenerComandoSynth() {
        return comandoSynth;
    }

    public abstract void iniciar() throws DispositivoNoDisponibleException;

    public abstract void detener() throws PowerBandException;
}
