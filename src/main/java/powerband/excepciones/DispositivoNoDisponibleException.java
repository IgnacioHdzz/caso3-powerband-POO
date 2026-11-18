package powerband.excepciones;

import powerband.util.IConstants;

public class DispositivoNoDisponibleException extends PowerBandException {
    private static final long serialVersionUID = IConstants.SERIAL_VERSION_UID;

    public DispositivoNoDisponibleException(String pMensaje) {
        super(pMensaje);
    }

    public DispositivoNoDisponibleException(String pMensaje, Throwable pCausa) {
        super(pMensaje, pCausa);
    }
}
