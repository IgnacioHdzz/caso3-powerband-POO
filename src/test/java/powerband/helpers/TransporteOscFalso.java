package powerband.helpers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import powerband.service.TransporteOsc;

public final class TransporteOscFalso implements TransporteOsc {
    private final List<byte[]> mensajes;
    private IOException falloUnaVez;

    public TransporteOscFalso() {
        this.mensajes = new CopyOnWriteArrayList<>();
    }

    @Override
    public synchronized void enviar(byte[] pMensaje) throws IOException {
        if (falloUnaVez != null) {
            IOException fallo = falloUnaVez;
            falloUnaVez = null;
            throw fallo;
        }
        mensajes.add(pMensaje.clone());
    }

    public synchronized void fallarUnaVez(IOException pFallo) {
        falloUnaVez = pFallo;
    }

    public List<byte[]> obtenerMensajes() {
        List<byte[]> copia = new ArrayList<>();
        for (byte[] mensaje : mensajes) {
            copia.add(mensaje.clone());
        }
        return copia;
    }
}
