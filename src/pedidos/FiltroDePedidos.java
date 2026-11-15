package pedidos;

import java.util.Map;

/**
 * Criterios de busqueda de pedidos.
 *
 * REFACTORING: Introduce Parameter Object + Replace Method with Method Object.
 *
 * Antes buscar() recibia cinco parametros opcionales:
 *
 *     buscar(estado, provincia, montoMinimo, soloUrgentes, codigoProducto)
 *
 * y el punto de llamada quedaba ilegible:
 *
 *     buscar(null, null, null, true, null)
 *
 * Hay que contar las comas para saber que se esta filtrando. Con el objeto de
 * filtro, la misma consulta se lee sola:
 *
 *     new FiltroDePedidos().soloUrgentes()
 *
 * Se usa interfaz fluida porque los criterios son opcionales e independientes:
 * cada uno se agrega si hace falta, sin pasar null por los demas.
 */
public class FiltroDePedidos {

    private EstadoPedido estado;
    private String provincia;
    private Dinero montoMinimo;
    private Boolean urgente;
    private String codigoProducto;

    public FiltroDePedidos conEstado(EstadoPedido estado) {
        this.estado = estado;
        return this;
    }

    public FiltroDePedidos enProvincia(String provincia) {
        this.provincia = provincia;
        return this;
    }

    public FiltroDePedidos desdeMonto(Dinero montoMinimo) {
        this.montoMinimo = montoMinimo;
        return this;
    }

    public FiltroDePedidos soloUrgentes() {
        this.urgente = true;
        return this;
    }

    public FiltroDePedidos delProducto(String codigoProducto) {
        this.codigoProducto = codigoProducto;
        return this;
    }

    /**
     * REFACTORING: Decompose Conditional. Antes eran cinco bloques if que
     * apagaban una bandera `pasa`. Cada criterio ahora es una pregunta
     * independiente y el conjunto se lee como la regla que es.
     */
    public boolean aceptaA(Pedido pedido, Map<String, Cliente> clientes) {
        return cumpleEstado(pedido)
                && cumpleProvincia(pedido, clientes)
                && cumpleMonto(pedido)
                && cumpleUrgencia(pedido)
                && cumpleProducto(pedido);
    }

    private boolean cumpleEstado(Pedido p) {
        return estado == null || p.estado() == estado;
    }

    private boolean cumpleProvincia(Pedido p, Map<String, Cliente> clientes) {
        if (provincia == null) {
            return true;
        }
        Cliente c = clientes.get(p.documentoCliente());
        return c != null && provincia.equals(c.provincia());
    }

    private boolean cumpleMonto(Pedido p) {
        return montoMinimo == null || p.total().esMayorOIgualA(montoMinimo);
    }

    private boolean cumpleUrgencia(Pedido p) {
        return urgente == null || !urgente || p.esUrgente();
    }

    private boolean cumpleProducto(Pedido p) {
        return codigoProducto == null || p.esDelProducto(codigoProducto);
    }
}
