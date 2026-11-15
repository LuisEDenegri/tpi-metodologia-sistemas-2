package pedidos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Notificador que no escribe nada: registra los avisos en memoria.
 *
 * Existe por dos razones:
 *
 *   1. Las pruebas no deberian llenarse de notificaciones. Con el codigo
 *      original eso era imposible, porque PedidoManager instanciaba
 *      EmailService con `new` y no habia forma de sustituirlo. Que esta clase
 *      pueda existir es la prueba de que la inversion de dependencias funciono.
 *
 *   2. Permite VERIFICAR las notificaciones en vez de solo silenciarlas: una
 *      prueba puede afirmar que al cancelar un pedido se aviso al cliente.
 *
 * Es un spy, no un mock: no simula comportamiento, solo recuerda lo que paso.
 */
public class NotificadorSilencioso implements Notificador {

    private final List<String> avisos = new ArrayList<>();
    private final List<String> eventos = new ArrayList<>();

    @Override
    public void notificarAlCliente(Cliente cliente, String mensaje) {
        String destino = cliente == null ? "(cliente inexistente)" : cliente.documento();
        avisos.add(destino + ": " + mensaje);
    }

    @Override
    public void notificarStockBajo(String codigoProducto, int disponible) {
        avisos.add("deposito: stock bajo de " + codigoProducto + " (" + disponible + ")");
    }

    @Override
    public void registrarEvento(String evento, String referencia) {
        eventos.add(evento + "::" + referencia);
    }

    public List<String> avisos() {
        return Collections.unmodifiableList(avisos);
    }

    public List<String> eventos() {
        return Collections.unmodifiableList(eventos);
    }

    public boolean seNotificoA(String documentoCliente) {
        return avisos.stream().anyMatch(a -> a.startsWith(documentoCliente + ":"));
    }

    public boolean seRegistro(String evento) {
        return eventos.stream().anyMatch(e -> e.startsWith(evento + "::"));
    }
}
