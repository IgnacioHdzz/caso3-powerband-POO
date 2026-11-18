package powerband.helpers;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class LectorOsc {
    private LectorOsc() {
    }

    public static String leerDireccion(byte[] pMensaje) {
        return leerCadena(pMensaje, new AtomicInteger());
    }

    public static List<Object> leerArgumentos(byte[] pMensaje) {
        AtomicInteger posicion = new AtomicInteger();
        leerCadena(pMensaje, posicion);
        String etiquetas = leerCadena(pMensaje, posicion);
        List<Object> argumentos = new ArrayList<>();
        for (int indice = 1; indice < etiquetas.length(); indice++) {
            char etiqueta = etiquetas.charAt(indice);
            if (etiqueta == 'i') {
                argumentos.add(ByteBuffer.wrap(pMensaje, posicion.get(), Integer.BYTES)
                    .order(ByteOrder.BIG_ENDIAN)
                    .getInt());
                posicion.addAndGet(Integer.BYTES);
            } else if (etiqueta == 'f') {
                argumentos.add(ByteBuffer.wrap(pMensaje, posicion.get(), Integer.BYTES)
                    .order(ByteOrder.BIG_ENDIAN)
                    .getFloat());
                posicion.addAndGet(Integer.BYTES);
            } else if (etiqueta == 's') {
                argumentos.add(leerCadena(pMensaje, posicion));
            } else {
                throw new IllegalArgumentException("Etiqueta OSC de prueba no admitida.");
            }
        }
        return argumentos;
    }

    private static String leerCadena(byte[] pMensaje, AtomicInteger pPosicion) {
        int inicio = pPosicion.get();
        int fin = inicio;
        while (fin < pMensaje.length && pMensaje[fin] != 0) {
            fin++;
        }
        if (fin == pMensaje.length) {
            throw new IllegalArgumentException("String OSC de prueba sin terminador.");
        }
        pPosicion.set((fin + Integer.BYTES) & ~(Integer.BYTES - 1));
        return new String(pMensaje, inicio, fin - inicio, StandardCharsets.UTF_8);
    }
}
