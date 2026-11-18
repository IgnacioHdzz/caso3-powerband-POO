package powerband.excepciones;

import powerband.util.IConstants;

public class ConexionSynthException extends PowerBandException {
    private static final long serialVersionUID = IConstants.SERIAL_VERSION_UID;

    public ConexionSynthException(String pMensaje, Throwable pCausa) {
        super(pMensaje, pCausa);
    }

    public ConexionSynthException(String pMensaje) {
        super(pMensaje);
    }
}
