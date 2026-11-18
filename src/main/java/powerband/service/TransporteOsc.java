package powerband.service;

import java.io.IOException;

@FunctionalInterface
public interface TransporteOsc {
    void enviar(byte[] pMensaje) throws IOException;
}
