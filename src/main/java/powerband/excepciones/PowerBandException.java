package powerband.excepciones;

import powerband.util.IConstants;

public class PowerBandException extends Exception {
    private static final long serialVersionUID = IConstants.SERIAL_VERSION_UID;

    public PowerBandException(String pMensaje) {
        super(pMensaje);
    }

    public PowerBandException(String pMensaje, Throwable pCausa) {
        super(pMensaje, pCausa);
    }
}
