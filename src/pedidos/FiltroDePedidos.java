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
    private boolean soloUrgentes;
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
        this.soloUrgentes = true;
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

    /**
     * CORRECCION: la comparacion de provincia era sensible a mayusculas y a
     * espacios, mientras que ZonaEnvio.deProvincia (que decide cuanto se cobra
     * de flete) normaliza con trim + equalsIgnoreCase.
     *
     * Dos criterios distintos para el mismo dato producian una incoherencia
     * visible: un pedido cargado como "mendoza" se cobraba con tarifa de
     * Mendoza pero NO aparecia al buscar por "Mendoza". El sistema cobraba
     * segun una regla y listaba segun otra.
     *
     * Se normaliza igual que ZonaEnvio. La forma de fondo seria filtrar por
     * ZonaEnvio en lugar de por texto libre, pero eso cambia la firma publica
     * del filtro; se deja anotado como mejora.
     *
     * ponytail: techo deliberado. Si la provincia pasa a ser un concepto del
     * dominio (y no un String), este metodo desaparece.
     *
     * SEGUNDA CORRECCION: ahora se filtra por la provincia DEL PEDIDO y no por
     * la del cliente. El cliente es mutable y refleja siempre su ultimo pedido,
     * asi que buscar por su provincia hacia que un pedido viejo apareciera bajo
     * un destino al que nunca se envio. El pedido guarda el destino con el que
     * se cotizo, que es el dato correcto para un listado historico. Se consulta
     * al cliente solo si el pedido no tiene direccion (retiro en local).
     */
    private boolean cumpleProvincia(Pedido p, Map<String, Cliente> clientes) {
        if (provincia == null) {
            return true;
        }
        String provinciaDelPedido = p.provinciaDeEntrega();
        if (provinciaDelPedido != null) {
            return mismaProvincia(provincia, provinciaDelPedido);
        }
        Cliente c = clientes.get(p.documentoCliente());
        return c != null && mismaProvincia(provincia, c.provincia());
    }

    private static boolean mismaProvincia(String a, String b) {
        if (a == null || b == null) {
            return a == b;
        }
        return a.trim().equalsIgnoreCase(b.trim());
    }

    private boolean cumpleMonto(Pedido p) {
        return montoMinimo == null || p.total().esMayorOIgualA(montoMinimo);
    }

    /**
     * El campo era Boolean con tres estados posibles (null, false, true) pero
     * el unico setter asigna true, asi que la rama "false" era codigo muerto:
     * no existia forma de alcanzarla. Un boolean simple expresa exactamente los
     * dos casos que el filtro tiene: o pide solo urgentes, o no filtra por
     * urgencia. Si alguna vez hace falta listar los NO urgentes, se agrega el
     * setter que falta y ahi si el tercer estado tendra sentido.
     */
    private boolean cumpleUrgencia(Pedido p) {
        return !soloUrgentes || p.esUrgente();
    }

    private boolean cumpleProducto(Pedido p) {
        return codigoProducto == null || p.esDelProducto(codigoProducto);
    }
}
